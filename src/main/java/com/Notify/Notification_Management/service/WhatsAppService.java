package com.Notify.Notification_Management.service;

import com.Notify.Notification_Management.client.DoctorServiceClient;
import com.Notify.Notification_Management.client.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final RestTemplate restTemplate;

    @Value("${ultramsg.instance.id}")
    private String instanceId;

    @Value("${ultramsg.token}")
    private String ultraMsgToken;

    @Value("${ultramsg.api.url:https://api.ultramsg.com}")
    private String ultraMsgApiUrl;

    public void sendAppointmentApprovedWhatsApp(String patientId, String appointmentId, String startTime, String token) {
        try {
            log.info("Attempting to send appointment approved WhatsApp message for patient ID: {}", patientId);

            // Check if Ultramsg is configured
            if (!isConfigured()) {
                log.warn("Ultramsg not configured. Skipping WhatsApp notification for patient {}", patientId);
                return;
            }

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
            String message = buildAppointmentApprovedWhatsAppContent(patientName, appointmentId, startTime);

            sendWhatsAppMessage(patientPhone, message);
            log.info("Appointment approved WhatsApp message sent successfully to patient {} at {}", patientId, patientPhone);

        } catch (Exception e) {
            log.error("Failed to send appointment approved WhatsApp message to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    public void sendAppointmentDeclinedWhatsApp(String patientId, String appointmentId, String startTime, String reason, String token) {
        try {
            log.info("Attempting to send appointment declined WhatsApp message for patient ID: {}", patientId);

            // Check if Ultramsg is configured
            if (!isConfigured()) {
                log.warn("Ultramsg not configured. Skipping WhatsApp notification for patient {}", patientId);
                return;
            }

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
            String message = buildAppointmentDeclinedWhatsAppContent(patientName, appointmentId, startTime, reason);

            sendWhatsAppMessage(patientPhone, message);
            log.info("Appointment declined WhatsApp message sent successfully to patient {} at {}", patientId, patientPhone);

        } catch (Exception e) {
            log.error("Failed to send appointment declined WhatsApp message to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return instanceId != null && !instanceId.isEmpty()
                && ultraMsgToken != null && !ultraMsgToken.isEmpty();
    }

    private void sendWhatsAppMessage(String phoneNumber, String messageText) {
        try {
            String url = String.format("%s/%s/messages/chat", ultraMsgApiUrl, instanceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("token", ultraMsgToken);
            map.add("to", phoneNumber);
            map.add("body", messageText);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp message sent successfully to {}. Response: {}", phoneNumber, response.getBody());
            } else {
                log.error("Failed to send WhatsApp message to {}. Status: {}, Response: {}",
                        phoneNumber, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send WhatsApp message: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send WhatsApp message", e);
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
        // Ultramsg requires format: 94123456789 (country code + number, no + or 00)
        if (phone.startsWith("00")) {
            phone = phone.substring(2);
        }
        if (phone.startsWith("+")) {
            phone = phone.substring(1);
        }
        // If no country code, assume Sri Lanka (+94)
        if (phone.length() == 9 && phone.startsWith("0")) {
            phone = "94" + phone.substring(1);
        } else if (phone.length() == 10 && phone.charAt(0) == '0') {
            phone = "94" + phone.substring(1);
        } else if (!phone.startsWith("94") && phone.length() <= 10) {
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

    private String buildAppointmentApprovedWhatsAppContent(String patientName, String appointmentId, String startTime) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your appointment has been *APPROVED* by the doctor.\n\n" +
                        "Appointment Details:\n" +
                        "Appointment ID: %s\n" +
                        "Date & Time: %s\n\n" +
                        "Please arrive 15 minutes before your scheduled appointment time.\n\n" +
                        "If you need to reschedule or have any questions, please contact us.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                patientName,
                appointmentId,
                startTime != null ? startTime : "Scheduled time"
        );
    }

    private String buildAppointmentDeclinedWhatsAppContent(String patientName, String appointmentId, String startTime, String reason) {
        String baseMessage = String.format(
                "Hello %s,\n\n" +
                        "Your appointment (ID: %s) scheduled for %s has been *DECLINED* by the doctor.",
                patientName,
                appointmentId,
                startTime != null ? startTime : "scheduled time"
        );

        if (reason != null && !reason.trim().isEmpty()) {
            baseMessage += "\n\nReason: " + reason;
        }

        baseMessage += "\n\nPlease reschedule through our app or contact us.\n\n" +
                "Best regards,\n" +
                "Healthcare Team";
        return baseMessage;
    }

    public void sendAppointmentCreatedWhatsAppToDoctor(String doctorId, String patientId, String appointmentId, String startTime, String token) {
        try {
            log.info("Attempting to send appointment created WhatsApp message to doctor ID: {}", doctorId);

            // Check if Ultramsg is configured
            if (!isConfigured()) {
                log.warn("Ultramsg not configured. Skipping WhatsApp notification for doctor {}", doctorId);
                return;
            }

            // Get doctor details
            var doctor = doctorServiceClient.getDoctorById(doctorId, token);

            if (doctor == null) {
                log.error("Doctor not found for doctor ID: {} - Doctor service returned null", doctorId);
                return;
            }

            String doctorPhone = getDoctorPhone(doctor);
            if (doctorPhone == null || doctorPhone.trim().isEmpty()) {
                log.error("No phone number available for doctor ID: {}", doctorId);
                return;
            }

            String doctorName = getDoctorName(doctor);
            
            // Get patient name for the notification
            var patient = patientServiceClient.getPatientById(patientId, token);
            String patientName = patient != null ? getPatientName(patient) : "A patient";

            String message = buildAppointmentCreatedWhatsAppContent(doctorName, patientName, appointmentId, startTime);

            sendWhatsAppMessage(doctorPhone, message);
            log.info("Appointment created WhatsApp message sent successfully to doctor {} at {}", doctorId, doctorPhone);

        } catch (Exception e) {
            log.error("Failed to send appointment created WhatsApp message to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    private String getDoctorPhone(Object doctor) {
        try {
            log.debug("Attempting to extract phone from doctor object of type: {}", doctor.getClass().getName());

            if (doctor instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) doctor;
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
                log.debug("No phone number found in doctor map. Available keys: {}", map.keySet());
                return null;
            }

            if (doctor.getClass().getMethod("getPhone") != null) {
                String phone = (String) doctor.getClass().getMethod("getPhone").invoke(doctor);
                return formatPhoneNumber(phone);
            }
        } catch (Exception e) {
            log.error("Could not extract doctor phone: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getDoctorName(Object doctor) {
        try {
            if (doctor instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) doctor;
                Object name = map.get("name");
                if (name != null && !name.toString().isEmpty()) {
                    return name.toString();
                }
                Object username = map.get("username");
                return username != null ? username.toString() : "Doctor";
            }

            if (doctor.getClass().getMethod("getName") != null) {
                String name = (String) doctor.getClass().getMethod("getName").invoke(doctor);
                return name != null ? name : "Doctor";
            }
        } catch (Exception e) {
            log.warn("Could not extract doctor name: {}", e.getMessage());
        }
        return "Doctor";
    }

    private String buildAppointmentCreatedWhatsAppContent(String doctorName, String patientName, String appointmentId, String startTime) {
        return String.format(
                "Hello Dr. %s,\n\n" +
                        "%s has requested an appointment with you.\n\n" +
                        "Appointment Details:\n" +
                        "Appointment ID: %s\n" +
                        "Date & Time: %s\n\n" +
                        "Please log in to your dashboard to review and accept or decline this appointment request.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                doctorName,
                patientName,
                appointmentId,
                startTime != null ? startTime : "To be scheduled"
        );
    }
}
