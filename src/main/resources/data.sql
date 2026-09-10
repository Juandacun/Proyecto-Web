INSERT INTO categorias (nombre, descripcion) VALUES
('Aulas y salones', 'Salones de clase y salas de estudio del campus'),
('Auditorios', 'Auditorios y aulas máximas para eventos'),
('Equipos audiovisuales', 'Proyectores, pantallas y equipos de sonido'),
('Laboratorios', 'Laboratorios de docencia e investigaci&oacute;n'),
('Espacios deportivos', 'Canchas y espacios para la actividad f&iacute;sica'),
('Equipos de c&oacute;mputo', 'Computadores y equipos para salas de sistemas');

INSERT INTO ubicaciones (nombre, edificio, descripcion) VALUES
('Biblioteca General Alfonso Borrero Cabal', 'Edificio 25', 'Biblioteca central del campus'),
('Auditorio Jorge Hoyos V&aacute;squez', 'Edificio 17', 'Auditorio principal o aula m&aacute;xima'),
('Edificio de Ingenier&iacute;a Jos&eacute; Gabriel Maldonado', 'Campus Central', 'Sede de la Facultad de Ingenier&iacute;a'),
('Facultad de Ciencias B&aacute;sicas', 'Edificio Gabriel Giraldo', 'Salones y laboratorios de ciencias'),
('Hospital Universitario San Ignacio', 'Campus Cl&iacute;nico', 'Hospital universitario docente asistencial'),
('Polideportivo universitario', 'Campus Norte', 'Espacios deportivos del campus');

INSERT INTO recursos (id_categoria, id_ubicacion, nombre, tipo, descripcion, caracteristicas, estado) VALUES
(3, 2, 'V&iacute;deo beam Epson PowerLite', 'Equipo audiovisual', 'Proyector para aulas y auditorios', 'Full HD, HDMI, VGA', 'Disponible'),
(1, 3, 'Sal&oacute;n 201', 'Aula', 'Sal&oacute;n para 45 estudiantes', 'Tablero acr&iacute;lico, videobeam', 'Disponible'),
(2, 2, 'Aula m&aacute;xima 101', 'Auditorio', 'Aula m&aacute;xima con capacidad de 200 personas', 'Sonido, micr&oacute;fonos', 'Reservado'),
(4, 4, 'Laboratorio de Qu&iacute;mica Org&aacute;nica', 'Laboratorio', 'Laboratorio de pr&aacute;cticas de qu&iacute;mica', 'Campanas de extracci&oacute;n, mesones', 'En mantenimiento'),
(4, 4, 'Laboratorio de F&iacute;sica', 'Laboratorio', 'Laboratorio de pr&aacute;cticas de f&iacute;sica', 'Mesas de experimentaci&oacute;n', 'Disponible'),
(6, 1, 'Computador Dell OptiPlex', 'Equipo de c&oacute;mputo', 'Computador de uso acad&eacute;mico', '8 GB RAM, i5', 'Disponible'),
(1, 1, 'Sala de estudio grupal 3', 'Sal&oacute;n', 'Sala para trabajo en grupo', 'Capacidad 8 personas', 'Disponible'),
(5, 6, 'Cancha de f&uacute;tbol', 'Espacio deportivo', 'Cancha de c&eacute;sped sint&eacute;tico', 'Iluminaci&oacute;n nocturna', 'Disponible'),
(3, 2, 'Micr&oacute;fonos inal&aacute;mbricos SHURE', 'Equipo audiovisual', 'Set de micr&oacute;fonos para auditorio', 'Set de 4 unidades', 'En pr&eacute;stamo'),
(3, 3, 'Tablero digital interactivo', 'Equipo audiovisual', 'Pantalla t&aacute;ctil para docencia', '85 pulgadas, HDMI', 'Disponible');