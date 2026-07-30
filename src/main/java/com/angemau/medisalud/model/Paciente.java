package com.angemau.medisalud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
public class Paciente {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nombreCompleto;
    private String documentoIdentidad;  // único
    private String telefono;
    private String email;
    private LocalDate fechaNacimiento;  // opcional (RN-03)
}
