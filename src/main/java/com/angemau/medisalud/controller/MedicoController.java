package com.angemau.medisalud.controller;

import com.angemau.medisalud.model.Medico;
import com.angemau.medisalud.service.MedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoService medicoService;

    @PostMapping
    public ResponseEntity<Medico> crear(@Valid @RequestBody Medico medico) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicoService.crear(medico));
    }

    @GetMapping
    public ResponseEntity<List<Medico>> listar() {
        return ResponseEntity.ok(medicoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(medicoService.obtenerPorId(id));
    }
}