package com.angemau.medisalud.service;

import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.repository.MedicoRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicoService")
class MedicoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @InjectMocks
    private MedicoService medicoService;

    @Test
    @DisplayName("crear delega en el repositorio y devuelve el médico guardado")
    void crear() {
        Medico aGuardar = TestDataFactory.unMedico(null);
        Medico guardado = TestDataFactory.unMedico(UUID.randomUUID());
        when(medicoRepository.save(aGuardar)).thenReturn(guardado);

        Medico resultado = medicoService.crear(aGuardar);

        assertThat(resultado).isSameAs(guardado);
        verify(medicoRepository).save(aGuardar);
    }

    @Test
    @DisplayName("listarTodos devuelve la lista del repositorio")
    void listarTodos() {
        List<Medico> medicos = List.of(TestDataFactory.unMedico(), TestDataFactory.unMedico());
        when(medicoRepository.findAll()).thenReturn(medicos);

        assertThat(medicoService.listarTodos()).isEqualTo(medicos).hasSize(2);
    }

    @Test
    @DisplayName("listarTodos devuelve lista vacía cuando no hay médicos")
    void listarTodosVacio() {
        when(medicoRepository.findAll()).thenReturn(List.of());

        assertThat(medicoService.listarTodos()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("obtenerPorId devuelve el médico cuando existe")
    void obtenerPorIdExistente() {
        UUID id = UUID.randomUUID();
        Medico medico = TestDataFactory.unMedico(id);
        when(medicoRepository.findById(id)).thenReturn(Optional.of(medico));

        assertThat(medicoService.obtenerPorId(id)).isSameAs(medico);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontrado cuando el médico no existe")
    void obtenerPorIdInexistente() {
        UUID id = UUID.randomUUID();
        when(medicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.obtenerPorId(id))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Médico no encontrado");
    }
}
