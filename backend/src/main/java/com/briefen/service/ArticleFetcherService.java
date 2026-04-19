package com.briefen.service;

import com.briefen.exception.ArticleExtractionException;
import com.briefen.exception.ArticleFetchException;
import com.briefen.validation.UrlValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.select.Elements;

@Service
public class ArticleFetcherService {

    private static final Logger log = LoggerFactory.getLogger(ArticleFetcherService.class);
    private static final int MIN_CONTENT_LENGTH = 100;
    private static final JsonMapper OBJECT_MAPPER = new JsonMapper();

    /**
     * Regex to extract string literals from compiled MDX/JSX source code.
     * Matches JSX text content like: _jsx("p", { children: "Some text" })
     * The key is unquoted in compiled JSX: children: "text"
     */
    private static final Pattern MDX_STRING_CONTENT = Pattern.compile(
            "children:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final int fetchTimeoutMs;
    private final UrlValidator urlValidator;

    public ArticleFetcherService(@Value("${article.fetch-timeout:10s}") Duration fetchTimeout,
                                 UrlValidator urlValidator) {
        this.fetchTimeoutMs = (int) fetchTimeout.toMillis();
        this.urlValidator = urlValidator;
    }

    public ArticleContent fetch(String url) {
        // Re-resolve and re-check the hostname just before connection to prevent DNS rebinding.
        // The URL was validated at request time; a second check here closes the TOCTOU window
        // between that validation and Jsoup's actual TCP connect.
        try {
            String host = URI.create(url).getHost();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                urlValidator.checkResolvedAddress(address);
            }
        } catch (UnknownHostException e) {
            throw new ArticleFetchException("Could not resolve host: " + URI.create(url).getHost(), e);
        }

        String path = URI.create(url).getPath();
        if (path != null && path.toLowerCase().endsWith(".pdf")) {
            return fetchPdf(url);
        }
        return fetchHtml(url);
    }

