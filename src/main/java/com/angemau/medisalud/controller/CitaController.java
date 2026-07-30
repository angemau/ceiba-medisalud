package com.angemau.medisalud.controller;

import com.angemau.medisalud.dto.CitaRequest;
import com.angemau.medisalud.model.Cita;
import com.angemau.medisalud.model.EstadoCita;
import com.angemau.medisalud.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<Cita> reservar(@Valid @RequestBody CitaRequest request) {
        Cita cita = citaService.reservarCita(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cita);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<LocalDateTime>> consultarDisponibilidad(
            @RequestParam UUID medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(citaService.consultarDisponibilidad(medicoId, fechaInicio, fechaFin));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Cita> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(citaService.cancelarCita(id));
    }

    @GetMapping
    public ResponseEntity<List<Cita>> listar(
            @RequestParam(required = false) UUID medicoId,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return ResponseEntity.ok(citaService.listarCitas(medicoId, pacienteId, estado, fechaInicio, fechaFin));
    }
}