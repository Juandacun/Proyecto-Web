package com.example.proyectoweb.service;

import com.example.proyectoweb.dto.IndicadoresDTO;
import com.example.proyectoweb.dto.IndicadoresDTO.FilaCategoria;
import com.example.proyectoweb.dto.IndicadoresDTO.FilaRecurso;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.repository.IncidenteRepository;
import com.example.proyectoweb.repository.PrestamoRepository;
import com.example.proyectoweb.repository.RecursoRepository;
import com.example.proyectoweb.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private static final List<String> ESTADOS_TABLERO = List.of("Disponible", "Reservado", "Prestado", "Bloqueado");

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private IncidenteRepository incidenteRepository;

    public IndicadoresDTO calcular() {
        List<Recurso> recursos = recursoRepository.findAll();
        List<Reserva> reservas = reservaRepository.findAll();
        List<Prestamo> prestamos = prestamoRepository.findAll();
        List<Incidente> incidentes = incidenteRepository.findAll();

        // Las reservas canceladas no cuentan como uso del recurso
        Map<Long, Long> reservasPorId = reservas.stream()
                .filter(r -> !"Cancelada".equalsIgnoreCase(r.getEstado()))
                .collect(Collectors.groupingBy(r -> r.getRecurso().getIdRecurso(), Collectors.counting()));

        List<FilaRecurso> porRecurso = recursos.stream()
                .map(r -> new FilaRecurso(
                        r.getIdRecurso(),
                        r.getNombre(),
                        r.getCategoria() != null ? r.getCategoria().getNombre() : "Sin categoría",
                        r.getUbicacion() != null ? r.getUbicacion().getNombre() : "Sin ubicación",
                        r.getClase() != null ? r.getClase().name() : null,
                        r.getEstado(),
                        reservasPorId.getOrDefault(r.getIdRecurso(), 0L)))
                .sorted(Comparator.comparingLong(FilaRecurso::reservas).reversed()
                        .thenComparing(FilaRecurso::recurso))
                .toList();

        List<FilaCategoria> porCategoria = porRecurso.stream()
                .collect(Collectors.groupingBy(FilaRecurso::categoria, Collectors.summingLong(FilaRecurso::reservas)))
                .entrySet().stream()
                .map(e -> new FilaCategoria(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(FilaCategoria::reservas).reversed()
                        .thenComparing(FilaCategoria::categoria))
                .toList();

        Map<String, Long> porEstado = new LinkedHashMap<>();
        ESTADOS_TABLERO.forEach(e -> porEstado.put(e, 0L));
        recursos.forEach(r -> porEstado.merge(estadoTablero(r), 1L, Long::sum));

        long reservasActivas = reservas.stream()
                .filter(r -> "Confirmada".equalsIgnoreCase(r.getEstado()))
                .count();
        long prestamosActivos = prestamos.stream()
                .filter(p -> !"Devuelto".equalsIgnoreCase(p.getEstado()))
                .count();
        long tardias = prestamos.stream().filter(Prestamo::esTardio).count();
        long criticos = incidentes.stream()
                .filter(i -> "Crítica".equalsIgnoreCase(i.getSeveridad()))
                .count();

        return new IndicadoresDTO(recursos.size(), porEstado, reservasActivas, prestamosActivos,
                tardias, incidentes.size(), criticos, porRecurso, porCategoria);
    }

    // Agrupa los estados textuales en los cuatro del tablero (4.f.i)
    private String estadoTablero(Recurso r) {
        if (r.estaBloqueado()) return "Bloqueado";
        if (r.estaPrestado()) return "Prestado";
        if (r.estaDisponible()) return "Disponible";
        if (r.getEstado() != null && r.getEstado().toLowerCase().contains("reserv")) return "Reservado";
        return r.getEstado() != null ? r.getEstado() : "Sin estado";
    }
}
