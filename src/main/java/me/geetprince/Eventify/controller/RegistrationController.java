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

    // 1. Create: Attendee Registers -> Gets QR Code in Response
    @PostMapping
    public ResponseEntity<?> registerAttendee(@RequestBody Registration registration) {
        // Saves the registration using your service
        Registration savedRegistration = registrationService.registerAttendee(registration);

        // Generates the Base64 QR Code using the saved UUID
        String qrCode = QRCodeGenerator.generateQRCodeBase64(savedRegistration.getId().toString());

        // Maps the data to the safe DTO that includes the QR image string
        RegistrationResponseDTO dto = new RegistrationResponseDTO();
        dto.setId(savedRegistration.getId());
        dto.setStudentName(savedRegistration.getAttendeeName());
        dto.setStudentEmail(savedRegistration.getAttendeeEmail());
        dto.setCheckedIn(savedRegistration.getCheckedIn());
        dto.setQrCodeBase64(qrCode);

        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    // 2. Read: Get all registrations for a specific event
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Registration>> getRegistrationsForEvent(@PathVariable UUID eventId) {
        List<Registration> attendees = registrationService.getRegistrationsForEvent(eventId);
        return ResponseEntity.ok(attendees);
    }

    // 3. Update: Security Scans QR Code -> Hits this endpoint to check them in
    @PutMapping("/{id}/check-in")
    public ResponseEntity<?> checkInAttendee(@PathVariable UUID id) {
        // Updates the checkedIn status using your service
        Registration updatedRegistration = registrationService.checkInAttendee(id);
        return ResponseEntity.ok("Successfully checked in: " + updatedRegistration.getAttendeeName());
    }
    @GetMapping("/event/{eventId}/export")
    public ResponseEntity<String> exportRegistrations(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer studyYear,
            @RequestParam(required = false) Boolean checkedIn) {

        String csvData = registrationService.exportFilteredRegistrationsToCSV(eventId, department, studyYear, checkedIn);

        // Tell the client to download this as a CSV file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event_" + eventId + "_attendance.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csvData);
    }
}