# Infrastructure — Reglas de capa

## Responsabilidad

La capa `infrastructure` contiene los detalles técnicos que conectan el sistema con mecanismos externos.

Incluye:

* Controllers REST.
* DTOs request y response.
* Mappers REST.
* Persistencia con JPA.
* Repositories Spring Data.
* Adapters que implementan puertos de salida.
* Integraciones con APIs externas.
* Mensajería.
* Seguridad técnica.
* Configuración de frameworks.
* Manejo de errores HTTP.

Su responsabilidad es adaptar datos y tecnologías externas a los contratos definidos por `application` y `domain`.

No debe:

* Definir reglas de negocio.
* Implementar casos de uso.
* Reproducir comportamiento de entidades.
* Hacer que `application` o `domain` dependan de Spring, JPA, HTTP, JWT o proveedores externos.

> Infrastructure traduce y conecta. Application coordina. Domain protege las reglas de negocio.

---

## Dependencias

`infrastructure`:

* Puede depender de `application`.
* Puede depender de `domain`.
* Puede depender de frameworks y librerías externas.
* No puede ser dependencia de `application`.
* No puede ser dependencia de `domain`.

Los contratos internos no deben exponer:

* Entidades JPA.
* DTOs REST.
* Tipos HTTP.
* Repositories Spring Data.
* Clases de seguridad de Spring.
* DTOs de proveedores externos.
* Excepciones concretas de infraestructura.

---

## Organización

Estructura recomendada:

```text
infrastructure
├── adapter
│   ├── in
│   │   ├── rest
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   │   ├── request
│   │   │   │   └── response
│   │   │   ├── mapper
│   │   │   └── error
│   │   └── security
│   └── out
│       ├── persistence
│       │   ├── adapter
│       │   ├── entity
│       │   ├── mapper
│       │   └── repository
│       └── external
│           ├── adapter
│           ├── client
│           ├── dto
│           └── mapper
└── configuration
    ├── security
    ├── web
    ├── persistence
    └── openapi
```

No deben crearse carpetas vacías.

Las clases dedicadas únicamente a crear o conectar beans deben ubicarse en `configuration` o en `boot`, no dentro de `adapter/in` o `adapter/out`.

---

# Adapter In — REST

## Controllers

Los controllers son adapters de entrada.

Deben:

* Recibir requests HTTP.
* Validar el contrato superficial con Bean Validation.
* Obtener parámetros de ruta o consulta.
* Obtener el actor autenticado cuando sea necesario.
* Convertir los datos externos a `Command`.
* Invocar un `UseCase`.
* Convertir el resultado a un DTO response.
* Devolver el código HTTP correspondiente.

Flujo:

```text
HTTP Request
    ↓
Request DTO + parámetros + ActorContext
        ↓
REST Mapper
        ↓
Command
        ↓
UseCase
        ↓
Domain
    ↓
Response DTO
    ↓
HTTP Response
```

Los controllers no deben:

* Consultar repositories.
* Acceder directamente a JPA.
* Implementar reglas de negocio.
* Modificar entidades.
* Resolver manualmente transiciones de estado.
* Aplicar autorización de negocio completa.
* Acceder directamente a clientes externos.
* Recibir implementaciones concretas cuando existe un `UseCase`.
* Contener conversiones extensas.
* Repetir manejo de excepciones mediante `try-catch`.

Ejemplo:

```java
@RestController
@RequestMapping("/api/columnas")
@RequiredArgsConstructor
public class ColumnaController {

    private final CreateColumnaUseCase createUseCase;

    @PostMapping("/create")
    public ResponseEntity<ColumnaResponse> create(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody CreateColumnaRequest request
    ) {
        CreateColumnaCommand command =
                ColumnaRestMapper.toCommand(request, actor);

        Columna columna = createUseCase.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ColumnaRestMapper.toResponse(columna));
    }
}
```

---

## DTO Request

Los DTO request representan exclusivamente el contrato HTTP de entrada.

Deben:

