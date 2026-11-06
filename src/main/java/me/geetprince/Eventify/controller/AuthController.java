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

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:https://eventify.geetprince.me}")
    private String frontendUrl;

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
    public ResponseEntity<?> verifyEmail(
            @RequestParam String token,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {
        boolean acceptsHtml = acceptHeader != null && acceptHeader.contains("text/html");
        try {
            Club verifiedClub = clubService.verifyClubEmail(token);
            if (acceptsHtml) {
                String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #e0e0e0; text-align: center;'>"
                        + "<h2 style='color: #4F46E5;'>Email Verified Successfully!</h2>"
                        + "<p>Email for <b>" + verifiedClub.getName() + "</b> (" + verifiedClub.getEmail() + ") has been verified.</p>"
                        + "<p>Your club registration request has been submitted for Institutional Admin approval. You will receive an email once approved.</p>"
                        + "<a href='" + frontendUrl + "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; margin-top: 15px;'>Return to Eventify</a>"
                        + "</div>";
                return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email verified successfully for: " + verifiedClub.getEmail() + ". Your application is now pending institutional approval.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (acceptsHtml) {
                String errorHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #fecaca; text-align: center;'>"
                        + "<h2 style='color: #DC2626;'>Verification Failed</h2>"
                        + "<p>" + e.getMessage() + "</p>"
                        + "<a href='" + frontendUrl + "' style='color: #4F46E5; text-decoration: underline;'>Return to Eventify</a>"
                        + "</div>";
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(org.springframework.http.MediaType.TEXT_HTML).body(errorHtml);
            }
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
