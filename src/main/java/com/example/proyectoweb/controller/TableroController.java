package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.service.IncidenteService;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class TableroController {

    @Autowired
    private RecursoService recursoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private IncidenteService incidenteService;

    public record Kpi(String label, long valor) {}

    public record Barra(String label, long valor, int altoPx) {}

    public record Franja(String label, long valor, String color) {}

    @GetMapping("/tablero")
    public String tablero(Model model) {
        model.addAttribute("usuario", null);

        List<Recurso> recursos = recursoService.listar();
        long total = recursos.size();
        long disponibles = recursos.stream().filter(r -> normalizar(r.getEstado()).contains("disponible")).count();
        long enPrestamo = recursos.stream().filter(r -> normalizar(r.getEstado()).contains("prest")).count();
        long mantenimiento = recursos.stream().filter(r -> normalizar(r.getEstado()).contains("mantenimiento")).count();
        long reservasActivas = reservaService.listar().stream()
                .filter(r -> !"Cancelada".equals(r.getEstado())).count();
        long incidentes = incidenteService.listar().size();

        model.addAttribute("indicadores", List.of(
                new Kpi("Recursos", total),
                new Kpi("Disponibles", disponibles),
                new Kpi("En préstamo", enPrestamo),
                new Kpi("En mantenimiento", mantenimiento),
                new Kpi("Reservas activas", reservasActivas),
                new Kpi("Incidentes", incidentes)));

        Map<String, Long> porEstado = recursos.stream()
                .collect(Collectors.groupingBy(Recurso::getEstado, LinkedHashMap::new, Collectors.counting()));
        model.addAttribute("estadoRecursos", porEstado.entrySet().stream()
                .map(e -> new Franja(e.getKey(), e.getValue(), Recurso.colorDeEstado(e.getKey())))
                .toList());

        Map<String, Long> porCategoria = reservaService.listar().stream()
                .filter(r -> !"Cancelada".equals(r.getEstado()))
                .map(r -> r.getRecurso().getCategoria().getNombre())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        long max = porCategoria.values().stream().mapToLong(Long::longValue).max().orElse(1);
        model.addAttribute("reservasPorCategoria", porCategoria.entrySet().stream()
                .map(e -> new Barra(e.getKey(), e.getValue(),
                        (int) Math.max(24, Math.round((double) e.getValue() * 120 / max))))
                .toList());

        return "dashboard/tablero";
    }

    private static String normalizar(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase();
    }
}