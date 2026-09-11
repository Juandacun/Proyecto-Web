package com.example.proyectoweb.config;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Incidente;
import com.example.proyectoweb.model.Prestamo;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Reserva;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.IncidenteService;
import com.example.proyectoweb.service.PrestamoService;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.ReservaService;
import com.example.proyectoweb.service.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    @Autowired
    private RecursoService recursoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private IncidenteService incidenteService;

    @Override
    public void run(String... args) {
        if (!categoriaService.listar().isEmpty()) {
            return;
        }

        Categoria aulas = categoria("Aulas y salones", "Salones de clase y salas de estudio del campus");
        Categoria auditorios = categoria("Auditorios", "Auditorios y aulas máximas para eventos");
        Categoria audiovisual = categoria("Equipos audiovisuales", "Proyectores, pantallas y equipos de sonido");
        Categoria laboratorios = categoria("Laboratorios", "Laboratorios de docencia e investigación");
        Categoria deportivos = categoria("Espacios deportivos", "Canchas y espacios para la actividad física");
        Categoria computo = categoria("Equipos de cómputo", "Computadores y equipos para salas de sistemas");

        Ubicacion biblioteca = ubicacion("Biblioteca General Alfonso Borrero Cabal", "Edificio 25",
                "Biblioteca central del campus");
        Ubicacion auditorio = ubicacion("Auditorio Jorge Hoyos Vásquez", "Edificio 17",
                "Auditorio principal o aula máxima");
        Ubicacion ingenieria = ubicacion("Edificio de Ingeniería José Gabriel Maldonado", "Campus Central",
                "Sede de la Facultad de Ingeniería");
        Ubicacion ciencias = ubicacion("Facultad de Ciencias Básicas", "Edificio Gabriel Giraldo",
                "Salones y laboratorios de ciencias");
        Ubicacion husi = ubicacion("Hospital Universitario San Ignacio", "Campus Clínico",
                "Hospital universitario docente asistencial");
        Ubicacion polideportivo = ubicacion("Polideportivo universitario", "Campus Norte",
                "Espacios deportivos del campus");

        Recurso videoBeam = recurso(audiovisual, auditorio, "Vídeo beam Epson PowerLite", "Equipo audiovisual",
                "Proyector para aulas y auditorios", "Full HD, HDMI, VGA", "Disponible", Recurso.Clase.EQUIPO);
        Recurso salon201 = recurso(aulas, ingenieria, "Salón 201", "Aula",
                "Salón para 45 estudiantes", "Tablero acrílico, videobeam", "Disponible", Recurso.Clase.ESPACIO);
        Recurso aulaMaxima = recurso(auditorios, auditorio, "Aula máxima 101", "Auditorio",
                "Aula máxima con capacidad de 200 personas", "Sonido, micrófonos", "Reservado", Recurso.Clase.ESPACIO);
        Recurso labQuimica = recurso(laboratorios, ciencias, "Laboratorio de Química Orgánica", "Laboratorio",
                "Laboratorio de prácticas de química", "Campanas de extracción, mesones", "Bloqueado", Recurso.Clase.ESPACIO);
        Recurso labFisica = recurso(laboratorios, ciencias, "Laboratorio de Física", "Laboratorio",
                "Laboratorio de prácticas de física", "Mesas de experimentación", "Disponible", Recurso.Clase.ESPACIO);
        Recurso computadorDell = recurso(computo, biblioteca, "Computador Dell OptiPlex", "Equipo de cómputo",
                "Computador de uso académico", "8 GB RAM, i5", "Disponible", Recurso.Clase.EQUIPO);
        Recurso salaEstudio = recurso(aulas, biblioteca, "Sala de estudio grupal 3", "Salón",
                "Sala para trabajo en grupo", "Capacidad 8 personas", "Disponible", Recurso.Clase.ESPACIO);
        Recurso canchaFutbol = recurso(deportivos, polideportivo, "Cancha de fútbol", "Espacio deportivo",
                "Cancha de césped sintético", "Iluminación nocturna", "Disponible", Recurso.Clase.ESPACIO);
        Recurso microfonos = recurso(audiovisual, auditorio, "Micrófonos inalámbricos SHURE", "Equipo audiovisual",
                "Set de micrófonos para auditorio", "Set de 4 unidades", "En préstamo", Recurso.Clase.EQUIPO);
        Recurso tableroDigital = recurso(audiovisual, ingenieria, "Tablero digital interactivo", "Equipo audiovisual",
                "Pantalla táctil para docencia", "85 pulgadas, HDMI", "Disponible", Recurso.Clase.EQUIPO);

        reserva(salaEstudio, LocalDate.of(2026, 9, 15), "08:00", "10:00", "Trabajo final",
                "Confirmada", "Invitado");
        reserva(aulaMaxima, LocalDate.of(2026, 9, 16), "14:00", "16:00", "Conferencia",
                "Confirmada", "Docente");

        prestamoDevuelto(videoBeam, "104.109.003", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 15), "Bueno", "Entrega sin novedades", LocalDate.of(2026, 9, 12));

        Incidente incidente = new Incidente(salon201, "Daño físico", "Media",
                "Silla del puesto 4 dañada", LocalDateTime.now().minusDays(1));
        incidenteService.guardar(incidente);
    }

    private Categoria categoria(String nombre, String descripcion) {
        return categoriaService.guardar(new Categoria(nombre, descripcion));
    }

    private Ubicacion ubicacion(String nombre, String edificio, String descripcion) {
        return ubicacionService.guardar(new Ubicacion(nombre, edificio, descripcion));
    }

    private Recurso recurso(Categoria categoria, Ubicacion ubicacion, String nombre, String tipo,
                            String descripcion, String caracteristicas, String estado, Recurso.Clase clase) {
        Recurso recurso = new Recurso(nombre, tipo, descripcion, caracteristicas, estado);
        recurso.setClase(clase);
        recurso.setCategoria(categoria);
        recurso.setUbicacion(ubicacion);
        return recursoService.guardar(recurso);
    }

    private void reserva(Recurso recurso, LocalDate fecha, String inicio, String fin, String motivo,
                         String estado, String solicitante) {
        Reserva reserva = new Reserva(recurso, fecha, inicio, fin, motivo, estado, solicitante);
        reservaService.guardar(reserva);
        recurso.setEstado("Reservado");
        recursoService.actualizar(recurso);
    }

    private void prestamoDevuelto(Recurso recurso, String documento, LocalDate entrega,
                                  LocalDate limite, String estadoEquipo, String observaciones,
                                  LocalDate devolucion) {
        Prestamo prestamo = new Prestamo(recurso, documento, entrega, limite, estadoEquipo, observaciones);
        prestamo.setEstado("Devuelto");
        prestamo.setFechaDevolucion(devolucion);
        prestamoService.guardar(prestamo);
    }
}