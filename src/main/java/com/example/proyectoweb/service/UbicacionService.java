package com.example.proyectoweb.service;


import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.repository.UbicacionRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;


@Service
public class UbicacionService {


    @Autowired
    private UbicacionRepository repository;

    // Listar ubicaciones
    public List<Ubicacion> listar(){
        return repository.findAll();
    }


    // Buscar por ID
    public Ubicacion buscarPorId(Long id){
        return repository.findById(id).orElse(null);
    }

    // Crear
    public Ubicacion guardar(Ubicacion ubicacion){
        return repository.save(ubicacion);
    }

    // Actualizar
    public Ubicacion actualizar(Ubicacion ubicacion){
        return repository.save(ubicacion);
    }


    // Eliminar
    public void eliminar(Long id){

        if(repository.existsById(id)){
            repository.deleteById(id);
        }

    }

}