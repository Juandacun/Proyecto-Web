package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/usuarios", "/usuarios"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Usuarios", description = "API REST para consulta de usuarios institucionales de campus")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Operation(summary = "Listar usuarios", description = "Obtiene la lista de usuarios registrados en el sistema universitario.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @Operation(summary = "Buscar usuario por ID", description = "Retorna el detalle de un usuario específico.")
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(usuario);
    }
}