* Ubicarse en `adapter/in/rest/dto/request`.
* Preferentemente implementarse como `record`.
* Contener únicamente datos recibidos del cliente.
* Utilizar Bean Validation para validaciones superficiales.
* Permanecer independientes de JPA.
* No utilizarse como `Command`.
* No invocar UseCases, ports o repositories.
* No contener reglas de negocio.

Ejemplo:

```java
public record CreateColumnaRequest(
        @NotBlank String nombre,
        String color,
        TipoTablero tipoTablero,
        TipoColumna tipoColumna
) {
}
```

Son validaciones superficiales:

* Campo obligatorio.
* Cadena vacía.
* Longitud máxima.
* Formato de correo, fecha o UUID.
* Rango básico del request.
* Estructura válida del JSON.

La validación del request no sustituye las reglas internas de las capas inferiores.

---

## Búsqueda actor-scoped: pushdown atómico, sin endpoint REST

Cuando una capacidad de búsqueda (típicamente `find_contacts` para un agente o asistente) es solicitada por una herramienta o por un orquestador — no por un cliente HTTP — la capa de infraestructura NO debe exponer un endpoint REST específico para esa búsqueda. La consulta es **tool-only** o **invocable sólo por orquestación interna**, y el actor que la dispara proviene del `ActorContext` autenticado, nunca del cuerpo del request ni del esquema visible de la herramienta.

Reglas:

* No se crean controllers, DTOs request, mappers REST de entrada específicos ni clases `*MvcTest` / `*IT` para una búsqueda tool-only. La superficie de entrada es el puerto de entrada de `application` (`GetAllContactosUseCase`), y la consulta de persistencia se materializa mediante el puerto de salida (`SearchContactosPort`) y su adaptador.
* El actor se obtiene desde el contexto confiable fuera del esquema visible: para una herramienta de IA, el actor es capturado por la factoría de callbacks y pasado como argumento del `Command` al construirlo; nunca llega por un argumento visible al modelo.
* El adaptador de salida aplica el predicado de alcance `WHERE (creado_por = :actor OR responsable_id = :actor)` como parte de la misma consulta que aplica los filtros opcionales y el orden. El alcance NUNCA se evalúa en `application` y NUNCA se construye en Java a partir de comparaciones entre `actor` y otros identificadores.
* La consulta se ejecuta en una sola operación pushdown contra la base de datos: alcance + filtros opcionales + `ORDER BY creado_en DESC, id ASC` + `LIMIT` (cuando el caso de uso lo solicite) en una sola sentencia.
* El límite del resultado, cuando aplica, se aplica en la base de datos (vía `Pageable` o equivalente) antes de la materialización; no se utiliza `repository.findAll()` ni `Stream.filter(...)` en Java para implementar el alcance, los filtros opcionales o el `LIMIT`.

## No debe

* Crear `ContactoController.getAll(...)`, `ContactoController.getAll()` (sobrecarga Java sin anotación Spring) ni cualquier método anotado con `@GetMapping` cuyo único propósito sea exponer `find_contacts` por HTTP. La búsqueda contact-only vive en el adaptador de salida; la ruta HTTP, cuando exista para otros fines, no la incluye.
* Definir un `GetAllContactosRequest` (o cualquier DTO request equivalente) ni un método `ContactoCommandMapper.toGetAllCommand(...)` cuya única razón de existir sea alimentar un endpoint REST de búsqueda tool-only.
* Mantener clases `ContactoControllerMvcTest` o `ContactoCommandMapperTest` dedicadas exclusivamente a ese endpoint o mapeo eliminado. Si una clase cubre únicamente get-all/get-by-all, se elimina con él.
* Recibir el `actorUsuarioId` por un campo libre del request, por un argumento visible del modelo de IA, ni por un campo controlado por el cliente. El actor lo aporta el `ActorContext` o un callback de orquestación confiable.
* Aplicar el alcance en `application` con `if (actor.equals(responsableId)) ...` o equivalentes; el alcance se aplica en la base de datos, en la misma consulta pushdown.
* Cargar todo el conjunto con `repository.findAll()` y filtrar en `Stream.filter(...)`; la consulta debe ser atómica en SQL/JPQL con `LIMIT` cuando se solicite.
* Reemplazar el alcance del actor por un filtro opcional equivalente (por ejemplo, `responsableId == actor` en el `Service`); el alcance siempre se aplica primero y se intersecta con los filtros opcionales.

