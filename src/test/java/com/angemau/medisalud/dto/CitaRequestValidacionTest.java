package com.angemau.medisalud.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validaciones de CitaRequest")
class CitaRequestValidacionTest {

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

    private static List<String> mensajesDe(CitaRequest request) {
        Set<ConstraintViolation<CitaRequest>> violaciones = validator.validate(request);
        return violaciones.stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    @DisplayName("un request completo con fecha futura no tiene violaciones")
    void requestValido() {
        CitaRequest request = new CitaRequest(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(7));

        assertThat(mensajesDe(request)).isEmpty();
    }

    @Test
    @DisplayName("pacienteId nulo es rechazado")
    void pacienteIdNulo() {
        CitaRequest request = new CitaRequest(null, UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        assertThat(mensajesDe(request)).containsExactly("El paciente es obligatorio");
    }

    @Test
    @DisplayName("medicoId nulo es rechazado")
    void medicoIdNulo() {
        CitaRequest request = new CitaRequest(UUID.randomUUID(), null, LocalDateTime.now().plusDays(1));

        assertThat(mensajesDe(request)).containsExactly("El médico es obligatorio");
    }

    @Test
    @DisplayName("fechaHora nula es rechazada")
    void fechaHoraNula() {
        CitaRequest request = new CitaRequest(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(mensajesDe(request)).containsExactly("La fecha y hora son obligatorias");
    }

    @Test
    @DisplayName("una fecha pasada viola @Future")
    void fechaPasada() {
        CitaRequest request = new CitaRequest(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusDays(1));

        assertThat(mensajesDe(request)).containsExactly("La fecha debe ser futura");
    }

    @Test
    @DisplayName("un request completamente vacío acumula las tres violaciones")
    void requestVacio() {
        assertThat(mensajesDe(new CitaRequest(null, null, null)))
                .containsExactlyInAnyOrder(
                        "El paciente es obligatorio",
                        "El médico es obligatorio",
                        "La fecha y hora son obligatorias");
    }

    @Test
    @DisplayName("@Future solo protege la capa web: CitaService no valida que la fecha sea futura")
    void futureSoloAplicaEnElController() {
        CitaRequest conFechaPasada = new CitaRequest(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.of(2020, 8, 3, 10, 0));

        // El validador la rechaza (así lo hace @Valid en CitaController)...
        assertThat(mensajesDe(conFechaPasada)).containsExactly("La fecha debe ser futura");
        // ...pero CitaService.reservarCita la aceptaría; ver CitaServiceTest#fechaPasadaNoEsValidadaPorElService.
    }
}
