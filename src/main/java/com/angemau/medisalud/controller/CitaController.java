package com.angemau.medisalud.controller;

import com.angemau.medisalud.dto.CitaRequest;
import com.angemau.medisalud.dto.CitaResponse;
import com.angemau.medisalud.dto.ReprogramarCitaRequest;
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

import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@Valid @RequestBody CitaRequest request) {
        Cita cita = citaService.reservarCita(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CitaResponse.from(cita));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<LocalDateTime>> consultarDisponibilidad(
            @RequestParam UUID medicoId,
            @Parameter(description = "Fecha en formato AAAA-MM-DD", example = "2026-08-03") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha en formato AAAA-MM-DD", example = "2026-08-03") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(citaService.consultarDisponibilidad(medicoId, fechaInicio, fechaFin));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(CitaResponse.from(citaService.cancelarCita(id)));
    }

    @GetMapping
    public ResponseEntity<List<CitaResponse>> listar(
            @RequestParam(required = false) UUID medicoId,
            @RequestParam(required = false) UUID pacienteId,
            @RequestParam(required = false) EstadoCita estado,
            @Parameter(description = "Fecha y hora en formato AAAA-MM-DDTHH:mm:ss", example = "2026-08-03T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaInicio,
            @Parameter(description = "Fecha y hora en formato AAAA-MM-DDTHH:mm:ss", example = "2026-08-03T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fechaFin) {
        List<CitaResponse> citas = citaService.listarCitas(medicoId, pacienteId, estado, fechaInicio, fechaFin)
                .stream()
                .map(CitaResponse::from)
                .toList();
        return ResponseEntity.ok(citas);
    }

    @PatchMapping("/{id}/reprogramar")
    public ResponseEntity<CitaResponse> reprogramar(@PathVariable UUID id,
                                                    @Valid @RequestBody ReprogramarCitaRequest request) {
        Cita cita = citaService.reprogramarCita(id, request.nuevaFechaHora());
        return ResponseEntity.ok(CitaResponse.from(cita));
    }
}