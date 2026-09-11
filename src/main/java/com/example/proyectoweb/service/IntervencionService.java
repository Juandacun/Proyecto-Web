package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Intervencion;
import com.example.proyectoweb.repository.IntervencionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntervencionService {

    @Autowired
    private IntervencionRepository repository;

    public List<Intervencion> listar() {
        return repository.findAll();
    }

    public Intervencion buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Intervencion guardar(Intervencion intervencion) {
        return repository.save(intervencion);
    }

    public Intervencion actualizar(Intervencion intervencion) {
        return repository.save(intervencion);
    }
}