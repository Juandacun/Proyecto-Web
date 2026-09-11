package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidentes")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIncidente;

    @ManyToOne
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String severidad;

    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Incidente() {
    }

    public Incidente(Recurso recurso, String tipo, String severidad, String descripcion, LocalDateTime fecha) {
        this.recurso = recurso;
        this.tipo = tipo;
        this.severidad = severidad;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public Long getIdIncidente() {
        return idIncidente;
    }

    public void setIdIncidente(Long idIncidente) {
        this.idIncidente = idIncidente;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public void setRecurso(Recurso recurso) {
        this.recurso = recurso;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getSeveridad() {
        return severidad;
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
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public static String colorDeSeveridad(String severidad) {
        if (severidad == null) {
            return "gris";
        }
        String s = severidad.toLowerCase();
        if (s.contains("critic")) {
            return "rojo";
        }
        if (s.contains("leve")) {
            return "verde";
        }
        return "azul";
    }
}