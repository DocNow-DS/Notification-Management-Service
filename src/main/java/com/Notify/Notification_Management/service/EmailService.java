package com.Notify.Notification_Management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.Notify.Notification_Management.client.PatientServiceClient;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PatientServiceClient patientServiceClient;

    public void sendAppointmentApprovedEmail(String patientId, String appointmentId, String startTime, String token) {
        try {
            log.info("Attempting to send appointment approved email for patient ID: {}", patientId);

            // Get patient details
            var patient = patientServiceClient.getPatientById(patientId, token);

            if (patient == null) {
                log.error("Patient not found for patient ID: {} - Patient service returned null", patientId);
                return;
            }

            log.info("Patient data retrieved: {}", patient);

            String patientEmail = getPatientEmail(patient);
            if (patientEmail == null || patientEmail.trim().isEmpty()) {
                log.error("No email available for patient ID: {} - Patient email is null or empty", patientId);
                log.info("Patient object fields check - available methods: {}",
                        java.util.Arrays.toString(patient.getClass().getMethods()));
                return;
            }

            String patientName = getPatientName(patient);
            log.info("Sending email to: {} for patient: {}", patientEmail, patientName);

            // Send HTML email
            sendHtmlEmail(patientEmail, patientName, appointmentId, startTime);

            log.info("Appointment approved email sent successfully to patient {} at {}", patientId, patientEmail);

        } catch (Exception e) {
            log.error("Failed to send appointment approved email to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String toEmail, String patientName, String appointmentId, String startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@healthcare.com");
            helper.setTo(toEmail);
            helper.setSubject("Appointment Approved - Healthcare System");

            String htmlContent = buildAppointmentApprovedEmailContent(patientName, appointmentId, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            // Fallback to simple email
            sendSimpleEmail(toEmail, patientName, appointmentId, startTime);
        }
    }

    private void sendSimpleEmail(String toEmail, String patientName, String appointmentId, String startTime) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@healthcare.com");
            message.setTo(toEmail);
            message.setSubject("Appointment Approved - Healthcare System");
            message.setText(buildAppointmentApprovedTextContent(patientName, appointmentId, startTime));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
        }
    }

    private String buildAppointmentApprovedEmailContent(String patientName, String appointmentId, String startTime) {
        return String.format(
                "<html><body>" +
                        "<h2>Appointment Approved!</h2>" +
                        "<p>Dear %s,</p>" +
                        "<p>Your appointment has been approved by the doctor.</p>" +
                        "<p><strong>Appointment Details:</strong></p>" +
                        "<ul>" +
                        "<li>Appointment ID: %s</li>" +
                        "<li>Date & Time: %s</li>" +
                        "</ul>" +
                        "<p>Please arrive 15 minutes before your scheduled appointment time.</p>" +
                        "<p>If you need to reschedule or have any questions, please contact us.</p>" +
                        "<br>" +
                        "<p>Best regards,<br/>Healthcare Team</p>" +
                        "</body></html>",
                patientName, appointmentId, startTime != null ? startTime : "Scheduled time");
    }

    private String buildAppointmentApprovedTextContent(String patientName, String appointmentId, String startTime) {
        return String.format(
                "Appointment Approved!\n\n" +
                        "Dear %s,\n\n" +
                        "Your appointment has been approved by the doctor.\n\n" +
                        "Appointment Details:\n" +
                        "Appointment ID: %s\n" +
                        "Date & Time: %s\n\n" +
                        "Please arrive 15 minutes before your scheduled appointment time.\n\n" +
                        "If you need to reschedule or have any questions, please contact us.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                patientName, appointmentId, startTime != null ? startTime : "Scheduled time");
    }

    private String getPatientEmail(Object patient) {
        try {
            log.debug("Attempting to extract email from patient object of type: {}", patient.getClass().getName());

            // Using reflection to get patient email safely
            if (patient.getClass().getMethod("getEmail") != null) {
                String email = (String) patient.getClass().getMethod("getEmail").invoke(patient);
                log.debug("Extracted email: {}", email);
                return email;
            } else {
                log.warn("getEmail method not found on patient object");
            }
        } catch (Exception e) {
            log.error("Could not extract patient email: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getPatientName(Object patient) {
        try {
            // Using reflection to get patient name safely
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
}
