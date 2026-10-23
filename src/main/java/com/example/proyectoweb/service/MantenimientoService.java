package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.MantenimientoDTO;
import com.example.proyectoweb.model.Mantenimiento;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.repository.MantenimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MantenimientoService {

    @Autowired
    private MantenimientoRepository repository;

    @Autowired
    private RecursoService recursoService;

    @Transactional(readOnly = true)
    public List<Mantenimiento> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mantenimiento> listarFiltrado(Long recursoId, String estado) {
        if (recursoId != null && estado != null && !estado.isBlank()) {
            return repository.findByRecursoIdRecursoAndEstadoIgnoreCase(recursoId, estado.trim());
        }
        if (recursoId != null) {
            return repository.findByRecursoIdRecurso(recursoId);
        }
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstadoIgnoreCase(estado.trim());
        }
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Mantenimiento buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Mantenimiento> buscarPorIdOptional(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id);
    }

    // CREAR ORDEN DE MANTENIMIENTO (Permite abrir sobre recursos bloqueados)
    public Mantenimiento crearOrden(MantenimientoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de mantenimiento no pueden ser nulos.");
        }
        if (dto.getRecursoId() == null) {
            throw new IllegalArgumentException("El ID del recurso para mantenimiento es obligatorio.");
        }

        Recurso recurso = recursoService.buscarPorId(dto.getRecursoId());
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso con ID " + dto.getRecursoId() + " no existe.");
        }

        String estadoInicial = dto.getEstado() != null && !dto.getEstado().isBlank()
                ? dto.getEstado().trim()
                : "Abierta";

        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setRecurso(recurso);
        mantenimiento.setFechaApertura(dto.getFechaApertura() != null ? dto.getFechaApertura() : LocalDateTime.now());
        mantenimiento.setDiagnostico(dto.getDiagnostico());
        mantenimiento.setEstado(estadoInicial);
        mantenimiento.setIdUsuarioResponsable(dto.getIdUsuarioResponsable());
        mantenimiento.setObservaciones(dto.getObservaciones());

        Mantenimiento guardado = repository.save(mantenimiento);

        // Asegurar que el recurso permanezca bloqueado durante el mantenimiento
        if (!recurso.estaBloqueado()) {
            recurso.setEstado("Bloqueado");
            recursoService.actualizar(recurso);
        }

        return guardado;
    }

    // RN-05: Al cambiar el estado de la intervención a Finalizada, actualizar automáticamente el recurso a 'Disponible'
    public Mantenimiento finalizar(Long id) {
        Mantenimiento mantenimiento = buscarPorId(id);
        if (mantenimiento == null) {
            return null;
        }

        mantenimiento.setEstado("Finalizada");
        mantenimiento.setFechaCierre(LocalDateTime.now());
        Mantenimiento guardado = repository.save(mantenimiento);

        // RN-05: Liberar el recurso a 'Disponible'
        Recurso recurso = mantenimiento.getRecurso();
        if (recurso != null) {
            recurso.setEstado("Disponible");
            recursoService.actualizar(recurso);
        }

        return guardado;
    }

    private static final List<String> ESTADOS = List.of("Abierta", "En proceso", "Finalizada", "Cancelada");

    // Cambiar estado de la intervención: Abierta -> En proceso -> Finalizada | Cancelada
    public Mantenimiento cambiarEstado(Long id, String nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio.");
        }
        String estado = ESTADOS.stream()
                .filter(e -> e.equalsIgnoreCase(nuevoEstado.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Estado no válido. Opciones: " + String.join(", ", ESTADOS) + "."));

        Mantenimiento mantenimiento = buscarPorId(id);
        if (mantenimiento == null) {
            return null;
        }
        if ("Finalizada".equalsIgnoreCase(mantenimiento.getEstado())
                || "Cancelada".equalsIgnoreCase(mantenimiento.getEstado())) {
            throw new IllegalStateException("La orden #" + id + " ya está " + mantenimiento.getEstado()
                    + " y no puede cambiar de estado.");
        }
        if ("Finalizada".equals(estado)) {
            return finalizar(id);
        }
        mantenimiento.setEstado(estado);
        if ("Cancelada".equals(estado)) {
            // El recurso sigue bloqueado: el gestor decide si lo habilita (RN-04)
            mantenimiento.setFechaCierre(LocalDateTime.now());
        }
        return repository.save(mantenimiento);
    }
}
