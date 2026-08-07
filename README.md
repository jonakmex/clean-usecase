# clean-usecase

Jar base para construir dominios reactivos (Java 21 + Reactor) siguiendo la
arquitectura de casos de uso propuesta por Uncle Bob. Es lenguaje de
negocio puro: sin Spring, sin HTTP, sin JDBC, sin ningún detalle de
implementación — eso se paga con `DomainPurityTest` (ArchUnit), no con
disciplina.

Este documento explica el **por qué** de cada decisión. El código explica
el qué; los commits explican el cuándo; esto explica el porqué, para que
no se pierda cuando ya no esté fresco en la cabeza de nadie.

## Los contratos base

```java
public interface UseCase<R extends Request, S extends Response> {
    Mono<S> execute(R request);
}

public abstract class AbstractUseCase<R extends Request, S extends Response> implements UseCase<R, S> {
    public final Mono<S> execute(R request) {
        return Mono.just(request).flatMap(this::guard).flatMap(this::process);
    }
    protected Mono<R> guard(R request) { return Mono.just(request); }
    protected abstract Mono<S> process(R request);
}
```

`UseCase` está parametrizado por su `Request`/`Response` concretos —
nunca hay un cast sin chequear entre ellos. Cada caso de uso implementa
`process()` (el trabajo real) y opcionalmente `guard()` (validación antes
de hacer ese trabajo).

### `guard()` vs `process()`: una sola llamada al gateway, no dos

La tentación natural es poner "¿existe esto?" en `guard()` y el trabajo
real en `process()` — pero eso son dos viajes al mismo dato. La forma
correcta es resolver la existencia **dentro** de la misma cadena reactiva
de `process()`, encadenando el fetch con el resultado:

```java
@Override
protected Mono<GetCatalogEntryResponse> process(GetCatalogEntryRequest request) {
    return catalogGateway.findById(request.id())
            .switchIfEmpty(Mono.error(new UseCaseException(
                    new ValidationFailure.NotFound("catalog entry", request.id()))))
            .map(entry -> new GetCatalogEntryResponse(entry.id(), entry.description()));
}
```

`guard()` se reserva para validaciones que **no** requieren un fetch (un
campo obligatorio ausente, por ejemplo) — esas sí son baratas y no ganan
nada por vivir dentro de `process()`. Si un método del gateway ya señala
"no existe" con `Mono.empty()` (como `update`/`deleteById`), no hace falta
un `findById` previo — se encadena directo sobre esa llamada.

Este patrón se tomó de una implementación anterior (`QueryBalanceUseCase`
en `clubix-core`, contra `clean-usecase` 0.1.0) y se generalizó al
reimplementar `catalog-domain`.

## `Request`/`Response`: records, no reflexión

```java
public interface Request {}
public interface Response {}
```

Son interfaces marcador vacías — los tipos concretos son `record`,
inmutables, construidos directamente por quien los usa. **No existe**
`RequestFactory`/`UseCaseFactory` resolviendo esto por nombre en runtime;
existieron en versiones anteriores y se eliminaron a propósito.

La razón original de esa reflexión era "reducir el acoplamiento del HTTP
layer contra el dominio, siguiendo a Uncle Bob" — pero eso confunde dos
cosas distintas:

- La **Regla de Dependencia** de Clean Architecture (las capas externas
  dependen de las internas, nunca al revés). Un `import` directo de un
  Controller a un `UseCase` ya la cumple perfectamente — es la dirección
  permitida, no la prohibida.
- **Desacople de despliegue** (poder cambiar qué caso de uso corre sin
  recompilar el HTTP layer). Esto sí lo resuelve la resolución dinámica
  por nombre, pero *solo* si HTTP y casos de uso se despliegan como
  artefactos independientes (arquitectura de plugins). Si todo se
  ensambla en una sola API — que es el caso normal — esa reflexión no
  compra nada, solo cuesta: type-safety, records inutilizables, y un bug
  real que causó (un typo en un nombre de parámetro fallaba en silencio).

