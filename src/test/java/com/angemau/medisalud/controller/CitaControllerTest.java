package com.angemau.medisalud.controller;

import com.angemau.medisalud.dto.CitaRequest;
import com.angemau.medisalud.exception.CitaConflictException;
import com.angemau.medisalud.exception.HorarioInvalidoException;
import com.angemau.medisalud.exception.PacienteBloqueadoException;
import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;
import com.angemau.medisalud.service.CitaService;
import com.angemau.medisalud.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.angemau.medisalud.util.TestDataFactory.LUNES;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de slice web: valida el mapeo HTTP del controller y la traducción de excepciones
 * que hace {@code GlobalExceptionHandler} (se detecta solo por ser {@code @RestControllerAdvice}).
 */
@WebMvcTest(CitaController.class)
@DisplayName("CitaController")
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CitaService citaService;

    private static final UUID PACIENTE_ID = UUID.randomUUID();
    private static final UUID MEDICO_ID = UUID.randomUUID();

    /** Fecha futura relativa a la ejecución: {@code @Future} en CitaRequest la exige. */
    private static LocalDateTime fechaFutura() {
        return LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private static final java.time.format.DateTimeFormatter FORMATO_FECHA =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static String jsonRequest(String pacienteId, String medicoId, String fechaHora) {
        return """
                {"pacienteId": %s, "medicoId": %s, "fechaHora": %s}
                """.formatted(comillas(pacienteId), comillas(medicoId), comillas(fechaHora));
    }

    private static String comillas(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static String bodyValido() {
        return jsonRequest(PACIENTE_ID.toString(), MEDICO_ID.toString(), FORMATO_FECHA.format(fechaFutura()));
    }

    // --- POST /api/citas ---------------------------------------------------------------
    @Test
    @DisplayName("POST /api/citas devuelve 201 con la cita creada")
    void reservarDevuelve201() throws Exception {
        Cita creada = TestDataFactory.unaCitaProgramada(fechaFutura());
        when(citaService.reservarCita(any(CitaRequest.class))).thenReturn(creada);

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.id").value(creada.getId().toString()))
                .andExpect(jsonPath("$.paciente.nombreCompleto").value("Ana Restrepo"))
                .andExpect(jsonPath("$.medico.especialidad").value("Medicina General"))
                // no deben exponerse datos sensibles del paciente
                .andExpect(jsonPath("$.paciente.documentoIdentidad").doesNotExist())
                .andExpect(jsonPath("$.paciente.email").doesNotExist())
                .andExpect(jsonPath("$.paciente.telefono").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/citas con campos nulos devuelve 400 y no invoca el service")
    void reservarConBodyInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(null, null, null)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(citaService);
    }

    @Test
    @DisplayName("POST /api/citas con fecha pasada devuelve 400 por @Future")
    void reservarConFechaPasadaDevuelve400() throws Exception {
        String fechaPasada = LocalDateTime.now().minusDays(1).withNano(0).toString();

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest(PACIENTE_ID.toString(), MEDICO_ID.toString(), fechaPasada)))
                .andExpect(status().isBadRequest());

        verify(citaService, never()).reservarCita(any());
    }

    // --- traducción de excepciones del GlobalExceptionHandler ---------------------------
    @Test
    @DisplayName("RecursoNoEncontradoException se traduce a 404 con timestamp, status y mensaje")
    void recursoNoEncontradoDevuelve404() throws Exception {
        when(citaService.reservarCita(any(CitaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException("Paciente no encontrado"));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Paciente no encontrado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("CitaConflictException se traduce a 409")
    void conflictoDevuelve409() throws Exception {
        when(citaService.reservarCita(any(CitaRequest.class)))
                .thenThrow(new CitaConflictException("El médico ya tiene una cita en ese horario"));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El médico ya tiene una cita en ese horario"));
    }

    @Test
    @DisplayName("PacienteBloqueadoException se traduce a 403")
    void pacienteBloqueadoDevuelve403() throws Exception {
        when(citaService.reservarCita(any(CitaRequest.class)))
                .thenThrow(new PacienteBloqueadoException("El paciente tiene 3 penalizaciones"));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("El paciente tiene 3 penalizaciones"));
    }

    @Test
    @DisplayName("HorarioInvalidoException se traduce a 400")
    void horarioInvalidoDevuelve400() throws Exception {
        when(citaService.reservarCita(any(CitaRequest.class)))
                .thenThrow(new HorarioInvalidoException("No hay atención los domingos"));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("No hay atención los domingos"));
    }

    // --- GET /api/citas/disponibilidad ---------------------------------------------------
    @Test
    @DisplayName("GET /api/citas/disponibilidad devuelve 200 con las franjas libres")
    void disponibilidadDevuelve200() throws Exception {
        when(citaService.consultarDisponibilidad(MEDICO_ID, LUNES, LUNES))
                .thenReturn(List.of(LUNES.atTime(8, 0), LUNES.atTime(8, 30)));

        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("medicoId", MEDICO_ID.toString())
                        .param("fechaInicio", "2026-08-03")
                        .param("fechaFin", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value(startsWith("2026-08-03T08:00")))
                .andExpect(jsonPath("$[1]").value(startsWith("2026-08-03T08:30")));
    }

    @Test
    @DisplayName("GET /api/citas/disponibilidad sin medicoId devuelve 400")
    void disponibilidadSinMedicoIdDevuelve400() throws Exception {
        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("fechaInicio", "2026-08-03")
                        .param("fechaFin", "2026-08-03"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(citaService);
    }

    @Test
    @DisplayName("GET /api/citas/disponibilidad con fecha mal formada devuelve 400")
    void disponibilidadConFechaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("medicoId", MEDICO_ID.toString())
                        .param("fechaInicio", "03-08-2026")
                        .param("fechaFin", "2026-08-03"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/citas/disponibilidad propaga el 404 de un médico inexistente")
    void disponibilidadDeMedicoInexistenteDevuelve404() throws Exception {
        when(citaService.consultarDisponibilidad(any(), any(), any()))
                .thenThrow(new RecursoNoEncontradoException("Médico no encontrado"));

        mockMvc.perform(get("/api/citas/disponibilidad")
                        .param("medicoId", MEDICO_ID.toString())
                        .param("fechaInicio", "2026-08-03")
                        .param("fechaFin", "2026-08-03"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Médico no encontrado"));
    }

    // --- PATCH /api/citas/{id}/cancelar --------------------------------------------------
    @Test
    @DisplayName("PATCH /api/citas/{id}/cancelar devuelve 200 con la cita cancelada")
    void cancelarDevuelve200() throws Exception {
        UUID citaId = UUID.randomUUID();
        Cita cancelada = TestDataFactory.unaCita(
                TestDataFactory.unPaciente(), TestDataFactory.unMedico(),
                LUNES.atTime(10, 0), EstadoCita.CANCELADA);
        cancelada.setFechaCancelacion(LocalDateTime.now());
        when(citaService.cancelarCita(citaId)).thenReturn(cancelada);

        mockMvc.perform(patch("/api/citas/{id}/cancelar", citaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaCancelacion").exists());
    }

    @Test
    @DisplayName("PATCH /api/citas/{id}/cancelar con UUID mal formado devuelve 400")
    void cancelarConUuidInvalidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/citas/{id}/cancelar", "no-es-un-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(citaService);
    }

    @Test
    @DisplayName("PATCH /api/citas/{id}/cancelar de una cita inexistente devuelve 404")
    void cancelarCitaInexistenteDevuelve404() throws Exception {
        UUID citaId = UUID.randomUUID();
        when(citaService.cancelarCita(citaId)).thenThrow(new RecursoNoEncontradoException("Cita no encontrada"));

        mockMvc.perform(patch("/api/citas/{id}/cancelar", citaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cita no encontrada"));
    }

    // --- GET /api/citas ------------------------------------------------------------------
    @Test
    @DisplayName("GET /api/citas sin filtros pasa nulos al service")
    void listarSinFiltros() throws Exception {
        when(citaService.listarCitas(null, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(citaService).listarCitas(null, null, null, null, null);
    }

    @Test
    @DisplayName("GET /api/citas convierte y reenvía todos los filtros al service")
    void listarConTodosLosFiltros() throws Exception {
        LocalDateTime desde = LUNES.atStartOfDay();
        LocalDateTime hasta = LocalDate.of(2026, 8, 8).atTime(23, 59);
        when(citaService.listarCitas(
                eq(MEDICO_ID), eq(PACIENTE_ID), eq(EstadoCita.PROGRAMADA), eq(desde), eq(hasta)))
                .thenReturn(List.of(TestDataFactory.unaCitaProgramada(LUNES.atTime(10, 0))));

        mockMvc.perform(get("/api/citas")
                        .param("medicoId", MEDICO_ID.toString())
                        .param("pacienteId", PACIENTE_ID.toString())
                        .param("estado", "PROGRAMADA")
                        .param("fechaInicio", "2026-08-03T00:00:00")
                        .param("fechaFin", "2026-08-08T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("PROGRAMADA"));

        verify(citaService).listarCitas(MEDICO_ID, PACIENTE_ID, EstadoCita.PROGRAMADA, desde, hasta);
    }

    @Test
    @DisplayName("GET /api/citas con un estado inexistente devuelve 400")
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/citas").param("estado", "INEXISTENTE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(citaService);
    }
}
