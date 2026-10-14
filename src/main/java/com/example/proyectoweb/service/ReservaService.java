package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.ReservaDTO;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaService {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private RecursoService recursoService;

    @Transactional(readOnly = true)
    public List<Reserva> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorRecurso(Long idRecurso) {
        if (idRecurso == null) return listar();
        return repository.findByRecursoIdRecurso(idRecurso);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorFecha(LocalDate fecha) {
        if (fecha == null) return listar();
        return repository.findByFecha(fecha);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) return listar();
        return repository.findByEstadoIgnoreCase(estado.trim());
    }

    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorIdOptional(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id);
    }

    // RN-01: Validación de solapamiento de reservas en el mismo recurso y horario
    @Transactional(readOnly = true)
    public boolean seSuperpone(Long idReservaAExcluir, Long recursoId, LocalDate fecha, String horaInicio, String horaFin) {
        if (recursoId == null || fecha == null || horaInicio == null || horaFin == null) {
            return false;
        }

        List<Reserva> activas = repository.findActivasPorRecursoYFecha(recursoId, fecha);

        return activas.stream().anyMatch(r ->
                (idReservaAExcluir == null || !r.getIdReserva().equals(idReservaAExcluir))
                        && horaInicio.compareTo(r.getHoraFin()) < 0
                        && horaFin.compareTo(r.getHoraInicio()) > 0
        );
    }

    // Sobrecarga de seSuperpone para compatibilidad
    @Transactional(readOnly = true)
    public boolean seSuperpone(Reserva reserva) {
        if (reserva == null || reserva.getRecurso() == null) {
            return false;
        }
        return seSuperpone(reserva.getIdReserva(), reserva.getRecurso().getIdRecurso(),
                reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
    }

    // CREATE desde DTO con aplicación rigurosa de RN-01 y RN-02
    public Reserva crearDesdeDTO(ReservaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la reserva no pueden ser nulos.");
        }
        if (dto.getRecursoId() == null) {
            throw new IllegalArgumentException("El ID del recurso es obligatorio.");
        }
        if (dto.getFecha() == null) {
            throw new IllegalArgumentException("La fecha de la reserva es obligatoria.");
        }
        if (dto.getHoraInicio() == null || dto.getHoraInicio().isBlank()
                || dto.getHoraFin() == null || dto.getHoraFin().isBlank()) {
            throw new IllegalArgumentException("Los horarios de inicio y fin son obligatorios.");
        }
        if (dto.getHoraInicio().compareTo(dto.getHoraFin()) >= 0) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // RN-02: Recursos bloqueados por daño/mantenimiento no se pueden reservar ni prestar
        Recurso recurso = recursoService.buscarPorId(dto.getRecursoId());
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso con ID " + dto.getRecursoId() + " no existe.");
        }
        if (recurso.getClase() != Recurso.Clase.ESPACIO) {
            throw new IllegalArgumentException("Solo se pueden reservar espacios y salas. Los equipos se gestionan mediante préstamos.");
        }
        if (recurso.estaBloqueado()) {
            throw new IllegalStateException("El recurso '" + recurso.getNombre() + "' se encuentra bloqueado por daño o mantenimiento y no puede ser reservado (RN-02).");
        }
        if (recurso.estaPrestado()) {
            throw new IllegalStateException("El recurso '" + recurso.getNombre() + "' figura en préstamo y no puede ser reservado (RN-02).");
        }

        // RN-01: No permitir solapamiento de reservas en el mismo recurso y horario
        if (seSuperpone(null, recurso.getIdRecurso(), dto.getFecha(), dto.getHoraInicio(), dto.getHoraFin())) {
            throw new IllegalStateException("Conflicto de horario: ya existe una reserva activa para el recurso '"
                    + recurso.getNombre() + "' en la fecha " + dto.getFecha() + " entre las "
                    + dto.getHoraInicio() + " y las " + dto.getHoraFin() + " (RN-01).");
        }

        Reserva reserva = new Reserva();
        reserva.setRecurso(recurso);
        reserva.setFecha(dto.getFecha());
        reserva.setHoraInicio(dto.getHoraInicio().trim());
        reserva.setHoraFin(dto.getHoraFin().trim());
        reserva.setMotivo(dto.getMotivo());
        reserva.setEstado(dto.getEstado() != null && !dto.getEstado().isBlank() ? dto.getEstado() : "Confirmada");
        reserva.setSolicitante(dto.getSolicitante() != null && !dto.getSolicitante().isBlank() ? dto.getSolicitante() : "Invitado");

        Reserva guardada = repository.save(reserva);

        // Actualizar estado del recurso si estaba Disponible
        if ("Disponible".equalsIgnoreCase(recurso.getEstado())) {
            recurso.setEstado("Reservado");
            recursoService.actualizar(recurso);
        }

        return guardada;
    }

    // CREATE directo (compatibilidad con entidad)
    public Reserva guardar(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva no puede ser nula.");
        }
        if (reserva.getRecurso() == null || reserva.getRecurso().getIdRecurso() == null) {
            throw new IllegalArgumentException("El recurso asociado es obligatorio.");
        }
        ReservaDTO dto = new ReservaDTO(reserva.getRecurso().getIdRecurso(),
                reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(),
                reserva.getMotivo(), reserva.getSolicitante());
        dto.setEstado(reserva.getEstado());
        return crearDesdeDTO(dto);
    }

    // UPDATE desde DTO con RN-01 y RN-02
    public Reserva actualizar(Long id, ReservaDTO dto) {
        Reserva existente = buscarPorId(id);
        if (existente == null) {
            return null;
        }

        Long recursoId = dto.getRecursoId() != null ? dto.getRecursoId() : existente.getRecurso().getIdRecurso();
        LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : existente.getFecha();
        String horaInicio = dto.getHoraInicio() != null && !dto.getHoraInicio().isBlank() ? dto.getHoraInicio().trim() : existente.getHoraInicio();
        String horaFin = dto.getHoraFin() != null && !dto.getHoraFin().isBlank() ? dto.getHoraFin().trim() : existente.getHoraFin();

        if (horaInicio.compareTo(horaFin) >= 0) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        Recurso recurso = recursoService.buscarPorId(recursoId);
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso con ID " + recursoId + " no existe.");
        }
        if (recurso.getClase() != Recurso.Clase.ESPACIO) {
            throw new IllegalArgumentException("Solo se pueden reservar espacios y salas.");
        }

        // RN-02: si cambia a otro recurso o este está bloqueado
        if (!existente.getRecurso().getIdRecurso().equals(recursoId) && recurso.estaBloqueado()) {
            throw new IllegalStateException("El nuevo recurso seleccionado está bloqueado y no se puede reservar (RN-02).");
        }

        // RN-01: verificar solapamiento excluyendo esta reserva
        if (seSuperpone(id, recursoId, fecha, horaInicio, horaFin)) {
            throw new IllegalStateException("Conflicto de horario: ya existe otra reserva activa para el recurso '"
                    + recurso.getNombre() + "' en la fecha " + fecha + " entre las "
                    + horaInicio + " y las " + horaFin + " (RN-01).");
        }

        Long recursoAnteriorId = existente.getRecurso().getIdRecurso();

        existente.setRecurso(recurso);
        existente.setFecha(fecha);
        existente.setHoraInicio(horaInicio);
        existente.setHoraFin(horaFin);
        if (dto.getMotivo() != null) existente.setMotivo(dto.getMotivo());
        if (dto.getSolicitante() != null && !dto.getSolicitante().isBlank()) existente.setSolicitante(dto.getSolicitante());
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) existente.setEstado(dto.getEstado());

        Reserva actualizada = repository.save(existente);

        if (!recursoAnteriorId.equals(recursoId)) {
            liberarRecursoSiSinOtrasReservas(recursoAnteriorId);
            if ("Disponible".equalsIgnoreCase(recurso.getEstado())) {
                recurso.setEstado("Reservado");
                recursoService.actualizar(recurso);
            }
        }

        return actualizada;
    }

    // UPDATE directo para compatibilidad
    public Reserva actualizar(Reserva reserva) {
        if (reserva == null || reserva.getIdReserva() == null) {
            return guardar(reserva);
        }
        return repository.save(reserva);
    }

    // CANCELAR reserva
    public Reserva cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva == null) {
            return null;
        }
        reserva.setEstado("Cancelada");
        Reserva guardada = repository.save(reserva);
        liberarRecursoSiSinOtrasReservas(reserva.getRecurso().getIdRecurso());
        return guardada;
    }

    // FINALIZAR reserva
    public Reserva finalizar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva == null) {
            return null;
        }
        reserva.setEstado("Finalizada");
        Reserva guardada = repository.save(reserva);
        liberarRecursoSiSinOtrasReservas(reserva.getRecurso().getIdRecurso());
        return guardada;
    }

    // ELIMINAR reserva
    public boolean eliminar(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva != null) {
            Long recursoId = reserva.getRecurso().getIdRecurso();
            repository.deleteById(id);
            liberarRecursoSiSinOtrasReservas(recursoId);
            return true;
        }
        return false;
    }

    private void liberarRecursoSiSinOtrasReservas(Long recursoId) {
        if (recursoId == null) return;
        boolean tieneActivas = repository.findByRecursoIdRecurso(recursoId).stream()
                .anyMatch(r -> !"Cancelada".equalsIgnoreCase(r.getEstado())
                        && !"Finalizada".equalsIgnoreCase(r.getEstado())
                        && !"Rechazada".equalsIgnoreCase(r.getEstado()));
        if (!tieneActivas) {
            Recurso recurso = recursoService.buscarPorId(recursoId);
            if (recurso != null && "Reservado".equalsIgnoreCase(recurso.getEstado())) {
                recurso.setEstado("Disponible");
                recursoService.actualizar(recurso);
            }
        }
    }
}