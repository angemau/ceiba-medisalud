package com.angemau.medisalud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "paciente", uniqueConstraints = {
        @UniqueConstraint(columnNames = "documento_identidad")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Paciente {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El documento de identidad es obligatorio")
    @Size(min = 7, message = "El documento debe tener mínimo 7 caracteres")
    @Column(name = "documento_identidad", nullable = false)
    private String documentoIdentidad;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "\\d{7,}", message = "El teléfono debe tener mínimo 7 dígitos")
    private String telefono;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;
}