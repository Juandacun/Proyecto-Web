package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class IncidenteDTO {

    private Long recursoId;
    private String usuarioId;
    private String reportadoPor;
    private String tipoNovedad;
    private String tipo;
    private String severidad;
    private String descripcion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fecha;

    public IncidenteDTO() {
    }

    public IncidenteDTO(Long recursoId, String tipoNovedad, String severidad, String descripcion, String usuarioId) {
        this.recursoId = recursoId;
        this.tipoNovedad = tipoNovedad;
        this.severidad = severidad;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }

    public String getUsuarioId() {
        return usuarioId != null && !usuarioId.isBlank() ? usuarioId : reportadoPor;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getReportadoPor() {
        return getUsuarioId();
    }

    public void setReportadoPor(String reportadoPor) {
        this.reportadoPor = reportadoPor;
    }

    public String getTipoNovedad() {
        if (tipoNovedad != null && !tipoNovedad.isBlank()) {
            return tipoNovedad;
        }
        return tipo != null && !tipo.isBlank() ? tipo : "Daño físico";
    }

    public void setTipoNovedad(String tipoNovedad) {
        this.tipoNovedad = tipoNovedad;
    }

    public String getTipo() {
        return getTipoNovedad();
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSeveridad() {
        return severidad != null ? severidad : "Media";
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFecha() {
        return fecha != null ? fecha : LocalDateTime.now();
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
