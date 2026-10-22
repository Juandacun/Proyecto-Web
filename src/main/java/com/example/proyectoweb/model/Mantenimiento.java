package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mantenimientos")
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMantenimiento;

    @ManyToOne
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private String diagnostico;

    @Column(nullable = false)
    private String estado; // Abierta, En proceso, Finalizada, Cancelada

    private LocalDateTime fechaCierre;

    private String idUsuarioResponsable;

    private String observaciones;

    public Mantenimiento() {
    }

    public Mantenimiento(Recurso recurso, LocalDateTime fechaApertura, String diagnostico,
                         String estado, String idUsuarioResponsable) {
        this.recurso = recurso;
        this.fechaApertura = fechaApertura;
        this.diagnostico = diagnostico;
        this.estado = estado;
        this.idUsuarioResponsable = idUsuarioResponsable;
    }

    public Long getIdMantenimiento() {
        return idMantenimiento;
    }

    public void setIdMantenimiento(Long idMantenimiento) {
        this.idMantenimiento = idMantenimiento;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
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
        return estado;
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
