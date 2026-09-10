package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PrestamoGestionController {

    @GetMapping("/prestamos/gestion")
    public String gestion(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamos", List.of());
        return "prestamos/prestamos-gestion";
    }
}