package com.angemau.medisalud.repository;

import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CitaRepository extends JpaRepository<Cita, UUID>, JpaSpecificationExecutor<Cita> {
    List<Cita> findByMedicoIdAndFechaHoraBetween(UUID medicoId, LocalDateTime inicio, LocalDateTime fin);
    boolean existsByMedicoIdAndFechaHoraAndEstado(UUID medicoId, LocalDateTime fechaHora, EstadoCita estado);
    boolean existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(UUID pacienteId, UUID medicoId, LocalDateTime fechaHora, EstadoCita estado);
}
