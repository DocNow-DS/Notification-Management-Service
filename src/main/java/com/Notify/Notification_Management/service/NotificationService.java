package com.Notify.Notification_Management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Notify.Notification_Management.client.DoctorServiceClient;
import com.Notify.Notification_Management.client.PatientServiceClient;
import com.Notify.Notification_Management.dto.NotificationDto;
import com.Notify.Notification_Management.model.Notification;
import com.Notify.Notification_Management.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PatientServiceClient patientServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;

    public List<Notification> getUserNotifications(String userId, String userType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, userType);
    }

    public List<Notification> getUserNotificationsByUsernameOrId(String username, String userId, String userType) {
        // Query notifications where recipientId matches either username OR userId
        List<Notification> byUsername = notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(username, userType);
        List<Notification> byId = notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, userType);
        
        // Merge and remove duplicates
        java.util.Set<Notification> merged = new java.util.LinkedHashSet<>();
        merged.addAll(byUsername);
        merged.addAll(byId);
        
        return merged.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Notification> getUnreadNotifications(String userId, String userType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
    }

    public List<Notification> getUnreadNotificationsByUsernameOrId(String username, String userId, String userType) {
        // Query unread notifications where recipientId matches either username OR userId
        List<Notification> byUsername = notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(username, userType);
        List<Notification> byId = notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
        
        // Merge and remove duplicates
        java.util.Set<Notification> merged = new java.util.LinkedHashSet<>();
        merged.addAll(byUsername);
        merged.addAll(byId);
        
        return merged.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Long getUnreadCount(String userId, String userType) {
        return notificationRepository.countByRecipientIdAndRecipientTypeAndIsReadFalse(userId, userType);
    }

    public Long getUnreadCountByUsernameOrId(String username, String userId, String userType) {
        // Count unread notifications where recipientId matches either username OR userId
        Long byUsername = notificationRepository.countByRecipientIdAndRecipientTypeAndIsReadFalse(username, userType);
        Long byId = notificationRepository.countByRecipientIdAndRecipientTypeAndIsReadFalse(userId, userType);
        
        // If same recipientId appears in both counts, we might double count
        // Get actual notifications to count unique ones
        List<Notification> unreadByUsername = notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(username, userType);
        List<Notification> unreadById = notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
        
        java.util.Set<String> uniqueIds = new java.util.HashSet<>();
        unreadByUsername.forEach(n -> uniqueIds.add(n.getId()));
        unreadById.forEach(n -> uniqueIds.add(n.getId()));
        
        return (long) uniqueIds.size();
    }

    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional
    public Notification updateNotification(String id, NotificationDto notificationDto) {
        Notification notification = getNotificationById(id);
        
        if (notificationDto.getRecipientId() != null) {
            notification.setRecipientId(notificationDto.getRecipientId());
        }
        if (notificationDto.getRecipientType() != null) {
            notification.setRecipientType(notificationDto.getRecipientType());
        }
        if (notificationDto.getMessage() != null) {
            notification.setMessage(notificationDto.getMessage());
        }
        if (notificationDto.getNotificationType() != null) {
            notification.setNotificationType(notificationDto.getNotificationType());
        }
        if (notificationDto.getIsRead() != null) {
            notification.setIsRead(notificationDto.getIsRead());
        }
        
        return notificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(String id) {
        if (!notificationRepository.existsById(id)) {
            throw new RuntimeException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllUserNotifications(String userId, String userType) {
        List<Notification> notifications = notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, userType);
        notificationRepository.deleteAll(notifications);
    }

    @Transactional
    public void deleteReadNotifications(String userId, String userType) {
        List<Notification> readNotifications = notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, userType)
                .stream()
                .filter(Notification::getIsRead)
                .collect(Collectors.toList());
        notificationRepository.deleteAll(readNotifications);
    }

    @Transactional
    public Notification markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId, String userType) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId, userType);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void markAllAsReadByUsernameOrId(String username, String userId, String userType) {
        // Get unread notifications for both username and userId
        List<Notification> unreadByUsername = getUnreadNotifications(username, userType);
        List<Notification> unreadById = getUnreadNotifications(userId, userType);
        
        // Merge and remove duplicates
        java.util.Set<Notification> merged = new java.util.LinkedHashSet<>();
        merged.addAll(unreadByUsername);
        merged.addAll(unreadById);
        
        // Mark all as read
        merged.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(merged);
    }

    @Transactional
    public Notification createNotification(String recipientId, String recipientType, String message, String notificationType) {
        Notification notification = new Notification(recipientId, recipientType, message, notificationType);
        return notificationRepository.save(notification);
    }

    public void createAppointmentCreatedNotification(String patientId, String doctorId, String appointmentId, String startTime, String token) {
        String patientMessage = String.format("Your appointment has been scheduled for %s", startTime);
        String doctorMessage = String.format("New appointment scheduled with patient for %s", startTime);
        
        log.info("Creating appointment notification: patientId={}, doctorId={}, appointmentId={}", patientId, doctorId, appointmentId);
        
        // Create notification for patient (patientId is already the username/email)
        Notification patientNotification = createNotification(patientId, "PATIENT", patientMessage, "APPOINTMENT_CREATED");
        log.info("Created PATIENT notification with id: {}, recipientId: {}", patientNotification.getId(), patientNotification.getRecipientId());
        
        // Fetch doctor's profile to get the username (email) for the recipientId
        String doctorUsername = doctorId;
        log.info("Attempting to resolve doctor profile for doctorId: {}", doctorId);
        
        // First try doctor service
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> doctorProfile = (java.util.Map<String, Object>) doctorServiceClient.getDoctorById(doctorId, token);
            log.info("Doctor service response: {}", doctorProfile);
            if (doctorProfile != null && doctorProfile.get("username") != null) {
                doctorUsername = (String) doctorProfile.get("username");
                log.info("Resolved doctor {} to username via doctor service: {}", doctorId, doctorUsername);
            } else {
                log.warn("Doctor service returned null or no username for {}, will try patient service", doctorId);
                // Fallback to patient service directly
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> patientProfile = (java.util.Map<String, Object>) patientServiceClient.getPatientById(doctorId, token);
                log.info("Patient service response: {}", patientProfile);
                if (patientProfile != null && patientProfile.get("username") != null) {
                    doctorUsername = (String) patientProfile.get("username");
                    log.info("Resolved doctor {} to username via patient service: {}", doctorId, doctorUsername);
                } else {
                    log.warn("Could not resolve doctor {} from either service, using doctorId as fallback", doctorId);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching doctor profile from doctor service: {}, trying patient service", e.getMessage());
            // Fallback to patient service
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> patientProfile = (java.util.Map<String, Object>) patientServiceClient.getPatientById(doctorId, token);
                log.info("Patient service response: {}", patientProfile);
                if (patientProfile != null && patientProfile.get("username") != null) {
                    doctorUsername = (String) patientProfile.get("username");
                    log.info("Resolved doctor {} to username via patient service fallback: {}", doctorId, doctorUsername);
                } else {
                    log.warn("Patient service also returned null or no username for {}, using doctorId as fallback", doctorId);
                }
            } catch (Exception ex) {
                log.error("Error fetching doctor profile from patient service: {}, using doctorId as fallback", ex.getMessage());
            }
        }
        
        Notification doctorNotification = createNotification(doctorUsername, "DOCTOR", doctorMessage, "APPOINTMENT_CREATED");
        log.info("Created DOCTOR notification with id: {}, recipientId: {}, recipientType: {}", 
                 doctorNotification.getId(), doctorNotification.getRecipientId(), doctorNotification.getRecipientType());
        
        log.info("Created APPOINTMENT_CREATED notifications for patient {} and doctor {} (username: {}) for appointment {}", patientId, doctorId, doctorUsername, appointmentId);
        
        // Send email notification to doctor
        try {
            emailService.sendAppointmentCreatedEmailToDoctor(doctorId, patientId, appointmentId, startTime, token);
        } catch (Exception e) {
            log.error("Failed to send appointment created email to doctor {}: {}", doctorId, e.getMessage());
        }
        
        // Send WhatsApp notification to doctor
        try {
            whatsAppService.sendAppointmentCreatedWhatsAppToDoctor(doctorId, patientId, appointmentId, startTime, token);
        } catch (Exception e) {
            log.error("Failed to send appointment created WhatsApp message to doctor {}: {}", doctorId, e.getMessage());
        }
    }

    public void createAppointmentApprovedNotification(String patientId, String appointmentId, String startTime, String token) {
        String message = String.format("Your appointment on %s (ID: %s) has been approved by the doctor", 
            startTime != null ? startTime : "scheduled time", appointmentId);
        createNotification(patientId, "PATIENT", message, "APPOINTMENT_APPROVED");
        
        log.info("Created APPOINTMENT_APPROVED notification for patient: {}", patientId);
        
        // Send email notification
        try {
            emailService.sendAppointmentApprovedEmail(patientId, appointmentId, startTime, token);
        } catch (Exception e) {
            log.error("Failed to send appointment approved email to patient {}: {}", patientId, e.getMessage());
        }
        
        // Send SMS notification
        try {
            smsService.sendAppointmentApprovedSms(patientId, appointmentId, startTime, token);
        } catch (Exception e) {
            log.error("Failed to send appointment approved SMS to patient {}: {}", patientId, e.getMessage());
        }
        
        // Send WhatsApp notification
        try {
            whatsAppService.sendAppointmentApprovedWhatsApp(patientId, appointmentId, startTime, token);
        } catch (Exception e) {
            log.error("Failed to send appointment approved WhatsApp message to patient {}: {}", patientId, e.getMessage());
        }
    }

    public void createAppointmentDeclinedNotification(String patientId, String appointmentId, String startTime, String reason, String token) {
        String message = String.format("Your appointment on %s (ID: %s) has been declined by the doctor. Reason: %s", 
            startTime != null ? startTime : "scheduled time", appointmentId, reason != null ? reason : "Not specified");
        createNotification(patientId, "PATIENT", message, "APPOINTMENT_DECLINED");
        
        log.info("Created APPOINTMENT_DECLINED notification for patient: {}", patientId);
        
        // Send email notification (to be implemented in EmailService if needed)
        // try {
        //     emailService.sendAppointmentDeclinedEmail(patientId, appointmentId, startTime, reason, token);
        // } catch (Exception e) {
        //     log.error("Failed to send appointment declined email to patient {}: {}", patientId, e.getMessage());
        // }
        
        // Send SMS notification
        try {
            smsService.sendAppointmentDeclinedSms(patientId, appointmentId, startTime, reason, token);
        } catch (Exception e) {
            log.error("Failed to send appointment declined SMS to patient {}: {}", patientId, e.getMessage());
        }
    }

    public void createPaymentCompletedNotification(String patientId, String doctorId, String paymentId, String consultationId, Long amountCents, String currency, String token) {
        // Create notification message for doctor
        String amountFormatted = amountCents != null ? String.format("%.2f", amountCents / 100.0) : "N/A";
        String currencyCode = currency != null ? currency.toUpperCase() : "LKR";
        String doctorMessage = String.format("Payment received from patient for bill request. Amount: %s %s (Consultation: %s)", 
            amountFormatted, currencyCode, consultationId);
        
        log.info("Creating payment notification: patientId={}, doctorId={}, paymentId={}, amount={} {}", 
            patientId, doctorId, paymentId, amountFormatted, currencyCode);
        
        // Fetch doctor's profile to get the username (email) for the recipientId
        String doctorUsername = doctorId;
        log.info("Attempting to resolve doctor profile for doctorId: {}", doctorId);
        
        // First try doctor service
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> doctorProfile = (java.util.Map<String, Object>) doctorServiceClient.getDoctorById(doctorId, token);
            log.info("Doctor service response: {}", doctorProfile);
            if (doctorProfile != null && doctorProfile.get("username") != null) {
                doctorUsername = (String) doctorProfile.get("username");
                log.info("Resolved doctor {} to username via doctor service: {}", doctorId, doctorUsername);
            } else {
                log.warn("Doctor service returned null or no username for {}, will try patient service", doctorId);
                // Fallback to patient service directly
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> patientProfile = (java.util.Map<String, Object>) patientServiceClient.getPatientById(doctorId, token);
                log.info("Patient service response: {}", patientProfile);
                if (patientProfile != null && patientProfile.get("username") != null) {
                    doctorUsername = (String) patientProfile.get("username");
                    log.info("Resolved doctor {} to username via patient service: {}", doctorId, doctorUsername);
                } else {
                    log.warn("Could not resolve doctor {} from either service, using doctorId as fallback", doctorId);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching doctor profile from doctor service: {}, trying patient service", e.getMessage());
            // Fallback to patient service
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> patientProfile = (java.util.Map<String, Object>) patientServiceClient.getPatientById(doctorId, token);
                log.info("Patient service response: {}", patientProfile);
                if (patientProfile != null && patientProfile.get("username") != null) {
                    doctorUsername = (String) patientProfile.get("username");
                    log.info("Resolved doctor {} to username via patient service fallback: {}", doctorId, doctorUsername);
                } else {
                    log.warn("Patient service also returned null or no username for {}, using doctorId as fallback", doctorId);
                }
            } catch (Exception ex) {
                log.error("Error fetching doctor profile from patient service: {}, using doctorId as fallback", ex.getMessage());
            }
        }
        
        // Create notification for doctor
        Notification doctorNotification = createNotification(doctorUsername, "DOCTOR", doctorMessage, "PAYMENT_COMPLETED");
        log.info("Created DOCTOR notification with id: {}, recipientId: {}, recipientType: {}", 
                 doctorNotification.getId(), doctorNotification.getRecipientId(), doctorNotification.getRecipientType());
        
        log.info("Created PAYMENT_COMPLETED notification for doctor {} (username: {}) for payment {} from patient {}", 
            doctorId, doctorUsername, paymentId, patientId);
        
        // Send email notification to doctor
        try {
            emailService.sendPaymentCompletedEmailToDoctor(doctorId, patientId, paymentId, consultationId, amountCents, currency, token);
        } catch (Exception e) {
            log.error("Failed to send payment completed email to doctor {}: {}", doctorId, e.getMessage());
        }
    }

    public void createCarePlanCreatedNotification(String patientId, String doctorId, String carePlanId, String appointmentId, String consultationNotes, String token) {
        // Create notification message for patient
        String patientMessage = String.format("Your doctor has created a new care plan for you. Care Plan ID: %s", carePlanId);
        
        log.info("Creating care plan notification: patientId={}, doctorId={}, carePlanId={}", 
            patientId, doctorId, carePlanId);
        
        // Fetch patient's profile to get the username (email) for the recipientId
        String patientUsername = patientId;
        log.info("Attempting to resolve patient profile for patientId: {}", patientId);
        
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> patientProfile = (java.util.Map<String, Object>) patientServiceClient.getPatientById(patientId, token);
            log.info("Patient service response: {}", patientProfile);
            if (patientProfile != null && patientProfile.get("username") != null) {
                patientUsername = (String) patientProfile.get("username");
                log.info("Resolved patient {} to username via patient service: {}", patientId, patientUsername);
            } else {
                log.warn("Patient service returned null or no username for {}, using patientId as fallback", patientId);
            }
        } catch (Exception e) {
            log.error("Error fetching patient profile from patient service: {}, using patientId as fallback", e.getMessage());
        }
        
        // Create notification for patient
        Notification patientNotification = createNotification(patientUsername, "PATIENT", patientMessage, "CARE_PLAN_CREATED");
        log.info("Created PATIENT notification with id: {}, recipientId: {}, recipientType: {}", 
                 patientNotification.getId(), patientNotification.getRecipientId(), patientNotification.getRecipientType());
        
        log.info("Created CARE_PLAN_CREATED notification for patient {} (username: {}) for care plan {} from doctor {}", 
            patientId, patientUsername, carePlanId, doctorId);
        
        // Send email notification to patient
        try {
            emailService.sendCarePlanCreatedEmailToPatient(patientId, doctorId, carePlanId, appointmentId, consultationNotes, token);
        } catch (Exception e) {
            log.error("Failed to send care plan created email to patient {}: {}", patientId, e.getMessage());
        }
        
        // Send SMS notification to patient
        try {
            smsService.sendCarePlanCreatedSms(patientId, doctorId, carePlanId, token);
        } catch (Exception e) {
            log.error("Failed to send care plan created SMS to patient {}: {}", patientId, e.getMessage());
        }
    }

    public boolean validateUserToken(String token) {
        return patientServiceClient.validateToken(token);
    }

    public String extractUsernameFromToken(String token) {
        return patientServiceClient.extractUsernameFromToken(token);
    }
}
