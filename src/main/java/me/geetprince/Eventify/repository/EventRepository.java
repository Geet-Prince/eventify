package me.geetprince.Eventify.repository;

import me.geetprince.Eventify.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByIsPublicTrue();

    List<Event> findByNameContainingIgnoreCase(String name);

    List<Event> findByClub_NameContainingIgnoreCase(String clubName);
}
