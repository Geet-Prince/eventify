package me.geetprince.Eventify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClubRequestDTO {

    @NotBlank(message = "Club name is required")
    @Size(min = 3, message = "Club name must be at least 3 characters long")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_~`*?|:;\"'<>,./\\\\\\-]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;
}
