# Application — Reglas de capa

## Convención del proyecto

La capa `application` se organiza por entidad, agregado o contexto funcional:

```text
application
└── columna
    ├── command
    ├── exception
    ├── port
    │   ├── in
    │   └── out
    └── service
```

En este proyecto:

* Se usa `Command` como entrada tanto para casos de uso de escritura como de lectura.
* Los casos de uso devuelven entidades de dominio, Value Objects, colecciones de dominio o `void`.
* No se agregan `Query`, `Result`, proyecciones, modelos de lectura ni DTOs propios de `application`.
* No se mezclan clases de contextos distintos en carpetas globales genéricas.

---

# Flujo de mapeo

```text
DTORequest o datos HTTP
        ↓ adapter in
Command
        ↓ Service
Entidad o Value Object de dominio
        ↓ port/out
Adapter out
        ↓
Entidad JPA
        ↓ JPARepository
Entidad JPA
        ↓ adapter out
Entidad de dominio
        ↓ Service / port/in
Entidad de dominio
        ↓ adapter in
DTOResponse
```

Responsable de cada conversión:

| Conversión | Responsable |
|---|---|
| `DTORequest` o parámetros HTTP → `Command` | Adapter de entrada |
| `Command` → Value Objects o entidad de dominio | `Service`, usando factories del dominio |
| Entidad de dominio → entidad JPA | Adapter de salida |
| Entidad JPA → entidad de dominio | Adapter de salida |
| Entidad de dominio → `DTOResponse` | Adapter de entrada |

Reglas:

* `application` no conoce DTOs REST ni entidades JPA.
* El adapter de salida no recibe `Command`.
* El `Service` no convierte dominio a DTOs REST ni a entidades JPA.
* No se crea una carpeta `mapper` en `application` para conversiones de infraestructura.

---

# Responsabilidad de Application

La capa `application` implementa y coordina los casos de uso.

Debe:

* Recibir la entrada mediante un `Command` cuando la operación requiera datos.
* Exponer cada acción mediante una interfaz en `port/in`.
* Implementar cada acción mediante un `Service`.
* Convertir datos simples del `Command` a Value Objects.
* Crear o recuperar entidades de dominio.
* Invocar comportamiento público del dominio.
* Consultar y persistir mediante interfaces en `port/out`.
* Verificar existencia, autorización, ownership, referencias, unicidad e idempotencia cuando se requiera información externa.

No debe contener:

* Controllers o DTOs REST.
* Entidades JPA o repositorios Spring Data.
* Implementaciones de adapters.
* Tipos HTTP o excepciones HTTP.
* `SecurityContext`, JWT o filtros de seguridad.
* `WebClient`, clientes HTTP o DTOs de proveedores.
* Configuración de Spring o clases de arranque.
* Reglas internas de entidades o agregados.

> `application` coordina. El dominio protege invariantes, comportamiento y transiciones de estado.

---

# Dependencias

`application`:

* Puede depender de `domain`.
* No puede depender de `infrastructure`, `boot` ni adapters concretos.
* No puede importar JPA, Spring MVC, Spring Security, Spring Data o clientes externos.
* Toda dependencia externa debe expresarse mediante una interfaz en `port/out`.

Dirección permitida:

```text
adapter in → application → domain
adapter out → application → domain
boot → application + adapters
```

Nunca:

```text
application → infrastructure
application → adapter
application → boot
```

---

# Command

Un `Command` contiene los datos de entrada necesarios para ejecutar un caso de uso, sea de lectura o escritura.

Ejemplos:

* `CreateEmpresaCommand`
* `UpdateEmpresaCommand`
* `DeleteEmpresaCommand`
* `GetEmpresaByIdCommand`
* `GetColumnaByIdCommand`

## Debe

* Ubicarse en `command`.
* Nombrarse con el sufijo `Command`.
* Representar una sola operación.
* Preferentemente ser un `record` inmutable.
* Contener únicamente los datos necesarios para esa operación.
* Usar datos de entrada simples, como `String`, `UUID`, números, fechas, booleanos o colecciones simples.
* Ser construido por el adapter de entrada.
* Ser independiente de REST, JPA, Spring y cualquier adapter.

