package me.geetprince.Eventify.dto;

import lombok.Data;
import me.geetprince.Eventify.model.Club;

import java.util.UUID;

@Data
public class ClubResponseDTO {
    private UUID id;
    private String name;
    private String email;
    public static ClubResponseDTO fromEntity(Club club) {
        ClubResponseDTO dto = new ClubResponseDTO();
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setEmail(club.getEmail());
        return dto;
    }
}
