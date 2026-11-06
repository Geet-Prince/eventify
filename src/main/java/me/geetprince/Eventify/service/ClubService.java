package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubService {

    private final ClubRepository clubRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Autowired
    public ClubService(ClubRepository clubRepo, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.clubRepo = clubRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
        club.setApprovalToken(UUID.randomUUID().toString());
        club.setApprovalStatus("PENDING_EMAIL");

        Club savedClub = clubRepo.save(club);

        // Dispatch verification email to applicant
        emailService.sendVerificationEmail(savedClub.getEmail(), savedClub.getName(), savedClub.getVerificationToken());

        return savedClub;
    }

    public Club verifyClubEmail(String token) {
        Club club = clubRepo.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired email verification token."));

        club.setVerified(true);
        club.setVerificationToken(null);
        club.setApprovalStatus("PENDING_ADMIN_APPROVAL");
        Club updatedClub = clubRepo.save(club);

        // Trigger notification to Super Admin with approval buttons
        emailService.sendAdminApprovalRequest(updatedClub);

        return updatedClub;
    }

    public Club approveClub(UUID clubId, String token) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found."));

        if (club.getApprovalToken() == null || !club.getApprovalToken().equals(token)) {
            throw new IllegalArgumentException("Invalid or already used approval token.");
        }

        club.setApprovalStatus("APPROVED");
        club.setApprovalToken(null);
        Club approvedClub = clubRepo.save(club);

        emailService.sendClubApprovalNotification(approvedClub.getEmail(), approvedClub.getName());

        return approvedClub;
    }

    public Club rejectClub(UUID clubId, String token) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found."));

        if (club.getApprovalToken() == null || !club.getApprovalToken().equals(token)) {
            throw new IllegalArgumentException("Invalid or already used approval token.");
        }

        club.setApprovalStatus("REJECTED");
        club.setApprovalToken(null);
        Club rejectedClub = clubRepo.save(club);

        emailService.sendClubRejectionNotification(rejectedClub.getEmail(), rejectedClub.getName());

        return rejectedClub;
    }

    public Club verifyLogin(String email, String rawPassword) {
        Club club = clubRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!passwordEncoder.matches(rawPassword, club.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!club.isVerified()) {
            throw new IllegalArgumentException("Email is not verified. Please verify your email before logging in.");
        }

        if ("PENDING_ADMIN_APPROVAL".equalsIgnoreCase(club.getApprovalStatus())) {
            throw new IllegalArgumentException("Your club registration is pending Institutional Admin approval. You will be notified via email once approved.");
        }

        if ("REJECTED".equalsIgnoreCase(club.getApprovalStatus())) {
            throw new IllegalArgumentException("Your club registration request was declined by the Institutional Administrator.");
        }

        if (!"APPROVED".equalsIgnoreCase(club.getApprovalStatus())) {
            throw new IllegalArgumentException("Your club account is not approved to access the system.");
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
