package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class TableroController {

    @GetMapping("/tablero")
    public String tablero(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("indicadores", List.of());
        model.addAttribute("reservasPorCategoria", List.of());
        model.addAttribute("estadoRecursos", List.of());
        return "dashboard/tablero";
    }
}