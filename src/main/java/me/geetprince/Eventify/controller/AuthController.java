package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.dto.LoginRequest;
import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.service.ClubService;
import me.geetprince.Eventify.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/eventify/auth")
public class AuthController {
    private final ClubService clubService;
    private final JwtUtil jwtUtil;
    @Autowired
    public AuthController(ClubService clubService, JwtUtil jwtUtil) {
        this.clubService = clubService;
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Verify credentials using the service
            Club club = clubService.verifyLogin(loginRequest.getEmail(), loginRequest.getPassword());

            // 2. Generate the JWT
            String token = jwtUtil.generateToken(club.getEmail());

            // 3. Return the token and club ID to the frontend
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("clubId", club.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}

