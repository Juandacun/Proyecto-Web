package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @ManyToOne
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String horaInicio;

    @Column(nullable = false)
    private String horaFin;

    private String motivo;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String solicitante;

    public Reserva() {
    }

    public Reserva(Recurso recurso, LocalDate fecha, String horaInicio, String horaFin, String motivo,
                   String estado, String solicitante) {
        this.recurso = recurso;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.motivo = motivo;
        this.estado = estado;
        this.solicitante = solicitante;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public static String colorDeEstado(String estado) {
        if (estado == null) {
            return "gris";
        }
        String e = estado.toLowerCase();
        if (e.contains("confirmad")) {
            return "verde";
        }
        if (e.contains("cancelad")) {
            return "rojo";
        }
        return "gris";
    }
}