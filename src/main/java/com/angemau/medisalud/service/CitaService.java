package com.angemau.medisalud.service;

import com.angemau.medisalud.dto.CitaRequest;
import com.angemau.medisalud.exception.*;
import com.angemau.medisalud.model.*;
import com.angemau.medisalud.repository.CitaRepository;
import com.angemau.medisalud.repository.MedicoRepository;
import com.angemau.medisalud.repository.PacienteRepository;
import com.angemau.medisalud.repository.PenalizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PenalizacionRepository penalizacionRepository;

    public Cita reservarCita(CitaRequest request) {

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Paciente no encontrado"));
        Medico medico = medicoRepository.findById(request.medicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));

        validarEdadMinima(paciente);                             // RN-03
        validarFranjaHorariaValida(request.fechaHora());       // RN-01
        validarPacienteSinPenalizacionActiva(paciente);         // RN-05 (bloqueo)

        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado(
                medico.getId(), request.fechaHora(), EstadoCita.PROGRAMADA)) {
            throw new CitaConflictException("El médico ya tiene una cita en ese horario");  // RN-02
        }
        if (citaRepository.existsByPacienteIdAndMedicoIdAndFechaHoraAndEstado(
                paciente.getId(), medico.getId(), request.fechaHora(), EstadoCita.PROGRAMADA)) {
            throw new CitaConflictException("El paciente ya tiene una cita con este médico en ese horario");
        }

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaHora(request.fechaHora());
        cita.setEstado(EstadoCita.PROGRAMADA);
        return citaRepository.save(cita);
    }

    public List<LocalDateTime> consultarDisponibilidad(UUID medicoId, LocalDate fechaInicio, LocalDate fechaFin) {
        medicoRepository.findById(medicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Médico no encontrado"));

        List<LocalDateTime> todasLasFranjas = generarFranjasPosibles(fechaInicio, fechaFin);

        List<LocalDateTime> ocupadas = citaRepository
                .findByMedicoIdAndFechaHoraBetween(medicoId, fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59))
                .stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA)
                .map(Cita::getFechaHora)
                .toList();

        return todasLasFranjas.stream()
                .filter(f -> !ocupadas.contains(f))
                .toList();
    }

    public Cita cancelarCita(UUID citaId) {

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        long horasAntelacion = Duration.between(LocalDateTime.now(), cita.getFechaHora()).toHours();
        if (horasAntelacion < 2) {
            Penalizacion penalizacion = new Penalizacion(cita.getPaciente(), LocalDateTime.now());
            penalizacionRepository.save(penalizacion);
        }

        cita.setEstado(EstadoCita.CANCELADA);
        cita.setFechaCancelacion(LocalDateTime.now());
        return citaRepository.save(cita);
    }

    public List<Cita> listarCitas(UUID medicoId, UUID pacienteId, EstadoCita estado,
                                  LocalDateTime fechaInicio, LocalDateTime fechaFin) {

        Specification<Cita> spec = (root, query, cb) -> cb.conjunction();

        if (medicoId != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("medico").get("id"), medicoId));
        if (pacienteId != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("paciente").get("id"), pacienteId));
        if (estado != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        if (fechaInicio != null && fechaFin != null)
            spec = spec.and((root, query, cb) -> cb.between(root.get("fechaHora"), fechaInicio, fechaFin));

        return citaRepository.findAll(spec);
    }

    public Cita reprogramarCita(UUID citaId, LocalDateTime nuevaFechaHora) {
        Cita citaAnterior = citaRepository.findById(citaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        UUID pacienteId = citaAnterior.getPaciente().getId();
        UUID medicoId = citaAnterior.getMedico().getId();

        cancelarCita(citaId);        // paso 1 — aplica RN-05 si cancela con <2h

        CitaRequest nuevaRequest = new CitaRequest(pacienteId, medicoId, nuevaFechaHora);
        return reservarCita(nuevaRequest);   // pasos 2-3 — RN-01, RN-05 (bloqueo), RN-02, RN-04
    }

    private void validarPacienteSinPenalizacionActiva(Paciente paciente) {
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        List<Penalizacion> penalizacionesRecientes =
                penalizacionRepository.findByPacienteIdAndFechaAfter(paciente.getId(), hace30Dias);

        if (penalizacionesRecientes.size() >= 3) {
            throw new PacienteBloqueadoException(
                    "El paciente tiene " + penalizacionesRecientes.size() +
                            " penalizaciones en los últimos 30 días y no puede agendar citas");
        }
    }

    private void validarFranjaHorariaValida(LocalDateTime fechaHora) {
        DayOfWeek dia = fechaHora.getDayOfWeek();
        LocalTime hora = fechaHora.toLocalTime();

        if (dia == DayOfWeek.SUNDAY) {
            throw new HorarioInvalidoException("No hay atención los domingos");
        }

        LocalTime horaInicio = LocalTime.of(8, 0);
        LocalTime horaFin = (dia == DayOfWeek.SATURDAY) ? LocalTime.of(13, 0) : LocalTime.of(18, 0);

        if (hora.isBefore(horaInicio) || !hora.isBefore(horaFin)) {
            throw new HorarioInvalidoException("La hora está fuera del horario de atención");
        }

        if (hora.getMinute() != 0 && hora.getMinute() != 30) {
            throw new HorarioInvalidoException("Las citas solo se pueden agendar en franjas de 30 minutos");
        }
    }

    private List<LocalDateTime> generarFranjasPosibles(LocalDate inicio, LocalDate fin) {
        List<LocalDateTime> franjas = new ArrayList<>();
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
            if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

            LocalTime horaFin = (fecha.getDayOfWeek() == DayOfWeek.SATURDAY)
                    ? LocalTime.of(13, 0) : LocalTime.of(18, 0);
            LocalTime hora = LocalTime.of(8, 0);

            while (hora.isBefore(horaFin)) {
                franjas.add(LocalDateTime.of(fecha, hora));
                hora = hora.plusMinutes(30);
            }
        }
        return franjas;
    }

    private void validarEdadMinima(Paciente paciente) {
        if (paciente.getFechaNacimiento() != null
                && paciente.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new EdadInvalidaException("La fecha de nacimiento no puede ser futura");
        }
    }

}
