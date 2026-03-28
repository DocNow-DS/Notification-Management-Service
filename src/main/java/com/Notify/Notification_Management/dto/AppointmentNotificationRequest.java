package com.Notify.Notification_Management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationRequest {
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;
    
    @NotBlank(message = "Notification type is required")
    private String notificationType; // APPOINTMENT_CREATED, APPOINTMENT_APPROVED
    
    private String startTime;
}
