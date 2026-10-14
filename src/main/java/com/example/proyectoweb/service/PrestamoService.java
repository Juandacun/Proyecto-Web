package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.PrestamoDevolucionDTO;
import com.example.proyectoweb.dto.PrestamoEntregaDTO;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PrestamoService {

    @Autowired
    private PrestamoRepository repository;

    @Autowired
    private RecursoService recursoService;

    @Transactional(readOnly = true)
    public List<Prestamo> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarActivos() {
        return repository.findActivos();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarTardios() {
        return repository.findAll().stream()
                .filter(Prestamo::esTardio)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarFiltrado(Long recursoId, String usuarioId, Boolean activo) {
        return repository.findAll().stream()
                .filter(p -> recursoId == null || p.getRecurso().getIdRecurso().equals(recursoId))
                .filter(p -> usuarioId == null || usuarioId.isBlank()
                        || (p.getDocumento() != null && p.getDocumento().equalsIgnoreCase(usuarioId.trim()))
                        || (p.getUsuarioResponsable() != null && p.getUsuarioResponsable().equalsIgnoreCase(usuarioId.trim())))
                .filter(p -> activo == null
                        || (activo && !"Devuelto".equalsIgnoreCase(p.getEstado()))
                        || (!activo && "Devuelto".equalsIgnoreCase(p.getEstado())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Prestamo buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Prestamo> buscarPorIdOptional(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id);
    }

    // REGISTRAR ENTREGA (RN-02 y RN-03)
    public Prestamo registrarEntrega(PrestamoEntregaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de entrega del préstamo no pueden ser nulos.");
        }
        if (dto.getRecursoId() == null) {
            throw new IllegalArgumentException("El ID del recurso es obligatorio.");
        }
        if (dto.getUsuarioResponsable() == null || dto.getUsuarioResponsable().isBlank()) {
            throw new IllegalArgumentException("El usuario responsable (documento o ID) es obligatorio.");
        }
        if (dto.getFechaLimite() == null) {
            throw new IllegalArgumentException("La fecha límite de devolución es obligatoria.");
        }

        LocalDate fechaEntrega = dto.getFechaEntrega() != null ? dto.getFechaEntrega() : LocalDate.now();
        if (dto.getFechaLimite().isBefore(fechaEntrega)) {
            throw new IllegalArgumentException("La fecha límite de devolución no puede ser anterior a la fecha de entrega.");
        }

        Recurso recurso = recursoService.buscarPorId(dto.getRecursoId());
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso con ID " + dto.getRecursoId() + " no existe.");
        }

        // Validación de tipo de recurso: solo EQUIPO
        if (recurso.getClase() != Recurso.Clase.EQUIPO) {
            throw new IllegalArgumentException("Solo se pueden prestar recursos de tipo EQUIPO. Los espacios y salas se apartan mediante reserva.");
        }

        // RN-02: Recursos bloqueados por daño/mantenimiento no se pueden prestar
        if (recurso.estaBloqueado()) {
            throw new IllegalStateException("El equipo '" + recurso.getNombre() + "' se encuentra bloqueado por daño o mantenimiento y no puede prestarse (RN-02).");
        }

        // RN-03: No prestar un equipo si ya figura en estado prestado
        boolean tienePrestamoActivo = !repository.findActivosPorRecurso(recurso.getIdRecurso()).isEmpty();
        if (recurso.estaPrestado() || tienePrestamoActivo) {
            throw new IllegalStateException("El equipo '" + recurso.getNombre() + "' ya figura en estado Prestado y no puede entregarse nuevamente (RN-03).");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setRecurso(recurso);
        prestamo.setDocumento(dto.getUsuarioResponsable().trim());
        prestamo.setUsuarioResponsable(dto.getUsuarioResponsable().trim());
        prestamo.setFechaEntrega(fechaEntrega);
        prestamo.setFechaDevolucionEstimada(dto.getFechaLimite());
        prestamo.setEstadoEquipo(dto.getEstadoInicial());
        prestamo.setObservaciones(dto.getObservaciones());
        prestamo.setAccesorios(dto.getAccesorios());
        prestamo.setEstado("Prestado");
        prestamo.setPuntualidad("En curso");

        Prestamo guardado = repository.save(prestamo);

        // Ciclo de vida: actualizar recurso a 'Prestado'
        recurso.setEstado("Prestado");
        recursoService.actualizar(recurso);

        return guardado;
    }

    // REGISTRAR DEVOLUCIÓN (Control de puntualidad y liberación de recurso)
    public Prestamo registrarDevolucion(Long id, PrestamoDevolucionDTO dto) {
        Prestamo prestamo = buscarPorId(id);
        if (prestamo == null) {
            return null;
        }

        if ("Devuelto".equalsIgnoreCase(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo con ID " + id + " ya figura como devuelto.");
        }

        LocalDate fechaDev = (dto != null && dto.getFechaDevolucion() != null)
                ? dto.getFechaDevolucion()
                : LocalDate.now();

        // Control de puntualidad
        String puntualidad = fechaDev.isAfter(prestamo.getFechaDevolucionEstimada())
                ? "Tardía"
                : "Puntual";

        prestamo.setFechaDevolucion(fechaDev);
        prestamo.setEstado("Devuelto");
        prestamo.setPuntualidad(puntualidad);

        if (dto != null) {
            if (dto.getEstadoFinal() != null && !dto.getEstadoFinal().isBlank()) {
                prestamo.setEstadoEquipo(dto.getEstadoFinal());
            }
            if (dto.getObservaciones() != null && !dto.getObservaciones().isBlank()) {
                String prevObs = prestamo.getObservaciones() != null ? prestamo.getObservaciones() + " | " : "";
                prestamo.setObservaciones(prevObs + "Devolución: " + dto.getObservaciones());
            }
        }

        Prestamo actualizado = repository.save(prestamo);

        // Ciclo de vida: al registrar la devolución, liberar el recurso a 'Disponible'
        Recurso recurso = prestamo.getRecurso();
        if (recurso != null) {
            recurso.setEstado("Disponible");
            recursoService.actualizar(recurso);
        }

        return actualizado;
    }

    // Métodos para compatibilidad previa
    public Prestamo guardar(Prestamo prestamo) {
        if (prestamo == null) {
            throw new IllegalArgumentException("El préstamo no puede ser nulo.");
        }
        PrestamoEntregaDTO dto = new PrestamoEntregaDTO(
                prestamo.getRecurso() != null ? prestamo.getRecurso().getIdRecurso() : null,
                prestamo.getDocumento(),
                prestamo.getFechaDevolucionEstimada(),
                prestamo.getEstadoEquipo(),
                prestamo.getObservaciones(),
                prestamo.getAccesorios()
        );
        dto.setFechaEntrega(prestamo.getFechaEntrega());
        return registrarEntrega(dto);
    }

    public Prestamo actualizar(Prestamo prestamo) {
        return repository.save(prestamo);
    }
}