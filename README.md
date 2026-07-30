# MediSalud API

Sistema de agendamiento de citas médicas — API REST desarrollada como prueba técnica.

Permite registrar médicos y pacientes, reservar citas, consultar disponibilidad por franjas horarias, cancelar y reprogramar citas, aplicando penalizaciones por cancelaciones tardías.

## Tecnologías

- **Java 21**
- **Spring Boot 4.1.0** (Web, Data JPA, Validation)
- **Gradle**
- **PostgreSQL** (Neon, base de datos en la nube)
- **Lombok**
- **springdoc-openapi** (Swagger UI)
- **JUnit 5 + Mockito + AssertJ** — pruebas unitarias

## Arquitectura

Arquitectura por capas (controller → service → repository → model), elegida sobre una arquitectura hexagonal para priorizar el tiempo disponible en pruebas y documentación dentro del plazo establecido.

```
com.angemau.medisalud
├── controller     # Endpoints REST
├── service        # Lógica de negocio y reglas (RN-01 a RN-06)
├── repository     # Spring Data JPA
├── model          # Entidades JPA
├── dto            # Records de entrada (CitaRequest, ReprogramarCitaRequest)
├── exception       # Excepciones de negocio + GlobalExceptionHandler
└── config          # Configuración (OpenAPI)
```

**Por qué capas y no hexagonal:** con un plazo corto, una arquitectura hexagonal agrega indirecciones (puertos/adaptadores) que no aportan valor cuando el dominio es simple y hay un solo consumidor (la API REST). Se prefirió invertir ese tiempo en cobertura de pruebas y en cubrir bien los casos borde de las reglas de negocio.

**Manejo de errores:** centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`). Cada excepción de negocio se mapea a un código HTTP específico y todas las respuestas de error siguen el mismo formato (`timestamp`, `status`, `mensaje`).

## Cómo ejecutar el proyecto localmente

### Requisitos
- Java 21
- Una base de datos PostgreSQL accesible (el proyecto usa Neon en la nube)

### Variables de entorno

```
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<password>
```

### Pasos

```powershell
git clone https://github.com/angemau/ceiba-medisalud.git
cd medisalud

# configurar las variables de entorno anteriores en el sistema o en un .env

.\gradlew.bat bootRun
```

La API queda disponible en `http://localhost:8080`.

### Documentación interactiva (Swagger)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Spec OpenAPI (JSON): `http://localhost:8080/v3/api-docs`

### Ejecutar las pruebas

```powershell
.\gradlew.bat test
```

Reporte HTML: `build/reports/tests/test/index.html`

## Reglas de negocio implementadas

| Regla | Descripción |
|---|---|
| RN-01 | Franjas horarias: L-V 08:00-18:00, sáb 08:00-13:00, dom cerrado; slots de 30 min |
| RN-02 | Un médico no puede tener dos citas en la misma franja |
| RN-03 | No se aceptan fechas de nacimiento futuras (si no se informa, se asume edad 0) |
| RN-04 | Un paciente no puede duplicar cita con el mismo médico en el mismo horario |
| RN-05 | Cancelación con menos de 2h de antelación → penalización; 3+ penalizaciones en 30 días bloquean al paciente |
| RN-06 | Reprogramación = cancelar la cita anterior (aplica RN-05) + reservar una nueva (aplica RN-01, RN-02, RN-04, RN-05) |

## Endpoints

### Médicos

**`POST /api/medicos`** — Registrar médico

```json
// Request
{
  "nombreCompleto": "Dra. María González",
  "especialidad": "Cardiología",
  "telefono": "555-1001",
  "email": "maria.gonzalez@medisalud.com"
}
```
```json
// Response 201
{
  "id": "b3f1c2a0-...",
  "nombreCompleto": "Dra. María González",
  "especialidad": "Cardiología",
  "telefono": "555-1001",
  "email": "maria.gonzalez@medisalud.com"
}
```

**`GET /api/medicos`** — Listar médicos → 200, array de médicos

**`GET /api/medicos/{id}`** — Obtener médico por id → 200, o 404 si no existe

### Pacientes

**`POST /api/pacientes`** — Registrar paciente

```json
// Request
{
  "nombreCompleto": "Juan Pérez",
  "documentoIdentidad": "1020304050",
  "telefono": "3001234567",
  "email": "juan.perez@example.com",
  "fechaNacimiento": "1990-05-14"
}
```
```json
// Response 201
{
  "id": "a7e4d1b2-...",
  "nombreCompleto": "Juan Pérez",
  "documentoIdentidad": "1020304050",
  "telefono": "3001234567",
  "email": "juan.perez@example.com",
  "fechaNacimiento": "1990-05-14"
}
```
Documento duplicado → 409.

