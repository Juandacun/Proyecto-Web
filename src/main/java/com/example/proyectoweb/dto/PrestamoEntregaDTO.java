package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class PrestamoEntregaDTO {

    private Long recursoId;
    private String usuarioResponsable;
    private String idUsuario;
    private String documento;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEntrega;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaLimite;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaDevolucionEstimada;

    private String estadoInicial;
    private String estadoEquipo;
    private String observaciones;
    private String accesorios;

    public PrestamoEntregaDTO() {
    }

    public PrestamoEntregaDTO(Long recursoId, String usuarioResponsable, LocalDate fechaLimite,
                              String estadoInicial, String observaciones, String accesorios) {
        this.recursoId = recursoId;
        this.usuarioResponsable = usuarioResponsable;
        this.fechaLimite = fechaLimite;
        this.estadoInicial = estadoInicial;
        this.observaciones = observaciones;
        this.accesorios = accesorios;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }

    public String getUsuarioResponsable() {
        if (usuarioResponsable != null && !usuarioResponsable.isBlank()) {
            return usuarioResponsable;
        }
        if (documento != null && !documento.isBlank()) {
            return documento;
        }
        return idUsuario;
    }

    public void setUsuarioResponsable(String usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDocumento() {
        return getUsuarioResponsable();
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDate getFechaLimite() {
        if (fechaLimite != null) {
            return fechaLimite;
        }
        return fechaDevolucionEstimada;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public LocalDate getFechaDevolucionEstimada() {
        return getFechaLimite();
    }

    public void setFechaDevolucionEstimada(LocalDate fechaDevolucionEstimada) {
        this.fechaDevolucionEstimada = fechaDevolucionEstimada;
    }

    public String getEstadoInicial() {
        if (estadoInicial != null && !estadoInicial.isBlank()) {
            return estadoInicial;
        }
        return estadoEquipo != null && !estadoEquipo.isBlank() ? estadoEquipo : "Bueno";
    }

    public void setEstadoInicial(String estadoInicial) {
        this.estadoInicial = estadoInicial;
    }

    public String getEstadoEquipo() {
        return getEstadoInicial();
    }

    public void setEstadoEquipo(String estadoEquipo) {
        this.estadoEquipo = estadoEquipo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getAccesorios() {
        return accesorios;
    }

    public void setAccesorios(String accesorios) {
        this.accesorios = accesorios;
    }
}
