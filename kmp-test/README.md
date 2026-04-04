# kmp-test

This module is a consumer-side compatibility test for Restaurant's multiplatform API surface.
It exists to catch JVM-only code leaking into `commonMain` by compiling and running common tests
against both JVM and `iosSimulatorArm64`.

Today this module depends on every production module that is KMP-consumable:

- `restaurant-api`
- `restaurant-kotlinx-serialization`

Add more dependencies here when more modules expose a real non-JVM target and are intended to be
consumed from `commonMain`.
