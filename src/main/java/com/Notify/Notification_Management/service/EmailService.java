package com.Notify.Notification_Management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.Notify.Notification_Management.client.DoctorServiceClient;
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
    private final DoctorServiceClient doctorServiceClient;

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

            // Handle LinkedHashMap from RestTemplate
            if (patient instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) patient;
                Object email = map.get("email");
                log.debug("Extracted email from map: {}", email);
                return email != null ? email.toString() : null;
            }

            // Fallback to reflection for actual objects
            if (patient.getClass().getMethod("getEmail") != null) {
                String email = (String) patient.getClass().getMethod("getEmail").invoke(patient);
                log.debug("Extracted email via reflection: {}", email);
                return email;
            }
        } catch (Exception e) {
            log.error("Could not extract patient email: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getPatientName(Object patient) {
        try {
            // Handle LinkedHashMap from RestTemplate
            if (patient instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) patient;
                Object name = map.get("name");
                if (name != null && !name.toString().isEmpty() && !"Unknown".equals(name)) {
                    return name.toString();
                }
                // Fallback to username if name is not set
                Object username = map.get("username");
                return username != null ? username.toString() : "Patient";
            }

            // Fallback to reflection for actual objects
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

    public void sendAppointmentCreatedEmailToDoctor(String doctorId, String patientId, String appointmentId, String startTime, String token) {
        try {
            log.info("Attempting to send appointment created email to doctor ID: {}", doctorId);

            // Get doctor details
            var doctor = doctorServiceClient.getDoctorById(doctorId, token);

            if (doctor == null) {
                log.error("Doctor not found for doctor ID: {} - Doctor service returned null", doctorId);
                return;
            }

            log.info("Doctor data retrieved: {}", doctor);

            String doctorEmail = getDoctorEmail(doctor);
            if (doctorEmail == null || doctorEmail.trim().isEmpty()) {
                log.error("No email available for doctor ID: {}", doctorId);
                return;
            }

            String doctorName = getDoctorName(doctor);
            
            // Get patient name for the notification
            var patient = patientServiceClient.getPatientById(patientId, token);
            String patientName = patient != null ? getPatientName(patient) : "A patient";

            log.info("Sending email to: {} for doctor: {}", doctorEmail, doctorName);

            // Send HTML email
            sendAppointmentCreatedHtmlEmail(doctorEmail, doctorName, patientName, appointmentId, startTime);

            log.info("Appointment created email sent successfully to doctor {} at {}", doctorId, doctorEmail);

        } catch (Exception e) {
            log.error("Failed to send appointment created email to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    private void sendAppointmentCreatedHtmlEmail(String toEmail, String doctorName, String patientName, String appointmentId, String startTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@healthcare.com");
            helper.setTo(toEmail);
            helper.setSubject("New Appointment Request - Healthcare System");

            String htmlContent = buildAppointmentCreatedEmailContent(doctorName, patientName, appointmentId, startTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            // Fallback to simple email
            sendAppointmentCreatedSimpleEmail(toEmail, doctorName, patientName, appointmentId, startTime);
        }
    }

    private void sendAppointmentCreatedSimpleEmail(String toEmail, String doctorName, String patientName, String appointmentId, String startTime) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@healthcare.com");
            message.setTo(toEmail);
            message.setSubject("New Appointment Request - Healthcare System");
            message.setText(buildAppointmentCreatedTextContent(doctorName, patientName, appointmentId, startTime));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
        }
    }

    private String buildAppointmentCreatedEmailContent(String doctorName, String patientName, String appointmentId, String startTime) {
        return String.format(
                "<html><body>" +
                        "<h2>New Appointment Request!</h2>" +
                        "<p>Dear Dr. %s,</p>" +
                        "<p>%s has requested an appointment with you.</p>" +
                        "<p><strong>Appointment Details:</strong></p>" +
                        "<ul>" +
                        "<li>Appointment ID: %s</li>" +
                        "<li>Date & Time: %s</li>" +
                        "</ul>" +
                        "<p>Please log in to your dashboard to review and accept or decline this appointment request.</p>" +
                        "<br>" +
                        "<p>Best regards,<br/>Healthcare Team</p>" +
                        "</body></html>",
                doctorName, patientName, appointmentId, startTime != null ? startTime : "To be scheduled");
    }

    private String buildAppointmentCreatedTextContent(String doctorName, String patientName, String appointmentId, String startTime) {
        return String.format(
                "New Appointment Request!\n\n" +
                        "Dear Dr. %s,\n\n" +
                        "%s has requested an appointment with you.\n\n" +
                        "Appointment Details:\n" +
                        "Appointment ID: %s\n" +
                        "Date & Time: %s\n\n" +
                        "Please log in to your dashboard to review and accept or decline this appointment request.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                doctorName, patientName, appointmentId, startTime != null ? startTime : "To be scheduled");
    }

    private String getDoctorEmail(Object doctor) {
        try {
            log.debug("Attempting to extract email from doctor object of type: {}", doctor.getClass().getName());

            // Handle LinkedHashMap from RestTemplate
            if (doctor instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) doctor;
                Object email = map.get("email");
                log.debug("Extracted email from map: {}", email);
                return email != null ? email.toString() : null;
            }

            // Fallback to reflection for actual objects
            if (doctor.getClass().getMethod("getEmail") != null) {
                String email = (String) doctor.getClass().getMethod("getEmail").invoke(doctor);
                log.debug("Extracted email via reflection: {}", email);
                return email;
            }
        } catch (Exception e) {
            log.error("Could not extract doctor email: {}", e.getMessage(), e);
        }
        return null;
    }

    private String getDoctorName(Object doctor) {
        try {
            // Handle LinkedHashMap from RestTemplate
            if (doctor instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) doctor;
                Object name = map.get("name");
                if (name != null && !name.toString().isEmpty()) {
                    return name.toString();
                }
                // Fallback to username if name is not set
                Object username = map.get("username");
                return username != null ? username.toString() : "Doctor";
            }

            // Fallback to reflection for actual objects
            if (doctor.getClass().getMethod("getName") != null) {
                String name = (String) doctor.getClass().getMethod("getName").invoke(doctor);
                return name != null ? name : "Doctor";
            }
        } catch (Exception e) {
            log.warn("Could not extract doctor name: {}", e.getMessage());
        }
        return "Doctor";
    }

    public void sendPaymentCompletedEmailToDoctor(String doctorId, String patientId, String paymentId, String consultationId, Long amountCents, String currency, String token) {
        try {
            log.info("Attempting to send payment completed email to doctor ID: {}", doctorId);

            // Get doctor details
            var doctor = doctorServiceClient.getDoctorById(doctorId, token);

            if (doctor == null) {
                log.error("Doctor not found for doctor ID: {} - Doctor service returned null", doctorId);
                return;
            }

            log.info("Doctor data retrieved: {}", doctor);

            String doctorEmail = getDoctorEmail(doctor);
            if (doctorEmail == null || doctorEmail.trim().isEmpty()) {
                log.error("No email available for doctor ID: {}", doctorId);
                return;
            }

            String doctorName = getDoctorName(doctor);
            
            // Get patient name for the notification
            var patient = patientServiceClient.getPatientById(patientId, token);
            String patientName = patient != null ? getPatientName(patient) : "A patient";

            log.info("Sending payment email to: {} for doctor: {}", doctorEmail, doctorName);

            // Send HTML email
            sendPaymentCompletedHtmlEmail(doctorEmail, doctorName, patientName, paymentId, consultationId, amountCents, currency);

            log.info("Payment completed email sent successfully to doctor {} at {}", doctorId, doctorEmail);

        } catch (Exception e) {
            log.error("Failed to send payment completed email to doctor {}: {}", doctorId, e.getMessage(), e);
        }
    }

    private void sendPaymentCompletedHtmlEmail(String toEmail, String doctorName, String patientName, String paymentId, String consultationId, Long amountCents, String currency) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@healthcare.com");
            helper.setTo(toEmail);
            helper.setSubject("Payment Received - Bill Request Paid");

            String htmlContent = buildPaymentCompletedEmailContent(doctorName, patientName, paymentId, consultationId, amountCents, currency);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            // Fallback to simple email
            sendPaymentCompletedSimpleEmail(toEmail, doctorName, patientName, paymentId, consultationId, amountCents, currency);
        }
    }

    private void sendPaymentCompletedSimpleEmail(String toEmail, String doctorName, String patientName, String paymentId, String consultationId, Long amountCents, String currency) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@healthcare.com");
            message.setTo(toEmail);
            message.setSubject("Payment Received - Bill Request Paid");
            message.setText(buildPaymentCompletedTextContent(doctorName, patientName, paymentId, consultationId, amountCents, currency));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
        }
    }

    private String buildPaymentCompletedEmailContent(String doctorName, String patientName, String paymentId, String consultationId, Long amountCents, String currency) {
        String amountFormatted = amountCents != null ? String.format("%.2f", amountCents / 100.0) : "N/A";
        String currencyCode = currency != null ? currency.toUpperCase() : "LKR";
        return String.format(
                "<html><body>" +
                        "<h2>Payment Received!</h2>" +
                        "<p>Dear Dr. %s,</p>" +
                        "<p>%s has successfully completed the payment for your bill request.</p>" +
                        "<p><strong>Payment Details:</strong></p>" +
                        "<ul>" +
                        "<li>Payment ID: %s</li>" +
                        "<li>Consultation ID: %s</li>" +
                        "<li>Amount: %s %s</li>" +
                        "</ul>" +
                        "<p>Please log in to your dashboard to view more details about this payment.</p>" +
                        "<br>" +
                        "<p>Best regards,<br/>Healthcare Team</p>" +
                        "</body></html>",
                doctorName, patientName, paymentId, consultationId, amountFormatted, currencyCode);
    }

    private String buildPaymentCompletedTextContent(String doctorName, String patientName, String paymentId, String consultationId, Long amountCents, String currency) {
        String amountFormatted = amountCents != null ? String.format("%.2f", amountCents / 100.0) : "N/A";
        String currencyCode = currency != null ? currency.toUpperCase() : "LKR";
        return String.format(
                "Payment Received!\n\n" +
                        "Dear Dr. %s,\n\n" +
                        "%s has successfully completed the payment for your bill request.\n\n" +
                        "Payment Details:\n" +
                        "Payment ID: %s\n" +
                        "Consultation ID: %s\n" +
                        "Amount: %s %s\n\n" +
                        "Please log in to your dashboard to view more details about this payment.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                doctorName, patientName, paymentId, consultationId, amountFormatted, currencyCode);
    }

    public void sendCarePlanCreatedEmailToPatient(String patientId, String doctorId, String carePlanId, String appointmentId, String consultationNotes, String token) {
        try {
            log.info("Attempting to send care plan created email to patient ID: {}", patientId);

            // Get patient details
            var patient = patientServiceClient.getPatientById(patientId, token);

            if (patient == null) {
                log.error("Patient not found for patient ID: {} - Patient service returned null", patientId);
                return;
            }

            log.info("Patient data retrieved: {}", patient);

            String patientEmail = getPatientEmail(patient);
            if (patientEmail == null || patientEmail.trim().isEmpty()) {
                log.error("No email available for patient ID: {}", patientId);
                return;
            }

            String patientName = getPatientName(patient);
            
            // Get doctor name for the notification
            var doctor = doctorServiceClient.getDoctorById(doctorId, token);
            String doctorName = doctor != null ? getDoctorName(doctor) : "Your doctor";

            log.info("Sending care plan email to: {} for patient: {}", patientEmail, patientName);

            // Send HTML email
            sendCarePlanCreatedHtmlEmail(patientEmail, patientName, doctorName, carePlanId, appointmentId, consultationNotes);

            log.info("Care plan created email sent successfully to patient {} at {}", patientId, patientEmail);

        } catch (Exception e) {
            log.error("Failed to send care plan created email to patient {}: {}", patientId, e.getMessage(), e);
        }
    }

    private void sendCarePlanCreatedHtmlEmail(String toEmail, String patientName, String doctorName, String carePlanId, String appointmentId, String consultationNotes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@healthcare.com");
            helper.setTo(toEmail);
            helper.setSubject("New Care Plan Created - Healthcare System");

            String htmlContent = buildCarePlanCreatedEmailContent(patientName, doctorName, carePlanId, appointmentId, consultationNotes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email: {}", e.getMessage(), e);
            // Fallback to simple email
            sendCarePlanCreatedSimpleEmail(toEmail, patientName, doctorName, carePlanId, appointmentId, consultationNotes);
        }
    }

    private void sendCarePlanCreatedSimpleEmail(String toEmail, String patientName, String doctorName, String carePlanId, String appointmentId, String consultationNotes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@healthcare.com");
            message.setTo(toEmail);
            message.setSubject("New Care Plan Created - Healthcare System");
            message.setText(buildCarePlanCreatedTextContent(patientName, doctorName, carePlanId, appointmentId, consultationNotes));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
        }
    }

    private String buildCarePlanCreatedEmailContent(String patientName, String doctorName, String carePlanId, String appointmentId, String consultationNotes) {
        String appointmentInfo = appointmentId != null ? String.format("<li>Related Appointment: %s</li>", appointmentId) : "";
        String notesInfo = consultationNotes != null && !consultationNotes.isBlank() 
            ? String.format("<p><strong>Consultation Notes:</strong> %s</p>", consultationNotes) 
            : "";
        return String.format(
                "<html><body>" +
                        "<h2>New Care Plan Created!</h2>" +
                        "<p>Dear %s,</p>" +
                        "<p>%s has created a new care plan for you.</p>" +
                        "<p><strong>Care Plan Details:</strong></p>" +
                        "<ul>" +
                        "<li>Care Plan ID: %s</li>" +
                        "%s" +
                        "</ul>" +
                        "%s" +
                        "<p>Please log in to your dashboard to view your complete care plan details, including medications and instructions.</p>" +
                        "<br>" +
                        "<p>Best regards,<br/>Healthcare Team</p>" +
                        "</body></html>",
                patientName, doctorName, carePlanId, appointmentInfo, notesInfo);
    }

    private String buildCarePlanCreatedTextContent(String patientName, String doctorName, String carePlanId, String appointmentId, String consultationNotes) {
        String appointmentInfo = appointmentId != null ? String.format("Related Appointment: %s\n", appointmentId) : "";
        String notesInfo = consultationNotes != null && !consultationNotes.isBlank() 
            ? String.format("Consultation Notes: %s\n", consultationNotes) 
            : "";
        return String.format(
                "New Care Plan Created!\n\n" +
                        "Dear %s,\n\n" +
                        "%s has created a new care plan for you.\n\n" +
                        "Care Plan Details:\n" +
                        "Care Plan ID: %s\n" +
                        "%s" +
                        "%s" +
                        "Please log in to your dashboard to view your complete care plan details, including medications and instructions.\n\n" +
                        "Best regards,\n" +
                        "Healthcare Team",
                patientName, doctorName, carePlanId, appointmentInfo, notesInfo);
    }
}
