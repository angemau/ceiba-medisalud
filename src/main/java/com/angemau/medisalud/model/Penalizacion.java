package com.angemau.medisalud.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
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