---

## Identidad autenticada

Los identificadores del actor autenticado no deben recibirse libremente desde el request.

Ejemplos:

* `creadoPor`.
* `actualizadoPor`.
* `solicitadoPor`.
* `confirmadoPor`.

Deben obtenerse desde `ActorContext` y agregarse al `Command`.

Incorrecto:

```java
public record CreateEmpresaRequest(
        String nombre,
        UUID creadoPor
) {
}
```

Correcto:

```java
CreateEmpresaCommand command =
        EmpresaRestMapper.toCommand(request, actorContext);
```

Los identificadores que representan referencias seleccionadas por el cliente sí pueden recibirse.

Ejemplos:

* `responsableId`.
* `empresaId`.
* `tratoId`.
* `contactoId`.
* `columnaId`.

---

## DTO Response

Los DTO response representan exclusivamente el contrato HTTP de salida.

Deben:

* Ubicarse en `adapter/in/rest/dto/response`.
* Exponer únicamente los datos requeridos por el cliente.
* No exponer entidades JPA.
* No contener reglas de negocio.
* No ser utilizados por `application`.
* No utilizarse como modelos de persistencia.

Ejemplo:

```java
public record ColumnaResponse(
        UUID id,
        String nombre,
        String color,
        String tipoTablero,
        String tipoColumna
) {
}
```

La conversión puede realizarse mediante:

```java
ColumnaRestMapper.toResponse(columna);
```

o mediante:

```java
ColumnaResponse.fromDomain(columna);
```

Ambas opciones son válidas cuando se utiliza una convención consistente.

Debe preferirse un mapper separado cuando la conversión:

* Combina varios objetos.
* Incluye colecciones.
* Requiere extraer múltiples Value Objects.
* Produce diferentes Responses.
* Es suficientemente grande para ensuciar el DTO o el controller.

---

# Mappers REST

## Responsabilidad

Los mappers REST convierten contratos externos en contratos internos y viceversa.

Flujo de entrada:

```text
Request DTO
+ path/query parameters
+ ActorContext
        ↓
Command
```

Flujo de salida:

```text
Domain Entity
        ↓
Response DTO
```

Ejemplo:

```java
public final class ColumnaRestMapper {

    private ColumnaRestMapper() {
    }

    public static CreateColumnaCommand toCommand(
            CreateColumnaRequest request,
            ActorContext actor
    ) {
        return new CreateColumnaCommand(
                actor.requireUsuarioId(),
                request.nombre(),
                request.color(),
                request.tipoTablero(),
                request.tipoColumna()
        );
    }

    public static ColumnaResponse toResponse(
            Columna columna
    ) {
        return new ColumnaResponse(
                columna.getId().value(),
                columna.getColumnanombre(),
                columna.getColor(),
                columna.getTipoTablero().name(),
                columna.getTipoColumna().name()
        );
    }
}
```

Los mappers pueden:

* Combinar Request, parámetros y `ActorContext`.
* Convertir tipos equivalentes.
* Extraer valores de Value Objects.
* Convertir colecciones.
* Convertir entidades de dominio a Responses.

Los mappers no deben:

* Consultar repositories.
* Invocar UseCases.
* Invocar puertos.
* Aplicar autorización.
* Comprobar existencia.
* Implementar reglas de negocio.
* Modificar entidades.
* Consultar estado externo.
* Ocultar excepciones internas.
* Inventar valores de negocio.

El mapeo debe ser determinista: los mismos datos de entrada deben producir el mismo resultado.

No deben conservarse métodos de mapeo que no sean utilizados.

---

# Manejo de errores REST

`GlobalExceptionHandler` o `@RestControllerAdvice` debe traducir errores internos a respuestas HTTP.

Debe encargarse de:

