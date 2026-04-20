package com.briefen.persistence.jpa;

import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class JpaUserPersistenceTest {

    @Autowired
    private UserPersistence persistence;

    private User buildUser(String username, String role) {
        return new User(UUID.randomUUID().toString(), username, "$2a$hashed", role);
    }

    @Test
    void shouldReturnEmptyWhenUserNotFoundById() {
        Optional<User> result = persistence.findById("nonexistent-id");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveAndFindUserById() {
        User user = buildUser("alice", "USER");
        persistence.save(user);

        Optional<User> found = persistence.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
        assertThat(found.get().getRole()).isEqualTo("USER");
    }

    @Test
    void shouldFindUserByUsername() {
        User user = buildUser("bob", "ADMIN");
        persistence.save(user);

        Optional<User> found = persistence.findByUsername("bob");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        Optional<User> result = persistence.findByUsername("ghost");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindAllUsers() {
        persistence.save(buildUser("user1", "USER"));
        persistence.save(buildUser("user2", "ADMIN"));

        List<User> all = persistence.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(User::getUsername).containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void shouldDeleteUserById() {
        User user = buildUser("to-delete", "USER");
        persistence.save(user);
        assertThat(persistence.findById(user.getId())).isPresent();

        persistence.deleteById(user.getId());

        assertThat(persistence.findById(user.getId())).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenUsernameExists() {
        persistence.save(buildUser("existing", "USER"));
        assertThat(persistence.existsByUsername("existing")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUsernameDoesNotExist() {
        assertThat(persistence.existsByUsername("nobody")).isFalse();
    }

    @Test
    void shouldCountAllUsers() {
        persistence.save(buildUser("c1", "USER"));
        persistence.save(buildUser("c2", "USER"));
        persistence.save(buildUser("c3", "ADMIN"));

        assertThat(persistence.count()).isEqualTo(3);
    }

    @Test
    void shouldCountUsersByRole() {
        persistence.save(buildUser("u1", "USER"));
        persistence.save(buildUser("u2", "USER"));
        persistence.save(buildUser("a1", "ADMIN"));

        assertThat(persistence.countByRole("USER")).isEqualTo(2);
        assertThat(persistence.countByRole("ADMIN")).isEqualTo(1);
    }

    @Test
    void shouldFindUsersByRole() {
        persistence.save(buildUser("admin1", "ADMIN"));
        persistence.save(buildUser("admin2", "ADMIN"));
        persistence.save(buildUser("reg", "USER"));

        List<User> admins = persistence.findByRole("ADMIN");
        assertThat(admins).hasSize(2);
        assertThat(admins).extracting(User::getRole).containsOnly("ADMIN");
    }

    @Test
    void shouldPreservePasswordHashOnSave() {
        User user = buildUser("secureuser", "USER");
        persistence.save(user);

        User found = persistence.findById(user.getId()).orElseThrow();
        assertThat(found.getPasswordHash()).isEqualTo("$2a$hashed");
    }
}
