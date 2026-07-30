package com.angemau.medisalud.util;

import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;
import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.model.Penalizacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datos de prueba compartidos.
 *
 * <p>Las fechas son fijas y no dependen de {@code now()}, para que la suite sea determinista
 * sin importar el día en que se ejecute. {@code CitaService.reservarCita} no valida que la fecha
 * sea futura (eso lo hace {@code @Future} en el controller), así que fechas fijas son seguras.
 */
public final class TestDataFactory {

    /** 2026-08-03 es lunes. */
    public static final LocalDate LUNES = LocalDate.of(2026, 8, 3);
    /** 2026-08-07 es viernes. */
    public static final LocalDate VIERNES = LocalDate.of(2026, 8, 7);
    /** 2026-08-08 es sábado. */
    public static final LocalDate SABADO = LocalDate.of(2026, 8, 8);
    /** 2026-08-09 es domingo. */
    public static final LocalDate DOMINGO = LocalDate.of(2026, 8, 9);

    /** Lunes a las 10:00: franja válida usada como caso base. */
    public static final LocalDateTime FRANJA_VALIDA = LUNES.atTime(10, 0);

    private TestDataFactory() {
    }

    public static Paciente unPaciente() {
        return unPaciente(UUID.randomUUID());
    }

    public static Paciente unPaciente(UUID id) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNombreCompleto("Ana Restrepo");
        paciente.setDocumentoIdentidad("1020304050");
        paciente.setTelefono("3001234567");
        paciente.setEmail("ana.restrepo@example.com");
        return paciente;
    }

    public static Medico unMedico() {
        return unMedico(UUID.randomUUID());
    }

    public static Medico unMedico(UUID id) {
        Medico medico = new Medico();
        medico.setId(id);
        medico.setNombreCompleto("Carlos Jaramillo");
        medico.setEspecialidad("Medicina General");
        medico.setTelefono("6041234567");
        medico.setEmail("carlos.jaramillo@example.com");
        return medico;
    }

    public static Cita unaCita(Paciente paciente, Medico medico, LocalDateTime fechaHora, EstadoCita estado) {
        Cita cita = new Cita();
        cita.setId(UUID.randomUUID());
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setFechaHora(fechaHora);
        cita.setEstado(estado);
        return cita;
    }

    public static Cita unaCitaProgramada(LocalDateTime fechaHora) {
        return unaCita(unPaciente(), unMedico(), fechaHora, EstadoCita.PROGRAMADA);
    }

    /** Devuelve {@code cantidad} penalizaciones para el paciente dado, todas dentro de los últimos 30 días. */
    public static List<Penalizacion> penalizaciones(Paciente paciente, int cantidad) {
        List<Penalizacion> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(new Penalizacion(paciente, LocalDateTime.now().minusDays(i + 1L)));
        }
        return lista;
    }

    /**
     * Réplica de las franjas que {@code CitaService} genera para un día:
     * de 08:00 hasta 17:30 entre semana y hasta 12:30 los sábados, cada 30 minutos.
     * Los domingos no generan franjas.
     */
    public static List<LocalDateTime> franjasEsperadasDe(LocalDate fecha) {
        List<LocalDateTime> franjas = new ArrayList<>();
        switch (fecha.getDayOfWeek()) {
            case SUNDAY -> {
                return franjas;
            }
            case SATURDAY -> agregarFranjas(franjas, fecha, LocalTime.of(13, 0));
            default -> agregarFranjas(franjas, fecha, LocalTime.of(18, 0));
        }
        return franjas;
    }

    private static void agregarFranjas(List<LocalDateTime> destino, LocalDate fecha, LocalTime horaFin) {
        for (LocalTime hora = LocalTime.of(8, 0); hora.isBefore(horaFin); hora = hora.plusMinutes(30)) {
            destino.add(LocalDateTime.of(fecha, hora));
        }
    }
}
