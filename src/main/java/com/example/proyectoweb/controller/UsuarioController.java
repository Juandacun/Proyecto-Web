package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/usuarios", "/usuarios"})
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Usuarios", description = "API REST para administración de usuarios institucionales y asignación de roles (ROLE_ADMIN)")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // GET /api/usuarios/me - Obtener información del usuario autenticado actual
    @Operation(summary = "Perfil del usuario autenticado", description = "Devuelve los datos del usuario actualmente autenticado según su token JWT.")
    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado"));
        }
        return usuarioService.buscarPorLogin(auth.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // GET /api/usuarios - Listar todos los usuarios (solo ADMIN)
    @Operation(summary = "Listar usuarios", description = "Obtiene la lista de usuarios registrados en el sistema universitario (exclusivo ROLE_ADMIN).")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    // GET /api/usuarios/{id} - Buscar usuario por ID (solo ADMIN)
    @Operation(summary = "Buscar usuario por ID", description = "Retorna el detalle de un usuario específico (exclusivo ROLE_ADMIN).")
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(usuario);
    }

    // POST /api/usuarios - Crear nuevo usuario (solo ADMIN)
    @Operation(summary = "Crear nuevo usuario", description = "Registra un nuevo usuario en el sistema con su rol correspondiente (exclusivo ROLE_ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos")
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio."));
        }
        if (usuario.getCodigo() == null || usuario.getCodigo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El código es obligatorio."));
        }
        if (usuario.getTipo() == null || usuario.getTipo().isBlank()) {
            usuario.setTipo("Estudiante");
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            usuario.setRol("ROLE_USER");
        }

        Usuario guardado = usuarioService.guardar(usuario);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(guardado.getIdUsuario())
                .toUri();
        return ResponseEntity.created(location).body(guardado);
    }

    // PUT /api/usuarios/{id} - Actualizar usuario existente (solo ADMIN)
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario (exclusivo ROLE_ADMIN).")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Usuario datos) {
        Usuario actualizado = usuarioService.actualizar(id, datos);
        if (actualizado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario con ID " + id + " no encontrado."));
        }
        return ResponseEntity.ok(actualizado);
    }

    // PATCH /api/usuarios/{id}/rol - Modificar rol de usuario (solo ADMIN)
    @Operation(summary = "Modificar rol de usuario", description = "Asigna o modifica el rol de un usuario: ROLE_USER, ROLE_GESTOR, ROLE_ADMIN (exclusivo ROLE_ADMIN).")
    @PatchMapping("/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuevoRol = body != null ? body.get("rol") : null;
        if (nuevoRol == null || nuevoRol.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'rol' es obligatorio."));
        }
        Usuario actualizado = usuarioService.cambiarRol(id, nuevoRol);
        if (actualizado == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario con ID " + id + " no encontrado."));
        }
        return ResponseEntity.ok(actualizado);
    }

    // DELETE /api/usuarios/{id} - Eliminar usuario (solo ADMIN)
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema (exclusivo ROLE_ADMIN).")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean eliminado = usuarioService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario con ID " + id + " no encontrado."));
        }
        return ResponseEntity.noContent().build();
    }
}
