package com.example.proyectoweb.controller;


import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.service.CategoriaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;



@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService service;

    // Listar categorías
    @GetMapping
    public String listar(Model model){

        model.addAttribute("categorias", service.listar());
        return "categorias/listar";
    }


    // Mostrar formulario crear
    @GetMapping("/nuevo")
    public String nuevo(Model model){

        model.addAttribute("categoria", new Categoria());
        return "categorias/formulario";
    }


    // Guardar categoría
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Categoria categoria){

        service.guardar(categoria);
        return "redirect:/categorias";
    }

    
    // Eliminar categoría
    @GetMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id){

        service.eliminar(id);
        return "redirect:/categorias";
    }

    // Mostrar formulario editar
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model){

        Categoria categoria = service.buscarPorId(id);
        model.addAttribute("categoria", categoria);
        return "categorias/formulario";
    }

    // Actualizar categoría
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Categoria categoria){

        service.actualizar(categoria);
        return "redirect:/categorias";
    }

}