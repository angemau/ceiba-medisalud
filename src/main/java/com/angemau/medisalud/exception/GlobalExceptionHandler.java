package com.angemau.medisalud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());  // 404
    }

    @ExceptionHandler(CitaConflictException.class)
    public ResponseEntity<Object> handleConflicto(CitaConflictException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());  // 409
    }

    private ResponseEntity<Object> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "mensaje", mensaje
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(PacienteBloqueadoException.class)
    public ResponseEntity<Object> handlePacienteBloqueado(PacienteBloqueadoException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());  // 403
    }

    @ExceptionHandler(HorarioInvalidoException.class)
    public ResponseEntity<Object> handleHorarioInvalido(HorarioInvalidoException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());  // 400
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ResponseEntity<Object> handleDocumentoDuplicado(DocumentoDuplicadoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());  // 409
    }
}
