package com.angemau.medisalud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Errores de @Valid en el body (ej: PacienteRequest con email inválido)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "mensaje", "Error de validación",
                "errores", errores
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 2. JSON mal formado o vacío en el body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido");
    }

    // 3. Parámetro con tipo incorrecto (ej: ?estado=INEXISTENTE, o un UUID mal formado en el path)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parámetro '%s' tiene un valor inválido: '%s'"
                .formatted(ex.getName(), ex.getValue());
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 4. Falta un @RequestParam obligatorio (ej: /disponibilidad sin medicoId)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleParametroFaltante(MissingServletRequestParameterException ex) {
        String mensaje = "El parámetro obligatorio '%s' no fue enviado".formatted(ex.getParameterName());
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 5. Cualquier otra excepción no controlada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenerico(Exception ex) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    private ResponseEntity<Object> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "mensaje", mensaje
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());  // 404
    }

    @ExceptionHandler(CitaConflictException.class)
    public ResponseEntity<Object> handleConflicto(CitaConflictException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());  // 409
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

    @ExceptionHandler(EdadInvalidaException.class)
    public ResponseEntity<Object> handleEdadInvalida(EdadInvalidaException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());  // 400
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<Object> handleEstadoInvalido(EstadoInvalidoException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());  // 409
    }
}
