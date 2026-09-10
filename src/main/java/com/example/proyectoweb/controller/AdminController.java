package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.RecursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private RecursoService recursoService;

    public record RecursoVM(Long id, String nombre, String categoria, String ubicacion,
                            String tipo, String estado, String estadoColor) {}

    @GetMapping("/admin")
    public String admin(@RequestParam(name = "tab", required = false, defaultValue = "recursos") String tab,
                        Model model) {
        List<RecursoVM> recursos = recursoService.listar().stream()
                .map(r -> new RecursoVM(r.getIdRecurso(), r.getNombre(),
                        r.getCategoria().getNombre(), r.getUbicacion().getNombre(),
                        r.getTipo(), r.getEstado(), r.getEstadoColor()))
                .toList();
        model.addAttribute("usuario", null);
        model.addAttribute("tab", tab);
        model.addAttribute("recursos", recursos);
        return "admin/admin";
    }

    @PostMapping("/admin/recursos/{id}/eliminar")
    public String eliminar(@PathVariable Long id) {
        recursoService.eliminar(id);
        return "redirect:/admin";
    }
}