package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository repository;

    public List<Categoria> listar(){
        return repository.findAll();
    }

    public Categoria buscarPorId(Long id){

        return repository.findById(id).orElse(null);
    }

    public Categoria guardar(Categoria categoria){

        return repository.save(categoria);
    }

    public Categoria actualizar(Categoria categoria){

        return repository.save(categoria);
    }

    public void eliminar(Long id){

        if(repository.existsById(id)){
            repository.deleteById(id);
        }
    }

}