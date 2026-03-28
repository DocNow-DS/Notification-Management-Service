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

    public Object getPatientById(String patientId, String token) {
        try {
            // Check if patientId looks like a MongoDB ObjectId (24 hex characters)
            // or a username (contains non-hex characters or wrong length)
            String url;
            if (patientId != null && patientId.matches("^[0-9a-fA-F]{24}$")) {
                // It's a valid ObjectId format
                url = patientServiceBaseUrl + "/api/patient/" + patientId;
            } else {
                // It's likely a username
                url = patientServiceBaseUrl + "/api/patient/username/" + patientId;
            }
            System.out.println("DEBUG: Calling patient service at URL: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            System.out.println("DEBUG: Patient service response status: " + response.getStatusCode());
            System.out.println("DEBUG: Patient service response body: " + response.getBody());
            
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            System.out.println("DEBUG: Error calling patient service: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
