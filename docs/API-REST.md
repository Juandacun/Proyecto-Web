# API REST — CampusRecursos

Referencia de los servicios REST que consume la SPA. La misma información está disponible en vivo en Swagger UI (`http://localhost:8080/swagger-ui/index.html`).

## Convenciones

- **URL base:** `http://localhost:8080/api`
- **Formato:** JSON (`Content-Type: application/json`) en peticiones y respuestas.
- **Fechas:** `yyyy-MM-dd`. **Fecha-hora:** `yyyy-MM-dd'T'HH:mm:ss`. **Horas de reserva:** `HH:mm`.
- **CORS:** permitido para `http://localhost:4200` con `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- **Autenticación:** no aplica en la segunda entrega.
- **Alias:** salvo `recursos` e `indicadores`, cada recurso también responde sin el prefijo `/api` (por ejemplo `/reservas`), por compatibilidad. La SPA usa siempre `/api/...`.

### Códigos de estado

| Código | Uso |
|--------|-----|
| `200 OK` | Consulta o actualización correcta |
| `201 Created` | Recurso creado. Incluye cabecera `Location` con la URL del nuevo elemento |
| `204 No Content` | Eliminación correcta |
| `400 Bad Request` | Datos inválidos o faltantes |
| `404 Not Found` | El ID no existe |
| `409 Conflict` | Se viola una regla de negocio (RN-01, RN-02, RN-03) o el estado no permite la operación |

### Formato de error

```json
{ "error": "Conflicto de horario: el recurso 'Sala 3' ya tiene una reserva ... (RN-01)", "codigo": "RN-01" }
```

`codigo` aparece en los `409` de reservas y préstamos: `RN-01`, `RN-02`, `RN-03` o `ESTADO_RESERVA`.

## Resumen de endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/recursos` | Listar recursos (filtros `clase`, `estado`, `disponible`, `categoriaId`, `ubicacionId`, `tipo`) |
| GET | `/api/recursos/equipos` | Listar equipos |
| GET | `/api/recursos/espacios` | Listar espacios |
| GET | `/api/recursos/{id}` | Detalle de un recurso |
| POST | `/api/recursos` | Crear recurso |
| PUT | `/api/recursos/{id}` | Actualizar recurso |
| PATCH | `/api/recursos/{id}/estado` | Cambiar estado (bloquear / habilitar) |
| DELETE | `/api/recursos/{id}` | Eliminar recurso |
| GET | `/api/categorias` | Listar categorías |
| GET | `/api/categorias/{id}` | Detalle de categoría |
| POST | `/api/categorias` | Crear categoría |
| PUT | `/api/categorias/{id}` | Actualizar categoría |
| DELETE | `/api/categorias/{id}` | Eliminar categoría |
| GET | `/api/ubicaciones` | Listar ubicaciones |
| GET | `/api/ubicaciones/{id}` | Detalle de ubicación |
| POST | `/api/ubicaciones` | Crear ubicación |
| PUT | `/api/ubicaciones/{id}` | Actualizar ubicación |
| DELETE | `/api/ubicaciones/{id}` | Eliminar ubicación |
| GET | `/api/reservas` | Listar reservas (filtros `recursoId`, `fecha`, `estado`) |
| GET | `/api/reservas/{id}` | Detalle de reserva |
| GET | `/api/reservas/recurso/{recursoId}` | Reservas de un recurso |
| GET | `/api/reservas/verificar-disponibilidad` | Verificar disponibilidad sin guardar |
| POST | `/api/reservas` | Crear reserva |
| PUT | `/api/reservas/{id}` | Modificar reserva |
| PATCH | `/api/reservas/{id}/cancelar` | Cancelar reserva |
| PATCH | `/api/reservas/{id}/finalizar` | Finalizar reserva |
| DELETE | `/api/reservas/{id}` | Eliminar reserva |
| GET | `/api/prestamos` | Listar préstamos (filtros `recursoId`, `usuarioId`, `activo`) |
| GET | `/api/prestamos/{id}` | Detalle de préstamo |
| GET | `/api/prestamos/activos` | Préstamos sin devolver |
| GET | `/api/prestamos/tardios` | Préstamos vencidos o devueltos tarde |
| POST | `/api/prestamos/entrega` | Registrar entrega |
| PATCH | `/api/prestamos/{id}/devolucion` | Registrar devolución |
| GET | `/api/incidentes` | Listar incidentes (filtros `recursoId`, `severidad`) |
| GET | `/api/incidentes/{id}` | Detalle de incidente |
| POST | `/api/incidentes` | Reportar incidente |
| GET | `/api/mantenimientos` | Listar mantenimientos (filtros `recursoId`, `estado`) |
| GET | `/api/mantenimientos/{id}` | Detalle de mantenimiento |
| POST | `/api/mantenimientos` | Abrir orden de mantenimiento |
| PATCH | `/api/mantenimientos/{id}/finalizar` | Finalizar mantenimiento |
| PATCH | `/api/mantenimientos/{id}/estado` | Cambiar estado (En proceso, Finalizada, Cancelada) |
| GET | `/api/indicadores` | Indicadores del tablero |
| GET | `/api/usuarios` | Listar usuarios |
| GET | `/api/usuarios/{id}` | Detalle de usuario |

