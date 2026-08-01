package com.angemau.medisalud.dto;

import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;

import java.time.LocalDateTime;
import java.util.UUID;

public record CitaResponse(
        UUID id,
        PacienteResumenResponse paciente,
        MedicoResumenResponse medico,
        LocalDateTime fechaHora,
        EstadoCita estado,
        LocalDateTime fechaCancelacion
) {
    public static CitaResponse from(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                PacienteResumenResponse.from(cita.getPaciente()),
                MedicoResumenResponse.from(cita.getMedico()),
                cita.getFechaHora(),
                cita.getEstado(),
                cita.getFechaCancelacion()
        );
    }
}
