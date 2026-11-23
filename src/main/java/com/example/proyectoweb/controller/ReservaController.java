package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.ReservaDTO;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.security.SecurityUtils;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/reservas", "/reservas"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Reservas", description = "API REST para gestión, reserva y control de horarios de espacios y salas (RN-01, RN-02)")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private RecursoService recursoService;

    @Autowired
    private SecurityUtils securityUtils;

    // GET /api/reservas - Listar reservas con filtros opcionales
    @Operation(summary = "Listar reservas", description = "Obtiene la lista de reservas. Usuarios solicitantes (ROLE_USER) solo visualizan sus propias reservas; gestores y administradores visualizan todas.")
    @ApiResponse(responseCode = "200", description = "Lista de reservas obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Reserva>> listar(
            @Parameter(description = "Filtrar por ID del recurso reservado")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "Filtrar por fecha de reserva (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Filtrar por estado textual: Confirmada, Cancelada, Finalizada")
            @RequestParam(required = false) String estado) {

        List<Reserva> reservas = reservaService.filtrar(recursoId, fecha, estado);
        if (!securityUtils.esGestorOAdmin()) {
            reservas = reservas.stream()
                    .filter(securityUtils::esPropietarioDeReserva)
                    .toList();
        }
        return ResponseEntity.ok(reservas);
    }

    // GET /api/reservas/{id} - Obtener reserva por ID
    @Operation(summary = "Buscar reserva por ID", description = "Obtiene el detalle de una reserva específica. Requiere que pertenezca al usuario solicitante autenticado o rol gestor/admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: no es propietario de la reserva"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id);
        if (reserva == null) {
            return ResponseEntity.notFound().build();
        }
        if (!securityUtils.esPropietarioDeReserva(reserva)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado: solo puedes consultar tus propias reservas."));
        }
        return ResponseEntity.ok(reserva);
    }

    // GET /api/reservas/recurso/{recursoId} - Listar reservas de un recurso específico
    @Operation(summary = "Listar reservas de un recurso", description = "Obtiene todas las reservas asociadas a un espacio o sala.")
    @ApiResponse(responseCode = "200", description = "Lista de reservas del recurso obtenida exitosamente")
    @GetMapping("/recurso/{recursoId}")
    public ResponseEntity<List<Reserva>> listarPorRecurso(@PathVariable Long recursoId) {
        return ResponseEntity.ok(reservaService.listarPorRecurso(recursoId));
    }

    // GET /api/reservas/verificar-disponibilidad - Validar solapamiento y bloqueo previo (RN-01, RN-02)
    @Operation(summary = "Verificar disponibilidad de reserva", description = "Verifica si un recurso está disponible y sin solapamiento de horario (RN-01, RN-02) sin persistir cambios.")
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(
            @RequestParam Long recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin,
            @RequestParam(required = false) Long reservaIdExcluir) {

        Recurso recurso = recursoService.buscarPorId(recursoId);
        if (recurso == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "disponible", false,
                    "error", "El recurso con ID " + recursoId + " no existe."
            ));
        }
        if (recurso.getClase() != Recurso.Clase.ESPACIO) {
            return ResponseEntity.badRequest().body(Map.of(
                    "disponible", false,
                    "error", "Solo se reservan espacios y salas. Los equipos se gestionan mediante préstamos."
            ));
        }
        if (recurso.estaBloqueado()) {
            return ResponseEntity.ok(Map.of(
                    "disponible", false,
                    "motivo", "RN-02",
                    "mensaje", "El recurso se encuentra bloqueado por daño o mantenimiento (RN-02)."
            ));
        }
        if (recurso.estaPrestado()) {
            return ResponseEntity.ok(Map.of(
                    "disponible", false,
                    "motivo", "RN-02",
                    "mensaje", "El recurso figura en préstamo y no puede ser reservado (RN-02)."
            ));
        }
        String inicio;
        String fin;
        try {
            inicio = ReservaService.normalizarHora(horaInicio);
            fin = ReservaService.normalizarHora(horaFin);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("disponible", false, "error", e.getMessage()));
        }
        if (inicio.compareTo(fin) >= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "disponible", false,
                    "error", "La hora de inicio debe ser anterior a la hora de fin."
            ));
        }
        if (!LocalDateTime.of(fecha, LocalTime.parse(inicio)).isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "disponible", false,
                    "error", "No se puede reservar en una fecha u hora que ya pasó."
            ));
        }

        Optional<Reserva> conflicto = reservaService.buscarConflicto(reservaIdExcluir, recursoId, fecha, inicio, fin);
        if (conflicto.isPresent()) {
            Reserva otra = conflicto.get();
            return ResponseEntity.ok(Map.of(
                    "disponible", false,
                    "motivo", "RN-01",
                    "mensaje", "El recurso no está disponible: ya tiene una reserva de " + otra.getHoraInicio()
                            + " a " + otra.getHoraFin() + " ese día (RN-01).",
                    "conflictoInicio", otra.getHoraInicio(),
                    "conflictoFin", otra.getHoraFin()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "disponible", true,
                "mensaje", "El horario y recurso se encuentran disponibles."
        ));
    }

    // POST /api/reservas - Crear nueva reserva con RN-01 y RN-02
    @Operation(summary = "Crear nueva reserva", description = "Crea una nueva reserva validando que no haya solapamiento de horarios (RN-01) y que el recurso no esté bloqueado (RN-02). El solicitante se vincula automáticamente al usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de reserva inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto por solapamiento de horario (RN-01) o recurso bloqueado (RN-02)")
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ReservaDTO dto) {
        // Para usuarios no-gestor/admin, garantizar que la reserva pertenezca a su identidad autenticada
        if (!securityUtils.esGestorOAdmin()) {
            String currentLogin = securityUtils.getCurrentUsername();
            if (currentLogin != null && !currentLogin.isBlank()) {
                dto.setSolicitante(currentLogin);
            }
        }
        try {
            Reserva nueva = reservaService.crearDesdeDTO(dto);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(nueva.getIdReserva())
                    .toUri();
            return ResponseEntity.created(location).body(nueva);
        } catch (IllegalStateException e) {
            // Conflicto de regla de negocio (RN-01 solapamiento o RN-02 recurso bloqueado)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", e.getMessage(),
                    "codigo", codigoDe(e)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/reservas/{id} - Actualizar reserva existente con RN-01 y RN-02
    @Operation(summary = "Actualizar reserva", description = "Actualiza los datos de una reserva existente verificando solapamiento (RN-01), estado del recurso (RN-02) y pertenencia al usuario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva actualizada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: no es propietario de la reserva"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto por horario solapado (RN-01), recurso bloqueado (RN-02) o la reserva ya inició")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody ReservaDTO dto) {
        Reserva existente = reservaService.buscarPorId(id);
        if (existente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La reserva con ID " + id + " no existe."));
        }
        if (!securityUtils.esPropietarioDeReserva(existente)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado: no puedes modificar reservas pertenecientes a otros usuarios."));
        }

        try {
            Reserva actualizada = reservaService.actualizar(id, dto);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", e.getMessage(),
                    "codigo", codigoDe(e)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH o POST /api/reservas/{id}/cancelar - Cancelar reserva
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva Confirmada que aún no ha iniciado y pertenece al usuario autenticado (o gestor/admin).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva cancelada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: no es propietario de la reserva"),
            @ApiResponse(responseCode = "409", description = "La reserva ya inició o no está Confirmada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @RequestMapping(value = "/{id}/cancelar", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        Reserva existente = reservaService.buscarPorId(id);
        if (existente == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La reserva con ID " + id + " no existe."));
        }
        if (!securityUtils.esPropietarioDeReserva(existente)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado: no puedes cancelar reservas pertenecientes a otros usuarios."));
        }

        Reserva cancelada;
        try {
            cancelada = reservaService.cancelar(id);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage(), "codigo", codigoDe(e)));
        }
        return ResponseEntity.ok(cancelada);
    }

    // PATCH o POST /api/reservas/{id}/finalizar - Finalizar reserva
    @Operation(summary = "Finalizar reserva", description = "Marca la reserva como finalizada y libera el recurso si corresponde (ROLE_GESTOR o ROLE_ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva finalizada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol gestor o admin"),
            @ApiResponse(responseCode = "409", description = "La reserva no está Confirmada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @RequestMapping(value = "/{id}/finalizar", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> finalizar(@PathVariable Long id) {
        if (!securityUtils.esGestorOAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado: solo el gestor o administrador pueden finalizar reservas operativamente."));
        }
        Reserva finalizada;
        try {
            finalizada = reservaService.finalizar(id);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage(), "codigo", codigoDe(e)));
        }
        if (finalizada == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La reserva con ID " + id + " no existe."));
        }
        return ResponseEntity.ok(finalizada);
    }

    // DELETE /api/reservas/{id} - Eliminar reserva
    @Operation(summary = "Eliminar reserva", description = "Elimina físicamente una reserva por su ID (ROLE_ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reserva eliminada exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol admin"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!securityUtils.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Acceso denegado: solo el administrador del sistema puede eliminar reservas."));
        }
        boolean eliminada = reservaService.eliminar(id);
        if (!eliminada) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La reserva con ID " + id + " no existe."));
        }
        return ResponseEntity.noContent().build();
    }

    private static String codigoDe(IllegalStateException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("RN-01")) return "RN-01";
        if (m.contains("RN-02")) return "RN-02";
        return "ESTADO_RESERVA";
    }
}
