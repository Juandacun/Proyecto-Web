package com.example.proyectoweb.repository;


import com.example.proyectoweb.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {


    //    findAll()
    //    findById()
    //    save()
    //    deleteById()
    //    El service ya puede usarlos
}