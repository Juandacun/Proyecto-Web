package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IndicadoresDTO;
import com.example.proyectoweb.service.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/indicadores")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Indicadores", description = "API REST del tablero operativo: estado de recursos, reservas por recurso/categoría, devoluciones tardías e incidentes")
public class IndicadorController {

    @Autowired
    private IndicadorService indicadorService;

    // GET /api/indicadores - Indicadores del tablero
    @Operation(summary = "Obtener indicadores del tablero", description = "Devuelve el número de recursos por estado, reservas por recurso y por categoría (sin contar canceladas), préstamos activos, devoluciones tardías e incidentes registrados.")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados exitosamente")
    @GetMapping
    public ResponseEntity<IndicadoresDTO> obtener() {
        return ResponseEntity.ok(indicadorService.calcular());
    }
}
