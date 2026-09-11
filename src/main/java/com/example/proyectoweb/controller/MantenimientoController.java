package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Intervencion;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.IntervencionService;
import com.example.proyectoweb.service.RecursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class MantenimientoController {

    @Autowired
    private IntervencionService intervencionService;

    @Autowired
    private RecursoService recursoService;

    public record IntervencionForm(String recursoId, String diagnostico) {}

    public record IntervencionVM(Long id, String recurso, LocalDateTime apertura, String diagnostico,
                                 String estado, String estadoColor, LocalDateTime cierre) {}

    @GetMapping("/mantenimiento")
    public String mantenimiento(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("intervenciones", listado());
        return "mantenimiento/mantenimiento";
    }

    @GetMapping("/mantenimiento/nuevo")
    public String nueva(Model model) {
        vistaFormulario(new IntervencionForm("", ""), null, model);
        return "mantenimiento/intervencion-nueva";
    }

    @PostMapping("/mantenimiento")
    public String registrar(@ModelAttribute IntervencionForm form, Model model) {
        if (form.recursoId() == null || form.recursoId().isBlank()) {
            vistaFormulario(form, "Selecciona el recurso a intervenir.", model);
            return "mantenimiento/intervencion-nueva";
        }

        Recurso recurso = recursoService.buscarPorId(Long.valueOf(form.recursoId()));
        if (recurso == null) {
            vistaFormulario(form, "El recurso seleccionado ya no existe.", model);
            return "mantenimiento/intervencion-nueva";
        }

        Intervencion intervencion = new Intervencion(recurso, LocalDateTime.now(),
                form.diagnostico(), "En mantenimiento", null);
        intervencionService.guardar(intervencion);
        recurso.setEstado("En mantenimiento");
        recursoService.actualizar(recurso);
        return "redirect:/mantenimiento";
    }

    @PostMapping("/mantenimiento/{id}/finalizar")
    public String finalizar(@PathVariable Long id) {
        Intervencion intervencion = intervencionService.buscarPorId(id);
        if (intervencion != null && intervencion.getCierre() == null) {
            intervencion.setEstado("Finalizada");
            intervencion.setCierre(LocalDateTime.now());
            intervencionService.actualizar(intervencion);

            Recurso recurso = intervencion.getRecurso();
            recurso.setEstado("Disponible");
            recursoService.actualizar(recurso);
        }
        return "redirect:/mantenimiento";
    }

    private List<IntervencionVM> listado() {
        return intervencionService.listar().stream()
                .map(iv -> new IntervencionVM(iv.getIdIntervencion(), iv.getRecurso().getNombre(),
                        iv.getApertura(), iv.getDiagnostico(), iv.getEstado(),
                        Intervencion.colorDeEstado(iv.getEstado()), iv.getCierre()))
                .toList();
    }

    private void vistaFormulario(IntervencionForm form, String error, Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("intervencion", form);
        model.addAttribute("recursos", recursoService.listar());
        model.addAttribute("error", error);
    }
}