package com.careconnect.model;

import jakarta.persistence.*;
import java.time.LocalTime;

/** One weekly working window for a caregiver (dayOfWeek: 1=Monday ... 7=Sunday). */
@Entity
@Table(name = "caregiver_availability", indexes = @Index(name = "idx_availability_caregiver", columnList = "caregiver_id"))
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private User caregiver;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    protected Availability() {}

    public Availability(User caregiver, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.caregiver = caregiver;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public User getCaregiver() { return caregiver; }
    public int getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}
