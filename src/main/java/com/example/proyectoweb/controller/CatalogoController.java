package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class CatalogoController {

    @GetMapping("/catalogo")
    public String catalogo(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("categorias", List.of());
        model.addAttribute("ubicaciones", List.of());
        model.addAttribute("tipos", List.of());
        model.addAttribute("estados", List.of());
        model.addAttribute("filtro", null);
        model.addAttribute("recursos", List.of());
        return "recursos/catalogo";
    }
}