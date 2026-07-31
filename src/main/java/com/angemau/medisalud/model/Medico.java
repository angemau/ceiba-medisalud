package com.angemau.medisalud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "La especialidad es obligatoria")
    private String especialidad;

    @Pattern(regexp = "^$|^[0-9-]{7,}$", message = "El teléfono debe tener mínimo 7 dígitos")
    private String telefono;

    @Email(message = "El email no tiene un formato válido")
    private String email;
}