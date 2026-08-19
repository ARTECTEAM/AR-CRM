# Domain — Reglas para dominio rico

La capa `domain` representa el corazón del negocio. No es una carpeta de datos; es donde viven las reglas, invariantes, decisiones y lenguaje del negocio.

## Principio central

- El dominio debe ser rico: los objetos se validan a sí mismos y exponen comportamiento de negocio.
- El dominio no debe ser anémico: no crear clases que sólo tengan atributos, getters y setters.
- Si una regla pertenece al negocio, debe vivir en el modelo de dominio o en un servicio de dominio, no en controllers, DTOs, repositories ni services de aplicación.

## Qué es dominio rico

- Entidades con identidad y comportamiento.
- Value Objects inmutables que validan su propio estado.
- Métodos con nombres de negocio: `activar`, `desactivar`, `cambiarCorreo`, `asignarResponsable`.
- Constructores o factories que impiden crear objetos inválidos.
- Invariantes protegidas en todo momento, no sólo al guardar en base de datos.
- Excepciones de dominio para representar violaciones de reglas.

## Qué evitar: dominio anémico

No hacer esto:

```java
@Getter
@Setter
public class Usuario {
    private UUID id;
    private String nombre;
    private String correo;
    private Boolean activo;
}
```

Por qué está mal:

- Permite estados inválidos.
- Cualquier capa puede modificar cualquier dato sin reglas.
- Las reglas terminan duplicadas en services, controllers o mappers.
- El modelo no expresa negocio; sólo transporta datos.

## Validación e invariantes

Estas reglas son **validaciones base** para que el objeto exista en un estado válido. No deben confundirse con comportamiento de negocio.

Ejemplos de validación base:

- `notNull`
- `notBlank`
- longitud mínima/máxima
- formato de email
- formato de link/URL

Estas validaciones protegen la integridad mínima del modelo, pero no representan por sí solas lo que la entidad **puede hacer**.

- Toda entidad debe crearse mediante métodos factory (`create`, `crear`, `reconstitute`) que validen invariantes antes de instanciar.
- El constructor de entidades puede ser generado con Lombok, pero debe ser privado para no saltarse validaciones.
- Los value objects sí pueden validar en su propia creación/factory.
- No aceptar `null`, strings vacíos, rangos inválidos ni combinaciones imposibles.
- No confiar en validaciones externas como `@Valid`, base de datos o frontend para proteger el dominio.
- Las validaciones del dominio deben ejecutarse aunque el objeto se cree desde tests, factories, mappers o persistencia.

## Domain Assertions

Las validaciones básicas del dominio deben centralizarse en assertions propias del dominio.

```java
public final class DomainAssert {

    private DomainAssert() {
    }

    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw InvariantViolationException.required(fieldName);
        }
        return value;
    }

    public static String notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw InvariantViolationException.blank(fieldName);
        }
        return value.trim();
    }

    public static String lengthBetween(String value, String fieldName, int min, int max) {
        String normalized = notBlank(value, fieldName);
        if (normalized.length() < min || normalized.length() > max) {
            throw InvariantViolationException.lengthOutsideRange(fieldName, min, max);
        }
        return normalized;
    }
}
```

Reglas:

- Usar `DomainAssert` para validaciones comunes de invariantes.
- `InvariantViolationException` debe pertenecer a `domain`.
- Las validaciones genéricas reciben **nombre de campo**, no mensajes completos.
- Los mensajes de excepciones deben estar centralizados en las excepciones y en español.
- Ejemplo correcto: `DomainAssert.notNull(columnaId, "columnaId")`.
- Ejemplo incorrecto: `DomainAssert.notNull(columnaId, "columnaId is mandatory")`.
- No lanzar excepciones HTTP, Spring, JPA ni infraestructura desde dominio.
- Agregar nuevos métodos de assertion sólo cuando representen validaciones reutilizables.
- **Validación de longitud:** usar `lengthBetween(min, max)` en una sola llamada para validar mínimo y máximo juntos.
- **Sin wrappers privados:** no crear métodos privados como `validarNombre`, `validarCorreo`, etc. dentro de la entidad. Usar `DomainAssert` directamente en factories y comportamiento de negocio, a menos que emerja un concepto de dominio real que justifique abstracción.

## Comportamiento de dominio

El comportamiento de dominio son acciones reales que una entidad puede ejecutar y las condiciones bajo las cuales puede ejecutarlas.

No confundir comportamiento con validaciones base. Que un campo sea obligatorio o tenga longitud válida es una invariante estructural. Que una entidad pueda o no ejecutar una acción según su estado actual es comportamiento de negocio.

Ejemplos de comportamiento de dominio:

- `pagar`: una orden no puede pagarse si ya está pagada o cancelada.
- `cancelar`: una orden no puede cancelarse si ya está pagada o cancelada.
- `agregarArticulo`: no se puede agregar un artículo vacío ni agregar artículos cuando la orden ya no está pendiente.
- `cambiarResponsable`: una entidad puede reasignarse sólo si la regla de negocio lo permite.
- Un trato sólo puede cambiar a estados válidos desde estados válidos.

Reglas:

