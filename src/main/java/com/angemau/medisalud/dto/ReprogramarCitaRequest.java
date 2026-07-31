package com.angemau.medisalud.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramarCitaRequest(
        @NotNull(message = "La nueva fecha y hora es obligatoria")
        @Future(message = "La nueva fecha y hora debe ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Fecha y hora en formato AAAA-MM-DDTHH:mm:ss", example = "2026-08-10T09:00:00")
        LocalDateTime nuevaFechaHora) {
}
