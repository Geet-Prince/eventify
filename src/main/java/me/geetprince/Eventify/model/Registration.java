package me.geetprince.Eventify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_registrations_event_attendee_email", columnNames = {"event_id", "attendee_email"})
})
@Data
@NoArgsConstructor
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(name = "attendee_name", nullable = false, length = 100)
    private String attendeeName;
    @Column(name = "attendee_email", nullable = false, length = 150)
    private String attendeeEmail;
    @Column(name = "roll_number", length = 50)
    private String rollNumber;
    @Column(name = "department", length = 100)
    private String department;
    @Column(name = "study_year")
    @Min(value = 1, message = "Study year must be at least 1")
    @Max(value = 5, message = "Study year cannot exceed 5")
    private Integer studyYear;

    @Column(name = "is_checked_in", nullable = false)
    private Boolean checkedIn = false;
    @CreationTimestamp
    @Column(name = "registered_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime registeredAt;
    @Column(name = "allowed_departments")
    private String allowedDepartments;
    @Column(name = "allowed_study_years")
    private String allowedStudyYears;

}
