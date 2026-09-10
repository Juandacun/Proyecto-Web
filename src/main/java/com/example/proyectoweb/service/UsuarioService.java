package com.example.proyectoweb.service;


import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;



@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;


    // Listar usuarios
    public List<Usuario> listar(){
        return repository.findAll();
    }



    // Buscar usuario por ID
    public Usuario buscarPorId(Long id){
        return repository.findById(id)
                .orElse(null);
    }



    // Crear usuario
    public Usuario guardar(Usuario usuario){
        return repository.save(usuario);
    }



    // Actualizar usuario
    public Usuario actualizar(Usuario usuario){
        return repository.save(usuario);
    }



    // Eliminar usuario
    public void eliminar(Long id){
        if(repository.existsById(id)){

            repository.deleteById(id);
        }
    }

}