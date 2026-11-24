package com.example.proyectoweb.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** HU-004 / HU-005: reservas con validación de solapamiento (RN-01), bloqueo (RN-02) y cancelación. */
class ReservasE2ETest extends E2EBase {

    private static final String ESPACIO = "Sala de estudio grupal 3";
    private static final String ESPACIO_BLOQUEADO = "Laboratorio de Química Orgánica";

    private void abrirFormulario(String espacio, LocalDate fecha, String inicio, String fin) {
        boton("+ Nueva reserva").click();
        seleccionarOpcion("#r-recurso", espacio);
        page.locator("#r-fecha").fill(fecha.toString());
        page.locator("#r-inicio").selectOption(inicio);
        page.locator("#r-fin").selectOption(fin);
    }

    @Test
    @DisplayName("Crear, detectar conflicto de horario (RN-01) y cancelar una reserva")
    void crearConflictoYCancelar() {
        LocalDate fecha = LocalDate.now().plusDays(10);
        iniciarSesionComo("user", "user123");
        irA("/reservas");

        // Crea la reserva 10:00–12:00
        abrirFormulario(ESPACIO, fecha, "10:00", "12:00");
        assertThat(page.locator(".disp--ok")).isVisible();
        page.locator("#r-solicitante").fill("Juan Pérez");
        page.locator("#r-motivo").fill("E2E: sustentación");
        boton("Confirmar reserva").click();
        assertThat(alertaExito()).containsText("confirmada: " + ESPACIO);

        // Un intervalo solapado 11:00–13:00 es rechazado en el formulario
        abrirFormulario(ESPACIO, fecha, "11:00", "13:00");
        assertThat(page.locator(".disp--no")).isVisible();
        assertThat(boton("Confirmar reserva")).isDisabled();
        boton("Volver").click();

        // Cancela la reserva creada y el horario queda libre
        var fila = fila(ESPACIO).filter(new Locator.FilterOptions().setHasText("E2E: sustentación"));
        assertThat(fila).hasCount(1);
        fila.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancelar")).click();
        boton("Sí, cancelar reserva").click();
        assertThat(alertaExito()).containsText("cancelada. El horario quedó libre.");

        abrirFormulario(ESPACIO, fecha, "11:00", "13:00");
        assertThat(page.locator(".disp--ok")).isVisible();
    }

    @Test
    @DisplayName("Un recurso bloqueado no se puede reservar (RN-02)")
    void recursoBloqueadoNoSeReserva() {
        iniciarSesionComo("user", "user123");
        irA("/reservas");
        abrirFormulario(ESPACIO_BLOQUEADO, LocalDate.now().plusDays(11), "09:00", "10:00");
        assertThat(page.locator(".disp--no")).isVisible();
        assertThat(boton("Confirmar reserva")).isDisabled();
    }
}
