# Building evaluation packages

The web, Android and iOS applications can be tried independently. StarJar is optional: it is needed only to use exported templates in StarJar. There is no subscription or purchase in this companion.

## Web

Build `web/dist` with Node 24.12 or later and deploy it over HTTPS. The repository includes a Firebase Hosting configuration. Register the exact deployment hostname on the configured reCAPTCHA Enterprise web key, keep Firebase AI Logic App Check enforcement enabled, and verify real attestation in normal Chrome. A development debug token is not distribution evidence.

Firebase preview channels expire after at most 30 days. Set an expiry beyond the required evaluation period, read it back after deployment, and check the URL rather than relying on a local build. Keep the app and model loaded before testing offline behaviour. See [local AI setup](LOCAL_AI_SETUP.md) for Chrome prerequisites and language limits.

## Android

Configure the ignored Firebase and signing properties described in the main README. Build an evaluation APK with:

```sh
./gradlew :core:test :shared:testDebugUnitTest :androidApp:assembleRelease -PjudgingAccess=true
```

Evaluation disables the installation's courtesy cloud counter, not App Check or explicit cloud consent. The Release variant uses the real reCAPTCHA provider. Verify the package, version, release signature and non-debuggable manifest before distributing the APK. Test real attestation on the signed package; an emulator may fail attestation even when the interface works correctly.

Share the signed APK through a trusted download location and provide its SHA-256 checksum. Android may ask users to permit installation from that download source. For an existing installation, use an update signed with the same certificate; do not uninstall or clear saved plans to work around a signing mismatch. Export plans first if a migration is required.

## iOS

The Evaluation configuration disables the installation's courtesy cloud counter and uses real reCAPTCHA App Check. It requires the explicit bundle ID, App Attest capability and production App Attest entitlement, matching provisioning, and the ignored Firebase/App Check configuration described in the README.

Build the release `RoutineShared` framework and resources, generate the Xcode project, then build or archive the Evaluation configuration with the intended signing identity. Keep one Kotlin/Native framework in the app process.

A development-signed app only runs on registered devices. It is not a general judge download. For external iOS testers, prepare an App Store Connect app record, a distribution-signed upload and an external TestFlight group. Apple processing, required declarations and beta review must complete before an external invitation can be treated as working access. Check the actual TestFlight build's expiry against the evaluation period and test installation through that route. Building a development archive does not satisfy these steps.

## Access and evidence

No platform's evaluation build guarantees uninterrupted cloud service: service quotas and billing controls still apply. Keep those controls active and document any limitations. No shared App Check debug token belongs in a distributed package or instructions.

Record the exact build/hash, test device and OS, SDK inference route, model evidence and relevant limitations. Android Nano-v3, Chrome Gemma 4 and Apple Intelligence are different evidence. A simulator using the Mac's local model is not a physical iPhone local-model test. For generated content, passing JSON validation does not replace a parent reviewing suitability and wording.
