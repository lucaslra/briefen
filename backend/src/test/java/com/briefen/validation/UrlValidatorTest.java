package com.briefen.validation;

import com.briefen.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() {
        urlValidator = new UrlValidator();
    }

    // ---- validate() happy paths ----

    @Test
    void shouldAcceptValidHttpUrl() {
        URI result = urlValidator.validate("http://example.com");
        assertThat(result.getScheme()).isEqualTo("http");
        assertThat(result.getHost()).isEqualTo("example.com");
    }

    @Test
    void shouldAcceptValidHttpsUrl() {
        URI result = urlValidator.validate("https://www.example.com");
        assertThat(result.getScheme()).isEqualTo("https");
        assertThat(result.getHost()).isEqualTo("www.example.com");
    }

    @Test
    void shouldAcceptUrlWithPath() {
        URI result = urlValidator.validate("https://example.com/articles/spring-boot?ref=home");
        assertThat(result.getPath()).isEqualTo("/articles/spring-boot");
        assertThat(result.getQuery()).isEqualTo("ref=home");
    }

    // ---- validate() rejection paths ----

    @Test
    void shouldRejectNullUrl() {
        assertThatThrownBy(() -> urlValidator.validate(null))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void shouldRejectEmptyUrl() {
        assertThatThrownBy(() -> urlValidator.validate("   "))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void shouldRejectFtpUrl() {
        assertThatThrownBy(() -> urlValidator.validate("ftp://files.example.com/data.zip"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("HTTP or HTTPS");
    }

    @Test
    void shouldRejectUrlWithoutHost() {
        assertThatThrownBy(() -> urlValidator.validate("http:///no-host"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shouldRejectLocalhostByName() {
        assertThatThrownBy(() -> urlValidator.validate("http://localhost/path"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldRejectLoopbackIp() {
        assertThatThrownBy(() -> urlValidator.validate("http://127.0.0.1/path"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldRejectPrivateIp10Block() {
        assertThatThrownBy(() -> urlValidator.validate("http://10.0.0.1/internal"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldRejectPrivateIp172Block() {
        assertThatThrownBy(() -> urlValidator.validate("http://172.16.0.1/internal"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldRejectPrivateIp192Block() {
        assertThatThrownBy(() -> urlValidator.validate("http://192.168.1.1/router"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldRejectCloudMetadataIp() {
        assertThatThrownBy(() -> urlValidator.validate("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    // ---- validateServiceUrl() ----

    @Test
    void shouldAllowPrivateIpForServiceUrl() {
        // Self-hosted Readeck on the local network — must NOT throw
        urlValidator.validateServiceUrl("http://192.168.1.100:8080");
    }

    @Test
    void shouldRejectNonHttpSchemeForServiceUrl() {
        assertThatThrownBy(() -> urlValidator.validateServiceUrl("ftp://192.168.1.100"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("HTTP or HTTPS");
    }

    @Test
    void shouldRejectNullForServiceUrl() {
        assertThatThrownBy(() -> urlValidator.validateServiceUrl(null))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("empty");
    }

    // ---- IPv6 cloud-metadata / ULA blocking ----

    @Test
    void shouldBlockIpv6UlaAddress() {
        // fd00::/8 is ULA (Unique Local Address) — not blocked by Java's isSiteLocalAddress()
        // but should be blocked to prevent SSRF via IPv6 ULA ranges including fd00:ec2::254
        assertThatThrownBy(() -> urlValidator.checkResolvedAddress(
                java.net.InetAddress.getByName("fd12:3456::1")))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }

    @Test
    void shouldBlockIpv6FcAddress() {
        // fc00::/7 covers both fc00::/8 and fd00::/8
        assertThatThrownBy(() -> urlValidator.checkResolvedAddress(
                java.net.InetAddress.getByName("fc00::1")))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("private");
    }
}
