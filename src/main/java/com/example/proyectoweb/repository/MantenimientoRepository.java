package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    List<Mantenimiento> findByRecursoIdRecurso(Long idRecurso);

    List<Mantenimiento> findByEstadoIgnoreCase(String estado);

    List<Mantenimiento> findByRecursoIdRecursoAndEstadoIgnoreCase(Long idRecurso, String estado);
}
