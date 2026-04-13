package com.briefen.service;

/**
 * Centralises summarization prompt construction for all LLM providers.
 * <p>
 * Resolution order (first non-blank wins):
 * <ol>
 *   <li>User's personal custom prompt (from Settings → Summarization)</li>
 *   <li>Deployment-wide default prompt ({@code BRIEFEN_DEFAULT_PROMPT} env var)</li>
 *   <li>Built-in English prompt</li>
 * </ol>
 * <p>
 * When the built-in prompt is used, the {@code %s} placeholder is replaced with
 * a length guideline matching the requested length hint. Custom prompts are used
 * as-is (no placeholder substitution) — users have full control over the text.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    static final String BUILT_IN_PROMPT = """
            You are a skilled article summarizer. Produce a clear and faithful summary of the article provided.

            Guidelines:
            - %s
            - Start with a brief introduction stating the article's main topic.
            - Cover the key points, arguments, and findings.
            - End with the conclusion or main takeaway if the article has one.
            - Use clear, accessible English and markdown formatting.
            - Do NOT add information that is not present in the article.
            - Do NOT include your own opinions or commentary.
            - Do NOT include inline parenthetical citations — keep the summary clean.
            - At the end, include a "Key Quotes" section with 2-4 direct, verbatim short quotes from the article that support the most important claims.
            - Each quote must be in quotation marks, attributed with brief context such as the section or speaker if known.
            - Do NOT invent dates, URLs, or any metadata not present in the article.
            - Begin the response with a single markdown H1 heading (# Title) that captures the article's topic. Use the article's own title if available, or create a concise, descriptive one.
            - As the very last line of your response, include exactly one line starting with "Tags:" followed by 2 to 4 short, lowercase topic tags separated by commas. These should capture the article's main themes (e.g., "Tags: artificial intelligence, healthcare, regulation"). Do not use hashtags.""";

    static final String LENGTH_DEFAULT = "Write 3 to 6 concise paragraphs depending on the article's length and complexity.";
    static final String LENGTH_SHORTER = "Write 1 to 2 short, concise paragraphs capturing only the most essential points.";
    static final String LENGTH_LONGER = "Write 6 to 10 detailed paragraphs providing thorough coverage of all points, arguments, and nuances.";

    /**
     * Builds the system prompt for a summarization request.
     *
     * @param userCustomPrompt  the user's custom prompt from settings (nullable)
     * @param deploymentDefault the deployment-wide default prompt from env var (nullable)
     * @param lengthHint        "shorter", "longer", or null/blank for default
     * @return the resolved system prompt ready to send to the LLM
     */
    public static String build(String userCustomPrompt, String deploymentDefault, String lengthHint) {
        // User's custom prompt takes highest priority
        if (userCustomPrompt != null && !userCustomPrompt.isBlank()) {
            return userCustomPrompt;
        }

        // Deployment-wide default from BRIEFEN_DEFAULT_PROMPT env var
        if (deploymentDefault != null && !deploymentDefault.isBlank()) {
            return deploymentDefault;
        }

        // Built-in prompt with length guideline
        String lengthGuideline = switch (lengthHint != null ? lengthHint.toLowerCase() : "") {
            case "shorter" -> LENGTH_SHORTER;
            case "longer" -> LENGTH_LONGER;
            default -> LENGTH_DEFAULT;
        };

        return BUILT_IN_PROMPT.formatted(lengthGuideline);
    }

    /**
     * Returns the built-in default prompt (for display in the UI "Reset to default" flow).
     */
    public static String getBuiltInPrompt() {
        return BUILT_IN_PROMPT.formatted(LENGTH_DEFAULT);
    }
}
