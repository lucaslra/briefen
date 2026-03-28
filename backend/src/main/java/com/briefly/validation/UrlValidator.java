package com.briefly.validation;

import com.briefly.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Component
public class UrlValidator {

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

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("URL must use HTTP or HTTPS scheme.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must have a valid host.");
        }

        checkNotPrivateIp(host);

        return uri;
    }

    private void checkNotPrivateIp(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress()) {
                throw new InvalidUrlException("URLs pointing to local or private IP addresses are not allowed.");
            }
        } catch (UnknownHostException e) {
            throw new InvalidUrlException("Could not resolve host: " + host);
        }
    }
}
