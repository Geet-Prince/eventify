package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubService {

    private final ClubRepository clubRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.email-verification-required:true}")
    private boolean emailVerificationRequired;

    @Autowired
    public ClubService(ClubRepository clubRepo, PasswordEncoder passwordEncoder) {
        this.clubRepo = clubRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Club registerClub(Club club) {
        Optional<Club> existingClub = clubRepo.findByEmail(club.getEmail());
        if (existingClub.isPresent()) {
            throw new IllegalArgumentException("A club with this email already exists.");
        }
        if (club.getRole() == null || club.getRole().isBlank()) {
            club.setRole("ROLE_CLUB");
        }
        String hashedPassword = passwordEncoder.encode(club.getPasswordHash());
        club.setPasswordHash(hashedPassword);
        club.setVerified(false);
        club.setVerificationToken(UUID.randomUUID().toString());
        return clubRepo.save(club);
    }

    public Club verifyClubEmail(String token) {
        Club club = clubRepo.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired email verification token."));
        club.setVerified(true);
        club.setVerificationToken(null);
        return clubRepo.save(club);
    }

    public Club verifyLogin(String email, String rawPassword) {
        Club club = clubRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!passwordEncoder.matches(rawPassword, club.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        if (emailVerificationRequired && !club.isVerified()) {
            throw new IllegalArgumentException("Email is not verified. Please verify your email before logging in.");
        }

        return club;
    }

    public Club getClubById(UUID id) {
        return clubRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Club not found."));
    }

    public Club getClubByEmail(String email) {
        return clubRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Club not found."));
    }

    public List<Club> getAllClubs() {
        return clubRepo.findAll();
    }
}
