package com.Notify.Notification_Management.config;
 
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
 
@Configuration
public class MongoConfig {
 
    @Value("${spring.data.mongodb.uri:mongodb+srv://lasannavodya:DelLCS5EiIBT3jFu@course.uf2pi.mongodb.net/HealthCare?retryWrites=true&w=majority&appName=Course}")
    private String mongoUri;
 
    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(MongoClients.create(mongoUri), "HealthCare");
    }
}