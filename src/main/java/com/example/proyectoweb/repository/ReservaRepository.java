package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByRecursoIdRecurso(Long idRecurso);

    List<Reserva> findByFecha(LocalDate fecha);

    List<Reserva> findByRecursoIdRecursoAndFecha(Long idRecurso, LocalDate fecha);

    List<Reserva> findByEstadoIgnoreCase(String estado);

    @Query("SELECT r FROM Reserva r WHERE r.recurso.idRecurso = :recursoId AND r.fecha = :fecha AND LOWER(r.estado) NOT IN ('cancelada', 'rechazada', 'finalizada')")
    List<Reserva> findActivasPorRecursoYFecha(@Param("recursoId") Long recursoId, @Param("fecha") LocalDate fecha);
}