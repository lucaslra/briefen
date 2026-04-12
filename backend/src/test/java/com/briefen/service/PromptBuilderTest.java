package com.briefen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void shouldReturnBuiltInPromptWithDefaultLengthWhenNoCustomPrompts() {
        String result = PromptBuilder.build(null, null, null);

        assertThat(result)
                .contains("You are a skilled article summarizer")
                .contains(PromptBuilder.LENGTH_DEFAULT)
                .doesNotContain("%s");
    }

    @Test
    void shouldUseUserCustomPromptWhenProvided() {
        String customPrompt = "Summarize in French with bullet points.";

        String result = PromptBuilder.build(customPrompt, null, null);

        assertThat(result).isEqualTo(customPrompt);
    }

    @Test
    void shouldUseDeploymentDefaultWhenNoUserCustomPrompt() {
        String deploymentPrompt = "Enterprise prompt: summarize formally.";

        String result = PromptBuilder.build(null, deploymentPrompt, null);

        assertThat(result).isEqualTo(deploymentPrompt);
    }

    @Test
    void shouldPreferUserCustomPromptOverDeploymentDefault() {
        String customPrompt = "My custom prompt.";
        String deploymentPrompt = "Deployment default prompt.";

        String result = PromptBuilder.build(customPrompt, deploymentPrompt, null);

        assertThat(result).isEqualTo(customPrompt);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldIgnoreBlankUserCustomPromptAndFallThrough(String blankPrompt) {
        String deploymentPrompt = "Deployment prompt.";

        String result = PromptBuilder.build(blankPrompt, deploymentPrompt, null);

        assertThat(result).isEqualTo(deploymentPrompt);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldIgnoreBlankDeploymentPromptAndUseBuiltIn(String blankDeployment) {
        String result = PromptBuilder.build(null, blankDeployment, null);

        assertThat(result)
                .contains("You are a skilled article summarizer")
                .contains(PromptBuilder.LENGTH_DEFAULT);
    }

    @Test
    void shouldUseShorterLengthHint() {
        String result = PromptBuilder.build(null, null, "shorter");

        assertThat(result)
                .contains(PromptBuilder.LENGTH_SHORTER)
                .doesNotContain(PromptBuilder.LENGTH_DEFAULT)
                .doesNotContain(PromptBuilder.LENGTH_LONGER);
    }

    @Test
    void shouldUseLongerLengthHint() {
        String result = PromptBuilder.build(null, null, "longer");

        assertThat(result)
                .contains(PromptBuilder.LENGTH_LONGER)
                .doesNotContain(PromptBuilder.LENGTH_DEFAULT)
                .doesNotContain(PromptBuilder.LENGTH_SHORTER);
    }

    @Test
    void shouldHandleLengthHintCaseInsensitively() {
        String result = PromptBuilder.build(null, null, "SHORTER");

        assertThat(result).contains(PromptBuilder.LENGTH_SHORTER);
    }

    @Test
    void shouldDefaultLengthForUnknownHint() {
        String result = PromptBuilder.build(null, null, "medium");

        assertThat(result).contains(PromptBuilder.LENGTH_DEFAULT);
    }

    @Test
    void shouldNotSubstitutePlaceholdersInCustomPrompt() {
        String customPrompt = "Summarize with %s and %d placeholders.";

        String result = PromptBuilder.build(customPrompt, null, "shorter");

        // Custom prompts are used as-is — no placeholder substitution
        assertThat(result).isEqualTo(customPrompt);
    }

    @Test
    void shouldIgnoreLengthHintWhenCustomPromptIsProvided() {
        String customPrompt = "My custom summarizer prompt.";

        String resultShorter = PromptBuilder.build(customPrompt, null, "shorter");
        String resultLonger = PromptBuilder.build(customPrompt, null, "longer");

        assertThat(resultShorter).isEqualTo(customPrompt);
        assertThat(resultLonger).isEqualTo(customPrompt);
    }

    @Test
    void shouldReturnBuiltInPromptFromGetBuiltInPrompt() {
        String result = PromptBuilder.getBuiltInPrompt();

        assertThat(result)
                .contains("You are a skilled article summarizer")
                .contains(PromptBuilder.LENGTH_DEFAULT)
                .doesNotContain("%s");
    }

    @Test
    void shouldMatchGetBuiltInPromptWithDefaultBuild() {
        String fromBuild = PromptBuilder.build(null, null, null);
        String fromGetter = PromptBuilder.getBuiltInPrompt();

        assertThat(fromBuild).isEqualTo(fromGetter);
    }
}
