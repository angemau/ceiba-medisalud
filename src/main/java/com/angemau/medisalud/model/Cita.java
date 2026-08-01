package com.angemau.medisalud.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Cita {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    private Paciente paciente;
    @ManyToOne private Medico medico;
    private LocalDateTime fechaHora;
    @Enumerated(EnumType.STRING)
    private EstadoCita estado;       // PROGRAMADA, CANCELADA, ATENDIDA
    private LocalDateTime fechaCancelacion;  // nullable
}
