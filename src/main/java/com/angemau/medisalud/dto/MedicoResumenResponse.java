package com.angemau.medisalud.dto;

import com.angemau.medisalud.model.Medico;

import java.util.UUID;

public record MedicoResumenResponse(UUID id, String nombreCompleto, String especialidad) {
    public static MedicoResumenResponse from(Medico medico) {
        return new MedicoResumenResponse(medico.getId(), medico.getNombreCompleto(), medico.getEspecialidad());
    }
}
