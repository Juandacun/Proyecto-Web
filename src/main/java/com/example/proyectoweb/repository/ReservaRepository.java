package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}