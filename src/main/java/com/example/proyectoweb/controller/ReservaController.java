package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReservaController {

    public record ReservaForm(String recursoId, String fecha, String horaInicio, String horaFin, String motivo) {}

    @GetMapping("/reservas")
    public String reservas(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("reservas", List.of());
        return "reservas/reservas";
    }

    @GetMapping("/reservas/nueva")
    public String nueva(Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("reserva", new ReservaForm("", "", "", "", ""));
        model.addAttribute("recursos", List.of());
        model.addAttribute("error", null);
        model.addAttribute("exito", null);
        return "reservas/reserva-nueva";
    }
}