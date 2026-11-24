package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.PrestamoEntregaDTO;
import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.PrestamoService;
import com.example.proyectoweb.service.RecursoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class PrestamoControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private PrestamoController prestamoController;

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(prestamoController).build();
    }

    private Recurso crearEquipoPrueba(String nombre, String estado) {
        Recurso r = new Recurso();
        r.setNombre(nombre);
        r.setTipo("Equipo de cómputo");
        r.setEstado(estado);
        r.setClase(Recurso.Clase.EQUIPO);
        Categoria cat = new Categoria();
        cat.setIdCategoria(1L);
        r.setCategoria(cat);
        Ubicacion ubi = new Ubicacion();
        ubi.setIdUbicacion(1L);
        r.setUbicacion(ubi);
        Recurso guardado = recursoService.guardar(r);
        System.out.println("[TEST SETUP] Creado recurso id=" + guardado.getIdRecurso()
                + " | nombre=" + guardado.getNombre() + " | estado=" + guardado.getEstado());
        return guardado;
    }

    @Test
    void testListarPrestamosDevuelveJsonYEstado200() throws Exception {
        System.out.println("=== TEST: Listar préstamos (200) ===");
        mockMvc.perform(get("/api/prestamos")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testRegistrarEntregaValidaYTransicionEstadoRecurso() throws Exception {
        System.out.println("=== TEST: Entrega válida y transición a Prestado ===");
        Recurso equipo = crearEquipoPrueba("Laptop Lenovo ThinkPad", "Disponible");

        String jsonEntrega = String.format("""
                {
                    "recursoId": %d,
                    "usuarioResponsable": "1019283746",
                    "fechaEntrega": "2026-10-15",
                    "fechaLimite": "2026-10-25",
                    "estadoInicial": "Bueno",
                    "observaciones": "Entrega con cargador y mouse",
                    "accesorios": "Cargador 65W, Cable HDMI"
                }
                """, equipo.getIdRecurso());

        mockMvc.perform(post("/api/prestamos/entrega")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEntrega))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPrestamo", notNullValue()))
                .andExpect(jsonPath("$.estado", is("Prestado")))
                .andExpect(jsonPath("$.documento", is("1019283746")))
                .andExpect(jsonPath("$.accesorios", is("Cargador 65W, Cable HDMI")));

        Recurso recursoActualizado = recursoService.buscarPorId(equipo.getIdRecurso());
        System.out.println("[OUTPUT] Estado actualizado del recurso " + recursoActualizado.getIdRecurso()
                + ": " + recursoActualizado.getEstado());
        org.junit.jupiter.api.Assertions.assertEquals("Prestado", recursoActualizado.getEstado());
    }

    @Test
    void testRN02RecursoBloqueadoNoSePuedePrestarRetorna409Conflict() throws Exception {
        System.out.println("=== TEST: RN-02 Recurso bloqueado retorna 409 ===");
        Recurso equipoBloqueado = crearEquipoPrueba("Proyector Dañado", "Bloqueado");

        String jsonEntrega = String.format("""
                {
                    "recursoId": %d,
                    "usuarioResponsable": "987654321",
                    "fechaLimite": "%s",
                    "estadoInicial": "Regular"
                }
                """, equipoBloqueado.getIdRecurso(), LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/prestamos/entrega")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEntrega))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-02")))
                .andExpect(jsonPath("$.error", containsString("bloqueado por daño o mantenimiento")));
    }

    @Test
    void testRN03RecursoYaPrestadoNoSePuedeEntregarNuevamenteRetorna409Conflict() throws Exception {
        System.out.println("=== TEST: RN-03 Recurso ya prestado retorna 409 ===");
        Recurso equipo = crearEquipoPrueba("Tablet iPad Pro", "Disponible");

        PrestamoEntregaDTO primerPrestamo = new PrestamoEntregaDTO(
                equipo.getIdRecurso(), "111222333", LocalDate.now().plusDays(5),
                "Bueno", "Primer préstamo", "Funda");
        Prestamo p1 = prestamoService.registrarEntrega(primerPrestamo);
        System.out.println("[OUTPUT] Primer préstamo creado id=" + p1.getIdPrestamo() + " | estado=" + p1.getEstado());

        String jsonSegundoPrestamo = String.format("""
                {
                    "recursoId": %d,
                    "usuarioResponsable": "444555666",
                    "fechaLimite": "%s",
                    "estadoInicial": "Bueno"
                }
                """, equipo.getIdRecurso(), LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/prestamos/entrega")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSegundoPrestamo))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-03")))
                .andExpect(jsonPath("$.error", containsString("ya figura en estado Prestado")));
    }

    @Test
    void testNoPrestarEspaciosRetorna400BadRequest() throws Exception {
        System.out.println("=== TEST: No prestar espacios retorna 400 ===");
        String jsonEspacio = String.format("""
                {
                    "recursoId": 2,
                    "usuarioResponsable": "100200300",
                    "fechaLimite": "%s"
                }
                """, LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/prestamos/entrega")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEspacio))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Solo se pueden prestar recursos de tipo EQUIPO")));
    }

    @Test
    void testRegistrarDevolucionPuntualYRecursoQuedaDisponible() throws Exception {
        System.out.println("=== TEST: Devolución puntual y liberación de recurso ===");
        Recurso equipo = crearEquipoPrueba("Cámara Fotográfica Canon", "Disponible");

        PrestamoEntregaDTO entrega = new PrestamoEntregaDTO(
                equipo.getIdRecurso(), "555666777", LocalDate.now().plusDays(5),
                "Bueno", "Entrega", "Lente 50mm");
        Prestamo guardado = prestamoService.registrarEntrega(entrega);
        System.out.println("[OUTPUT] Préstamo inicial id=" + guardado.getIdPrestamo());

        String jsonDevolucion = """
                {
                    "fechaDevolucion": "2026-10-16",
                    "estadoFinal": "Bueno",
                    "observaciones": "Equipo devuelto en perfectas condiciones"
                }
                """;

        mockMvc.perform(patch("/api/prestamos/" + guardado.getIdPrestamo() + "/devolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDevolucion))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Devuelto")))
                .andExpect(jsonPath("$.puntualidad", is("Puntual")));

        Recurso recursoLiberado = recursoService.buscarPorId(equipo.getIdRecurso());
        System.out.println("[OUTPUT] Estado final del recurso " + recursoLiberado.getIdRecurso()
                + ": " + recursoLiberado.getEstado());
        org.junit.jupiter.api.Assertions.assertEquals("Disponible", recursoLiberado.getEstado());
    }

    @Test
    void testRegistrarDevolucionTardia() throws Exception {
        System.out.println("=== TEST: Devolución tardía ===");
        Recurso equipo = crearEquipoPrueba("Micrófono Inalámbrico Prueba", "Disponible");

        PrestamoEntregaDTO entrega = new PrestamoEntregaDTO(
                equipo.getIdRecurso(), "888999000", LocalDate.of(2026, 10, 10),
                "Bueno", "Entrega", "Pilas recargables");
        entrega.setFechaEntrega(LocalDate.of(2026, 10, 1));
        Prestamo guardado = prestamoService.registrarEntrega(entrega);
        System.out.println("[OUTPUT] Préstamo con vencimiento 2026-10-10 creado id=" + guardado.getIdPrestamo());

        String jsonDevolucionTardia = """
                {
                    "fechaDevolucion": "2026-10-15",
                    "estadoFinal": "Bueno",
                    "observaciones": "Devuelto con días de retraso"
                }
                """;

        mockMvc.perform(patch("/api/prestamos/" + guardado.getIdPrestamo() + "/devolucion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDevolucionTardia))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Devuelto")))
                .andExpect(jsonPath("$.puntualidad", is("Tardía")));
    }

    @Test
    void testListarPrestamosActivosYTardios() throws Exception {
        System.out.println("=== TEST: Listar préstamos activos y tardíos ===");
        mockMvc.perform(get("/api/prestamos/activos")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));

        mockMvc.perform(get("/api/prestamos/tardios")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testDeserializacionDirectaJackson() throws Exception {
        System.out.println("=== TEST: Deserialización directa Jackson ===");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String json = """
                {
                    "recursoId": 6,
                    "usuarioResponsable": "Juan Pérez",
                    "fechaEntrega": "2026-10-22T15:55:00",
                    "fechaLimite": "2026-10-23T18:00:00"
                }
                """;
        PrestamoEntregaDTO dto = mapper.readValue(json, PrestamoEntregaDTO.class);
        System.out.println("[OUTPUT] DTO deserializado -> fechaEntrega=" + dto.getFechaEntrega()
                + ", fechaLimite=" + dto.getFechaLimite() + ", responsable=" + dto.getUsuarioResponsable());
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2026, 10, 22), dto.getFechaEntrega());
        org.junit.jupiter.api.Assertions.assertEquals(java.time.LocalDate.of(2026, 10, 23), dto.getFechaLimite());
    }

    @Test
    void testRegistrarEntregaConPayloadIsoCompletoEIdUsuario() throws Exception {
        System.out.println("=== TEST: Entrega con payload ISO completo e ID de usuario ===");
        Recurso equipo = crearEquipoPrueba("Tablet iPad Pro", "Disponible");

        String jsonPayload = String.format("""
                {
                    "recursoId": %d,
                    "usuarioResponsable": "Juan Pérez - Estudiante (EST-202410)",
                    "idUsuario": 1,
                    "usuarioId": 1,
                    "fechaEntrega": "2026-10-22T15:55:00",
                    "fechaLimite": "2026-10-23T18:00:00",
                    "estadoInicial": "Operativo",
                    "observaciones": "",
                    "accesorios": "Cables, cargador, estuche"
                }
                """, equipo.getIdRecurso());

        mockMvc.perform(post("/api/prestamos/entrega")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPrestamo", notNullValue()))
                .andExpect(jsonPath("$.usuarioResponsable", is("Juan Pérez - Estudiante (EST-202410)")))
                .andExpect(jsonPath("$.fechaEntrega", is("2026-10-22")))
                .andExpect(jsonPath("$.fechaDevolucionEstimada", is("2026-10-23")))
                .andExpect(jsonPath("$.estado", is("Prestado")));
    }
}