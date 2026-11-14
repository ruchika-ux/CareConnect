package com.careconnect.dto;

import com.careconnect.model.User;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Request/response shapes for the REST API. */
public final class Dtos {
    private Dtos() {}

    // ---- inputs
    public record AuthInput(@Email @NotBlank String email, @NotBlank String password) {}
    public record RegisterInput(@NotBlank String name, @Email @NotBlank String email,
                                @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
                                @NotNull User.Role role, String phone, String bio, LocalDate dateOfBirth) {}
    public record ProfileInput(String name, String phone, String bio, String serviceArea, Integer experienceYears,
                               Boolean available, List<Long> serviceIds, LocalDate dateOfBirth) {}
    public record AvailabilityInput(@Min(1) @Max(7) int dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record RequestInput(@NotNull Long serviceId, @NotNull LocalDate requestedDate, @NotNull LocalTime startTime,
                               @Min(30) @Max(720) int durationMinutes, @NotBlank String location,
                               @Size(max = 2000) String careRequirements) {}
    public record DecisionInput(@NotBlank String action) {}
    public record VisitInput(@Size(max = 2000) String notes) {}
    public record ReviewInput(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment) {}

    // ---- views
    public record UserView(long id, String name, String email, String role, String phone, String bio, boolean available,
                           boolean verified, boolean enabled, String serviceArea, int experienceYears, double rating,
                           int reviewCount, LocalDate dateOfBirth, List<Long> serviceIds, List<String> services) {}
    public record ServiceView(long id, String name, String description) {}
    public record AvailabilityView(int dayOfWeek, LocalTime startTime, LocalTime endTime) {}
    public record MatchView(UserView caregiver, int score, Map<String, Integer> breakdown, List<String> reasons) {}
    public record RequestView(long id, long patientId, String patientName, long serviceId, String serviceName,
                              LocalDate requestedDate, LocalTime startTime, int durationMinutes, String location,
                              String careRequirements, String status, Long caregiverId, String caregiverName,
                              String matchReason, String visitNotes, boolean reviewed, Integer reviewRating,
                              String reviewComment) {}
    public record ReviewView(long id, long requestId, String patientName, String caregiverName, int rating, String comment) {}
    public record CaregiverDetail(UserView caregiver, List<AvailabilityView> availability, List<ReviewView> reviews) {}
}