## Puede validar

Únicamente su contrato estructural:

* Campos obligatorios nulos.
* Cadenas obligatorias vacías.
* Rangos evidentemente inválidos.
* Parámetros incompatibles recibidos en la misma entrada.
* Identificadores necesarios para ejecutar la operación.

```java
public record GetColumnaByIdCommand(UUID id) {

    public GetColumnaByIdCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}
```

## No debe

* Contener DTOs REST, entidades de dominio completas o entidades JPA.
* Recibir repositorios, puertos o servicios.
* Consultar persistencia.
* Comprobar existencia, unicidad, autorización u ownership.
* Verificar idempotencia persistida.
* Ejecutar transiciones de dominio.
* Crear o guardar entidades.
* Contener métodos `toEntity()`, `save()`, `execute()` o equivalentes.
* Usar anotaciones de Spring MVC, Jackson, JPA o Swagger.

La existencia de una entidad, los permisos y las referencias se verifican en el `Service`. Las invariantes se protegen en el dominio.

---

# Port In — UseCase

Un puerto de entrada define una operación que `application` expone a sus adapters de entrada.

## Debe

* Ubicarse en `port/in`.
* Ser una interfaz.
* Nombrarse con el sufijo `UseCase`.
* Representar una sola acción.
* Tener un único método principal.
* Recibir el `Command` específico cuando necesite datos.
* Devolver una entidad de dominio, un Value Object, una colección de dominio o `void`.
* Ser independiente de REST, mensajería, herramientas de IA y cualquier otro mecanismo de entrada.

```java
public interface GetColumnaByIdUseCase {

    Columna getById(GetColumnaByIdCommand command);
}
```

Una operación sin datos de entrada puede no recibir parámetros:

```java
public interface GetAllEmpresasUseCase {

    List<Empresa> getAll();
}
```

## No debe

* Recibir DTOs REST o entidades JPA.
* Recibir `HttpServletRequest`, `Authentication`, `Principal` o `SecurityContext`.
* Devolver DTOs response, entidades JPA, `ResponseEntity` o códigos HTTP.
* Contener lógica en métodos `default`.
* Acceder a persistencia.
* Agrupar acciones distintas en una sola interfaz CRUD.

Incorrecto:

```java
public interface EmpresaUseCase {
    Empresa create(CreateEmpresaCommand command);
    Empresa update(UpdateEmpresaCommand command);
    void delete(DeleteEmpresaCommand command);
    Empresa getById(GetEmpresaByIdCommand command);
}
```

Correcto:

```text
CreateEmpresaUseCase
UpdateEmpresaUseCase
DeleteEmpresaUseCase
GetEmpresaByIdUseCase
```

---

# Port Out

Un puerto de salida define una capacidad externa que un caso de uso necesita: persistencia, consulta, autorización, mensajería o una integración externa.

## Debe

* Ubicarse en `port/out`.
* Ser una interfaz.
* Nombrarse con el sufijo `Port`.
* Describir la necesidad de `application`, no la tecnología que la implementa.
* Ser granular y exponer únicamente la operación necesaria.
* Ser implementado por un adapter de salida.

Ejemplos:

* `SaveEmpresaPort`
* `FindEmpresaByIdPort`
* `ExistsEmpresaByNombrePort`
* `FindUsuarioByIdPort`
* `SendEmailPort`

Todas las dependencias de salida usan el sufijo `Port`; no se introduce una categoría separada llamada `Gateway`.

## Tipos permitidos

Puede recibir o devolver:

* Entidades de dominio.
* Value Objects.
* Enums de dominio.
* `Optional` o colecciones de tipos de dominio.
* Primitivos o tipos estándar cuando expresen completamente el contrato.
* `void`.

```java
public interface SaveEmpresaPort {

    Empresa save(Empresa empresa);
}
```

```java
public interface FindEmpresaByIdPort {

    Optional<Empresa> findById(EmpresaId empresaId);
}
```

## No debe

