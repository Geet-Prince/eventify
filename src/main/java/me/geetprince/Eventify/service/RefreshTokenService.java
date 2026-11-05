package me.geetprince.Eventify.service;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.model.RefreshToken;
import me.geetprince.Eventify.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepo;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    public RefreshToken createRefreshToken(Club club) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setClub(club);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setRevoked(false);
        return refreshTokenRepo.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token was revoked.");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepo.delete(token);
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    public void revokeToken(String token) {
        refreshTokenRepo.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepo.save(refreshToken);
        });
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found."));
    }

    @Transactional
    public void deleteByClub(Club club) {
        refreshTokenRepo.deleteByClub(club);
    }
}