---

## Recursos — `/api/recursos`

Controlador: `RecursoController` · Servicio: `RecursoService`

Un recurso tiene una **clase**: `ESPACIO` (se reserva) o `EQUIPO` (se presta).

### Modelo `Recurso`

```json
{
  "idRecurso": 1,
  "nombre": "Sala de estudio 3",
  "tipo": "Sala de estudio",
  "descripcion": "Sala para trabajo en grupo",
  "caracteristicas": "Capacidad 8, tablero, TV",
  "estado": "Disponible",
  "clase": "ESPACIO",
  "categoria": { "idCategoria": 1, "nombre": "Aulas y salones", "descripcion": "..." },
  "ubicacion": { "idUbicacion": 1, "nombre": "Biblioteca General", "edificio": "Edificio 25", "descripcion": "..." }
}
```

Estados usados: `Disponible`, `Reservado`, `Prestado`, `Bloqueado`.

### `GET /api/recursos`

| Parámetro (query) | Tipo | Descripción |
|-------------------|------|-------------|
| `clase` | `EQUIPO` \| `ESPACIO` | Filtra por clase |
| `estado` | string | Filtra por estado (sin distinguir mayúsculas) |
| `disponible` | boolean | Si es `true`, solo recursos en estado `Disponible` (tiene prioridad sobre `estado`) |
| `categoriaId` | long | Filtra por categoría |
| `ubicacionId` | long | Filtra por ubicación |
| `tipo` | string | Filtra por tipo exacto (sin distinguir mayúsculas), p. ej. `Proyector` |

Todos los filtros son combinables. Ejemplo: `GET /api/recursos?clase=EQUIPO&categoriaId=3&estado=Disponible`.
**200** `Recurso[]`

### `GET /api/recursos/equipos` · `GET /api/recursos/espacios`

**200** `Recurso[]` de la clase correspondiente.

### `GET /api/recursos/{id}`

**200** `Recurso` · **404** si no existe.

### `POST /api/recursos`

```json
{
  "nombre": "Proyector Epson",
  "tipo": "Proyector",
  "descripcion": "Proyector portátil",
  "caracteristicas": "3500 lúmenes, HDMI",
  "clase": "EQUIPO",
  "categoria": { "idCategoria": 3 },
  "ubicacion": { "idUbicacion": 2 }
}
```

Obligatorios: `nombre`, `tipo`, `categoria.idCategoria`, `ubicacion.idUbicacion`. Si no se envía `estado` queda `Disponible`.
**201** `Recurso` · **400** campo faltante o categoría/ubicación inexistente.

### `PUT /api/recursos/{id}`

Mismo cuerpo que `POST`; actualiza los campos enviados.
**200** `Recurso` · **400** datos inválidos · **404** no existe.

### `PATCH /api/recursos/{id}/estado`

Permite al gestor bloquear o habilitar un recurso manualmente.

```json
{ "estado": "Bloqueado" }
```

**200** `Recurso` · **400** falta `estado` · **404** no existe.

### `DELETE /api/recursos/{id}`

**204** · **404** no existe.

---

## Categorías — `/api/categorias`

Controlador: `CategoriaController`

```json
{ "idCategoria": 1, "nombre": "Laboratorios", "descripcion": "Laboratorios de docencia e investigación" }
```

| Método | Ruta | Cuerpo | Respuestas |
|--------|------|--------|-----------|
| GET | `/api/categorias` | — | 200 `Categoria[]` |
| GET | `/api/categorias/{id}` | — | 200, 404 |
| POST | `/api/categorias` | `{ "nombre", "descripcion" }` | 201, 400 (`nombre` obligatorio) |
| PUT | `/api/categorias/{id}` | `{ "nombre", "descripcion" }` | 200, 400, 404 |
| DELETE | `/api/categorias/{id}` | — | 204, 404 |

