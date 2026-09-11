package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.RecursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class RecursoDetalleController {

    @Autowired
    private RecursoService recursoService;

    public record RecursoVM(Long id, String nombre, String estado, String estadoColor, String foto,
                            String descripcion, String categoria, String ubicacion, String tipo, Integer capacidad) {}

    @GetMapping({"/catalogo/{id}", "/catalogo/{id}/disponibilidad"})
    public String detalle(@PathVariable Long id, Model model) {
        Recurso recurso = recursoService.buscarPorId(id);
        if (recurso == null) {
            return "redirect:/catalogo";
        }
        RecursoVM vm = new RecursoVM(recurso.getIdRecurso(), recurso.getNombre(), recurso.getEstado(),
                recurso.getEstadoColor(), null, recurso.getDescripcion(),
                recurso.getCategoria().getNombre(), recurso.getUbicacion().getNombre(), recurso.getTipo(), null);
        model.addAttribute("usuario", null);
        model.addAttribute("recurso", vm);
        model.addAttribute("franjas", List.of());
        return "recursos/detalle-recurso";
    }
}