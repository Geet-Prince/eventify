package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClubService {

    private final ClubRepository clubRepo;
    @Autowired
    public ClubService(ClubRepository clubRepo) {
        this.clubRepo = clubRepo;
    }
    public Club registerClub(Club club){
        Optional<Club> existingClub = clubRepo.findByEmail(club.getEmail());
        if (existingClub.isPresent()) {
            throw new IllegalArgumentException("A club with this email already exists.");
        }
        return clubRepo.save(club);

    }

}
