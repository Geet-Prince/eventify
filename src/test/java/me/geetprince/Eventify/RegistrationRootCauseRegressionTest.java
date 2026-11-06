package me.geetprince.Eventify;

import me.geetprince.Eventify.model.Club;
import me.geetprince.Eventify.repository.ClubRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Root Cause Regression Test Suite
 *
 * Tests that all four HTTP 500 bugs are fixed:
 *   FIX 1 – Email dispatch is @Async (non-blocking)
 *   FIX 2 – MailSendException / MailAuthenticationException are caught inside EmailService
 *   FIX 3 – SMTP timeouts are configured in application.properties
 *   FIX 4 – GlobalExceptionHandler has a catch-all @ExceptionHandler(Exception.class)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegistrationRootCauseRegressionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClubRepository clubRepository;

    // Spring Boot 4.x: @MockitoBean replaced the removed @MockBean
    @MockitoBean
    JavaMailSender mailSender;

    @BeforeEach
    void cleanDb() {
        clubRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX 1 & 2  –  SMTP failure must NOT return 500
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("FIX 1+2: Registration returns 201 when SMTP throws MailSendException")
    void registration_Returns201_WhenSmtpThrowsMailSendException() throws Exception {
        // The original bug: only MessagingException was caught → MailSendException escaped → HTTP 500
        stubMailSenderThrows(new MailSendException("Simulated SMTP relay rejection"));

        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("smtpfail@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Club must be persisted – registration is not rolled back due to email failure
        Optional<Club> saved = clubRepository.findByEmail("smtpfail@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getApprovalStatus()).isEqualTo("PENDING_EMAIL");
        assertThat(saved.get().isVerified()).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("FIX 1+2: Registration returns 201 when SMTP throws MailAuthenticationException (Brevo unauthorized sender)")
    void registration_Returns201_WhenSmtpThrowsMailAuthenticationException() throws Exception {
        stubMailSenderThrows(new MailAuthenticationException("535 Brevo: Sender not authorized"));

        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("authfail@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.club.email").value("authfail@test.com"));

        assertThat(clubRepository.findByEmail("authfail@test.com")).isPresent();
    }

    @Test
    @Order(3)
    @DisplayName("FIX 1+2: Registration returns 201 when SMTP throws generic RuntimeException (timeout)")
    void registration_Returns201_WhenSmtpThrowsConnectionTimeout() throws Exception {
        stubMailSenderThrows(new RuntimeException("Connection timed out after 5000ms"));

        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("timeout@test.com")))
                .andExpect(status().isCreated());

        assertThat(clubRepository.findByEmail("timeout@test.com")).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX 1  –  @Async annotation present on EmailService methods
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("FIX 1: All EmailService send methods carry @Async")
    void emailService_AllPublicSendMethods_AreAnnotatedAsync() throws Exception {
        var clazz = me.geetprince.Eventify.service.EmailService.class;

        assertAsync(clazz, "sendVerificationEmail", String.class, String.class, String.class);
        assertAsync(clazz, "sendAdminApprovalRequest", Club.class);
        assertAsync(clazz, "sendClubApprovalNotification", String.class, String.class);
        assertAsync(clazz, "sendClubRejectionNotification", String.class, String.class);
    }

    @Test
    @Order(5)
    @DisplayName("FIX 1: @EnableAsync is present on EventifyApplication")
    void mainApplication_HasEnableAsync() {
        assertThat(EventifyApplication.class.isAnnotationPresent(
                org.springframework.scheduling.annotation.EnableAsync.class))
                .as("@EnableAsync must be on EventifyApplication").isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX 3  –  SMTP timeout properties in application.properties
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("FIX 3: SMTP timeouts are configured (1000–10000ms) in src/main/resources/application.properties")
    void smtpTimeouts_AreConfigured_InApplicationProperties() throws Exception {
        // Load the production application.properties directly from src/main/resources
        // (the test-classpath version overrides it, so we read from the source directory)
        var propsFile = java.nio.file.Paths.get("src/main/resources/application.properties");
        var props = new java.util.Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(propsFile)) {
            props.load(reader);
        }

        assertTimeout(props, "spring.mail.properties.mail.smtp.connectiontimeout");
        assertTimeout(props, "spring.mail.properties.mail.smtp.timeout");
        assertTimeout(props, "spring.mail.properties.mail.smtp.writetimeout");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX 4  –  GlobalExceptionHandler catch-all
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("FIX 4: GlobalExceptionHandler has @ExceptionHandler(Exception.class) catch-all")
    void globalExceptionHandler_HasCatchAllExceptionHandler() {
        var handlerClass = me.geetprince.Eventify.exception.GlobalExceptionHandler.class;
        boolean hasCatchAll = false;
        for (var method : handlerClass.getMethods()) {
            var ann = method.getAnnotation(
                    org.springframework.web.bind.annotation.ExceptionHandler.class);
            if (ann != null) {
                for (var t : ann.value()) {
                    if (t.equals(Exception.class)) hasCatchAll = true;
                }
            }
        }
        assertThat(hasCatchAll)
                .as("GlobalExceptionHandler must have @ExceptionHandler(Exception.class)").isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("FIX 4: handleGeneralException accepts Exception and returns ResponseEntity<ErrorResponse>")
    void globalExceptionHandler_MethodSignature_IsCorrect() throws Exception {
        var method = me.geetprince.Eventify.exception.GlobalExceptionHandler.class
                .getMethod("handleGeneralException", Exception.class);

        assertThat(method.getParameterTypes()[0])
                .as("Must accept Exception").isEqualTo(Exception.class);
        assertThat(method.getReturnType().getSimpleName())
                .as("Must return ResponseEntity").isEqualTo("ResponseEntity");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // End-to-end: happy path + edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("E2E: Successful registration returns 201 and persists club with correct state")
    void registration_HappyPath_Returns201AndPersistsClub() throws Exception {
        stubMailSenderSuccess();

        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("success@test.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("verify your email")))
                .andExpect(jsonPath("$.club.email").value("success@test.com"))
                .andExpect(jsonPath("$.club.id").exists());

        Optional<Club> saved = clubRepository.findByEmail("success@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().isVerified()).isFalse();
        assertThat(saved.get().getApprovalStatus()).isEqualTo("PENDING_EMAIL");
        assertThat(saved.get().getVerificationToken()).isNotNull();
    }

    @Test
    @Order(10)
    @DisplayName("E2E: Duplicate email registration returns 400, not 500")
    void registration_DuplicateEmail_Returns400() throws Exception {
        stubMailSenderSuccess();
        String json = validRegistrationJson("dup@test.com");
        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @Order(11)
    @DisplayName("E2E: Invalid request body returns 400 Validation, not 500")
    void registration_InvalidBody_Returns400() throws Exception {
        mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"notanemail\", \"password\": \"weak\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("E2E: Invalid verification token returns 400, not 500")
    void emailVerification_InvalidToken_Returns400() throws Exception {
        mockMvc.perform(get("/eventify/auth/verify")
                        .param("token", "token-does-not-exist"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    @DisplayName("E2E: Full flow — register → verify token → DB status becomes PENDING_ADMIN_APPROVAL")
    void fullFlow_RegisterThenVerify_ProgressesApprovalStatus() throws Exception {
        stubMailSenderSuccess();

        // Step 1: Register
        String regBody = mockMvc.perform(post("/eventify/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("fullflow@test.com")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Security check: verificationToken MUST NOT be leaked in the API HTTP response!
        assertThat(regBody).doesNotContain("verificationToken");

        // The token is safely stored in DB and sent via email only
        Club registeredClub = clubRepository.findByEmail("fullflow@test.com").orElseThrow();
        String verificationToken = registeredClub.getVerificationToken();
        assertThat(verificationToken).isNotNull().isNotBlank();

        // Step 2: Verify email
        mockMvc.perform(get("/eventify/auth/verify").param("token", verificationToken))
                .andExpect(status().isOk());

        // Step 3: Assert DB state
        Club club = clubRepository.findByEmail("fullflow@test.com").orElseThrow();
        assertThat(club.isVerified()).isTrue();
        assertThat(club.getVerificationToken()).isNull();         // consumed
        assertThat(club.getApprovalStatus()).isEqualTo("PENDING_ADMIN_APPROVAL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void stubMailSenderSuccess() throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));
        doNothing().when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    private void stubMailSenderThrows(RuntimeException ex) throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null));
        doThrow(ex).when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    private void assertAsync(Class<?> clazz, String methodName, Class<?>... params) throws Exception {
        var method = clazz.getMethod(methodName, params);
        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class))
                .as(methodName + " must be @Async").isTrue();
    }

    private void assertTimeout(java.util.Properties props, String key) {
        String value = props.getProperty(key);
        assertThat(value).as(key + " must be set in application.properties").isNotBlank();
        int ms = Integer.parseInt(value);
        assertThat(ms).as(key + " must be between 1000ms and 10000ms").isBetween(1000, 10000);
    }

    /** Minimal JSON field extractor to avoid ObjectMapper class-not-found issues. */
    private String extractJsonField(String json, String field) {
        String search = "\"" + field + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String validRegistrationJson(String email) {
        return """
                {
                    "name": "Test Club",
                    "email": "%s",
                    "password": "Password1!"
                }
                """.formatted(email);
    }
}
