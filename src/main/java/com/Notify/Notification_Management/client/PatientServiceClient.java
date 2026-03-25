package com.Notify.Notification_Management.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PatientServiceClient {

    private final RestTemplate restTemplate;
    private final String patientServiceBaseUrl;

    public PatientServiceClient(
            RestTemplate restTemplate,
            @Value("${services.patient.base-url}") String patientServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.patientServiceBaseUrl = patientServiceBaseUrl;
    }

    public boolean validateToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                patientServiceBaseUrl + "/api/auth/validate",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsernameFromToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                patientServiceBaseUrl + "/api/auth/username",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
