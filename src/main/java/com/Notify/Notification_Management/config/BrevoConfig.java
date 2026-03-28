package com.Notify.Notification_Management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "brevo")
@Data
public class BrevoConfig {
    
    private String apiKey;
    private String emailFrom;
    private String emailFromName;
    private String smsSender;
    
}
