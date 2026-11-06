package me.geetprince.Eventify.repository;

import me.geetprince.Eventify.model.Registration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    @EntityGraph(attributePaths = {"event", "event.club"})
    Optional<Registration> findById(UUID id);

    @EntityGraph(attributePaths = {"event", "event.club"})
    List<Registration> findByEvent_Id(UUID eventId);

    boolean existsByEvent_IdAndAttendeeEmail(UUID id, String attendeeEmail);
}

