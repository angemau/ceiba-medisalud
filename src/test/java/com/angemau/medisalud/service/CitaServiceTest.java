package com.angemau.medisalud.service;

import com.angemau.medisalud.dto.CitaRequest;
import com.angemau.medisalud.exception.CitaConflictException;
import com.angemau.medisalud.exception.HorarioInvalidoException;
import com.angemau.medisalud.exception.PacienteBloqueadoException;
import com.angemau.medisalud.exception.RecursoNoEncontradoException;
import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;
import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.model.Penalizacion;
import com.angemau.medisalud.repository.CitaRepository;
import com.angemau.medisalud.repository.MedicoRepository;
import com.angemau.medisalud.repository.PacienteRepository;
import com.angemau.medisalud.repository.PenalizacionRepository;
import com.angemau.medisalud.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.angemau.medisalud.util.TestDataFactory.DOMINGO;
import static com.angemau.medisalud.util.TestDataFactory.FRANJA_VALIDA;
import static com.angemau.medisalud.util.TestDataFactory.LUNES;
import static com.angemau.medisalud.util.TestDataFactory.SABADO;
import static com.angemau.medisalud.util.TestDataFactory.franjasEsperadasDe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitaService")
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private PenalizacionRepository penalizacionRepository;

    @InjectMocks
    private CitaService citaService;

    private Paciente paciente;
    private Medico medico;
    private UUID pacienteId;
    private UUID medicoId;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        medicoId = UUID.randomUUID();
        paciente = TestDataFactory.unPaciente(pacienteId);
        medico = TestDataFactory.unMedico(medicoId);
    }

    private CitaRequest requestPara(LocalDateTime fechaHora) {
        return new CitaRequest(pacienteId, medicoId, fechaHora);
    }

    /** Stub mínimo para llegar a la validación de horario. */
    private void dadoPacienteYMedicoExistentes() {
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
    }

    /** Stub completo para que la reserva llegue hasta el {@code save}. */
    private void dadoTodoEnOrdenPara(LocalDateTime fechaHora) {
        dadoPacienteYMedicoExistentes();
        when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, fechaHora, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(
                pacienteId, medicoId, fechaHora, EstadoCita.PROGRAMADA))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // =====================================================================================
    @Nested
    @DisplayName("reservarCita")
    class ReservarCita {

        @Test
        @DisplayName("guarda la cita en estado PROGRAMADA con paciente, médico y fecha correctos")
        void reservaValida() {
            dadoTodoEnOrdenPara(FRANJA_VALIDA);

            Cita resultado = citaService.reservarCita(requestPara(FRANJA_VALIDA));

            ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
            verify(citaRepository).save(captor.capture());
            Cita guardada = captor.getValue();

            assertThat(guardada.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
            assertThat(guardada.getPaciente()).isSameAs(paciente);
            assertThat(guardada.getMedico()).isSameAs(medico);
            assertThat(guardada.getFechaHora()).isEqualTo(FRANJA_VALIDA);
            assertThat(guardada.getFechaCancelacion()).isNull();
            assertThat(resultado).isSameAs(guardada);
        }

        @Test
        @DisplayName("lanza RecursoNoEncontrado si el paciente no existe, sin consultar el médico")
        void pacienteInexistente() {
            when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Paciente no encontrado");

            verifyNoInteractions(medicoRepository);
            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza RecursoNoEncontrado si el médico no existe")
        void medicoInexistente() {
            when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Médico no encontrado");

            verify(citaRepository, never()).save(any());
        }

        // --- RN-01: franja horaria -------------------------------------------------------
        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "2026-08-09T10:00, No hay atención los domingos",
                "2026-08-03T07:30, La hora está fuera del horario de atención",
                "2026-08-03T07:59, La hora está fuera del horario de atención",
                "2026-08-03T18:00, La hora está fuera del horario de atención",
                "2026-08-03T20:00, La hora está fuera del horario de atención",
                "2026-08-08T13:00, La hora está fuera del horario de atención",
                "2026-08-08T17:00, La hora está fuera del horario de atención",
                "2026-08-03T10:15, Las citas solo se pueden agendar en franjas de 30 minutos",
                "2026-08-03T10:45, Las citas solo se pueden agendar en franjas de 30 minutos",
                "2026-08-03T10:01, Las citas solo se pueden agendar en franjas de 30 minutos"
        })
        @DisplayName("RN-01: rechaza franjas fuera del horario de atención")
        void franjaInvalida(LocalDateTime fechaHora, String mensajeEsperado) {
            dadoPacienteYMedicoExistentes();

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(fechaHora)))
                    .isInstanceOf(HorarioInvalidoException.class)
                    .hasMessage(mensajeEsperado);

            verify(citaRepository, never()).save(any());
            verifyNoInteractions(penalizacionRepository);
        }

        @ParameterizedTest(name = "{0} es una franja válida")
        @CsvSource({
                "2026-08-03T08:00",  // lunes, borde inferior inclusive
                "2026-08-03T17:30",  // lunes, último slot (el corte es < 18:00)
                "2026-08-07T12:30",  // viernes, media jornada
                "2026-08-08T08:00",  // sábado, borde inferior
                "2026-08-08T12:30"   // sábado, último slot (el corte es < 13:00)
        })
        @DisplayName("RN-01: acepta los bordes válidos del horario de atención")
        void franjaValidaEnLosBordes(LocalDateTime fechaHora) {
            dadoTodoEnOrdenPara(fechaHora);

            Cita resultado = citaService.reservarCita(requestPara(fechaHora));

            assertThat(resultado.getFechaHora()).isEqualTo(fechaHora);
            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        }

        // --- RN-05: paciente bloqueado por penalizaciones ---------------------------------
        @Test
        @DisplayName("RN-05: bloquea al paciente con 3 penalizaciones en los últimos 30 días")
        void pacienteConTresPenalizaciones() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(TestDataFactory.penalizaciones(paciente, 3));

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(PacienteBloqueadoException.class)
                    .hasMessageContaining("3 penalizaciones");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN-05: permite reservar con 2 penalizaciones (borde inferior)")
        void pacienteConDosPenalizaciones() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(TestDataFactory.penalizaciones(paciente, 2));
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA))
                    .thenReturn(false);
            when(citaRepository.existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(
                    pacienteId, medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA)).thenReturn(false);
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            Cita resultado = citaService.reservarCita(requestPara(FRANJA_VALIDA));

            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.PROGRAMADA);
        }

        @Test
        @DisplayName("RN-05: el mensaje reporta el número real de penalizaciones")
        void pacienteConCuatroPenalizaciones() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(TestDataFactory.penalizaciones(paciente, 4));

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(PacienteBloqueadoException.class)
                    .hasMessageContaining("4 penalizaciones")
                    .hasMessageContaining("no puede agendar citas");
        }

        @Test
        @DisplayName("RN-05: consulta las penalizaciones desde hace exactamente 30 días")
        void ventanaDeTreintaDias() {
            dadoTodoEnOrdenPara(FRANJA_VALIDA);
            LocalDateTime esperado = LocalDateTime.now().minusDays(30);

            citaService.reservarCita(requestPara(FRANJA_VALIDA));

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(penalizacionRepository).findByPacienteIdAndFechaAfter(eq(pacienteId), captor.capture());
            assertThat(captor.getValue()).isCloseTo(esperado, within(5, ChronoUnit.SECONDS));
        }

        // --- RN-02 / RN-04: conflictos ----------------------------------------------------
        @Test
        @DisplayName("RN-02: rechaza si el médico ya tiene una cita PROGRAMADA en ese horario")
        void medicoOcupado() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA))
                    .thenReturn(true);

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(CitaConflictException.class)
                    .hasMessage("El médico ya tiene una cita en ese horario");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("RN-04: rechaza si el paciente ya tiene una cita con ese médico en ese horario")
        void pacienteDuplicaCitaConElMismoMedico() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA))
                    .thenReturn(false);
            when(citaRepository.existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(
                    pacienteId, medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA)).thenReturn(true);

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .isInstanceOf(CitaConflictException.class)
                    .hasMessage("El paciente ya tiene una cita con este médico en ese horario");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("el conflicto de médico se evalúa antes que el de paciente")
        void prioridadDelConflictoDeMedico() {
            dadoPacienteYMedicoExistentes();
            when(penalizacionRepository.findByPacienteIdAndFechaAfter(eq(pacienteId), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado(medicoId, FRANJA_VALIDA, EstadoCita.PROGRAMADA))
                    .thenReturn(true);

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(FRANJA_VALIDA)))
                    .hasMessage("El médico ya tiene una cita en ese horario");

            verify(citaRepository, never())
                    .existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(any(), any(), any(), any());
        }

        // --- orden de validaciones --------------------------------------------------------
        @Test
        @DisplayName("el horario se valida antes que las penalizaciones del paciente")
        void horarioTienePrioridadSobrePenalizaciones() {
            dadoPacienteYMedicoExistentes();

            assertThatThrownBy(() -> citaService.reservarCita(requestPara(DOMINGO.atTime(10, 0))))
                    .isInstanceOf(HorarioInvalidoException.class);

            verifyNoInteractions(penalizacionRepository);
        }

        @Test
        @DisplayName("acepta una fecha pasada: @Future solo se aplica en el controller, no en el service")
        void fechaPasadaNoEsValidadaPorElService() {
            LocalDateTime lunesDe2020 = LocalDate.of(2020, 8, 3).atTime(10, 0);
            dadoTodoEnOrdenPara(lunesDe2020);

            Cita resultado = citaService.reservarCita(requestPara(lunesDe2020));

            assertThat(resultado.getFechaHora()).isEqualTo(lunesDe2020);
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("consultarDisponibilidad")
    class ConsultarDisponibilidad {

        private void dadoMedicoExistenteSinCitas() {
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
            when(citaRepository.findByMedicoIdAndFechaHoraBetween(eq(medicoId), any(), any()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("lanza RecursoNoEncontrado si el médico no existe y no consulta las citas")
        void medicoInexistente() {
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.consultarDisponibilidad(medicoId, LUNES, LUNES))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Médico no encontrado");

            verifyNoInteractions(citaRepository);
        }

        @Test
        @DisplayName("un lunes libre tiene 20 franjas, de 08:00 a 17:30")
        void lunesCompleto() {
            dadoMedicoExistenteSinCitas();

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, LUNES, LUNES);

            assertThat(disponibles).hasSize(20)
                    .startsWith(LUNES.atTime(8, 0))
                    .endsWith(LUNES.atTime(17, 30))
                    .containsExactlyElementsOf(franjasEsperadasDe(LUNES));
        }

        @Test
        @DisplayName("un sábado libre tiene 10 franjas, de 08:00 a 12:30")
        void sabadoCompleto() {
            dadoMedicoExistenteSinCitas();

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, SABADO, SABADO);

            assertThat(disponibles).hasSize(10)
                    .startsWith(SABADO.atTime(8, 0))
                    .endsWith(SABADO.atTime(12, 30));
        }

        @Test
        @DisplayName("un domingo no tiene franjas")
        void domingoSinFranjas() {
            dadoMedicoExistenteSinCitas();

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, DOMINGO, DOMINGO);

            assertThat(disponibles).isEmpty();
        }

        @Test
        @DisplayName("una semana completa (lunes a domingo) tiene 110 franjas: 5x20 + 10 + 0")
        void semanaCompleta() {
            dadoMedicoExistenteSinCitas();

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, LUNES, DOMINGO);

            assertThat(disponibles).hasSize(110);
            assertThat(disponibles).noneMatch(f -> f.toLocalDate().equals(DOMINGO));
        }

        @Test
        @DisplayName("excluye la franja ocupada por una cita PROGRAMADA")
        void excluyeFranjaOcupada() {
            LocalDateTime ocupada = LUNES.atTime(10, 0);
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
            when(citaRepository.findByMedicoIdAndFechaHoraBetween(eq(medicoId), any(), any()))
                    .thenReturn(List.of(TestDataFactory.unaCita(paciente, medico, ocupada, EstadoCita.PROGRAMADA)));

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, LUNES, LUNES);

            assertThat(disponibles).hasSize(19).doesNotContain(ocupada);
            assertThat(disponibles).contains(LUNES.atTime(9, 30), LUNES.atTime(10, 30));
        }

        @Test
        @DisplayName("una cita CANCELADA no bloquea la franja")
        void citaCanceladaLiberaLaFranja() {
            LocalDateTime franja = LUNES.atTime(10, 0);
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
            when(citaRepository.findByMedicoIdAndFechaHoraBetween(eq(medicoId), any(), any()))
                    .thenReturn(List.of(TestDataFactory.unaCita(paciente, medico, franja, EstadoCita.CANCELADA)));

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, LUNES, LUNES);

            assertThat(disponibles).hasSize(20).contains(franja);
        }

        @Test
        @DisplayName("una cita ATENDIDA tampoco bloquea la franja")
        void citaAtendidaNoBloquea() {
            LocalDateTime franja = LUNES.atTime(11, 0);
            when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(medico));
            when(citaRepository.findByMedicoIdAndFechaHoraBetween(eq(medicoId), any(), any()))
                    .thenReturn(List.of(TestDataFactory.unaCita(paciente, medico, franja, EstadoCita.ATENDIDA)));

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, LUNES, LUNES);

            assertThat(disponibles).hasSize(20).contains(franja);
        }

        @Test
        @DisplayName("un rango invertido (inicio posterior al fin) devuelve lista vacía")
        void rangoInvertido() {
            dadoMedicoExistenteSinCitas();

            List<LocalDateTime> disponibles = citaService.consultarDisponibilidad(medicoId, DOMINGO, LUNES);

            assertThat(disponibles).isEmpty();
        }

        @Test
        @DisplayName("consulta las citas en el rango [inicio 00:00, fin 23:59]")
        void rangoConsultadoAlRepositorio() {
            dadoMedicoExistenteSinCitas();

            citaService.consultarDisponibilidad(medicoId, LUNES, SABADO);

            ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(citaRepository)
                    .findByMedicoIdAndFechaHoraBetween(eq(medicoId), desde.capture(), hasta.capture());

            assertThat(desde.getValue()).isEqualTo(LUNES.atStartOfDay());
            assertThat(hasta.getValue()).isEqualTo(SABADO.atTime(23, 59));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("cancelarCita")
    class CancelarCita {

        private final UUID citaId = UUID.randomUUID();

        /**
         * Las fechas se construyen relativas a {@code now()} porque el service invoca
         * {@code LocalDateTime.now()} directamente (no hay {@code Clock} inyectado).
         */
        private Cita dadaUnaCitaEn(LocalDateTime fechaHora) {
            Cita cita = TestDataFactory.unaCita(paciente, medico, fechaHora, EstadoCita.PROGRAMADA);
            when(citaRepository.findById(citaId)).thenReturn(Optional.of(cita));
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
            return cita;
        }

        @Test
        @DisplayName("lanza RecursoNoEncontrado si la cita no existe")
        void citaInexistente() {
            when(citaRepository.findById(citaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.cancelarCita(citaId))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Cita no encontrada");

            verifyNoInteractions(penalizacionRepository);
        }

        @Test
        @DisplayName("con más de 2 horas de antelación cancela sin penalización")
        void cancelacionConAntelacionSuficiente() {
            LocalDateTime dentroDeTresHoras = LocalDateTime.now().plusHours(3);
            dadaUnaCitaEn(dentroDeTresHoras);

            Cita resultado = citaService.cancelarCita(citaId);

            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.CANCELADA);
            assertThat(resultado.getFechaCancelacion()).isNotNull();
            verify(penalizacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("con menos de 2 horas de antelación registra una penalización al paciente de la cita")
        void cancelacionTardiaGeneraPenalizacion() {
            dadaUnaCitaEn(LocalDateTime.now().plusMinutes(90));

            Cita resultado = citaService.cancelarCita(citaId);

            ArgumentCaptor<Penalizacion> captor = ArgumentCaptor.forClass(Penalizacion.class);
            verify(penalizacionRepository).save(captor.capture());
            Penalizacion penalizacion = captor.getValue();

            assertThat(penalizacion.getPaciente()).isSameAs(paciente);
            assertThat(penalizacion.getFecha()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        }

        @Test
        @DisplayName("borde: a 2h01m no penaliza porque toHours() trunca a 2")
        void bordeSuperiorNoPenaliza() {
            dadaUnaCitaEn(LocalDateTime.now().plusHours(2).plusMinutes(1));

            citaService.cancelarCita(citaId);

            verify(penalizacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("borde: a 1h59m sí penaliza porque toHours() trunca a 1")
        void bordeInferiorPenaliza() {
            dadaUnaCitaEn(LocalDateTime.now().plusHours(1).plusMinutes(59));

            citaService.cancelarCita(citaId);

            verify(penalizacionRepository).save(any(Penalizacion.class));
        }

        @Test
        @DisplayName("una cita ya vencida se cancela con penalización (antelación negativa)")
        void citaEnElPasadoPenaliza() {
            dadaUnaCitaEn(LocalDateTime.now().minusHours(1));

            Cita resultado = citaService.cancelarCita(citaId);

            verify(penalizacionRepository).save(any(Penalizacion.class));
            assertThat(resultado.getEstado()).isEqualTo(EstadoCita.CANCELADA);
        }

        @Test
        @DisplayName("la fecha de cancelación se registra con el instante actual")
        void registraFechaDeCancelacion() {
            dadaUnaCitaEn(LocalDateTime.now().plusDays(1));

            Cita resultado = citaService.cancelarCita(citaId);

            assertThat(resultado.getFechaCancelacion())
                    .isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("comportamiento actual: cancelar una cita ya CANCELADA vuelve a penalizar (ver README)")
        void doblecancelacionVuelveAPenalizar() {
            Cita yaCancelada = TestDataFactory.unaCita(
                    paciente, medico, LocalDateTime.now().plusMinutes(30), EstadoCita.CANCELADA);
            when(citaRepository.findById(citaId)).thenReturn(Optional.of(yaCancelada));
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.cancelarCita(citaId);

            // El service no valida el estado previo, por lo que se genera una segunda penalización.
            verify(penalizacionRepository).save(any(Penalizacion.class));
        }
    }

    // =====================================================================================
    @Nested
    @DisplayName("listarCitas")
    class ListarCitas {

        @Test
        @DisplayName("sin filtros delega en findAll con una Specification no nula")
        void sinFiltros() {
            List<Cita> esperadas = List.of(TestDataFactory.unaCitaProgramada(FRANJA_VALIDA));
            when(citaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(esperadas);

            List<Cita> resultado = citaService.listarCitas(null, null, null, null, null);

            assertThat(resultado).isEqualTo(esperadas);
            verify(citaRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
        }

        @Test
        @DisplayName("con todos los filtros combinados delega en findAll sin lanzar excepción")
        void todosLosFiltros() {
            when(citaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(List.of());

            List<Cita> resultado = citaService.listarCitas(
                    medicoId, pacienteId, EstadoCita.PROGRAMADA,
                    LUNES.atStartOfDay(), SABADO.atTime(23, 59));

            assertThat(resultado).isEmpty();
            verify(citaRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
        }

        @Test
        @DisplayName("con un solo filtro devuelve lo que entrega el repositorio")
        void filtroPorEstado() {
            List<Cita> esperadas = List.of(TestDataFactory.unaCitaProgramada(FRANJA_VALIDA));
            when(citaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(esperadas);

            assertThat(citaService.listarCitas(null, null, EstadoCita.CANCELADA, null, null))
                    .isEqualTo(esperadas);
        }

        @Test
        @DisplayName("devuelve lista vacía, nunca null, cuando el repositorio no encuentra nada")
        void sinResultados() {
            when(citaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                    .thenReturn(List.of());

            assertThat(citaService.listarCitas(medicoId, null, null, null, null))
                    .isNotNull()
                    .isEmpty();
        }
    }
}
