package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByRecursoIdRecurso(Long idRecurso);

    List<Prestamo> findByDocumentoIgnoreCase(String documento);

    List<Prestamo> findByEstadoIgnoreCase(String estado);

    @Query("SELECT p FROM Prestamo p WHERE LOWER(p.estado) NOT LIKE 'devuelt%'")
    List<Prestamo> findActivos();

    @Query("SELECT p FROM Prestamo p WHERE p.recurso.idRecurso = :recursoId AND LOWER(p.estado) NOT LIKE 'devuelt%'")
    List<Prestamo> findActivosPorRecurso(@Param("recursoId") Long recursoId);
}