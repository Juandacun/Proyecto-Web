package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.IncidenteService;
import com.example.proyectoweb.service.RecursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class IncidenteController {

    private static final List<String> TIPOS = List.of(
            "Daño físico", "Falla técnica", "Conectividad",
            "Accesorios faltantes", "Limpieza", "Otro");

    private static final List<String> SEVERIDADES = List.of(
            "Leve", "Media", "Crítica");

    @Autowired
    private IncidenteService incidenteService;

    @Autowired
    private RecursoService recursoService;

    public record IncidenteForm(String recursoId, String tipo, String severidad, String descripcion) {}

    public record IncidenteVM(String recurso, String tipo, String severidad, String severidadColor,
                              LocalDateTime fecha) {}

    @GetMapping("/incidentes")
    public String incidentes(Model model) {
        return vista(formularioVacio(), null, model);
    }

    @PostMapping("/incidentes")
    public String reportar(@ModelAttribute IncidenteForm form, Model model) {
        if (form.recursoId() == null || form.recursoId().isBlank()) {
            return vista(form, "Selecciona el recurso afectado.", model);
        }
        if (form.descripcion() == null || form.descripcion().isBlank()) {
            return vista(form, "Describe la novedad o falla del recurso.", model);
        }

        Recurso recurso = recursoService.buscarPorId(Long.valueOf(form.recursoId()));
        if (recurso == null) {
            return vista(form, "El recurso seleccionado ya no existe.", model);
        }

        Incidente incidente = new Incidente(recurso, form.tipo(), form.severidad(),
                form.descripcion(), LocalDateTime.now());
        incidenteService.guardar(incidente);

        if ("Crítica".equals(form.severidad())) {
            recurso.setEstado("Bloqueado");
            recursoService.actualizar(recurso);
        }
        return "redirect:/incidentes";
    }

    private IncidenteForm formularioVacio() {
        return new IncidenteForm("", "", "", "");
    }

    private String vista(IncidenteForm form, String error, Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("incidente", form);
        model.addAttribute("recursos", recursoService.listar());
        model.addAttribute("tipos", TIPOS);
        model.addAttribute("severidades", SEVERIDADES);
        model.addAttribute("error", error);
        model.addAttribute("incidentes", incidenteService.listar().stream()
                .map(i -> new IncidenteVM(i.getRecurso().getNombre(), i.getTipo(), i.getSeveridad(),
                        Incidente.colorDeSeveridad(i.getSeveridad()), i.getFecha()))
                .toList());
        return "incidentes/incidentes";
    }
}