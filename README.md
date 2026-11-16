# clean-usecase

Base jar for building reactive domains (Java 21 + Reactor) following the
use-case architecture proposed by Uncle Bob. It is pure business
language: no Spring, no HTTP, no JDBC, no implementation details of any
kind — and that is enforced by `DomainPurityTest` (ArchUnit), not by
discipline.

This document explains the **why** behind each decision. The code
explains the what; the commits explain the when; this explains the why,
so it isn't lost once it's no longer fresh in anyone's head.

## The base contracts

```java
public interface UseCase<R extends Request, S extends Response> {
    Mono<S> execute(R request);
}

public abstract class AbstractUseCase<R extends Request, S extends Response> implements UseCase<R, S> {
    public final Mono<S> execute(R request) {
        return Mono.fromSupplier(() -> Objects.requireNonNull(request, "request must not be null"))
                .flatMap(this::guard)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("guard completed empty")))
                .flatMap(this::process);
    }
    protected Mono<R> guard(R request) { return Mono.just(request); }
    protected abstract Mono<S> process(R request);
}
```

`UseCase` is parameterized by its concrete `Request`/`Response` — there
is never an unchecked cast between them. Each use case implements
`process()` (the actual work) and optionally `guard()` (validation before
doing that work). `guard()` must either emit the request or signal an
error — completing empty is a programming mistake, so `execute()` turns
it into an `IllegalStateException` instead of silently skipping
`process()`.

### `guard()` vs `process()`: one gateway call, not two

The natural temptation is to put "does this exist?" in `guard()` and the
actual work in `process()` — but that's two trips to the same data. The
right way is to resolve existence **inside** the same reactive chain in
`process()`, chaining the fetch with the result:

```java
@Override
protected Mono<GetCatalogEntryResponse> process(GetCatalogEntryRequest request) {
    return catalogGateway.findById(request.id())
            .switchIfEmpty(Mono.error(new UseCaseException(
                    new DomainFailure.NotFound("catalog entry", request.id()))))
            .map(entry -> new GetCatalogEntryResponse(entry.id(), entry.description()));
}
```

`guard()` is reserved for validations that do **not** require a fetch (a
missing required field, for example) — those are cheap and gain nothing
from living inside `process()`. If a gateway method already signals
"doesn't exist" with `Mono.empty()` (like `update`/`deleteById`), there's
no need for a prior `findById` — chain directly on that call.

This pattern was taken from an earlier implementation
(`QueryBalanceUseCase` in `clubix-core`, against `clean-usecase` 0.1.0)
and generalized when reimplementing `catalog-domain`.

## `Request`/`Response`: records, not reflection

```java
public interface Request {}
public interface Response {}
```

They are empty marker interfaces — the concrete types are immutable
`record`s, constructed directly by whoever uses them. There is **no**
`RequestFactory`/`UseCaseFactory` resolving them by name at runtime;
those existed in earlier versions and were removed on purpose.

The original reason for that reflection was "reduce the HTTP layer's
coupling to the domain, following Uncle Bob" — but that conflates two
different things:

- Clean Architecture's **Dependency Rule** (outer layers depend on inner
  ones, never the other way around). A direct `import` of a `UseCase`
  from a Controller already satisfies it perfectly — that's the allowed
  direction, not the forbidden one.
- **Deployment decoupling** (being able to change which use case runs
  without recompiling the HTTP layer). Dynamic resolution by name does
  solve this, but *only* if HTTP and use cases are deployed as
  independent artifacts (a plugin architecture). If everything is
  assembled into a single API — the normal case — that reflection buys
  nothing and only costs: type safety, unusable records, and a real bug
  it caused (a typo in a parameter name failed silently).

If some future project *does* genuinely need decoupled deployment
(plugins, a rules engine, use cases loaded at runtime), it's worth
reconsidering — but as an explicit decision for that project, not as an
unquestioned inherited default.

### When a `Request` grows: named static factory, not a Builder

With 1-2 fields, `new SomeRequest(x)` is enough — it's the idiomatic way
to build a record. But when a `Request` is legitimately built differently
depending on context (several fields, some optional, different
"assembly paths"), the temptation is to reach for a `Builder` (Lombok or
hand-written). It's not the right tool for that: a builder doesn't tell
you *which scenario* you're building just by looking at the call — you
have to read the whole chain to infer it. A named static factory says it
up front, stays an immutable record, and doesn't reintroduce Lombok or
reflection:

