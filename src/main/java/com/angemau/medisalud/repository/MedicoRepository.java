package com.angemau.medisalud.repository;

import com.angemau.medisalud.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicoRepository extends JpaRepository<Medico, UUID> {
    Optional<Medico> findById(String id);
    boolean existsById(String id);
}
