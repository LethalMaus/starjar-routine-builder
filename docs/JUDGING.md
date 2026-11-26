# Evaluation walkthrough

The companion is free and has no purchase or subscription. StarJar is optional. Use invented situations, not names or other personal details.

## Access

- Web: https://starjar-hybrid-ai-2026--owner-trial-3bw6xpvp.web.app — hosting expiry is 26 December 2026.
- Android: [evaluation release](https://github.com/LethalMaus/starjar-routine-builder/releases/tag/v0.10.0-evaluation), version 0.10-hackathon (10), Android 8/API 26 or later. Download the APK and check its SHA-256 against the included checksum file.
- iOS: [TestFlight invitation](https://testflight.apple.com/join/rfumXyZV), install Apple’s TestFlight app and open the invitation to see the latest approved external build and its expiry. App Store installation is not required.

The Android APK uses the companion’s standalone signing certificate, which differs from Google Play’s managed app-signing certificate. Do not try to install it over a Play installation or uninstall to work around a mismatch without exporting plans and planning the migration. It does not replace the separate StarJar app.

## Try a plan

1. Open the app. The web version has an adult confirmation.
2. Describe an invented situation, for example: “Getting out of bed before school is difficult. Dressing already works. My child enjoys music and board games.”
3. For local English generation in Chrome, follow [local AI setup](LOCAL_AI_SETUP.md). Download the compatible model before going offline. Leave cloud permission and advanced generation options off.
4. Check the route label returned with the result. Keep an idea, add more context, and request alternatives. Existing choices should remain unchanged.
5. For German generation or advanced generated settings, explicitly allow cloud processing. App Check remains required. The cloud route does not require a local model.
6. Review and edit the ideas. Export kept items as StarJar templates for review in StarJar, or save an editable plan to transfer between companion versions. Sharing opens the system chooser; it does not prove that the receiving app saved anything.
7. Use the web print option to try a paper plan without StarJar. Drafts and saved plans remain in local device storage; export them before clearing app/browser data.

## Tested routes and limits

Chrome Gemma 4 has completed on-device generation and offline follow-up through Firebase AI Logic. Model identity comes from Chrome component metadata; inference route comes from the SDK response. German local Chrome output was unreliable, so German currently uses cloud generation or manual editing.

Android local evidence is from a Pixel 9 with stable Nano-v3, not Nano 4/Gemma 4. iOS protected cloud generation is tested. Simulator-on-compatible-Mac local inference is separate evidence; no physical iPhone local-model result is claimed. The signed Android 0.10 (10) APK was checksum-verified and launched on an Android 14 tablet emulator. The earlier public Android 0.9 evaluation APK was downloaded, checksum-verified, installed in place on a Pixel 9, and verified with both on-device generation and a consented cloud result using its release App Check provider. This verifies the standalone APK route, not Google Play installation. TestFlight installation and attestation still need final end-to-end verification.

Cloud processing requires explicit session consent. The web app has no per-browser generation counter; native evaluation builds omit the installation courtesy counter. Shared service limits remain: currently 100 model requests per day and a configured EUR2 monthly Gemini service pause, within an approved EUR5 budget period through 16 December. These controls can interrupt cloud use. Hosting expiry does not guarantee unrestricted cloud availability. Manual editing remains available; local AI requires compatible hardware and a downloaded model.

See [usage policy](USAGE_POLICY.md), [asset rights](ASSET_LICENSES.md) and [pre-existing work](EXISTING_WORK.md). Suggestions require an adult to review their suitability.
