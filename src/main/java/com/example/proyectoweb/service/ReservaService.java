package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.ReservaDTO;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaService {
    public static final String CONFIRMADA = "Confirmada";
    public static final String FINALIZADA = "Finalizada";
    public static final String CANCELADA = "Cancelada";

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private RecursoService recursoService;

    @Transactional(readOnly = true)
    public List<Reserva> listar() {
        return ordenar(repository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorRecurso(Long idRecurso) {
        if (idRecurso == null) return listar();
        return ordenar(repository.findByRecursoIdRecurso(idRecurso));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorFecha(LocalDate fecha) {
        if (fecha == null) return listar();
        return ordenar(repository.findByFecha(fecha));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorEstado(String estado) {
        if (estado == null || estado.isBlank()) return listar();
        return ordenar(repository.findByEstadoIgnoreCase(estado.trim()));
    }

    @Transactional(readOnly = true)
    public List<Reserva> filtrar(Long recursoId, LocalDate fecha, String estado) {
        return listar().stream()
                .filter(r -> recursoId == null || r.getRecurso().getIdRecurso().equals(recursoId))
                .filter(r -> fecha == null || fecha.equals(r.getFecha()))
                .filter(r -> estado == null || estado.isBlank() || estado.trim().equalsIgnoreCase(r.getEstado()))
                .toList();
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
        return buscarConflicto(idReservaAExcluir, recursoId, fecha, horaInicio, horaFin).isPresent();
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

    @Transactional(readOnly = true)
    public Optional<Reserva> buscarConflicto(Long idReservaAExcluir, Long recursoId, LocalDate fecha,
                                             String horaInicio, String horaFin) {
        if (recursoId == null || fecha == null || horaInicio == null || horaFin == null) {
            return Optional.empty();
        }
        String inicio = normalizarHora(horaInicio);
        String fin = normalizarHora(horaFin);
        return repository.findActivasPorRecursoYFecha(recursoId, fecha).stream()
                .filter(r -> idReservaAExcluir == null || !r.getIdReserva().equals(idReservaAExcluir))
                .filter(r -> inicio.compareTo(normalizarHora(r.getHoraFin())) < 0
                        && normalizarHora(r.getHoraInicio()).compareTo(fin) < 0)
                .findFirst();
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
        String horaInicio = normalizarHora(dto.getHoraInicio());
        String horaFin = normalizarHora(dto.getHoraFin());
        validarIntervalo(dto.getFecha(), horaInicio, horaFin);

        Recurso recurso = validarRecursoReservable(dto.getRecursoId());

        validarSinConflicto(null, recurso, dto.getFecha(), horaInicio, horaFin);

        Reserva reserva = new Reserva();
        reserva.setRecurso(recurso);
        reserva.setFecha(dto.getFecha());
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFin(horaFin);
        reserva.setMotivo(limpiar(dto.getMotivo()));
        reserva.setEstado(CONFIRMADA);
        String solicitante = limpiar(dto.getSolicitante());
        reserva.setSolicitante(solicitante != null ? solicitante : "Invitado");

        Reserva guardada = repository.save(reserva);
        // Actualizar estado del recurso si estaba Disponible
        marcarRecursoReservado(recurso);
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
        return crearDesdeDTO(dto);
    }

    // UPDATE desde DTO con RN-01 y RN-02
    public Reserva actualizar(Long id, ReservaDTO dto) {
        Reserva existente = buscarPorId(id);
        if (existente == null) {
            return null;
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la reserva no pueden ser nulos.");
        }
        validarEditable(existente, "modificar");

        Long recursoId = dto.getRecursoId() != null ? dto.getRecursoId() : existente.getRecurso().getIdRecurso();
        LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : existente.getFecha();
        String horaInicio = normalizarHora(dto.getHoraInicio() != null && !dto.getHoraInicio().isBlank()
                ? dto.getHoraInicio() : existente.getHoraInicio());
        String horaFin = normalizarHora(dto.getHoraFin() != null && !dto.getHoraFin().isBlank()
                ? dto.getHoraFin() : existente.getHoraFin());
        validarIntervalo(fecha, horaInicio, horaFin);

        Long recursoAnteriorId = existente.getRecurso().getIdRecurso();
        // RN-02: si cambia a otro recurso o este está bloqueado
        Recurso recurso = validarRecursoReservable(recursoId);

        // RN-01: verificar solapamiento excluyendo esta reserva
        validarSinConflicto(id, recurso, fecha, horaInicio, horaFin);

        existente.setRecurso(recurso);
        existente.setFecha(fecha);
        existente.setHoraInicio(horaInicio);
        existente.setHoraFin(horaFin);
        if (dto.getMotivo() != null) existente.setMotivo(limpiar(dto.getMotivo()));
        if (dto.getSolicitante() != null && !dto.getSolicitante().isBlank()) {
            existente.setSolicitante(dto.getSolicitante().trim());
        }

        Reserva actualizada = repository.save(existente);

        if (!recursoAnteriorId.equals(recursoId)) {
            liberarRecursoSiSinOtrasReservas(recursoAnteriorId);
            marcarRecursoReservado(recurso);
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
        validarEditable(reserva, "cancelar");
        reserva.setEstado(CANCELADA);
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
        if (!CONFIRMADA.equalsIgnoreCase(reserva.getEstado())) {
            throw new IllegalStateException("Solo se puede finalizar una reserva Confirmada (estado actual: "
                    + reserva.getEstado() + ").");
        }
        reserva.setEstado(FINALIZADA);
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

    public static boolean esEditable(Reserva r) {
        return r != null && CONFIRMADA.equalsIgnoreCase(r.getEstado()) && !haIniciado(r);
    }

    public static boolean haIniciado(Reserva r) {
        if (r == null || r.getFecha() == null || r.getHoraInicio() == null) return false;
        LocalDateTime inicio = LocalDateTime.of(r.getFecha(), LocalTime.parse(normalizarHora(r.getHoraInicio())));
        return !inicio.isAfter(LocalDateTime.now());
    }

    public static String normalizarHora(String hora) {
        if (hora == null || hora.isBlank()) {
            throw new IllegalArgumentException("La hora es obligatoria.");
        }
        String h = hora.trim();
        if (h.matches("\\d:\\d{2}.*")) {
            h = "0" + h;
        }
        try {
            LocalTime t = LocalTime.parse(h.length() > 5 ? h.substring(0, 5) : h);
            return String.format("%02d:%02d", t.getHour(), t.getMinute());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de hora inválido: '" + hora + "'. Use HH:mm.");
        }
    }

    private void validarIntervalo(LocalDate fecha, String horaInicio, String horaFin) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la reserva es obligatoria.");
        }
        if (horaInicio.compareTo(horaFin) >= 0) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        LocalDateTime inicio = LocalDateTime.of(fecha, LocalTime.parse(horaInicio));
        if (!inicio.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede reservar en una fecha u hora que ya pasó.");
        }
    }

    private Recurso validarRecursoReservable(Long recursoId) {
        Recurso recurso = recursoService.buscarPorId(recursoId);
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso con ID " + recursoId + " no existe.");
        }
        if (recurso.getClase() != Recurso.Clase.ESPACIO) {
            throw new IllegalArgumentException("Solo se pueden reservar espacios y salas. Los equipos se gestionan mediante préstamos.");
        }
        // RN-02: Recursos bloqueados por daño/mantenimiento no se pueden reservar ni prestar
        if (recurso.estaBloqueado()) {
            throw new IllegalStateException("El recurso '" + recurso.getNombre()
                    + "' se encuentra bloqueado por daño o mantenimiento y no puede ser reservado (RN-02).");
        }
        if (recurso.estaPrestado()) {
            throw new IllegalStateException("El recurso '" + recurso.getNombre()
                    + "' figura en préstamo y no puede ser reservado (RN-02).");
        }
        return recurso;
    }

    private void validarSinConflicto(Long idExcluir, Recurso recurso, LocalDate fecha, String horaInicio, String horaFin) {
        // RN-01: No permitir solapamiento de reservas en el mismo recurso y horario
        buscarConflicto(idExcluir, recurso.getIdRecurso(), fecha, horaInicio, horaFin).ifPresent(otra -> {
            throw new IllegalStateException("Conflicto de horario: el recurso '" + recurso.getNombre()
                    + "' no está disponible el " + fecha + " entre las " + horaInicio + " y las " + horaFin
                    + ". Ya existe una reserva de " + otra.getHoraInicio() + " a " + otra.getHoraFin() + " (RN-01).");
        });
    }

    private void validarEditable(Reserva reserva, String accion) {
        if (!CONFIRMADA.equalsIgnoreCase(reserva.getEstado())) {
            throw new IllegalStateException("No se puede " + accion + " una reserva en estado "
                    + reserva.getEstado() + ".");
        }
        if (haIniciado(reserva)) {
            throw new IllegalStateException("No se puede " + accion
                    + " la reserva porque ya inició. Solo se permite antes de su hora de inicio.");
        }
    }

    private void marcarRecursoReservado(Recurso recurso) {
        if (recurso != null && "Disponible".equalsIgnoreCase(recurso.getEstado())) {
            recurso.setEstado("Reservado");
            recursoService.actualizar(recurso);
        }
    }

    private void liberarRecursoSiSinOtrasReservas(Long recursoId) {
        if (recursoId == null) return;
        boolean tieneActivas = repository.findByRecursoIdRecurso(recursoId).stream()
                .anyMatch(r -> CONFIRMADA.equalsIgnoreCase(r.getEstado()));
        if (!tieneActivas) {
            Recurso recurso = recursoService.buscarPorId(recursoId);
            if (recurso != null && "Reservado".equalsIgnoreCase(recurso.getEstado())) {
                recurso.setEstado("Disponible");
                recursoService.actualizar(recurso);
            }
        }
    }

    private static String limpiar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    private static List<Reserva> ordenar(List<Reserva> reservas) {
        return reservas.stream()
                .sorted(Comparator.comparing(Reserva::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Reserva::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