**`GET /api/pacientes`** — Listar pacientes → 200

**`GET /api/pacientes/{id}`** — Obtener paciente por id → 200, o 404 si no existe

### Citas

**`POST /api/citas`** — Reservar cita

```json
// Request
{
  "pacienteId": "a7e4d1b2-...",
  "medicoId": "b3f1c2a0-...",
  "fechaHora": "2026-08-10T09:00:00"
}
```
```json
// Response 201
{
  "id": "c9d8e7f6-...",
  "paciente": { "id": "a7e4d1b2-...", "nombreCompleto": "Juan Pérez", "..." : "..." },
  "medico": { "id": "b3f1c2a0-...", "nombreCompleto": "Dra. María González", "..." : "..." },
  "fechaHora": "2026-08-10T09:00:00",
  "estado": "PROGRAMADA",
  "fechaCancelacion": null
}
```

Posibles errores: `404` (paciente/médico no existe), `400` (horario inválido), `403` (paciente bloqueado por penalizaciones), `409` (conflicto de horario médico o paciente).

**`GET /api/citas/disponibilidad?medicoId={id}&fechaInicio=2026-08-10&fechaFin=2026-08-10`** — Franjas disponibles

```json
// Response 200
["2026-08-10T08:00:00", "2026-08-10T08:30:00", "2026-08-10T09:30:00", "..."]
```

**`PATCH /api/citas/{id}/cancelar`** — Cancelar cita

```json
// Response 200
{
  "id": "c9d8e7f6-...",
  "estado": "CANCELADA",
  "fechaCancelacion": "2026-07-30T15:20:00",
  "..." : "..."
}
```
Si se cancela con menos de 2h de antelación, se registra una penalización para el paciente automáticamente.

**`PATCH /api/citas/{id}/reprogramar`** — Reprogramar cita

```json
// Request
{
  "nuevaFechaHora": "2026-08-12T10:00:00"
}
```
```json
// Response 200 — nueva cita creada
{
  "id": "d1e2f3a4-...",
  "estado": "PROGRAMADA",
  "fechaHora": "2026-08-12T10:00:00",
  "..." : "..."
}
```

**`GET /api/citas`** — Listar citas con filtros opcionales

```
GET /api/citas?medicoId={id}&estado=PROGRAMADA&fechaInicio=2026-08-01T00:00:00&fechaFin=2026-08-31T23:59:00
```
→ 200, array de citas que cumplen los filtros enviados (todos son opcionales y combinables).

### Formato de error (todas las excepciones de negocio)

```json
{
  "timestamp": "2026-07-30T15:20:00",
  "status": 409,
  "mensaje": "El médico ya tiene una cita en ese horario"
}
```

| Excepción | Status |
|---|---|
| `RecursoNoEncontradoException` | 404 |
| `CitaConflictException` | 409 |
| `PacienteBloqueadoException` | 403 |
| `HorarioInvalidoException` | 400 |
| `DocumentoDuplicadoException` | 409 |
| `EdadInvalidaException` | 400 |

## Pruebas automatizadas

144 pruebas unitarias con JUnit 5 + Mockito, sin levantar contexto de Spring, cubriendo las 6 reglas de negocio incluyendo sus valores frontera (bordes de horario, bordes de penalización, orden de validaciones).

## Limitaciones conocidas

- **`reservarCita` y `cancelarCita` no tienen `@Transactional`.** En `reprogramarCita`, si la cancelación de la cita original se completa pero la creación de la nueva cita falla (por ejemplo, el nuevo horario ya está ocupado), no hay rollback: el paciente pierde la cita original sin obtener la nueva. Se documenta como mejora pendiente.
- `listarCitas` ignora el filtro de fecha si solo se envía uno de los dos extremos (`fechaInicio` o `fechaFin`).
- La unicidad del documento de identidad del paciente se valida a nivel de aplicación, no con una restricción `UNIQUE` en base de datos, por lo que existe una ventana de condición de carrera en escrituras concurrentes.
- No se implementó paginación en `listarCitas` ni índices adicionales en la tabla `Cita` (estado, fechaHora, medico_id); se decidió no implementarlo por tiempo.

## Posibles mejoras futuras

- Inyectar un `Clock` en `CitaService` para hacer determinista la lógica de cancelación/penalización (hoy depende de `LocalDateTime.now()`).
- Extraer la lógica de horarios repetida entre `validarFranjaHorariaValida` y `generarFranjasPosibles` a una clase compartida (`HorarioLaboral`).
- Agregar `@Transactional` a las operaciones que combinan más de una escritura.
- Despliegue en la nube (Railway/Render).
