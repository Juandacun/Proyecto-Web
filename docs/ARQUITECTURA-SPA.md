# Diseño de servicios REST y arquitectura SPA

Documento de diseño de la segunda entrega: cómo está construida la aplicación de una sola página (SPA) y cómo se diseñaron los servicios REST que la soportan.

## 1. Visión general

```
┌──────────────────────────── Navegador ────────────────────────────┐
│  Angular SPA (http://localhost:4200)                              │
│                                                                   │
│  AppComponent (sidebar + topbar + <router-outlet>)                │
│     ├── /catalogo       CatalogoComponent                         │
│     ├── /reservas       ReservasComponent                         │
│     ├── /prestamos      PrestamosComponent                        │
│     ├── /mantenimiento  MantenimientoComponent                    │
│     └── /tablero        TableroComponent                          │
│                 │ inyectan                                        │
│                 ▼                                                 │
│  Servicios HTTP: RecursoService, ReservaService, PrestamoService, │
│                  IncidenteService, MantenimientoService,          │
│                  IndicadorService, CategoriaService,              │
│                  UbicacionService, UsuarioService                 │
└─────────────────┬─────────────────────────────────────────────────┘
                  │ JSON sobre HTTP (GET/POST/PUT/PATCH/DELETE)
                  ▼
┌──────────────── Spring Boot (http://localhost:8080) ───────────────┐
│  WebConfig (CORS)   OpenApiConfig (Swagger)                        │
│                                                                    │
│  @RestController  /api/recursos, /api/categorias, /api/ubicaciones,│
│                   /api/reservas, /api/prestamos, /api/incidentes,  │
│                   /api/mantenimientos, /api/usuarios,              │
│                   /api/indicadores                                 │
│        │                                                           │
│        ▼                                                           │
│  @Service   reglas de negocio RN-01 … RN-05, validaciones,         │
│             transiciones de estado del recurso                     │
│        │                                                           │
│        ▼                                                           │
│  JpaRepository  ──►  H2 (dev) / PostgreSQL (prod)                  │
└────────────────────────────────────────────────────────────────────┘
```

El cliente y el servidor son proyectos independientes que se comunican solo por la API REST. La SPA no renderiza nada en el servidor: carga una vez y luego cambia de vista con el router de Angular, pidiendo datos en JSON.

## 2. Arquitectura del frontend (SPA)

### 2.1 Tecnología

- Angular 22 con **componentes standalone** (sin `NgModule`).
- `provideRouter(routes)` para la navegación y `provideHttpClient()` para las llamadas REST (`src/app/app.config.ts`).
- `FormsModule` (`ngModel`) para los formularios.
- RxJS para manejar respuestas asíncronas (`subscribe`, `switchMap`, `catchError`, `Subject`).

### 2.2 Enrutamiento

`src/app/app.routes.ts`:

| Ruta | Componente | Usuario principal |
|------|-----------|-------------------|
| `''` | redirige a `catalogo` | — |
| `catalogo` | `CatalogoComponent` | Solicitante / gestor |
| `reservas` | `ReservasComponent` | Solicitante |
| `prestamos` | `PrestamosComponent` | Gestor de recursos |
| `mantenimiento` | `MantenimientoComponent` | Gestor de recursos |
| `tablero` | `TableroComponent` | Gestor de recursos |
| `**` | redirige a `catalogo` | — |

La barra lateral (`app.html`) usa `routerLink` y `routerLinkActive`, así que cambiar de módulo no recarga la página.

### 2.3 Capas del cliente

| Capa | Responsabilidad | Archivos |
|------|-----------------|----------|
| Componentes | Estado de la vista, formularios, mensajes de éxito/error, filtros en memoria | `components/*/*.ts|html|css` |
| Servicios | Un servicio por recurso REST; arma URLs y `HttpParams`, tipa DTOs | `services/*.ts` |
| Interfaces | Contratos de datos (`Reserva`, `ReservaDTO`, `Disponibilidad`, `PrestamoEntregaDTO`, `IncidenteDTO`, `MantenimientoDTO`, `Indicadores`, `Categoria`, `Ubicacion`, `Usuario`) | dentro de cada servicio |

