package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ReservaControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private ReservaController reservaController;

    @Autowired
    private ReservaService reservaService;

    private final String futura = LocalDate.now().plusDays(30).toString();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(reservaController).build();
    }

    private String json(long recursoId, String fecha, String inicio, String fin, String motivo) {
        return """
                {
                    "recursoId": %d,
                    "fecha": "%s",
                    "horaInicio": "%s",
                    "horaFin": "%s",
                    "motivo": "%s",
                    "solicitante": "Prueba"
                }
                """.formatted(recursoId, fecha, inicio, fin, motivo);
    }

    private long crear(long recursoId, String fecha, String inicio, String fin) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recursoId, fecha, inicio, fin, "Prueba")))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m = Pattern.compile("\"idReserva\"\\s*:\\s*(\\d+)").matcher(res.getResponse().getContentAsString());
        if (!m.find()) throw new IllegalStateException("La respuesta no trae idReserva");
        return Long.parseLong(m.group(1));
    }

    @Test
    void testListarReservasDevuelveJsonYEstado200() throws Exception {
        mockMvc.perform(get("/api/reservas").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testListarConFiltrosCombinados() throws Exception {
        crear(2, futura, "07:00", "08:00");
        mockMvc.perform(get("/api/reservas")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("estado", "Confirmada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].horaInicio", is("07:00")))
                .andExpect(jsonPath("$[0].editable", is(true)));
    }

    @Test
    void testBuscarPorIdExistente() throws Exception {
        // En data.sql existe al menos una reserva inicial
        Reserva primera = reservaService.listar().getFirst();
        mockMvc.perform(get("/api/reservas/" + primera.getIdReserva()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.recurso", notNullValue()))
                .andExpect(jsonPath("$.estado", notNullValue()));
    }

    @Test
    void testBuscarPorIdInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/reservas/999999")).andExpect(status().isNotFound());
    }

    @Test
    void testCrearReservaValida() throws Exception {
        // Recurso 2 es Salón 201 (ESPACIO, Disponible)
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "10:00", "12:00", "Examen de programación")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva", notNullValue()))
                .andExpect(jsonPath("$.horaInicio", is("10:00")))
                .andExpect(jsonPath("$.horaFin", is("12:00")))
                .andExpect(jsonPath("$.estado", is("Confirmada")));
    }

    @Test
    void testCrearNormalizaHoraSinCeroInicial() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "7:00", "8:30", "Hora corta")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio", is("07:00")))
                .andExpect(jsonPath("$.horaFin", is("08:30")));
    }

    @Test
    void testRN01SolapamientoDeHorariosRetorna409Conflict() throws Exception {
        // 1. Crear una primera reserva válida de 08:00 a 10:00 el 2026-11-25 para el recurso 2
        crear(2, futura, "08:00", "10:00");

        // 2. Intentar crear una reserva solapada (09:00 a 11:00) en la misma fecha y recurso (RN-01)
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "09:00", "11:00", "Reunión de departamento")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")))
                .andExpect(jsonPath("$.error", containsString("Conflicto de horario")));
    }

    @Test
    void testRN01ReservaQueContieneAOtraRetorna409() throws Exception {
        crear(2, futura, "09:00", "10:00");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "08:00", "12:00", "Contiene a la otra")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")));
    }

    @Test
    void testRN01HorariosContiguosNoSonConflicto() throws Exception {
        crear(2, futura, "08:00", "10:00");
        crear(2, futura, "10:00", "12:00");
    }

    @Test
    void testRN01ReservaCanceladaLiberaElHorario() throws Exception {
        long id = crear(2, futura, "14:00", "16:00");
        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar")).andExpect(status().isOk());
        crear(2, futura, "14:00", "16:00");
    }

    @Test
    void testRN02RecursoBloqueadoNoSePuedeReservarRetorna409Conflict() throws Exception {
        // El recurso 4 en data.sql es 'Laboratorio de Química Orgánica' con estado 'Bloqueado'
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(4, futura, "14:00", "16:00", "Práctica libre")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-02")))
                .andExpect(jsonPath("$.error", containsString("bloqueado")));
    }

    @Test
    void testNoPermitirReservaDeEquipoRetorna400BadRequest() throws Exception {
        // Recurso 1 es 'Vídeo beam Epson PowerLite' (EQUIPO)
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(1, futura, "08:00", "10:00", "Uso para exposición")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Solo se pueden reservar espacios y salas")));
    }

    @Test
    void testNoPermitirReservaEnFechaPasadaRetorna400() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, LocalDate.now().minusDays(1).toString(), "08:00", "10:00", "Ayer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("ya pasó")));
    }

    @Test
    void testHoraInicioPosteriorAFinRetorna400() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "12:00", "10:00", "Al revés")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testModificarReservaAntesDeIniciar() throws Exception {
        long id = crear(2, futura, "08:00", "09:00");
        mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "08:00", "09:30", "Se extiende")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaFin", is("09:30")))
                .andExpect(jsonPath("$.motivo", is("Se extiende")));
    }

    @Test
    void testModificarHaciaHorarioOcupadoRetorna409() throws Exception {
        crear(2, futura, "08:00", "10:00");
        long id = crear(2, futura, "11:00", "12:00");
        mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "09:00", "12:00", "Choca")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")));
    }

    @Test
    void testModificarReservaQueYaInicioRetorna409() throws Exception {
        Reserva pasada = reservaService.listar().getFirst();
        mockMvc.perform(put("/api/reservas/" + pasada.getIdReserva())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pasada.getRecurso().getIdRecurso(), futura, "08:00", "09:00", "Mover")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ESTADO_RESERVA")));
    }

    @Test
    void testCancelarReserva() throws Exception {
        long id = crear(2, futura, "16:00", "18:00");
        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is((int) id)))
                .andExpect(jsonPath("$.estado", is("Cancelada")));

        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    void testCancelarReservaQueYaInicioRetorna409() throws Exception {
        Reserva pasada = reservaService.listar().getFirst();
        mockMvc.perform(patch("/api/reservas/" + pasada.getIdReserva() + "/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("ya inició")));
    }

    @Test
    void testFinalizarReserva() throws Exception {
        Reserva primera = reservaService.listar().getFirst();
        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.estado", is("Finalizada")));

        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/finalizar"))
                .andExpect(status().isConflict());
    }

    @Test
    void testVerificarDisponibilidadEndpoint() throws Exception {
        // Recurso 2 disponible en 2026-12-01
        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(true)));

        // Recurso 4 bloqueado
        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "4")
                        .param("fecha", futura)
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)))
                .andExpect(jsonPath("$.motivo", is("RN-02")));
    }

    @Test
    void testVerificarDisponibilidadDetectaSolapamiento() throws Exception {
        long id = crear(2, futura, "08:00", "10:00");

        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("horaInicio", "09:00")
                        .param("horaFin", "11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)))
                .andExpect(jsonPath("$.motivo", is("RN-01")))
                .andExpect(jsonPath("$.conflictoInicio", is("08:00")));

        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("horaInicio", "09:00")
                        .param("horaFin", "11:00")
                        .param("reservaIdExcluir", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(true)));
    }
}
