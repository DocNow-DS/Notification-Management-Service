package com.Notify.Notification_Management.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationRequest {
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;
    
    @NotNull(message = "Appointment time is required")
    private LocalDateTime appointmentTime;
    
    @NotBlank(message = "Notification type is required")
    private String notificationType; // APPOINTMENT_CREATED, APPOINTMENT_APPROVED
}
