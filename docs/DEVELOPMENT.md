# Architecture and testing

## Data flow

The shared Android/iOS editor and browser planning flow turn a child’s difficulty, optional age/time context and feedback into task and reward alternatives. Native instructions are under `shared/src/commonMain/composeResources/files/`; web instructions are in `web/src/`. Firebase AI Logic provides generation. Android/web use the returned inference source; iOS records which explicit Firebase model session completed.

Model output is untrusted. `shared/PlanEngine` and `web/src/parent-plan.js` validate it before merging, preserve existing choices and exclude discarded names. Fields are validated again before export: stars, icon/colour allowlists, frequency limits, approval, badges, streaks, reminders and urgency. The export constructs a fresh allowlisted `starjar-templates` version-1 envelope without local IDs, original requests or generation metadata. StarJar exports are limited to 100,000 characters; editable plan files are separately bounded to 200,000 UTF-8 bytes.

Editable `starjar-ideas-plan` version-1 files preserve kept/suggested/discarded choices across platforms. Both formats require review before replacing a draft. Android migrates the earlier local routine/editor files without deleting their originals. The `core/` module retains that legacy model and usage accounting. Browser rendering uses text nodes rather than HTML interpolation.

## Tests

Run `./gradlew :core:test :shared:testDebugUnitTest` for Kotlin contracts, storage, exports and allowance accounting. `ParentFlowTest` covers the current native manual creation and share flow. Earlier instrumentation tests target the legacy prototype and need updating before treating the whole suite as current coverage. Compose previews and Paparazzi snapshots cover English/German layouts at multiple sizes and font scales. Runtime and live-inference tests are separate from preview rendering.

From `web/`, run `npm test` for JavaScript validation, storage, export and cancellation tests. After building, run `node tests/browser.mjs` for real browser downloads, persistence, offline reload and layout checks. See [web setup](../web/README.md) for optional live-inference tests and environment variables.

The iOS `RoutineBuilder` scheme runs UI smoke tests. The separate `RoutineBuilderLiveAI` scheme makes live requests. `web/tests/print-transfer-browser.mjs` checks reviewed file opening, editable downloads and EN/DE print output.

Use synthetic descriptions for live tests. A successful model request demonstrates a route, not general output correctness. Fake transports are useful for deterministic failure and cancellation tests, but are not live-inference evidence.

## Configuration and secrets

Firebase client configuration is supplied through ignored local properties or environment files. Never commit service-account credentials, signing keys or App Check debug tokens. Web variables prefixed `VITE_` are compiled into the client bundle and must not contain server secrets.

## Offline behaviour

Android stores edited drafts/routines in app-private no-backup files. The browser uses local storage and caches bundled application assets through a service worker. Cloud traffic, credentials and prompts are not cached by the service worker. Local generation also requires a downloaded browser model.

## Full task options

`task-options.js` validates badge thresholds and counts, streak units, reminder schedules and urgency dates. Rewards never receive task-only fields. Explicit generation preferences override model output; requested badge counts are checked rather than silently shortened. Basic generation supports the local route. AI-generated optional task details require explicit cloud permission; local attempts, including per-task enrichment, exceeded the tested model response window. Manual editing/export of all fields remains offline-capable. Structured schemas omit nested array cardinality constraints because the provider rejects their combined state space; application validation enforces all limits.

## Native accessibility

The shared editor uses labelled, whole-row checkbox controls; contextual task action labels; headings; selected and expanded state; and polite announcements for status updates. Interactive controls have at least 48dp layout targets, with gaps between wrapped rows. Numeric errors include readable supporting text and error semantics, and fields use a contrasting boundary and cursor in both themes.

`ParentFlowTest` checks these semantics and target sizes alongside real editing/export. The iOS UI suite audits the visible creation and editor screens for hit regions, element descriptions, traits, contrast and clipped text. Preview exports cover English/German, small and large screens, doubled font scale and dark mode. Automated checks cover those states; they do not replace a complete VoiceOver/TalkBack user evaluation.

## Evaluation access and live validation

Evaluation builds omit only the client courtesy counter (see [usage policy](USAGE_POLICY.md)). `ParentAccessPolicyTest` and iOS `AccessPolicyTests` verify that an exhausted standard allowance is preserved across evaluation mode; neither makes AI requests. The iOS test is in the `RoutineBuilderLiveAI` scheme, so select only `AccessPolicyTests` when running offline checks.

`ParentLiveInferenceTest` exercises the current shared prompt, schema validation and template export using one synthetic cloud request. It runs only with instrumentation argument `liveParentAi=true`. `LocalCompatibilityTest` checks model categories without downloading a model or making cloud requests when `localProbe=true`. Run these only against your configured test project with App Check and a known budget. They write synthetic evidence under the app's external `acceptance` directory; retrieve and remove it after validation. App Check tokens are never written to those receipts.

