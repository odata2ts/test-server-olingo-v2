# test-server-olingo-v2

An [Apache Olingo 2](https://olingo.apache.org/) implementation of the odata2ts **"Library"** OData **V2**
feature test model — a deliberately feature-dense model used to evaluate which OData spec features a
given server implementation actually supports.

Java 8, in memory only. No database, no ORM, no Spring, no JAX-RS runtime.

## Why this exists

The reference model lives in its own repository,
[odata2ts/test-reference-model](https://github.com/odata2ts/test-reference-model). This server implements
its **V2 rendition**:

- [`model/library-v2.xml`](https://github.com/odata2ts/test-reference-model/blob/main/model/library-v2.xml) — the reference EDMX (CSDL 2.0)
- [`model/library-v2-v3.md`](https://github.com/odata2ts/test-reference-model/blob/main/model/library-v2-v3.md) — what that rendition contains and why

It is the counterpart to [test-server-cap](https://github.com/odata2ts/test-server-cap), which reaches V2
through the `@cap-js-community/odata-v2-adapter` middleware in front of a V4 service. This one speaks V2
natively, on a stack that was current when V2 was — which is the point: the two answer the same requests
for entirely different reasons, and comparing them separates "what V2 is" from "what one adapter does".

The verdict is in **[FEATURE-COVERAGE.md](FEATURE-COVERAGE.md)**, based on the emitted `$metadata` and on
requests against the running service.

Short version: the protocol surface is complete — all 26 operations, every query option V2 defines,
media link entries, `$links`, composite keys, complex types, and a concurrency token with a working
round trip. The interesting part is **inheritance**: Olingo renders the four-level hierarchy into
`$metadata` exactly as declared, and then cannot serialize it. §1 of FEATURE-COVERAGE.md explains what
that costs and what this server does instead.

Three things Olingo gets wrong are corrected here rather than passed on, because each would have made the
service misstate the protocol: it cannot declare its own `DataServiceVersion`, it cannot serve an
operation that returns nothing, and it demands a concurrency token without ever comparing it.

## Architecture

Two halves, and choosing them was the whole design decision:

| Half                                   | What it does                                                                      |
| -------------------------------------- | --------------------------------------------------------------------------------- |
| **hand-written `EdmProvider`**         | the model — the only Olingo API that can express `BaseType` and `Abstract`        |
| **Olingo's `ListsProcessor`**          | the protocol — `$filter`, `$orderby`, `$top`/`$skip`, `$inlinecount`, `$expand`, `$select`, `$count`, `$links`, CRUD, operation dispatch, serialization |

Olingo ships two processors that would have *derived* the EDM instead, and neither can express this
model. The **annotation processor**'s `@EdmEntityType` carries only a name and a namespace, so it has no
way to say `BaseType`; the **JPA processor** derives the EDM from a JPA metamodel, which shapes names and
facets after the persistence layer and needs a database underneath. `ListsProcessor` is decoupled from
both — its constructor takes a `DataSource` and a `ValueAccess`, nothing else — so the free
implementation of the whole query surface is available without either.

What is left to write is therefore small: the EDM, a 12-method `DataSource` over in-memory lists, and the
seed data.

```
src/main/java/org/odata2ts/library/
├─ LibraryServer.java               embedded Jetty + Olingo's plain ODataServlet
├─ LibraryServiceFactory.java       assembles EdmProvider + processor
├─ LibraryProcessor.java            ListsProcessor + its two gaps (void operations, ETag validation)
├─ DataServiceVersionFilter.java    corrects the version Olingo declares for itself
├─ edm/LibraryEdmProvider.java      the model
├─ edm/LibraryOperations.java       the 26 function imports
├─ data/LibraryDataSource.java      the DataSource implementation
├─ data/SeedData.java               fixed, well-known starting state
└─ model/                           the POJOs
```

## Getting started

### As a container

The published image is the intended way to consume this server:

```bash
docker run --rm -p 4004:4004 ghcr.io/odata2ts/test-server-olingo-v2:latest
```

The data is in memory, so every container starts from the identical, well-known state — which is what
makes it usable from an automated test suite.

`latest` is republished from every push to `main`. Releases are cut by release-please, and the image a
release ships is built while its release PR is open, pushed as `:rc` and smoke-tested there — including
a four-level derived type and one operation, the two parts most likely to break. Merging the release PR
only re-tags that manifest as `0.2.0`, `0.2`, `0` and `latest`, which takes seconds and ships exactly the
artifact that was tested. The release then dispatches to odata2ts, where a PR raising the pinned version
opens straight away.

### Locally

Requires JDK 8 or newer and Maven.

```bash
mvn package
java -jar target/library-server.jar
```

Service root: <http://localhost:4004/odata/v2/library/> ·
metadata: <http://localhost:4004/odata/v2/library/$metadata>

`PORT` overrides the port. There is nothing else to configure and nothing to deploy: the jar is
self-contained, Jetty is embedded, and the state is rebuilt per process.

> **Ask for JSON.** V2's default format is Atom/XML, so `curl` without an `Accept` header gets XML.
> Add `-H 'Accept: application/json'` for the JSON Verbose format that odata2ts uses.

## Seed data

Fixed keys, chosen to match [test-server-cap](https://github.com/odata2ts/test-server-cap) wherever the
same entity exists in both, so assertions can be compared across the two servers rather than re-derived:

| Entity          | Key                                      |
| --------------- | ---------------------------------------- |
| Book            | `11111111-1111-1111-1111-111111111111` — "Der Prozess" |
| Audiobook       | `44444444-4444-4444-4444-444444444441`   |
| EBook           | `66666666-6666-6666-6666-666666666661` — a media link entry |
| Copy            | `(MediumId=…1111, InventoryNumber=1001)` |
| Member          | `1` — Anna Berger                        |
| Branch          | `1` — Zentralbibliothek                  |

## Conventions

Aligned with the other odata2ts repositories: EditorConfig (LF, UTF-8, 2 spaces), Conventional Commits
with squash-merged PRs whose **title** is itself a valid commit message, MIT licensed.

Deviation: no Prettier here — this is a Java repository, and the Markdown is the only thing Prettier
would touch.
