package com.angemau.medisalud.service;

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

    public Paciente crear(Paciente paciente) {
        if (pacienteRepository.existsByDocumentoIdentidad(paciente.getDocumentoIdentidad())) {
            throw new DocumentoDuplicadoException(
                    "Ya existe un paciente con el documento " + paciente.getDocumentoIdentidad());
        }
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
