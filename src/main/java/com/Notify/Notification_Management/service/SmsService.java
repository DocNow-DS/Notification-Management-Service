package com.Notify.Notification_Management.service;

import com.Notify.Notification_Management.client.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final PatientServiceClient patientServiceClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${smsapi.api.key}")
    private String apiKey;

    @Value("${smsapi.api.url}")
    private String apiUrl;

    @Value("${smsapi.sender.id}")
    private String senderId;

    public void sendAppointmentApprovedSms(String patientId, String appointmentId, String startTime, String token) {
        try {
            log.info("Attempting to send appointment approved SMS for patient ID: {}", patientId);

            var patient = patientServiceClient.getPatientById(patientId, token);

            if (patient == null) {
                log.error("Patient not found for patient ID: {} - Patient service returned null", patientId);
                return;
            }

            String patientPhone = getPatientPhone(patient);
            if (patientPhone == null || patientPhone.trim().isEmpty()) {
                log.error("No phone number available for patient ID: {}", patientId);
                return;
            }

            String patientName = getPatientName(patient);
            String message = buildAppointmentApprovedSmsContent(patientName, appointmentId, startTime);

            sendSms(patientPhone, message);
            log.info("Appointment approved SMS sent successfully to patient {} at {}", patientId, patientPhone);

        } catch (Exception e) {
            log.error("Failed to send appointment approved SMS to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    public void sendAppointmentDeclinedSms(String patientId, String appointmentId, String startTime, String reason, String token) {
        try {
            log.info("Attempting to send appointment declined SMS for patient ID: {}", patientId);

            var patient = patientServiceClient.getPatientById(patientId, token);

            if (patient == null) {
                log.error("Patient not found for patient ID: {} - Patient service returned null", patientId);
                return;
            }

            String patientPhone = getPatientPhone(patient);
            if (patientPhone == null || patientPhone.trim().isEmpty()) {
                log.error("No phone number available for patient ID: {}", patientId);
                return;
            }

            String patientName = getPatientName(patient);
            String message = buildAppointmentDeclinedSmsContent(patientName, appointmentId, startTime, reason);

            sendSms(patientPhone, message);
            log.info("Appointment declined SMS sent successfully to patient {} at {}", patientId, patientPhone);

        } catch (Exception e) {
            log.error("Failed to send appointment declined SMS to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    private void sendSms(String phoneNumber, String messageText) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("SMSAPI.LK not configured. Would have sent SMS to {}: {}", phoneNumber, messageText);
                return;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("recipient", phoneNumber);
            requestBody.put("sender_id", senderId);
            requestBody.put("type", "plain");
            requestBody.put("message", messageText);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to {}. Response: {}", phoneNumber, response.getBody());
            } else {
                log.error("Failed to send SMS to {}. Status: {}, Response: {}", 
                    phoneNumber, response.getStatusCode(), response.getBody());
                throw new RuntimeException("SMS API returned non-success status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    private String getPatientPhone(Object patient) {
        try {
            log.debug("Attempting to extract phone from patient object of type: {}", patient.getClass().getName());

            if (patient instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) patient;
                Object phone = map.get("phone");
                if (phone != null && !phone.toString().isEmpty()) {
                    return formatPhoneNumber(phone.toString());
                }
                Object phoneNumber = map.get("phoneNumber");
                if (phoneNumber != null && !phoneNumber.toString().isEmpty()) {
                    return formatPhoneNumber(phoneNumber.toString());
                }
                Object mobile = map.get("mobile");
                if (mobile != null && !mobile.toString().isEmpty()) {
                    return formatPhoneNumber(mobile.toString());
                }
                log.debug("No phone number found in patient map. Available keys: {}", map.keySet());
                return null;
            }

            if (patient.getClass().getMethod("getPhone") != null) {
                String phone = (String) patient.getClass().getMethod("getPhone").invoke(patient);
                return formatPhoneNumber(phone);
            }
        } catch (Exception e) {
            log.error("Could not extract patient phone: {}", e.getMessage(), e);
        }
        return null;
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        phone = phone.trim().replaceAll("[^\\d]", "");
        
        // smsapi.lk expects numbers in format 94XXXXXXXXX (without + prefix)
        if (phone.startsWith("00")) {
            phone = phone.substring(2);
        } else if (phone.startsWith("+")) {
            phone = phone.substring(1);
        } else if (phone.startsWith("0")) {
            // Local Sri Lankan number starting with 0
            phone = "94" + phone.substring(1);
        } else if (!phone.startsWith("94")) {
            // Add country code if not present
            phone = "94" + phone;
        }
        
        return phone;
    }

    private String getPatientName(Object patient) {
        try {
            if (patient instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) patient;
                Object name = map.get("name");
                if (name != null && !name.toString().isEmpty() && !"Unknown".equals(name)) {
                    return name.toString();
                }
                Object username = map.get("username");
                return username != null ? username.toString() : "Patient";
            }

            if (patient.getClass().getMethod("getFirstName") != null &&
                    patient.getClass().getMethod("getLastName") != null) {
                String firstName = (String) patient.getClass().getMethod("getFirstName").invoke(patient);
                String lastName = (String) patient.getClass().getMethod("getLastName").invoke(patient);
                return firstName + " " + lastName;
            }
        } catch (Exception e) {
            log.warn("Could not extract patient name: {}", e.getMessage());
        }
        return "Patient";
    }

    private String buildAppointmentApprovedSmsContent(String patientName, String appointmentId, String startTime) {
        return String.format(
                "Dear %s, your appointment (ID: %s) has been APPROVED by the doctor. " +
                        "Scheduled for: %s. Please arrive 15 minutes early. - HealthCare",
                patientName,
                appointmentId,
                startTime != null ? startTime : "scheduled time"
        );
    }

    private String buildAppointmentDeclinedSmsContent(String patientName, String appointmentId, String startTime, String reason) {
        String baseMessage = String.format(
                "Dear %s, your appointment (ID: %s) scheduled for %s has been DECLINED by the doctor.",
                patientName,
                appointmentId,
                startTime != null ? startTime : "scheduled time"
        );

        if (reason != null && !reason.trim().isEmpty()) {
            baseMessage += " Reason: " + reason;
        }

        baseMessage += " Please reschedule. - HealthCare";
        return baseMessage;
    }
}
