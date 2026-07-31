package com.angemau.medisalud.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validaciones de PacienteRequest")
class PacienteRequestValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static List<String> propiedadesConViolacion(PacienteRequest request) {
        Set<ConstraintViolation<PacienteRequest>> violaciones = validator.validate(request);
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).toList();
    }

    @Test
    @DisplayName("un request completo y válido no tiene violaciones")
    void requestValido() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "3001234567", "ana.restrepo@example.com",
                LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).isEmpty();
    }

    @Test
    @DisplayName("nombreCompleto vacío es rechazado")
    void nombreCompletoVacio() {
        PacienteRequest request = new PacienteRequest(
                "", "1020304050", "3001234567", "ana@example.com", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("nombreCompleto");
    }

    @Test
    @DisplayName("documentoIdentidad con menos de 7 caracteres es rechazado")
    void documentoMuyCorto() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "12345", "3001234567", "ana@example.com", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("documentoIdentidad");
    }

    @Test
    @DisplayName("telefono vacío es rechazado: en Paciente el contacto es obligatorio")
    void telefonoVacio() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "", "ana@example.com", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("telefono");
    }

    @Test
    @DisplayName("telefono con letras es rechazado")
    void telefonoConLetras() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "300abc4567", "ana@example.com", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("telefono");
    }

    @Test
    @DisplayName("email vacío es rechazado")
    void emailVacio() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "3001234567", "", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("email");
    }

    @Test
    @DisplayName("email con formato inválido es rechazado")
    void emailInvalido() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "3001234567", "sin-arroba", LocalDate.of(1990, 5, 20));

        assertThat(propiedadesConViolacion(request)).contains("email");
    }

    @Test
    @DisplayName("fechaNacimiento futura es rechazada")
    void fechaNacimientoFutura() {
        PacienteRequest request = new PacienteRequest(
                "Ana Restrepo", "1020304050", "3001234567", "ana@example.com",
                LocalDate.now().plusDays(1));

        assertThat(propiedadesConViolacion(request)).contains("fechaNacimiento");
    }

    @Test
    @DisplayName("un request completamente vacío acumula las violaciones de los campos obligatorios")
    void requestVacio() {
        PacienteRequest request = new PacienteRequest("", "", "", "", null);

        assertThat(propiedadesConViolacion(request))
                .contains("nombreCompleto", "documentoIdentidad", "telefono", "email");
    }
}
