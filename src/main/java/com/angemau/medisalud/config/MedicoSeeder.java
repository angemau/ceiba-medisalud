package com.angemau.medisalud.config;

import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicoSeeder implements CommandLineRunner {

    private final MedicoRepository medicoRepository;

    @Override
    public void run(String... args) {
        if (medicoRepository.count() > 0) {
            return;
        }

        medicoRepository.saveAll(List.of(
                new Medico(null, "Dra. María González", "Cardiología", "555-1001", "maria.gonzalez@medisalud.com"),
                new Medico(null, "Dr. Carlos Ruiz", "Pediatría", "555-1002", "carlos.ruiz@medisalud.com"),
                new Medico(null, "Dra. Ana López", "Dermatología", "555-1003", "ana.lopez@medisalud.com")
        ));
    }
}