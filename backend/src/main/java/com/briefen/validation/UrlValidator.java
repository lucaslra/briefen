package com.briefen.validation;

import com.briefen.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class UrlValidator {

    /**
     * Validates and normalizes a URL for article fetching.
     * Rejects private/loopback IPs, non-HTTP(S) schemes, cloud metadata addresses.
     */
    public URI validate(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be empty.");
        }

        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("Malformed URL: " + url);
        }

        validateScheme(uri);
        validateHost(uri);
        checkNotPrivateIp(uri.getHost());

        return uri;
    }

    /**
     * Validates a base URL for user-configured services (e.g., Readeck).
     * Only checks scheme and host validity — does NOT block private IPs,
     * since self-hosted services are typically on the local network.
     * The user explicitly configures these URLs, so they are trusted.
     */
    public void validateServiceUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be empty.");
        }

        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("Malformed URL: " + url);
        }

        validateScheme(uri);
        validateHost(uri);
        // No private IP check — self-hosted services live on the local network
    }

    /**
     * Re-validates a resolved IP address against the private IP blocklist.
     * Call this just before making the actual HTTP request to prevent DNS rebinding.
     */
    public void checkResolvedAddress(InetAddress address) {
        if (isPrivateAddress(address)) {
            throw new InvalidUrlException(
                    "DNS resolved to a private/local address (%s). Request blocked."
                            .formatted(address.getHostAddress()));
        }
    }

    private void validateScheme(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("URL must use HTTP or HTTPS scheme.");
        }
    }

    private void validateHost(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must have a valid host.");
        }
    }

    private void checkNotPrivateIp(String host) {
        try {
            // Resolve ALL addresses (catches dual-stack hosts)
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    throw new InvalidUrlException(
                            "URLs pointing to local or private IP addresses are not allowed.");
                }
            }
        } catch (UnknownHostException e) {
            throw new InvalidUrlException("Could not resolve host: " + host);
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        return address.isLoopbackAddress()       // 127.0.0.0/8, ::1
                || address.isSiteLocalAddress()   // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
                || address.isLinkLocalAddress()    // 169.254.0.0/16, fe80::/10
                || address.isAnyLocalAddress()     // 0.0.0.0, ::
                || isCloudMetadata(address);       // 169.254.169.254 (AWS/GCP/Azure)
    }

    /**
     * Blocks the well-known cloud metadata endpoint (169.254.169.254)
     * which is technically link-local but deserves an explicit check.
     */
    private boolean isCloudMetadata(InetAddress address) {
        byte[] bytes = address.getAddress();
        // IPv4: 169.254.169.254
        if (bytes.length == 4) {
            return (bytes[0] & 0xFF) == 169
                    && (bytes[1] & 0xFF) == 254
                    && (bytes[2] & 0xFF) == 169
                    && (bytes[3] & 0xFF) == 254;
        }
        return false;
    }
}
