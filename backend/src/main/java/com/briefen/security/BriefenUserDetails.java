package com.briefen.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security principal that carries the Briefen user's database ID.
 * This avoids an extra DB lookup per request to resolve userId from username.
 */
public record BriefenUserDetails(
        String userId,
        String username,
        String passwordHash,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
