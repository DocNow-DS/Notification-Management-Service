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

    private String doctorId;
    
    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;
    
    @NotBlank(message = "Notification type is required")
    private String notificationType; // APPOINTMENT_CREATED, APPOINTMENT_APPROVED, APPOINTMENT_DECLINED
    
    private String startTime;
    
    private String reason; // For APPOINTMENT_DECLINED notifications

    public String getNotificationType() {
        return notificationType;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getReason() {
        return reason;
    }

    public String getDoctorId() {
        return doctorId;
    }
}
