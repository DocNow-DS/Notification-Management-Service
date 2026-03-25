package com.Notify.Notification_Management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Notify.Notification_Management.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(String recipientId, String recipientType);

    List<Notification> findByRecipientIdAndRecipientTypeAndIsReadFalseOrderByCreatedAtDesc(String recipientId, String recipientType);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.isRead = false")
    Long countUnreadNotifications(@Param("recipientId") String recipientId, @Param("recipientType") String recipientType);

    List<Notification> findByNotificationTypeOrderByCreatedAtDesc(String notificationType);
}
