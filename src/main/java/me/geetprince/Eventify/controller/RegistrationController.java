package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.dto.RegistrationResponseDTO;
import me.geetprince.Eventify.model.Registration;
import me.geetprince.Eventify.service.RegistrationService;
import me.geetprince.Eventify.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    // 1. Create: Attendee Registers -> Gets QR Code in Response (Public)
    @PostMapping
    public ResponseEntity<?> registerAttendee(@RequestBody Registration registration) {
        Registration savedRegistration = registrationService.registerAttendee(registration);
        String qrCode = QRCodeGenerator.generateQRCodeBase64(savedRegistration.getId().toString());

        RegistrationResponseDTO dto = new RegistrationResponseDTO();
        dto.setId(savedRegistration.getId());
        dto.setStudentName(savedRegistration.getAttendeeName());
        dto.setStudentEmail(savedRegistration.getAttendeeEmail());
        dto.setCheckedIn(savedRegistration.getCheckedIn());
        dto.setQrCodeBase64(qrCode);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    // 2. Read: Get all registrations for a specific event (Protected - Owner Club only)
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Registration>> getRegistrationsForEvent(@PathVariable UUID eventId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Registration> attendees = registrationService.getRegistrationsForEvent(eventId, principal.getName());
        return ResponseEntity.ok(attendees);
    }

    // 3. Update: Authorized Club scans QR Code -> Checks attendee in (Protected - Owner Club only)
    @PutMapping("/{id}/check-in")
    public ResponseEntity<?> checkInAttendee(@PathVariable UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Registration updatedRegistration = registrationService.checkInAttendee(id, principal.getName());
        return ResponseEntity.ok("Successfully checked in: " + updatedRegistration.getAttendeeName());
    }

    // 4. Export: Download attendees CSV (Protected - Owner Club only)
    @GetMapping("/event/{eventId}/export")
    public ResponseEntity<String> exportRegistrations(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer studyYear,
            @RequestParam(required = false) Boolean checkedIn,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String csvData = registrationService.exportFilteredRegistrationsToCSV(
                eventId, department, studyYear, checkedIn, principal.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event_" + eventId + "_attendance.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csvData);
    }
}
