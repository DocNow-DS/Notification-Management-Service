package com.Notify.Notification_Management.service;

import com.Notify.Notification_Management.client.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final PatientServiceClient patientServiceClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${infobip.api.key}")
    private String apiKey;

    @Value("${infobip.base.url}")
    private String baseUrl;

    @Value("${infobip.sender.id:HealthCare}")
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

    private void sendSms(String phoneNumber, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "App " + apiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", senderId);

            Map<String, Object> destination = new HashMap<>();
            destination.put("to", phoneNumber);

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("destinations", List.of(destination));
            messageMap.put("text", message);
            messageMap.put("from", senderId);

            payload.put("messages", List.of(messageMap));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            String url = baseUrl + "/sms/2/text/advanced";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.info("SMS sent successfully. Response status: {}, body: {}", response.getStatusCode(), response.getBody());
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
        phone = phone.trim().replaceAll("[^\\d+]", "");
        if (!phone.startsWith("+") && !phone.startsWith("00")) {
            phone = "+94" + phone;
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
