package com.example.proyectoweb.repository;


import com.example.proyectoweb.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface RecursoRepository extends JpaRepository<Recurso, Long>{

    //    findAll()
    //    findById()
    //    save()
    //    deleteById()
    //    El service ya puede usarlos


    List<Recurso> findByEstado(String estado);

    List<Recurso> findByTipo(String tipo);

    List<Recurso> findByCategoriaIdCategoria(Long idCategoria);

    List<Recurso> findByUbicacionIdUbicacion(Long idUbicacion);
}