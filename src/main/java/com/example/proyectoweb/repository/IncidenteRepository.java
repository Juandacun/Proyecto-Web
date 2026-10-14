package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    List<Incidente> findByRecursoIdRecurso(Long idRecurso);

    List<Incidente> findBySeveridadIgnoreCase(String severidad);

    List<Incidente> findByRecursoIdRecursoAndSeveridadIgnoreCase(Long idRecurso, String severidad);
}