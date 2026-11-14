package com.careconnect.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "care_requests", indexes = {
    @Index(name = "idx_booking_caregiver_slot", columnList = "caregiver_id, requested_date, start_time, status"),
    @Index(name = "idx_booking_patient", columnList = "patient_id")
})
public class CareRequest {
    public enum Status { REQUESTED, ACCEPTED, REJECTED, CANCELLED, IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceCatalog service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id")
    private User caregiver;

    @Column(nullable = false)
    private LocalDate requestedDate;

    @Column(nullable = false)
    private LocalTime startTime = LocalTime.of(9, 0);

    @Column(nullable = false)
    private int durationMinutes = 120;

    @Column(nullable = false)
    private String location = "";

    @Column(length = 2000)
    private String careRequirements = "";

    @Column(length = 2000)
    private String notes = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.REQUESTED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Column(length = 1000)
    private String matchReason = "";

    protected CareRequest() {}

    public CareRequest(User patient, ServiceCatalog service, LocalDate requestedDate, LocalTime startTime, int durationMinutes, String location, String careRequirements, String notes) {
        this.patient = patient;
        this.service = service;
        this.requestedDate = requestedDate;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.careRequirements = careRequirements;
        this.notes = notes;
        this.status = Status.REQUESTED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public ServiceCatalog getService() { return service; }
    public void setService(ServiceCatalog service) { this.service = service; }
    public User getCaregiver() { return caregiver; }
    public void setCaregiver(User caregiver) { this.caregiver = caregiver; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCareRequirements() { return careRequirements; }
    public void setCareRequirements(String careRequirements) { this.careRequirements = careRequirements; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
}