For iOS, regenerate the Xcode project with `ruby generate-project.rb` from the `iosApp` directory. `Evaluation` uses release compilation, real App Check and evaluation access; `Release` retains the standard allowance. Both use reCAPTCHA Enterprise App Check. Supply an ignored `iosApp/AppCheck.local.plist` containing a `RecaptchaSiteKey` string, register its iOS key for the exact bundle ID in Firebase App Check, and keep enforcement enabled. For real iOS attestation, enable App Attest on an explicit Apple App ID and regenerate its provisioning profile. The app's release/evaluation entitlements set `com.apple.developer.devicecheck.appattest-environment` to `production`, as required by reCAPTCHA. A wildcard profile is insufficient. Verify the signed app contains this entitlement before device testing. `iosApp/Signing.xcconfig` defaults to automatic signing and optionally includes ignored `Signing.local.xcconfig` overrides for a machine's explicit development profile. Never commit signing credentials or a local profile override.

Missing configuration does not fall back to debug attestation. Debug builds use the Firebase debug provider; register tokens privately, never in source.

Evaluation and Debug builds accept `--routine-probe=attestation` for a forced token check without inference, and `--routine-probe=cloud` for one synthetic paid request. Their receipt is `Documents/routine-generation-probe.json`; retrieve it privately after validation. These diagnostic launch arguments are excluded from ordinary Release builds. The reCAPTCHA mobile provider is currently a Firebase preview feature; review its SDK support lifecycle before distribution.

`LiveAITests/testLocalParentPromptProducesImportableIdeas` and `testLocalBedtimePlanningAndFollowUp` exercise English/German planning and follow-ups with cloud disabled, validates suggestions, and exports/reimports StarJar templates. It saves synthetic response and export attachments in the test result. On a compatible Mac, run it on an iOS 26+ simulator with Apple Intelligence available. A skipped test is not inference evidence; distinguish simulator-on-Mac from physical-device results. Generated native suggestions use Check/Games for unknown icon IDs; file imports remain strict.

Apple local generation uses typed Firebase sessions with a compact request, a limited valid icon palette, and separate task/reward star ranges. The full icon catalogue remains editable. Latest feedback takes priority in follow-ups; the shared merge preserves existing choices and filters discarded names. Review actual output for relevance and natural wording as well as passing schema tests.

`StableLocalInferenceTest` is separately opt-in with `downloadStableLocal=true`. It attempts the supported stable-model download, then a synthetic local-only request with no cloud fallback. It records model status, model name and inference source; a timeout or unavailable model is not a pass. Stable-model results do not prove preview-model availability or competition eligibility.

For stable-model diagnostics, add `fullParentPrompt=true` to test the current text prompt and strict export contract, or `structuredProbe=true` to test the SDK typed-output route separately. A simple typed probe does not establish full parent-schema parity. Structured SDK responses may omit raw response text; validate `getObject()` and the actual inference source.

Android basic planning uses a compact typed task/reward schema. It checks preview model categories before the stable model, and requires the SDK to report `ON_DEVICE` for a local result. Unsupported icon/colour values receive valid visual defaults; star values are bounded to 3–10 for tasks and 10–30 for rewards before the shared validator runs. All icons and advanced fields remain manually editable. Optional AI-generated details use the explicitly consented cloud path. Local generation does not reserve cloud allowance.

`ParentLocalInferenceTest` runs real English/German initial and follow-up requests only with `liveParentLocal=true`, with cloud disabled. It checks shared validation, preservation of edited choices and StarJar template round trips. Keep the test device unlocked and the app in the foreground. Generated suggestions still require parent review; passing structural tests does not guarantee useful advice or natural language. A stable Nano-v3 result does not demonstrate Nano 4/Gemma 4 eligibility.

Chrome local parent planning uses only `{name, description}` items in `tasks` and `rewards`. `local-planning.js` validates the reduced output, supplies editable metadata defaults and delegates to the normal import/export validator. This avoids asking a small model to produce product configuration alongside prose. The cloud-only route retains the full schema. German local parent planning is disabled after live output-quality checks: without consent it throws `LOCAL_LANGUAGE_UNAVAILABLE` before initializing a model or App Check; with consent it selects `ONLY_IN_CLOUD`. English retains the existing consent-aware local preference. Never infer content-quality approval from a structural test pass.

Icon names live in `shared/src/commonMain/composeResources/files/icon-labels.json`. The web imports this catalogue directly; run `node tools/generate-icon-labels.mjs` after editing it to update the native Kotlin map. Both platforms validate coverage against the complete selectable icon catalogue. English is the fallback for other device languages.

Firebase Hosting can serve the built `web/dist` using the repository's `firebase.json`. Use an expiring preview channel for review and register its exact hostname on the configured reCAPTCHA key. A registered development debug token is not proof of real hosted attestation; test the real provider in normal Chrome and keep enforcement enabled. Hosting deployment does not publish the source repository.
