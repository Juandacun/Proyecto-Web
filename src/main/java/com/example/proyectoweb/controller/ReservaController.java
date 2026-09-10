package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReservaController {

    @GetMapping("/reservas")
    public String reservas(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("reservas", List.of());
        return "reservas/reservas";
    }
}