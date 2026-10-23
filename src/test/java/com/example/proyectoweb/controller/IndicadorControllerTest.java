package com.example.proyectoweb.controller;

import com.example.proyectoweb.dto.IncidenteDTO;
import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.IncidenteService;
import com.example.proyectoweb.service.RecursoService;
import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IndicadorControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private IndicadorController indicadorController;

    @Autowired
    private IncidenteService incidenteService;

    @Autowired
    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(indicadorController).build();
    }

    private Recurso crearRecursoPrueba(String nombre) {
        Recurso r = new Recurso();
        r.setNombre(nombre);
        r.setTipo("Equipo de cómputo");
        r.setEstado("Disponible");
        r.setClase(Recurso.Clase.EQUIPO);
        Categoria cat = new Categoria();
        cat.setIdCategoria(1L);
        r.setCategoria(cat);
        Ubicacion ubi = new Ubicacion();
        ubi.setIdUbicacion(1L);
        r.setUbicacion(ubi);
        return recursoService.guardar(r);
    }

    private String obtenerJson() throws Exception {
        return mockMvc.perform(get("/api/indicadores").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void testIndicadoresDevuelveEstructuraCompleta() throws Exception {
        mockMvc.perform(get("/api/indicadores").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRecursos", greaterThan(0)))
                .andExpect(jsonPath("$.recursosPorEstado.Disponible").exists())
                .andExpect(jsonPath("$.recursosPorEstado.Reservado").exists())
                .andExpect(jsonPath("$.recursosPorEstado.Prestado").exists())
                .andExpect(jsonPath("$.recursosPorEstado.Bloqueado").exists())
                .andExpect(jsonPath("$.reservasPorRecurso", isA(java.util.List.class)))
                .andExpect(jsonPath("$.reservasPorCategoria", isA(java.util.List.class)))
                .andExpect(jsonPath("$.devolucionesTardias").isNumber())
                .andExpect(jsonPath("$.incidentes").isNumber());
    }

    @Test
    void testIncidenteCriticoSeReflejaEnIndicadores() throws Exception {
        String antes = obtenerJson();
        int criticosAntes = JsonPath.read(antes, "$.incidentesCriticos");
        int bloqueadosAntes = JsonPath.read(antes, "$.recursosPorEstado.Bloqueado");
        int totalAntes = JsonPath.read(antes, "$.totalRecursos");

        Recurso recurso = crearRecursoPrueba("Tablet indicadores");
        IncidenteDTO dto = new IncidenteDTO();
        dto.setRecursoId(recurso.getIdRecurso());
        dto.setTipoNovedad("Daño físico");
        dto.setSeveridad("Crítica");
        dto.setDescripcion("Pantalla rota");
        incidenteService.registrarIncidente(dto);

        String despues = obtenerJson();
        assertEquals(totalAntes + 1, (int) JsonPath.read(despues, "$.totalRecursos"));
        assertEquals(criticosAntes + 1, (int) JsonPath.read(despues, "$.incidentesCriticos"));
        assertEquals(bloqueadosAntes + 1, (int) JsonPath.read(despues, "$.recursosPorEstado.Bloqueado"));
    }
}