Si algún proyecto futuro *sí* necesita despliegue desacoplado de verdad
(plugins, un motor de reglas, casos de uso cargados en runtime), vale la
pena reconsiderar esto — pero como una decisión explícita para ese
proyecto, no como default heredado sin cuestionar.

## Manejo de errores: `ValidationFailure` sellado, no excepciones sueltas

```java
public sealed interface ValidationFailure permits
        ValidationFailure.MissingField,
        ValidationFailure.InvalidValue,
        ValidationFailure.NotFound,
        ValidationFailure.Forbidden {
    String message();
    record MissingField(String field) implements ValidationFailure { ... }
    record InvalidValue(String field, String reason) implements ValidationFailure { ... }
    record NotFound(String entity, String id) implements ValidationFailure { ... }
    record Forbidden(String reason) implements ValidationFailure { ... }
}

public final class UseCaseException extends RuntimeException {
    public UseCaseException(ValidationFailure failure) { ... }
    public ValidationFailure failure() { ... }
}
```

Los fallos de negocio siguen viajando por `Mono.error(...)` — para
Reactor esa **es** la vía idiomática de propagar un fallo, el equivalente
al canal `Left` de un `Either` ya integrado en la cadena de operadores.
Lo que cambia es qué viaja ahí: no una `RuntimeException` genérica con un
`String`, sino un tipo sellado. Quien consuma el error puede hacer
`switch` exhaustivo sobre `ValidationFailure` — si mañana se agrega una
variante nueva, el código que no la contempla deja de compilar, en vez de
caer silenciosamente en una rama por default.

No se modela el "no encontrado" como una variante de `Response` — viaja
por el canal de error igual que cualquier otro `ValidationFailure`. Un
`Response` sellado sí tiene sentido cuando el *éxito* tiene formas
distintas (ver `FeatureResponse.Success`/`Error` en los tests como
ejemplo) — ahí sí conviene sellar el propio `Response` concreto, nunca
el `Response` compartido de este jar (sus `permits` tendrían que listar
tipos que no existen todavía cuando este jar se compila — los proyectos
downstream dependen de dominio, nunca al revés).

## Pureza forzada, no solo pedida

`DomainPurityTest` (ArchUnit) corre en cada build y falla si cualquier
clase de `com.usecase` depende de Spring, servlets, JDBC, Apache HTTP,
Netty o Jackson. Cualquier proyecto que dependa de este jar debería traer
la misma regla, apuntada a su propio paquete raíz — ver el skill
`clean-domain-module`.

## El ecosistema alrededor de este jar

Este jar es solo la base. El flujo completo de un proyecto nuevo está
codificado en skills, no en este README (para no duplicar y quedar
desactualizado):

- **`gherkin-features`** — bootstrap de un repo `features/` separado
  (submódulo git, versionado independiente) con el guardrail de lenguaje
  ubicuo, y el proceso de "3 amigos" para redactar escenarios.
- **`clean-domain-module`** — scaffold de un nuevo `<name>-domain` sobre
  este jar, con `ArchUnit` desde el día uno.
- **`cucumber-red-green`** — conecta los `.feature` externos al código
  vía Cucumber, casos de uso en rojo primero, implementación después sin
  tocar el contrato ya aprobado.
- **`codeartifact-publish`** — publica cualquier jar de este ecosistema a
  CodeArtifact (dominio `clubix`, repo `koden`).

`catalog` (en `projects/catalog`) es el primer proyecto de referencia que
recorrió este flujo completo, de features a implementación en verde —
sirve como ejemplo vivo cuando el código de aquí no alcance a explicar
algo.

## Versionado

SemVer pre-1.0: bump de minor por cambios incompatibles (mientras el
major siga en 0), patch para cambios compatibles. `0.1.0 → 0.2.0` fue por
eliminar las factories reflexivas y parametrizar `UseCase` — un cambio de
API incompatible.
