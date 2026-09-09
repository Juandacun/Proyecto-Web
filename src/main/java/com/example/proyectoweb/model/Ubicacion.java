package com.example.proyectoweb.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "ubicaciones")
public class Ubicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUbicacion;


    @Column(nullable = false)
    private String nombre;

    private String edificio;

    private String descripcion;

    // Relación con recursos
    @OneToMany(mappedBy = "categoria")
    private List<Recurso> recursos;

    // Constructores
    public Ubicacion() {

    }

    public Ubicacion(String nombre, String Edificio,String descripcion) {
        this.nombre = nombre;
        this.edificio = Edificio;
        this.descripcion = descripcion;
    }

    // Getters y setters
    public Long getIdUbicacion() {
        return idUbicacion;
    }

    public void setIdUbicacion(Long idUbicacion) {
        this.idUbicacion = idUbicacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEdificio() {
        return edificio;
    }

    public void setEdificio(String edificio) {
        this.edificio = edificio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setRecursos(List<Recurso> recursos) {
        this.recursos = recursos;
    }
}