# StarJar Routine Builder

Find tasks and rewards to help your child with an everyday difficulty, review the ideas and export your choices to StarJar. Routine Builder is for adults aged 18 and over. It includes an Android app and a Chrome web app, with English and German interfaces.

You can write routines yourself without an account, subscription or AI service. AI suggestions always open in the editor for review; they are never added to StarJar automatically.

## Try the web app locally

Requirements: Node.js 24.12 or later and npm.

```sh
cd web
npm ci
npm test
npm run build
npm run preview
```

Open the localhost address printed by Vite. See [web setup](web/README.md) for local AI, optional cloud configuration and browser tests.

The Chrome app suggests a suitable number of task and reward alternatives. Keep, edit or discard each idea; asking for more adds suggestions without replacing existing cards. Optional reward preferences exclude purchases or screen time. Android currently uses the earlier fixed-size routine flow.

## Build the Android app

Requirements: Android SDK 36, Java 17 for compilation and Java 21 for preview tests. Gradle 9.1.0 is included. Set `ANDROID_HOME` or provide `sdk.dir` in an ignored `local.properties` file.

```sh
./gradlew :core:test :androidApp:assembleDebug
./gradlew :androidApp:recordPaparazziDebug
```

The debug APK is written to `androidApp/build/outputs/apk/debug/`. The minimum supported Android version is API 26. Without Firebase configuration, the manual editor, saved routines and exports remain available.

For cloud generation, copy `firebase.example.properties` to `firebase.local.properties` and configure a Firebase Android app with Firebase AI Logic and App Check enforcement. Development builds use registered App Check debug tokens. Release builds use reCAPTCHA Enterprise: configure a package-restricted Android key, non-Play distribution support and its App Check registration, then set `recaptchaSiteKey`.

To exercise the real provider in a debug build:

```sh
./gradlew :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest -PrecaptchaAppCheck=true
```

For signed releases, copy `signing.example.properties` to `signing.local.properties` and configure your own keystore. Run `./gradlew :androidApp:assembleRelease`. Without signing configuration, this produces an unsigned APK. Private signing files and debug tokens must not be committed.

## How generation works

Local-only is the default. Cloud fallback requires explicit consent for the current session and uses Firebase AI Logic with `gemini-3.5-flash-lite`. Results are labelled using the SDK's actual inference source. A compatible Chrome setup with a downloaded model supports offline generation. Android on-device support depends on the device and model availability; it was unavailable on the tested Pixel 9 configuration.

Suggestions can contain mistakes. Review names, language, suitability and star amounts before saving or exporting. [Architecture and testing](docs/DEVELOPMENT.md) describes the validation boundaries and test suites.

## Saving, privacy and export

- Your original problem description, age range and follow-up feedback stay in memory. When cloud use is allowed, these and existing/discarded idea context can be sent to Google's model service. Edited task/reward descriptions are saved with the plan.
- Edited drafts and saved routines stay on the device: browser local storage on web, app-private no-backup files on Android. They are not synchronised between devices.
- App Check processes app/device signals. Model downloads and provider verification can use the network independently of prompt inference.
- The Chrome app exports only items marked Kept, including names, descriptions, stars, icons, colours, frequency limits and approval settings. Android currently retains the earlier basic names/stars editor and export. Import the file using StarJar's template library.
- Export a copy before clearing browser/app data. There is no account-based backup or restore.

See [usage limits](docs/USAGE_POLICY.md) for the differences between Android and web.

## Project layout

| Directory | Purpose |
| --- | --- |
| `androidApp/` | Compose Android app, Firebase adapters and UI tests |
| `core/` | Kotlin validation, persistence, usage accounting and export |
| `web/` | Browser app, offline caching and browser tests |
| `docs/` | Developer documentation, asset inventory and existing-work disclosure |
| `licenses/` | Artwork and font licence notices |

## Licences

Code and adapted theme: Apache 2.0. Starry and launcher artwork: CC BY 4.0. Nunito: SIL Open Font License. See [LICENSE](LICENSE), [NOTICE](NOTICE), [asset rights](docs/ASSET_LICENSES.md) and [existing work](docs/EXISTING_WORK.md).