* Errores de Bean Validation.
* Excepciones de `application`.
* Excepciones de dominio.
* Errores de autenticación.
* Errores de autorización.
* Errores inesperados.

Debe definir:

* Código HTTP.
* Estructura uniforme de error.
* Mensaje público.
* Identificador de seguimiento cuando corresponda.

Los controllers no deben repetir bloques `try-catch` para transformar las mismas excepciones.

No deben exponerse:

* Stack traces.
* SQL.
* Tablas o constraints internos.
* Datos sensibles.
* Excepciones de Hibernate.
* Respuestas completas de proveedores externos.

---

# Adapter Out — Persistencia

## Repository Adapter

El repository adapter implementa los puertos de salida definidos en `application`.

Flujo:

```text
Application Port
    ↓
Repository Adapter
    ↓
Persistence Mapper
    ↓
JPA Entity
    ↓
Spring Data Repository
```

Para operaciones de lectura:

```text
Spring Data Repository
    ↓
JPA Entity
    ↓
Persistence Mapper
    ↓
Domain Entity
```

Debe:

* Implementar uno o varios puertos relacionados.
* Convertir dominio a persistencia.
* Ejecutar operaciones con Spring Data.
* Convertir persistencia a dominio.
* Respetar exactamente el contrato del puerto.
* Mantener aislados los detalles de JPA.

Ejemplo:

```java
@RequiredArgsConstructor
public class ColumnaRepositoryAdapter
        implements SaveColumnaPort,
                   FindColumnaByIdPort {

    private final ColumnaRepository repository;

    @Override
    public Columna save(Columna columna) {
        ColumnaEntity entity =
                ColumnaPersistenceMapper.toEntity(columna);

        ColumnaEntity saved = repository.save(entity);

        return ColumnaPersistenceMapper.toDomain(saved);
    }
}
```

Un adapter puede implementar varios puertos granulares cuando:

* Pertenecen al mismo contexto.
* Utilizan la misma tecnología.
* La clase mantiene cohesión.
* No coordina reglas de negocio.

No es obligatorio crear un adapter por cada puerto.

---

## Consultas con varios repositories

Un repository adapter puede utilizar más de un repository técnico cuando sea necesario para implementar una consulta concreta.

Ejemplo:

```java
private final ColumnaRepository columnaRepository;
private final TableroRepository tableroRepository;
```

Esto es válido si:

* La operación sigue siendo técnica.
* Implementa exactamente un contrato de `application`.
* No coordina comportamiento de varios agregados.
* No aplica decisiones de negocio.

Si la clase pierde cohesión, debe extraerse un adapter de consulta específico.

Ejemplo:

```text
ColumnaAssignmentLookupAdapter
    implements ExistsColumnaAsignadaPort
```

---

## Repositories Spring Data

Los repositories Spring Data:

* Permanecen exclusivamente en infraestructura.
* Trabajan con entidades de persistencia.
* Pueden declarar consultas derivadas, JPQL o SQL nativo.
* No contienen reglas de negocio.
* No deben utilizarse directamente desde controllers.
* No deben inyectarse en Services de `application`.

Ejemplo:

```java
public interface ColumnaRepository
        extends JpaRepository<ColumnaEntity, String> {
}
```

---

## Entidades de persistencia

Las entidades JPA representan la estructura almacenada.

Deben:

* Contener anotaciones JPA.
* Permanecer dentro de infraestructura.
* Representar tablas, columnas y relaciones.
* Convertirse mediante mappers.

No deben:

* Reemplazar entidades de dominio.
* Ser devueltas por UseCases.
* Llegar a controllers.
* Utilizarse como Commands o Responses.
* Implementar comportamiento de negocio.

---

# Mappers de persistencia

Los mappers de persistencia convierten:

```text
Domain → JPA Entity
JPA Entity → Domain
```

Deben:

* Conservar identidad.
* Conservar estado.
* Conservar relaciones.
* Conservar timestamps persistidos.
* Utilizar el mecanismo de reconstitución del dominio.

No deben:

