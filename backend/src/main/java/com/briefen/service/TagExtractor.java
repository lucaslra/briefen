package com.briefen.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts auto-generated tags from LLM summary output.
 *
 * The built-in prompt asks the LLM to append a "Tags: tag1, tag2, tag3" line
 * at the end of the response. This class parses that line and returns both
 * the cleaned summary text and the extracted tags.
 */
final class TagExtractor {

    private TagExtractor() {}

    /**
     * Pattern matches a trailing "Tags:" line (case-insensitive) at the end of text,
     * possibly preceded by blank lines.
     */
    private static final Pattern TAGS_LINE = Pattern.compile(
            "\\n\\s*[Tt]ags:\\s*(.+?)\\s*$"
    );

    record Result(String summary, List<String> tags) {}

    /**
     * Extracts tags from the end of the LLM summary and returns the cleaned text.
     * Returns empty tags list if no Tags line is found.
     */
    static Result extract(String summaryText) {
        if (summaryText == null || summaryText.isBlank()) {
            return new Result(summaryText, List.of());
        }

        Matcher matcher = TAGS_LINE.matcher(summaryText);
        if (!matcher.find()) {
            return new Result(summaryText, List.of());
        }

        String tagsRaw = matcher.group(1);
        String cleanedSummary = summaryText.substring(0, matcher.start()).stripTrailing();

        List<String> tags = java.util.Arrays.stream(tagsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .map(t -> t.startsWith("#") ? t.substring(1) : t)  // strip accidental hashtags
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();

        return new Result(cleanedSummary, tags);
    }
}
