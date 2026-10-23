package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.dto.MantenimientoDTO;
import com.example.proyectoweb.model.Categoria;
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
        return recursoService.guardar(r);
    }

    @Test
    void testListarIncidentesDevuelveJsonYEstado200() throws Exception {
        mockMvcIncidentes.perform(get("/api/incidentes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testRN04IncidenteCriticoBloqueaRecursoAutomaticamente() throws Exception {
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idIncidente", notNullValue()))
                .andExpect(jsonPath("$.severidad", is("Crítica")));

        // RN-04: Verificar que el recurso pasó automáticamente a 'Bloqueado'
        Recurso actualizado = recursoService.buscarPorId(recurso.getIdRecurso());
        assertEquals("Bloqueado", actualizado.getEstado());
        assertEquals("rojo", actualizado.getEstadoColor());
    }

    @Test
    void testIncidenteLeveNoBloqueaRecurso() throws Exception {
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
                .andExpect(status().isCreated());

        // El recurso sigue en Disponible
        Recurso recursoSinCambio = recursoService.buscarPorId(recurso.getIdRecurso());
        assertEquals("Disponible", recursoSinCambio.getEstado());
    }

    @Test
    void testCrearOrdenMantenimientoSobreRecursoBloqueado() throws Exception {
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMantenimiento", notNullValue()))
                .andExpect(jsonPath("$.estado", is("Abierta")))
                .andExpect(jsonPath("$.diagnostico", is("Falla en fuente de poder redundante")));
    }

    @Test
    void testRN05FinalizarMantenimientoLiberaRecursoADisponible() throws Exception {
        Recurso recurso = crearRecursoPrueba("Impresora 3D", "Bloqueado");

        // 1. Crear orden de mantenimiento
        MantenimientoDTO dto = new MantenimientoDTO(recurso.getIdRecurso(),
                "Calibración de cabezal térmico", "tecnico.juan");
        Mantenimiento orden = mantenimientoService.crearOrden(dto);

        // 2. Finalizar la intervención (RN-05)
        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMantenimiento", is(orden.getIdMantenimiento().intValue())))
                .andExpect(jsonPath("$.estado", is("Finalizada")))
                .andExpect(jsonPath("$.fechaCierre", notNullValue()));

        // RN-05: El recurso debe transicionar automáticamente a 'Disponible'
        Recurso recursoLiberado = recursoService.buscarPorId(recurso.getIdRecurso());
        assertEquals("Disponible", recursoLiberado.getEstado());
    }

    @Test
    void testCambiarEstadoMantenimientoAEnProcesoYCancelar() throws Exception {
        Recurso recurso = crearRecursoPrueba("Osciloscopio digital", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(new MantenimientoDTO(recurso.getIdRecurso(),
                "Revisión de sondas", "tecnico.ana"));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"en proceso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("En proceso")));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Cancelada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("Cancelada")))
                .andExpect(jsonPath("$.fechaCierre", notNullValue()));

        // Una orden cancelada no vuelve a cambiar de estado y el recurso sigue bloqueado
        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Finalizada\"}"))
                .andExpect(status().isConflict());
        assertEquals("Bloqueado", recursoService.buscarPorId(recurso.getIdRecurso()).getEstado());
    }

    @Test
    void testCambiarEstadoMantenimientoInvalidoRetorna400() throws Exception {
        Recurso recurso = crearRecursoPrueba("Fuente de poder", "Bloqueado");
        Mantenimiento orden = mantenimientoService.crearOrden(new MantenimientoDTO(recurso.getIdRecurso(),
                "Cambio de fusible", "tecnico.ana"));

        mockMvcMantenimientos.perform(patch("/api/mantenimientos/" + orden.getIdMantenimiento() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"Pausada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Estado no válido")));
    }
}
