package com.Notify.Notification_Management.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String recipientId;
    private String recipientType;
    private String message;
    private String notificationType;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
