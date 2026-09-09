package com.example.proyectoweb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.proyectoweb.model.Recurso;

public interface RecursoRepository extends JpaRepository<Recurso, Long>{

//    findAll()
//    findById()
//    save()
//    deleteById()
//    El service ya puede usarlos
}