```java
public record CreateOrderRequest(String customerId, String promoCode, boolean expedited, String giftMessage)
        implements Request {

    public static CreateOrderRequest standard(String customerId) {
        return new CreateOrderRequest(customerId, null, false, null);
    }

    public static CreateOrderRequest withPromo(String customerId, String promoCode) {
        return new CreateOrderRequest(customerId, promoCode, false, null);
    }

    public static CreateOrderRequest expeditedGift(String customerId, String giftMessage) {
        return new CreateOrderRequest(customerId, null, true, giftMessage);
    }
}
```

This wins when the contexts are a known, enumerable set (3-5 typical
ways to build the object). If the variation is genuinely combinatorial —
many independent optional fields combined in ways you can't enumerate up
front — then a `Builder` does avoid an explosion of factory methods; it's
the exception, not the default.

## Error handling: sealed `DomainFailure`, not loose exceptions

```java
public sealed interface DomainFailure permits
        DomainFailure.MissingField,
        DomainFailure.InvalidValue,
        DomainFailure.NotFound,
        DomainFailure.Forbidden,
        DomainFailure.NotActive,
        DomainFailure.Conflict,
        DomainFailure.Custom,
        DomainFailure.Multiple {
    String message();
    static DomainFailure combine(List<? extends DomainFailure> failures) { ... }
    record MissingField(String field) implements DomainFailure { ... }
    record InvalidValue(String field, String reason) implements DomainFailure { ... }
    record NotFound(String entity, String id) implements DomainFailure { ... }
    record Forbidden(String reason) implements DomainFailure { ... }
    record NotActive(String entity, String id) implements DomainFailure { ... }
    record Conflict(String entity, String id) implements DomainFailure { ... }
    record Custom<T extends Enum<T>>(T reason, String message) implements DomainFailure { ... }
    record Multiple(List<DomainFailure> failures) implements DomainFailure { ... }
}

public final class UseCaseException extends RuntimeException {
    public UseCaseException(DomainFailure failure) { ... }
    public DomainFailure failure() { ... }
}
```

Business failures still travel through `Mono.error(...)` — for Reactor
that **is** the idiomatic way to propagate a failure, the equivalent of
an `Either`'s `Left` channel already built into the operator chain. What
changes is what travels there: not a generic `RuntimeException` with a
`String`, but a sealed type. Whoever consumes the error can do an
exhaustive `switch` over `DomainFailure` — if a new variant is added
tomorrow, code that doesn't handle it stops compiling, instead of
silently falling into a default branch.

`Custom` is the escape hatch for reasons specific to one consumer's
domain (e.g. insufficient funds in a ledger): the consumer defines its
own enum and this jar doesn't need a new variant for it. Every component
of every variant is non-null — the records reject `null` at construction.

`Multiple` reports several failures at once — every missing detail of a
registration, say — so the caller fixes them all in one attempt instead
of discovering them one per round trip. It always holds at least two
failures and never nests. Build it with `DomainFailure.combine(list)`:
one failure comes back as is, several become a flattened `Multiple`. A
`guard()` that checks independent rules can collect them all and reject
once:

```java
@Override
protected Mono<RegisterRequest> guard(RegisterRequest request) {
    return Flux.concat(requireName(request), requireEmail(request))   // each Mono<DomainFailure>, empty when fine
            .collectList()
            .flatMap(failures -> failures.isEmpty()
                    ? Mono.just(request)
                    : Mono.error(new UseCaseException(DomainFailure.combine(failures))));
}
```

It was called `ValidationFailure` up to 0.3.x. Renamed because most
variants aren't validation at all — `NotFound`, `Conflict`, `Forbidden`
and `NotActive` are business outcomes discovered while doing the work,
not checks on the input.

"Not found" is not modeled as a `Response` variant — it travels through
the error channel like any other `DomainFailure`. A sealed `Response`
does make sense when *success* has different shapes (see
`FeatureResponse.Created`/`PendingApproval` in the tests as an example) — there it
pays to seal the concrete `Response` itself, never this jar's shared
`Response` (its `permits` would have to list types that don't exist yet
when this jar is compiled — downstream projects depend on the domain,
never the other way around).

## Purity enforced, not just requested

