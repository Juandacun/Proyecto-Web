package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PrestamoGestionController {

    public record PrestamoForm(String recursoId, String documento, String fechaEntrega,
                               String fechaDevolucionEstimada, String estadoEquipo, String observaciones) {}

    @GetMapping("/prestamos/gestion")
    public String gestion(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamos", List.of());
        return "prestamos/prestamos-gestion";
    }

    @GetMapping("/prestamos/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamo", new PrestamoForm("", "", "", "", "", ""));
        model.addAttribute("recursos", List.of());
        model.addAttribute("estadosEquipo", List.of());
        model.addAttribute("error", null);
        return "prestamos/prestamo-entrega";
    }
}