## Ubicaciones — `/api/ubicaciones`

Controlador: `UbicacionController`

```json
{ "idUbicacion": 1, "nombre": "Biblioteca General", "edificio": "Edificio 25", "descripcion": "Biblioteca central" }
```

| Método | Ruta | Cuerpo | Respuestas |
|--------|------|--------|-----------|
| GET | `/api/ubicaciones` | — | 200 `Ubicacion[]` |
| GET | `/api/ubicaciones/{id}` | — | 200, 404 |
| POST | `/api/ubicaciones` | `{ "nombre", "edificio", "descripcion" }` | 201, 400 (`nombre` obligatorio) |
| PUT | `/api/ubicaciones/{id}` | `{ "nombre", "edificio", "descripcion" }` | 200, 400, 404 |
| DELETE | `/api/ubicaciones/{id}` | — | 204, 404 |

---

## Reservas — `/api/reservas`

Controlador: `ReservaController` · Servicio: `ReservaService`

Solo se reservan recursos de clase `ESPACIO`. Estados: `Confirmada` → `Finalizada` | `Cancelada`.

### Modelo `Reserva`

```json
{
  "idReserva": 10,
  "recurso": { "idRecurso": 1, "nombre": "Sala de estudio 3", "...": "..." },
  "fecha": "2026-11-05",
  "horaInicio": "08:00",
  "horaFin": "10:00",
  "motivo": "Estudio grupal",
  "estado": "Confirmada",
  "solicitante": "Ana Pérez",
  "editable": true,
  "iniciada": false
}
```

`editable` (calculado): la reserva está `Confirmada` y aún no ha iniciado, por lo que se puede modificar o cancelar.
`iniciada` (calculado): la fecha/hora de inicio ya pasó.

### Validaciones al crear o modificar

1. `recursoId`, `fecha`, `horaInicio` y `horaFin` son obligatorios; las horas deben tener formato `HH:mm` → **400**.
2. `horaInicio` < `horaFin` y el inicio debe ser futuro → **400**.
3. El recurso debe existir y ser `ESPACIO` → **400**.
4. **RN-02:** el recurso no puede estar bloqueado ni prestado → **409** (`codigo: "RN-02"`).
5. **RN-01:** no puede existir otra reserva `Confirmada` del mismo recurso con horario solapado → **409** (`codigo: "RN-01"`).

### `GET /api/reservas`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `recursoId` | long | Reservas del recurso |
| `fecha` | `yyyy-MM-dd` | Reservas de ese día |
| `estado` | string | `Confirmada`, `Finalizada`, `Cancelada` |

Filtros combinables. **200** `Reserva[]`

### `GET /api/reservas/{id}`

**200** `Reserva` · **404**.

### `GET /api/reservas/recurso/{recursoId}`

**200** `Reserva[]` del recurso.

### `GET /api/reservas/verificar-disponibilidad`

Comprueba RN-01 y RN-02 **sin guardar** nada. La SPA lo usa para avisar al usuario antes de confirmar.

| Parámetro | Obligatorio | Descripción |
|-----------|-------------|-------------|
| `recursoId` | sí | ID del espacio |
| `fecha` | sí | `yyyy-MM-dd` |
| `horaInicio` | sí | `HH:mm` |
| `horaFin` | sí | `HH:mm` |
| `reservaIdExcluir` | no | ID de la reserva que se está editando (para no chocar consigo misma) |

Respuestas **200**:

```json
{ "disponible": true, "mensaje": "El horario y recurso se encuentran disponibles." }
```

```json
{
  "disponible": false,
  "motivo": "RN-01",
  "mensaje": "El recurso no está disponible: ya tiene una reserva de 08:00 a 10:00 ese día (RN-01).",
  "conflictoInicio": "08:00",
  "conflictoFin": "10:00"
}
```

```json
{ "disponible": false, "motivo": "RN-02", "mensaje": "El recurso se encuentra bloqueado por daño o mantenimiento (RN-02)." }
```

**400** `{ "disponible": false, "error": "..." }` si el recurso no existe, no es un espacio, el formato de hora es inválido, las horas están invertidas o el horario ya pasó.

### `POST /api/reservas`

```json
{
  "recursoId": 1,
  "fecha": "2026-11-05",
  "horaInicio": "08:00",
  "horaFin": "10:00",
  "motivo": "Estudio grupal",
  "solicitante": "Ana Pérez"
}
```