* Consultar repositories.
* Invocar UseCases.
* Aplicar autorización.
* Ejecutar reglas de negocio.
* Generar nuevos IDs durante una lectura.
* Reemplazar timestamps por la hora actual.
* Utilizar una factory de creación para reconstruir una entidad existente.

Ejemplo:

```java
public final class ColumnaPersistenceMapper {

    private ColumnaPersistenceMapper() {
    }

    public static ColumnaEntity toEntity(
            Columna columna
    ) {
        // Conversión técnica.
    }

    public static Columna toDomain(
            ColumnaEntity entity
    ) {
        return Columna.reconstitute(
                // Estado persistido.
        );
    }
}
```

Para reconstruir una entidad existente debe utilizarse:

```java
Columna.reconstitute(...);
```

No:

```java
Columna.create(...);
```

si `create()` genera nueva identidad, timestamps o estado inicial.

---

# Seguridad

La seguridad técnica pertenece a infraestructura.

## Adapter de entrada de seguridad

Incluye:

* Filtros JWT.
* Conversores de autenticación.
* Resolvers de `ActorContext`.
* Authentication providers.
* Entry points.
* Access denied handlers.

Puede:

* Leer `SecurityContext`.
* Extraer el subject autenticado.
* Extraer identificadores y authorities.
* Construir `ActorContext`.
* Rechazar peticiones no autenticadas.

No debe:

* Enviar `Jwt`, `Authentication` o `SecurityContext` a `application`.
* Permitir que el cliente controle la identidad autenticada.
* Concentrar toda la autorización de negocio en filtros o controllers.

## Configuración de seguridad

Incluye:

* `SecurityConfig`.
* `SecurityFilterChain`.
* Configuración CORS.
* Beans de seguridad.

Debe ubicarse en:

```text
configuration/security
```

o en `boot`, cuando este sea el composition root.

No debe ubicarse dentro de:

```text
adapter/out/security
```

salvo que exista una integración saliente real con un proveedor de identidad.

---

# Configuración

Las clases de configuración crean y conectan componentes técnicos.

Ejemplos:

* `SecurityConfig`.
* `CorsConfig`.
* `OpenApiConfig`.
* `ActorContextConfiguration`.
* Configuración de clientes HTTP.
* Configuración de persistencia.
* Registro de implementaciones de UseCases y ports.

Deben ubicarse en:

```text
infrastructure/configuration
```

o en `boot/configuration`.

No son adapters de entrada ni de salida.

No deben contener reglas de negocio.

---

# Testing

Las pruebas de infraestructura pueden levantar Spring cuando se valida una integración técnica.

## Adapter In

Debe verificarse:

* Validación del Request.
* Mapeo de Request a Command.
* Incorporación del `ActorContext`.
* Invocación del UseCase correcto.
* Mapeo del resultado a Response.
* Código HTTP.
* Manejo de excepciones.
* Seguridad del endpoint.

Se puede utilizar:

* `MockMvc`.
* Tests MVC.
* Spring Security Test.

## Adapter Out

Debe verificarse:

* Implementación correcta del puerto.
* Mapeo dominio–persistencia.
* Reconstitución.
* Consultas Spring Data.
* Restricciones de base de datos.
* Integraciones HTTP externas.
* Traducción de errores técnicos.

Puede utilizarse:

* Testcontainers.
* Tests de integración JPA.
* Servidores HTTP simulados para APIs externas.

---

# Checklist

Antes de aprobar código de `infrastructure`, comprobar:

