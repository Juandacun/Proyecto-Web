package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CatalogoController {

    @Autowired
    private RecursoService recursoService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    public record RecursoVM(Long id, String nombre, String estado, String estadoColor, String imagen,
                            String categoria, String ubicacion) {}

    public record Filtro(String q, String categoria, String ubicacion, String tipo, String estado) {}

    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(name = "q", required = false) String q,
                           @RequestParam(name = "categoria", required = false) String categoria,
                           @RequestParam(name = "ubicacion", required = false) String ubicacion,
                           @RequestParam(name = "tipo", required = false) String tipo,
                           @RequestParam(name = "estado", required = false) String estado,
                           Model model) {
        List<Recurso> todos = recursoService.listar();
        String busqueda = q == null ? null : normalizar(q);

        List<Recurso> recursos = todos.stream()
                .filter(r -> busqueda == null || busqueda.isBlank() || coindice(r, busqueda))
                .filter(r -> categoria == null || categoria.isBlank() || r.getCategoria().getNombre().equals(categoria))
                .filter(r -> ubicacion == null || ubicacion.isBlank() || r.getUbicacion().getNombre().equals(ubicacion))
                .filter(r -> tipo == null || tipo.isBlank() || r.getTipo().equals(tipo))
                .filter(r -> estado == null || estado.isBlank() || r.getEstado().equals(estado))
                .toList();

        List<String> categorias = categoriaService.listar().stream().map(c -> c.getNombre()).distinct().sorted().toList();
        List<String> ubicaciones = ubicacionService.listar().stream().map(u -> u.getNombre()).distinct().sorted().toList();
        List<String> tipos = todos.stream().map(Recurso::getTipo).distinct().sorted().toList();
        List<String> estados = todos.stream().map(Recurso::getEstado).distinct().sorted().toList();

        List<RecursoVM> recursosVM = recursos.stream()
                .map(r -> new RecursoVM(r.getIdRecurso(), r.getNombre(), r.getEstado(), r.getEstadoColor(), null,
                        r.getCategoria().getNombre(), r.getUbicacion().getNombre()))
                .toList();

        model.addAttribute("usuario", null);
        model.addAttribute("categorias", categorias);
        model.addAttribute("ubicaciones", ubicaciones);
        model.addAttribute("tipos", tipos);
        model.addAttribute("estados", estados);
        model.addAttribute("filtro", new Filtro(busqueda, categoria, ubicacion, tipo, estado));
        model.addAttribute("recursos", recursosVM);
        return "recursos/catalogo";
    }

    private boolean coindice(Recurso r, String busqueda) {
        String texto = String.join(" ", r.getNombre(), r.getCategoria().getNombre(),
                r.getUbicacion().getNombre(), r.getTipo(), r.getEstado(),
                r.getDescripcion() == null ? "" : r.getDescripcion());
        return normalizar(texto).contains(busqueda);
    }

    private static String normalizar(String s) {
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase();
    }

    @GetMapping("/catalogo/nuevo")
    public String nuevo() {
        return "redirect:/recursos/nuevo";
    }
}