package me.geetprince.Eventify.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
    @Column(nullable = false,length = 100)
    private String name;
    @Column(name = "start_time",nullable = false,columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;
    @Column(name="end_time",nullable = false,columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;
    @Column(name = "registration_deadline",nullable = false,columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime registrationDeadline;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(name = "is_Published",nullable = false)
    private boolean isPublic=false;
    @Column(nullable = false,length = 300)
    private String venue;
    @Column(columnDefinition = "TEXT")
    private String guests;
    @Column(name = "image_link")
    private String imageLink;
    @CreationTimestamp
    @Column(name = "created_at",updatable = false,columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;
    @Column(name = "allowed_departments")
    private String allowedDepartments;

    @Column(name = "allowed_study_years")
    private String allowedStudyYears;
}
