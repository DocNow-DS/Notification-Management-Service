package com.Notify.Notification_Management.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Notify.Notification_Management.model.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(String recipientId, String recipientType);

    List<Notification> findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(String recipientId, String recipientType);

    @Query("{ 'recipientId' : ?0, 'recipientType' : ?1, 'isRead' : false }")
    Long countUnreadNotifications(@Param("recipientId") String recipientId, @Param("recipientType") String recipientType);

    List<Notification> findByNotificationTypeOrderByCreatedAtDesc(String notificationType);
}
