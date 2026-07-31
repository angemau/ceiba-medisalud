package com.angemau.medisalud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MedicoRequest(
        @NotBlank @Size(min = 3, max = 100) String nombreCompleto,
        @NotBlank String especialidad,
        @Pattern(regexp = "^$|^[0-9-]{7,}$") String telefono,
        @Email String email
) {}
