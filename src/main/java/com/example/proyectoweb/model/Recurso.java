package com.example.proyectoweb.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recursos")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRecurso;


    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo;

    private String descripcion;

    private String caracteristicas;

    @Column(nullable = false)
    private String estado;

    // Relación con recursos
    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "id_ubicacion", nullable = false)
    private Ubicacion ubicacion;

    // Constructores
    public Recurso() {

    }

    public Recurso(String nombre,
                   String tipo,
                   String descripcion,
                   String caracteristicas,
                   String estado) {

        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.caracteristicas = caracteristicas;
        this.estado = estado;
    }

    public Long getIdRecurso() {
        return idRecurso;
    }

    public void setIdRecurso(Long idRecurso) {
        this.idRecurso = idRecurso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(String caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(Ubicacion ubicacion) {
        this.ubicacion = ubicacion;
    }
}