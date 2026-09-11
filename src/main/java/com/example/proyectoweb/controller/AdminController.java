package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.UbicacionService;
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

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    public record RecursoVM(Long id, String nombre, String categoria, String ubicacion,
                            String tipo, String estado, String estadoColor) {}

    public record CategoriaVM(Long id, String nombre, String descripcion) {}

    public record UbicacionVM(Long id, String nombre, String edificio, String descripcion) {}

    @GetMapping("/admin")
    public String admin(@RequestParam(name = "tab", required = false, defaultValue = "recursos") String tab,
                        Model model) {
        List<RecursoVM> recursos = recursoService.listar().stream()
                .map(r -> new RecursoVM(r.getIdRecurso(), r.getNombre(),
                        r.getCategoria().getNombre(), r.getUbicacion().getNombre(),
                        r.getTipo(), r.getEstado(), r.getEstadoColor()))
                .toList();
        List<CategoriaVM> categorias = categoriaService.listar().stream()
                .map(c -> new CategoriaVM(c.getIdCategoria(), c.getNombre(), c.getDescripcion()))
                .toList();
        List<UbicacionVM> ubicaciones = ubicacionService.listar().stream()
                .map(u -> new UbicacionVM(u.getIdUbicacion(), u.getNombre(), u.getEdificio(), u.getDescripcion()))
                .toList();

        model.addAttribute("usuario", null);
        model.addAttribute("tab", tab);
        model.addAttribute("recursos", recursos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("ubicaciones", ubicaciones);
        return "admin/admin";
    }

    @PostMapping("/admin/recursos/{id}/eliminar")
    public String eliminarRecurso(@PathVariable Long id) {
        recursoService.eliminar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaService.eliminar(id);
        } catch (Exception ignored) {
            // No se puede eliminar si tiene recursos asociados
        }
        return "redirect:/admin?tab=categorias";
    }

    @PostMapping("/admin/ubicaciones/{id}/eliminar")
    public String eliminarUbicacion(@PathVariable Long id) {
        try {
            ubicacionService.eliminar(id);
        } catch (Exception ignored) {
            // No se puede eliminar si tiene recursos asociados
        }
        return "redirect:/admin?tab=ubicaciones";
    }
}