* Recibir un `Command`.
* Recibir o devolver DTOs REST o DTOs de proveedores.
* Exponer entidades JPA.
* Exponer `JpaRepository`, `Page`, `Pageable`, `Sort` o `Specification`.
* Exponer tipos HTTP, `WebClient`, `RestClient` o clientes concretos.
* Copiar un repositorio CRUD completo cuando el caso de uso solo necesita una operación.

Correcto:

```java
EmpresaId empresaId = EmpresaId.from(command.id());

Empresa empresa = findEmpresaByIdPort.findById(empresaId)
        .orElseThrow(() -> EmpresaNotFoundException.forId(empresaId));
```

Incorrecto:

```java
findEmpresaByIdPort.findById(command);
```

En lugar de:

```java
public interface EmpresaRepositoryPort {
    Empresa save(Empresa empresa);
    Optional<Empresa> findById(EmpresaId id);
    List<Empresa> findAll();
    void delete(EmpresaId id);
}
```

se prefieren puertos separados:

```text
SaveEmpresaPort
FindEmpresaByIdPort
FindAllEmpresasPort
DeleteEmpresaPort
```

## Adapter de salida

La implementación concreta del puerto:

* Recibe dominio desde `application`.
* Convierte dominio a JPA antes de persistir.
* Usa internamente el `JpaRepository`.
* Convierte JPA a dominio antes de devolver información.
* Oculta los detalles de persistencia o del proveedor externo.

---

# Service

Un `Service` implementa un `UseCase` y coordina su ejecución.

## Debe

* Ubicarse en `service`.
* Nombrarse con el sufijo `Service`.
* Implementar un solo `UseCase`.
* Tener un único método público correspondiente al caso de uso.
* Recibir sus puertos mediante constructor.
* Puede usar `@RequiredArgsConstructor` de Lombok para generar el constructor de sus dependencias `final`.
* Depender únicamente de contratos de `application` y tipos de `domain`.
* Poder instanciarse y probarse sin levantar Spring.

## No debe

* Tener `@Service`, `@Component`, `@Transactional` u otras anotaciones de Spring o infraestructura.

`@RequiredArgsConstructor` sí está permitido porque es una anotación de Lombok que genera el constructor en compilación; no registra el `Service` como bean ni introduce una dependencia hacia Spring.
* Usar inyección mediante campos.
* Depender de adapters concretos o repositorios JPA.
* Recibir o devolver DTOs REST o entidades JPA.
* Usar tipos HTTP, `SecurityContext`, JWT o clientes HTTP concretos.
* Realizar mapeos de dominio a JPA o de dominio a DTOResponse.

El wiring, las transacciones y el registro como bean se configuran fuera de `application`.

## Flujo interno permitido

1. Recibir el `Command`.
2. Convertir datos simples a Value Objects.
3. Consultar entidades mediante puertos.
4. Verificar existencia, autorización, ownership, referencias, unicidad o idempotencia.
5. Crear una entidad mediante su factory o recuperar una existente.
6. Invocar comportamiento público del dominio.
7. Persistir mediante un puerto.
8. Devolver dominio, un Value Object, una colección de dominio o `void`.

Ejemplo de creación:

```java
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CreateEmpresaService
        implements CreateEmpresaUseCase {

    private final SaveEmpresaPort saveEmpresaPort;

    @Override
    public Empresa create(CreateEmpresaCommand command) {
        UsuarioId responsableId = command.responsableId() == null
                ? null
                : UsuarioId.from(command.responsableId());

        UsuarioId creadoPor = UsuarioId.from(command.creadoPor());

        Empresa empresa = Empresa.create(
                command.nombre(),
                command.sector(),
                responsableId,
                creadoPor
        );

        return saveEmpresaPort.save(empresa);
    }
}
```

Ejemplo de lectura:

```java
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GetColumnaByIdService
        implements GetColumnaByIdUseCase {

    private final FindColumnaByIdPort findColumnaByIdPort;

    @Override
    public Columna getById(GetColumnaByIdCommand command) {
        ColumnaId columnaId = ColumnaId.from(command.id());

        return findColumnaByIdPort.findById(columnaId)
                .orElseThrow(
                        () -> ColumnaNotFoundException.forId(columnaId)
                );
    }
}
```

