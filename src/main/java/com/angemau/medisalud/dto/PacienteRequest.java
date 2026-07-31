package com.angemau.medisalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank @Size(min = 3, max = 100) String nombreCompleto,
        @NotBlank @Size(min = 7) String documentoIdentidad,
        @NotBlank @Pattern(regexp = "\\d{7,}") String telefono,
        @NotBlank @Email String email,
        @PastOrPresent LocalDate fechaNacimiento
) {}
