package com.example.proyectoweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String admin(@RequestParam(name = "tab", required = false, defaultValue = "recursos") String tab,
                        Model model) {
        model.addAttribute("usuario", null);
        model.addAttribute("tab", tab);
        model.addAttribute("recursos", List.of());
        return "admin/admin";
    }
}