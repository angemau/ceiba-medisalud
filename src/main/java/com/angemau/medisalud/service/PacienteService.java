package com.angemau.medisalud.service;

import com.angemau.medisalud.dto.PacienteRequest;
import com.angemau.medisalud.exception.DocumentoDuplicadoException;
import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente crear(PacienteRequest request) {
        if (pacienteRepository.existsByDocumentoIdentidad(request.documentoIdentidad())) {
            throw new DocumentoDuplicadoException(
                    "Ya existe un paciente con el documento " + request.documentoIdentidad());
        }

        Paciente paciente = new Paciente();
        paciente.setNombreCompleto(request.nombreCompleto());
        paciente.setDocumentoIdentidad(request.documentoIdentidad());
        paciente.setTelefono(request.telefono());
        paciente.setEmail(request.email());
        paciente.setFechaNacimiento(request.fechaNacimiento());

        return pacienteRepository.save(paciente);
    }

    public List<Paciente> listar() {
        return pacienteRepository.findAll();
    }

    public Paciente obtenerPorId(UUID id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado: " + id));
    }
}