Los componentes nunca usan `HttpClient` directamente: siempre pasan por un servicio inyectable (`providedIn: 'root'`).

### 2.4 Mapa componente → servicio → endpoint

| Componente | Acción | Servicio | Endpoint |
|-----------|--------|----------|----------|
| Catálogo | Listar y filtrar recursos | `RecursoService.obtenerRecursos` | `GET /api/recursos?clase&estado&categoriaId&ubicacionId&tipo` |
| | Opciones de los filtros | `CategoriaService.listar`, `UbicacionService.listar` | `GET /api/categorias`, `GET /api/ubicaciones` |
| | Detalle y próximas reservas | `ReservaService.listar` | `GET /api/reservas?recursoId&estado=Confirmada` |
| Reservas | Listar reservas | `ReservaService.listar` | `GET /api/reservas` |
| | Cargar espacios | `RecursoService.obtenerRecursos({clase:'ESPACIO'})` | `GET /api/recursos?clase=ESPACIO` |
| | Cargar solicitantes | `UsuarioService.listar` | `GET /api/usuarios` |
| | Validar horario en vivo | `ReservaService.verificarDisponibilidad` | `GET /api/reservas/verificar-disponibilidad` |
| | Ver ocupación del día | `ReservaService.listarPorRecursoYFecha` | `GET /api/reservas?recursoId&fecha` |
| | Crear / modificar | `ReservaService.crear` / `actualizar` | `POST /api/reservas` / `PUT /api/reservas/{id}` |
| | Cancelar / finalizar | `ReservaService.cancelar` / `finalizar` | `PATCH /api/reservas/{id}/cancelar` / `finalizar` |
| Préstamos | Pestañas activos / tardíos / todos | `PrestamoService.listarActivos` / `listarTardios` / `listar` | `GET /api/prestamos/activos` / `tardios` / `GET /api/prestamos` |
| | Cargar equipos | `RecursoService.listarEquipos` | `GET /api/recursos/equipos` |
| | Registrar entrega | `PrestamoService.registrarEntrega` | `POST /api/prestamos/entrega` |
| | Registrar devolución | `PrestamoService.registrarDevolucion` | `PATCH /api/prestamos/{id}/devolucion` |
| Incidentes y mantenimiento | Listar recursos | `RecursoService.listar` | `GET /api/recursos` |
| | Listar / reportar incidentes | `IncidenteService.listar` / `registrar` | `GET` / `POST /api/incidentes` |
| | Listar / abrir órdenes | `MantenimientoService.listar` / `crearOrden` | `GET` / `POST /api/mantenimientos` |
| | Finalizar orden | `MantenimientoService.finalizar` | `PATCH /api/mantenimientos/{id}/finalizar` |
| | Iniciar / cancelar orden | `MantenimientoService.cambiarEstado` | `PATCH /api/mantenimientos/{id}/estado` |
| Tablero | Indicadores y estado actual | `IndicadorService.obtener` | `GET /api/indicadores` |

### 2.5 Manejo de errores en la interfaz

Los servicios devuelven `Observable`; cada componente se suscribe con `next` / `error`. En el `error` se lee `err.error.error` (mensaje del servidor) y se muestra al usuario. Así, las violaciones de RN-01/RN-02/RN-03 (HTTP 409) aparecen como mensajes claros, cumpliendo el requisito de "mensajes claros de éxito o error".

### 2.6 Diseño responsive

El layout (`app.css`) convierte el menú lateral en una barra superior desplazable por debajo de 760 px. Cada módulo apila sus filtros y formularios, y las tablas se desplazan horizontalmente dentro de su tarjeta (en reservas y tablero se muestran como fichas).

## 3. Diseño de los servicios REST

### 3.1 Principios

