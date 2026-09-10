package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PrestamoController {

    @GetMapping("/prestamos")
    public String prestamos(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamos", List.of());
        return "prestamos/prestamos";
    }
}