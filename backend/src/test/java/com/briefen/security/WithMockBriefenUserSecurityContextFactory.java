package com.briefen.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockBriefenUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockBriefenUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockBriefenUser annotation) {
        var authority = new SimpleGrantedAuthority("ROLE_" + annotation.role());
        var principal = new BriefenUserDetails(
                annotation.userId(),
                annotation.username(),
                "{noop}password",
                List.of(authority)
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        return ctx;
    }
}
