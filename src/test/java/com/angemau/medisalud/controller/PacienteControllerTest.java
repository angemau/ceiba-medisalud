package com.angemau.medisalud.controller;

import com.angemau.medisalud.dto.PacienteRequest;
import com.angemau.medisalud.exception.DocumentoDuplicadoException;
import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.service.PacienteService;
import com.angemau.medisalud.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
@DisplayName("PacienteController")
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteService pacienteService;

    private static final String PACIENTE_VALIDO = """
            {
              "nombreCompleto": "Ana Restrepo",
              "documentoIdentidad": "1020304050",
              "telefono": "3001234567",
              "email": "ana.restrepo@example.com"
            }
            """;

    @Test
    @DisplayName("POST /api/pacientes devuelve 201 con el paciente creado")
    void crearDevuelve201() throws Exception {
        Paciente creado = TestDataFactory.unPaciente(UUID.randomUUID());
        when(pacienteService.crear(any(PacienteRequest.class))).thenReturn(creado);

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(creado.getId().toString()))
                .andExpect(jsonPath("$.documentoIdentidad").value("1020304050"));
    }

    @Test
    @DisplayName("POST /api/pacientes con documento duplicado devuelve 409")
    void crearDuplicadoDevuelve409() throws Exception {
        when(pacienteService.crear(any(PacienteRequest.class)))
                .thenThrow(new DocumentoDuplicadoException("Ya existe un paciente con el documento 1020304050"));

        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PACIENTE_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya existe un paciente con el documento 1020304050"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/pacientes con body vacío devuelve 400 y no invoca el service")
    void crearInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pacienteService);
    }

    @Test
    @DisplayName("POST /api/pacientes sin teléfono devuelve 400: en Paciente el contacto es obligatorio")
    void crearSinTelefonoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto": "Ana Restrepo",
                                 "documentoIdentidad": "1020304050",
                                 "email": "ana.restrepo@example.com"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pacienteService);
    }

    @Test
    @DisplayName("POST /api/pacientes con email mal formado devuelve 400")
    void crearConEmailInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto": "Ana Restrepo",
                                 "documentoIdentidad": "1020304050",
                                 "telefono": "3001234567",
                                 "email": "sin-arroba"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pacienteService);
    }

    @Test
    @DisplayName("GET /api/pacientes devuelve 200 con la lista")
    void listarDevuelve200() throws Exception {
        when(pacienteService.listar()).thenReturn(List.of(TestDataFactory.unPaciente()));

        mockMvc.perform(get("/api/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Ana Restrepo"));
    }

    @Test
    @DisplayName("GET /api/pacientes/{id} devuelve 200 con el paciente")
    void obtenerDevuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        when(pacienteService.obtenerPorId(id)).thenReturn(TestDataFactory.unPaciente(id));

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/pacientes/{id} de un paciente inexistente devuelve 404")
    void obtenerInexistenteDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(pacienteService.obtenerPorId(id))
                .thenThrow(new RecursoNoEncontradoException("Paciente no encontrado: " + id));

        mockMvc.perform(get("/api/pacientes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Paciente no encontrado: " + id));
    }
}