    private ArticleContent fetchPdf(String url) {
        byte[] pdfBytes;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(fetchTimeoutMs))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(fetchTimeoutMs))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.error("HTTP {} fetching PDF from {}", response.statusCode(), url);
                throw new ArticleFetchException("Failed to fetch PDF: HTTP " + response.statusCode());
            }
            pdfBytes = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ArticleFetchException("PDF fetch interrupted", e);
        } catch (ArticleFetchException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to fetch PDF from {}: {}", url, e.getMessage());
            throw new ArticleFetchException("Failed to fetch PDF. Check server logs for details.", e);
        }

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc).replaceAll("\\s+", " ").strip();

            if (text.length() < MIN_CONTENT_LENGTH) {
                throw new ArticleExtractionException(
                        "Extracted PDF content too short (%d chars). The PDF may be scanned or image-based."
                                .formatted(text.length()));
            }

            String title = doc.getDocumentInformation().getTitle();
            if (title == null || title.isBlank()) {
                title = extractFilenameFromUrl(url);
            }

            log.info("Fetched PDF '{}' from {} ({} chars, {} pages)", title, url, text.length(), doc.getNumberOfPages());
            return new ArticleContent(title, text);
        } catch (ArticleExtractionException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to parse PDF from {}: {}", url, e.getMessage());
            throw new ArticleFetchException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    private ArticleContent fetchHtml(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .timeout(fetchTimeoutMs)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .followRedirects(false)
                    .get();
        } catch (HttpStatusException e) {
            log.error("HTTP {} fetching article from {}: {}", e.getStatusCode(), url, e.getMessage());
            if (e.getStatusCode() == 403) {
                throw new ArticleFetchException(
                        "This website blocked the request (HTTP 403). It may use bot protection (e.g. Cloudflare) that prevents automated access.", e);
            }
            throw new ArticleFetchException("Failed to fetch article: HTTP " + e.getStatusCode(), e);
        } catch (IOException e) {
            log.error("Failed to fetch article from {}: {}", url, e.getMessage());
            throw new ArticleFetchException("Failed to fetch article: " + e.getMessage(), e);
        }

        String title = extractTitle(doc);
        String text = extractText(doc);

        if (text.length() < MIN_CONTENT_LENGTH) {
            throw new ArticleExtractionException(
                    "Extracted content too short (%d chars). This URL may not contain an article."
                            .formatted(text.length()));
        }

        log.info("Fetched article '{}' from {} ({} chars)", title, url, text.length());
        return new ArticleContent(title, text);
    }

    private String extractFilenameFromUrl(String url) {
        String path = URI.create(url).getPath();
        if (path == null || path.isBlank()) return "Untitled";
        String[] parts = path.split("/");
        String filename = parts[parts.length - 1];
        if (filename.toLowerCase().endsWith(".pdf")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        return filename.replace('_', ' ').replace('-', ' ').strip();
    }

    private String extractTitle(Document doc) {
        // Try HTML-based title extraction first
        Element h1 = doc.selectFirst("article h1, main h1, h1");
        if (h1 != null && !h1.text().isBlank()) {
            return h1.text().strip();
        }

        // Try __NEXT_DATA__ for title
        Element nextDataScript = doc.selectFirst("script#__NEXT_DATA__");
        if (nextDataScript != null) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(nextDataScript.data());
                JsonNode pageProps = root.path("props").path("pageProps");
                for (String field : new String[]{"title", "name", "heading"}) {
                    String t = findTextField(pageProps, field);
                    if (t != null && !t.isBlank()) {
                        return t.strip();
                    }
                }
                // Check frontmatter/meta (Next.js uses both casings)
                for (String fmKey : new String[]{"frontmatter", "frontMatter", "meta"}) {
                    JsonNode frontmatter = pageProps.path(fmKey);
                    if (!frontmatter.isMissingNode()) {
                        String t = frontmatter.path("title").asText(null);
                        if (t != null && !t.isBlank()) return t.strip();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract title from __NEXT_DATA__: {}", e.getMessage());
            }
        }

        String title = doc.title();
        return (title != null && !title.isBlank()) ? title.strip() : "Untitled";
    }

    private String extractText(Document doc) {
        // Try Next.js __NEXT_DATA__ extraction first (handles SSG/SSR pages where
        // content is embedded as JSON and rendered client-side via JavaScript).
        String nextDataText = extractFromNextData(doc);
        if (nextDataText != null && nextDataText.length() >= MIN_CONTENT_LENGTH) {
            log.debug("Extracted content from __NEXT_DATA__ ({} chars)", nextDataText.length());
            return nextDataText;
        }

        // Try React streaming SSR extraction (React Router / Remix).
        // These frameworks place content in <div hidden id="S:0"> elements that
        // get swapped into the DOM by client-side JavaScript. Without JS execution,
        // the visible <article>/<main> tags are empty shells.
        String streamingText = extractFromStreamingSSR(doc);
        if (streamingText != null && streamingText.length() >= MIN_CONTENT_LENGTH) {
            log.debug("Extracted content from React streaming SSR ({} chars)", streamingText.length());
            return streamingText;
        }

        // Remove non-content elements
        doc.select("script, style, nav, footer, header, aside, iframe, form, .ad, .ads, .advertisement, .sidebar, .menu, .navigation").remove();

        // Try to find the main article content
        Element content = doc.selectFirst("article");
        if (content == null) content = doc.selectFirst("main");
        if (content == null) content = doc.selectFirst("[role=main]");
        if (content == null) content = doc.body();

        if (content == null) {
            return "";
        }

        return content.text().strip().replaceAll("\\s+", " ");
    }

    /**
     * Extracts article content from React streaming SSR hidden divs.
     * React Router v7, Remix, and similar frameworks use a pattern where page content
     * is placed in {@code <div hidden id="S:0">}, {@code <div hidden id="S:1">}, etc.
     * Client-side JavaScript then swaps this content into the visible DOM.
     * JSoup can't execute JS, so we extract directly from these hidden divs.
     */
    private String extractFromStreamingSSR(Document doc) {
        // Select hidden divs with React streaming IDs (S:0, S:1, etc.)
        Elements streamingDivs = doc.select("div[hidden][id~=^S:\\d+$]");
        if (streamingDivs.isEmpty()) {
            return null;
        }

        // Collect all text from streaming divs, clean up noise elements within them
        StringBuilder sb = new StringBuilder();
        for (Element div : streamingDivs) {
            // Clone to avoid mutating the original document
            Element clone = div.clone();
            clone.select("nav, footer, header, aside, .sidebar, .menu, .navigation").remove();
            String text = clone.text().strip();
            if (!text.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(text);
            }
        }

        String result = sb.toString().replaceAll("\\s+", " ").strip();
        return result.isEmpty() ? null : result;
    }

    /**
     * Extracts article content from Next.js __NEXT_DATA__ script tag.
     * Many modern blogs (Next.js, Gatsby) embed page content as JSON in this tag,
     * rendering it client-side. JSoup can't execute JavaScript, so we parse the JSON directly.
     */
    private String extractFromNextData(Document doc) {
        Element nextDataScript = doc.selectFirst("script#__NEXT_DATA__");
        if (nextDataScript == null) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(nextDataScript.data());
            JsonNode pageProps = root.path("props").path("pageProps");

            // Strategy 1: MDX compiled source (common in Next.js MDX blogs)
            String compiledSource = pageProps.path("mdxSource").path("compiledSource").asText(null);
            if (compiledSource == null) {
                // Also check common alternative paths
                compiledSource = pageProps.path("source").path("compiledSource").asText(null);
            }
            if (compiledSource != null && !compiledSource.isBlank()) {
                String text = extractTextFromCompiledMdx(compiledSource);
                if (text.length() >= MIN_CONTENT_LENGTH) {
                    return text;
                }
            }

            // Strategy 2: Raw markdown/content field
            for (String field : new String[]{"content", "markdown", "body", "rawContent", "article"}) {
                String raw = findTextField(pageProps, field);
                if (raw != null && raw.length() >= MIN_CONTENT_LENGTH) {
                    // Strip markdown syntax for a cleaner text extraction
                    return stripMarkdown(raw);
                }
            }

            // Strategy 3: Rendered HTML content in pageProps
            for (String field : new String[]{"contentHtml", "html", "renderedContent"}) {
                String html = findTextField(pageProps, field);
                if (html != null && !html.isBlank()) {
                    String text = Jsoup.parse(html).text().strip().replaceAll("\\s+", " ");
                    if (text.length() >= MIN_CONTENT_LENGTH) {
                        return text;
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Failed to parse __NEXT_DATA__: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extracts readable text from compiled MDX source by pulling out string literals
     * that represent text content (children props in JSX).
     */
    private String extractTextFromCompiledMdx(String compiledSource) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = MDX_STRING_CONTENT.matcher(compiledSource);

        while (matcher.find()) {
            String value = matcher.group(1);
            // Skip very short values (single chars, punctuation, JSX artifacts)
            if (value.length() <= 1 || value.startsWith("_") || value.startsWith("{")) {
                continue;
            }
            // Unescape basic escape sequences
            value = value.replace("\\n", " ")
                         .replace("\\t", " ")
                         .replace("\\\"", "\"")
                         .replace("\\\\", "\\");
            if (!value.isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(value.strip());
            }
        }

        return sb.toString().replaceAll("\\s+", " ").strip();
    }

    /**
     * Recursively searches a JSON tree for a text field by name.
     */
    private String findTextField(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) return null;

        JsonNode direct = node.get(fieldName);
        if (direct != null && direct.isTextual()) {
            return direct.asText();
        }

        // Search one level deeper
        for (JsonNode child : node) {
            if (child.isObject()) {
                JsonNode found = child.get(fieldName);
                if (found != null && found.isTextual()) {
                    return found.asText();
                }
            }
        }

        return null;
    }

    /**
     * Basic markdown stripping for raw markdown content.
     */
    private String stripMarkdown(String md) {
        return md
                .replaceAll("```[\\s\\S]*?```", " ")   // code blocks
                .replaceAll("`[^`]+`", " ")             // inline code
                .replaceAll("!?\\[[^]]*]\\([^)]*\\)", " ") // links/images
                .replaceAll("^#{1,6}\\s+", "")          // headings
                .replaceAll("\\*{1,3}|_{1,3}", "")      // bold/italic
                .replaceAll("^[>\\-*+]\\s+", "")        // blockquotes/lists
                .replaceAll("\\s+", " ")
                .strip();
    }

    public record ArticleContent(String title, String text) {}
}
