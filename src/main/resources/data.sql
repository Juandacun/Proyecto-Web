INSERT INTO categorias (nombre, descripcion) VALUES
('Aulas y salones', 'Salones de clase y salas de estudio del campus'),
('Auditorios', 'Auditorios y aulas máximas para eventos'),
('Equipos audiovisuales', 'Proyectores, pantallas y equipos de sonido'),
('Laboratorios', 'Laboratorios de docencia e investigación'),
('Espacios deportivos', 'Canchas y espacios para la actividad física'),
('Equipos de cómputo', 'Computadores y equipos para salas de sistemas');

INSERT INTO ubicaciones (nombre, edificio, descripcion) VALUES
('Biblioteca General Alfonso Borrero Cabal', 'Edificio 25', 'Biblioteca central del campus'),
('Auditorio Jorge Hoyos Vásquez', 'Edificio 17', 'Auditorio principal o aula máxima'),
('Edificio de Ingeniería José Gabriel Maldonado', 'Campus Central', 'Sede de la Facultad de Ingeniería'),
('Facultad de Ciencias Básicas', 'Edificio Gabriel Giraldo', 'Salones y laboratorios de ciencias'),
('Hospital Universitario San Ignacio', 'Campus Clínico', 'Hospital universitario docente asistencial'),
('Polideportivo universitario', 'Campus Norte', 'Espacios deportivos del campus');

INSERT INTO recursos (id_categoria, id_ubicacion, nombre, tipo, descripcion, caracteristicas, estado) VALUES
(3, 2, 'Vídeo beam Epson PowerLite', 'Equipo audiovisual', 'Proyector para aulas y auditorios', 'Full HD, HDMI, VGA', 'Disponible'),
(1, 3, 'Salón 201', 'Aula', 'Salón para 45 estudiantes', 'Tablero acrílico, videobeam', 'Disponible'),
(2, 2, 'Aula máxima 101', 'Auditorio', 'Aula máxima con capacidad de 200 personas', 'Sonido, micrófonos', 'Reservado'),
(4, 4, 'Laboratorio de Química Orgánica', 'Laboratorio', 'Laboratorio de prácticas de química', 'Campanas de extracción, mesones', 'En mantenimiento'),
(4, 4, 'Laboratorio de Física', 'Laboratorio', 'Laboratorio de prácticas de física', 'Mesas de experimentación', 'Disponible'),
(6, 1, 'Computador Dell OptiPlex', 'Equipo de cómputo', 'Computador de uso académico', '8 GB RAM, i5', 'Disponible'),
(1, 1, 'Sala de estudio grupal 3', 'Salón', 'Sala para trabajo en grupo', 'Capacidad 8 personas', 'Disponible'),
(5, 6, 'Cancha de fútbol', 'Espacio deportivo', 'Cancha de césped sintético', 'Iluminación nocturna', 'Disponible'),
(3, 2, 'Micrófonos inalámbricos SHURE', 'Equipo audiovisual', 'Set de micrófonos para auditorio', 'Set de 4 unidades', 'En préstamo'),
(3, 3, 'Tablero digital interactivo', 'Equipo audiovisual', 'Pantalla táctil para docencia', '85 pulgadas, HDMI', 'Disponible');