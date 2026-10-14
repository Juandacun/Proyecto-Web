package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class PrestamoDevolucionDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaDevolucion;

    private String estadoFinal;
    private String estadoEquipo;
    private String observaciones;

    public PrestamoDevolucionDTO() {
    }

    public PrestamoDevolucionDTO(LocalDate fechaDevolucion, String estadoFinal, String observaciones) {
        this.fechaDevolucion = fechaDevolucion;
        this.estadoFinal = estadoFinal;
        this.observaciones = observaciones;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion != null ? fechaDevolucion : LocalDate.now();
    }

    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getEstadoFinal() {
        if (estadoFinal != null && !estadoFinal.isBlank()) {
            return estadoFinal;
        }
        return estadoEquipo != null && !estadoEquipo.isBlank() ? estadoEquipo : "Bueno";
    }

    public void setEstadoFinal(String estadoFinal) {
        this.estadoFinal = estadoFinal;
    }

    public String getEstadoEquipo() {
        return getEstadoFinal();
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
}
