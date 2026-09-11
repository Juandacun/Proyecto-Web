package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intervenciones")
public class Intervencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIntervencion;

    @ManyToOne
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private LocalDateTime apertura;

    private String diagnostico;

    @Column(nullable = false)
    private String estado;

    private LocalDateTime cierre;

    public Intervencion() {
    }

    public Intervencion(Recurso recurso, LocalDateTime apertura, String diagnostico, String estado,
                        LocalDateTime cierre) {
        this.recurso = recurso;
        this.apertura = apertura;
        this.diagnostico = diagnostico;
        this.estado = estado;
        this.cierre = cierre;
    }

    public Long getIdIntervencion() {
        return idIntervencion;
    }

    public void setIdIntervencion(Long idIntervencion) {
        this.idIntervencion = idIntervencion;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public LocalDateTime getApertura() {
        return apertura;
    }

    public void setApertura(LocalDateTime apertura) {
        this.apertura = apertura;
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

    public LocalDateTime getCierre() {
        return cierre;
    }

    public void setCierre(LocalDateTime cierre) {
        this.cierre = cierre;
    }

    public static String colorDeEstado(String estado) {
        if (estado == null) {
            return "gris";
        }
        String e = estado.toLowerCase();
        if (e.contains("finaliz") || e.contains("cerrad")) {
            return "verde";
        }
        if (e.contains("mantenimiento") || e.contains("abierta")) {
            return "rojo";
        }
        return "gris";
    }
}