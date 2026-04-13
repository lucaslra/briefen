package com.briefen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

class TagExtractorTest {

    @Test
    void shouldExtractTagsFromTrailingLine() {
        String input = "# Summary Title\n\nSome summary content.\n\nTags: artificial intelligence, healthcare, regulation";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.summary()).isEqualTo("# Summary Title\n\nSome summary content.");
        assertThat(result.tags()).containsExactly("artificial intelligence", "healthcare", "regulation");
    }

    @Test
    void shouldHandleCaseInsensitiveTagsPrefix() {
        String input = "Summary content.\n\ntags: python, web development";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.summary()).isEqualTo("Summary content.");
        assertThat(result.tags()).containsExactly("python", "web development");
    }

    @Test
    void shouldStripHashtags() {
        String input = "Summary content.\n\nTags: #ai, #machine-learning, healthcare";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.tags()).containsExactly("ai", "machine-learning", "healthcare");
    }

    @Test
    void shouldReturnEmptyTagsWhenNoTagsLine() {
        String input = "# Title\n\nJust a summary without tags.";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.summary()).isEqualTo(input);
        assertThat(result.tags()).isEmpty();
    }

    @Test
    void shouldDeduplicateTags() {
        String input = "Summary.\n\nTags: ai, AI, machine learning";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.tags()).containsExactly("ai", "machine learning");
    }

    @Test
    void shouldHandleTrailingWhitespace() {
        String input = "Summary.\n\nTags: tag1, tag2   \n  ";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.tags()).containsExactly("tag1", "tag2");
        assertThat(result.summary()).isEqualTo("Summary.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldHandleNullAndEmptyInput(String input) {
        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.summary()).isEqualTo(input);
        assertThat(result.tags()).isEmpty();
    }

    @Test
    void shouldFilterEmptyTags() {
        String input = "Summary.\n\nTags: valid, , ,another";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.tags()).containsExactly("valid", "another");
    }

    @Test
    void shouldLowercaseTags() {
        String input = "Summary.\n\nTags: Machine Learning, CLOUD COMPUTING, DevOps";

        TagExtractor.Result result = TagExtractor.extract(input);

        assertThat(result.tags()).containsExactly("machine learning", "cloud computing", "devops");
    }
}
