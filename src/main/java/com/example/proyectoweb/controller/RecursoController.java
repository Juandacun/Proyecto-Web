package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.RecursoService;
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
@RequestMapping({"/api/recursos"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Recursos", description = "API REST para administración, consulta y gestión de estados de recursos (equipos y espacios)")
public class RecursoController {

    @Autowired
    private RecursoService service;

    // GET /api/recursos - Listar todos los recursos, con filtros opcionales
    @Operation(summary = "Listar recursos", description = "Obtiene todos los recursos o los filtra por clase (EQUIPO/ESPACIO), estado o disponibilidad.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de recursos obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<Recurso>> listar(
            @Parameter(description = "Filtrar por clase: EQUIPO o ESPACIO")
            @RequestParam(required = false) Recurso.Clase clase,
            @Parameter(description = "Filtrar por estado textual: Disponible, Reservado, Prestado, Bloqueado, etc.")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Filtrar únicamente recursos en estado Disponible")
            @RequestParam(required = false) Boolean disponible) {

        if (disponible != null && disponible) {
            if (clase != null) {
                return ResponseEntity.ok(clase == Recurso.Clase.EQUIPO
                        ? service.listarEquiposDisponibles()
                        : service.listarEspaciosDisponibles());
            }
            return ResponseEntity.ok(service.listarDisponibles());
        }

        if (clase != null) {
            return ResponseEntity.ok(service.listarPorClase(clase));
        }

        if (estado != null && !estado.isBlank()) {
            return ResponseEntity.ok(service.listarPorEstado(estado));
        }

        return ResponseEntity.ok(service.listar());
    }

    // GET /api/recursos/equipos - Listar solo equipos
    @Operation(summary = "Listar solo equipos", description = "Obtiene los recursos clasificados como EQUIPO (utilizados para préstamos).")
    @ApiResponse(responseCode = "200", description = "Lista de equipos obtenida exitosamente")
    @GetMapping("/equipos")
    public ResponseEntity<List<Recurso>> listarEquipos() {
        return ResponseEntity.ok(service.listarEquipos());
    }

    // GET /api/recursos/espacios - Listar solo espacios y salas
    @Operation(summary = "Listar solo espacios", description = "Obtiene los recursos clasificados como ESPACIO (utilizados para reservas).")
    @ApiResponse(responseCode = "200", description = "Lista de espacios obtenida exitosamente")
    @GetMapping("/espacios")
    public ResponseEntity<List<Recurso>> listarEspacios() {
        return ResponseEntity.ok(service.listarEspacios());
    }

    // GET /api/recursos/{id} - Obtener recurso por ID
    @Operation(summary = "Buscar recurso por ID", description = "Retorna el detalle de un recurso dado su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recurso encontrado"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Recurso> buscarPorId(@PathVariable Long id) {
        Recurso recurso = service.buscarPorId(id);
        if (recurso == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(recurso);
    }

    // POST /api/recursos - Crear nuevo recurso
    @Operation(summary = "Crear nuevo recurso", description = "Crea un nuevo recurso asociando obligatoriamente su categoría y ubicación.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recurso creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes")
    })
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Recurso recurso) {
        if (recurso.getNombre() == null || recurso.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del recurso es obligatorio."));
        }
        if (recurso.getTipo() == null || recurso.getTipo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El tipo del recurso es obligatorio."));
        }
        if (recurso.getCategoria() == null || recurso.getCategoria().getIdCategoria() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Selecciona una categoría válida para el recurso."));
        }
        if (recurso.getUbicacion() == null || recurso.getUbicacion().getIdUbicacion() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Selecciona una ubicación válida para el recurso."));
        }

        try {
            Recurso nuevo = service.guardar(recurso);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(nuevo.getIdRecurso())
                    .toUri();
            return ResponseEntity.created(location).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/recursos/{id} - Actualizar recurso completo
    @Operation(summary = "Actualizar recurso", description = "Actualiza los campos de un recurso existente por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recurso actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Recurso recurso) {
        try {
            Recurso actualizado = service.actualizar(id, recurso);
            if (actualizado == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "El recurso con ID " + id + " no existe."));
            }
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/recursos/{id}/estado - Actualizar solo el estado del recurso (RN-02, RN-04)
    @Operation(summary = "Actualizar estado de recurso", description = "Permite cambiar el estado de un recurso (ej: Disponible, Bloqueado, Prestado, etc.).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado del recurso actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado"),
            @ApiResponse(responseCode = "400", description = "Estado no especificado")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String nuevoEstado = body != null ? body.get("estado") : null;
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es obligatorio en el cuerpo de la petición."));
        }

        Recurso actualizado = service.cambiarEstado(id, nuevoEstado.trim());
        if (actualizado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El recurso con ID " + id + " no existe."));
        }
        return ResponseEntity.ok(actualizado);
    }

    // DELETE /api/recursos/{id} - Eliminar recurso
    @Operation(summary = "Eliminar recurso", description = "Elimina un recurso por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recurso eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean eliminado = service.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "El recurso con ID " + id + " no existe."));
        }
        return ResponseEntity.noContent().build();
    }
}