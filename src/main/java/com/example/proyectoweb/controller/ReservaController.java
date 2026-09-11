package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private RecursoService recursoService;

    public record ReservaForm(String recursoId, String fecha, String horaInicio, String horaFin, String motivo) {}

    public record ReservaVM(Long id, String recurso, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                            String solicitante, String estado, String estadoColor, boolean modificable) {}

    @GetMapping("/reservas")
    public String reservas(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("reservas", reservaService.listar().stream()
                .map(r -> new ReservaVM(r.getIdReserva(), r.getRecurso().getNombre(), r.getFecha(),
                        LocalTime.parse(r.getHoraInicio()), LocalTime.parse(r.getHoraFin()),
                        r.getSolicitante(), r.getEstado(), Reserva.colorDeEstado(r.getEstado()),
                        !"Cancelada".equals(r.getEstado())))
                .toList());
        return "reservas/reservas";
    }

    @GetMapping("/reservas/nueva")
    public String nueva(Model model) {
        vistaFormulario(new ReservaForm("", "", "", "", ""), "nuevo", null, null, null, model);
        return "reservas/reserva-nueva";
    }

    @PostMapping("/reservas")
    public String confirmar(@ModelAttribute ReservaForm form, Model model) {
        String error = guardarReserva(new Reserva(), form, model);
        if (error != null) {
            vistaFormulario(form, "nuevo", null, error, null, model);
            return "reservas/reserva-nueva";
        }
        vistaFormulario(new ReservaForm("", "", "", "", ""), "nuevo", null, null,
                "Reserva confirmada.", model);
        return "reservas/reserva-nueva";
    }

    @GetMapping("/reservas/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Reserva r = reservaService.buscarPorId(id);
        if (r == null) {
            return "redirect:/reservas";
        }
        vistaFormulario(new ReservaForm(String.valueOf(r.getRecurso().getIdRecurso()),
                r.getFecha().toString(), r.getHoraInicio(), r.getHoraFin(), r.getMotivo()),
                "editar", r.getIdReserva(), null, null, model);
        return "reservas/reserva-nueva";
    }

    @PostMapping("/reservas/actualizar")
    public String actualizar(@RequestParam Long id, @ModelAttribute ReservaForm form, Model model) {
        Reserva reserva = reservaService.buscarPorId(id);
        if (reserva == null) {
            return "redirect:/reservas";
        }
        String error = guardarReserva(reserva, form, model);
        if (error != null) {
            vistaFormulario(form, "editar", id, error, null, model);
            return "reservas/reserva-nueva";
        }
        return "redirect:/reservas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id) {
        Reserva reserva = reservaService.buscarPorId(id);
        if (reserva != null) {
            reserva.setEstado("Cancelada");
            reservaService.actualizar(reserva);
        }
        return "redirect:/reservas";
    }

    private String guardarReserva(Reserva reserva, ReservaForm form, Model model) {
        if (form.recursoId() == null || form.recursoId().isBlank()
                || form.fecha() == null || form.fecha().isBlank()
                || form.horaInicio() == null || form.horaInicio().isBlank()
                || form.horaFin() == null || form.horaFin().isBlank()) {
            return "Completa el recurso, la fecha y el horario de la reserva.";
        }
        Recurso recurso = recursoService.buscarPorId(Long.valueOf(form.recursoId()));
        if (recurso == null) {
            return "El recurso seleccionado ya no existe.";
        }
        if (form.horaInicio().compareTo(form.horaFin()) >= 0) {
            return "La hora de inicio debe ser anterior a la hora de fin.";
        }
        reserva.setRecurso(recurso);
        reserva.setFecha(LocalDate.parse(form.fecha()));
        reserva.setHoraInicio(form.horaInicio());
        reserva.setHoraFin(form.horaFin());
        reserva.setMotivo(form.motivo());
        if (reserva.getEstado() == null) {
            reserva.setEstado("Confirmada");
        }
        if (reserva.getSolicitante() == null || reserva.getSolicitante().isBlank()) {
            reserva.setSolicitante("Invitado");
        }
        if (reservaService.seSuperpone(reserva)) {
            return "Ya existe una reserva para ese recurso en ese horario.";
        }
        reservaService.guardar(reserva);
        return null;
    }

    private void vistaFormulario(ReservaForm form, String modo, Long reservaId, String error,
                                 String exito, Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("reserva", form);
        model.addAttribute("recursos", recursoService.listar());
        model.addAttribute("modo", modo);
        model.addAttribute("reservaId", reservaId);
        model.addAttribute("error", error);
        model.addAttribute("exito", exito);
    }
}