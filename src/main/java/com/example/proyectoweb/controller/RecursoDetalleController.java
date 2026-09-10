package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class RecursoDetalleController {

    public record RecursoVM(Long id, String nombre, String estado, String estadoColor, String foto,
                            String descripcion, String categoria, String ubicacion, String tipo, Integer capacidad) {}

    @GetMapping({"/catalogo/{id}", "/catalogo/{id}/disponibilidad"})
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("recurso", new RecursoVM(id, null, null, null, null, null, null, null, null, null));
        model.addAttribute("franjas", List.of());
        return "recursos/detalle-recurso";
    }
}