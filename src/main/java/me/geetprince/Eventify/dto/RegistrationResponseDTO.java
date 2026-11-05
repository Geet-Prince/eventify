package me.geetprince.Eventify.dto;

import lombok.Data;

import java.util.UUID;
@Data
public class RegistrationResponseDTO {
    private UUID id;
    private String studentName;
    private String studentEmail;
    private Boolean checkedIn;
    private String qrCodeBase64;
}
