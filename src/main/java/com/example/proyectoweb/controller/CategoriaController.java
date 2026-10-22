package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.service.CategoriaService;
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
@RequestMapping({"/api/categorias", "/categorias"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Categorías", description = "API REST para gestión y consulta de categorías de recursos")
public class CategoriaController {

    @Autowired
    private CategoriaService service;

    // GET /api/categorias - Listar todas las categorías
    @Operation(summary = "Listar categorías", description = "Obtiene la lista completa de categorías del catálogo.")
    @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // GET /api/categorias/{id} - Detalle de categoría
    @Operation(summary = "Buscar categoría por ID", description = "Retorna el detalle de una categoría específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarPorId(@PathVariable Long id) {
        Categoria categoria = service.buscarPorId(id);
        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(categoria);
    }

    // POST /api/categorias - Crear nueva categoría
    @Operation(summary = "Crear nueva categoría", description = "Registra una nueva categoría en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de categoría inválidos")
    })
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Categoria categoria) {
        try {
            Categoria guardada = service.guardar(categoria);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(guardada.getIdCategoria())
                    .toUri();
            return ResponseEntity.created(location).body(guardada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/categorias/{id} - Actualizar categoría existente
    @Operation(summary = "Actualizar categoría", description = "Actualiza el nombre y descripción de una categoría existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos de categoría inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Categoria categoria) {
        try {
            Categoria actualizada = service.actualizar(id, categoria);
            if (actualizada == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "La categoría con ID " + id + " no existe."));
            }
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/categorias/{id} - Eliminar categoría
    @Operation(summary = "Eliminar categoría", description = "Elimina una categoría por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean eliminada = service.eliminar(id);
        if (!eliminada) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "La categoría con ID " + id + " no existe."));
        }
        return ResponseEntity.noContent().build();
    }
}