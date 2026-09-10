package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MantenimientoController {

    @GetMapping("/mantenimiento")
    public String mantenimiento(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("intervenciones", List.of());
        return "mantenimiento/mantenimiento";
    }
}