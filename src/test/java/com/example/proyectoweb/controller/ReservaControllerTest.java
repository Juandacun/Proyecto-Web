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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
        System.out.println("[TEST SETUP] Fecha futura de prueba: " + futura);
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
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m = Pattern.compile("\"idReserva\"\\s*:\\s*(\\d+)").matcher(res.getResponse().getContentAsString());
        if (!m.find()) throw new IllegalStateException("La respuesta no trae idReserva");
        long id = Long.parseLong(m.group(1));
        System.out.println("[OUTPUT] Creada reserva id=" + id + " para recurso=" + recursoId
                + " en " + fecha + " (" + inicio + " - " + fin + ")");
        return id;
    }

    @Test
    void testListarReservasDevuelveJsonYEstado200() throws Exception {
        System.out.println("=== TEST: Listar reservas (200) ===");
        mockMvc.perform(get("/api/reservas").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testListarConFiltrosCombinados() throws Exception {
        System.out.println("=== TEST: Listar reservas con filtros combinados ===");
        crear(2, futura, "07:00", "08:00");
        mockMvc.perform(get("/api/reservas")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("estado", "Confirmada"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].horaInicio", is("07:00")))
                .andExpect(jsonPath("$[0].editable", is(true)));
    }

    @Test
    void testBuscarPorIdExistente() throws Exception {
        System.out.println("=== TEST: Buscar reserva por ID existente ===");
        Reserva primera = reservaService.listar().getFirst();
        System.out.println("[OUTPUT] Buscando reserva id=" + primera.getIdReserva());
        mockMvc.perform(get("/api/reservas/" + primera.getIdReserva()).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.recurso", notNullValue()))
                .andExpect(jsonPath("$.estado", notNullValue()));
    }

    @Test
    void testBuscarPorIdInexistenteRetorna404() throws Exception {
        System.out.println("=== TEST: Buscar reserva inexistente retorna 404 ===");
        mockMvc.perform(get("/api/reservas/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testCrearReservaValida() throws Exception {
        System.out.println("=== TEST: Crear reserva válida (201) ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "10:00", "12:00", "Examen de programación")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva", notNullValue()))
                .andExpect(jsonPath("$.horaInicio", is("10:00")))
                .andExpect(jsonPath("$.horaFin", is("12:00")))
                .andExpect(jsonPath("$.estado", is("Confirmada")));
    }

    @Test
    void testCrearNormalizaHoraSinCeroInicial() throws Exception {
        System.out.println("=== TEST: Normaliza hora sin cero inicial (7:00 -> 07:00) ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "7:00", "8:30", "Hora corta")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio", is("07:00")))
                .andExpect(jsonPath("$.horaFin", is("08:30")));
    }

    @Test
    void testRN01SolapamientoDeHorariosRetorna409Conflict() throws Exception {
        System.out.println("=== TEST: RN-01 Solapamiento de horarios retorna 409 ===");
        crear(2, futura, "08:00", "10:00");

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "09:00", "11:00", "Reunión de departamento")))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")))
                .andExpect(jsonPath("$.error", containsString("Conflicto de horario")));
    }

    @Test
    void testRN01ReservaQueContieneAOtraRetorna409() throws Exception {
        System.out.println("=== TEST: RN-01 Reserva que contiene a otra retorna 409 ===");
        crear(2, futura, "09:00", "10:00");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "08:00", "12:00", "Contiene a la otra")))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")));
    }

    @Test
    void testRN01HorariosContiguosNoSonConflicto() throws Exception {
        System.out.println("=== TEST: RN-01 Horarios contiguos (08:00-10:00 y 10:00-12:00) ===");
        crear(2, futura, "08:00", "10:00");
        crear(2, futura, "10:00", "12:00");
    }

    @Test
    void testRN01ReservaCanceladaLiberaElHorario() throws Exception {
        System.out.println("=== TEST: RN-01 Reserva cancelada libera el horario ===");
        long id = crear(2, futura, "14:00", "16:00");
        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar"))
                .andDo(print())
                .andExpect(status().isOk());
        System.out.println("[OUTPUT] Reserva " + id + " cancelada; reintentando crear en el mismo horario");
        crear(2, futura, "14:00", "16:00");
    }

    @Test
    void testRN02RecursoBloqueadoNoSePuedeReservarRetorna409Conflict() throws Exception {
        System.out.println("=== TEST: RN-02 Recurso bloqueado retorna 409 ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(4, futura, "14:00", "16:00", "Práctica libre")))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-02")))
                .andExpect(jsonPath("$.error", containsString("bloqueado")));
    }

    @Test
    void testNoPermitirReservaDeEquipoRetorna400BadRequest() throws Exception {
        System.out.println("=== TEST: No permitir reserva de equipo retorna 400 ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(1, futura, "08:00", "10:00", "Uso para exposición")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Solo se pueden reservar espacios y salas")));
    }

    @Test
    void testNoPermitirReservaEnFechaPasadaRetorna400() throws Exception {
        String ayer = LocalDate.now().minusDays(1).toString();
        System.out.println("=== TEST: No permitir reserva en fecha pasada (" + ayer + ") retorna 400 ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, ayer, "08:00", "10:00", "Ayer")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("ya pasó")));
    }

    @Test
    void testHoraInicioPosteriorAFinRetorna400() throws Exception {
        System.out.println("=== TEST: Hora inicio posterior a hora fin retorna 400 ===");
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "12:00", "10:00", "Al revés")))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testModificarReservaAntesDeIniciar() throws Exception {
        System.out.println("=== TEST: Modificar reserva antes de iniciar ===");
        long id = crear(2, futura, "08:00", "09:00");
        mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "08:00", "09:30", "Se extiende")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horaFin", is("09:30")))
                .andExpect(jsonPath("$.motivo", is("Se extiende")));
    }

    @Test
    void testModificarHaciaHorarioOcupadoRetorna409() throws Exception {
        System.out.println("=== TEST: Modificar reserva hacia horario ocupado retorna 409 ===");
        crear(2, futura, "08:00", "10:00");
        long id = crear(2, futura, "11:00", "12:00");
        mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(2, futura, "09:00", "12:00", "Choca")))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")));
    }

    @Test
    void testModificarReservaQueYaInicioRetorna409() throws Exception {
        System.out.println("=== TEST: Modificar reserva que ya inició retorna 409 ===");
        Reserva pasada = reservaService.listar().getFirst();
        System.out.println("[OUTPUT] Probando modificar reserva inicial id=" + pasada.getIdReserva());
        mockMvc.perform(put("/api/reservas/" + pasada.getIdReserva())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(pasada.getRecurso().getIdRecurso(), futura, "08:00", "09:00", "Mover")))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ESTADO_RESERVA")));
    }

    @Test
    void testCancelarReserva() throws Exception {
        System.out.println("=== TEST: Cancelar reserva y rechazar segunda cancelación ===");
        long id = crear(2, futura, "16:00", "18:00");
        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is((int) id)))
                .andExpect(jsonPath("$.estado", is("Cancelada")));

        mockMvc.perform(patch("/api/reservas/" + id + "/cancelar"))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    void testCancelarReservaQueYaInicioRetorna409() throws Exception {
        System.out.println("=== TEST: Cancelar reserva que ya inició retorna 409 ===");
        Reserva pasada = reservaService.listar().getFirst();
        mockMvc.perform(patch("/api/reservas/" + pasada.getIdReserva() + "/cancelar"))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("ya inició")));
    }

    @Test
    void testFinalizarReserva() throws Exception {
        System.out.println("=== TEST: Finalizar reserva y rechazar segunda finalización ===");
        Reserva primera = reservaService.listar().getFirst();
        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/finalizar"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.estado", is("Finalizada")));

        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/finalizar"))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    void testVerificarDisponibilidadEndpoint() throws Exception {
        System.out.println("=== TEST: Verificar disponibilidad (recurso 2 libre vs recurso 4 bloqueado) ===");
        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(true)));

        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "4")
                        .param("fecha", futura)
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)))
                .andExpect(jsonPath("$.motivo", is("RN-02")));
    }

    @Test
    void testVerificarDisponibilidadDetectaSolapamiento() throws Exception {
        System.out.println("=== TEST: Verificar disponibilidad detecta solapamiento y respeta reservaIdExcluir ===");
        long id = crear(2, futura, "08:00", "10:00");

        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", futura)
                        .param("horaInicio", "09:00")
                        .param("horaFin", "11:00"))
                .andDo(print())
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
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(true)));
    }
}