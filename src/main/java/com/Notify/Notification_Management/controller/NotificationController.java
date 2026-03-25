package com.Notify.Notification_Management.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Notify.Notification_Management.dto.AppointmentNotificationRequest;
import com.Notify.Notification_Management.dto.NotificationDto;
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
        
        if (authorization != null && !notificationService.validateUserToken(authorization.replace("Bearer ", ""))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            switch (request.getNotificationType()) {
                case "APPOINTMENT_CREATED":
                    notificationService.createAppointmentCreatedNotification(
                        request.getPatientId(), 
                        request.getDoctorId(), 
                        request.getAppointmentTime()
                    );
                    break;
                case "APPOINTMENT_APPROVED":
                    notificationService.createAppointmentApprovedNotification(
                        request.getPatientId(), 
                        request.getAppointmentTime()
                    );
                    break;
                default:
                    return ResponseEntity.badRequest().body("Invalid notification type");
            }
            
            log.info("Created {} notification for patient {} and doctor {}", 
                request.getNotificationType(), request.getPatientId(), request.getDoctorId());
            
            return ResponseEntity.ok("Notification created successfully");
        } catch (Exception e) {
            log.error("Error creating notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating notification");
        }
    }

    // READ Operations

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotificationById(
            @PathVariable Long id,
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
            @PathVariable Long notificationId,
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
            @PathVariable Long id,
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
            @PathVariable Long id,
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
