package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RecursoControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private RecursoController recursoController;

    @Autowired
    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(recursoController).build();
    }

    @Test
    void testListarRecursosDevuelveJsonYEstado200() throws Exception {
        mockMvc.perform(get("/api/recursos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testBuscarPorIdExistente() throws Exception {
        mockMvc.perform(get("/api/recursos/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRecurso", is(1)))
                .andExpect(jsonPath("$.nombre", notNullValue()))
                .andExpect(jsonPath("$.estado", notNullValue()));
    }

    @Test
    void testBuscarPorIdInexistente() throws Exception {
        mockMvc.perform(get("/api/recursos/999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testListarEquipos() throws Exception {
        mockMvc.perform(get("/api/recursos/equipos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clase", is("EQUIPO")));
    }

    @Test
    void testListarEspacios() throws Exception {
        mockMvc.perform(get("/api/recursos/espacios")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clase", is("ESPACIO")));
    }

    @Test
    void testCrearRecurso() throws Exception {
        String jsonPayload = """
                {
                    "nombre": "Cámara Sony Alpha 4K",
                    "tipo": "Equipo audiovisual",
                    "descripcion": "Cámara para transmisiones y grabaciones",
                    "caracteristicas": "Sensor full frame, lente 24-70mm",
                    "estado": "Disponible",
                    "clase": "EQUIPO",
                    "categoria": { "idCategoria": 3 },
                    "ubicacion": { "idUbicacion": 1 }
                }
                """;

        mockMvc.perform(post("/api/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRecurso", notNullValue()))
                .andExpect(jsonPath("$.nombre", is("Cámara Sony Alpha 4K")))
                .andExpect(jsonPath("$.estado", is("Disponible")))
                .andExpect(jsonPath("$.clase", is("EQUIPO")));
    }

    @Test
    void testCrearRecursoSinCategoriaRetorna400() throws Exception {
        String jsonInvalido = """
                {
                    "nombre": "Recurso Invalido",
                    "tipo": "Equipo"
                }
                """;

        mockMvc.perform(post("/api/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void testActualizarRecurso() throws Exception {
        String jsonActualizar = """
                {
                    "nombre": "Vídeo beam Epson PowerLite Modificado",
                    "tipo": "Equipo audiovisual",
                    "descripcion": "Proyector con mantenimiento reciente",
                    "caracteristicas": "Full HD, HDMI, Bluetooth",
                    "estado": "Disponible",
                    "clase": "EQUIPO"
                }
                """;

        mockMvc.perform(put("/api/recursos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonActualizar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRecurso", is(1)))
                .andExpect(jsonPath("$.nombre", is("Vídeo beam Epson PowerLite Modificado")));
    }

    @Test
    void testCambiarEstadoRecurso() throws Exception {
        String jsonEstado = """
                {
                    "estado": "Bloqueado"
                }
                """;

        mockMvc.perform(patch("/api/recursos/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEstado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRecurso", is(1)))
                .andExpect(jsonPath("$.estado", is("Bloqueado")))
                .andExpect(jsonPath("$.estadoColor", is("rojo")));
    }

    @Test
    void testEliminarRecurso() throws Exception {
        Recurso auxiliar = new Recurso();
        auxiliar.setNombre("Recurso a eliminar");
        auxiliar.setTipo("Prueba");
        auxiliar.setEstado("Disponible");
        auxiliar.setClase(Recurso.Clase.EQUIPO);
        Categoria cat = new Categoria();
        cat.setIdCategoria(1L);
        auxiliar.setCategoria(cat);
        Ubicacion ubi = new Ubicacion();
        ubi.setIdUbicacion(1L);
        auxiliar.setUbicacion(ubi);
        Recurso guardado = recursoService.guardar(auxiliar);

        mockMvc.perform(delete("/api/recursos/" + guardado.getIdRecurso()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recursos/" + guardado.getIdRecurso()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testFiltrarPorCategoriaUbicacionYTipoCombinados() throws Exception {
        Recurso r = new Recurso();
        r.setNombre("Proyector filtro combinado");
        r.setTipo("Proyector de prueba");
        r.setEstado("Disponible");
        r.setClase(Recurso.Clase.EQUIPO);
        Categoria cat = new Categoria();
        cat.setIdCategoria(2L);
        r.setCategoria(cat);
        Ubicacion ubi = new Ubicacion();
        ubi.setIdUbicacion(3L);
        r.setUbicacion(ubi);
        recursoService.guardar(r);

        mockMvc.perform(get("/api/recursos")
                        .param("categoriaId", "2")
                        .param("ubicacionId", "3")
                        .param("tipo", "proyector de prueba")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Proyector filtro combinado")));
    }

    @Test
    void testFiltrarPorCategoriaDevuelveSoloEsaCategoria() throws Exception {
        mockMvc.perform(get("/api/recursos")
                        .param("categoriaId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].categoria.idCategoria", everyItem(is(1))));
    }

    @Test
    void testFiltrarPorClaseYEstadoCombinados() throws Exception {
        mockMvc.perform(get("/api/recursos")
                        .param("clase", "ESPACIO")
                        .param("estado", "Disponible")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].clase", everyItem(is("ESPACIO"))))
                .andExpect(jsonPath("$[*].estado", everyItem(is("Disponible"))));
    }
}
