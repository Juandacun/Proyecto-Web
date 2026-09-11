-- ============================================================
-- Carga inicial del catálogo (perfil dev, base H2)
-- Los INSERT son idempotentes: cada fila solo se inserta cuando
-- aún no existe, así no se duplican al reiniciar la aplicación.
-- ============================================================

-- ---------- Categorías ----------
INSERT INTO categorias (nombre, descripcion)
SELECT 'Aulas y salones', 'Salones de clase y salas de estudio del campus'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Aulas y salones');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Auditorios', 'Auditorios y aulas máximas para eventos'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Auditorios');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Equipos audiovisuales', 'Proyectores, pantallas y equipos de sonido'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Equipos audiovisuales');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Laboratorios', 'Laboratorios de docencia e investigación'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Laboratorios');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Espacios deportivos', 'Canchas y espacios para la actividad física'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Espacios deportivos');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Equipos de cómputo', 'Computadores y equipos para salas de sistemas'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Equipos de cómputo');

-- ---------- Ubicaciones ----------
INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Biblioteca General Alfonso Borrero Cabal', 'Edificio 25', 'Biblioteca central del campus'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Biblioteca General Alfonso Borrero Cabal');

INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Auditorio Jorge Hoyos Vásquez', 'Edificio 17', 'Auditorio principal o aula máxima'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Auditorio Jorge Hoyos Vásquez');

INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Edificio de Ingeniería José Gabriel Maldonado', 'Campus Central', 'Sede de la Facultad de Ingeniería'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Edificio de Ingeniería José Gabriel Maldonado');

INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Facultad de Ciencias Básicas', 'Edificio Gabriel Giraldo', 'Salones y laboratorios de ciencias'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Facultad de Ciencias Básicas');

INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Hospital Universitario San Ignacio', 'Campus Clínico', 'Hospital universitario docente asistencial'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Hospital Universitario San Ignacio');

INSERT INTO ubicaciones (nombre, edificio, descripcion)
SELECT 'Polideportivo universitario', 'Campus Norte', 'Espacios deportivos del campus'
WHERE NOT EXISTS (SELECT 1 FROM ubicaciones WHERE nombre = 'Polideportivo universitario');

-- ---------- Recursos ----------
INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Vídeo beam Epson PowerLite', 'Equipo audiovisual', 'Proyector para aulas y auditorios', 'Full HD, HDMI, VGA', 'Disponible', 'EQUIPO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Equipos audiovisuales'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Auditorio Jorge Hoyos Vásquez')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Vídeo beam Epson PowerLite');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Salón 201', 'Aula', 'Salón para 45 estudiantes', 'Tablero acrílico, videobeam', 'Disponible', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Aulas y salones'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Edificio de Ingeniería José Gabriel Maldonado')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Salón 201');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Aula máxima 101', 'Auditorio', 'Aula máxima con capacidad de 200 personas', 'Sonido, micrófonos', 'Reservado', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Auditorios'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Auditorio Jorge Hoyos Vásquez')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Aula máxima 101');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Laboratorio de Química Orgánica', 'Laboratorio', 'Laboratorio de prácticas de química', 'Campanas de extracción, mesones', 'Bloqueado', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Laboratorios'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Facultad de Ciencias Básicas')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Laboratorio de Química Orgánica');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Laboratorio de Física', 'Laboratorio', 'Laboratorio de prácticas de física', 'Mesas de experimentación', 'Disponible', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Laboratorios'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Facultad de Ciencias Básicas')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Laboratorio de Física');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Computador Dell OptiPlex', 'Equipo de cómputo', 'Computador de uso académico', '8 GB RAM, i5', 'Disponible', 'EQUIPO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Equipos de cómputo'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Biblioteca General Alfonso Borrero Cabal')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Computador Dell OptiPlex');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Sala de estudio grupal 3', 'Salón', 'Sala para trabajo en grupo', 'Capacidad 8 personas', 'Disponible', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Aulas y salones'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Biblioteca General Alfonso Borrero Cabal')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Sala de estudio grupal 3');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Cancha de fútbol', 'Espacio deportivo', 'Cancha de césped sintético', 'Iluminación nocturna', 'Disponible', 'ESPACIO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Espacios deportivos'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Polideportivo universitario')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Cancha de fútbol');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Micrófonos inalámbricos SHURE', 'Equipo audiovisual', 'Set de micrófonos para auditorio', 'Set de 4 unidades', 'En préstamo', 'EQUIPO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Equipos audiovisuales'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Auditorio Jorge Hoyos Vásquez')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Micrófonos inalámbricos SHURE');

