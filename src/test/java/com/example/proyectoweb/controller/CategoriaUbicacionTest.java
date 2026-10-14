package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.UbicacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class CategoriaUbicacionTest {

    private MockMvc mockMvcCat;
    private MockMvc mockMvcUbi;

    @Autowired
    private CategoriaController categoriaController;

    @Autowired
    private UbicacionController ubicacionController;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    @BeforeEach
    void setUp() {
        this.mockMvcCat = MockMvcBuilders.standaloneSetup(categoriaController).build();
        this.mockMvcUbi = MockMvcBuilders.standaloneSetup(ubicacionController).build();
    }

    @Test
    void testListarCategoriasSinCiclosJson() throws Exception {
        mockMvcCat.perform(get("/api/categorias")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testCrearYActualizarCategoria() throws Exception {
        String jsonCrear = """
                {
                    "nombre": "Nueva Categoria Robotica",
                    "descripcion": "Robots y controladores"
                }
                """;

        String response = mockMvcCat.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCategoria", notNullValue()))
                .andExpect(jsonPath("$.nombre", is("Nueva Categoria Robotica")))
                .andReturn().getResponse().getContentAsString();

        // Extraer id creado
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Categoria creada = mapper.readValue(response, Categoria.class);

        String jsonActualizar = """
                {
                    "nombre": "Robotica Avanzada",
                    "descripcion": "Kits Arduino y sensores"
                }
                """;

        mockMvcCat.perform(put("/api/categorias/" + creada.getIdCategoria())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonActualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Robotica Avanzada")));
    }

    @Test
    void testListarUbicacionesSinCiclosJson() throws Exception {
        mockMvcUbi.perform(get("/api/ubicaciones")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testCrearYEliminarUbicacion() throws Exception {
        String jsonCrear = """
                {
                    "nombre": "Laboratorio D102",
                    "edificio": "Edificio D",
                    "descripcion": "Taller de electrónica"
                }
                """;

        String response = mockMvcUbi.perform(post("/api/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCrear))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUbicacion", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Ubicacion creada = mapper.readValue(response, Ubicacion.class);

        mockMvcUbi.perform(delete("/api/ubicaciones/" + creada.getIdUbicacion()))
                .andExpect(status().isNoContent());

        mockMvcUbi.perform(get("/api/ubicaciones/" + creada.getIdUbicacion()))
                .andExpect(status().isNotFound());
    }
}
