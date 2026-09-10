package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IncidenteController {

    public record IncidenteForm(String recursoId, String tipo, String severidad, String descripcion) {}

    @GetMapping("/incidentes")
    public String incidentes(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("incidente", new IncidenteForm("", "", "", ""));
        model.addAttribute("recursos", List.of());
        model.addAttribute("tipos", List.of());
        model.addAttribute("severidades", List.of());
        model.addAttribute("incidentes", List.of());
        return "incidentes/incidentes";
    }
}