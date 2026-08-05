package com.briefen.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches only when {@code briefen.oidc.issuer} is set to a non-blank value.
 *
 * <p>{@code @ConditionalOnProperty} is unsuitable here because the property is
 * bound from {@code ${BRIEFEN_OIDC_ISSUER:}}, which resolves to an empty string
 * when the env var is unset — and an empty string counts as "present", which would
 * wrongly activate SSO. This condition treats blank as disabled.
 */
public class OidcEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String issuer = context.getEnvironment().getProperty("briefen.oidc.issuer");
        return issuer != null && !issuer.isBlank();
    }
}