1. **Recursos como sustantivos en plural** bajo el prefijo `/api`: `/api/reservas`, `/api/prestamos`, …
2. **Verbos HTTP con su semántica:**
   - `GET` consulta, sin efectos secundarios (incluido `verificar-disponibilidad`).
   - `POST` crea y responde `201 Created` con cabecera `Location`.
   - `PUT` reemplaza los datos editables de un elemento.
   - `PATCH` sobre sub-recursos de acción para **transiciones de estado**: `/{id}/cancelar`, `/{id}/finalizar`, `/{id}/devolucion`, `/{id}/estado`.
   - Los indicadores se exponen como un recurso de solo lectura (`GET /api/indicadores`) calculado en el servidor, para que la SPA no tenga que descargar todas las reservas y préstamos para contarlos.
   - `DELETE` elimina y responde `204 No Content`.
3. **Filtros por query string** en los listados (`?recursoId=&fecha=&estado=`), combinables.
4. **Errores uniformes** en JSON `{ "error": "...", "codigo": "RN-0X" }` con el código HTTP adecuado (`400`, `404`, `409`).
5. **Validación en el servidor:** todas las reglas de negocio se aplican en la capa `@Service`; la SPA solo anticipa (por ejemplo con `verificar-disponibilidad`), nunca decide.
6. **DTOs de entrada** (`ReservaDTO`, `PrestamoEntregaDTO`, `PrestamoDevolucionDTO`, `IncidenteDTO`, `MantenimientoDTO`) para no exponer la entidad JPA a escritura directa; las respuestas devuelven la entidad con sus relaciones (`recurso`, `categoria`, `ubicacion`) para que la SPA no tenga que hacer peticiones adicionales.
7. **Documentación automática** con anotaciones OpenAPI (`@Operation`, `@ApiResponse`, `@Parameter`) visibles en Swagger UI.

### 3.2 Capas del backend

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Configuración | `config` | `WebConfig` (CORS para `:4200`), `OpenApiConfig` (metadatos Swagger) |
| Controladores REST | `controller` | Mapear HTTP ↔ servicio, traducir excepciones a códigos de estado |
| DTO | `dto` | Contratos de entrada de la API |
| Servicios | `service` | Reglas de negocio, validaciones, transiciones de estado |
| Repositorios | `repository` | Acceso a datos con Spring Data JPA y consultas derivadas |
| Modelo | `model` | Entidades JPA |

Traducción de excepciones en los controladores:

| Excepción del servicio | Significado | HTTP |
|------------------------|-------------|------|
| `IllegalArgumentException` | Dato inválido o faltante | 400 |
| `IllegalStateException` | Regla de negocio o estado incompatible | 409 |
| `null` devuelto por el servicio | Elemento inexistente | 404 |

### 3.3 Modelo de datos

```
Categoria 1 ──── * Recurso * ──── 1 Ubicacion
                     │
     ┌───────────────┼────────────────┬──────────────────┐
     * Reserva       * Prestamo       * Incidente        * Mantenimiento

Usuario (catálogo de solicitantes/responsables)
```

| Entidad | Campos principales |
|---------|-------------------|
| `Recurso` | nombre, tipo, descripción, características, estado, clase (`EQUIPO`/`ESPACIO`), categoría, ubicación |
| `Reserva` | recurso, fecha, horaInicio, horaFin, motivo, estado, solicitante |
| `Prestamo` | recurso, documento, usuarioResponsable, fechaEntrega, fechaDevolucionEstimada, estadoEquipo, accesorios, observaciones, estado, fechaDevolucion, puntualidad |
| `Incidente` | recurso, tipo, severidad, descripción, fecha, reportadoPor |
| `Mantenimiento` | recurso, fechaApertura, diagnóstico, estado, fechaCierre, idUsuarioResponsable, observaciones |
| `Categoria` / `Ubicacion` | nombre, descripción (+ edificio en ubicación) |
| `Usuario` | nombre, tipo, código, documento, email |

Estados válidos del recurso: `Disponible`, `Reservado`, `Prestado`, `Bloqueado`. Estados de la orden de mantenimiento: `Abierta` → `En proceso` → `Finalizada` | `Cancelada`.

### 3.4 Ciclo de vida del estado de un recurso

