# StarJar Routine Builder

Find tasks and rewards to help your child with an everyday difficulty, review the ideas and export your choices to StarJar. Routine Builder is for adults aged 18 and over. It includes Android and iOS apps sharing a Kotlin/Compose editor, and a Chrome web app, with English and German interfaces. Native apps follow the phone language (German when selected, English for unsupported languages) and open directly into the editor without an age-confirmation screen.

You can write routines yourself without an account, subscription or AI service. AI suggestions always open in the editor for review; they are never added to StarJar automatically.

## Try the evaluation builds

- [Web app](https://starjar-hybrid-ai-2026--owner-trial-3bw6xpvp.web.app)
- [Android evaluation APK and checksum](https://github.com/LethalMaus/starjar-routine-builder/releases/tag/v0.10.0-evaluation)
- [iOS TestFlight invitation](https://testflight.apple.com/join/rfumXyZV)

See the [evaluation walkthrough](docs/JUDGING.md) for tested models, offline setup, signing differences and cloud availability limits. These are evaluation builds, not a production release.

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

The Chrome app suggests a suitable number of task and reward alternatives. Keep, edit or discard each idea; asking for more adds suggestions without replacing existing cards. Optional reward preferences exclude purchases or screen time. The native apps use the same keep/edit/discard approach and StarJar template format.

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

## Build the iOS app

Requirements: macOS, Xcode, Java 17 and Ruby with the `xcodeproj` gem. The app links one static Kotlin framework, `RoutineShared`.

```sh
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:prepareIosResources
ruby iosApp/generate-project.rb
open iosApp/RoutineBuilder.xcodeproj
```

Select an iOS simulator and run the `RoutineBuilder` scheme. For a physical device, build `:shared:linkDebugFrameworkIosArm64` and choose your own signing team in Xcode. The host selects the matching framework by SDK; build the release framework before a Release build.

For Firebase AI generation (local or cloud), add your own ignored `iosApp/GoogleService-Info.plist`, regenerate the project, and register the development App Check token privately. Release and Evaluation builds use reCAPTCHA Enterprise App Check with an iOS bundle-restricted key in ignored `iosApp/AppCheck.local.plist` (`RecaptchaSiteKey`). Register that key in Firebase App Check, keep enforcement enabled, and validate the actual distribution route. See [development setup](docs/DEVELOPMENT.md). The separate `RoutineBuilderLiveAI` scheme makes real model requests and can consume quota.

The iOS Firebase SDK is pinned to 12.19.3 for its documented hybrid `SystemLanguageModel` API. Local generation requires Apple Intelligence with its model available. A supported iOS simulator can use the compatible host Mac’s local model; record that as simulator-on-Mac inference, not a physical iPhone/iPad test. Unsupported physical devices can still use manual editing and consent-based cloud generation.

## Transfer and paper plans

Save an editable plan to move choices between web, Android and iOS. Opening it shows a confirmation before replacing the current draft. StarJar template exports contain only kept items; sharing opens the operating system's chooser and does not confirm that another app saved the file. Browser file sharing depends on HTTPS and browser support; download remains available.

The web app can print a weekly task and reward chart without StarJar. Paper ticks do not sync to an app, and the chart does not calculate advanced bonuses.

## How generation works

Local-only is the default. Cloud fallback requires explicit consent for the current session and uses Firebase AI Logic with `gemini-3.5-flash-lite`. Results are labelled using the SDK's actual inference source. A compatible Chrome setup with a downloaded model supports offline generation. Android tries the available preview and stable model routes. Local generation was verified on a Pixel 9 using the stable Nano-v3 model; this is not evidence of Nano 4/Gemma 4. Preview-model availability still depends on the device and programme access.

Suggestions can contain mistakes. Review names, language, suitability and star amounts before saving or exporting. [Architecture and testing](docs/DEVELOPMENT.md) describes the validation boundaries and test suites.

## Saving, privacy and export

- Your original problem description, age range and follow-up feedback stay in memory. When cloud use is allowed, these and existing/discarded idea context can be sent to Google's model service. Edited task/reward descriptions are saved with the plan.
- Edited drafts and saved routines stay on the device: browser local storage on web, app-private no-backup files on Android, and app preferences on iOS. They are not synchronised between devices. Native saved-plan deletion asks for confirmation and removes only the saved copy, leaving the current draft intact. If saved data cannot be read, the app reports it and blocks overwriting it; you can still edit and export your current ideas.
- On Android, you can explicitly report a selected suggestion with an optional comment. Only that suggestion’s name and description plus your comment are sent to private developer storage; the original prompt and other ideas are not included. Reports expire after 30 days and are deleted by an hourly job. Technical infrastructure logs have separate retention. See [reporting setup and privacy](reporting/README.md). iOS and web do not currently offer this action.
- App Check processes app/device signals. Model downloads and provider verification can use the network independently of prompt inference.
- All platforms export only items marked Kept, including names, descriptions, stars, all StarJar icons, colours, frequency limits, approval settings and selected task-only options (badges, streaks, reminders and urgency). Native apps support the same task options. Import the file using StarJar's template library; direct OS sharing requires a compatible StarJar build.
- Export a copy before clearing browser/app data. There is no account-based backup or restore.

See [usage limits](docs/USAGE_POLICY.md) for the differences between Android and web.

See [evaluation package distribution](docs/DISTRIBUTION.md) for Android signing, iOS TestFlight requirements and hosting expiry checks.

## Project layout

| Directory | Purpose |
| --- | --- |
| `androidApp/` | Compose Android app, Firebase adapters and UI tests |
| `shared/` | Compose Android/iOS editor, full template contract and generation validation |
| `iosApp/` | Swift host, Firebase adapter and native document sharing |
| `core/` | Earlier routine format, migration support and Android usage accounting |
| `web/` | Browser app, offline caching and browser tests |
| `docs/` | Developer documentation, asset inventory and existing-work disclosure |
| `licenses/` | Artwork and font licence notices |

## Licences

Code and adapted theme: Apache 2.0. Starry and launcher artwork: CC BY 4.0. Nunito: SIL Open Font License. See [LICENSE](LICENSE), [NOTICE](NOTICE), [asset rights](docs/ASSET_LICENSES.md) and [existing work](docs/EXISTING_WORK.md).
