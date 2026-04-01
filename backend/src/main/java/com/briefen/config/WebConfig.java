package com.briefen.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-static-file requests to index.html so that
 * React Router client-side routing works on browser refresh.
 * Spring Boot auto-serves static assets from classpath:/static/.
 * Controller mappings (/api/**, /actuator/**) take precedence over this catch-all.
 */
@Controller
public class WebConfig {

    @GetMapping(value = {
            "/{path:[^\\.]*}",
            "/{path:[^\\.]*}/{subpath:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