---

# Validaciones en Service

## Permitidas

El `Service` valida condiciones que requieren coordinación o información externa:

* Existencia de entidades.
* Existencia de referencias.
* Unicidad mediante un puerto.
* Autorización y ownership.
* Idempotencia.
* Coordinación entre varios agregados.
* Interpretación de la ausencia o respuesta de un puerto.

## Prohibidas

El `Service` no debe reimplementar reglas internas del dominio.

Incorrecto:

```java
trato.setEstado(EstadoTrato.CERRADO);
```

Correcto:

La creación y reconstitución de `Trato` mantienen la validación y el estado
válido dentro del agregado; el `Service` sólo coordina puertos y dependencias.

También está prohibido:

* Cambiar estado mediante setters.
* Reproducir transiciones comparando enums.
* Duplicar invariantes del dominio.
* Crear entidades evitando factories o constructores controlados.
* Modificar colecciones internas sin métodos del agregado.
* Repetir normalizaciones propias de un Value Object.
* Ocultar reglas de negocio en métodos privados.

---

# Métodos privados en Service

Se permiten para expresar pasos de orquestación:

* Convertir un `UUID` a Value Object.
* Recuperar una entidad o lanzar una excepción.
* Resolver una referencia mediante un puerto.
* Aplicar autorización mediante un puerto.
* Crear una entidad mediante su factory.

No se permiten para:

* Implementar transiciones de estado.
* Reproducir invariantes del dominio.
* Manipular directamente campos internos.
* Convertir entidades a DTOs REST o entidades JPA.

---

# Identidad y asignación

## `creadoPor`

* Representa al actor autenticado.
* El adapter de entrada lo obtiene desde `ActorContext`.
* El controller lo incluye al construir el `Command`.
* No debe provenir de un campo libre controlado por el cliente.
* El `Service` lo convierte de `UUID` a `UsuarioId`.

## `responsableId`

* Representa al usuario asignado a la entidad.
* Puede venir del cliente y ser diferente de `creadoPor`.
* Puede ser opcional o requerido según el caso de uso.
* No debe sustituirse automáticamente por el actor autenticado.
* El `Service` comprueba su existencia y la autorización para asignarlo cuando corresponda.

La autorización debe representar la política real del sistema, no una comparación provisional entre ambos identificadores.

---

# Excepciones de Application

Representan fallos esperables durante la coordinación del caso de uso.

Ejemplos:

* `EmpresaNotFoundException`
* `UsuarioResponsableNotFoundException`
* `InvalidCreateEmpresaCommandException`
* `OperationNotAuthorizedException`
* `IdempotencyConflictException`

Reglas:

* Se ubican en `exception` dentro de su contexto.
* Expresan una causa concreta.
* No contienen códigos HTTP.
* No dependen de Spring MVC, JPA, SQL o proveedores externos.
* No sustituyen excepciones de dominio que ya expresan una invariante.
* El adapter de entrada decide cómo convertirlas a respuestas HTTP.

---

# Helpers de Application — `application.shared`

`application.shared` aloja helpers estructurales, sin estado y sin framework que se reutilizan en distintos contextos de `application` (por ejemplo, `ApplicationAssert`). No es un cajón genérico ni un sustituto de `Command`, `port` o `service`.

## Debe

* Contener únicamente utilidades puras, sin estado, sin Spring, JPA, HTTP u otro framework.
* Realizar exclusivamente comprobaciones estructurales: presencia, normalización de texto opcional, enteros positivos.
* Poder ser instanciadas y probadas sin levantar Spring.
* Ubicarse en `application.shared` con subpaquetes solo cuando agrupen un mismo conjunto de utilidades estructurales.

## No debe

* Definir reglas de negocio ni invariantes de dominio.
* Recibir o devolver entidades JPA, DTOs REST, `Command`s, puertos, servicios, `ActorContext`, JWT o tipos HTTP.
* Reemplazar responsabilidades de `Command` (validación estructural) ni de `Service` (conversión a `domain` y coordinación). El `Service` puede invocar helpers estructurales solo como herramienta, no para reemplazarlos.
* Almacenar estado mutable, dependencias inyectadas o referencias a infraestructura.
* Convertir tipos de framework en tipos de `domain` o viceversa.

