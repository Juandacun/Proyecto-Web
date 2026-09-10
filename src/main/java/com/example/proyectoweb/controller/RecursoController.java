package com.example.proyectoweb.controller;


import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.RecursoService;

import com.example.proyectoweb.service.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/recursos")
public class RecursoController {


    @Autowired
    private RecursoService service;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;


    // Listar Recursos
    @GetMapping
    public String listar(Model model){
        model.addAttribute("usuario", null);
        model.addAttribute("recursos", service.listar());
        return "recursos/listar";
    }


    // Formulario Nuevo Recurso
    @GetMapping("/nuevo")
    public String nuevo(Model model){

        model.addAttribute("usuario", null);
        model.addAttribute("recurso", new Recurso());

        model.addAttribute("categorias", categoriaService.listar());

        model.addAttribute("ubicaciones", ubicacionService.listar());

        return "recursos/formulario";
    }

    // Guardar recurso
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Recurso recurso){
        service.guardar(recurso);
        return "redirect:/recursos";
    }


    // Formulario Editar
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model){

        Recurso recurso = service.buscarPorId(id);

        model.addAttribute("usuario", null);
        model.addAttribute("recurso", recurso);

        model.addAttribute("categorias", categoriaService.listar());

        model.addAttribute("ubicaciones", ubicacionService.listar());

        return "recursos/formulario";
    }


    // Actualizar
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Recurso recurso){
        service.actualizar(recurso);
        return "redirect:/recursos";
    }


    // Eliminar
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id){
        service.eliminar(id);
        return "redirect:/recursos";
    }

}