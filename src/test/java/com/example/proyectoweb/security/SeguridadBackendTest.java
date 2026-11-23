package com.example.proyectoweb.security;

import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.IncidenteRepository;
import com.example.proyectoweb.repository.PrestamoRepository;
import com.example.proyectoweb.repository.RecursoRepository;
import com.example.proyectoweb.repository.ReservaRepository;
import com.example.proyectoweb.repository.UsuarioRepository;
import com.example.proyectoweb.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static com.example.proyectoweb.security.TokenJwtConfig.SECRET_KEY;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class SeguridadBackendTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private IncidenteRepository incidenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        usuarioService.inicializarUsuariosSiEstaVacio();
    }

    private String obtenerToken(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("01. Login exitoso con credenciales correctas devuelve 200, JWT y cabecera Authorization")
    void test01_loginExitosoConCredencialesCorrectas() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.rol", is("ROLE_ADMIN")))
                .andExpect(jsonPath("$.expiresIn", notNullValue()));
    }

    @Test
    @DisplayName("02. Login rechazado con contraseña incorrecta devuelve 401 Unauthorized")
    void test02_loginRechazadoConPasswordIncorrecto() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"password_erroneo\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("incorrectos")));
    }

    @Test
    @DisplayName("03. Acceso rechazado con 401 cuando no se envía token a un endpoint protegido")
    void test03_accesoRechazadoSinToken() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("No autorizado")));
    }

    @Test
    @DisplayName("04. Acceso rechazado con 401 cuando el JWT es manipulado o inválido")
    void test04_accesoRechazadoConJwtInvalido() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tokenFalsoInvalido.firmaErronea"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Token")));
    }

    @Test
    @DisplayName("05. Requisito 9: Un token sigue las restricciones de expiración (JWT vencido retorna 401)")
    void test05_tokenSigueRestriccionesDeExpiracion() throws Exception {
        Claims claims = Jwts.claims()
                .subject("admin")
                .add("authorities", List.of("ROLE_ADMIN"))
                .build();

        String tokenExpirado = Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(SECRET_KEY)
                .compact();

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Token")));
    }

    @Test
    @DisplayName("06. Requisito 1: Un usuario no puede modificar el rol de otro (403 Forbidden)")
    void test06_usuarioNoPuedeModificarRolDeOtro() throws Exception {
        String tokenUser = obtenerToken("user", "user123");
        String body = "{\"rol\":\"ROLE_ADMIN\"}";

        mockMvc.perform(patch("/api/usuarios/1/rol")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));
    }

    @Test
    @DisplayName("07. Requisito 2: Un usuario no puede crear un administrador (403 Forbidden)")
    void test07_usuarioNoPuedeCrearAdministrador() throws Exception {
        String tokenUser = obtenerToken("user", "user123");
        String body = """
                {
                    "nombre": "Nuevo Admin Ilegítimo",
                    "tipo": "Administrativo",
                    "codigo": "ADM-9999",
                    "documento": "99999999",
                    "email": "hacker@campus.edu.co",
                    "username": "hacker_admin",
                    "password": "password123",
                    "rol": "ROLE_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));
    }

    @Test
    @DisplayName("08. Requisito 3: Un usuario no puede manipular la identidad del solicitante al crear reserva")
    void test08_usuarioNoPuedeModificarIdentidadSolicitanteAlCrearReserva() throws Exception {
        Recurso espacio = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.ESPACIO && "Disponible".equalsIgnoreCase(r.getEstado()))
                .findFirst()
                .orElse(null);
        assertNotNull(espacio, "Debe existir al menos un espacio disponible para la prueba");

        String tokenUser = obtenerToken("user", "user123");
        LocalDate fechaPrueba = LocalDate.now().plusDays(25);

        String bodyReserva = String.format("""
                {
                    "recursoId": %d,
                    "fecha": "%s",
                    "horaInicio": "14:00",
                    "horaFin": "16:00",
                    "motivo": "Reserva de prueba",
                    "solicitante": "admin_suplantado"
                }
                """, espacio.getIdRecurso(), fechaPrueba.toString());

        MvcResult result = mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyReserva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitante", is("user")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("user", json.get("solicitante").asText());
    }

    @Test
    @DisplayName("09. Requisito 4: Un usuario no puede consultar préstamos ajenos (403 Forbidden)")
    void test09_usuarioNoPuedeConsultarPrestamosAjenos() throws Exception {
        Recurso equipo = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElse(null);
        assertNotNull(equipo, "Debe existir un equipo para registrar el préstamo");

        Prestamo prestamoAjeno = new Prestamo(
                equipo,
                "DOC-AJENO-777",
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                "Bueno",
                "Préstamo asignado a otra persona"
        );
        prestamoAjeno = prestamoRepository.save(prestamoAjeno);

        String tokenUser = obtenerToken("user", "user123");

        mockMvc.perform(get("/api/prestamos/" + prestamoAjeno.getIdPrestamo())
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("solo puedes consultar tus propios préstamos")));
    }

    @Test
    @DisplayName("10. Requisito 5: Un usuario no puede acceder al módulo de mantenimiento (403 Forbidden)")
    void test10_usuarioNoPuedeAccederModuloMantenimiento() throws Exception {
        String tokenUser = obtenerToken("user", "user123");

        mockMvc.perform(get("/api/mantenimientos")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));
    }

    @Test
    @DisplayName("11. Requisito 6: Un gestor no puede administrar usuarios ni roles (403 Forbidden)")
    void test11_gestorNoPuedeAdministrarUsuariosNiRoles() throws Exception {
        String tokenGestor = obtenerToken("gestor", "gestor123");

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));

        mockMvc.perform(patch("/api/usuarios/1/rol")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));
    }

    @Test
    @DisplayName("12. Requisito 7: Un gestor puede gestionar mantenimientos (200 OK)")
    void test12_gestorPuedeGestionarMantenimientos() throws Exception {
        String tokenGestor = obtenerToken("gestor", "gestor123");

        mockMvc.perform(get("/api/mantenimientos")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("13. Requisito 8: Un administrador puede acceder a las operaciones administrativas (200 OK)")
    void test13_adminPuedeAccederOperacionesAdministrativas() throws Exception {
        String tokenAdmin = obtenerToken("admin", "admin123");

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("14. Requisito 10: Un usuario cuyo rol ha sido modificado en DB no conserva privilegios indebidamente")
    void test14_usuarioCuyoRolHaSidoModificadoNoConservaPrivilegios() throws Exception {
        Usuario adminTemp = new Usuario(
                "Administrador Temporal", "Administrativo", "ADM-TEMP-01",
                "88812345", "admin.temp@campus.edu.co", "admintemp",
                passwordEncoder.encode("admin123"), "ROLE_ADMIN"
        );
        adminTemp = usuarioRepository.save(adminTemp);

        String tokenOriginal = obtenerToken("admintemp", "admin123");

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenOriginal))
                .andExpect(status().isOk());

        usuarioService.cambiarRol(adminTemp.getIdUsuario(), "ROLE_USER");

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenOriginal))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Acceso denegado")));
    }

    @Test
    @DisplayName("15. Usuario solicitante no puede modificar la reserva de otro usuario (403 Forbidden)")
    void test15_usuarioNoPuedeModificarReservaDeOtroUsuario() throws Exception {
        Recurso espacio = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.ESPACIO)
                .findFirst()
                .orElse(null);
        assertNotNull(espacio, "Debe existir al menos un espacio para la prueba");

        LocalDate fechaPrueba = LocalDate.now().plusDays(30);
        Reserva reservaAjena = new Reserva(
                espacio,
                fechaPrueba,
                "10:00",
                "12:00",
                "Reunión de otro usuario",
                "Confirmada",
                "otro_usuario"
        );
        reservaAjena = reservaRepository.save(reservaAjena);

        String tokenUser = obtenerToken("user", "user123");

        String bodyModificacion = String.format("""
                {
                    "recursoId": %d,
                    "fecha": "%s",
                    "horaInicio": "10:00",
                    "horaFin": "12:00",
                    "motivo": "Intento de modificación no autorizada"
                }
                """, espacio.getIdRecurso(), fechaPrueba.toString());

        mockMvc.perform(put("/api/reservas/" + reservaAjena.getIdReserva())
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyModificacion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("no puedes modificar reservas pertenecientes a otros usuarios")));
    }

    @Test
    @DisplayName("16. Batch: No se crean usuarios de prueba cuando el perfil es default o no explícito")
    void test16_noSeCreanUsuariosDePruebaConPerfilDefault() {
        try {
            // Configurar entorno simulado sin perfiles activos (default)
            MockEnvironment envDefault = new MockEnvironment();
            usuarioService.setEnvironment(envDefault);

            long totalAntes = usuarioRepository.count();
            usuarioService.inicializarUsuariosSiEstaVacio();
            long totalDespues = usuarioRepository.count();

            // Debe salir inmediatamente sin insertar ningún nuevo usuario
            assertEquals(totalAntes, totalDespues);
        } finally {
            usuarioService.setEnvironment(environment);
        }
    }

    @Test
    @DisplayName("17. Batch: Sí se crean usuarios de prueba cuando el perfil explícito es dev")
    void test17_siSeCreanUsuariosDePruebaConPerfilDev() {
        try {
            MockEnvironment envDev = new MockEnvironment();
            envDev.setActiveProfiles("dev");
            usuarioService.setEnvironment(envDev);

            usuarioService.inicializarUsuariosSiEstaVacio();

            assertTrue(usuarioRepository.findByLoginIdentifier("user").isPresent());
            assertTrue(usuarioRepository.findByLoginIdentifier("gestor").isPresent());
            assertTrue(usuarioRepository.findByLoginIdentifier("admin").isPresent());
        } finally {
            usuarioService.setEnvironment(environment);
        }
    }

    @Test
    @DisplayName("18. Batch: No se sobrescriben contraseñas ni roles de usuarios existentes")
    void test18_noSeSobrescribenContrasenasNiRolesExistentes() {
        try {
            MockEnvironment envDev = new MockEnvironment();
            envDev.setActiveProfiles("dev");
            usuarioService.setEnvironment(envDev);

            // Crear un usuario con contraseña personalizada y rol específico
            String rawPasswordPersonalizada = "mi_password_segura_999";
            String hashOriginal = passwordEncoder.encode(rawPasswordPersonalizada);

            Usuario usuarioExistente = new Usuario(
                    "Usuario Fijo Test", "Docente", "DOC-99988",
                    "11223344", "fijo@campus.edu.co", "usuario_fijo",
                    hashOriginal, "ROLE_ADMIN"
            );
            usuarioRepository.save(usuarioExistente);

            // Ejecutar inicialización batch
            usuarioService.inicializarUsuariosSiEstaVacio();

            Usuario verificado = usuarioRepository.findByUsername("usuario_fijo").orElseThrow();
            assertEquals(hashOriginal, verificado.getPassword(), "La contraseña no debe ser sobrescrita");
            assertEquals("ROLE_ADMIN", verificado.getRol(), "El rol existente no debe ser modificado");
        } finally {
            usuarioService.setEnvironment(environment);
        }
    }

    @Test
    @DisplayName("19. Incidentes: Usuario solicitante no puede suplantar al reportante en POST /api/incidentes")
    void test19_usuarioNoPuedeSuplantarAlReportanteEnviandoOtroNombre() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        String tokenUser = obtenerToken("user", "user123");

        // El usuario envía en el JSON que el incidente fue reportado por "rector_suplantado"
        String bodyIncidente = String.format("""
                {
                    "recursoId": %d,
                    "tipoNovedad": "Falla de funcionamiento",
                    "severidad": "Media",
                    "descripcion": "El equipo no enciende",
                    "reportadoPor": "rector_suplantado",
                    "usuarioId": "rector_suplantado"
                }
                """, recurso.getIdRecurso());

        MvcResult result = mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyIncidente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportadoPor", is("user"))) // El sistema impone estrictamente el actor autenticado
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("user", json.get("reportadoPor").asText());

        // Verificar persistencia en base de datos
        Long idIncidente = json.get("idIncidente").asLong();
        Incidente guardado = incidenteRepository.findById(idIncidente).orElseThrow();
        assertEquals("user", guardado.getReportadoPor());
    }

    @Test
    @DisplayName("20. Incidentes: ROLE_USER consulta incidentes compartidos sin ver identidad de otros reportantes")
    void test20_usuarioPuedeConsultarIncidentesCompartidosSinVisualizarIdentidadOtrosReportantes() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        // 1. Incidente reportado por otra persona ("docente_ajeno")
        Incidente incidenteAjeno = new Incidente(recurso, "Daño físico", "Leve", "Tapa suelta", LocalDateTime.now());
        incidenteAjeno.setReportadoPor("docente_ajeno");
        incidenteAjeno = incidenteRepository.save(incidenteAjeno);

        // 2. Incidente reportado por el usuario autenticado ("user")
        Incidente incidentePropio = new Incidente(recurso, "Falla eléctrica", "Media", "Cable pelado", LocalDateTime.now());
        incidentePropio.setReportadoPor("user");
        incidentePropio = incidenteRepository.save(incidentePropio);

        String tokenUser = obtenerToken("user", "user123");

        // 3. Consulta global GET /api/incidentes con ROLE_USER
        MvcResult resultGlobal = mockMvc.perform(get("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arrayJson = objectMapper.readTree(resultGlobal.getResponse().getContentAsString());
        assertTrue(arrayJson.isArray());

        // Para el incidente propio, sí se identifica como reportante ("user")
        // Para el incidente ajeno, la identidad del reportante se anonimiza (null)
        for (JsonNode nodo : arrayJson) {
            long id = nodo.get("idIncidente").asLong();
            if (id == incidentePropio.getIdIncidente()) {
                assertEquals("user", nodo.get("reportadoPor").asText());
            } else if (id == incidenteAjeno.getIdIncidente()) {
                assertTrue(nodo.get("reportadoPor").isNull(), "La identidad de otros reportantes debe estar anonimizada");
            }
        }

        // 4. Consulta puntual GET /api/incidentes/{idAjeno} con ROLE_USER
        mockMvc.perform(get("/api/incidentes/" + incidenteAjeno.getIdIncidente())
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPor").value(nullValue()));

        // 5. Consulta puntual GET /api/incidentes/{idPropio} con ROLE_USER
        mockMvc.perform(get("/api/incidentes/" + incidentePropio.getIdIncidente())
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPor", is("user")));
    }

    @Test
    @DisplayName("21. Incidentes: ROLE_GESTOR puede consultar la información completa con todas las identidades")
    void test21_gestorPuedeConsultarInformacionCompletaDeIncidentes() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        Incidente incidenteAjeno = new Incidente(recurso, "Desgaste", "Leve", "Botón desgastado", LocalDateTime.now());
        incidenteAjeno.setReportadoPor("profesor_gonzalez");
        incidenteAjeno = incidenteRepository.save(incidenteAjeno);

        String tokenGestor = obtenerToken("gestor", "gestor123");

        // Consulta puntual por ID
        mockMvc.perform(get("/api/incidentes/" + incidenteAjeno.getIdIncidente())
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPor", is("profesor_gonzalez")));

        // Consulta listado
        mockMvc.perform(get("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idIncidente == " + incidenteAjeno.getIdIncidente() + ")].reportadoPor")
                        .value(hasItem("profesor_gonzalez")));
    }

    @Test
    @DisplayName("22. Incidentes: Consulta GET por ROLE_USER no altera el reportadoPor original en base de datos")
    void test22_consultaGetNoAlteraReportadoPorOriginalEnBaseDeDatos() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        // 1. Crear un incidente reportado por "docente_ajeno"
        Incidente incidente = new Incidente(recurso, "Daño físico", "Leve", "Fisura en carcasa", LocalDateTime.now());
        incidente.setReportadoPor("docente_ajeno");
        incidente = incidenteRepository.save(incidente);
        Long idIncidente = incidente.getIdIncidente();

        // 2. Autenticar como ROLE_USER ("user")
        String tokenUser = obtenerToken("user", "user123");

        // 3. Ejecutar GET /api/incidentes y GET /api/incidentes/{id}
        mockMvc.perform(get("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidentes/" + idIncidente)
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPor").value(nullValue()));

        // 4. Consultar directamente el incidente en la base de datos mediante IncidenteRepository
        Incidente enBd = incidenteRepository.findById(idIncidente).orElseThrow();

        // 5. Comprobar que el valor original "docente_ajeno" continúa almacenado en la base de datos
        assertEquals("docente_ajeno", enBd.getReportadoPor(),
                "La consulta GET no debe mutar ni eliminar el reportadoPor persistido en base de datos");
    }

    @Test
    @DisplayName("23. Incidentes: Gestor reportando por un tercero conserva trazabilidad del actor real")
    void test23_gestorReportandoPorTerceroConservaTrazabilidadActorReal() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        // 1. Autenticar a un usuario con ROLE_GESTOR ("gestor")
        String tokenGestor = obtenerToken("gestor", "gestor123");

        // 2. Ejecutar POST /api/incidentes enviando un tercero como reportadoPor ("Profesor Gómez")
        String bodyIncidente = String.format("""
                {
                    "recursoId": %d,
                    "tipoNovedad": "Falla de funcionamiento",
                    "severidad": "Leve",
                    "descripcion": "El proyector parpadea intermitentemente",
                    "reportadoPor": "Profesor Gómez",
                    "usuarioId": "Profesor Gómez"
                }
                """, recurso.getIdRecurso());

        MvcResult result = mockMvc.perform(post("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenGestor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyIncidente))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportadoPor", is("Profesor Gómez (registrado por gestor)")))
                .andReturn();

        // 3. Comprobar que el registro persistido conserva el formato con trazabilidad
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long idIncidente = json.get("idIncidente").asLong();

        Incidente persistido = incidenteRepository.findById(idIncidente).orElseThrow();
        assertEquals("Profesor Gómez (registrado por gestor)", persistido.getReportadoPor());
        assertTrue(persistido.getReportadoPor().contains("gestor"), "Debe identificar al gestor como actor real");
    }

    @Test
    @DisplayName("24. Incidentes: ROLE_ADMIN puede consultar la identidad completa de los reportantes")
    void test24_adminPuedeConsultarIdentidadCompletaDeReportantes() throws Exception {
        Recurso recurso = recursoRepository.findAll().stream()
                .filter(r -> r.getClase() == Recurso.Clase.EQUIPO)
                .findFirst()
                .orElseThrow();

        // 1. Crear un incidente con reportante específico
        Incidente incidente = new Incidente(recurso, "Accesorio faltante", "Leve", "Falta cable HDMI", LocalDateTime.now());
        incidente.setReportadoPor("coordinador_sistemas");
        incidente = incidenteRepository.save(incidente);

        // 2. Autenticar como ROLE_ADMIN ("admin")
        String tokenAdmin = obtenerToken("admin", "admin123");

        // 3. Consultar GET /api/incidentes y verificar que visualiza la identidad completa
        mockMvc.perform(get("/api/incidentes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idIncidente == " + incidente.getIdIncidente() + ")].reportadoPor")
                        .value(hasItem("coordinador_sistemas")));

        // 4. Consultar GET /api/incidentes/{id} y verificar detalle con identidad completa
        mockMvc.perform(get("/api/incidentes/" + incidente.getIdIncidente())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportadoPor", is("coordinador_sistemas")));
    }
}