package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.repository.IncidenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidenteService {

    @Autowired
    private IncidenteRepository repository;

    public List<Incidente> listar() {
        return repository.findAll();
    }

    public Incidente guardar(Incidente incidente) {
        return repository.save(incidente);
    }
}