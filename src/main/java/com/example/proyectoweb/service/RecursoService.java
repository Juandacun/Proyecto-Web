package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.repository.RecursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecursoService {

    @Autowired
    private RecursoRepository repository;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    // READ - listar todos
    @Transactional(readOnly = true)
    public List<Recurso> listar() {
        return repository.findAll();
    }

    // READ - listar solo equipos (los usan los prestamos)
    @Transactional(readOnly = true)
    public List<Recurso> listarEquipos() {
        return repository.findByClase(Recurso.Clase.EQUIPO);
    }

    // READ - listar solo espacios y salas (usan las reservas)
    @Transactional(readOnly = true)
    public List<Recurso> listarEspacios() {
        return repository.findByClase(Recurso.Clase.ESPACIO);
    }

    // READ - listar por clase (EQUIPO o ESPACIO)
    @Transactional(readOnly = true)
    public List<Recurso> listarPorClase(Recurso.Clase clase) {
        if (clase == null) {
            return listar();
        }
        return repository.findByClase(clase);
    }

    // READ - listar por estado
    @Transactional(readOnly = true)
    public List<Recurso> listarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return listar();
        }
        return repository.findByEstadoIgnoreCase(estado.trim());
    }

    // READ - listar disponibles
    @Transactional(readOnly = true)
    public List<Recurso> listarDisponibles() {
        return repository.findByEstadoIgnoreCase("Disponible");
    }

    // READ - listar equipos disponibles (RN-02, RN-03)
    @Transactional(readOnly = true)
    public List<Recurso> listarEquiposDisponibles() {
        return repository.findByClaseAndEstadoIgnoreCase(Recurso.Clase.EQUIPO, "Disponible");
    }

    // READ - listar espacios disponibles (RN-01, RN-02)
    @Transactional(readOnly = true)
    public List<Recurso> listarEspaciosDisponibles() {
        return repository.findByClaseAndEstadoIgnoreCase(Recurso.Clase.ESPACIO, "Disponible");
    }

    // READ - filtros combinables del catálogo (clase, estado, disponibilidad, categoría, ubicación y tipo)
    @Transactional(readOnly = true)
    public List<Recurso> filtrar(Recurso.Clase clase, String estado, Boolean disponible,
                                 Long categoriaId, Long ubicacionId, String tipo) {
        String estadoBuscado = Boolean.TRUE.equals(disponible) ? "Disponible"
                : (estado != null && !estado.isBlank() ? estado.trim() : null);
        String tipoBuscado = tipo != null && !tipo.isBlank() ? tipo.trim() : null;

        return repository.findAll().stream()
                .filter(r -> clase == null || r.getClase() == clase)
                .filter(r -> estadoBuscado == null || estadoBuscado.equalsIgnoreCase(r.getEstado()))
                .filter(r -> categoriaId == null
                        || (r.getCategoria() != null && categoriaId.equals(r.getCategoria().getIdCategoria())))
                .filter(r -> ubicacionId == null
                        || (r.getUbicacion() != null && ubicacionId.equals(r.getUbicacion().getIdUbicacion())))
                .filter(r -> tipoBuscado == null || tipoBuscado.equalsIgnoreCase(r.getTipo()))
                .toList();
    }

    // READ - buscar uno por ID
    @Transactional(readOnly = true)
    public Recurso buscarPorId(Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id).orElse(null);
    }

    // READ - buscar uno por ID (Optional)
    @Transactional(readOnly = true)
    public Optional<Recurso> buscarPorIdOptional(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    // CREATE - guardar nuevo recurso con validaciones y valores por defecto
    public Recurso guardar(Recurso recurso) {
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso no puede ser nulo.");
        }

        // Valores por defecto
        if (recurso.getEstado() == null || recurso.getEstado().isBlank()) {
            recurso.setEstado("Disponible");
        }
        if (recurso.getClase() == null) {
            recurso.setClase(Recurso.Clase.ESPACIO);
        }

        // Resolver y validar categoría
        if (recurso.getCategoria() != null && recurso.getCategoria().getIdCategoria() != null) {
            Categoria cat = categoriaService.buscarPorId(recurso.getCategoria().getIdCategoria());
            if (cat == null) {
                throw new IllegalArgumentException("La categoría con ID " + recurso.getCategoria().getIdCategoria() + " no existe.");
            }
            recurso.setCategoria(cat);
        } else {
            throw new IllegalArgumentException("La categoría es obligatoria para el recurso.");
        }

        // Resolver y validar ubicación
        if (recurso.getUbicacion() != null && recurso.getUbicacion().getIdUbicacion() != null) {
            Ubicacion ubi = ubicacionService.buscarPorId(recurso.getUbicacion().getIdUbicacion());
            if (ubi == null) {
                throw new IllegalArgumentException("La ubicación con ID " + recurso.getUbicacion().getIdUbicacion() + " no existe.");
            }
            recurso.setUbicacion(ubi);
        } else {
            throw new IllegalArgumentException("La ubicación es obligatoria para el recurso.");
        }

        return repository.save(recurso);
    }

    // UPDATE - actualizar recurso existente por ID
    public Recurso actualizar(Long id, Recurso recursoActualizado) {
        Recurso existente = buscarPorId(id);
        if (existente == null) {
            return null;
        }

        if (recursoActualizado.getNombre() != null && !recursoActualizado.getNombre().isBlank()) {
            existente.setNombre(recursoActualizado.getNombre());
        }
        if (recursoActualizado.getTipo() != null && !recursoActualizado.getTipo().isBlank()) {
            existente.setTipo(recursoActualizado.getTipo());
        }
        existente.setDescripcion(recursoActualizado.getDescripcion());
        existente.setCaracteristicas(recursoActualizado.getCaracteristicas());

        if (recursoActualizado.getEstado() != null && !recursoActualizado.getEstado().isBlank()) {
            existente.setEstado(recursoActualizado.getEstado());
        }
        if (recursoActualizado.getClase() != null) {
            existente.setClase(recursoActualizado.getClase());
        }

        // Actualizar categoría si viene especificada
        if (recursoActualizado.getCategoria() != null && recursoActualizado.getCategoria().getIdCategoria() != null) {
            Categoria cat = categoriaService.buscarPorId(recursoActualizado.getCategoria().getIdCategoria());
            if (cat == null) {
                throw new IllegalArgumentException("La categoría con ID " + recursoActualizado.getCategoria().getIdCategoria() + " no existe.");
            }
            existente.setCategoria(cat);
        }

        // Actualizar ubicación si viene especificada
        if (recursoActualizado.getUbicacion() != null && recursoActualizado.getUbicacion().getIdUbicacion() != null) {
            Ubicacion ubi = ubicacionService.buscarPorId(recursoActualizado.getUbicacion().getIdUbicacion());
            if (ubi == null) {
                throw new IllegalArgumentException("La ubicación con ID " + recursoActualizado.getUbicacion().getIdUbicacion() + " no existe.");
            }
            existente.setUbicacion(ubi);
        }

        return repository.save(existente);
    }

    // UPDATE - sobrecarga para compatibilidad con llamadas existentes
    public Recurso actualizar(Recurso recurso) {
        if (recurso == null || recurso.getIdRecurso() == null) {
            return guardar(recurso);
        }
        Recurso existente = buscarPorId(recurso.getIdRecurso());
        if (existente == null) {
            return guardar(recurso);
        }
        return repository.save(recurso);
    }

    // PATCH / UPDATE STATE - cambiar solo el estado (clave para RN-02, RN-03, RN-04)
    public Recurso cambiarEstado(Long id, String nuevoEstado) {
        Recurso recurso = buscarPorId(id);
        if (recurso == null) {
            return null;
        }
        recurso.setEstado(nuevoEstado);
        return repository.save(recurso);
    }

    // DELETE - eliminar por ID
    public boolean eliminar(Long id) {
        if (id != null && repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    // REGLAS DE NEGOCIO HELPERS
    // RN-02: Recursos bloqueados por daño/mantenimiento no se pueden reservar ni prestar
    @Transactional(readOnly = true)
    public boolean estaBloqueado(Long id) {
        Recurso r = buscarPorId(id);
        return r != null && r.estaBloqueado();
    }

    // RN-02 & RN-03: Verifica disponibilidad
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long id) {
        Recurso r = buscarPorId(id);
        return r != null && r.estaDisponible();
    }

    // RN-03: No prestar un equipo si ya figura en estado prestado
    @Transactional(readOnly = true)
    public boolean estaPrestado(Long id) {
        Recurso r = buscarPorId(id);
        return r != null && r.estaPrestado();
    }
}