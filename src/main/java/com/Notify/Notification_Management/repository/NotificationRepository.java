package com.Notify.Notification_Management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.Notify.Notification_Management.model.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(String recipientId, String recipientType);

    List<Notification> findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(String recipientId, String recipientType);

    Long countByRecipientIdAndRecipientTypeAndIsReadFalse(String recipientId, String recipientType);

    List<Notification> findByNotificationTypeOrderByCreatedAtDesc(String notificationType);
}