```
                 POST /reservas                 PATCH /reservas/{id}/cancelar|finalizar
   Disponible ──────────────────► Reservado ──────────────────────────────► Disponible
       │                                       (si no quedan reservas activas)
       │ POST /prestamos/entrega                PATCH /prestamos/{id}/devolucion
       ├──────────────────────────► Prestado ─────────────────────────────► Disponible
       │
       │ POST /incidentes (Crítica)  ─┐
       │ POST /mantenimientos        ─┼──► Bloqueado ── PATCH /mantenimientos/{id}/finalizar ──► Disponible
       │ PATCH /recursos/{id}/estado ─┘                   (o PATCH /recursos/{id}/estado)
```

### 3.5 Reglas de negocio en el servidor

| Regla | Implementación | Respuesta |
|-------|----------------|-----------|
| RN-01 Sin reservas solapadas | `ReservaService.buscarConflicto`: busca reservas `Confirmada` del mismo recurso y fecha con `inicioA < finB && inicioB < finA`, excluyendo la propia al editar | 409 `RN-01` |
| RN-02 Bloqueado no se reserva ni presta | `Recurso.estaBloqueado()` revisado en `ReservaService` y `PrestamoService` | 409 `RN-02` |
| RN-03 Prestado no se entrega de nuevo | `Recurso.estaPrestado()` + `PrestamoRepository.findActivosPorRecurso` | 409 `RN-03` |
| RN-04 Incidente crítico bloquea | `IncidenteService.registrarIncidente` cambia el recurso a `Bloqueado` | 201 |
| RN-05 Mantenimiento finalizado habilita | `MantenimientoService.finalizar` cambia el recurso a `Disponible` | 200 |

Reglas adicionales del enunciado:

- Modificar y cancelar una reserva solo **antes de su inicio** y si está `Confirmada` (`ReservaService.validarEditable`).
- Devolución **puntual o tardía** comparando la fecha real con la fecha límite (`PrestamoService.registrarDevolucion`).
- Solo se reservan `ESPACIO`s y solo se prestan `EQUIPO`s.

### 3.6 Configuración transversal

- **CORS:** `WebConfig` permite el origen `http://localhost:4200` y los métodos usados; además cada controlador lleva `@CrossOrigin`.
- **Perfiles:** `dev` (H2 en archivo, carga `data.sql`), `h2` (memoria), `prod` (PostgreSQL por variables de entorno).
- **Swagger:** `springdoc-openapi-starter-webmvc-ui` expone `/swagger-ui/index.html` y `/v3/api-docs`.

## 4. Pruebas

Pruebas de controladores con `MockMvc` (`src/test/java/.../controller`):

| Clase | Qué cubre |
|-------|-----------|
| `RecursoControllerTest` | CRUD de recursos, filtros combinables (categoría, ubicación, tipo, clase, estado), cambio de estado |
| `ReservaControllerTest` | Creación, RN-01, RN-02, edición, cancelación, verificación de disponibilidad |
| `PrestamoControllerTest` | Entrega, RN-02, RN-03, devolución puntual/tardía |
| `IncidenteMantenimientoTest` | RN-04 (bloqueo por incidente crítico), RN-05 (habilitación) y cambios de estado de la orden |
| `IndicadorControllerTest` | Estructura de los indicadores y actualización tras un incidente crítico |
| `CategoriaUbicacionTest` | CRUD de categorías y ubicaciones |

Se ejecutan con `./mvnw test`.

## 5. Seguridad (pendiente para la tercera entrega)

Según el enunciado, la autenticación y autorización por roles no se implementa en esta entrega. El diseño ya separa las acciones por rol para añadir la seguridad después:

| Rol | Endpoints que usará |
|-----|---------------------|
| Solicitante | `GET /api/recursos*`, `GET/POST/PUT /api/reservas`, `PATCH /api/reservas/{id}/cancelar`, `GET /api/prestamos?usuarioId=`, `POST /api/incidentes` |
| Gestor de recursos | Todo lo anterior + `/api/prestamos/**`, `/api/mantenimientos/**`, `/api/indicadores`, `PATCH /api/recursos/{id}/estado`, `PATCH /api/reservas/{id}/finalizar` |
| Administrador | Todo lo anterior + CRUD de `/api/recursos`, `/api/categorias`, `/api/ubicaciones`, `/api/usuarios` |
