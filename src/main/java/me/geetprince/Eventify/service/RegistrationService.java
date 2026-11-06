package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Event;
import me.geetprince.Eventify.model.Registration;
import me.geetprince.Eventify.repository.EventRepository;
import me.geetprince.Eventify.repository.RegistrationRepository;
import me.geetprince.Eventify.util.CSVExportUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RegistrationService {

    private final RegistrationRepository registrationRepo;
    private final EventRepository eventRepo;

    @Autowired
    public RegistrationService(RegistrationRepository registrationRepo, EventRepository eventRepo) {
        this.registrationRepo = registrationRepo;
        this.eventRepo = eventRepo;
    }

    public Registration registerAttendee(Registration registration) {
        Event event = eventRepo.findById(registration.getEvent().getId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        String allowedDepts = event.getAllowedDepartments();
        if (allowedDepts != null && !allowedDepts.isBlank()) {
            boolean deptOk = java.util.Arrays.stream(allowedDepts.split(","))
                    .map(String::trim)
                    .anyMatch(d -> d.equalsIgnoreCase(registration.getDepartment()));
            if (!deptOk) {
                throw new IllegalArgumentException("Your department is not eligible for this event.");
            }
        }

        String allowedYears = event.getAllowedStudyYears();
        if (allowedYears != null && !allowedYears.isBlank()) {
            boolean yearOk = java.util.Arrays.stream(allowedYears.split(","))
                    .map(String::trim)
                    .anyMatch(y -> y.equals(String.valueOf(registration.getStudyYear())));
            if (!yearOk) {
                throw new IllegalArgumentException("Your study year is not eligible for this event.");
            }
        }

        if (registrationRepo.existsByEvent_IdAndAttendeeEmail(event.getId(), registration.getAttendeeEmail())) {
            throw new IllegalArgumentException("Already registered for this event.");
        }
        registration.setEvent(event);
        return registrationRepo.save(registration);
    }

    public List<Registration> getRegistrationsForEvent(UUID eventId, String authenticatedEmail) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getClub().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("You are not authorized to view registrations for this event.");
        }

        return registrationRepo.findByEvent_Id(eventId);
    }

    public Registration checkInAttendee(UUID registrationId, String authenticatedEmail) {
        Registration registration = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration ticket not found!"));

        if (!registration.getEvent().getClub().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("You are not authorized to check in attendees for this event.");
        }

        if (Boolean.TRUE.equals(registration.getCheckedIn())) {
            throw new IllegalArgumentException("Attendee has already checked in!");
        }
        registration.setCheckedIn(true);
        return registrationRepo.save(registration);
    }

    public String exportFilteredRegistrationsToCSV(UUID eventId, String department, Integer studyYear, Boolean checkedIn, String authenticatedEmail) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getClub().getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException("You are not authorized to export registrations for this event.");
        }

        List<Registration> allRegistrations = registrationRepo.findByEvent_Id(eventId);

        List<Registration> filteredList = allRegistrations.stream()
                .filter(reg -> department == null || department.equalsIgnoreCase(reg.getDepartment()))
                .filter(reg -> studyYear == null || studyYear.equals(reg.getStudyYear()))
                .filter(reg -> checkedIn == null || checkedIn.equals(reg.getCheckedIn()))
                .toList();

        return CSVExportUtil.generateRegistrationCSV(filteredList);
    }
}
