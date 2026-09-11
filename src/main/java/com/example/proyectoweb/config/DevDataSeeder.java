package com.example.proyectoweb.config;

import com.example.proyectoweb.model.Categoria;
import com.example.proyectoweb.model.Recurso;
import com.example.proyectoweb.model.Ubicacion;
import com.example.proyectoweb.service.CategoriaService;
import com.example.proyectoweb.service.RecursoService;
import com.example.proyectoweb.service.UbicacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private UbicacionService ubicacionService;

    @Autowired
    private RecursoService recursoService;

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

        recurso(audiovisual, auditorio, "Vídeo beam Epson PowerLite", "Equipo audiovisual",
                "Proyector para aulas y auditorios", "Full HD, HDMI, VGA", "Disponible");
        recurso(aulas, ingenieria, "Salón 201", "Aula",
                "Salón para 45 estudiantes", "Tablero acrílico, videobeam", "Disponible");
        recurso(auditorios, auditorio, "Aula máxima 101", "Auditorio",
                "Aula máxima con capacidad de 200 personas", "Sonido, micrófonos", "Reservado");
        recurso(laboratorios, ciencias, "Laboratorio de Química Orgánica", "Laboratorio",
                "Laboratorio de prácticas de química", "Campanas de extracción, mesones", "En mantenimiento");
        recurso(laboratorios, ciencias, "Laboratorio de Física", "Laboratorio",
                "Laboratorio de prácticas de física", "Mesas de experimentación", "Disponible");
        recurso(computo, biblioteca, "Computador Dell OptiPlex", "Equipo de cómputo",
                "Computador de uso académico", "8 GB RAM, i5", "Disponible");
        recurso(aulas, biblioteca, "Sala de estudio grupal 3", "Salón",
                "Sala para trabajo en grupo", "Capacidad 8 personas", "Disponible");
        recurso(deportivos, polideportivo, "Cancha de fútbol", "Espacio deportivo",
                "Cancha de césped sintético", "Iluminación nocturna", "Disponible");
        recurso(audiovisual, auditorio, "Micrófonos inalámbricos SHURE", "Equipo audiovisual",
                "Set de micrófonos para auditorio", "Set de 4 unidades", "En préstamo");
        recurso(audiovisual, ingenieria, "Tablero digital interactivo", "Equipo audiovisual",
                "Pantalla táctil para docencia", "85 pulgadas, HDMI", "Disponible");
    }

    private Categoria categoria(String nombre, String descripcion) {
        return categoriaService.guardar(new Categoria(nombre, descripcion));
    }

    private Ubicacion ubicacion(String nombre, String edificio, String descripcion) {
        return ubicacionService.guardar(new Ubicacion(nombre, edificio, descripcion));
    }

    private void recurso(Categoria categoria, Ubicacion ubicacion, String nombre, String tipo,
                         String descripcion, String caracteristicas, String estado) {
        Recurso recurso = new Recurso(nombre, tipo, descripcion, caracteristicas, estado);
        recurso.setCategoria(categoria);
        recurso.setUbicacion(ubicacion);
        recursoService.guardar(recurso);
    }
}