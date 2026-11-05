package me.geetprince.Eventify.repository;

import me.geetprince.Eventify.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID> {
    Optional<Club> findByEmail(String email);

    Optional<Club> findByVerificationToken(String verificationToken);
}
