package com.example.proyectoweb.service;


import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.repository.RecursoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecursoService {

    @Autowired
    private RecursoRepository repository;

    // READ - listar todos
    public List<Recurso> listar() {
        return repository.findAll();
    }

    // READ - buscar uno
    public Recurso buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    // CREATE
    public Recurso guardar(Recurso recurso) {
        return repository.save(recurso);
    }

    // UPDATE
    public Recurso actualizar(Recurso recurso) {
        return repository.save(recurso);
    }

    // DELETE
    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}