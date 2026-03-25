# Notification Management Service

A Spring Boot microservice for managing notifications in the healthcare appointment system. This service handles creating and retrieving notifications for patients and doctors when appointments are created and approved.

## Features

- **Authentication Integration**: Uses RestTemplate to validate tokens with the patient management service
- **Appointment Notifications**: Automatically creates notifications when:
  - New appointments are created (notifies both patient and doctor)
  - Appointments are approved by doctors (notifies patient)
- **Notification Management**: Mark notifications as read, get unread count, retrieve user notifications
- **RESTful API**: Full CRUD operations for notifications

## API Endpoints

### CRUD Operations

#### CREATE
- `POST /api/notifications` - Create a new notification
  ```json
  {
    "recipientId": "user123",
    "recipientType": "PATIENT",
    "message": "Your appointment has been confirmed",
    "notificationType": "APPOINTMENT_APPROVED"
  }
  ```
- `POST /api/notifications/appointment` - Create appointment notification
  ```json
  {
    "patientId": "patient123",
    "doctorId": "doctor456",
    "appointmentTime": "2024-03-25T10:30:00",
    "notificationType": "APPOINTMENT_CREATED" // or "APPOINTMENT_APPROVED"
  }
  ```

#### READ
- `GET /api/notifications` - Get all notifications
- `GET /api/notifications/{id}` - Get notification by ID
- `GET /api/notifications/user/{userId}?userType={PATIENT|DOCTOR}` - Get all user notifications
- `GET /api/notifications/user/{userId}/unread?userType={PATIENT|DOCTOR}` - Get unread notifications
- `GET /api/notifications/user/{userId}/unread/count?userType={PATIENT|DOCTOR}` - Get unread count

#### UPDATE
- `PUT /api/notifications/{id}` - Update notification
- `PUT /api/notifications/{notificationId}/read` - Mark notification as read
- `PUT /api/notifications/user/{userId}/read-all?userType={PATIENT|DOCTOR}` - Mark all as read

#### DELETE
- `DELETE /api/notifications/{id}` - Delete notification by ID
- `DELETE /api/notifications/user/{userId}?userType={PATIENT|DOCTOR}` - Delete all user notifications
- `DELETE /api/notifications/user/{userId}/read?userType={PATIENT|DOCTOR}` - Delete read notifications

## Authentication

All endpoints support optional Bearer token authentication. When provided, the token is validated against the patient management service:

```
Authorization: Bearer <jwt_token>
```

## Configuration

The service runs on port 8084 and connects to:
- Patient Management Service: http://localhost:8081
- Doctor Management Service: http://localhost:8082
- Appointment Management Service: http://localhost:8083

## Database

Uses H2 in-memory database with console enabled at: http://localhost:8084/h2-console
- URL: jdbc:h2:mem:testdb
- Username: sa
- Password: password

## Running the Service

```bash
mvn spring-boot:run
```

## Integration with Other Services

### Appointment Creation
When an appointment is created in the Appointment Management Service, call:
```bash
POST /api/notifications/appointment
{
  "patientId": "patient123",
  "doctorId": "doctor456", 
  "appointmentTime": "2024-03-25T10:30:00",
  "notificationType": "APPOINTMENT_CREATED"
}
```

### Appointment Approval
When a doctor approves an appointment, call:
```bash
POST /api/notifications/appointment
{
  "patientId": "patient123",
  "doctorId": "doctor456",
  "appointmentTime": "2024-03-25T10:30:00", 
  "notificationType": "APPOINTMENT_APPROVED"
}
```