---

# Capacidad de búsqueda dinámica

Cuando un caso de uso de lectura necesite exponer filtros opcionales combinables, se declara exactamente un puerto de salida con un solo método que reciba cada filtro opcional en tipos de `domain` o del JDK y un máximo opcional. Esto evita una familia combinatoria de métodos `FindBy...`.

Reglas:

* El puerto se nombra con el sufijo `Port` y describe la capacidad (`Search<Aggregate>Port`).
* Ningún filtro combinable es obligatorio; `null` significa "sin restricción sobre esa dimensión". Esta regla no aplica a parámetros obligatorios de alcance o seguridad, como `actorUsuarioId`.
* El método no introduce tipos JPA, HTTP, DTOs ni proyecciones.
* El `Service` realiza la conversión de `Command` a tipos de `domain` y, si el `Command` valida estructuralmente, esa validación queda en el `Command`. El `Service` no realiza validación estructural.
* El `Service` no codifica pushdown, orden ni límite; el límite, el orden y la aplicación de los filtros se resuelven en la fuente de datos y se documentan en `INFRASTRUCTURE_RULES.md` cuando se implemente la unidad correspondiente.
* Cuando el `Command` recibe un valor crudo de `String` opcional que representa un nombre de enum de `domain`, la membresía estructural por nombre exacto es validación del `Command`. El `Command` rechaza valores no nulos que no coinciden exactamente con un `name()` del enum durante su construcción; el `Service` solo convierte el `String` ya validado al tipo de `domain` mediante `valueOf`, sin `try/catch` propio y sin un validador `parseX` paralelo.

## No debe

* Crear un puerto por combinación de filtros (`FindByTextPort`, `FindByEstadoPort`, etc.).
* Devolver un `Query`, `Result` o DTO de lectura desde `application`.
* Exponer `Specification`, `Page`, `Pageable` o `Sort` en el contrato del puerto.
* Asignar responsabilidades de pushdown, orden o límite a `application`; esas viven en `infrastructure`.
* Duplicar en el `Service` la validación de membresía estructural que ya realiza el `Command` (incluido un `try/catch` sobre `Enum.valueOf` o un método `parseX` paralelo).

---

# Alcance obligatorio vs filtros opcionales en búsquedas actor-scoped

Cuando un caso de uso de búsqueda ejecuta sobre datos sensibles al actor (visibilidad, propiedad, asignación), el `Command` exige un identificador de actor obligatorio y lo trata como **alcance de seguridad**, no como filtro adicional. El `Service` lo convierte al tipo de `domain` correspondiente y lo entrega al puerto como un parámetro separado, anterior a los filtros opcionales. Ningún filtro opcional puede sustituir ni extender el alcance del actor.

Reglas:

* El `Command` declara el actor como campo obligatorio de tipo `UUID` (o `String` cuando la fuente externa ya validó el identificador) y rechaza un valor nulo durante su construcción con un `Reason` específico (`MISSING_ACTOR`, etc.). El `actorUsuarioId` no se normaliza, recorta ni transforma.
* El `Service` convierte el `actorUsuarioId` del `Command` al tipo de `domain` (`UsuarioId`, `EmpresaId`, etc.) y lo pasa al puerto como **primer parámetro**, separado de cualquier otro filtro.
* El puerto declara el actor como parámetro obligatorio (no opcional) en tipos de `domain`, nunca como `String` o `UUID` crudo, y lo aplica como predicado de alcance obligatorio (`(creado_por = actor OR responsable_id = actor)` u otro equivalente) intersectado con cada filtro opcional en la misma consulta.
* Un filtro opcional que conceptualmente podría coincidir con el actor (por ejemplo, `responsableId`) **nunca** reemplaza el alcance: el adaptador siempre aplica la disyunción de alcance primero y luego intersecta el filtro opcional.
* El `Command` preserva el `actorUsuarioId` sin alteraciones porque la identidad proviene de un `ActorContext` confiable fuera del `Command`. El `Command` no decide de dónde viene el actor; sólo exige su presencia.

