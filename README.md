# CampusRecursos

Sistema web para la gestión de reservas, préstamos y mantenimiento de recursos de un campus universitario (Proyecto Desarrollo Web, Pontificia Universidad Javeriana).

El sistema centraliza el catálogo de espacios y equipos, las reservas con validación de conflictos, los préstamos y devoluciones de equipos, el reporte de incidentes, el mantenimiento básico y un tablero con indicadores.

## Arquitectura

| Capa | Tecnología | Ubicación |
|------|------------|-----------|
| Frontend SPA | Angular 22 (standalone components, `HttpClient`, Router) | `cliente-frontend/` |
| API REST | Spring Boot 4.1 · Spring Web · Spring Data JPA | `src/main/java/com/example/proyectoweb/` |
| Documentación de la API | springdoc-openapi (Swagger UI) | `/swagger-ui/index.html` |
| Vistas MPA (Entrega I) | Thymeleaf | `src/main/resources/templates/` |
| Base de datos | H2 en archivo (perfil `dev`), PostgreSQL (perfil `prod`) | `data/`, `application-*.properties` |
| Carga inicial | `data.sql` idempotente | `src/main/resources/data.sql` |

```
 Navegador ──► Angular SPA (localhost:4200)
                    │  HTTP/JSON (CORS habilitado para :4200)
                    ▼
              Spring Boot (localhost:8080)
              Controller ─► Service (reglas RN-01..RN-05) ─► Repository (JPA)
                                                                  │
                                                                  ▼
                                                           H2 / PostgreSQL
```

Más detalle en [docs/ARQUITECTURA-SPA.md](docs/ARQUITECTURA-SPA.md).

## Requisitos

- Java 21
- Node.js **22.22+** (o 24.15+) y npm — exigido por Angular CLI 22. Si `node_modules` se instaló en Windows y se compila desde WSL (o al revés), hay que reinstalarlo con `npm ci` en ese sistema.
- No se necesita instalar Maven: se usa el wrapper `./mvnw`

## Ejecución

### 1. Backend (API REST)

```bash
./mvnw spring-boot:run
```

- API: `http://localhost:8080/api/...`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Consola H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/web_db`, usuario `sa`, sin contraseña)

El perfil activo por defecto es `dev` (H2 persistente en `./data/web_db`). Al arrancar se ejecuta `data.sql`, que carga categorías, ubicaciones, recursos, usuarios y datos de ejemplo sin duplicarlos.

Para usar PostgreSQL:

```bash
DB_URL=jdbc:postgresql://localhost:5432/web_db DB_USERNAME=postgres DB_PASSWORD=postgres \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 2. Frontend (SPA)

```bash
cd cliente-frontend
npm install
npm start          # ng serve
```

Abrir `http://localhost:4200`. La SPA consume la API en `http://localhost:8080/api`.

### 3. Pruebas

```bash
./mvnw test
```

Pruebas de controladores con MockMvc sobre H2 en memoria (`src/test/`): recursos, reservas, préstamos, incidentes/mantenimiento, indicadores y categorías/ubicaciones.

## Módulos de la SPA

| Ruta | Componente | Funcionalidad |
|------|-----------|---------------|
| `/catalogo` | `CatalogoComponent` | Catálogo con búsqueda por texto y filtros por clase, estado, categoría, ubicación y tipo; detalle del recurso con sus próximas reservas |
| `/reservas` | `ReservasComponent` | Crear, consultar, modificar, cancelar y finalizar reservas con verificación de disponibilidad |
| `/prestamos` | `PrestamosComponent` | Entrega y devolución de equipos; pestañas de activos, tardíos y todos |
| `/mantenimiento` | `MantenimientoComponent` | Reporte de incidentes y órdenes de mantenimiento (abrir, iniciar, finalizar, cancelar) |
| `/tablero` | `TableroComponent` | Indicadores: recursos por estado, reservas por recurso y categoría, préstamos activos, devoluciones tardías e incidentes |

## Reglas de negocio

| Regla | Descripción | Dónde se valida |
|-------|-------------|-----------------|
| RN-01 | No puede haber dos reservas activas solapadas para el mismo recurso | `ReservaService.buscarConflicto` → HTTP 409 |
| RN-02 | Un recurso bloqueado no puede reservarse ni prestarse | `ReservaService`, `PrestamoService` → HTTP 409 |
| RN-03 | Un equipo prestado no puede entregarse de nuevo hasta su devolución | `PrestamoService.registrarEntrega` → HTTP 409 |
| RN-04 | Un incidente de severidad *Crítica* bloquea el recurso | `IncidenteService.registrarIncidente` |
| RN-05 | Finalizar un mantenimiento deja el recurso *Disponible* | `MantenimientoService.finalizar` |

## Documentación

- [docs/API-REST.md](docs/API-REST.md) — referencia completa de endpoints, cuerpos, respuestas y códigos de error.
- [docs/ARQUITECTURA-SPA.md](docs/ARQUITECTURA-SPA.md) — diseño de servicios REST y arquitectura SPA.
- `Incidentes CampusRecursos.postman_collection.json` — colección Postman para probar incidentes.

## Estructura del repositorio

```
.
├── cliente-frontend/           SPA Angular
│   └── src/app/
│       ├── components/         catalogo, reservas, prestamos, mantenimiento, tablero
│       └── services/           clientes HTTP de la API (recurso, reserva, prestamo, indicador, ...)
├── src/main/java/com/example/proyectoweb/
│   ├── config/                 CORS (WebConfig) y OpenAPI (OpenApiConfig)
│   ├── controller/             controladores REST (/api/...) y MPA (Thymeleaf)
│   ├── dto/                    objetos de entrada de la API
│   ├── model/                  entidades JPA
│   ├── repository/             repositorios Spring Data
│   └── service/                lógica y reglas de negocio
├── src/main/resources/
│   ├── data.sql                carga inicial
│   ├── templates/              vistas MPA de la Entrega I
│   └── application*.properties perfiles dev / h2 / prod
├── src/test/                   pruebas de controladores
└── docs/                       documentación del proyecto
```

> La autenticación y autorización por roles se implementan en la tercera entrega, según el enunciado.
