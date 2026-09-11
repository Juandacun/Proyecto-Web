package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {
}