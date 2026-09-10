# Changelog

## [0.2.4](https://github.com/odata2ts/test-server-olingo-v2/compare/v0.2.3...v0.2.4) (2026-09-10)


### Bug Fixes

* **ci:** re-run release-please after promote to fix the draft-release race ([695f217](https://github.com/odata2ts/test-server-olingo-v2/commit/695f21714b459e7697ffa26b5b4f5a678cf6e2e1))

## [0.2.3](https://github.com/odata2ts/test-server-olingo-v2/compare/v0.2.2...v0.2.3) (2026-09-10)


### Bug Fixes

* resolve $&lt;id&gt; body references within a batch change set ([#18](https://github.com/odata2ts/test-server-olingo-v2/issues/18)) ([537064d](https://github.com/odata2ts/test-server-olingo-v2/commit/537064d1b2a9ba7ba1707e253681779b1183bbf8))

## [0.2.2](https://github.com/odata2ts/test-server-olingo-v2/compare/v0.2.1...v0.2.2) (2026-08-25)


### Build System

* re-cut the current server as 0.2.2 ([b235e91](https://github.com/odata2ts/test-server-olingo-v2/commit/b235e916594892d921d3d5c3efb31d80b6721267))

## [0.2.1](https://github.com/odata2ts/test-server-olingo-v2/compare/v0.2.0...v0.2.1) (2026-08-25)


### Bug Fixes

* accept Edm.Byte the way OData V2 states it ([2cbb0d6](https://github.com/odata2ts/test-server-olingo-v2/commit/2cbb0d67056f87f4c4c4041d5545d13de2571c62))

## [0.2.0](https://github.com/odata2ts/test-server-olingo-v2/compare/v0.1.0...v0.2.0) (2026-08-20)


### Features

* annotate the generated keys and let the client assign Branch ([700ad14](https://github.com/odata2ts/test-server-olingo-v2/commit/700ad14923b6871dfeee63bf1ce21bc71d684d10))

## 0.1.0 (2026-08-19)


### Features

* Apache Olingo 2 implementation of the "Library" OData V2 test model ([271bb33](https://github.com/odata2ts/test-server-olingo-v2/commit/271bb33a86b18f460208d29115d273bec8d1b715))
* state and enforce the managed-property annotations ([df2f417](https://github.com/odata2ts/test-server-olingo-v2/commit/df2f417b4fb2f5b17a1641585d4a6c2f8f1d2af6))
* write relationships stated by $links or by a payload reference ([#2](https://github.com/odata2ts/test-server-olingo-v2/issues/2)) ([7df197d](https://github.com/odata2ts/test-server-olingo-v2/commit/7df197ded53096ff22253256eb10f33a8676d3c5))


### Bug Fixes

* do not unbox a null popularity score in MostReadMedium ([89629aa](https://github.com/odata2ts/test-server-olingo-v2/commit/89629aafa19d0fabb6da0f5bdcbbe5a2db0514f3))
* enforce the concurrency token instead of only demanding it ([#1](https://github.com/odata2ts/test-server-olingo-v2/issues/1)) ([83e9da5](https://github.com/odata2ts/test-server-olingo-v2/commit/83e9da5a016c75f3760caf8f772fe8906a49745d))