* [ ] Los controllers dependen de UseCases.
* [ ] Los controllers no consultan repositories.
* [ ] Los Requests y Responses son DTOs exclusivos de REST.
* [ ] Los DTOs REST no se utilizan como Commands.
* [ ] Request, parámetros y ActorContext se convierten mediante un mapper.
* [ ] La identidad autenticada no se recibe libremente desde el cliente.
* [ ] Los mappers no consultan estado externo.
* [ ] Los mappers no contienen reglas de negocio.
* [ ] `GlobalExceptionHandler` centraliza la traducción a HTTP.
* [ ] Los repository adapters implementan puertos de `application`.
* [ ] Los repositories Spring Data permanecen en infraestructura.
* [ ] Las entidades JPA están separadas del dominio.
* [ ] Existe mapeo explícito entre dominio y persistencia.
* [ ] La reconstitución conserva identidad, estado y timestamps.
* [ ] Los adapters externos aíslan HTTP y DTOs del proveedor.
* [ ] JWT, `Authentication` y `SecurityContext` no llegan a `application`.
* [ ] Las clases de configuración no están dentro de `adapter/out`.
* [ ] La infraestructura no implementa reglas de negocio.

---

# Flujos obligatorios

## REST de escritura

```text
Request DTO
+ parámetros
+ ActorContext
        ↓
REST Mapper
        ↓
Command
        ↓
UseCase
        ↓
Domain
        ↓
Response Mapper
        ↓
Response DTO
```

## REST de lectura

```text
Path / Query parameters
+ ActorContext cuando corresponda
        ↓
REST Mapper
        ↓
Command de lectura
        ↓
UseCase
        ↓
Domain
        ↓
Response DTO
```

## Persistencia

```text
Application Port
        ↓
Repository Adapter
        ↓
Persistence Mapper
        ↓
JPA Entity
        ↓
Spring Data Repository
```

# Integraciones externas

Los adapters de integraciones externas implementan puertos de salida definidos en `application`.

En Pipely, los contratos hacia APIs, proveedores o servicios externos también deben nombrarse con el sufijo `*Port`.

No se debe utilizar el sufijo `*Gateway`.

Ejemplos:

* `FetchProviderRecordsPort`
* `EnviarCorreoPort`
* `UploadFilePort`

Flujo:

```text
Application Port
        ↓
External Adapter
        ↓
External Client
        ↓
Provider DTO
        ↓
External Mapper
        ↓
Modelo esperado por Application
```

Responsabilidades:

* El puerto se define en `application/port/out`.
* El adapter externo implementa el puerto.
* El cliente técnico ejecuta la comunicación HTTP o con el proveedor.
* Los DTOs externos representan exclusivamente el contrato del proveedor.
* El mapper convierte la respuesta externa al modelo esperado por `application`.
* Ningún tipo específico del proveedor debe filtrarse hacia `application`.

Estructura recomendada:

```text
adapter/out/provider
├── ProviderAdapter
├── ProviderWebClient
├── dto
└── mapper
```

Ejemplo:

```java
public interface FetchProviderRecordsPort {

    List<ExternalRecord> fetchRecords(
            ExternalReference reference
    );
}
```

```java
@RequiredArgsConstructor
public class ProviderAdapter
        implements FetchProviderRecordsPort {

    private final ProviderWebClient webClient;

    @Override
    public List<ExternalRecord> fetchRecords(
            ExternalReference reference
    ) {
        ProviderRecordsResponse response =
                webClient.fetchRecords(reference);

        return ProviderMapper.toDomain(response);
    }
}
```

Los adapters externos deben aislar:

* `WebClient`.
* URLs.
* Headers.
* API keys.
* Timeouts.
* Reintentos técnicos.
* DTOs del proveedor.
* Códigos HTTP externos.
* Excepciones específicas del cliente o proveedor.

No deben filtrarse hacia `application`:

* DTOs externos.
* Clases de `WebClient`.
* Headers externos.
* Códigos HTTP del proveedor.
* Excepciones concretas del SDK.
* Modelos propios de la plataforma externa.


# Regla final

* El controller recibe HTTP e invoca UseCases.
* El Request DTO representa la entrada externa.
* El mapper REST convierte Request, parámetros y ActorContext a Command.
* El Response DTO representa la salida HTTP.
* El repository adapter implementa puertos de persistencia.
* El persistence mapper separa dominio y JPA.
* El external adapter aísla APIs y proveedores.
* La seguridad adapta la identidad técnica a `ActorContext`.
* La configuración crea y conecta beans.
* Ninguna clase de infraestructura debe implementar reglas de negocio.
