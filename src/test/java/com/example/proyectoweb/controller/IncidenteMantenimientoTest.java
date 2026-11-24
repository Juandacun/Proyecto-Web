package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.dto.MantenimientoDTO;
import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Mantenimiento;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.IncidenteService;
import com.example.proyectoweb.service.MantenimientoService;
import com.example.proyectoweb.service.RecursoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IncidenteMantenimientoTest {

    private MockMvc mockMvcIncidentes;
    private MockMvc mockMvcMantenimientos;

    @Autowired
    private IncidenteController incidenteController;

    @Autowired
    private MantenimientoController mantenimientoController;

    @Autowired
    private IncidenteService incidenteService;

    @Autowired
    private MantenimientoService mantenimientoService;

    @Autowired
    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        this.mockMvcIncidentes = MockMvcBuilders.standaloneSetup(incidenteController).build();
        this.mockMvcMantenimientos = MockMvcBuilders.standaloneSetup(mantenimientoController).build();
    }

    private Recurso crearRecursoPrueba(String nombre, String estado) {
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
    void testGetListarYBuscarIncidentePorId() throws Exception {
        System.out.println("=== TEST: Listar, buscar por ID y 404 en Incidentes ===");
        Recurso recurso = crearRecursoPrueba("Proyector Aula 101", "Disponible");
        Incidente creado = incidenteService.registrarIncidente(
                new IncidenteDTO(recurso.getIdRecurso(), "Falla de funcionamiento", "Media", "Lámpara parpadea", "docente01"));
        System.out.println("[OUTPUT] Incidente creado id=" + creado.getIdIncidente());

        mockMvcIncidentes.perform(get("/api/incidentes")
                        .param("recursoId", recurso.getIdRecurso().toString())
                        .param("severidad", "Media")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idIncidente", is(creado.getIdIncidente().intValue())));

        mockMvcIncidentes.perform(get("/api/incidentes/" + creado.getIdIncidente())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion", is("Lámpara parpadea")));

        mockMvcIncidentes.perform(get("/api/incidentes/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetListarYBuscarMantenimientoPorId() throws Exception {
        System.out.println("=== TEST: Listar, buscar por ID y 404 en Mantenimientos ===");
        Recurso recurso = crearRecursoPrueba("Router Laboratorio", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(
                new MantenimientoDTO(recurso.getIdRecurso(), "Actualización de firmware", "tecnico01"));
        System.out.println("[OUTPUT] Orden de mantenimiento creada id=" + orden.getIdMantenimiento());

        mockMvcMantenimientos.perform(get("/api/mantenimientos")
                        .param("recursoId", recurso.getIdRecurso().toString())
                        .param("estado", "Abierta")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idMantenimiento", is(orden.getIdMantenimiento().intValue())));

        mockMvcMantenimientos.perform(get("/api/mantenimientos/" + orden.getIdMantenimiento()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico", is("Actualización de firmware")));

        mockMvcMantenimientos.perform(get("/api/mantenimientos/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testPostRN04IncidenteCriticoBloqueaRecursoAutomaticamente() throws Exception {
        System.out.println("=== TEST: RN-04 Incidente crítico bloquea recurso automáticamente ===");
        Recurso recurso = crearRecursoPrueba("Monitor Curvo 34", "Disponible");

        String jsonCritico = String.format("""
                {
                    "recursoId": %d,
                    "tipoNovedad": "Daño físico",
                    "severidad": "Crítica",
                    "descripcion": "Pantalla quebrada por impacto accidental",
                    "usuarioId": "admin@universidad.edu"
                }
                """, recurso.getIdRecurso());

        mockMvcIncidentes.perform(post("/api/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCritico))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idIncidente", notNullValue()))
                .andExpect(jsonPath("$.severidad", is("Crítica")));

        Recurso actualizado = recursoService.buscarPorId(recurso.getIdRecurso());
        System.out.println("[OUTPUT] Recurso " + actualizado.getIdRecurso()
                + " -> estado=" + actualizado.getEstado() + ", estadoColor=" + actualizado.getEstadoColor());
        assertEquals("Bloqueado", actualizado.getEstado());
        assertEquals("rojo", actualizado.getEstadoColor());
    }

    @Test
    void testPostIncidenteLeveNoBloqueaRecurso() throws Exception {
        System.out.println("=== TEST: Incidente leve no bloquea el recurso ===");
        Recurso recurso = crearRecursoPrueba("Teclado Mecánico", "Disponible");

        String jsonLeve = String.format("""
                {
                    "recursoId": %d,
                    "tipoNovedad": "Falla de funcionamiento",
                    "severidad": "Leve",
                    "descripcion": "Tecla Shift ocasionalmente se traba",
                    "usuarioId": "estudiante123"
                }
                """, recurso.getIdRecurso());

        mockMvcIncidentes.perform(post("/api/incidentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLeve))
                .andDo(print())
                .andExpect(status().isCreated());

        Recurso recursoSinCambio = recursoService.buscarPorId(recurso.getIdRecurso());
        System.out.println("[OUTPUT] Recurso " + recursoSinCambio.getIdRecurso()
                + " mantiene estado=" + recursoSinCambio.getEstado());
        assertEquals("Disponible", recursoSinCambio.getEstado());
    }

    @Test
    void testPostCrearOrdenMantenimientoSobreRecursoBloqueado() throws Exception {
        System.out.println("=== TEST: Crear orden de mantenimiento sobre recurso bloqueado ===");
        Recurso recurso = crearRecursoPrueba("Servidor de Prácticas", "Bloqueado");

        String jsonMantenimiento = String.format("""
                {
                    "recursoId": %d,
                    "diagnostico": "Falla en fuente de poder redundante",
                    "idUsuarioResponsable": "tecnico.soporte@universidad.edu",
                    "observaciones": "Se solicita repuesto oficial"
                }
                """, recurso.getIdRecurso());

        mockMvcMantenimientos.perform(post("/api/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMantenimiento))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMantenimiento", notNullValue()))
                .andExpect(jsonPath("$.estado", is("Abierta")))
                .andExpect(jsonPath("$.diagnostico", is("Falla en fuente de poder redundante")));
    }

    @Test
    void testPutActualizarIncidenteACriticoBloqueaRecursoRN04() throws Exception {
        System.out.println("=== TEST: Actualizar incidente a Crítica bloquea recurso (RN-04) ===");
        Recurso recurso = crearRecursoPrueba("Cámara DSLR Canon", "Disponible");
        Incidente creado = incidenteService.registrarIncidente(
                new IncidenteDTO(recurso.getIdRecurso(), "Daño físico", "Leve", "Rayón superficial", "est01"));
        System.out.println("[OUTPUT] Incidente inicial id=" + creado.getIdIncidente()
                + " | estado recurso=" + recursoService.buscarPorId(recurso.getIdRecurso()).getEstado());
        assertEquals("Disponible", recursoService.buscarPorId(recurso.getIdRecurso()).getEstado());

        String jsonPut = """
                {
                    "tipoNovedad": "Daño físico",
                    "severidad": "Crítica",
                    "descripcion": "Lente interno fracturado tras revisión"
                }
                """;

        mockMvcIncidentes.perform(put("/api/incidentes/" + creado.getIdIncidente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPut))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severidad", is("Crítica")))
                .andExpect(jsonPath("$.descripcion", is("Lente interno fracturado tras revisión")));

        Recurso trasPut = recursoService.buscarPorId(recurso.getIdRecurso());
        System.out.println("[OUTPUT] Estado recurso tras actualizar a Crítica: " + trasPut.getEstado());
        assertEquals("Bloqueado", trasPut.getEstado());
    }

    @Test
    void testPutActualizarMantenimientoActivoYRechazarSiCerrado() throws Exception {
        System.out.println("=== TEST: Actualizar mantenimiento activo y rechazar si está cerrado ===");
        Recurso recurso = crearRecursoPrueba("Switch Cisco 24P", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(
                new MantenimientoDTO(recurso.getIdRecurso(), "Diagnóstico inicial", "tecnico01"));
        System.out.println("[OUTPUT] Orden creada id=" + orden.getIdMantenimiento());

        String jsonUpdate = """
                {
                    "diagnostico": "Puertos 5 y 6 quemados, reemplazo de módulo",
                    "idUsuarioResponsable": "tecnico02",
                    "observaciones": "Repuesto instalado"
                }
                """;

        mockMvcMantenimientos.perform(put("/api/mantenimientos/" + orden.getIdMantenimiento())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUpdate))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico", is("Puertos 5 y 6 quemados, reemplazo de módulo")))
                .andExpect(jsonPath("$.idUsuarioResponsable", is("tecnico02")));

        mantenimientoService.finalizar(orden.getIdMantenimiento());
        System.out.println("[OUTPUT] Orden " + orden.getIdMantenimiento() + " finalizada; probando PUT en orden cerrada");
        mockMvcMantenimientos.perform(put("/api/mantenimientos/" + orden.getIdMantenimiento())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUpdate))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    void testPatchRN05FinalizarMantenimientoLiberaRecursoADisponible() throws Exception {
        System.out.println("=== TEST: RN-05 Finalizar mantenimiento libera recurso a Disponible ===");
        Recurso recurso = crearRecursoPrueba("Impresora 3D", "Bloqueado");

        MantenimientoDTO dto = new MantenimientoDTO(recurso.getIdRecurso(),
                "Calibración de cabezal térmico", "tecnico.juan");
        Mantenimiento orden = mantenimientoService.crearOrden(dto);
        System.out.println("[OUTPUT] Orden creada id=" + orden.getIdMantenimiento());

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/finalizar"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMantenimiento", is(orden.getIdMantenimiento().intValue())))
                .andExpect(jsonPath("$.estado", is("Finalizada")))
                .andExpect(jsonPath("$.fechaCierre", notNullValue()));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/finalizar"))
                .andDo(print())
                .andExpect(status().isConflict());

        Recurso recursoLiberado = recursoService.buscarPorId(recurso.getIdRecurso());
        System.out.println("[OUTPUT] Estado final del recurso " + recursoLiberado.getIdRecurso()
                + ": " + recursoLiberado.getEstado());
        assertEquals("Disponible", recursoLiberado.getEstado());
    }

    @Test
    void testPatchCambiarEstadoMantenimientoAEnProcesoYCancelar() throws Exception {
        System.out.println("=== TEST: Cambiar estado mantenimiento a En proceso, Cancelada y rechazar Finalizada ===");
        Recurso recurso = crearRecursoPrueba("Osciloscopio digital", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(new MantenimientoDTO(recurso.getIdRecurso(),
                "Revisión de sondas", "tecnico.ana"));
        System.out.println("[OUTPUT] Orden creada id=" + orden.getIdMantenimiento());

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en proceso\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("En proceso")));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Cancelada\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Cancelada")))
                .andExpect(jsonPath("$.fechaCierre", notNullValue()));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Finalizada\"}"))
                .andDo(print())
                .andExpect(status().isConflict());

        Recurso estadoFinal = recursoService.buscarPorId(recurso.getIdRecurso());
        System.out.println("[OUTPUT] Recurso " + estadoFinal.getIdRecurso() + " sigue en estado=" + estadoFinal.getEstado());
        assertEquals("Bloqueado", estadoFinal.getEstado());
    }

    @Test
    void testPatchCambiarEstadoMantenimientoInvalidoRetorna400() throws Exception {
        System.out.println("=== TEST: Estado de mantenimiento inválido retorna 400 ===");
        Recurso recurso = crearRecursoPrueba("Fuente de poder", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(new MantenimientoDTO(recurso.getIdRecurso(),
                "Cambio de fusible", "tecnico.ana"));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Pausada\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Estado no válido")));
    }

    @Test
    void testDeleteEliminarIncidenteYMantenimiento() throws Exception {
        System.out.println("=== TEST: Eliminar incidente y mantenimiento (204 -> 404) ===");
        Recurso recurso = crearRecursoPrueba("Micrófono Condensador", "Disponible");
        Incidente incidente = incidenteService.registrarIncidente(
                new IncidenteDTO(recurso.getIdRecurso(), "Accesorio faltante", "Leve", "Falta cable XLR", "est02"));
        System.out.println("[OUTPUT] Eliminando incidente id=" + incidente.getIdIncidente());

        mockMvcIncidentes.perform(delete("/api/incidentes/" + incidente.getIdIncidente()))
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvcIncidentes.perform(get("/api/incidentes/" + incidente.getIdIncidente()))
                .andDo(print())
                .andExpect(status().isNotFound());

        Mantenimiento orden = mantenimientoService.crearOrden(
                new MantenimientoDTO(recurso.getIdRecurso(), "Revisión de cápsula", "tecnico03"));
        System.out.println("[OUTPUT] Eliminando mantenimiento id=" + orden.getIdMantenimiento());

        mockMvcMantenimientos.perform(delete("/api/mantenimientos/" + orden.getIdMantenimiento()))
                .andDo(print())
                .andExpect(status().isNoContent());

        mockMvcMantenimientos.perform(get("/api/mantenimientos/" + orden.getIdMantenimiento()))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}