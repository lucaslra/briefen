package com.briefen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping
    public Map<String, String> version() {
        String version = "dev";
        String buildTime = "unknown";

        if (buildProperties != null) {
            String v = buildProperties.getVersion();
            if (v != null) {
                version = v;
            }
            Instant time = buildProperties.getTime();
            if (time != null) {
                buildTime = time.toString();
            }
        }

        return Map.of("version", version, "buildTime", buildTime);
    }
}
