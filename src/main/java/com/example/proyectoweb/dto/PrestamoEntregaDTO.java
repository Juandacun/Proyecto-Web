package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class PrestamoEntregaDTO {

    private Long recursoId;
    private String usuarioResponsable;
    private Long usuarioId;
    private String idUsuario;
    private String documento;

    private LocalDate fechaEntrega;
    private LocalDate fechaLimite;
    private LocalDate fechaDevolucionEstimada;

    private String estadoInicial;
    private String estadoEquipo;
    private String observaciones;
    private String accesorios;

    @com.fasterxml.jackson.annotation.JsonCreator
    public PrestamoEntregaDTO() {
    }

    public PrestamoEntregaDTO(Long recursoId, String usuarioResponsable, Object fechaLimite,
                              String estadoInicial, String observaciones, String accesorios) {
        this.recursoId = recursoId;
        this.usuarioResponsable = usuarioResponsable;
        this.fechaLimite = parseDateFlexible(fechaLimite);
        this.estadoInicial = estadoInicial;
        this.observaciones = observaciones;
        this.accesorios = accesorios;
    }

    public static LocalDate parseDateFlexible(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate ld) {
            return ld;
        }
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        if (str.contains("T")) {
            str = str.substring(0, str.indexOf("T"));
        } else if (str.contains(" ")) {
            str = str.substring(0, str.indexOf(" "));
        }
        return LocalDate.parse(str);
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
        if (idUsuario != null && !idUsuario.isBlank()) {
            return idUsuario;
        }
        return usuarioId != null ? String.valueOf(usuarioId) : null;
    }

    public void setUsuarioResponsable(String usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }

    public Long getUsuarioId() {
        if (usuarioId != null) {
            return usuarioId;
        }
        if (idUsuario != null && !idUsuario.isBlank()) {
            try {
                return Long.valueOf(idUsuario.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
        if (this.idUsuario == null && usuarioId != null) {
            this.idUsuario = String.valueOf(usuarioId);
        }
    }

    public String getIdUsuario() {
        if (idUsuario != null && !idUsuario.isBlank()) {
            return idUsuario;
        }
        return usuarioId != null ? String.valueOf(usuarioId) : null;
    }

    public void setIdUsuario(Object idUsuario) {
        if (idUsuario == null) {
            this.idUsuario = null;
        } else {
            this.idUsuario = idUsuario.toString().trim();
            try {
                this.usuarioId = Long.valueOf(this.idUsuario);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public String getDocumento() {
        return getUsuarioResponsable();
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(Object value) {
        this.fechaEntrega = parseDateFlexible(value);
    }

    public void setFechaEntregaFromString(Object value) {
        setFechaEntrega(value);
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate getFechaLimite() {
        if (fechaLimite != null) {
            return fechaLimite;
        }
        return fechaDevolucionEstimada;
    }

    public void setFechaLimite(Object value) {
        this.fechaLimite = parseDateFlexible(value);
    }

    public void setFechaLimiteFromString(Object value) {
        setFechaLimite(value);
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate getFechaDevolucionEstimada() {
        return getFechaLimite();
    }

    public void setFechaDevolucionEstimada(Object value) {
        this.fechaDevolucionEstimada = parseDateFlexible(value);
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
