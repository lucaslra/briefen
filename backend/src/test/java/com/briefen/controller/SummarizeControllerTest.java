package com.briefen.controller;

import com.briefen.exception.ArticleFetchException;
import com.briefen.exception.InvalidUrlException;
import com.briefen.exception.SummarizationException;
import com.briefen.exception.SummaryNotFoundException;
import com.briefen.model.Summary;
import com.briefen.security.WithMockBriefenUser;
import com.briefen.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@WithMockBriefenUser
class SummarizeControllerTest {

    private static final String TEST_USER_ID = "test-user-id";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---- POST /api/summarize ----

    @Test
    void shouldReturn200WithSummaryForValidUrl() throws Exception {
        // Arrange
        Summary summary = buildSummary("abc123", "https://example.com/article");
        when(summaryService.summarize(eq(TEST_USER_ID), eq("https://example.com/article"), eq(false), isNull(), isNull()))
                .thenReturn(summary);

        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/article"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.url").value("https://example.com/article"))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void shouldReturn400WhenUrlAndTextAreAbsent() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400WhenUrlIsInvalid() throws Exception {
        // Arrange
        when(summaryService.summarize(anyString(), anyString(), anyBoolean(), any(), any()))
                .thenThrow(new InvalidUrlException("URL must use HTTP or HTTPS scheme."));

        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"ftp://bad.example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL must use HTTP or HTTPS scheme."));
    }

    @Test
    void shouldReturn502WhenArticleFetchFails() throws Exception {
        // Arrange
        when(summaryService.summarize(anyString(), anyString(), anyBoolean(), any(), any()))
                .thenThrow(new ArticleFetchException("Failed to fetch article: HTTP 404"));

        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/gone"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn504WhenSummarizationTimesOut() throws Exception {
        // Arrange
        when(summaryService.summarize(anyString(), anyString(), anyBoolean(), any(), any()))
                .thenThrow(new SummarizationException("Summarization timed out.", true));

        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/article"}
                                """))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn200ForTextSummarization() throws Exception {
        // Arrange
        Summary summary = buildSummary("text-id", null);
        when(summaryService.summarizeText(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(summary);

        // Act & Assert
        mockMvc.perform(post("/api/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"This is the article text content to summarize directly."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("text-id"));
    }

    // ---- GET /api/summaries ----

    @Test
    void shouldReturn200ForSummariesList() throws Exception {
        // Arrange
        List<Summary> items = List.of(buildSummary("id-1", "https://example.com/1"));
        Page<Summary> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);
        when(summaryService.getSummaries(eq(TEST_USER_ID), eq(0), eq(10), eq("all"), isNull()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("id-1"));
    }

    // ---- GET /api/summaries/unread-count ----

    @Test
    void shouldReturn200ForUnreadCount() throws Exception {
        // Arrange
        when(summaryService.getUnreadCount(TEST_USER_ID)).thenReturn(7L);

        // Act & Assert
        mockMvc.perform(get("/api/summaries/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    // ---- DELETE /api/summaries/{id} ----

    @Test
    void shouldReturn404ForUnknownSummaryId() throws Exception {
        // Arrange
        doThrow(new SummaryNotFoundException("unknown-id"))
                .when(summaryService).deleteSummary(TEST_USER_ID, "unknown-id");

        // Act & Assert
        mockMvc.perform(delete("/api/summaries/unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Summary not found: unknown-id"));
    }

    @Test
    void shouldReturn204ForSuccessfulDelete() throws Exception {
        // Arrange
        doNothing().when(summaryService).deleteSummary(TEST_USER_ID, "existing-id");

        // Act & Assert
        mockMvc.perform(delete("/api/summaries/existing-id"))
                .andExpect(status().isNoContent());
    }

    // ---- PATCH /api/summaries/{id}/read-status ----

    @Test
    void shouldReturn200ForReadStatusPatch() throws Exception {
        // Arrange
        Summary summary = buildSummary("id-1", "https://example.com/1");
        summary.setIsRead(true);
        when(summaryService.updateReadStatus(TEST_USER_ID, "id-1", true)).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(patch("/api/summaries/id-1/read-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRead\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void shouldReturn400WhenIsReadFieldMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/summaries/id-1/read-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH /api/summaries/read-status/bulk ----

    @Test
    void shouldReturn200ForBulkMarkAllRead() throws Exception {
        // Arrange
        when(summaryService.markAllAsRead(TEST_USER_ID)).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(patch("/api/summaries/read-status/bulk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(3));
    }

    // ---- PATCH /api/summaries/{id}/notes ----

    @Test
    void shouldReturn200ForNotesUpdate() throws Exception {
        // Arrange
        Summary summary = buildSummary("id-1", "https://example.com/1");
        summary.setNotes("My note");
        when(summaryService.updateNotes(TEST_USER_ID, "id-1", "My note")).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(patch("/api/summaries/id-1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"My note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("My note"));
    }

    // ---- Helpers ----

    private Summary buildSummary(String id, String url) {
        Summary s = new Summary();
        s.setId(id);
        s.setUserId(TEST_USER_ID);
        s.setUrl(url);
        s.setTitle("Test Title");
        s.setSummary("Test summary content.");
        s.setModelUsed("gemma3:4b");
        s.setCreatedAt(Instant.now());
        s.setIsRead(false);
        s.setSavedAt(Instant.now());
        return s;
    }
}
