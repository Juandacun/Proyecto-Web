package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.repository.PrestamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrestamoService {

    @Autowired
    private PrestamoRepository repository;

    public List<Prestamo> listar() {
        return repository.findAll();
    }

    public Prestamo buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Prestamo guardar(Prestamo prestamo) {
        return repository.save(prestamo);
    }

    public Prestamo actualizar(Prestamo prestamo) {
        return repository.save(prestamo);
    }
}