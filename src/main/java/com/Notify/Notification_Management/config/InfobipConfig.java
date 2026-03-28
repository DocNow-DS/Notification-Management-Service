package com.Notify.Notification_Management.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class InfobipConfig {
    // Configuration is now handled directly in SmsService using RestTemplate
    // to avoid compatibility issues with Infobip SDK versions
}
