package com.briefen.service;

import com.briefen.config.OidcProperties;
import com.briefen.exception.AuthReasonException;
import com.briefen.model.User;
import com.briefen.persistence.UserPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Maps verified OIDC ID-token claims to a Briefen {@link User}.
 *
 * <p>Resolution priority (ported from the reference implementation):
 * <ol>
 *   <li>An account already linked to this {@code (issuer, subject)} pair.</li>
 *   <li>Link to an existing local account by <b>verified</b> email.</li>
 *   <li>Link to an existing local account by username.</li>
 *   <li>Just-in-time provisioning of a new SSO-only account.</li>
 * </ol>
 *
 * <p>When {@code adminGroup} is configured, admin status is synced from group
 * membership on every login. The first-ever user is always bootstrapped as admin.
 */
@Service
public class OidcUserResolver {

    private static final Logger log = LoggerFactory.getLogger(OidcUserResolver.class);

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final int MAX_USERNAME_LENGTH = 64;

    private final OidcProperties props;
    private final UserPersistence users;

    public OidcUserResolver(OidcProperties props, UserPersistence users) {
        this.props = props;
        this.users = users;
    }

    /** Normalized identity extracted from a verified ID token. */
    public record Claims(
            String issuer,
            String subject,
            String username,
            String email,
            boolean emailVerified,
            List<String> groups
    ) {}

    @Transactional
    public User resolve(Claims claims) {
        if (claims.subject() == null || claims.subject().isBlank()) {
            throw new AuthReasonException("missing_subject");
        }
        boolean inAdminGroup = props.getAdminGroup() != null && !props.getAdminGroup().isBlank()
                && claims.groups() != null && claims.groups().contains(props.getAdminGroup());

        // 1. Already linked to this provider identity.
        var linked = users.findByOidc(claims.issuer(), claims.subject());
        if (linked.isPresent()) {
            User u = linked.get();
            applyAdminSync(u, inAdminGroup);
            return u;
        }

        // 2a. Link by verified email.
        if (props.isLinkByEmail() && claims.email() != null && !claims.email().isBlank() && claims.emailVerified()) {
            var byEmail = users.findByEmail(claims.email());
            if (byEmail.isPresent() && !byEmail.get().isOidcLinked()) {
                return link(byEmail.get(), claims, inAdminGroup);
            }
        }

        // 2b. Link by username.
        if (props.isLinkByUsername() && claims.username() != null && !claims.username().isBlank()) {
            var byUsername = users.findByUsername(claims.username());
            if (byUsername.isPresent() && !byUsername.get().isOidcLinked()) {
                return link(byUsername.get(), claims, inAdminGroup);
            }
        }

        // 3. Just-in-time provisioning.
        if (!props.isAllowSignup()) {
            throw new AuthReasonException("signup_disabled");
        }
        boolean firstUser = users.count() == 0;
        String username = uniqueUsername(claims.username() != null ? claims.username() : claims.email());
        String role = (inAdminGroup || firstUser) ? ROLE_ADMIN : ROLE_USER;

        Instant now = Instant.now();
        var user = new User(UUID.randomUUID().toString(), username, "", role);
        user.setEmail(blankToNull(claims.email()));
        user.setOidcIssuer(claims.issuer());
        user.setOidcSubject(claims.subject());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = users.save(user);
        log.info("Provisioned new SSO user '{}' (role={})", username, role);
        return saved;
    }

    private User link(User existing, Claims claims, boolean inAdminGroup) {
        existing.setOidcIssuer(claims.issuer());
        existing.setOidcSubject(claims.subject());
        if (existing.getEmail() == null || existing.getEmail().isBlank()) {
            existing.setEmail(blankToNull(claims.email()));
        }
        existing.setUpdatedAt(Instant.now());
        User saved = users.save(existing);
        log.info("Linked SSO identity to existing user '{}'", saved.getUsername());
        applyAdminSync(saved, inAdminGroup);
        return saved;
    }

    /**
     * Syncs the user's role from group membership, but only when an admin group
     * is configured. Without {@code adminGroup} the role is left untouched (so a
     * manually promoted admin stays admin).
     */
    private void applyAdminSync(User user, boolean inAdminGroup) {
        if (props.getAdminGroup() == null || props.getAdminGroup().isBlank()) {
            return;
        }
        String desired = inAdminGroup ? ROLE_ADMIN : ROLE_USER;
        // Never demote the protected main admin via group sync.
        if (user.isMainAdmin()) {
            return;
        }
        if (!desired.equals(user.getRole())) {
            user.setRole(desired);
            user.setUpdatedAt(Instant.now());
            users.save(user);
            log.info("Synced role for SSO user '{}' to {} from group membership", user.getUsername(), desired);
        }
    }

    /**
     * Derives a username that does not collide with an existing account,
     * appending a numeric suffix if needed (never links to the existing account).
     */
    private String uniqueUsername(String base) {
        String sanitized = sanitizeUsername(base);
        if (sanitized.isBlank()) {
            sanitized = "user";
        }
        if (users.findByUsername(sanitized).isEmpty()) {
            return sanitized;
        }
        for (int i = 2; i < 10000; i++) {
            String candidate = sanitized + "-" + i;
            if (users.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new AuthReasonException("server_error");
    }

    private static String sanitizeUsername(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_USERNAME_LENGTH);
        }
        return trimmed;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
