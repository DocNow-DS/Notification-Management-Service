package com.Notify.Notification_Management.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Notify.Notification_Management.client.PatientServiceClient;
import com.Notify.Notification_Management.dto.AppointmentNotificationRequest;
import com.Notify.Notification_Management.dto.CarePlanNotificationRequest;
import com.Notify.Notification_Management.dto.NotificationDto;
import com.Notify.Notification_Management.dto.PaymentNotificationRequest;
import com.Notify.Notification_Management.model.Notification;
import com.Notify.Notification_Management.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final PatientServiceClient patientServiceClient;

    // CREATE Operations
    
    @PostMapping
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody NotificationDto notificationDto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.createNotification(
                notificationDto.getRecipientId(),
                notificationDto.getRecipientType(),
                notificationDto.getMessage(),
                notificationDto.getNotificationType()
            );
            
            log.info("Created notification for recipient {}: {}", notificationDto.getRecipientId(), notificationDto.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(notification));
        } catch (Exception e) {
            log.error("Error creating notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/appointment")
    public ResponseEntity<String> createAppointmentNotification(
            @Valid @RequestBody AppointmentNotificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        // Authorization is required for appointment notifications to access patient service
        if (authorization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is required for appointment notifications");
        }
        
        if (!notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            switch (request.getNotificationType()) {
                case "APPOINTMENT_CREATED":
                    String createdToken = authorization.replace("Bearer ", "");
                    notificationService.createAppointmentCreatedNotification(
                        request.getPatientId(),
                        request.getDoctorId(),
                        request.getAppointmentId(),
                        request.getStartTime(),
                        createdToken
                    );
                    break;
                case "APPOINTMENT_APPROVED":
                    String token = authorization.replace("Bearer ", "");
                    notificationService.createAppointmentApprovedNotification(
                        request.getPatientId(),
                        request.getAppointmentId(),
                        request.getStartTime(),
                        token
                    );
                    break;
                case "APPOINTMENT_DECLINED":
                    String declineToken = authorization.replace("Bearer ", "");
                    notificationService.createAppointmentDeclinedNotification(
                        request.getPatientId(),
                        request.getAppointmentId(),
                        request.getStartTime(),
                        request.getReason(),
                        declineToken
                    );
                    break;
                default:
                    return ResponseEntity.badRequest().body("Invalid notification type");
            }
            
            log.info("Created {} notification for patient {} for appointment {}", 
                request.getNotificationType(), request.getPatientId(), request.getAppointmentId());
            
            return ResponseEntity.ok("Notification created successfully");
        } catch (Exception e) {
            log.error("Error creating notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating notification");
        }
    }

    @PostMapping("/payment")
    public ResponseEntity<String> createPaymentNotification(
            @Valid @RequestBody PaymentNotificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        // Token validation is optional for payment notifications (e.g., from webhooks)
        String token = null;
        if (authorization != null && !authorization.isBlank()) {
            token = authorization.replace("Bearer ", "");
            if (!notificationService.validateUserToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            notificationService.createPaymentCompletedNotification(
                request.getPatientId(),
                request.getDoctorId(),
                request.getPaymentId(),
                request.getConsultationId(),
                request.getAmountCents(),
                request.getCurrency(),
                token
            );
            
            log.info("Created payment notification for doctor {} from patient {} for payment {}", 
                request.getDoctorId(), request.getPatientId(), request.getPaymentId());
            
            return ResponseEntity.ok("Payment notification created successfully");
        } catch (Exception e) {
            log.error("Error creating payment notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating payment notification");
        }
    }

    @PostMapping("/care-plan")
    public ResponseEntity<String> createCarePlanNotification(
            @Valid @RequestBody CarePlanNotificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        // Token validation is optional for care plan notifications (e.g., from internal services)
        String token = null;
        if (authorization != null && !authorization.isBlank()) {
            token = authorization.replace("Bearer ", "");
            if (!notificationService.validateUserToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            notificationService.createCarePlanCreatedNotification(
                request.getPatientId(),
                request.getDoctorId(),
                request.getCarePlanId(),
                request.getAppointmentId(),
                request.getConsultationNotes(),
                token
            );
            
            log.info("Created care plan notification for patient {} from doctor {} for care plan {}", 
                request.getPatientId(), request.getDoctorId(), request.getCarePlanId());
            
            return ResponseEntity.ok("Care plan notification created successfully");
        } catch (Exception e) {
            log.error("Error creating care plan notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating care plan notification");
        }
    }

    // READ Operations

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotificationById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.getNotificationById(id);
            return ResponseEntity.ok(convertToDto(notification));
        } catch (RuntimeException e) {
            log.error("Notification not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications = notificationService.getAllNotifications();
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDtos);
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            @RequestParam(defaultValue = "PATIENT") String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.replace("Bearer ", "");
        if (!notificationService.validateUserToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = notificationService.extractUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Also try to get userId from patient service (it may be the same as username or an ObjectId)
        String userId = username; // Default to username
        try {
            // Try to resolve userId from patient service using the username
            var patientProfile = patientServiceClient.getPatientById(username, token);
            if (patientProfile != null) {
                // Try to extract id from profile
                if (patientProfile instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> profile = (java.util.Map<String, Object>) patientProfile;
                    if (profile.get("id") != null) {
                        userId = (String) profile.get("id");
                    } else if (profile.get("_id") != null) {
                        userId = (String) profile.get("_id");
                    }
                }
            }
        } catch (Exception e) {
            // If patient service fails, use username as userId fallback
            log.debug("Could not resolve userId from patient service, using username: {}", username);
        }

        List<Notification> notifications = notificationService.getUserNotificationsByUsernameOrId(username, userId, userType);
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDtos);
    }

    @GetMapping("/me/unread")
    public ResponseEntity<List<NotificationDto>> getMyUnreadNotifications(
            @RequestParam(defaultValue = "PATIENT") String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.replace("Bearer ", "");
        if (!notificationService.validateUserToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = notificationService.extractUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Also try to get userId from patient service (it may be the same as username or an ObjectId)
        String userId = username; // Default to username
        try {
            // Try to resolve userId from patient service using the username
            var patientProfile = patientServiceClient.getPatientById(username, token);
            if (patientProfile != null) {
                // Try to extract id from profile
                if (patientProfile instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> profile = (java.util.Map<String, Object>) patientProfile;
                    if (profile.get("id") != null) {
                        userId = (String) profile.get("id");
                    } else if (profile.get("_id") != null) {
                        userId = (String) profile.get("_id");
                    }
                }
            }
        } catch (Exception e) {
            // If patient service fails, use username as userId fallback
            log.debug("Could not resolve userId from patient service, using username: {}", username);
        }

        List<Notification> notifications = notificationService.getUnreadNotificationsByUsernameOrId(username, userId, userType);
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDtos);
    }

    @GetMapping("/me/unread/count")
    public ResponseEntity<Long> getMyUnreadCount(
            @RequestParam(defaultValue = "PATIENT") String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.replace("Bearer ", "");
        if (!notificationService.validateUserToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = notificationService.extractUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Also try to get userId from patient service (it may be the same as username or an ObjectId)
        String userId = username; // Default to username
        try {
            // Try to resolve userId from patient service using the username
            var patientProfile = patientServiceClient.getPatientById(username, token);
            if (patientProfile != null) {
                // Try to extract id from profile
                if (patientProfile instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> profile = (java.util.Map<String, Object>) patientProfile;
                    if (profile.get("id") != null) {
                        userId = (String) profile.get("id");
                    } else if (profile.get("_id") != null) {
                        userId = (String) profile.get("_id");
                    }
                }
            }
        } catch (Exception e) {
            // If patient service fails, use username as userId fallback
            log.debug("Could not resolve userId from patient service, using username: {}", username);
        }

        Long count = notificationService.getUnreadCountByUsernameOrId(username, userId, userType);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<Void> markAllMyAsRead(
            @RequestParam(defaultValue = "PATIENT") String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || authorization.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorization.replace("Bearer ", "");
        if (!notificationService.validateUserToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = notificationService.extractUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Also try to get userId from patient service (it may be the same as username or an ObjectId)
        String userId = username; // Default to username
        try {
            // Try to resolve userId from patient service using the username
            var patientProfile = patientServiceClient.getPatientById(username, token);
            if (patientProfile != null) {
                // Try to extract id from profile
                if (patientProfile instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> profile = (java.util.Map<String, Object>) patientProfile;
                    if (profile.get("id") != null) {
                        userId = (String) profile.get("id");
                    } else if (profile.get("_id") != null) {
                        userId = (String) profile.get("_id");
                    }
                }
            }
        } catch (Exception e) {
            // If patient service fails, use username as userId fallback
            log.debug("Could not resolve userId from patient service, using username: {}", username);
        }

        notificationService.markAllAsReadByUsernameOrId(username, userId, userType);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications = notificationService.getUserNotifications(userId, userType);
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDtos);
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications = notificationService.getUnreadNotifications(userId, userType);
        List<NotificationDto> notificationDtos = notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDtos);
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long count = notificationService.getUnreadCount(userId, userType);
        return ResponseEntity.ok(count);
    }

    // UPDATE Operations

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification notification = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(convertToDto(notification));
        } catch (RuntimeException e) {
            log.error("Notification not found with id: {}", notificationId);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationDto> updateNotification(
            @PathVariable String id,
            @Valid @RequestBody NotificationDto notificationDto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Notification updatedNotification = notificationService.updateNotification(id, notificationDto);
            log.info("Updated notification with id: {}", id);
            return ResponseEntity.ok(convertToDto(updatedNotification));
        } catch (RuntimeException e) {
            log.error("Notification not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        notificationService.markAllAsRead(userId, userType);
        return ResponseEntity.ok().build();
    }

    // DELETE Operations

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            notificationService.deleteNotification(id);
            log.info("Deleted notification with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Notification not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllUserNotifications(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        notificationService.deleteAllUserNotifications(userId, userType);
        log.info("Deleted all notifications for user {} of type {}", userId, userType);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}/read")
    public ResponseEntity<Void> deleteReadNotifications(
            @PathVariable String userId,
            @RequestParam String userType,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        notificationService.deleteReadNotifications(userId, userType);
        log.info("Deleted read notifications for user {} of type {}", userId, userType);
        return ResponseEntity.noContent().build();
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setRecipientId(notification.getRecipientId());
        dto.setRecipientType(notification.getRecipientType());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}
