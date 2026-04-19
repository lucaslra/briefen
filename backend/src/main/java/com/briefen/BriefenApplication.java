package com.briefen;

import com.briefen.config.AnthropicProperties;
import com.briefen.config.BriefenProperties;
import com.briefen.config.OllamaProperties;
import com.briefen.config.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BriefenProperties.class, OllamaProperties.class, OpenAiProperties.class, AnthropicProperties.class})
public class BriefenApplication {

    public static void main(String[] args) {
        SpringApplication.run(BriefenApplication.class, args);
    }
}
