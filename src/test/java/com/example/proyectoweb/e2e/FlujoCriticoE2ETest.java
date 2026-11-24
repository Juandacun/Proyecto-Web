package com.example.proyectoweb.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Prueba de sistema del caso de uso más largo y complejo (HU-008, HU-009, HU-006, HU-007, HU-010):
 * ciclo de vida completo de un equipo tras un incidente crítico.
 *
 *  1. Un solicitante reporta un incidente CRÍTICO sobre un equipo           (RN-04 → Bloqueado)
 *  2. El gestor verifica el bloqueo en el catálogo y no puede prestarlo     (RN-02)
 *  3. El gestor abre una orden de mantenimiento, la inicia y la finaliza    (RN-05 → Disponible)
 *  4. El gestor registra la entrega del equipo                              (→ Prestado, RN-03)
 *  5. El gestor registra la devolución                                      (→ Disponible, Puntual)
 *  6. El tablero refleja el incidente crítico y el estado final del recurso
 */
class FlujoCriticoE2ETest extends E2EBase {

    private static final String EQUIPO = "Tablero digital interactivo";

    @Test
    @DisplayName("Incidente crítico → bloqueo → mantenimiento → préstamo → devolución → tablero")
    void cicloCompletoDeUnEquipoConIncidenteCritico() {
        // ---- 1. Solicitante reporta incidente crítico (HU-008 / RN-04)
        iniciarSesionComo("user", "user123");
        irA("/mantenimiento");
        seleccionarOpcion("#recursoIncidente", EQUIPO);
        page.locator("#tipoNovedad").selectOption("Daño físico");
        page.locator("#severidad").selectOption("Crítica");
        assertThat(page.locator(".alerta--error").filter(
                new Locator.FilterOptions().setHasText("RN-04"))).not().hasCount(0);
        page.locator("#descripcionIncidente").fill("E2E: la pantalla táctil quedó fisurada y no responde.");
        boton("Registrar Reporte").click();
        assertThat(alertaExito()).containsText("quedó Bloqueado (RN-04)");
        cerrarSesion();

        // ---- 2. Gestor comprueba el bloqueo en el catálogo (HU-001) y que no se puede prestar (RN-02)
        iniciarSesionComo("gestor", "gestor123");
        irA("/catalogo");
        assertThat(tarjetaRecurso(EQUIPO).locator(".tag")).hasText("Bloqueado");
        assertThat(tarjetaRecurso(EQUIPO)).containsText("No disponible");

        // ---- 3. Mantenimiento: abrir orden, iniciar y finalizar (HU-009 / RN-05)
        irA("/mantenimiento");
        boton("+ Nueva Orden de Mantenimiento").click();
        seleccionarOpcion("#recursoMant", EQUIPO);
        page.locator("#diagnostico").fill("E2E: reemplazo del panel táctil.");
        boton("Crear Orden").click();
        assertThat(alertaExito()).containsText("creada con éxito");

        var ordenEquipo = fila(EQUIPO).filter(new Locator.FilterOptions().setHasText("Abierta"));
        assertThat(ordenEquipo).hasCount(1);
        ordenEquipo.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Iniciar")).click();
        assertThat(alertaExito()).containsText("en proceso");

        var ordenEnProceso = fila(EQUIPO).filter(new Locator.FilterOptions().setHasText("En proceso"));
        ordenEnProceso.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Finalizar")).click();
        assertThat(alertaExito()).containsText("fue liberado y ahora está en estado \"Disponible\"");

        irA("/catalogo");
        assertThat(tarjetaRecurso(EQUIPO).locator(".tag")).hasText("Disponible");

        // ---- 4. Entrega del equipo (HU-006 / RN-03)
        irA("/prestamos");
        boton("+ Registrar Entrega").first().click();
        seleccionarOpcion("#selectEquipo", EQUIPO);
        assertThat(page.locator(".form-card .alerta--exito")).containsText("Equipo disponible para entrega");
        seleccionarOpcion("#usuarioResponsable", "Laura Martínez");
        page.locator("#accesorios").fill("Lápiz táctil y cable HDMI");
        boton("Confirmar Entrega").click();
        assertThat(alertaExito()).containsText("El equipo pasó a estado \"Prestado\"");

        // RN-03: mientras esté prestado, su estado en el catálogo es Prestado y no ofrece préstamo
        irA("/catalogo");
        assertThat(tarjetaRecurso(EQUIPO).locator(".tag")).hasText("Prestado");
        assertThat(tarjetaRecurso(EQUIPO).getByText("Prestar", new Locator.GetByTextOptions().setExact(true))).hasCount(0);

        // ---- 5. Devolución puntual (HU-007)
        irA("/prestamos");
        var prestamo = fila(EQUIPO).filter(new Locator.FilterOptions().setHasText("En Curso"));
        assertThat(prestamo).hasCount(1);
        prestamo.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Registrar Devolución")).click();
        page.locator("#estadoFinal").selectOption("Buen estado general");
        page.locator("#obsDevolucion").fill("E2E: sin novedades tras la reparación.");
        boton("Liberar a Disponible").click();
        assertThat(alertaExito()).containsText("Control de puntualidad: Puntual");

        irA("/catalogo");
        assertThat(tarjetaRecurso(EQUIPO).locator(".tag")).hasText("Disponible");

        // ---- 6. Tablero e indicadores (HU-010)
        irA("/tablero");
        assertThat(page.locator("h1")).hasText("Tablero e indicadores");
        assertThat(page.locator(".kpi").filter(new Locator.FilterOptions()
                .setHasText("Incidentes registrados"))).containsText("crítico");
        assertThat(fila(EQUIPO)).containsText("Disponible");
    }
}
