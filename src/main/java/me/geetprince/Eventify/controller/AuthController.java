package me.geetprince.Eventify.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import me.geetprince.Eventify.dto.ClubRequestDTO;
import me.geetprince.Eventify.dto.ClubResponseDTO;
import me.geetprince.Eventify.dto.ErrorResponse;
import me.geetprince.Eventify.dto.LoginRequest;
import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.model.RefreshToken;
import me.geetprince.Eventify.service.ClubService;
import me.geetprince.Eventify.service.LoginRateLimiterService;
import me.geetprince.Eventify.service.RefreshTokenService;
import me.geetprince.Eventify.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/eventify/auth")
public class AuthController {
    private final ClubService clubService;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiterService rateLimiterService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(
            ClubService clubService,
            JwtUtil jwtUtil,
            LoginRateLimiterService rateLimiterService,
            RefreshTokenService refreshTokenService) {
        this.clubService = clubService;
        this.jwtUtil = jwtUtil;
        this.rateLimiterService = rateLimiterService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody ClubRequestDTO requestDTO) {
        Club club = new Club();
        club.setName(requestDTO.getName());
        club.setEmail(requestDTO.getEmail());
        club.setPasswordHash(requestDTO.getPassword());
        Club registeredClub = clubService.registerClub(club);
        ClubResponseDTO responseDTO = ClubResponseDTO.fromEntity(registeredClub);

        Map<String, Object> response = new HashMap<>();
        response.put("club", responseDTO);
        response.put("message", "Registration successful. Please verify your email using the verification token sent to your email.");
        if (registeredClub.getVerificationToken() != null) {
            response.put("verificationToken", registeredClub.getVerificationToken());
        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            Club verifiedClub = clubService.verifyClubEmail(token);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email verified successfully for: " + verifiedClub.getEmail() + ". You can now log in.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Verification Failed",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String email = loginRequest.getEmail();

        if (rateLimiterService.isBlocked(clientIp) || rateLimiterService.isBlocked(email)) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Too many failed login attempts. Please try again after 15 minutes."
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        try {
            Club club = clubService.verifyLogin(loginRequest.getEmail(), loginRequest.getPassword());
            rateLimiterService.loginSucceeded(clientIp);
            rateLimiterService.loginSucceeded(email);

            String accessToken = jwtUtil.generateToken(club.getEmail(), club.getRole());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(club);

            Map<String, Object> response = new HashMap<>();
            response.put("token", accessToken);
            response.put("refreshToken", refreshToken.getToken());
            response.put("clubId", club.getId());
            response.put("role", club.getRole());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            rateLimiterService.loginFailed(clientIp);
            rateLimiterService.loginFailed(email);

            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return ResponseEntity.badRequest().body("refreshToken is required.");
        }

        try {
            RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
            refreshTokenService.verifyExpiration(refreshToken);

            Club club = refreshToken.getClub();
            String newAccessToken = jwtUtil.generateToken(club.getEmail(), club.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("token", newAccessToken);
            response.put("refreshToken", refreshToken.getToken());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Token Refresh Failed",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> request) {
        if (request != null && request.containsKey("refreshToken")) {
            refreshTokenService.revokeToken(request.get("refreshToken"));
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully. Refresh token revoked.");
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
