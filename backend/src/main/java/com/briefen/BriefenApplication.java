package com.briefen;

import com.briefen.config.BriefenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BriefenProperties.class)
public class BriefenApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefenApplication.class, args);
    }
}
