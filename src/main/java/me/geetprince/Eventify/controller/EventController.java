package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.dto.EventResponseDTO;
import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.model.Event;
import me.geetprince.Eventify.service.ClubService;
import me.geetprince.Eventify.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/eventify/events")
public class EventController {
    private final EventService eventService;
    private final ClubService clubService;

    @Autowired
    public EventController(EventService eventService, ClubService clubService) {
        this.eventService = eventService;
        this.clubService = clubService;
    }

    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(@RequestBody Event event, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Club authenticatedClub = clubService.getClubByEmail(principal.getName());
        event.setClub(authenticatedClub);
        Event savedEvent = eventService.createEvent(event);
        return new ResponseEntity<>(EventResponseDTO.fromEntity(savedEvent), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        List<EventResponseDTO> dtos = eventService.getPublicEvents().stream()
                .map(EventResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getEventById(@PathVariable UUID id, Principal principal) {
        try {
            Event event = eventService.getEventById(id);
            if (!event.isPublic()) {
                if (principal == null || !event.getClub().getEmail().equalsIgnoreCase(principal.getName())) {
                    return ResponseEntity.notFound().build();
                }
            }
            return ResponseEntity.ok(EventResponseDTO.fromEntity(event));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> updateEvent(@PathVariable UUID id, @RequestBody Event eventDetails, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Event updated = eventService.updateEvent(id, eventDetails, principal.getName());
            return ResponseEntity.ok(EventResponseDTO.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            eventService.deleteEvent(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
