package com.angemau.medisalud.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CitaRequest(
        @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId,

        @NotNull(message = "El médico es obligatorio")
        UUID medicoId,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La fecha debe ser futura")
        LocalDateTime fechaHora
) {}
