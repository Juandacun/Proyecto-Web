package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaService {

    @Autowired
    private CategoriaRepository repository;

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    public Categoria guardar(Categoria categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no puede ser nula.");
        }
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }
        return repository.save(categoria);
    }

    public Categoria actualizar(Long id, Categoria categoriaActualizada) {
        Categoria existente = buscarPorId(id);
        if (existente == null) {
            return null;
        }
        if (categoriaActualizada.getNombre() != null && !categoriaActualizada.getNombre().isBlank()) {
            existente.setNombre(categoriaActualizada.getNombre().trim());
        }
        existente.setDescripcion(categoriaActualizada.getDescripcion());
        return repository.save(existente);
    }

    public Categoria actualizar(Categoria categoria) {
        return repository.save(categoria);
    }

    public boolean eliminar(Long id) {
        if (id != null && repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}