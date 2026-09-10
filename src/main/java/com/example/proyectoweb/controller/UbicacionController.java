package com.example.proyectoweb.controller;


import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.UbicacionService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/ubicaciones")
public class UbicacionController {


    @Autowired
    private UbicacionService service;

    // LISTAR
    @GetMapping
    public String listar(Model model){
        model.addAttribute("usuario", null);
        model.addAttribute("ubicaciones", service.listar());
        return "ubicaciones/listar";
    }


    // FORMULARIO NUEVO
    @GetMapping("/nuevo")
    public String nuevo(Model model){
        model.addAttribute("usuario", null);
        model.addAttribute("ubicacion", new Ubicacion());
        return "ubicaciones/formulario";
    }


    // GUARDAR
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Ubicacion ubicacion){
        service.guardar(ubicacion);
        return "redirect:/ubicaciones";
    }


    // EDITAR
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model){
        Ubicacion ubicacion = service.buscarPorId(id);
        model.addAttribute("usuario", null);
        model.addAttribute("ubicacion", ubicacion);
        return "ubicaciones/formulario";
    }


    // ACTUALIZAR
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Ubicacion ubicacion){
        service.actualizar(ubicacion);
        return "redirect:/ubicaciones";
    }


    // ELIMINAR
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id){
        service.eliminar(id);
        return "redirect:/ubicaciones";
    }

}