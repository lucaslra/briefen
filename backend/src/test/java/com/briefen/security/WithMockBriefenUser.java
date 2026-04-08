package com.briefen.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation that populates the SecurityContext with a {@link BriefenUserDetails} principal.
 * Use on test classes or methods that need an authenticated user.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockBriefenUserSecurityContextFactory.class)
public @interface WithMockBriefenUser {

    String userId() default "test-user-id";

    String username() default "admin";

    String role() default "ADMIN";
}
