package com.angemau.medisalud.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramarCitaRequest(
        @NotNull(message = "La nueva fecha y hora es obligatoria")
        @Future(message = "La nueva fecha y hora debe ser futura")
        LocalDateTime nuevaFechaHora) {
}