`DomainPurityTest` (ArchUnit) runs on every build and fails if any
production class in `com.usecase` depends on anything outside an
allowlist: `java.lang`, `java.util`, `java.time`, `java.math`, Reactor
and Reactive Streams. It's an allowlist rather than a denylist so that a
library nobody thought to ban (Lombok, Guava, `java.net`, ...) still
fails the build. Any project depending on this jar should bring the same rule,
pointed at its own root package — see the `clean-domain-module` skill.

## The ecosystem around this jar

This jar is only the foundation. The full flow for a new project is
encoded in skills, not in this README (to avoid duplication and going
stale):

- **`gherkin-features`** — bootstraps a separate `features/` repo (git
  submodule, independently versioned) with the ubiquitous-language
  guardrail, and the "three amigos" process for drafting scenarios.
- **`clean-domain-module`** — scaffolds a new `<name>-domain` on top of
  this jar, with `ArchUnit` from day one.
- **`cucumber-red-green`** — wires the external `.feature` files to the
  code via Cucumber, use cases red first, implementation afterwards
  without touching the already-approved contract.

Publishing this jar is no longer a skill: CI does it from a tag (see
[Releasing](#releasing)). It used to go to AWS CodeArtifact (domain
`clubix`, repo `koden`); that domain is still active for other projects,
but only stale 0.1/0.2 snapshots of this jar remain there — never resolve
`com.usecase` from it.

`catalog` (in `projects/catalog/`, with `catalog-core`, `catalog-api`,
`catalog-repo-postgres`, `catalog-web`, `catalog-infra` and
`catalog-features` as sibling repos inside) is the first reference
project that went through this full flow, from features to a green
implementation — it serves as a living example when the code here isn't
enough to explain something.

## Versioning

Pre-1.0 SemVer: minor bump for incompatible changes (while the major is
still 0), patch for compatible changes. `0.1.0 → 0.2.0` was due to
removing the reflective factories and parameterizing `UseCase` — an
incompatible API change.

- `0.3.0` added `NotActive`, `Conflict` and `Custom`. New variants of a
  sealed interface break consumers' exhaustive `switch`es, hence a minor.
- `0.4.0` renamed `ValidationFailure` to `DomainFailure`, made an empty
  `guard()` signal an error instead of skipping `process()`, and made
  `DomainFailure` records reject `null` components.
- `0.5.0` added `Multiple` and `DomainFailure.combine()`. `apiCheck`
  doesn't flag a new permitted subtype of a sealed interface (japicmp
  sees it as an addition), so the minor bump for these is a manual
  decision — see [API compatibility](#api-compatibility).

## Releasing

CI (`.github/workflows/ci.yml`) builds and tests every push and PR to
`main`, but never publishes from there. Publishing to GitHub Packages
happens only from a `v*` tag, and only if the tag matches the version in
`build.gradle`:

1. Set `version = 'X.Y.Z'` (no `-SNAPSHOT`) in `build.gradle` and commit.
2. `git tag vX.Y.Z && git push origin main vX.Y.Z`.
3. Once the workflow has published it, set `apiBaseline=X.Y.Z` in
   `gradle.properties`, bump to the next **patch** `-SNAPSHOT`
   (`X.Y.(Z+1)-SNAPSHOT`) and commit.

### API compatibility

`apiCheck` (japicmp) compares the jar against the `apiBaseline` release.
While the version shares the baseline's major.minor, any binary- or
source-incompatible change fails the build. That's why step 3 bumps the
patch, not the minor: a breaking change then forces a deliberate edit to
the next minor (pre-1.0 SemVer), instead of slipping into a patch release
that every consumer would pick up as safe.

It does **not** catch a new variant of a sealed interface such as
`DomainFailure`: for japicmp that's an added class, compatible, yet it
breaks every consumer's exhaustive `switch`. Bump the minor by hand
whenever a sealed type gains a permitted subtype.

It runs as part of `check` on CI. Locally, run `./gradlew apiCheck` with
`GITHUB_TOKEN` set to a PAT that has `read:packages`; the report lands in
`build/reports/apiCheck.txt`.

The workflow publishes with the run's own `GITHUB_TOKEN` — no secrets to
configure. Release versions are immutable in GitHub Packages, so a tag
can't be republished; fix forward with a new patch version.

Consuming the jar requires authentication even though the repo is
public: GitHub Packages doesn't allow anonymous Maven downloads. Use a
personal access token with `read:packages`.
