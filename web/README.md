# Chrome companion

Browser application for parents to find, review, save and export tasks and rewards for their children. See [licences](../NOTICE) for the reused artwork and fonts.

## Run locally

Use Node 24.12 or later. From this directory:

```
npm ci
npm test
npm run build
npm run preview
```

Open the printed localhost address. No account or cloud configuration is needed for manual editing, saving, exporting, or local generation. Local generation needs compatible Chrome hardware and a downloaded model. For the hackathon's Gemma 4 setup, enable `chrome://flags/#gemma4-for-built-in-ai` and relaunch Chrome. Chrome manages the model; the application does not select or identify its precise version. Actual runtime source is read from Firebase's `response.inferenceSource`.

The service worker caches only bundled app files. After a successful first online visit, reload offline to use editing/saved routines; AI also requires a downloaded local model. Browser storage is device/profile-specific and is not a backup. Export a JSON copy before clearing browser data. Goals are held only in memory, never saved in routine storage. Edited drafts and saved routines stay in localStorage. Cloud permission resets on reload. reCAPTCHA provider traffic starts only after cloud use is allowed and requested.

## Optional protected cloud fallback

Copy `.env.example` to ignored `.env.local`, using a dedicated Firebase web app and a reCAPTCHA Enterprise score-based site key registered with Firebase App Check for the chosen hosting domain. Enforce App Check on Firebase AI Logic. Rebuild. No backend service-account credential belongs in this app. Firebase client configuration/site keys are not server secrets, and must be restricted appropriately for your deployment.

For localhost testing, use Firebase's supported debug provider: a private browser harness can set `self.FIREBASE_APPCHECK_DEBUG_TOKEN` **before loading the app**, and register that token in the Firebase console. Never commit, bundle, screenshot, or publish that value. Do not embed a shared debug token in a public build. For deployment, register the hosting domain with the real provider and test App Check on that domain.

The cloud checkbox uses Firebase `PREFER_ON_DEVICE` when the local model is ready, otherwise `ONLY_IN_CLOUD` with `gemini-3.5-flash-lite`. A capability probe is bounded to three seconds so a stalled browser check cannot block an approved fallback. Unchecked uses `ONLY_ON_DEVICE`. Cloud failures do not silently bypass App Check. Output is validated before it reaches the editor. The local and cloud routes share the same browser planning prompt. Cancellation attempts to abort transport and suppresses late results; already-sent requests can still run/bill server-side.

The web app has no client request counter. Configure server quotas and billing controls for your deployment; see [usage limits](../docs/USAGE_POLICY.md).

## Tests

- `npm test`: request/output validation, strict stars, export allowlist, persistence and cancellation.
- `node tests/browser.mjs`: built app in isolated installed Chrome; EN/DE real downloads, saving, narrow layouts, offline reload/reopen. `ROUTINE_EVIDENCE` selects the output folder.
- `node tests/local-live.mjs`: opt-in real model test. Uses an isolated `/tmp/routine-parent-local-test-profile` (override `ROUTINE_CHROME_PROFILE`), may download several GB. Checks generation and adding ideas while offline, preserving existing cards. No paid cloud call.

- `node tests/cloud-live.mjs`: opt-in real cloud requests using your configured Firebase project. This can incur charges. It checks the App Check header and actual cloud inference source.

Synthetic recovery tests do not exercise real models. Run live tests explicitly using synthetic, non-personal descriptions.

Browser tests use an installed Google Chrome. Set `ROUTINE_CHROME_PATH` to use a custom browser executable.

## Planning for your child

Describe what is difficult and what already works. Optionally give a broad age range and the time available (1–180 minutes). The app suggests task and reward alternatives; there is no fixed task-count selector. Purchase-free and screen-free reward preferences are optional.

Keep, edit or discard each card. Asking for more ideas sends your additional context, current cards and discarded names. Existing cards and selections stay unchanged. Exact-name repeats are filtered locally; semantic duplicates can still occur and need review.

Only kept items are exported: names, descriptions, stars, icons, colours, frequency limits and approval settings, in StarJar's version-1 template format. Badges, reminders and streak bonuses are not generated. Import through StarJar's template library; direct app-to-app transfer is not implemented.

The original goal, age, time and follow-up feedback remain in memory for the current page session. Edited cards, kept choices and discarded names are stored locally. Reloading restores the draft but not the conversation context. Existing version-1 browser drafts and saved routines are migrated when opened; their original records remain until explicitly replaced or deleted.
