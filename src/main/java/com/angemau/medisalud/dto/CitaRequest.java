package com.angemau.medisalud.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record CitaRequest(
        @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId,

        @NotNull(message = "El médico es obligatorio")
        UUID medicoId,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha debe ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Fecha y hora en formato AAAA-MM-DDTHH:mm:ss", example = "2026-08-10T09:00:00")
        LocalDateTime fechaHora
) {}
