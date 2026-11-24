package com.example.proyectoweb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo; // Estudiante, Docente, Investigador, Administrativo

    @Column(nullable = false, unique = true)
    private String codigo;

    private String documento;

    private String email;

    @Column(unique = true)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'ROLE_USER'")
    private String rol = "ROLE_USER"; // ROLE_USER, ROLE_GESTOR, ROLE_ADMIN

    public Usuario() {
    }

    public Usuario(String nombre, String tipo, String codigo, String documento, String email) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.codigo = codigo;
        this.documento = documento;
        this.email = email;
        this.username = codigo;
        this.rol = "ROLE_USER";
    }

    public Usuario(String nombre, String tipo, String codigo, String documento, String email,
                   String username, String password, String rol) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.codigo = codigo;
        this.documento = documento;
        this.email = email;
        this.username = username != null ? username : codigo;
        this.password = password;
        setRol(rol);
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return codigo;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        if (rol == null || rol.isBlank()) {
            return "ROLE_USER";
        }
        return rol;
    }

    public void setRol(String rol) {
        if (rol != null && !rol.isBlank() && !rol.startsWith("ROLE_")) {
            this.rol = "ROLE_" + rol.trim().toUpperCase();
        } else {
            this.rol = rol != null ? rol.trim() : "ROLE_USER";
        }
    }
}
