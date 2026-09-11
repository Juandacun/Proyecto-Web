package com.example.proyectoweb.controller;

import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class TableroController {

    @Autowired
    private RecursoService recursoService;

    @Autowired
    private ReservaService reservaService;

    public record Fila(String recurso, String categoria, long reservas) {}

    @GetMapping("/tablero")
    public String tablero(Model model) {
        model.addAttribute("usuario", null);

        List<Fila> filas = recursoService.listar().stream()
                .map(r -> new Fila(r.getNombre(), r.getCategoria().getNombre(),
                        reservasDe(r.getIdRecurso())))
                .sorted(Comparator.comparingLong(Fila::reservas).reversed())
                .toList();

        model.addAttribute("filas", filas);
        return "dashboard/tablero";
    }

    private long reservasDe(Long idRecurso) {
        return reservaService.listar().stream()
                .filter(r -> !"Cancelada".equals(r.getEstado()))
                .filter(r -> r.getRecurso().getIdRecurso().equals(idRecurso))
                .count();
    }
}