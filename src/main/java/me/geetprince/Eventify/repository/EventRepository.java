package me.geetprince.Eventify.repository;

import me.geetprince.Eventify.model.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"club"})
    List<Event> findByIsPublicTrue();

    @EntityGraph(attributePaths = {"club"})
    Optional<Event> findById(UUID id);

    @EntityGraph(attributePaths = {"club"})
    List<Event> findByNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = {"club"})
    List<Event> findByClub_NameContainingIgnoreCase(String clubName);

    @EntityGraph(attributePaths = {"club"})
    List<Event> findByClub_Id(UUID clubId);
}

