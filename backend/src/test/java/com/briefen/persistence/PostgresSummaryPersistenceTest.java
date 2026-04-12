package com.briefen.persistence;

import com.briefen.model.Summary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("testpostgres")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PostgresSummaryPersistenceTest {

    private static final String USER_ID = "persistence-test-user";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("briefen_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("briefen.db.type", () -> "postgres");
        registry.add("BRIEFEN_DATASOURCE_URL", postgres::getJdbcUrl);
        registry.add("BRIEFEN_DATASOURCE_USERNAME", postgres::getUsername);
        registry.add("BRIEFEN_DATASOURCE_PASSWORD", postgres::getPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private SummaryPersistence persistence;

    @Test
    void shouldSaveAndFindSummaryByUrl() {
        Summary summary = buildSummary("https://example.com/test", "Test Title", "Test summary body", false);

        persistence.save(summary);
        Optional<Summary> found = persistence.findByUrl(USER_ID, "https://example.com/test");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Title");
        assertThat(found.get().getSummary()).isEqualTo("Test summary body");
    }

    @Test
    void shouldReturnEmptyWhenUrlNotFound() {
        Optional<Summary> found = persistence.findByUrl(USER_ID, "https://nonexistent.example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPageSummariesByCreatedAtDesc() {
        Summary oldest = buildSummary("https://example.com/oldest", "Oldest", "Oldest summary", false);
        oldest.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        Summary middle = buildSummary("https://example.com/middle", "Middle", "Middle summary", false);
        middle.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));

        Summary newest = buildSummary("https://example.com/newest", "Newest", "Newest summary", false);
        newest.setCreatedAt(Instant.now());

        persistence.save(oldest);
        persistence.save(middle);
        persistence.save(newest);

        Page<Summary> page = persistence.findAll(USER_ID, 0, 10);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Newest");
        assertThat(page.getContent().get(2).getTitle()).isEqualTo("Oldest");
    }

    @Test
    void shouldFilterByUnreadStatus() {
        persistence.save(buildSummary("https://example.com/unread", "Unread Article", "Content", false));
        persistence.save(buildSummary("https://example.com/read1", "Read Article 1", "Content", true));
        persistence.save(buildSummary("https://example.com/read2", "Read Article 2", "Content", true));

        Page<Summary> page = persistence.findAll(USER_ID, 0, 10, "unread", null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Unread Article");
    }

    @Test
    void shouldFilterByReadStatus() {
        persistence.save(buildSummary("https://example.com/unread", "Unread Article", "Content", false));
        persistence.save(buildSummary("https://example.com/read1", "Read Article 1", "Content", true));
        persistence.save(buildSummary("https://example.com/read2", "Read Article 2", "Content", true));

        Page<Summary> page = persistence.findAll(USER_ID, 0, 10, "read", null);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(s -> Boolean.TRUE.equals(s.getIsRead()));
    }

    @Test
    void shouldSearchByTitleCaseInsensitive() {
        persistence.save(buildSummary("https://example.com/java", "Java Programming Guide", "Content about Java", false));
        persistence.save(buildSummary("https://example.com/other", "Something Else", "Unrelated content", false));

        Page<Summary> page = persistence.findAll(USER_ID, 0, 10, "all", "java");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Java Programming Guide");
    }

    @Test
    void shouldMarkAllAsRead() {
        persistence.save(buildSummary("https://example.com/a", "A", "Content", false));
        persistence.save(buildSummary("https://example.com/b", "B", "Content", false));
        persistence.save(buildSummary("https://example.com/c", "C", "Content", false));

        long updated = persistence.markAllAsRead(USER_ID);

        assertThat(updated).isEqualTo(3);
        Page<Summary> unread = persistence.findAll(USER_ID, 0, 10, "unread", null);
        assertThat(unread.getContent()).isEmpty();
    }

    @Test
    void shouldMarkAllAsUnread() {
        persistence.save(buildSummary("https://example.com/a", "A", "Content", true));
        persistence.save(buildSummary("https://example.com/b", "B", "Content", true));
        persistence.save(buildSummary("https://example.com/c", "C", "Content", true));

        long updated = persistence.markAllAsUnread(USER_ID);

        assertThat(updated).isEqualTo(3);
        Page<Summary> read = persistence.findAll(USER_ID, 0, 10, "read", null);
        assertThat(read.getContent()).isEmpty();
    }

    @Test
    void shouldCountUnread() {
        persistence.save(buildSummary("https://example.com/unread1", "Unread 1", "Content", false));
        persistence.save(buildSummary("https://example.com/unread2", "Unread 2", "Content", false));
        persistence.save(buildSummary("https://example.com/read1", "Read 1", "Content", true));

        long count = persistence.countUnread(USER_ID);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldDeleteById() {
        Summary saved = persistence.save(buildSummary("https://example.com/to-delete", "To Delete", "Content", false));
        String id = saved.getId();

        persistence.deleteById(USER_ID, id);

        assertThat(persistence.findById(USER_ID, id)).isEmpty();
    }

    @Test
    void shouldIsolateSummariesBetweenUsers() {
        persistence.save(buildSummary("https://example.com/mine", "Mine", "Content", false));

        Summary other = buildSummary("https://example.com/mine", "Theirs", "Content", false);
        other.setUserId("other-user-id");
        persistence.save(other);

        List<Summary> mine = persistence.findAll(USER_ID, "all", null);
        List<Summary> theirs = persistence.findAll("other-user-id", "all", null);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getTitle()).isEqualTo("Mine");
        assertThat(theirs).hasSize(1);
        assertThat(theirs.get(0).getTitle()).isEqualTo("Theirs");
    }

    private Summary buildSummary(String url, String title, String summaryText, boolean isRead) {
        Summary s = new Summary();
        s.setUserId(USER_ID);
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
