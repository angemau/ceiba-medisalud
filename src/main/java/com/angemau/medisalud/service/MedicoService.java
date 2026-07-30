package com.angemau.medisalud.service;

import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public Medico crear(Medico medico) {
        return medicoRepository.save(medico);
    }

    public List<Medico> listarTodos() {
        return medicoRepository.findAll();
    }

    public Medico obtenerPorId(UUID id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));
    }
}