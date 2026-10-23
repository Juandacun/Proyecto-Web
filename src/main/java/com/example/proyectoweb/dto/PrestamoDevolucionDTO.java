package com.example.proyectoweb.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class PrestamoDevolucionDTO {

    private LocalDate fechaDevolucion;
    private String estadoFinal;
    private String estadoEquipo;
    private String observaciones;

    @com.fasterxml.jackson.annotation.JsonCreator
    public PrestamoDevolucionDTO() {
    }

    public PrestamoDevolucionDTO(Object fechaDevolucion, String estadoFinal, String observaciones) {
        this.fechaDevolucion = PrestamoEntregaDTO.parseDateFlexible(fechaDevolucion);
        this.estadoFinal = estadoFinal;
        this.observaciones = observaciones;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate getFechaDevolucion() {
        return fechaDevolucion != null ? fechaDevolucion : LocalDate.now();
    }

    public void setFechaDevolucion(Object value) {
        this.fechaDevolucion = PrestamoEntregaDTO.parseDateFlexible(value);
    }

    public void setFechaDevolucionFromString(Object value) {
        setFechaDevolucion(value);
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
