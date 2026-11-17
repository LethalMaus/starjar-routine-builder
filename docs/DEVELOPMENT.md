# Architecture and testing

## Data flow

A request contains a goal, output language, one to three tasks, optional reward and supported constraints. Android uses `androidApp/src/main/assets/routine-instructions.txt`. The browser planning flow uses `web/src/planning-instructions.txt` to turn a difficulty, optional time window and revision feedback into a small routine. Firebase AI Logic provides local and cloud inference; the returned inference source determines the result label.

Generated names are validated before editing. Reviewed names and star amounts are validated again before saving/export. The export constructs a fresh allowlisted `starjar-templates` version-1 envelope, excluding local IDs, routine titles, original requests and generation metadata.

Kotlin validation lives in `core/`; the browser equivalent is `web/src/core.js`. Keep their limits and export contracts aligned. Model output is untrusted data; browser rendering uses text nodes rather than HTML interpolation.

## Tests

Run `./gradlew :core:test` for Kotlin contracts, storage, exports and allowance accounting. Android instrumentation tests cover manual journeys and generation recovery. Compose previews and Paparazzi snapshots cover English/German layouts at multiple sizes and font scales. Runtime and live-inference tests are separate from preview rendering.

From `web/`, run `npm test` for JavaScript validation, storage, export and cancellation tests. After building, run `node tests/browser.mjs` for real browser downloads, persistence, offline reload and layout checks. See [web setup](../web/README.md) for optional live-inference tests and environment variables.

Use synthetic descriptions for live tests. A successful model request demonstrates a route, not general output correctness. Fake transports are useful for deterministic failure and cancellation tests, but are not live-inference evidence.

## Configuration and secrets

Firebase client configuration is supplied through ignored local properties or environment files. Never commit service-account credentials, signing keys or App Check debug tokens. Web variables prefixed `VITE_` are compiled into the client bundle and must not contain server secrets.

## Offline behaviour

Android stores edited drafts/routines in app-private no-backup files. The browser uses local storage and caches bundled application assets through a service worker. Cloud traffic, credentials and prompts are not cached by the service worker. Local generation also requires a downloaded browser model.
