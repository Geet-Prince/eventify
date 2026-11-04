package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.model.Registration;
import me.geetprince.Eventify.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/eventify/registrations")
public class RegistrationController {
    private final RegistrationService registrationService;
    @Autowired
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }
    @PostMapping
    public ResponseEntity<?> registerAttendee(@RequestBody Registration registration) {
        try {
            Registration savedRegistration = registrationService.registerAttendee(registration);
            return new ResponseEntity<>(savedRegistration, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Registration>> getRegistrationsForEvent(@PathVariable UUID eventId) {
        List<Registration> attendees = registrationService.getRegistrationsForEvent(eventId);
        return ResponseEntity.ok(attendees);
    }
    @PutMapping("/{id}/checkin")
    public ResponseEntity<?> checkInAttendee(@PathVariable UUID id) {
        try {
            Registration checkedInAttendee = registrationService.checkInAttendee(id);
            return ResponseEntity.ok(checkedInAttendee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