INSERT INTO recursos (nombre, tipo, descripcion, caracteristicas, estado, clase, id_categoria, id_ubicacion)
SELECT 'Tablero digital interactivo', 'Equipo audiovisual', 'Pantalla táctil para docencia', '85 pulgadas, HDMI', 'Disponible', 'EQUIPO',
       (SELECT id_categoria FROM categorias WHERE nombre = 'Equipos audiovisuales'),
       (SELECT id_ubicacion FROM ubicaciones WHERE nombre = 'Edificio de Ingeniería José Gabriel Maldonado')
WHERE NOT EXISTS (SELECT 1 FROM recursos WHERE nombre = 'Tablero digital interactivo');

-- ---------- Reservas de ejemplo ----------
INSERT INTO reservas (id_recurso, fecha, hora_inicio, hora_fin, motivo, estado, solicitante)
SELECT r.id_recurso, DATE '2026-09-15', '08:00', '10:00', 'Trabajo final', 'Confirmada', 'Invitado'
FROM recursos r
WHERE r.nombre = 'Sala de estudio grupal 3'
  AND NOT EXISTS (SELECT 1 FROM reservas WHERE id_recurso = r.id_recurso AND fecha = DATE '2026-09-15' AND hora_inicio = '08:00');

INSERT INTO reservas (id_recurso, fecha, hora_inicio, hora_fin, motivo, estado, solicitante)
SELECT r.id_recurso, DATE '2026-09-16', '14:00', '16:00', 'Conferencia', 'Confirmada', 'Docente'
FROM recursos r
WHERE r.nombre = 'Aula máxima 101'
  AND NOT EXISTS (SELECT 1 FROM reservas WHERE id_recurso = r.id_recurso AND fecha = DATE '2026-09-16' AND hora_inicio = '14:00');

-- Los espacios reservados quedan marcados como Reservado
UPDATE recursos SET estado = 'Reservado'
WHERE estado <> 'Reservado'
  AND EXISTS (SELECT 1 FROM reservas WHERE id_recurso = recursos.id_recurso AND estado = 'Confirmada');

-- ---------- Préstamo de ejemplo (devuelto a tiempo) ----------
INSERT INTO prestamos (id_recurso, documento, fecha_entrega, fecha_devolucion_estimada, estado_equipo, observaciones, estado, fecha_devolucion)
SELECT r.id_recurso, '104.109.003', DATE '2026-09-01', DATE '2026-09-15', 'Bueno', 'Entrega sin novedades', 'Devuelto', DATE '2026-09-12'
FROM recursos r
WHERE r.nombre = 'Vídeo beam Epson PowerLite'
  AND NOT EXISTS (SELECT 1 FROM prestamos WHERE documento = '104.109.003' AND fecha_entrega = DATE '2026-09-01');

-- ---------- Incidente de ejemplo ----------
INSERT INTO incidentes (id_recurso, tipo, severidad, descripcion, fecha)
SELECT r.id_recurso, 'Daño físico', 'Media', 'Silla del puesto 4 dañada', TIMESTAMP '2026-09-09 08:00:00'
FROM recursos r
WHERE r.nombre = 'Salón 201'
  AND NOT EXISTS (SELECT 1 FROM incidentes WHERE descripcion = 'Silla del puesto 4 dañada');