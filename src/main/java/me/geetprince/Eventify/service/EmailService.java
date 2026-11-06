package me.geetprince.Eventify.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import me.geetprince.Eventify.model.Club;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.mail.sender-email:noreply@eventify.com}")
    private String senderEmail;

    @Value("${app.base-url:https://eventify-backend-bcb0.onrender.com}")
    private String baseUrl;

    @Value("${app.frontend-url:https://eventify.geetprince.me}")
    private String frontendUrl;

    @Autowired(required = false)
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String recipientEmail, String clubName, String verificationToken) {
        String verificationUrl = baseUrl + "/eventify/auth/verify?token=" + verificationToken;
        String subject = "Verify your email for Eventify Club Registration";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<h2 style='color: #4F46E5;'>Welcome to Eventify!</h2>"
                + "<p>Hello <b>" + escapeHtml(clubName) + "</b>,</p>"
                + "<p>Thank you for initiating club registration on Eventify. Please verify your email address to submit your request for University Admin approval.</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + verificationUrl + "' style='background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Verify My Email</a>"
                + "</div>"
                + "<p style='color: #666; font-size: 14px;'>Or copy and paste this link in your browser:<br><a href='" + verificationUrl + "'>" + verificationUrl + "</a></p>"
                + "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>"
                + "<p style='color: #999; font-size: 12px;'>After your email is verified, your registration will be forwarded to the Institutional Super Admin for review.</p>"
                + "</div>";

        sendHtmlEmail(recipientEmail, subject, content);
    }

    @Async
    public void sendAdminApprovalRequest(Club club) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("ADMIN_EMAIL is not configured. Admin approval notification cannot be sent for club: {}", club.getName());
            return;
        }

        String approveUrl = baseUrl + "/eventify/admin/clubs/" + club.getId() + "/approve?token=" + club.getApprovalToken();
        String rejectUrl = baseUrl + "/eventify/admin/clubs/" + club.getId() + "/reject?token=" + club.getApprovalToken();
        String subject = "Action Required: New Club Registration Request - " + club.getName();

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<h2 style='color: #1E293B;'>Institutional Governance: New Club Request</h2>"
                + "<p>A new student club has verified their email and is requesting institutional authorization to host events on Eventify.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;'>Club Name:</td><td style='padding: 8px; border-bottom: 1px solid #eee;'>" + escapeHtml(club.getName()) + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;'>Official Email:</td><td style='padding: 8px; border-bottom: 1px solid #eee;'>" + escapeHtml(club.getEmail()) + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #eee; font-weight: bold;'>Registration ID:</td><td style='padding: 8px; border-bottom: 1px solid #eee; font-family: monospace;'>" + club.getId() + "</td></tr>"
                + "</table>"
                + "<p>Please review and choose an action:</p>"
                + "<div style='margin: 30px 0;'>"
                + "<a href='" + approveUrl + "' style='background-color: #16A34A; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; margin-right: 15px; display: inline-block;'>Approve Club</a>"
                + "<a href='" + rejectUrl + "' style='background-color: #DC2626; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Reject Request</a>"
                + "</div>"
                + "<p style='color: #666; font-size: 13px;'>Approving will activate their account and notify the applicant that they may log in and publish campus events.</p>"
                + "</div>";

        sendHtmlEmail(adminEmail, subject, content);
    }

    @Async
    public void sendClubApprovalNotification(String recipientEmail, String clubName) {
        String loginUrl = frontendUrl + "/login";
        String subject = "Congratulations! Your Club Has Been Approved - Eventify";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<h2 style='color: #16A34A;'>Your Club is Approved!</h2>"
                + "<p>Hello <b>" + escapeHtml(clubName) + "</b>,</p>"
                + "<p>Great news! The University Administrator has reviewed and approved your club registration on Eventify.</p>"
                + "<p>You now have full access to publish events, manage ticketing, and monitor attendee check-ins.</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + loginUrl + "' style='background-color: #16A34A; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;'>Log In to Dashboard</a>"
                + "</div>"
                + "</div>";

        sendHtmlEmail(recipientEmail, subject, content);
    }

    @Async
    public void sendClubRejectionNotification(String recipientEmail, String clubName) {
        String subject = "Update regarding your Eventify Club Registration Request";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>"
                + "<h2 style='color: #DC2626;'>Registration Request Declined</h2>"
                + "<p>Hello <b>" + escapeHtml(clubName) + "</b>,</p>"
                + "<p>We regret to inform you that your registration request for <b>" + escapeHtml(clubName) + "</b> was not approved by the Institutional Administrator.</p>"
                + "<p>If you believe this is an error, please reach out directly to the university activities office.</p>"
                + "</div>";

        sendHtmlEmail(recipientEmail, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Email will be logged only.\nTo: {}\nSubject: {}\nBody: {}", to, subject, htmlBody);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email successfully dispatched to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
