package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @PostConstruct
    public void inicializarUsuariosSiEstaVacio() {
        if (repository.count() == 0) {
            repository.save(new Usuario("Juan Pérez", "Estudiante", "EST-202410", "1098765432", "juan.perez@universidad.edu.co"));
            repository.save(new Usuario("Dra. María Gómez", "Docente", "DOC-108234", "52891234", "maria.gomez@universidad.edu.co"));
            repository.save(new Usuario("Carlos Rodríguez", "Investigador", "INV-309112", "79845612", "carlos.rodriguez@universidad.edu.co"));
            repository.save(new Usuario("Ing. Ana Torres", "Administrativo", "ADM-401928", "63901234", "ana.torres@universidad.edu.co"));
            repository.save(new Usuario("Laura Martínez", "Estudiante", "EST-202411", "104109003", "laura.martinez@universidad.edu.co"));
            repository.save(new Usuario("David Morales", "Estudiante", "EST-202412", "1018902345", "david.morales@universidad.edu.co"));
        }
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        if (id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Transactional
    public Usuario guardar(Usuario usuario) {
        return repository.save(usuario);
    }
}
