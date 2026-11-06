package me.geetprince.Eventify.dto;

import lombok.Data;
import me.geetprince.Eventify.model.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class EventResponseDTO {
    private UUID id;
    private String name;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime registrationDeadline;
    private String details;
    @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
    private boolean isPublic;
    private String venue;
    private String guests;
    private String imageLink;
    private OffsetDateTime createdAt;
    private String allowedDepartments;
    private String allowedStudyYears;
    private ClubResponseDTO club;

    public static EventResponseDTO fromEntity(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setRegistrationDeadline(event.getRegistrationDeadline());
        dto.setDetails(event.getDetails());
        dto.setPublic(event.isPublic());
        dto.setVenue(event.getVenue());
        dto.setGuests(event.getGuests());
        dto.setImageLink(event.getImageLink());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setAllowedDepartments(event.getAllowedDepartments());
        dto.setAllowedStudyYears(event.getAllowedStudyYears());
        if (event.getClub() != null) {
            dto.setClub(ClubResponseDTO.fromEntity(event.getClub()));
        }
        return dto;
    }
}
