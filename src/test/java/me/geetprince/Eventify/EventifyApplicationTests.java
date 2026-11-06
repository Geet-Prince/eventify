package me.geetprince.Eventify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EventifyApplicationTests {

    // Mock mail sender so context loads without a real SMTP server
    @MockitoBean
    JavaMailSender mailSender;

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts up correctly
    }
}
