package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.security.SecurityUtils;
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

    @Autowired
    private SecurityUtils securityUtils;

    // GET /api/incidentes - Listar incidentes con filtros opcionales
    @Operation(summary = "Listar incidentes", description = "Obtiene la lista de incidentes o novedades reportadas, con filtros opcionales por ID de recurso y severidad. Para solicitantes (ROLE_USER), anonimiza la identidad de otros reportantes por privacidad.")
    @ApiResponse(responseCode = "200", description = "Lista de incidentes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Incidente>> listar(
            @Parameter(description = "Filtrar por ID del recurso afectado")
            @RequestParam(required = false) Long recursoId,
            @Parameter(description = "Filtrar por severidad: Leve, Media, Crítica")
            @RequestParam(required = false) String severidad) {

        List<Incidente> lista = incidenteService.listarFiltrado(recursoId, severidad);
        if (!securityUtils.esGestorOAdmin()) {
            String currentUser = securityUtils.getCurrentUsername();
            lista = lista.stream()
                    .map(inc -> sanitizarParaUsuario(inc, currentUser))
                    .toList();
        }
        return ResponseEntity.ok(lista);
    }

    // GET /api/incidentes/{id} - Detalle de un incidente
    @Operation(summary = "Buscar incidente por ID", description = "Obtiene los detalles de un incidente específico. Protege la identidad de terceros para usuarios solicitantes.")
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
        if (!securityUtils.esGestorOAdmin()) {
            String currentUser = securityUtils.getCurrentUsername();
            incidente = sanitizarParaUsuario(incidente, currentUser);
        }
        return ResponseEntity.ok(incidente);
    }

    // POST /api/incidentes - Reportar nuevo incidente (RN-04)
    @Operation(summary = "Reportar incidente", description = "Registra un incidente sobre un recurso. La identidad del reportante se asocia al usuario autenticado en SecurityContext para evitar suplantación (RN-04).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incidente registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de incidente inválidos o recurso inexistente")
    })
    @PostMapping
    public ResponseEntity<?> reportar(@RequestBody IncidenteDTO dto) {
        String currentUser = securityUtils.getCurrentUsername();
        if (currentUser != null && !currentUser.isBlank()) {
            if (!securityUtils.esGestorOAdmin()) {
                // Para ROLE_USER: Forzar estrictamente la identidad del usuario autenticado (evita suplantación)
                dto.setReportadoPor(currentUser);
                dto.setUsuarioId(currentUser);
            } else {
                // Para gestores o administradores: Si registran en representación de un tercero, conservar la trazabilidad del actor real
                String reportanteSolicitado = dto.getReportadoPor();
                if (reportanteSolicitado != null && !reportanteSolicitado.isBlank()
                        && !reportanteSolicitado.equalsIgnoreCase(currentUser)) {
                    String valorConTrazabilidad = reportanteSolicitado.trim() + " (registrado por " + currentUser + ")";
                    dto.setReportadoPor(valorConTrazabilidad);
                    dto.setUsuarioId(valorConTrazabilidad);
                } else {
                    dto.setReportadoPor(currentUser);
                    dto.setUsuarioId(currentUser);
                }
            }
        }
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

    private Incidente sanitizarParaUsuario(Incidente original, String currentUsername) {
        if (original == null) return null;
        boolean esPropio = esReportantePropio(original, currentUsername);

        // Si es propio, el usuario visualiza su propia identidad; si es ajeno, se anonimiza como null
        String reportanteProtegido = esPropio ? original.getReportadoPor() : null;

        Incidente copia = new Incidente();
        copia.setIdIncidente(original.getIdIncidente());
        copia.setRecurso(original.getRecurso());
        copia.setTipo(original.getTipo());
        copia.setSeveridad(original.getSeveridad());
        copia.setDescripcion(original.getDescripcion());
        copia.setFecha(original.getFecha());
        copia.setReportadoPor(reportanteProtegido);
        return copia;
    }

    private boolean esReportantePropio(Incidente inc, String currentUsername) {
        if (inc == null || inc.getReportadoPor() == null || currentUsername == null) return false;
        String rep = inc.getReportadoPor().trim();
        if (rep.equalsIgnoreCase(currentUsername.trim())) return true;
        Usuario u = securityUtils.getCurrentUsuario().orElse(null);
        if (u != null) {
            if (rep.equalsIgnoreCase(u.getCodigo())) return true;
            if (rep.equalsIgnoreCase(u.getNombre())) return true;
            if (u.getDocumento() != null && rep.equalsIgnoreCase(u.getDocumento())) return true;
            if (u.getEmail() != null && rep.equalsIgnoreCase(u.getEmail())) return true;
        }
        return false;
    }
}