## No debe

* Tratar el actor como un filtro opcional más del `Command` o del puerto.
* Recibir el `actorUsuarioId` desde un campo controlado por el cliente, desde el cuerpo de un request o desde los argumentos visibles de una herramienta de IA; sólo proviene del contexto autenticado o de metadatos confiables capturados fuera del esquema visible.
* Definir el alcance con dos predicados disjuntos a nivel de `application` (por ejemplo, un `if (actor.equals(responsableId))` en el `Service`); el alcance se aplica como una sola expresión en el adaptador, en la misma consulta pushdown.
* Permitir que un filtro opcional anule o extienda el alcance del actor; el alcance siempre gana.
* Aplicar `try/catch` sobre `Enum.valueOf` ni construir un validador `parseX` paralelo en el `Service`; la conversión de strings validados la realiza el `Service` y los alcances los aplica el adaptador.

---

# Testing

Las pruebas de `application` deben:

* Instanciar el `Service` directamente.
* Mockear los puertos de salida.
* No levantar Spring.
* No usar controllers, DTOs REST, entidades JPA ni repositorios reales.
* Verificar llamadas a puertos.
* Verificar que no se persiste cuando falla una condición previa.
* Verificar existencia, referencias, autorización, ownership, unicidad e idempotencia.
* Verificar la conversión del `Command` a Value Objects.
* Verificar que se invoca la factory o el método correcto del dominio.
* Verificar la entidad de dominio devuelta.

Las pruebas del `Service` no deben duplicar las pruebas internas del dominio.

---

# Checklist

## Command

* [ ] Representa una sola operación.
* [ ] Solo contiene los datos de entrada necesarios.
* [ ] Solo valida su contrato estructural.
* [ ] No consulta puertos ni persistencia.
* [ ] No contiene DTOs, JPA, dominio completo ni lógica de negocio.

## Port In

* [ ] Cada acción tiene su propio `UseCase`.
* [ ] Tiene un único método principal.
* [ ] Recibe el `Command` específico cuando necesita datos.
* [ ] Devuelve dominio, Value Object, colección de dominio o `void`.
* [ ] No recibe ni devuelve DTOs, JPA o tipos HTTP.

## Port Out

* [ ] Cada dependencia externa se expresa mediante un `Port`.
* [ ] El nombre describe la capacidad requerida.
* [ ] El contrato es granular.
* [ ] Usa tipos de dominio o tipos estándar necesarios.
* [ ] No recibe `Command`.
* [ ] No expone JPA, Spring Data, HTTP ni DTOs externos.

## Service

* [ ] Implementa un solo `UseCase`.
* [ ] Tiene un único método público.
* [ ] Recibe dependencias mediante constructor explícito o `@RequiredArgsConstructor`.
* [ ] Convierte el `Command` a tipos de dominio.
* [ ] Coordina sin reproducir reglas internas.
* [ ] Devuelve dominio, Value Object, colección de dominio o `void`.
* [ ] No depende de Spring, JPA, HTTP, JWT o adapters.

## General

* [ ] No existen `Query`, `Result`, proyecciones ni DTOs propios de `application`.
* [ ] No existen dependencias hacia `infrastructure`, `boot` o adapters.
* [ ] Los mapeos se realizan en la capa correspondiente.
* [ ] Las excepciones de `application` no contienen conceptos HTTP.
* [ ] Las pruebas no levantan Spring.

---

# Regla final

* `Command`: entrada de un caso de uso de lectura o escritura.
* `port/in`: operación que `application` expone.
* `Service`: convierte la entrada a dominio y coordina la ejecución.
* `port/out`: capacidad externa que el `Service` necesita.
* Adapter de salida: convierte dominio ↔ JPA.
* Adapter de entrada: convierte `DTORequest` o datos HTTP → `Command`, y dominio → `DTOResponse`.
* Validación estructural: `Command`.
* Validación con puertos o coordinación: `Service`.
* Invariantes y transiciones internas: dominio.
