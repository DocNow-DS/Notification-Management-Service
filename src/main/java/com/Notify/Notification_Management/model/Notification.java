package com.Notify.Notification_Management.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Field
    private String recipientId;

    @Field
    private String recipientType; // PATIENT, DOCTOR

    @Field
    private String message;

    @Field
    private String notificationType; // APPOINTMENT_CREATED, APPOINTMENT_APPROVED

    @Field
    private Boolean isRead = false;

    @Field
    private LocalDateTime createdAt;

    @Field
    private LocalDateTime readAt;

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(String recipientId, String recipientType, String message, String notificationType) {
        this();
        this.recipientId = recipientId;
        this.recipientType = recipientType;
        this.message = message;
        this.notificationType = notificationType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        if (isRead && this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