La reserva queda `Confirmada` y el recurso pasa a `Reservado`.
**201** `Reserva` · **400** · **409** (RN-01 / RN-02).

### `PUT /api/reservas/{id}`

Mismo cuerpo que `POST`. Solo para reservas `Confirmada` que no han iniciado. Si cambia el recurso, el anterior se libera si ya no tiene reservas activas.
**200** `Reserva` · **400** · **404** · **409** (RN-01 / RN-02 / `ESTADO_RESERVA`).

### `PATCH /api/reservas/{id}/cancelar`

También acepta `POST`. Sin cuerpo. Solo antes del inicio y si está `Confirmada`. Libera el recurso a `Disponible` si no le quedan reservas activas.
**200** `Reserva` (estado `Cancelada`) · **404** · **409** (`ESTADO_RESERVA`).

### `PATCH /api/reservas/{id}/finalizar`

También acepta `POST`. Sin cuerpo. Solo si está `Confirmada`.
**200** `Reserva` (estado `Finalizada`) · **404** · **409**.

### `DELETE /api/reservas/{id}`

Borrado físico. **204** · **404**.

---

## Préstamos — `/api/prestamos`

Controlador: `PrestamoController` · Servicio: `PrestamoService`

Solo se prestan recursos de clase `EQUIPO`. Estados del préstamo: `Prestado` → `Devuelto`.

### Modelo `Prestamo`

```json
{
  "idPrestamo": 4,
  "recurso": { "idRecurso": 7, "nombre": "Portátil Dell 05", "...": "..." },
  "documento": "1012345678",
  "usuarioResponsable": "Carlos Gómez",
  "fechaEntrega": "2026-10-20",
  "fechaDevolucionEstimada": "2026-10-27",
  "estadoEquipo": "Bueno",
  "accesorios": "Cargador, mouse",
  "observaciones": "Sin novedad | Devolución: pantalla rayada",
  "estado": "Devuelto",
  "fechaDevolucion": "2026-10-28",
  "puntualidad": "Tardía"
}
```

`puntualidad` vale `En curso` mientras el préstamo está activo y se calcula al devolver: `Tardía` si `fechaDevolucion` > `fechaDevolucionEstimada`, si no `Puntual`. La respuesta incluye además `estadoColor` (calculado, `rojo` si está vencido sin devolver).

### `GET /api/prestamos`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `recursoId` | long | Préstamos del equipo |
| `usuarioId` | string | Documento o nombre del responsable |
| `activo` | boolean | `true` = sin devolver, `false` = devueltos |

**200** `Prestamo[]`

### `GET /api/prestamos/{id}`

**200** `Prestamo` · **404**.

### `GET /api/prestamos/activos`

**200** `Prestamo[]` con estado distinto de `Devuelto`.

### `GET /api/prestamos/tardios`

**200** `Prestamo[]` vencidos sin devolver o devueltos fuera de plazo. Sirve como indicador de devoluciones tardías.

### `POST /api/prestamos/entrega`

También disponible como `POST /api/prestamos`.

```json
{
  "recursoId": 7,
  "usuarioResponsable": "Carlos Gómez",
  "documento": "1012345678",
  "fechaEntrega": "2026-10-20",
  "fechaLimite": "2026-10-27",
  "estadoInicial": "Bueno",
  "accesorios": "Cargador, mouse",
  "observaciones": "Sin novedad"
}
```

| Campo | Obligatorio | Notas |
|-------|-------------|-------|
| `recursoId` | sí | Debe ser un `EQUIPO` |
| `usuarioResponsable` (o `documento`) | sí | Identifica al responsable; se guarda en `documento` y `usuarioResponsable` |
| `fechaLimite` (alias `fechaDevolucionEstimada`) | sí | No puede ser anterior a la entrega |
| `fechaEntrega` | no | Por defecto, hoy |
| `estadoInicial` (alias `estadoEquipo`) | no | Estado del equipo al entregarlo; por defecto `Bueno` |
| `accesorios`, `observaciones` | no | Texto libre |

El préstamo queda `Prestado` y el recurso pasa a `Prestado`.
**201** `Prestamo` · **400** datos inválidos o el recurso no es equipo · **409** `codigo: "RN-02"` (bloqueado) o `"RN-03"` (ya prestado).

### `PATCH /api/prestamos/{id}/devolucion`

También acepta `POST`. Cuerpo opcional:

```json
{ "fechaDevolucion": "2026-10-28", "estadoFinal": "Pantalla rayada", "observaciones": "Se reporta rayón" }
```

