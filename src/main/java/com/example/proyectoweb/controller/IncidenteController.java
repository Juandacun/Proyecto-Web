package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.service.IncidenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/incidentes", "/incidentes"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Incidentes", description = "API REST para reporte de fallas y bloqueo automático por incidentes críticos (RN-04)")
public class IncidenteController {

    @Autowired
    private IncidenteService incidenteService;

    // GET /api/incidentes - Listar incidentes con filtros opcionales
    @Operation(summary = "Listar incidentes", description = "Obtiene la lista de incidentes o novedades reportadas, con filtros opcionales por ID de recurso y severidad.")
    @ApiResponse(responseCode = "200", description = "Lista de incidentes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Incidente>> listar(
            @Parameter(description = "Filtrar por ID del recurso afectado")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "Filtrar por severidad: Leve, Media, Crítica")
            @RequestParam(required = false) String severidad) {

        return ResponseEntity.ok(incidenteService.listarFiltrado(recursoId, severidad));
    }

    // GET /api/incidentes/{id} - Detalle de un incidente
    @Operation(summary = "Buscar incidente por ID", description = "Obtiene los detalles de un incidente específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidente encontrado"),
            @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Incidente> buscarPorId(@PathVariable Long id) {
        Incidente incidente = incidenteService.buscarPorId(id);
        if (incidente == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(incidente);
    }

    // POST /api/incidentes - Reportar nuevo incidente (RN-04)
    @Operation(summary = "Reportar incidente", description = "Registra un incidente sobre un recurso. Si la severidad es 'Crítica', el recurso pasa automáticamente a 'Bloqueado' (RN-04).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incidente registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de incidente inválidos o recurso inexistente")
    })
    @PostMapping
    public ResponseEntity<?> reportar(@RequestBody IncidenteDTO dto) {
        try {
            Incidente nuevo = incidenteService.registrarIncidente(dto);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(nuevo.getIdIncidente())
                    .toUri();
            return ResponseEntity.created(location).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}