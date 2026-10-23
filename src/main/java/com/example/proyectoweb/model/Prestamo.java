package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "prestamos")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrestamo;

    @ManyToOne
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private String documento;

    private String usuarioResponsable;

    @Column(nullable = false)
    private LocalDate fechaEntrega;

    @Column(nullable = false)
    private LocalDate fechaDevolucionEstimada;

    private String estadoEquipo;

    private String observaciones;

    private String accesorios;

    @Column(nullable = false)
    private String estado;

    private LocalDate fechaDevolucion;

    private String puntualidad;

    public Prestamo() {
    }

    public Prestamo(Recurso recurso, String documento, LocalDate fechaEntrega,
                    LocalDate fechaDevolucionEstimada, String estadoEquipo, String observaciones) {
        this.recurso = recurso;
        this.documento = documento;
        this.usuarioResponsable = documento;
        this.fechaEntrega = fechaEntrega;
        this.fechaDevolucionEstimada = fechaDevolucionEstimada;
        this.estadoEquipo = estadoEquipo;
        this.observaciones = observaciones;
        this.estado = "Prestado";
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public String getDocumento() {
        return documento != null ? documento : usuarioResponsable;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
        if (this.usuarioResponsable == null) {
            this.usuarioResponsable = documento;
        }
    }

    public String getUsuarioResponsable() {
        return usuarioResponsable != null ? usuarioResponsable : documento;
    }

    public void setUsuarioResponsable(String usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
        if (this.documento == null) {
            this.documento = usuarioResponsable;
        }
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDate getFechaDevolucionEstimada() {
        return fechaDevolucionEstimada;
    }

    public void setFechaDevolucionEstimada(LocalDate fechaDevolucionEstimada) {
        this.fechaDevolucionEstimada = fechaDevolucionEstimada;
    }

    public String getEstadoEquipo() {
        return estadoEquipo;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getPuntualidad() {
        if (puntualidad != null && !puntualidad.isBlank()) {
            return puntualidad;
        }
        if ("Devuelto".equalsIgnoreCase(this.estado) && fechaDevolucion != null && fechaDevolucionEstimada != null) {
            return fechaDevolucion.isAfter(fechaDevolucionEstimada) ? "Tardía" : "Puntual";
        }
        if (!"Devuelto".equalsIgnoreCase(this.estado) && fechaDevolucionEstimada != null) {
            return LocalDate.now().isAfter(fechaDevolucionEstimada) ? "Vencido" : "En curso";
        }
        return "Pendiente";
    }

    public void setPuntualidad(String puntualidad) {
        this.puntualidad = puntualidad;
    }

    public boolean esTardio() {
        if ("Devuelto".equalsIgnoreCase(this.estado)) {
            return (fechaDevolucion != null && fechaDevolucionEstimada != null && fechaDevolucion.isAfter(fechaDevolucionEstimada))
                    || "Tardía".equalsIgnoreCase(this.puntualidad);
        }
        return fechaDevolucionEstimada != null && LocalDate.now().isAfter(fechaDevolucionEstimada);
    }

    public String getEstadoColor() {
        if (esTardio() && !"Devuelto".equalsIgnoreCase(this.estado)) {
            return "rojo";
        }
        return colorDeEstado(this.estado);
    }

    public static String colorDeEstado(String estado) {
        if (estado == null) {
            return "gris";
        }
        String e = estado.toLowerCase();
        if (e.contains("devuelt")) {
            return "verde";
        }
        if (e.contains("prest")) {
            return "ambar";
        }
        if (e.contains("vencid") || e.contains("tard")) {
            return "rojo";
        }
        return "gris";
    }
}