Si no se envía `fechaDevolucion` se usa la fecha actual. Calcula `puntualidad`, marca el préstamo `Devuelto` y deja el recurso `Disponible`.
**200** `Prestamo` · **404** · **409** si ya estaba devuelto.

---

## Incidentes — `/api/incidentes`

Controlador: `IncidenteController` · Servicio: `IncidenteService`

### Modelo `Incidente`

```json
{
  "idIncidente": 3,
  "recurso": { "idRecurso": 7, "nombre": "Portátil Dell 05", "estado": "Bloqueado", "...": "..." },
  "tipo": "Falla de funcionamiento",
  "severidad": "Crítica",
  "descripcion": "No enciende",
  "fecha": "2026-10-21T14:32:10",
  "reportadoPor": "1012345678"
}
```

### `GET /api/incidentes`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `recursoId` | long | Incidentes del recurso |
| `severidad` | string | `Leve`, `Media`, `Crítica` |

**200** `Incidente[]`

### `GET /api/incidentes/{id}`

**200** `Incidente` · **404**.

### `POST /api/incidentes`

```json
{
  "recursoId": 7,
  "tipoNovedad": "Falla de funcionamiento",
  "severidad": "Crítica",
  "descripcion": "No enciende",
  "usuarioId": "1012345678"
}
```

| Campo | Obligatorio | Valores |
|-------|-------------|---------|
| `recursoId` | sí | — |
| `descripcion` | sí | máx. 255 caracteres |
| `tipoNovedad` | no (por defecto `Daño físico`) | `Daño físico`, `Falla de funcionamiento`, `Accesorio faltante`, `Condición inadecuada del espacio` |
| `severidad` | no (por defecto `Media`) | `Leve`, `Media`, `Crítica` (acepta sin tilde) |
| `usuarioId` | no (por defecto `Anónimo`) | Quién reporta |

La fecha la asigna el servidor. **RN-04:** si la severidad es `Crítica`, el recurso pasa a `Bloqueado`.
**201** `Incidente` · **400** campo faltante, tipo o severidad inválidos, recurso inexistente.

---

## Mantenimientos — `/api/mantenimientos`

Controlador: `MantenimientoController` · Servicio: `MantenimientoService`

Estados de la intervención: `Abierta`, `En proceso`, `Finalizada`, `Cancelada`.

### Modelo `Mantenimiento`

```json
{
  "idMantenimiento": 2,
  "recurso": { "idRecurso": 7, "nombre": "Portátil Dell 05", "...": "..." },
  "fechaApertura": "2026-10-21T15:00:00",
  "diagnostico": "Fuente de poder dañada",
  "estado": "Abierta",
  "fechaCierre": null,
  "idUsuarioResponsable": "tecnico01",
  "observaciones": "Se pidió repuesto"
}
```

### `GET /api/mantenimientos`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `recursoId` | long | Órdenes del recurso |
| `estado` | string | Estado de la orden |

**200** `Mantenimiento[]`

### `GET /api/mantenimientos/{id}`

**200** `Mantenimiento` · **404**.

### `POST /api/mantenimientos`

```json
{
  "recursoId": 7,
  "diagnostico": "Fuente de poder dañada",
  "estado": "Abierta",
  "fechaApertura": "2026-10-21T15:00:00",
  "idUsuarioResponsable": "tecnico01",
  "observaciones": "Se pidió repuesto"
}
```

Solo `recursoId` es obligatorio. `estado` por defecto `Abierta`; `fechaApertura` por defecto, ahora. El recurso queda `Bloqueado` mientras dure la intervención.
**201** `Mantenimiento` · **400** recurso faltante o inexistente.

### `PATCH /api/mantenimientos/{id}/finalizar`

Sin cuerpo. Marca la orden `Finalizada`, registra `fechaCierre` y, por **RN-05**, deja el recurso `Disponible`.
**200** `Mantenimiento` · **404**.

### `PATCH /api/mantenimientos/{id}/estado`

```json
{ "estado": "En proceso" }
```

| Estado destino | Efecto |
|----------------|--------|
| `En proceso` | La orden queda en curso; el recurso sigue `Bloqueado` |
| `Finalizada` | Igual que `/finalizar`: cierra la orden y deja el recurso `Disponible` (RN-05) |
| `Cancelada` | Cierra la orden (registra `fechaCierre`); el recurso sigue `Bloqueado` hasta que el gestor lo habilite con `PATCH /api/recursos/{id}/estado` |

