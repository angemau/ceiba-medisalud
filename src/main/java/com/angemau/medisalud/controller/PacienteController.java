package com.angemau.medisalud.controller;

import com.angemau.medisalud.dto.PacienteRequest;
import com.angemau.medisalud.model.Paciente;
import com.angemau.medisalud.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<Paciente> crear(@Valid @RequestBody PacienteRequest paciente) {
        Paciente creado = pacienteService.crear(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<Paciente>> listar() {
        return ResponseEntity.ok(pacienteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }
}