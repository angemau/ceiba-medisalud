package com.angemau.medisalud.service;

import com.angemau.medisalud.exception.DocumentoDuplicadoException;
import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.repository.PacienteRepository;
import com.angemau.medisalud.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteService")
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    @Test
    @DisplayName("crear guarda al paciente cuando el documento no está registrado")
    void crearConDocumentoNuevo() {
        Paciente aGuardar = TestDataFactory.unPaciente(null);
        Paciente guardado = TestDataFactory.unPaciente(UUID.randomUUID());
        when(pacienteRepository.existsByDocumentoIdentidad(aGuardar.getDocumentoIdentidad())).thenReturn(false);
        when(pacienteRepository.save(aGuardar)).thenReturn(guardado);

        Paciente resultado = pacienteService.crear(aGuardar);

        assertThat(resultado).isSameAs(guardado);
        verify(pacienteRepository).save(aGuardar);
    }

    @Test
    @DisplayName("crear lanza DocumentoDuplicado y no guarda cuando el documento ya existe")
    void crearConDocumentoDuplicado() {
        Paciente aGuardar = TestDataFactory.unPaciente(null);
        when(pacienteRepository.existsByDocumentoIdentidad("1020304050")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.crear(aGuardar))
                .isInstanceOf(DocumentoDuplicadoException.class)
                .hasMessage("Ya existe un paciente con el documento 1020304050");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("listar devuelve la lista del repositorio")
    void listar() {
        List<Paciente> pacientes = List.of(TestDataFactory.unPaciente());
        when(pacienteRepository.findAll()).thenReturn(pacientes);

        assertThat(pacienteService.listar()).isEqualTo(pacientes);
    }

    @Test
    @DisplayName("listar devuelve lista vacía cuando no hay pacientes")
    void listarVacio() {
        when(pacienteRepository.findAll()).thenReturn(List.of());

        assertThat(pacienteService.listar()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("obtenerPorId devuelve el paciente cuando existe")
    void obtenerPorIdExistente() {
        UUID id = UUID.randomUUID();
        Paciente paciente = TestDataFactory.unPaciente(id);
        when(pacienteRepository.findById(id)).thenReturn(Optional.of(paciente));

        assertThat(pacienteService.obtenerPorId(id)).isSameAs(paciente);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontrado incluyendo el id en el mensaje")
    void obtenerPorIdInexistente() {
        UUID id = UUID.randomUUID();
        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.obtenerPorId(id))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Paciente no encontrado: " + id);
    }
}
