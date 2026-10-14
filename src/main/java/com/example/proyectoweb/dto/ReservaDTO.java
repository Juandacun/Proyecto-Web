package com.example.proyectoweb.dto;

import com.example.proyectoweb.model.Recurso;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class ReservaDTO {

    private Long idReserva;
    private Long recursoId;
    private Recurso recurso;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String horaInicio;
    private String horaFin;
    private String motivo;
    private String solicitante;
    private String estado;

    public ReservaDTO() {
    }

    public ReservaDTO(Long recursoId, LocalDate fecha, String horaInicio, String horaFin, String motivo, String solicitante) {
        this.recursoId = recursoId;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.motivo = motivo;
        this.solicitante = solicitante;
    }

    public Long getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public Long getRecursoId() {
        if (recursoId != null) {
            return recursoId;
        }
        if (recurso != null && recurso.getIdRecurso() != null) {
            return recurso.getIdRecurso();
        }
        return null;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
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

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
