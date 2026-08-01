package com.angemau.medisalud.dto;

import com.angemau.medisalud.model.Paciente;

import java.util.UUID;

public record PacienteResumenResponse(UUID id, String nombreCompleto) {
    public static PacienteResumenResponse from(Paciente paciente) {
        return new PacienteResumenResponse(paciente.getId(), paciente.getNombreCompleto());
    }
}
