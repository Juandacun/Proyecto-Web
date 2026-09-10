package com.example.proyectoweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.proyectoweb.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>{

//    findAll()
//    findById()
//    save()
//    deleteById()
//    El service ya puede usarlos
}