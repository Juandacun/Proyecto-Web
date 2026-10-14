package com.example.proyectoweb.controller;

import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ReservaControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ReservaController reservaController;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private RecursoService recursoService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(reservaController).build();
    }

    @Test
    void testListarReservasDevuelveJsonYEstado200() throws Exception {
        mockMvc.perform(get("/api/reservas")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void testBuscarPorIdExistente() throws Exception {
        // En data.sql existe al menos una reserva inicial
        Reserva primera = reservaService.listar().getFirst();

        mockMvc.perform(get("/api/reservas/" + primera.getIdReserva())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.recurso", notNullValue()))
                .andExpect(jsonPath("$.estado", notNullValue()));
    }

    @Test
    void testCrearReservaValida() throws Exception {
        // Recurso 2 es Salón 201 (ESPACIO, Disponible)
        String jsonPayload = """
                {
                    "recursoId": 2,
                    "fecha": "2026-11-20",
                    "horaInicio": "10:00",
                    "horaFin": "12:00",
                    "motivo": "Examen de programación",
                    "solicitante": "Profesor Carlos"
                }
                """;

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva", notNullValue()))
                .andExpect(jsonPath("$.horaInicio", is("10:00")))
                .andExpect(jsonPath("$.horaFin", is("12:00")))
                .andExpect(jsonPath("$.estado", is("Confirmada")));
    }

    @Test
    void testRN01SolapamientoDeHorariosRetorna409Conflict() throws Exception {
        // 1. Crear una primera reserva válida de 08:00 a 10:00 el 2026-11-25 para el recurso 2
        String jsonPrimera = """
                {
                    "recursoId": 2,
                    "fecha": "2026-11-25",
                    "horaInicio": "08:00",
                    "horaFin": "10:00",
                    "motivo": "Clase matutina",
                    "solicitante": "Prof. Ana"
                }
                """;

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPrimera))
                .andExpect(status().isCreated());

        // 2. Intentar crear una reserva solapada (09:00 a 11:00) en la misma fecha y recurso (RN-01)
        String jsonSolapada = """
                {
                    "recursoId": 2,
                    "fecha": "2026-11-25",
                    "horaInicio": "09:00",
                    "horaFin": "11:00",
                    "motivo": "Reunión de departamento",
                    "solicitante": "Prof. Pedro"
                }
                """;

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSolapada))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-01")))
                .andExpect(jsonPath("$.error", containsString("Conflicto de horario")));
    }

    @Test
    void testRN02RecursoBloqueadoNoSePuedeReservarRetorna409Conflict() throws Exception {
        // El recurso 4 en data.sql es 'Laboratorio de Química Orgánica' con estado 'Bloqueado'
        String jsonRecursoBloqueado = """
                {
                    "recursoId": 4,
                    "fecha": "2026-11-26",
                    "horaInicio": "14:00",
                    "horaFin": "16:00",
                    "motivo": "Práctica libre",
                    "solicitante": "Estudiante Laura"
                }
                """;

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRecursoBloqueado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("RN-02")))
                .andExpect(jsonPath("$.error", containsString("bloqueado")));
    }

    @Test
    void testNoPermitirReservaDeEquipoRetorna400BadRequest() throws Exception {
        // Recurso 1 es 'Vídeo beam Epson PowerLite' (EQUIPO)
        String jsonEquipo = """
                {
                    "recursoId": 1,
                    "fecha": "2026-11-27",
                    "horaInicio": "08:00",
                    "horaFin": "10:00",
                    "motivo": "Uso para exposición",
                    "solicitante": "Estudiante Mateo"
                }
                """;

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEquipo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Solo se pueden reservar espacios y salas")));
    }

    @Test
    void testCancelarReserva() throws Exception {
        Reserva primera = reservaService.listar().getFirst();

        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.estado", is("Cancelada")));
    }

    @Test
    void testFinalizarReserva() throws Exception {
        Reserva primera = reservaService.listar().getFirst();

        mockMvc.perform(patch("/api/reservas/" + primera.getIdReserva() + "/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva", is(primera.getIdReserva().intValue())))
                .andExpect(jsonPath("$.estado", is("Finalizada")));
    }

    @Test
    void testVerificarDisponibilidadEndpoint() throws Exception {
        // Recurso 2 disponible en 2026-12-01
        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "2")
                        .param("fecha", "2026-12-01")
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(true)));

        // Recurso 4 bloqueado
        mockMvc.perform(get("/api/reservas/verificar-disponibilidad")
                        .param("recursoId", "4")
                        .param("fecha", "2026-12-01")
                        .param("horaInicio", "08:00")
                        .param("horaFin", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible", is(false)))
                .andExpect(jsonPath("$.motivo", is("RN-02")));
    }
}
