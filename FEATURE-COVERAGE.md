# Apache Olingo 2 and the "Library" OData V2 test model

How far [Apache Olingo 2](https://olingo.apache.org/) can reproduce
[`model/library-v2.xml`](https://github.com/odata2ts/test-reference-model/blob/main/model/library-v2.xml),
and where it does something else.

Measured against **Olingo 2.0.13** (October 2023, the last release before the project was retired) on
Java 8, with a hand-written `EdmProvider` and Olingo's own `ListsProcessor`. Every statement below was
verified against the emitted `$metadata` and against the running service.

## How to read this document

Olingo 2 is the reference implementation of OData V2 for the JVM, and the model it is measured against is
the V2 rendition of the reference model — so unlike the CAP report, this is not a story about a framework
solving the same problem differently. Olingo has no domain-modelling opinion of its own; it implements
the protocol. Where it deviates, it is a **gap**, not a design.

That makes the result sharper than expected in both directions. The protocol surface is essentially
complete. The gaps are few, but two of them are load-bearing: one shapes the whole service layout, and
one makes a feature the model deliberately includes untestable.

---

## 1. Inheritance: rendered, not served

**The single most consequential finding.**

The model's centrepiece is a four-level hierarchy — `Medium → PrintMedium → Magazine → TradeJournal`,
plus `Medium → AudioMedium → Audiobook`/`DVD` — with abstract roots and a single entity set `Media` over
the abstract `Medium`.

Olingo renders that **perfectly** into `$metadata`. `Abstract="true"`, `BaseType=`, all four levels, the
abstract complex type `Address` and its derived `PostalAddress`: everything comes out exactly as declared.
A client generating code from this document sees the full hierarchy.

Then it cannot serialize it. `EntityInfoAggregator` takes the entity type from the **entity set**
(`EntityInfoAggregator.java:316`), and nothing in the `ep/producer` package ever consults
`getBaseType()` — inheritance is understood only in the metadata layer
(`EdmEntityTypeImplProv`, `XmlMetadataProducer`, `XmlMetadataConsumer`). Writing a `Book` into a set
typed on `Medium` produces:

```json
{"d":{"__metadata":{"type":"Library.Catalog.Medium"},
      "Id":"11111111-…","Title":"Der Prozess","Language":"de","PopularityScore":"87.5"}}
```

`ISBN`, `PageCount` and `AgeRating` are **silently dropped** and `__metadata.type` names the wrong type.
No error, no warning.

**What this server does instead: table-per-leaf-class.** One entity set per concrete type — `Books`,
`Magazines`, `TradeJournals`, `Audiobooks`, `DVDs`, `EBooks` — and no `Media` set. Each is typed on a
concrete type, and Olingo then serializes the inherited properties correctly: a `TradeJournal` comes back
with `Field` from itself, `IssueNumber` from `Magazine`, `ISBN` from `PrintMedium` and `Title` from
`Medium`, with `type: Library.Catalog.TradeJournal`. The hierarchy is fully declared and does reach the
client; only the addressing differs.

Three further consequences follow from that choice, and they are the price of it:

| Consequence                          | Detail                                                                                     |
| ------------------------------------ | ------------------------------------------------------------------------------------------ |
| Media-returning operations narrowed  | `MostReadMedium`, `NewReleases`, `Search`, `RunStockCheck` return `Book` and bind to `Books` — an entity-returning operation is serialized through its entity set and would truncate a mixed result exactly as `Media` would |
| One association, six association sets | `Medium_Copies` needs binding per leaf, so the container carries `Medium_Copies_Books`, `…_Magazines`, and so on |
| `Copy.Medium` reaches only `Books`   | The reverse navigation has no single set to point at; the association set binds it to `Books`, so a copy of a DVD has no reachable `Medium` |

**The comparison worth drawing.** [test-server-cap](https://github.com/odata2ts/test-server-cap) lands on
table-per-leaf-class too (its FEATURE-COVERAGE.md §1.1) — but for the opposite reason. CDS has no entity
inheritance at all, so CAP never renders one; Olingo renders it and cannot serve it. Same layout, and the
`$metadata` a client receives is completely different.

---

## 2. What works, and works fully

### 2.1 The model

| Feature                                        | Result                                                              |
| ---------------------------------------------- | ------------------------------------------------------------------- |
| Entity types, keys, **composite keys**         | ✅ `Copies(MediumId=guid'…',InventoryNumber=1001)`                  |
| Complex types, incl. **abstract + `BaseType`** | ✅ `Address` → `PostalAddress`, serialized with `__metadata.type`   |
| `Association` / `AssociationSet`               | ✅ 8 associations, 13 association sets                              |
| Referential constraint, `OnDelete Cascade`     | ✅ rendered as declared                                             |
| Four namespaces in one document                | ✅ incl. the deliberate `Branch` name collision across two of them   |
| `ConcurrencyMode="Fixed"`                      | ✅ in the metadata, and it produces an ETag — but see §3.1           |
| Facets: `MaxLength`, `Precision`, `Scale`, `Unicode`, `DefaultValue`, `Nullable` | ✅            |
| `m:HasStream` media link entries               | ✅ `EBook` (inside the hierarchy) and `AudiobookChapter`            |
| All 26 operations                              | ✅ every return-type variant, see §2.3                              |

### 2.2 The protocol

| Scenario                                                        | Result                                                     |
| --------------------------------------------------------------- | ---------------------------------------------------------- |
| `$filter`, `$orderby`, `$top`, `$skip`, `$select`, `$expand`    | 200                                                        |
| `$inlinecount=allpages`                                         | 200, `__count` as a **string**, as V2 prescribes           |
| `/$count` path segment                                          | 200, `text/plain`                                          |
| V2 filter literals: `guid'…'`, `datetime'…'`, `substringof(…)`  | 200                                                        |
| `$links`                                                        | 200 — reading; writing is not implemented, see §4          |
| Navigation, single- and collection-valued                       | 200                                                        |
| Property access and `/$value`                                   | 200                                                        |
| Create, read, replace (`PUT`), delete                           | 201 / 200 / 204 / 204                                      |
| `MERGE` tunnelled through `POST` + `X-HTTP-Method`              | 204                                                        |
| Media link entry: `GET` and `PUT` on `/$value`                  | 200 / 204                                                  |
| `$batch`, multipart                                             | 202, with the inner response embedded                      |
| `$format=json`, and Atom as the default                         | ✅ both — unlike the CAP adapter, which answers 501 for XML |
| Errors                                                          | `{"error":{"code":…,"message":{"lang":"en","value":…}}}`   |

### 2.3 Operations

All 26 work, covering every V2 return-type variant:

| Return type             | Examples                                                              |
| ----------------------- | --------------------------------------------------------------------- |
| _(none)_                | `ClosureDay`, `CheckOut` → 204 — but only after §3.2                  |
| primitive               | `TotalMediaCount` (Int64 as string), `OutstandingBalance`, `Reserve`  |
| `Collection(primitive)` | `AllLanguages`, `AvailableLanguages`, `CleanUpKeywords`, `BulkRenew`  |
| complex                 | `LoanMetrics`, `LoanStatistics`, `YearEndClosing`, `AssessCondition`  |
| `Collection(complex)`   | `StatsPerBranch`, `NoticeHistory`, `RunOverdueNotices`, `RunReminders` |
| entity                  | `MostReadMedium`, `AvailableCopy`, `Renew`                            |
| `Collection(entity)`    | `NewReleases`, `Search`, `AvailableCopies`, `RenewAll`, `RunStockCheck` |

A primitive result is keyed by the operation name (`{"d":{"TotalMediaCount":"9"}}`), which is exactly what
`ODataValueResponseV2` in odata2ts expects — and notably **not** what the CAP V2 adapter does for
`Edm.Int64`, where the value arrives wrapped a second time.

### 2.4 Data types

Every type is serialized as V2 prescribes — verified against the running service:

| Type                                  | On the wire                | | Type                | On the wire        |
| ------------------------------------- | -------------------------- |-| ------------------- | ------------------ |
| `Edm.Int64`, `Decimal`, `Double`, `Single` | `"1841000"`, `"0.31"` — string | | `Edm.Byte`, `SByte` | `2`, `-2` — number |
| `Edm.Int16`, `Int32`                  | `224` — number             | | `Edm.Guid`          | bare in the payload, `guid'…'` in the URL |
| `Edm.DateTime`                        | `/Date(1517443200000)/`    | | `Edm.Time`          | `PT9H0M0S`         |
| `Edm.DateTimeOffset`                  | `/Date(…)/`                | | `Edm.Binary`        | base64             |

Worth pinning: `Edm.Byte` arrives as a **number**. odata2ts maps `Edm.Byte`/`Edm.SByte` to `string` in its
V2 digester, which is right for the four string-serialised types above and wrong for these two — the same
divergence the CAP adapter exposes, now confirmed against a second, independent V2 implementation.

---

## 3. The gaps

### 3.1 Optimistic concurrency is announced but not enforced

`Copy.Condition` is `ConcurrencyMode="Fixed"`, and Olingo does the visible half correctly: every copy
carries an `ETag: W/"1"` header, and a write without `If-Match` is refused with **428 Precondition
Required**.

It never compares the value:

```
DELETE Copies(…,InventoryNumber=1001)                      -> 428   (no If-Match)
DELETE Copies(…,InventoryNumber=1001)  If-Match: W/"99"    -> 204   (stale, and the entity is gone)
GET    Copies(…,InventoryNumber=1001)                      -> 404
```

The token is required to be *present* and is then ignored, so **412 Precondition Failed is unreachable**
and two clients can overwrite each other while both believe they are protected. That is worse than not
implementing it: a client that tests for 428 concludes the service supports optimistic concurrency.

This is the one gap that makes a feature the model deliberately includes untestable, and it is the obvious
candidate for a follow-up — enforcing it means overriding `deleteEntity` and `updateEntity` to compare the
token. CAP gets this right (test-server-cap FEATURE-COVERAGE.md §4.2: 428 / 200 / 412 all correct).

### 3.2 `ListsProcessor` cannot express an operation that returns nothing

V2 allows a service operation to return no value — [MS-ODATA] grades return values as "may return
nothing", and the model uses it twice. `ListsProcessor.executeFunctionImport` dereferences
`functionImport.getReturnType().getType()` before anything else, so a function import declared without a
return type fails with a `NullPointerException`; and even past that, the method answers **404** whenever
the data source returns `null`.

Fixed here by overriding the method
([`LibraryProcessor`](src/main/java/org/odata2ts/library/LibraryProcessor.java)) to answer 204. It is the
only place in this server where Olingo's processor had to be extended rather than used.

### 3.3 A service cannot declare its own `DataServiceVersion`

The runtime is 2.0 throughout — every response carries `DataServiceVersion: 2.0`, collections are wrapped
in `results`, `$inlinecount` and `$select` work. But `$metadata` declared
**`m:DataServiceVersion="1.0"`**, and a hand-written `EdmProvider` has no way to say otherwise:
`EdmServiceMetadataImplProv.getDataServiceVersion()` returns 1.0 unless it happens to find a property
carrying `CustomizableFeedMappings` with `FcKeepInContent=false` — an Atom feed-customisation flag with
nothing to do with the protocol version. There is no setter, and the `DataServices` object that would
carry the value is built internally.

Left alone, the service would lie about itself: a client that believes the declaration stops expecting
`results`, `__count`, `$select` and `$skiptoken`, all of which work. Corrected here by a filter
([`DataServiceVersionFilter`](src/main/java/org/odata2ts/library/DataServiceVersionFilter.java)) that
rewrites that one attribute on the metadata response and touches nothing else.

### 3.4 Operation parameters are typed from the literal, not the declaration

`ListsProcessor.mapFunctionParameters` converts each argument using `literal.getType()` — the type Olingo
**infers from the text it received** — and ignores the declared parameter type entirely. A parameter
declared `Edm.Int32` therefore arrives as:

| Request              | Java type in the handler |
| -------------------- | ------------------------ |
| `MemberId=2`         | `Byte`                   |
| `Year=2024`          | `Short`                  |
| `MemberId=70000`     | `Integer`                |

So the same operation hands a handler a different type depending on the *value* it was called with, and
the naive cast throws `ClassCastException` at runtime — a 500 the client cannot do anything about. Every
numeric parameter in this server is read through a normalising helper rather than cast.

### 3.5 Smaller things

| Observation                             | Detail                                                                     |
| --------------------------------------- | -------------------------------------------------------------------------- |
| `POST` operations answer **201**        | A side-effecting operation returning data answers 201 Created, not 200 — even when it creates nothing |
| `$batch` answers **202**                | The envelope is 202 Accepted; the embedded responses carry their own status |

---

## 4. Not implemented here

Deliberate, and not an Olingo limitation:

- **`$links` writes** (`POST`/`PUT`/`DELETE` on a `$links` URI) answer `501`. Reading links works. Every
  relationship in this model is carried by a foreign key on the dependent entity, so a link write would
  have to reach across into the other side; V1–V3 grade this a `MAY`.

---

## 5. Overview

**Realizable** answers "can Olingo 2 serve this?" — ✅ yes, ⚠️ with a caveat, ❌ no.

| Feature                                     | Realizable | Note                                                        |
| ------------------------------------------- | :--------: | ----------------------------------------------------------- |
| Entity types, composite keys, complex types |     ✅     |                                                             |
| Complex type inheritance, abstract          |     ✅     |                                                             |
| Associations, referential constraints, cascade | ✅      |                                                             |
| Multiple namespaces                         |     ✅     | four, incl. a deliberate type-name collision                |
| Media link entries                          |     ✅     | read and write on `/$value`                                 |
| All query options V2 defines                |     ✅     | incl. `$inlinecount`, `/$count`, `$links` (read)            |
| CRUD incl. `MERGE` tunnelling               |     ✅     |                                                             |
| All 26 operations, every return type        |     ✅     | void ones only after extending the processor (§3.2)         |
| Data type serialization                     |     ✅     | every type as V2 prescribes                                 |
| `$batch`, Atom **and** JSON                 |     ✅     |                                                             |
| **Entity type inheritance**                 |     ⚠️     | renders into `$metadata`, cannot be serialized (§1)         |
| Operations returning nothing                |     ⚠️     | needs a processor override (§3.2)                           |
| `DataServiceVersion` declaration            |     ⚠️     | not settable; corrected by a filter (§3.3)                  |
| Typed operation parameters                  |     ⚠️     | typed from the literal, not the declaration (§3.4)          |
| **Optimistic concurrency**                  |     ❌     | 428 without `If-Match`, but the token is never compared (§3.1) |

---

## Conclusion

For a retired project, Olingo 2 holds up remarkably well. The protocol surface of OData V2 is essentially
complete and correct — every query option, every payload shape, every data type serialization, all 26
operations across every return-type variant, media link entries, `$batch`, and both wire formats. Almost
none of that needed anything but a `DataSource` and an `EdmProvider`.

The gaps cluster in one revealing place: **things the metadata can say that the runtime cannot do.**
Inheritance renders and does not serialize. A concurrency token is declared, demanded, and never checked.
A version is declared that the service itself contradicts. A parameter's declared type is discarded in
favour of one guessed from the request. In each case `$metadata` promises something the runtime does not
deliver — and a generated client, which has nothing *but* `$metadata` to go on, is exactly the consumer
that gets hurt.

Two of those this server corrects, because leaving them would have made it lie about the protocol rather
than about its own limits. The other two are documented and kept visible, which is what this document is
for.
