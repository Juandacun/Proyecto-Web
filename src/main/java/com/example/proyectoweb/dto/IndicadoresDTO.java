package com.example.proyectoweb.dto;

import java.util.List;
import java.util.Map;

/**
 * Indicadores del tablero operativo (requisito 4.f del enunciado).
 */
public record IndicadoresDTO(
        long totalRecursos,
        Map<String, Long> recursosPorEstado,
        long reservasActivas,
        long prestamosActivos,
        long devolucionesTardias,
        long incidentes,
        long incidentesCriticos,
        List<FilaRecurso> reservasPorRecurso,
        List<FilaCategoria> reservasPorCategoria
) {

    public record FilaRecurso(Long idRecurso, String recurso, String categoria, String ubicacion,
                              String clase, String estado, long reservas) {}

    public record FilaCategoria(String categoria, long reservas) {}
}
