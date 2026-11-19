# Try local generation in Chrome

The web app can generate basic task and reward ideas through Firebase AI Logic using Chrome's on-device model. A local result is labelled **Generated on this device**. The app reports the SDK's inference route; that label does not identify a particular model version.

## Set up the browser

1. Use a current **desktop Google Chrome** on hardware supported by Chrome's built-in AI. Mobile Chrome, an Android emulator and another Chromium-based browser are not equivalent substitutes.
2. Update Chrome at `chrome://settings/help`.
3. For the Gemma 4 preview setup, enable `chrome://flags/#gemma4-for-built-in-ai` and relaunch Chrome. This is the setup recommended by the BuildHybridAI organisers. If the flag is absent, do not assume the default model is Gemma 4.
4. Follow the current [Firebase web hybrid setup](https://firebase.google.com/docs/ai-logic/hybrid/web/get-started) for Prompt API prerequisites. Model availability depends on Chrome, hardware, browser policy and sufficient free storage. The first use can download several GB and take several minutes. Keep the browser open and online until the model is ready.
5. Start this repository's web app using the commands in [web setup](../web/README.md), or use its published address when one is supplied. Localhost is suitable for development; a remote deployment needs HTTPS.

Flags and model versions are experimental and may change. Do not change a managed browser policy or clear your everyday browser profile to force availability. A separate test profile avoids mixing synthetic trial data with personal browsing and saved plans.

## Current language limitation

Local AI suggestions are enabled for English. German local output did not meet the quality bar and is disabled: the app explains this before generation and never sends the request to the cloud without permission. German manual editing works offline; German AI generation uses the separately configured cloud route only with explicit session consent.

Local generation asks for names and descriptions only. The app assigns editable defaults: 5 stars for tasks, 20 for rewards, valid icons/colours, parent approval and no frequency limit. Review these before export. Local suggestions can still repeat an activity in different words or be vague; exact-name duplicate filtering is not a semantic quality guarantee.

## Verify the local route

1. Open the app and complete the web age confirmation.
2. Leave **Allow cloud generation** unchecked. Leave badges blank and AI-generated frequency limits, streaks, reminders and urgency off. You can add these manually afterwards.
3. Enter a synthetic description, for example: “Getting out of bed is difficult for a primary-school child. Dressing is fine. They enjoy music and games.” Select a broad age and available time if helpful.
4. Generate suggestions. Wait for the result and check that its receipt says **Generated on this device**. An error or a cloud receipt is not a local pass.
5. Keep or edit some ideas. Disconnect from the network after the model and application have loaded, then ask for another reward idea. Confirm another local receipt and that the earlier choices remain unchanged.
6. Keep the desired cards and export them. The file should contain `format: "starjar-templates"`, `version: 1`, `tasks` and `rewards`. You can import it through StarJar's template library or back into Routine Builder for review.

The cached app can reload offline after its first successful load. Original descriptions and feedback are deliberately not persisted; add relevant context again after reloading. Suggestions, including spelling, relevance and possible duplicates, still need parent review.

## Reproducible developer test

From `web`, after `npm ci` and `npm run build`:

```sh
ROUTINE_TEST_LANGUAGE=en node tests/local-live.mjs
ROUTINE_TEST_LANGUAGE=de node tests/local-live.mjs # checks refusal, not German inference
```

Use `ROUTINE_CHROME_PROFILE` to reuse a dedicated model profile and `ROUTINE_EVIDENCE` for results. The test clears only the test origin's storage in that profile. Do not point it at an everyday profile. It uses Chrome's experimental `OnDeviceModelConversationBackend`, `OptimizationGuideManifestBroker` and `AIApiFoundationalModel:model_version/v4` features. These automation switches are version-specific, not an application-level model selector.

The app declares expected input/output languages to Chrome and uses matching language options for availability checks. This follows the [Prompt API guidance](https://developer.chrome.com/docs/ai/prompt-api#add_expected_input_and_output); an available model does not guarantee fluent output.

The test records the Chrome version, local receipt, synthetic ideas, offline follow-up, preservation of existing cards, downloaded export/reimport, offline reload and observed cloud inference requests. Check Chrome's model diagnostics/component metadata separately to establish the installed model identity. A local receipt alone is insufficient evidence of Gemma 4.

## Cloud and other platforms

Optional cloud generation needs the separate Firebase/App Check setup in [web setup](../web/README.md). Never publish a shared App Check debug token. Keep enforcement enabled. Cloud access is not necessary for the local-only test above.

Android and iOS use their own platform models. A Pixel running Nano-v3 or an Apple Intelligence result must not be described as Gemma 4. Competition eligibility is determined by the organisers; model metadata and working offline generation do not themselves establish eligibility.
