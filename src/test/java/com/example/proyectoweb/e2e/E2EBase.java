package com.example.proyectoweb.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Base de las pruebas de sistema (E2E): lanza Chromium, aísla cada prueba en su propio
 * {@link BrowserContext} y ofrece utilidades de sesión y de formularios de la SPA.
 *
 * Requiere el backend (http://localhost:8080) y la SPA (http://localhost:4200) en ejecución;
 * el script {@code e2e/run-e2e.sh} los levanta con una base H2 en memoria limpia.
 */
abstract class E2EBase {

    protected static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:4200");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("e2e.headless", "true"));
    private static final double SLOW_MO = Double.parseDouble(System.getProperty("e2e.slowMo", "0"));

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    /** Viewport de la prueba; por defecto escritorio. */
    protected int ancho() { return 1280; }
    protected int alto() { return 800; }

    @BeforeAll
    static void lanzarNavegador() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(HEADLESS).setSlowMo(SLOW_MO));
    }

    @AfterAll
    static void cerrarNavegador() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void crearContexto() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(ancho(), alto()));
        context.setDefaultTimeout(15_000);
        page = context.newPage();
        // Los confirm() de la SPA (finalizar/cancelar mantenimiento) se aceptan automáticamente.
        page.onDialog(dialog -> dialog.accept());
    }

    @AfterEach
    void cerrarContexto(TestInfo info) {
        if (page != null && !page.isClosed()) {
            String nombre = info.getTestClass().map(Class::getSimpleName).orElse("E2E")
                    + "-" + info.getTestMethod().map(m -> m.getName()).orElse("test");
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Path.of("target", "e2e-screenshots", nombre.replaceAll("[^A-Za-z0-9_-]", "_") + ".png"))
                    .setFullPage(true));
        }
        if (context != null) context.close();
    }

    // ---------------------------------------------------------------- sesión

    protected void iniciarSesion(String usuario, String clave) {
        page.navigate(BASE_URL + "/login");
        page.locator("#username").fill(usuario);
        page.locator("#password").fill(clave);
        page.locator("button[type=submit]").click();
    }

    protected void iniciarSesionComo(String usuario, String clave) {
        iniciarSesion(usuario, clave);
        assertThat(page.locator(".topbar")).isVisible();
    }

    protected void cerrarSesion() {
        page.locator("button.logout").click();
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));
    }

    // ------------------------------------------------------------ utilidades

    protected void irA(String ruta) {
        page.navigate(BASE_URL + ruta);
    }

    /** Selecciona en un &lt;select&gt; la opción cuyo texto contiene el fragmento (sirve con [ngValue] y [value]). */
    protected void seleccionarOpcion(String selectorSelect, String fragmentoTexto) {
        Locator opcion = page.locator(selectorSelect + " option").filter(
                new Locator.FilterOptions().setHasText(fragmentoTexto)).first();
        assertThat(opcion).hasCount(1);
        page.locator(selectorSelect).selectOption(opcion.getAttribute("value"));
    }

    protected Locator fila(String textoFila) {
        return page.locator("table.tabla tbody tr").filter(new Locator.FilterOptions().setHasText(textoFila));
    }

    protected Locator tarjetaRecurso(String nombre) {
        return page.locator("article.recurso").filter(new Locator.FilterOptions().setHasText(nombre));
    }

    /** Aviso de éxito de la vista (no confunde con las tarjetas informativas de reglas de negocio). */
    protected Locator alertaExito() {
        return page.locator(".content > .alerta--exito");
    }

    protected Locator boton(String nombre) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(nombre));
    }
}
