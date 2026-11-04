package me.geetprince.Eventify.repository;

import me.geetprince.Eventify.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    List<Registration> findByEvent_Id(UUID eventId);

    boolean existsByEvent_IdAndAttendeeEmail(UUID id, String attendeeEmail);
}
