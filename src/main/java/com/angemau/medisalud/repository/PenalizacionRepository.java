package com.angemau.medisalud.repository;

import com.angemau.medisalud.model.Penalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {
    List<Penalizacion> findByPacienteIdAndFechaAfter(UUID pacienteId, LocalDateTime fecha);
}
