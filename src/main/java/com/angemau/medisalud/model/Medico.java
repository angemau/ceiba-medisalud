package com.angemau.medisalud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Medico {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nombreCompleto;   // 3-100 caracteres
    private String especialidad;
    private String telefono;         // opcional
    private String email;            // opcional
}