Solo se pueden cambiar órdenes `Abierta` o `En proceso`. El estado no distingue mayúsculas.
**200** `Mantenimiento` · **400** estado faltante o no válido · **404** · **409** la orden ya está `Finalizada` o `Cancelada`.

---

## Indicadores — `/api/indicadores`

Controlador: `IndicadorController` · Servicio: `IndicadorService`

### `GET /api/indicadores`

Calcula los indicadores del tablero operativo (requisito 4.f). No recibe parámetros.

```json
{
  "totalRecursos": 12,
  "recursosPorEstado": { "Disponible": 9, "Reservado": 1, "Prestado": 1, "Bloqueado": 1 },
  "reservasActivas": 1,
  "prestamosActivos": 0,
  "devolucionesTardias": 1,
  "incidentes": 1,
  "incidentesCriticos": 0,
  "reservasPorRecurso": [
    { "idRecurso": 2, "recurso": "Salón 201", "categoria": "Aulas y salones", "ubicacion": "Edificio de Ingeniería", "clase": "ESPACIO", "estado": "Disponible", "reservas": 2 }
  ],
  "reservasPorCategoria": [
    { "categoria": "Aulas y salones", "reservas": 2 }
  ]
}
```

| Campo | Cálculo |
|-------|---------|
| `recursosPorEstado` | Recursos agrupados en los cuatro estados del tablero |
| `reservasActivas` | Reservas en estado `Confirmada` |
| `prestamosActivos` | Préstamos no devueltos |
| `devolucionesTardias` | Préstamos vencidos sin devolver o devueltos después de la fecha límite |
| `incidentes` / `incidentesCriticos` | Total de incidentes / con severidad `Crítica` |
| `reservasPorRecurso` | Todos los recursos con su estado actual y el número de reservas (sin canceladas), de mayor a menor |
| `reservasPorCategoria` | Suma de reservas (sin canceladas) por categoría, de mayor a menor |

**200** `Indicadores`.

---

## Usuarios — `/api/usuarios`

Controlador: `UsuarioController` (solo lectura; los usuarios se cargan desde `data.sql`).

```json
{ "idUsuario": 1, "nombre": "Ana Pérez", "tipo": "Estudiante", "codigo": "20231001", "documento": "1012345678", "email": "ana@javeriana.edu.co" }
```

| Método | Ruta | Respuestas |
|--------|------|-----------|
| GET | `/api/usuarios` | 200 `Usuario[]` |
| GET | `/api/usuarios/{id}` | 200, 404 |

---

## Ejemplos con `curl`

```bash
# Espacios disponibles
curl "http://localhost:8080/api/recursos?clase=ESPACIO&disponible=true"

# Verificar disponibilidad antes de reservar
curl "http://localhost:8080/api/reservas/verificar-disponibilidad?recursoId=1&fecha=2026-11-05&horaInicio=08:00&horaFin=10:00"

# Crear reserva
curl -X POST http://localhost:8080/api/reservas -H "Content-Type: application/json" \
  -d '{"recursoId":1,"fecha":"2026-11-05","horaInicio":"08:00","horaFin":"10:00","solicitante":"Ana Pérez"}'

# Registrar entrega de un equipo
curl -X POST http://localhost:8080/api/prestamos/entrega -H "Content-Type: application/json" \
  -d '{"recursoId":7,"usuarioResponsable":"Carlos Gómez","fechaLimite":"2026-10-27","estadoInicial":"Bueno"}'

# Registrar devolución
curl -X PATCH http://localhost:8080/api/prestamos/4/devolucion -H "Content-Type: application/json" \
  -d '{"estadoFinal":"Bueno","observaciones":"Sin novedad"}'

# Reportar incidente crítico (bloquea el recurso)
curl -X POST http://localhost:8080/api/incidentes -H "Content-Type: application/json" \
  -d '{"recursoId":7,"tipoNovedad":"Falla de funcionamiento","severidad":"Crítica","descripcion":"No enciende"}'

# Equipos de una categoría disponibles
curl "http://localhost:8080/api/recursos?clase=EQUIPO&categoriaId=3&disponible=true"

# Pasar una orden de mantenimiento a "En proceso"
curl -X PATCH http://localhost:8080/api/mantenimientos/2/estado -H "Content-Type: application/json" \
  -d '{"estado":"En proceso"}'

# Indicadores del tablero
curl http://localhost:8080/api/indicadores

# Finalizar mantenimiento (habilita el recurso)
curl -X PATCH http://localhost:8080/api/mantenimientos/2/finalizar
```
