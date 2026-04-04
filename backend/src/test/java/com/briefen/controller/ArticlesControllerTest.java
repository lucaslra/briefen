package com.briefen.controller;

import com.briefen.model.Summary;
import com.briefen.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ArticlesControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldReturn202ForValidUrl() throws Exception {
        Summary summary = buildSummary("https://example.com/article");
        when(summaryService.summarize(eq("https://example.com/article"), eq(false), isNull(), isNull()))
                .thenReturn(summary);

        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "url": "https://example.com/article" }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturn400WhenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "url": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUrlIsMissing() throws Exception {
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Summary buildSummary(String url) {
        Summary s = new Summary();
        s.setUrl(url);
        s.setTitle("Test Article");
        s.setSummary("A short summary.");
        s.setModelUsed("gemma3:4b");
        s.setCreatedAt(Instant.now());
        return s;
    }
}
