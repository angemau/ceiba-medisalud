package com.angemau.medisalud.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validaciones de MedicoRequest")
class MedicoRequestValidacionTest {

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

    private static List<String> propiedadesConViolacion(MedicoRequest request) {
        Set<ConstraintViolation<MedicoRequest>> violaciones = validator.validate(request);
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).toList();
    }

    @Test
    @DisplayName("un request completo y válido no tiene violaciones")
    void requestValido() {
        MedicoRequest request = new MedicoRequest(
                "Carlos Jaramillo", "Medicina General", "6041234567", "carlos.jaramillo@example.com");

        assertThat(propiedadesConViolacion(request)).isEmpty();
    }

    @Test
    @DisplayName("nombreCompleto vacío es rechazado")
    void nombreCompletoVacio() {
        MedicoRequest request = new MedicoRequest("", "Medicina General", "6041234567", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).contains("nombreCompleto");
    }

    @Test
    @DisplayName("nombreCompleto con menos de 3 caracteres es rechazado")
    void nombreCompletoMuyCorto() {
        MedicoRequest request = new MedicoRequest("Al", "Medicina General", "6041234567", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).contains("nombreCompleto");
    }

    @Test
    @DisplayName("nombreCompleto con más de 100 caracteres es rechazado")
    void nombreCompletoMuyLargo() {
        MedicoRequest request = new MedicoRequest(
                "A".repeat(101), "Medicina General", "6041234567", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).contains("nombreCompleto");
    }

    @Test
    @DisplayName("especialidad vacía es rechazada")
    void especialidadVacia() {
        MedicoRequest request = new MedicoRequest("Carlos Jaramillo", "", "6041234567", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).contains("especialidad");
    }

    @Test
    @DisplayName("telefono vacío es válido: el contacto es opcional")
    void telefonoVacioEsValido() {
        MedicoRequest request = new MedicoRequest("Carlos Jaramillo", "Medicina General", "", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).doesNotContain("telefono");
    }

    @Test
    @DisplayName("telefono con menos de 7 dígitos es rechazado")
    void telefonoMuyCorto() {
        MedicoRequest request = new MedicoRequest("Carlos Jaramillo", "Medicina General", "12345", "carlos@example.com");

        assertThat(propiedadesConViolacion(request)).contains("telefono");
    }

    @Test
    @DisplayName("email con formato inválido es rechazado")
    void emailInvalido() {
        MedicoRequest request = new MedicoRequest(
                "Carlos Jaramillo", "Medicina General", "6041234567", "sin-arroba");

        assertThat(propiedadesConViolacion(request)).contains("email");
    }

    @Test
    @DisplayName("un request completamente vacío acumula las violaciones de los campos obligatorios")
    void requestVacio() {
        MedicoRequest request = new MedicoRequest("", "", null, null);

        assertThat(propiedadesConViolacion(request))
                .contains("nombreCompleto", "especialidad");
    }
}
