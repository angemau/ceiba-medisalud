package com.angemau.medisalud.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Penalizacion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Paciente paciente;

    private LocalDateTime fecha;

    public Penalizacion() {}

    public Penalizacion(Paciente paciente, LocalDateTime fecha) {
        this.paciente = paciente;
        this.fecha = fecha;
    }
}
