package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.MantenimientoDTO;
import com.example.proyectoweb.model.Mantenimiento;
import com.example.proyectoweb.service.MantenimientoService;
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
@RequestMapping({"/api/mantenimientos", "/mantenimientos"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Mantenimiento", description = "API REST para gestión de órdenes de trabajo, mantenimiento y liberación de recursos (RN-05)")
public class MantenimientoController {

    @Autowired
    private MantenimientoService mantenimientoService;

    // GET /api/mantenimientos - Listar órdenes de mantenimiento con filtros
    @Operation(summary = "Listar mantenimientos", description = "Obtiene la lista de órdenes de mantenimiento, con filtros opcionales por ID de recurso y estado.")
    @ApiResponse(responseCode = "200", description = "Lista de órdenes de mantenimiento obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Mantenimiento>> listar(
            @Parameter(description = "Filtrar por ID del recurso intervenido")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "Filtrar por estado: Abierta, En proceso, Finalizada, Cancelada")
            @RequestParam(required = false) String estado) {

        return ResponseEntity.ok(mantenimientoService.listarFiltrado(recursoId, estado));
    }

    // GET /api/mantenimientos/{id} - Detalle de orden de mantenimiento
    @Operation(summary = "Buscar mantenimiento por ID", description = "Obtiene el detalle de una orden de mantenimiento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mantenimiento encontrado"),
            @ApiResponse(responseCode = "404", description = "Mantenimiento no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Mantenimiento> buscarPorId(@PathVariable Long id) {
        Mantenimiento mantenimiento = mantenimientoService.buscarPorId(id);
        if (mantenimiento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mantenimiento);
    }

    // POST /api/mantenimientos - Crear orden de mantenimiento
    @Operation(summary = "Crear orden de mantenimiento", description = "Abre una nueva orden de mantenimiento sobre un recurso (incluso si está bloqueado) y asegura que permanezca bloqueado mientras dure la intervención.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden de mantenimiento creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de mantenimiento inválidos")
    })
    @PostMapping
    public ResponseEntity<?> crearOrden(@RequestBody MantenimientoDTO dto) {
        try {
            Mantenimiento nuevo = mantenimientoService.crearOrden(dto);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(nuevo.getIdMantenimiento())
                    .toUri();
            return ResponseEntity.created(location).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/mantenimientos/{id}/finalizar - Finalizar mantenimiento y liberar recurso (RN-05)
    @Operation(summary = "Finalizar orden de mantenimiento", description = "Cierra la orden de mantenimiento con estado 'Finalizada' y actualiza automáticamente el recurso a 'Disponible' (RN-05).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mantenimiento finalizado exitosamente y recurso liberado a Disponible"),
            @ApiResponse(responseCode = "404", description = "Orden de mantenimiento no encontrada")
    })
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<?> finalizar(@PathVariable Long id) {
        Mantenimiento finalizado = mantenimientoService.finalizar(id);
        if (finalizado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La orden de mantenimiento con ID " + id + " no existe."));
        }
        return ResponseEntity.ok(finalizado);
    }

    // PATCH /api/mantenimientos/{id}/estado - Cambiar estado de la intervención
    @Operation(summary = "Cambiar estado de mantenimiento", description = "Cambia el estado de una orden abierta o en proceso a 'En proceso', 'Finalizada' o 'Cancelada'. 'Finalizada' aplica RN-05; 'Cancelada' cierra la orden y deja el recurso bloqueado hasta que el gestor lo habilite.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado faltante o no válido"),
            @ApiResponse(responseCode = "404", description = "Orden no encontrada"),
            @ApiResponse(responseCode = "409", description = "La orden ya está finalizada o cancelada")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body != null ? body.get("estado") : null;
        try {
            Mantenimiento actualizado = mantenimientoService.cambiarEstado(id, nuevoEstado);
            if (actualizado == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La orden de mantenimiento con ID " + id + " no existe."));
            }
            return ResponseEntity.ok(actualizado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
