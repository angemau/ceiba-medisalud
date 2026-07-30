package com.angemau.medisalud.service;

import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.repository.CitaRepository;
import com.angemau.medisalud.repository.MedicoRepository;
import com.angemau.medisalud.repository.PacienteRepository;
import com.angemau.medisalud.repository.PenalizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PenalizacionRepository penalizacionRepository;

}
