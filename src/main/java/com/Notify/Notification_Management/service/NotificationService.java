package com.Notify.Notification_Management.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;

    public List<Notification> getUserNotifications(String userId, String userType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(userId, userType);
    }

    public List<Notification> getUnreadNotifications(String userId, String userType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
    }

    public Long getUnreadCount(String userId, String userType) {
        return notificationRepository.countUnreadNotifications(userId, userType);
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
    public Notification createNotification(String recipientId, String recipientType, String message, String notificationType) {
        Notification notification = new Notification(recipientId, recipientType, message, notificationType);
        return notificationRepository.save(notification);
    }

    public void createAppointmentCreatedNotification(String patientId, String doctorId, LocalDateTime appointmentTime) {
        String patientMessage = String.format("Your appointment has been scheduled for %s", appointmentTime);
        String doctorMessage = String.format("New appointment scheduled with patient for %s", appointmentTime);
        
        createNotification(patientId, "PATIENT", patientMessage, "APPOINTMENT_CREATED");
        createNotification(doctorId, "DOCTOR", doctorMessage, "APPOINTMENT_CREATED");
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

    public boolean validateUserToken(String token) {
        return patientServiceClient.validateToken(token);
    }

    public String extractUsernameFromToken(String token) {
        return patientServiceClient.extractUsernameFromToken(token);
    }
}
