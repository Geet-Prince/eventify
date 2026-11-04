package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Event;
import me.geetprince.Eventify.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository eventRepo;
    @Autowired
    public EventService(EventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }
    public Event createEvent(Event event){
        return eventRepo.save(event);
    }
    public List<Event> getAllEvents(){
        return eventRepo.findAll();
    }
    public Event getEventById(UUID id){
        return eventRepo.findById(id).orElseThrow(()->new IllegalArgumentException("This event not found"));
    }
    public List<Event> searchEventsByName(String name) {
        return eventRepo.findByNameContainingIgnoreCase(name);
    }
    public List<Event> searchEventsByClubName(String clubName) {
        return eventRepo.findByClub_NameContainingIgnoreCase(clubName);
    }
    public Event updateEvent(UUID id, Event updatedEventDetails) {
        Event existingEvent = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("This event not found"));

        existingEvent.setName(updatedEventDetails.getName());
        existingEvent.setStartTime(updatedEventDetails.getStartTime());
        existingEvent.setEndTime(updatedEventDetails.getEndTime());
        existingEvent.setRegistrationDeadline(updatedEventDetails.getRegistrationDeadline());
        existingEvent.setDetails(updatedEventDetails.getDetails());
        existingEvent.setPublic(updatedEventDetails.isPublic());
        existingEvent.setVenue(updatedEventDetails.getVenue());
        existingEvent.setGuests(updatedEventDetails.getGuests());
        existingEvent.setImageLink(updatedEventDetails.getImageLink());
        existingEvent.setAllowedDepartments(updatedEventDetails.getAllowedDepartments());
        existingEvent.setAllowedStudyYears(updatedEventDetails.getAllowedStudyYears());

        return eventRepo.save(existingEvent);
    }



}
