package com.angemau.medisalud.controller;

import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.service.MedicoService;
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

@WebMvcTest(MedicoController.class)
@DisplayName("MedicoController")
class MedicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicoService medicoService;

    private static final String MEDICO_VALIDO = """
            {
              "nombreCompleto": "Carlos Jaramillo",
              "especialidad": "Medicina General",
              "telefono": "6041234567",
              "email": "carlos.jaramillo@example.com"
            }
            """;

    @Test
    @DisplayName("POST /api/medicos devuelve 201 con el médico creado")
    void crearDevuelve201() throws Exception {
        Medico creado = TestDataFactory.unMedico(UUID.randomUUID());
        when(medicoService.crear(any(Medico.class))).thenReturn(creado);

        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEDICO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(creado.getId().toString()))
                .andExpect(jsonPath("$.nombreCompleto").value("Carlos Jaramillo"))
                .andExpect(jsonPath("$.especialidad").value("Medicina General"));
    }

    @Test
    @DisplayName("POST /api/medicos sin nombre ni especialidad devuelve 400 y no invoca el service")
    void crearInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto": "", "especialidad": ""}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicoService);
    }

    @Test
    @DisplayName("POST /api/medicos con teléfono de menos de 7 dígitos devuelve 400")
    void crearConTelefonoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto": "Carlos Jaramillo",
                                 "especialidad": "Medicina General",
                                 "telefono": "12345"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicoService);
    }

    @Test
    @DisplayName("POST /api/medicos sin teléfono ni email devuelve 201: el contacto es opcional")
    void crearSinContactoDevuelve201() throws Exception {
        when(medicoService.crear(any(Medico.class))).thenReturn(TestDataFactory.unMedico(UUID.randomUUID()));

        mockMvc.perform(post("/api/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto": "Carlos Jaramillo", "especialidad": "Medicina General"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/medicos devuelve 200 con la lista")
    void listarDevuelve200() throws Exception {
        when(medicoService.listarTodos())
                .thenReturn(List.of(TestDataFactory.unMedico(), TestDataFactory.unMedico()));

        mockMvc.perform(get("/api/medicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/medicos/{id} devuelve 200 con el médico")
    void obtenerDevuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicoService.obtenerPorId(id)).thenReturn(TestDataFactory.unMedico(id));

        mockMvc.perform(get("/api/medicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/medicos/{id} de un médico inexistente devuelve 404")
    void obtenerInexistenteDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicoService.obtenerPorId(id)).thenThrow(new RecursoNoEncontradoException("Médico no encontrado"));

        mockMvc.perform(get("/api/medicos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Médico no encontrado"));
    }

    @Test
    @DisplayName("GET /api/medicos/{id} con UUID mal formado devuelve 400")
    void obtenerConUuidInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/medicos/{id}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicoService);
    }
}
