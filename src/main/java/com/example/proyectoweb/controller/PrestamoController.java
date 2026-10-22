package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.PrestamoDevolucionDTO;
import com.example.proyectoweb.dto.PrestamoEntregaDTO;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.service.PrestamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/prestamos", "/prestamos"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Préstamos", description = "API REST para gestión, entrega, devolución y control de puntualidad de equipos (RN-02, RN-03)")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    // GET /api/prestamos - Listar préstamos con filtros
    @Operation(summary = "Listar préstamos", description = "Obtiene la lista de préstamos con filtros opcionales por equipo (recursoId), usuario/documento y estado activo.")
    @ApiResponse(responseCode = "200", description = "Lista de préstamos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Prestamo>> listar(
            @Parameter(description = "Filtrar por ID del recurso / equipo")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "Filtrar por documento o identificador del usuario responsable")
            @RequestParam(required = false) String usuarioId,
            @Parameter(description = "Filtrar por activos (true: en curso, false: devueltos)")
            @RequestParam(required = false) Boolean activo) {

        return ResponseEntity.ok(prestamoService.listarFiltrado(recursoId, usuarioId, activo));
    }

    // GET /api/prestamos/{id} - Detalle de préstamo
    @Operation(summary = "Buscar préstamo por ID", description = "Obtiene el detalle completo de un préstamo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Préstamo encontrado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Préstamo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Prestamo> buscarPorId(@PathVariable Long id) {
        Prestamo prestamo = prestamoService.buscarPorId(id);
        if (prestamo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prestamo);
    }

    // GET /api/prestamos/activos - Listar préstamos en curso
    @Operation(summary = "Listar préstamos activos", description = "Obtiene únicamente los préstamos en curso que aún no han sido devueltos.")
    @ApiResponse(responseCode = "200", description = "Lista de préstamos activos obtenida exitosamente")
    @GetMapping("/activos")
    public ResponseEntity<List<Prestamo>> listarActivos() {
        return ResponseEntity.ok(prestamoService.listarActivos());
    }

    // GET /api/prestamos/tardios - Listar préstamos tardíos o vencidos
    @Operation(summary = "Listar préstamos tardíos", description = "Obtiene los préstamos cuya fecha límite ya venció o cuya devolución se registró fuera del plazo.")
    @ApiResponse(responseCode = "200", description = "Lista de préstamos tardíos obtenida exitosamente")
    @GetMapping("/tardios")
    public ResponseEntity<List<Prestamo>> listarTardios() {
        return ResponseEntity.ok(prestamoService.listarTardios());
    }

    // POST /api/prestamos/entrega y POST /api/prestamos - Registrar entrega de equipo (RN-02, RN-03)
    @Operation(summary = "Registrar entrega de equipo", description = "Registra un nuevo préstamo validando que el equipo no esté bloqueado (RN-02) ni prestado previamente (RN-03), y transiciona el recurso a 'Prestado'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Préstamo registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros de entrega inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto por equipo bloqueado (RN-02) o equipo ya prestado (RN-03)")
    })
    @PostMapping(path = {"", "/entrega"})
    public ResponseEntity<?> registrarEntrega(@RequestBody PrestamoEntregaDTO dto) {
        try {
            Prestamo guardado = prestamoService.registrarEntrega(dto);
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/prestamos/{id}")
                    .buildAndExpand(guardado.getIdPrestamo())
                    .toUri();
            return ResponseEntity.created(location).body(guardado);
        } catch (IllegalStateException e) {
            // Violación de reglas de negocio RN-02 o RN-03
            String codigo = e.getMessage().contains("RN-02") ? "RN-02" : "RN-03";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", e.getMessage(),
                    "codigo", codigo
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/prestamos/{id}/devolucion (y GET /devolucion para compatibilidad) - Registrar devolución de equipo
    @Operation(summary = "Registrar devolución de equipo", description = "Registra la devolución del equipo, calcula si fue puntual o tardía y libera el recurso a 'Disponible'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devolución registrada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Préstamo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El préstamo ya fue devuelto")
    })
    @RequestMapping(value = "/{id}/devolucion", method = {RequestMethod.PATCH, RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> registrarDevolucion(
            @PathVariable Long id,
            @RequestBody(required = false) PrestamoDevolucionDTO dto) {
        try {
            Prestamo devuelto = prestamoService.registrarDevolucion(id, dto);
            if (devuelto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El préstamo con ID " + id + " no existe."));
            }
            return ResponseEntity.ok(devuelto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
