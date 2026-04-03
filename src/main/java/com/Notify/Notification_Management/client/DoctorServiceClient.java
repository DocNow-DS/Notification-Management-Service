package com.Notify.Notification_Management.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DoctorServiceClient {

    private final RestTemplate restTemplate;
    private final String doctorServiceBaseUrl;

    public DoctorServiceClient(
            RestTemplate restTemplate,
            @Value("${services.doctor.base-url}") String doctorServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.doctorServiceBaseUrl = doctorServiceBaseUrl;
    }

    public Object getDoctorById(String doctorId, String token) {
        try {
            String url = doctorServiceBaseUrl + "/api/doctors/" + doctorId;
            log.info("Fetching doctor details from: {}", url);

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

            log.info("Doctor service response status: {}", response.getStatusCode());
            log.debug("Doctor service response body: {}", response.getBody());

            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            log.error("Error fetching doctor details for ID {}: {}", doctorId, e.getMessage());
            return null;
        }
    }
}