- Los métodos de comportamiento deben tener nombres del lenguaje del negocio.
- Una entidad sólo debe exponer comportamiento cuando exista una acción real del negocio.
- Las reglas de transición o acción deben vivir en la entidad o en un servicio de dominio, no en controllers, DTOs, repositories ni servicios de aplicación.
- Si una regla de negocio se aplica en más de un método o lugar, debe extraerse a un método aparte para evitar duplicación.
- Las excepciones específicas de comportamiento deben recibir datos contextuales mínimos y construir su propio mensaje en español.
- La capa application puede orquestar y cargar dependencias, pero no debe decidir reglas de negocio que pertenezcan al dominio.

## Entidades

- Las entidades de dominio no deben declararse `final`.
- Una entidad se compara por identidad, no por todos sus atributos.
- Sólo debe tener métodos de comportamiento cuando exista una regla real de negocio; no inventar métodos para parecer dominio rico.
- No exponer setters públicos genéricos.
- Si alguna entidad necesita cambiar estado más adelante, ese cambio debe existir por una regla real de negocio y preservar invariantes.
- El constructor debe ser privado; la entidad se crea desde factories que validan y devuelven un objeto válido.
- En entidades, las variables deben ser `private final` por defecto. Sólo se permite omitir `final` cuando exista un modelo/estado explícitamente mutable y justificado por una regla real de negocio.
- Los timestamps de creación se generan dentro del factory de creación con `LocalDateTime.now()`; no se reciben desde afuera al crear una entidad nueva.

Ejemplo correcto:

```java
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Usuario {

    @EqualsAndHashCode.Include
    private final UsuarioId id;
    private final String nombre;
    private final String correo;
    private final String passwordHash;
    private final LocalDateTime creadoEn;
    private final boolean activo;

    // ── Factory ──────────────────────────────────────────────────

    public static Usuario create(String nombre, String correo, String passwordHash) {
        return new Usuario(
            UsuarioId.create(),
            DomainAssert.lengthBetween(nombre, "nombre", 1, 100),
            DomainAssert.lengthBetween(correo, "correo", 1, 150),
            DomainAssert.lengthBetween(passwordHash, "passwordHash", 1, 255),
            LocalDateTime.now(),
            true
        );
    }

    public static Usuario reconstitute(UsuarioId id, String nombre, String correo,
            String passwordHash, LocalDateTime creadoEn, boolean activo) {
        return new Usuario(
            DomainAssert.notNull(id, "id"),
            DomainAssert.lengthBetween(nombre, "nombre", 1, 100),
            DomainAssert.lengthBetween(correo, "correo", 1, 150),
            DomainAssert.lengthBetween(passwordHash, "passwordHash", 1, 255),
            DomainAssert.notNull(creadoEn, "creadoEn"),
            activo
        );
    }
}
```

Los comentarios de métodos deben explicar qué hace el método. No usar bloques `@param` en entidades de dominio.

## Value Objects

- Deben ser inmutables.
- Para identificadores tipados como `UsuarioId`, preferir `record`.
- Deben validar su valor en el constructor.
- Se comparan por valor completo.
- Son ideales para conceptos como correo, teléfono, nombre, monto, porcentaje, color, fecha límite o identificadores tipados.

**Cuándo crear un Value Object:**

| Criterio | Crear VO | Simple campo String/primitive |
|----------|----------|-------------------------------|
| Tiene validación compleja (regex, rangos, etc.) | ✅ | ❌ |
| Representa un concepto de negocio distinto | ✅ | ❌ |
| Solo se usa como atributo simple sin lógica | ❌ | ✅ String con `lengthBetween` |
| Identidad del agregado/root entity | ✅ UsuarioId | ❌ |
| Combinación de varios valores | ✅ | ❌ |

**Regla práctica:** No crear un VO para cada campo primitivo "por si acaso". Crear VO solo cuando el concepto lo justifica (identidad, validación compleja, o concepto de negocio). Para atributos simples de una entidad, usar `String` con validaciones en el factory method.

## Lombok en domain

Lombok sí está permitido en `domain`, pero debe usarse con criterio arquitectónico.

Permitido:

- `@Getter`
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` en entidades.
- `@EqualsAndHashCode` en value objects.
- `@ToString`, siempre que no exponga datos sensibles.
- `@RequiredArgsConstructor` sólo cuando no saltee validaciones necesarias.
- `@AllArgsConstructor(access = AccessLevel.PRIVATE)` para entidades creadas desde factories validadoras.

Evitar:

- `@Setter` público en entidades.
- `@Data`, porque genera setters, `equals`, `hashCode` y `toString` sin intención de dominio.
- `@NoArgsConstructor` salvo necesidad técnica muy justificada y nunca para permitir estados inválidos.
- `@AllArgsConstructor` público si permite crear objetos sin validar invariantes.

## Dependencias prohibidas

El dominio no debe depender de:

- Spring.
- Jakarta Persistence / JPA.
- Controllers, DTOs, requests o responses.
- Repositories concretos.
- Seguridad JWT.
- Base de datos.
- Mensajería.
- APIs externas.

## Dependencias permitidas

- Java estándar.
- Lombok.
- Clases propias de `domain`.

## Excepciones de dominio

- Las excepciones deben expresar reglas rotas del negocio.
- Usar `InvariantViolationException` para invariantes inválidas.
- Crear excepciones más específicas cuando el negocio lo justifique.

## Testing

- Las pruebas de dominio no deben levantar Spring.
- Probar creación válida, creación inválida y comportamiento de negocio.
- Cada método de negocio debe tener tests que demuestren sus invariantes.
