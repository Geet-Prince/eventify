package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventify/clubs")
public class ClubController {
    private final ClubService clubService;
    @Autowired
    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }
    @PostMapping("/register")
    public ResponseEntity<Club> registerClub(@RequestBody Club club) {
        try {
            Club registeredClub = clubService.registerClub(club);
            return new ResponseEntity<>(registeredClub, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>((HttpHeaders) null, HttpStatus.BAD_REQUEST);
        }
    }
}
