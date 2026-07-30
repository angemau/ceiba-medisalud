package com.angemau.medisalud.model;

import com.angemau.medisalud.util.TestDataFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de las restricciones Bean Validation de las entidades.
 * No levantan contexto de Spring: usan un {@link Validator} construido a mano.
 */
@DisplayName("Validaciones de las entidades")
class ValidacionesModeloTest {

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

    private static <T> List<String> mensajesDe(T objeto) {
        Set<ConstraintViolation<T>> violaciones = validator.validate(objeto);
        return violaciones.stream().map(ConstraintViolation::getMessage).toList();
    }

    // =====================================================================================
    @Nested
    @DisplayName("Paciente")
    class ValidacionPaciente {

        @Test
        @DisplayName("un paciente completo y bien formado no tiene violaciones")
        void pacienteValido() {
            assertThat(mensajesDe(TestDataFactory.unPaciente())).isEmpty();
        }

        @ParameterizedTest(name = "nombreCompleto = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   ", "Ab"})
        @DisplayName("rechaza nombres vacíos o de menos de 3 caracteres")
        void nombreInvalido(String nombre) {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setNombreCompleto(nombre);

            assertThat(mensajesDe(paciente)).isNotEmpty();
        }

        @Test
        @DisplayName("rechaza nombres de más de 100 caracteres")
        void nombreDemasiadoLargo() {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setNombreCompleto("A".repeat(101));

            assertThat(mensajesDe(paciente)).contains("El nombre debe tener entre 3 y 100 caracteres");
        }

        @Test
        @DisplayName("acepta un nombre de exactamente 100 caracteres")
        void nombreEnElLimite() {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setNombreCompleto("A".repeat(100));

            assertThat(mensajesDe(paciente)).isEmpty();
        }

        @Test
        @DisplayName("rechaza documentos de menos de 7 caracteres")
        void documentoDemasiadoCorto() {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setDocumentoIdentidad("123456");

            assertThat(mensajesDe(paciente)).contains("El documento debe tener mínimo 7 caracteres");
        }

        @Test
        @DisplayName("acepta un documento de exactamente 7 caracteres")
        void documentoEnElLimite() {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setDocumentoIdentidad("1234567");

            assertThat(mensajesDe(paciente)).isEmpty();
        }

        @ParameterizedTest(name = "telefono = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "123456", "300abc4567", "300-123-4567"})
        @DisplayName("el teléfono es obligatorio y debe tener mínimo 7 dígitos")
        void telefonoInvalido(String telefono) {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setTelefono(telefono);

            assertThat(mensajesDe(paciente)).isNotEmpty();
        }

        @ParameterizedTest(name = "email = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "sin-arroba", "otro@"})
        @DisplayName("el email es obligatorio y debe tener formato válido")
        void emailInvalido(String email) {
            Paciente paciente = TestDataFactory.unPaciente();
            paciente.setEmail(email);

            assertThat(mensajesDe(paciente)).isNotEmpty();
        }

        @Test
        @DisplayName("un paciente vacío acumula una violación por cada campo obligatorio")
        void pacienteVacio() {
            assertThat(validator.validate(new Paciente())).hasSize(4);
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("Medico")
    class ValidacionMedico {

        @Test
        @DisplayName("un médico completo y bien formado no tiene violaciones")
        void medicoValido() {
            assertThat(mensajesDe(TestDataFactory.unMedico())).isEmpty();
        }

        @Test
        @DisplayName("teléfono y email son OPCIONALES en Médico: null es válido")
        void contactoOpcionalEnNulo() {
            Medico medico = TestDataFactory.unMedico();
            medico.setTelefono(null);
            medico.setEmail(null);

            assertThat(mensajesDe(medico)).isEmpty();
        }

        @Test
        @DisplayName("el teléfono vacío es válido porque la regex admite ^$")
        void telefonoVacioEsValido() {
            Medico medico = TestDataFactory.unMedico();
            medico.setTelefono("");

            assertThat(mensajesDe(medico)).isEmpty();
        }

        @ParameterizedTest(name = "telefono = \"{0}\"")
        @ValueSource(strings = {"123456", "abcdefg", "604 123 4567", "+576041234567"})
        @DisplayName("rechaza teléfonos que no sean 7 o más dígitos")
        void telefonoInvalido(String telefono) {
            Medico medico = TestDataFactory.unMedico();
            medico.setTelefono(telefono);

            assertThat(mensajesDe(medico)).contains("El teléfono debe tener mínimo 7 dígitos");
        }

        @ParameterizedTest(name = "nombreCompleto = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   ", "Ab"})
        @DisplayName("el nombre completo es obligatorio y debe tener al menos 3 caracteres")
        void nombreInvalido(String nombre) {
            Medico medico = TestDataFactory.unMedico();
            medico.setNombreCompleto(nombre);

            assertThat(mensajesDe(medico)).isNotEmpty();
        }

        @ParameterizedTest(name = "especialidad = \"{0}\"")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("la especialidad es obligatoria")
        void especialidadObligatoria(String especialidad) {
            Medico medico = TestDataFactory.unMedico();
            medico.setEspecialidad(especialidad);

            assertThat(mensajesDe(medico)).contains("La especialidad es obligatoria");
        }

        @Test
        @DisplayName("rechaza un email con formato inválido")
        void emailInvalido() {
            Medico medico = TestDataFactory.unMedico();
            medico.setEmail("no-es-email");

            assertThat(mensajesDe(medico)).contains("El email no tiene un formato válido");
        }

        @Test
        @DisplayName("un médico vacío solo viola nombre y especialidad (contacto opcional)")
        void medicoVacio() {
            assertThat(validator.validate(new Medico())).hasSize(2);
        }
    }
}
