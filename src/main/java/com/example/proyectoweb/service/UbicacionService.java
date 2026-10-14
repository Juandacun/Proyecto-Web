package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.repository.UbicacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UbicacionService {

    @Autowired
    private UbicacionRepository repository;

    @Transactional(readOnly = true)
    public List<Ubicacion> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Ubicacion buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    public Ubicacion guardar(Ubicacion ubicacion) {
        if (ubicacion == null) {
            throw new IllegalArgumentException("La ubicación no puede ser nula.");
        }
        if (ubicacion.getNombre() == null || ubicacion.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la ubicación es obligatorio.");
        }
        return repository.save(ubicacion);
    }

    public Ubicacion actualizar(Long id, Ubicacion ubicacionActualizada) {
        Ubicacion existente = buscarPorId(id);
        if (existente == null) {
            return null;
        }
        if (ubicacionActualizada.getNombre() != null && !ubicacionActualizada.getNombre().isBlank()) {
            existente.setNombre(ubicacionActualizada.getNombre().trim());
        }
        existente.setEdificio(ubicacionActualizada.getEdificio());
        existente.setDescripcion(ubicacionActualizada.getDescripcion());
        return repository.save(existente);
    }

    public Ubicacion actualizar(Ubicacion ubicacion) {
        return repository.save(ubicacion);
    }

    public boolean eliminar(Long id) {
        if (id != null && repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}