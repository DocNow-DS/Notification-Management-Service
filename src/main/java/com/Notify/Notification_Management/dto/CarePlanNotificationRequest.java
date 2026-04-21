package com.Notify.Notification_Management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanNotificationRequest {
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;
    
    @NotBlank(message = "Care Plan ID is required")
    private String carePlanId;
    
    private String appointmentId;
    
    private String consultationNotes;

    public String getPatientId() {
        return patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getCarePlanId() {
        return carePlanId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getConsultationNotes() {
        return consultationNotes;
    }
}
