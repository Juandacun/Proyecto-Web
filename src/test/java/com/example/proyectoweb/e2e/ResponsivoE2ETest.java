package com.example.proyectoweb.e2e;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QA responsivo (requisito no funcional: interfaz responsiva para computador y teléfono móvil).
 * Recorre cada vista de la SPA en teléfonos, tableta y escritorio verificando que no haya
 * desbordamiento horizontal de la página y que la navegación y las acciones principales sigan visibles.
 */
class ResponsivoE2ETest extends E2EBase {

    private int anchoActual = 1280;
    private int altoActual = 800;

    @Override protected int ancho() { return anchoActual; }
    @Override protected int alto() { return altoActual; }

    record Vista(String nombre, String ruta, String titulo) {}

    static final Vista[] VISTAS = {
            new Vista("catalogo", "/catalogo", "Catálogo de recursos"),
            new Vista("reservas", "/reservas", "Reservas de espacios"),
            new Vista("prestamos", "/prestamos", "Gestión de Préstamos y Devoluciones"),
            new Vista("mantenimiento", "/mantenimiento", "Incidentes y Mantenimiento de Recursos"),
            new Vista("tablero", "/tablero", "Tablero e indicadores"),
    };

    record Dispositivo(String nombre, int ancho, int alto) {}

    static final Dispositivo[] DISPOSITIVOS = {
            new Dispositivo("iPhone SE", 320, 568),
            new Dispositivo("Móvil 375", 375, 667),
            new Dispositivo("Móvil grande 414", 414, 896),
            new Dispositivo("Tableta 768", 768, 1024),
            new Dispositivo("Portátil 1280", 1280, 800),
            new Dispositivo("Escritorio 1920", 1920, 1080),
    };

    static Stream<Arguments> vistasPorDispositivo() {
        return Stream.of(DISPOSITIVOS).flatMap(d -> Stream.of(VISTAS).map(v -> Arguments.of(d, v)));
    }

    static Stream<Dispositivo> dispositivos() {
        return Stream.of(DISPOSITIVOS);
    }

    /** Desborde horizontal en px (0 = ninguno). Se tolera 1 px por redondeo. */
    private int desbordeHorizontal() {
        Object r = page.evaluate("() => document.documentElement.scrollWidth - document.documentElement.clientWidth");
        return ((Number) r).intValue();
    }

    private void fijarViewport(Dispositivo d) {
        anchoActual = d.ancho();
        altoActual = d.alto();
        page.setViewportSize(d.ancho(), d.alto());
    }

    @ParameterizedTest(name = "{0} · {1}")
    @MethodSource("vistasPorDispositivo")
    @DisplayName("Cada vista se renderiza sin desbordamiento horizontal")
    void vistaSinDesbordamiento(Dispositivo d, Vista v) {
        iniciarSesionComo("gestor", "gestor123");
        fijarViewport(d);
        irA(v.ruta());
        assertThat(page.locator("h1").first()).hasText(v.titulo());
        page.waitForLoadState();
        int desborde = desbordeHorizontal();
        assertTrue(desborde <= 1,
                v.nombre() + " desborda " + desborde + "px en " + d.nombre() + " (" + d.ancho() + "px)");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dispositivos")
    @DisplayName("El login se ve completo y usable en cada tamaño")
    void loginResponsivo(Dispositivo d) {
        fijarViewport(d);
        irA("/login");
        assertThat(page.locator("#username")).isVisible();
        assertThat(page.locator("#password")).isVisible();
        assertThat(page.locator("button[type=submit]")).isVisible();
        assertTrue(desbordeHorizontal() <= 1, "El login desborda en " + d.nombre());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dispositivos")
    @DisplayName("La navegación principal permanece accesible en cada tamaño")
    void navegacionAccesible(Dispositivo d) {
        iniciarSesionComo("gestor", "gestor123");
        fijarViewport(d);
        irA("/catalogo");
        Locator sidebar = page.locator(".sidebar");
        assertThat(sidebar).isVisible();
        // El enlace de Reservas se alcanza (en móvil la barra se desplaza horizontalmente).
        Locator enlace = sidebar.locator("a[href='/reservas']");
        enlace.scrollIntoViewIfNeeded();
        assertThat(enlace).isVisible();
        enlace.click();
        assertThat(page.locator("h1").first()).hasText("Reservas de espacios");
        assertThat(page.locator("button.logout")).isVisible();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dispositivos")
    @DisplayName("Registrar una entrega se puede hacer con el modal dentro del viewport")
    void modalEntregaCabeEnPantalla(Dispositivo d) {
        iniciarSesionComo("gestor", "gestor123");
        fijarViewport(d);
        irA("/prestamos");
        boton("+ Registrar Entrega").first().click();
        Locator modal = page.locator(".form-card").first();
        assertThat(modal).isVisible();
        Object anchoModal = modal.evaluate("el => el.getBoundingClientRect().right - document.documentElement.clientWidth");
        assertTrue(((Number) anchoModal).doubleValue() <= 1,
                "El modal de entrega se sale del ancho en " + d.nombre());
        // El botón de confirmar es alcanzable haciendo scroll dentro del modal/ventana.
        Locator confirmar = boton("Confirmar Entrega");
        confirmar.scrollIntoViewIfNeeded();
        assertThat(confirmar).isVisible();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dispositivos")
    @DisplayName("Las tablas se adaptan: la página no se desborda y su contenido es alcanzable")
    void tablaReservasAdaptada(Dispositivo d) {
        iniciarSesionComo("gestor", "gestor123");
        fijarViewport(d);
        irA("/reservas");
        assertThat(page.locator("table.tabla")).isVisible();
        assertTrue(desbordeHorizontal() <= 1, "La tabla de reservas desborda la página en " + d.nombre());
    }
}
