package me.geetprince.Eventify.controller;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/eventify/admin")
public class AdminController {

    private final ClubService clubService;

    @Autowired
    public AdminController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping(value = "/clubs/{id}/approve", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approveClub(@PathVariable UUID id, @RequestParam String token) {
        try {
            Club club = clubService.approveClub(id, token);
            String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #e0e0e0; text-align: center;'>"
                    + "<h2 style='color: #16A34A;'>Club Approved Successfully!</h2>"
                    + "<p>The registration for <b>" + club.getName() + "</b> (" + club.getEmail() + ") has been authorized.</p>"
                    + "<p>An automated confirmation email has been dispatched to the club notifying them that they can now log in and publish campus events.</p>"
                    + "</div>";
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            String errorHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #fecaca; text-align: center;'>"
                    + "<h2 style='color: #DC2626;'>Action Failed</h2>"
                    + "<p>" + e.getMessage() + "</p>"
                    + "</div>";
            return ResponseEntity.badRequest().body(errorHtml);
        }
    }

    @GetMapping(value = "/clubs/{id}/reject", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> rejectClub(@PathVariable UUID id, @RequestParam String token) {
        try {
            Club club = clubService.rejectClub(id, token);
            String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #e0e0e0; text-align: center;'>"
                    + "<h2 style='color: #DC2626;'>Club Registration Declined</h2>"
                    + "<p>The registration for <b>" + club.getName() + "</b> has been rejected.</p>"
                    + "<p>A notification email has been dispatched to the applicant.</p>"
                    + "</div>";
            return ResponseEntity.ok(html);
        } catch (IllegalArgumentException e) {
            String errorHtml = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 40px auto; padding: 25px; border-radius: 8px; border: 1px solid #fecaca; text-align: center;'>"
                    + "<h2 style='color: #DC2626;'>Action Failed</h2>"
                    + "<p>" + e.getMessage() + "</p>"
                    + "</div>";
            return ResponseEntity.badRequest().body(errorHtml);
        }
    }
}
