package com.briefen.persistence.sqlite;

import com.briefen.model.Summary;
import com.briefen.persistence.SummaryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SqliteSummaryPersistenceTest {

    @Autowired
    private SummaryPersistence persistence;

    @BeforeEach
    void setUp() {
        // Each test gets a fresh context via @DirtiesContext
    }

    @Test
    void shouldSaveAndFindSummaryByUrl() {
        // Arrange
        Summary summary = buildSummary("https://example.com/test", "Test Title", "Test summary body", false);

        // Act
        persistence.save(summary);
        Optional<Summary> found = persistence.findByUrl("https://example.com/test");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Title");
        assertThat(found.get().getSummary()).isEqualTo("Test summary body");
    }

    @Test
    void shouldReturnEmptyWhenUrlNotFound() {
        // Act
        Optional<Summary> found = persistence.findByUrl("https://nonexistent.example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPageSummariesByCreatedAtDesc() {
        // Arrange — save 3 summaries with distinct timestamps
        Summary oldest = buildSummary("https://example.com/oldest", "Oldest", "Oldest summary", false);
        oldest.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        Summary middle = buildSummary("https://example.com/middle", "Middle", "Middle summary", false);
        middle.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));

        Summary newest = buildSummary("https://example.com/newest", "Newest", "Newest summary", false);
        newest.setCreatedAt(Instant.now());

        persistence.save(oldest);
        persistence.save(middle);
        persistence.save(newest);

        // Act
        Page<Summary> page = persistence.findAll(0, 10);

        // Assert — newest first
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Newest");
        assertThat(page.getContent().get(2).getTitle()).isEqualTo("Oldest");
    }

    @Test
    void shouldFilterByUnreadStatus() {
        // Arrange
        Summary unread = buildSummary("https://example.com/unread", "Unread Article", "Content", false);
        Summary read1 = buildSummary("https://example.com/read1", "Read Article 1", "Content", true);
        Summary read2 = buildSummary("https://example.com/read2", "Read Article 2", "Content", true);

        persistence.save(unread);
        persistence.save(read1);
        persistence.save(read2);

        // Act
        Page<Summary> page = persistence.findAll(0, 10, "unread", null);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Unread Article");
    }

    @Test
    void shouldFilterByReadStatus() {
        // Arrange
        Summary unread = buildSummary("https://example.com/unread", "Unread Article", "Content", false);
        Summary read1 = buildSummary("https://example.com/read1", "Read Article 1", "Content", true);
        Summary read2 = buildSummary("https://example.com/read2", "Read Article 2", "Content", true);

        persistence.save(unread);
        persistence.save(read1);
        persistence.save(read2);

        // Act
        Page<Summary> page = persistence.findAll(0, 10, "read", null);

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(s -> Boolean.TRUE.equals(s.getIsRead()));
    }

    @Test
    void shouldSearchByTitleCaseInsensitive() {
        // Arrange
        Summary javaArticle = buildSummary("https://example.com/java", "Java Programming Guide", "Content about Java", false);
        Summary otherArticle = buildSummary("https://example.com/other", "Something Else", "Unrelated content", false);

        persistence.save(javaArticle);
        persistence.save(otherArticle);

        // Act — search with lowercase; title has "Java" (capitalized)
        Page<Summary> page = persistence.findAll(0, 10, "all", "java");

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Java Programming Guide");
    }

    @Test
    void shouldSearchBySummaryContent() {
        // Arrange
        Summary matching = buildSummary("https://example.com/spring", "Spring Boot", "Spring Boot makes building REST APIs easy.", false);
        Summary nonMatching = buildSummary("https://example.com/other", "Other Topic", "Completely different subject matter.", false);

        persistence.save(matching);
        persistence.save(nonMatching);

        // Act
        Page<Summary> page = persistence.findAll(0, 10, "all", "REST APIs");

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUrl()).isEqualTo("https://example.com/spring");
    }

    @Test
    void shouldMarkAllAsRead() {
        // Arrange
        persistence.save(buildSummary("https://example.com/a", "A", "Content", false));
        persistence.save(buildSummary("https://example.com/b", "B", "Content", false));
        persistence.save(buildSummary("https://example.com/c", "C", "Content", false));

        // Act
        long updated = persistence.markAllAsRead();

        // Assert
        assertThat(updated).isEqualTo(3);
        Page<Summary> unread = persistence.findAll(0, 10, "unread", null);
        assertThat(unread.getContent()).isEmpty();
    }

    @Test
    void shouldMarkAllAsUnread() {
        // Arrange
        persistence.save(buildSummary("https://example.com/a", "A", "Content", true));
        persistence.save(buildSummary("https://example.com/b", "B", "Content", true));
        persistence.save(buildSummary("https://example.com/c", "C", "Content", true));

        // Act
        long updated = persistence.markAllAsUnread();

        // Assert
        assertThat(updated).isEqualTo(3);
        Page<Summary> read = persistence.findAll(0, 10, "read", null);
        assertThat(read.getContent()).isEmpty();
    }

    @Test
    void shouldCountUnread() {
        // Arrange
        persistence.save(buildSummary("https://example.com/unread1", "Unread 1", "Content", false));
        persistence.save(buildSummary("https://example.com/unread2", "Unread 2", "Content", false));
        persistence.save(buildSummary("https://example.com/read1", "Read 1", "Content", true));

        // Act
        long count = persistence.countUnread();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldDeleteById() {
        // Arrange
        Summary saved = persistence.save(buildSummary("https://example.com/to-delete", "To Delete", "Content", false));
        String id = saved.getId();

        // Act
        persistence.deleteById(id);

        // Assert
        assertThat(persistence.findById(id)).isEmpty();
    }

    @Test
    void shouldUpsertOnDuplicateUrl() {
        // Arrange
        Summary first = buildSummary("https://example.com/upsert", "Original Title", "Original summary", false);
        Summary saved = persistence.save(first);

        // Update the saved entity with new values
        saved.setTitle("Updated Title");
        saved.setSummary("Updated summary content");

        // Act — save again with same URL but updated fields
        Summary updated = persistence.save(saved);

        // Assert — only one record exists, with updated content
        List<Summary> all = persistence.findAll("all", null);
        assertThat(all).hasSize(1);
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getSummary()).isEqualTo("Updated summary content");
    }

    @Test
    void shouldReturnTrueForExistingId() {
        // Arrange
        Summary saved = persistence.save(buildSummary("https://example.com/exists", "Exists", "Content", false));

        // Act & Assert
        assertThat(persistence.existsById(saved.getId())).isTrue();
    }

    @Test
    void shouldReturnFalseForNonExistingId() {
        // Act & Assert
        assertThat(persistence.existsById("non-existing-id")).isFalse();
    }

    // ---- Helpers ----

    private Summary buildSummary(String url, String title, String summaryText, boolean isRead) {
        Summary s = new Summary();
        s.setUrl(url);
        s.setTitle(title);
        s.setSummary(summaryText);
        s.setModelUsed("gemma3:4b");
        s.setCreatedAt(Instant.now());
        s.setIsRead(isRead);
        s.setSavedAt(Instant.now());
        return s;
    }
}
