package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.UbicacionService;
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
@RequestMapping({"/api/ubicaciones", "/ubicaciones"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Ubicaciones", description = "API REST para gestión y consulta de ubicaciones y sedes del campus")
public class UbicacionController {

    @Autowired
    private UbicacionService service;

    // GET /api/ubicaciones - Listar todas las ubicaciones
    @Operation(summary = "Listar ubicaciones", description = "Obtiene la lista completa de ubicaciones y edificios del campus.")
    @ApiResponse(responseCode = "200", description = "Lista de ubicaciones obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Ubicacion>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // GET /api/ubicaciones/{id} - Detalle de ubicación
    @Operation(summary = "Buscar ubicación por ID", description = "Retorna el detalle de una ubicación específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ubicación encontrada"),
            @ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Ubicacion> buscarPorId(@PathVariable Long id) {
        Ubicacion ubicacion = service.buscarPorId(id);
        if (ubicacion == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ubicacion);
    }

    // POST /api/ubicaciones - Crear nueva ubicación
    @Operation(summary = "Crear nueva ubicación", description = "Registra una nueva ubicación o edificio en el campus.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ubicación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de ubicación inválidos")
    })
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Ubicacion ubicacion) {
        try {
            Ubicacion guardada = service.guardar(ubicacion);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(guardada.getIdUbicacion())
                    .toUri();
            return ResponseEntity.created(location).body(guardada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/ubicaciones/{id} - Actualizar ubicación existente
    @Operation(summary = "Actualizar ubicación", description = "Actualiza el nombre, edificio y descripción de una ubicación existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ubicación actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ubicación no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos de ubicación inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Ubicacion ubicacion) {
        try {
            Ubicacion actualizada = service.actualizar(id, ubicacion);
            if (actualizada == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La ubicación con ID " + id + " no existe."));
            }
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/ubicaciones/{id} - Eliminar ubicación
    @Operation(summary = "Eliminar ubicación", description = "Elimina una ubicación por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ubicación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean eliminada = service.eliminar(id);
        if (!eliminada) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La ubicación con ID " + id + " no existe."));
        }
        return ResponseEntity.noContent().build();
    }
}