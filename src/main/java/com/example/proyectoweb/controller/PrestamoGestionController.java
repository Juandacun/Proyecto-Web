package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.PrestamoService;
import com.example.proyectoweb.service.RecursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class PrestamoGestionController {

    private static final List<String> ESTADOS_EQUIPO = List.of("Bueno", "Regular", "Dañado");

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private RecursoService recursoService;

    public record PrestamoForm(String recursoId, String documento, String fechaEntrega,
                               String fechaDevolucionEstimada, String estadoEquipo, String observaciones) {}

    public record PrestamoVM(Long id, String recurso, LocalDate fechaEntrega, LocalDate fechaDevolucionEstimada,
                             String horario, String responsable, String estado, String estadoColor,
                             String devolucion, String devolucionColor) {}

    @GetMapping("/prestamos")
    public String prestamos(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamos", listado());
        return "prestamos/prestamos";
    }

    @GetMapping("/prestamos/gestion")
    public String gestion(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamos", listado());
        return "prestamos/prestamos-gestion";
    }

    @GetMapping("/prestamos/nuevo")
    public String nuevo(Model model) {
        vistaFormulario(new PrestamoForm("", "", "", "", "", ""), null, model);
        return "prestamos/prestamo-entrega";
    }

    @PostMapping("/prestamos")
    public String registrar(@ModelAttribute PrestamoForm form, Model model) {
        if (form.recursoId() == null || form.recursoId().isBlank()
                || form.documento() == null || form.documento().isBlank()
                || form.fechaEntrega() == null || form.fechaEntrega().isBlank()
                || form.fechaDevolucionEstimada() == null || form.fechaDevolucionEstimada().isBlank()) {
            vistaFormulario(form, "Completa el equipo, el documento y las fechas de entrega y devolución.", model);
            return "prestamos/prestamo-entrega";
        }

        Recurso recurso = recursoService.buscarPorId(Long.valueOf(form.recursoId()));
        if (recurso == null) {
            vistaFormulario(form, "El recurso seleccionado ya no existe.", model);
            return "prestamos/prestamo-entrega";
        }
        if (!"Disponible".equalsIgnoreCase(recurso.getEstado())) {
            vistaFormulario(form, "No se puede prestar un recurso que no esté disponible (bloqueado o ya prestado).", model);
            return "prestamos/prestamo-entrega";
        }

        Prestamo prestamo = new Prestamo(recurso, form.documento(),
                LocalDate.parse(form.fechaEntrega()),
                LocalDate.parse(form.fechaDevolucionEstimada()),
                form.estadoEquipo(), form.observaciones());
        prestamoService.guardar(prestamo);
        recurso.setEstado("En préstamo");
        recursoService.actualizar(recurso);
        return "redirect:/prestamos/gestion";
    }

    @GetMapping("/prestamos/{id}/devolucion")
    public String registrarDevolucion(@PathVariable Long id) {
        Prestamo prestamo = prestamoService.buscarPorId(id);
        if (prestamo != null && !"Devuelto".equals(prestamo.getEstado())) {
            prestamo.setEstado("Devuelto");
            prestamo.setFechaDevolucion(LocalDate.now());
            prestamoService.actualizar(prestamo);

            Recurso recurso = prestamo.getRecurso();
            recurso.setEstado("Disponible");
            recursoService.actualizar(recurso);
        }
        return "redirect:/prestamos/gestion";
    }

    private List<PrestamoVM> listado() {
        return prestamoService.listar().stream()
                .map(p -> new PrestamoVM(p.getIdPrestamo(), p.getRecurso().getNombre(),
                        p.getFechaEntrega(), p.getFechaDevolucionEstimada(), "—",
                        p.getDocumento(), p.getEstado(), Prestamo.colorDeEstado(p.getEstado()),
                        devolucionTexto(p), devolucionColor(p)))
                .toList();
    }

    private String devolucionTexto(Prestamo p) {
        if (p.getEstado().equals("Devuelto") && p.getFechaDevolucion() != null) {
            return p.getFechaDevolucion().isAfter(p.getFechaDevolucionEstimada())
                    ? "Tardío" : "A tiempo";
        }
        return null;
    }

    private String devolucionColor(Prestamo p) {
        return "Tardío".equals(devolucionTexto(p)) ? "rojo" : "verde";
    }

    private void vistaFormulario(PrestamoForm form, String error, Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("prestamo", form);
        model.addAttribute("recursos", recursoService.listar());
        model.addAttribute("estadosEquipo", ESTADOS_EQUIPO);
        model.addAttribute("error", error);
    }
}