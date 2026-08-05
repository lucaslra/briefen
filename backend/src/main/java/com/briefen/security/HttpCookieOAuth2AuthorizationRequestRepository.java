package com.briefen.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Stores the in-flight {@link OAuth2AuthorizationRequest} (state, PKCE verifier,
 * nonce, redirect) in a short-lived, HMAC-signed, {@code HttpOnly} cookie instead
 * of an {@code HttpSession}.
 *
 * <p>This keeps Briefen fully stateless across the OIDC handshake — the direct
 * analog of the reference implementation's signed state cookie. {@code SameSite=Lax}
 * is required so the cookie survives the top-level redirect back from the provider.
 *
 * <p>The cookie is only ever deserialized after its HMAC signature is verified with
 * the server secret, so its contents cannot be forged; a strict {@link ObjectInputFilter}
 * adds defense-in-depth.
 */
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class);

    static final String COOKIE_NAME = "briefen_oauth2_authz";
    private static final int COOKIE_MAX_AGE_SECONDS = 300; // 5 minutes — ample for the handshake
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String OIF_PATTERN =
            "org.springframework.security.oauth2.**;java.util.**;java.lang.**;java.net.**;!*";

    private final byte[] hmacSecret;
    private final boolean secureCookies;

    public HttpCookieOAuth2AuthorizationRequestRepository(byte[] hmacSecret, boolean secureCookies) {
        this.hmacSecret = hmacSecret.clone();
        this.secureCookies = secureCookies;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String value = readCookie(request);
        if (value == null) {
            return null;
        }
        return deserialize(value);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }
        String value = serialize(authorizationRequest);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .path("/")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(COOKIE_MAX_AGE_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            deleteCookie(response);
        }
        return authRequest;
    }

    private void deleteCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }
        return null;
    }

    private String serialize(OAuth2AuthorizationRequest authRequest) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(authRequest);
            oos.flush();
            String payload = B64.encodeToString(bos.toByteArray());
            String sig = B64.encodeToString(hmac(payload));
            return payload + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize authorization request", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        int dot = value.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String payload = value.substring(0, dot);
        String sig = value.substring(dot + 1);
        if (!MessageDigest.isEqual(hmac(payload), decodeQuietly(sig))) {
            log.debug("Rejected OAuth2 authz cookie with invalid signature");
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(B64D.decode(payload)))) {
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(OIF_PATTERN));
            return (OAuth2AuthorizationRequest) ois.readObject();
        } catch (Exception e) {
            log.debug("Failed to deserialize OAuth2 authz cookie: {}", e.getMessage());
            return null;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static byte[] decodeQuietly(String s) {
        try {
            return B64D.decode(s);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
