package com.example.proyectoweb.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** HU-011: autenticación y control de acceso por rol en la SPA. */
class SeguridadRolesE2ETest extends E2EBase {

    @Test
    @DisplayName("Sin sesión, una ruta protegida redirige al login")
    void rutaProtegidaRedirigeAlLogin() {
        irA("/reservas");
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));
        assertThat(page.locator("h1")).hasText("Iniciar sesión");
    }

    @Test
    @DisplayName("Credenciales inválidas muestran un error y no inician sesión")
    void credencialesInvalidas() {
        iniciarSesion("user", "clave-incorrecta");
        assertThat(page.locator(".alert-error")).hasText("Usuario o contraseña incorrectos.");
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));
    }

    @Test
    @DisplayName("Campos vacíos en el login piden usuario y contraseña")
    void camposVacios() {
        irA("/login");
        page.locator("button[type=submit]").click();
        assertThat(page.locator(".alert-error")).hasText("Ingresa tu usuario y tu contraseña.");
    }

    @Test
    @DisplayName("El solicitante no ve tablero ni administración y es bloqueado por URL")
    void solicitanteSinAccesoAGestion() {
        iniciarSesionComo("user", "user123");
        assertThat(page.locator(".sidebar a[href='/tablero']")).hasCount(0);
        assertThat(page.locator(".sidebar a[href='/admin/usuarios']")).hasCount(0);

        irA("/tablero");
        assertThat(page).hasURL(Pattern.compile(".*/acceso-denegado.*"));
        irA("/admin/usuarios");
        assertThat(page).hasURL(Pattern.compile(".*/acceso-denegado.*"));
    }

    @Test
    @DisplayName("El gestor accede al tablero pero no a la administración de usuarios")
    void gestorVeTableroPeroNoAdmin() {
        iniciarSesionComo("gestor", "gestor123");
        assertThat(page.locator(".sidebar a[href='/tablero']")).isVisible();
        assertThat(page.locator(".role-pill")).isVisible();
        irA("/admin/usuarios");
        assertThat(page).hasURL(Pattern.compile(".*/acceso-denegado.*"));
    }

    @Test
    @DisplayName("El administrador accede a Usuarios y roles")
    void adminVeUsuariosYRoles() {
        iniciarSesionComo("admin", "admin123");
        assertThat(page.locator(".sidebar a[href='/admin/usuarios']")).isVisible();
        irA("/admin/usuarios");
        assertThat(page).hasURL(Pattern.compile(".*/admin/usuarios.*"));
    }

    @Test
    @DisplayName("Cerrar sesión devuelve al login y protege de nuevo las rutas")
    void cerrarSesionProtegeRutas() {
        iniciarSesionComo("gestor", "gestor123");
        cerrarSesion();
        irA("/prestamos");
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));
    }
}
