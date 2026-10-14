package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class MantenimientoDTO {

    private Long recursoId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaApertura;

    private String diagnostico;
    private String estado;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCierre;

    private String idUsuarioResponsable;
    private String observaciones;

    public MantenimientoDTO() {
    }

    public MantenimientoDTO(Long recursoId, String diagnostico, String idUsuarioResponsable) {
        this.recursoId = recursoId;
        this.diagnostico = diagnostico;
        this.idUsuarioResponsable = idUsuarioResponsable;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura != null ? fechaApertura : LocalDateTime.now();
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getEstado() {
        return estado != null && !estado.isBlank() ? estado : "Abierta";
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public String getIdUsuarioResponsable() {
        return idUsuarioResponsable;
    }

    public void setIdUsuarioResponsable(String idUsuarioResponsable) {
        this.idUsuarioResponsable = idUsuarioResponsable;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
