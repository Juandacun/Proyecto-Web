package com.example.proyectoweb.service;

import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository repository;

    public List<Reserva> listar() {
        return repository.findAll();
    }

    public Reserva buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Reserva guardar(Reserva reserva) {
        return repository.save(reserva);
    }

    public Reserva actualizar(Reserva reserva) {
        return repository.save(reserva);
    }

    public boolean seSuperpone(Reserva nueva) {
        Long id = nueva.getIdReserva();
        return repository.findAll().stream().anyMatch(r ->
                (id == null || !r.getIdReserva().equals(id))
                && !"Cancelada".equals(r.getEstado())
                && r.getRecurso().getIdRecurso().equals(nueva.getRecurso().getIdRecurso())
                && r.getFecha().equals(nueva.getFecha())
                && nueva.getHoraInicio().compareTo(r.getHoraFin()) < 0
                && nueva.getHoraFin().compareTo(r.getHoraInicio()) > 0);
    }
}