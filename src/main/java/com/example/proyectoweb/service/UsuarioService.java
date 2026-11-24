package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void inicializarUsuariosSiEstaVacio() {
        // Política de inclusión explícita:
        // Permitir creación de usuarios de prueba ÚNICAMENTE cuando esté activo "dev" o "test".
        // Si no existe un perfil explícito (default), o si es prod/production, NO crear usuarios de prueba.
        if (environment == null) {
            return;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            return;
        }
        boolean esDevOTest = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));
        if (!esDevOTest) {
            return;
        }

        // Carga idempotente y segura de usuarios de prueba para desarrollo y testing
        crearOActualizarUsuario("user", "user123", "Juan Pérez", "Estudiante",
                "EST-202410", "1098765432", "juan.perez@universidad.edu.co", "ROLE_USER");

        crearOActualizarUsuario("gestor", "gestor123", "Carlos Rodríguez", "Investigador",
                "INV-309112", "79845612", "carlos.rodriguez@universidad.edu.co", "ROLE_GESTOR");

        crearOActualizarUsuario("admin", "admin123", "Ing. Ana Torres", "Administrativo",
                "ADM-401928", "63901234", "ana.torres@universidad.edu.co", "ROLE_ADMIN");

        // Otros usuarios institucionales para compatibilidad con datos iniciales
        crearOActualizarUsuario("docente", "docente123", "Dra. María Gómez", "Docente",
                "DOC-108234", "52891234", "maria.gomez@universidad.edu.co", "ROLE_USER");

        crearOActualizarUsuario("laura", "user123", "Laura Martínez", "Estudiante",
                "EST-202411", "104109003", "laura.martinez@universidad.edu.co", "ROLE_USER");

        crearOActualizarUsuario("david", "user123", "David Morales", "Estudiante",
                "EST-202412", "1018902345", "david.morales@universidad.edu.co", "ROLE_USER");
    }

    private void crearOActualizarUsuario(String username, String rawPassword, String nombre,
                                         String tipo, String codigo, String documento,
                                         String email, String rol) {
        Optional<Usuario> existente = repository.findByLoginIdentifier(username)
                .or(() -> repository.findByCodigo(codigo))
                .or(() -> repository.findByDocumento(documento));

        if (existente.isPresent()) {
            Usuario u = existente.get();
            boolean modificado = false;
            boolean esUsuarioLegacySinSeguridad = (u.getPassword() == null || u.getPassword().isBlank());

            // 1. Asignar username institucional de acceso si no ha sido configurado
            if (u.getUsername() == null || u.getUsername().isBlank() || u.getUsername().equalsIgnoreCase(u.getCodigo())) {
                u.setUsername(username);
                modificado = true;
            }

            // 2. Asignación inicial de credenciales y rol para registros legacy
            if (esUsuarioLegacySinSeguridad) {
                u.setPassword(passwordEncoder.encode(rawPassword));

                // Asignación controlada: roles privilegiados solo para registros identificados
                if ("ROLE_ADMIN".equals(rol) && "ADM-401928".equals(u.getCodigo())) {
                    u.setRol("ROLE_ADMIN");
                } else if ("ROLE_GESTOR".equals(rol) && "INV-309112".equals(u.getCodigo())) {
                    u.setRol("ROLE_GESTOR");
                } else if (u.getRol() == null || u.getRol().isBlank()) {
                    u.setRol("ROLE_USER");
                }
                modificado = true;
            }

            if (modificado) {
                repository.save(u);
            }
        } else {
            String encodedPassword = passwordEncoder.encode(rawPassword);
            Usuario nuevo = new Usuario(nombre, tipo, codigo, documento, email, username, encodedPassword, rol);
            repository.save(nuevo);
        }
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        return repository.findByLoginIdentifier(login.trim());
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank() && !usuario.getPassword().startsWith("$2")) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            usuario.setUsername(usuario.getCodigo());
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            usuario.setRol("ROLE_USER");
        }
        return repository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Long id, Usuario datos) {
        Usuario existente = buscarPorId(id);
        if (existente == null) return null;

        if (datos.getNombre() != null) existente.setNombre(datos.getNombre());
        if (datos.getTipo() != null) existente.setTipo(datos.getTipo());
        if (datos.getCodigo() != null) existente.setCodigo(datos.getCodigo());
        if (datos.getDocumento() != null) existente.setDocumento(datos.getDocumento());
        if (datos.getEmail() != null) existente.setEmail(datos.getEmail());
        if (datos.getUsername() != null) existente.setUsername(datos.getUsername());
        if (datos.getRol() != null) existente.setRol(datos.getRol());

        if (datos.getPassword() != null && !datos.getPassword().isBlank() && !datos.getPassword().startsWith("$2")) {
            existente.setPassword(passwordEncoder.encode(datos.getPassword()));
        }

        return repository.save(existente);
    }

    @Transactional
    public Usuario cambiarRol(Long id, String nuevoRol) {
        Usuario existente = buscarPorId(id);
        if (existente == null) return null;
        existente.setRol(nuevoRol);
        return repository.save(existente);
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (id == null || !repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
