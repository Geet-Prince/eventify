package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.dto.ClubResponseDTO;
import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/eventify/clubs")
public class ClubController {
    private final ClubService clubService;
    @Autowired
    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getClubById(@PathVariable UUID id) {
        try {
            Club club = clubService.getClubById(id);
            return ResponseEntity.ok(ClubResponseDTO.fromEntity(club));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Club not found.");
        }
    }
    @GetMapping
    public ResponseEntity<List<ClubResponseDTO>> getAllClubs() {
        List<ClubResponseDTO> clubs = clubService.getAllClubs().stream()
                .map(ClubResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(clubs);
    }
}
