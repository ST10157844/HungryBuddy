# HungryBuddy

**HungryBuddy** is a Kotlin Android recipe-discovery application built with traditional Android XML Views. It allows users to browse meals, search for recipes, view ingredients and cooking instructions, and save favourites for access across sessions and devices.

This documentation describes the current Part 3 implementation on the `part3-settings` branch. The repository's `main` branch has not been merged with this work yet.

- **Repository:** <https://github.com/ST10157844/HungryBuddy>
- **Working branch:** `part3-settings`
- **Module:** OPSC6312 – Open Source Code, PoE Part 3
- **Group:** 5
- **Application ID:** `com.example.hungrybuddy`

## Features

### Accounts and recipe discovery

- Register and sign in with Firebase Authentication using email and password.
- Browse recipe results from TheMealDB.
- Search for meals by name.
- Open recipe details, including available images, category, cuisine/area, ingredients, measurements and cooking instructions.
- Open a recipe's YouTube video when a video link is available.
- Retain the current search query and results when moving between the main tabs.

### Favourites and synchronisation

- Save and remove favourites for the authenticated user.
- Store favourite recipe information in a local Room database so saved recipes can be accessed without making a new recipe API request.
- Synchronise pending favourite additions and deletions with Cloud Firestore.
- Download remote favourites to the local database and refresh records that are already synchronised.
- Schedule background synchronisation when network connectivity is available. The scheduler queues an initial one-time sync and a periodic sync (15-minute interval, subject to Android WorkManager scheduling).
- Stop scheduled synchronisation when the user logs out.

### Settings and notifications

- Persist the selected language tag and dark-mode preference using Android SharedPreferences.
- Include default English resources and additional isiZulu (`values-zu`) and Setswana (`values-tn`) resource folders.
- Schedule a local inactivity reminder 24 hours after the app moves out of the foreground for a signed-in user. The worker checks that the same account is still signed in, the app is not in the foreground, and notification permission is available.
- Receive Firebase Cloud Messaging (FCM) messages through `HungryBuddyMessagingService` and display them as Android notifications.

**Notification distinction:** the 24-hour inactivity reminder is a local WorkManager notification. FCM delivery was tested by sending a message manually from the Firebase Console to the Samsung test device. Automated server-side inactivity detection that sends an FCM message is not implemented.

## Technology stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Android XML Views, ViewBinding, Activities, RecyclerView |
| App state | ViewModel, StateFlow and Kotlin coroutines |
| Recipe API | TheMealDB REST API, Retrofit and Gson |
| Recipe images | Glide |
| Authentication | Firebase Authentication |
| Cloud favourites | Cloud Firestore |
| Local favourites | Room (SQLite) |
| Background work | WorkManager |
| Push messaging | Firebase Cloud Messaging (FCM) |
| Preferences | Android SharedPreferences |
| Tests | JUnit and AndroidX test dependencies |
| Build | Gradle wrapper, Android Gradle Plugin and Kotlin/KSP plugins |
| CI | GitHub Actions |

The Part 3 implementation uses XML Views. Jetpack Compose is not used for the current UI implementation.

## Project structure

```text
app/src/main/
├── java/com/example/hungrybuddy/
│   ├── HungryBuddyApp.kt
│   ├── data/
│   │   ├── local/          # Room database, DAO, entity and local repository
│   │   ├── notifications/  # Local inactivity reminder and FCM service
│   │   ├── remote/         # TheMealDB API and Firestore favourites repository
│   │   ├── repository/     # Recipe repository
│   │   └── sync/           # WorkManager scheduling and favourite sync logic
│   ├── model/              # Recipe model
│   ├── ui/
│   │   ├── activities/     # Login, registration, home, search, details, favourites, settings
│   │   ├── adapters/       # RecyclerView adapters
│   │   └── navigation/     # Bottom navigation helper
│   ├── utils/              # Validation and settings preferences
│   └── viewmodel/          # Recipe ViewModel
└── res/
    ├── layout/             # XML layouts
    ├── values/             # Default resources (English)
    ├── values-night/       # Night-mode resources
    ├── values-zu/           # isiZulu resources
    └── values-tn/           # Setswana resources
```

## Requirements

The project configuration currently specifies:

- Android `compileSdk`: 37 (Android 17 platform package `platforms;android-37.0`)
- Android `targetSdk`: 36
- Android `minSdk`: 26
- Android Gradle Plugin: 9.3.1
- Gradle wrapper: 9.5.0
- Kotlin plugin: 2.3.20
- CI JDK: Eclipse Temurin 17
- Build Tools: 36.0.0

Use an Android Studio installation with a compatible JDK and Android SDK. The Android 17 platform package may need to be installed from the SDK Manager preview/Canary channel:

```bash
sdkmanager --channel=3 "platforms;android-37.0" "build-tools;36.0.0"
```

On Windows, run Gradle commands from the project root using `gradlew.bat`.

## Getting started

### 1. Clone the Part 3 branch

```bash
git clone --branch part3-settings --single-branch https://github.com/ST10157844/HungryBuddy.git
cd HungryBuddy
```

Open the project folder in Android Studio and allow Gradle to sync.

### 2. Configure Firebase

The repository intentionally excludes `app/google-services.json`. To use your own Firebase project:

1. Create or open a Firebase project.
2. Register an Android application with package name `com.example.hungrybuddy`.
3. Download that project's `google-services.json` and place it at `app/google-services.json`.
4. Enable Email/Password in Firebase Authentication.
5. Create/configure a Cloud Firestore database.
6. Publish Firestore security rules that restrict each signed-in user to their own user document and favourites. Do not use open public read/write rules.
7. Ensure FCM is enabled for the Firebase project if testing remote notifications.

The app's favourites are stored under the signed-in user's Firestore path:

```text
users/{uid}/favourites/{mealId}
```

The Firebase configuration file is required for local builds. Do not commit it or any Firebase service-account private key.

### 3. Build and run

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:installDebug
```

A device or emulator with Android API 26 or later is required. Internet access is needed for TheMealDB requests and Firebase services. Android 13 and later require the user to grant notification permission before the app can display notifications.

## GitHub Actions

The workflow is located at `.github/workflows/android.yml`. It runs on pushes to `main` and `part3-settings`, and on pull requests targeting `main`. It:

1. Checks out the repository.
2. Creates `app/google-services.json` from the `GOOGLE_SERVICES_JSON_BASE64` repository Actions secret.
3. Sets up JDK 17 and the Android SDK.
4. Runs `:app:testDebugUnitTest`.
5. Builds `:app:assembleDebug`.

Configure `GOOGLE_SERVICES_JSON_BASE64` under **Repository Settings → Secrets and variables → Actions**. Its value is the Base64-encoded contents of the project's Firebase configuration file. Base64 is an encoding, not encryption; keep the value only in GitHub's secret store and never paste it into source code or commit it.

The workflow has been verified successfully on `part3-settings` at commit `45cff29` (workflow run #6). Both the unit-test task and debug APK build passed. The workflow currently builds the APK but does not upload it as a downloadable Actions artifact.

## Release build

A local release signing key has been created and a release APK has been built and verified. Signing setup is intentionally local; the keystore and `keystore.properties` must not be committed.

See [`docs/RELEASE.md`](docs/RELEASE.md) for the secure signing setup, build and verification steps.

## Testing

See [`docs/TESTING.md`](docs/TESTING.md) for the test commands, recorded CI result, device checks and items that still need additional evidence.

## Known limitations and outstanding work

The following items are not represented as completed in this version:

- **Blob/object storage:** user-uploaded image or file storage has not been implemented. Recipe images are provided by TheMealDB through image URLs.
- **Automated server-triggered inactivity FCM:** FCM receipt has been manually tested from the Firebase Console, but there is no backend that detects inactivity and automatically sends remote push messages.
- **Federated SSO:** the implemented account flow uses Firebase email/password authentication; Google Sign-In or another federated SSO provider is not documented as implemented.
- **User-generated recipes and profile images:** users cannot publish their own recipes or upload profile photographs in the current scope.
- **Comments/social features:** comments and other social interactions are not implemented.
- **Play Store publication:** a signed APK has been verified locally. A Play Store release has not been published, and a signed release App Bundle has not yet been verified.

The local 24-hour inactivity reminder and remote FCM receipt are separate features and should be demonstrated and assessed separately.

## Credits

Meal data and recipe images are provided by TheMealDB. HungryBuddy uses Firebase, Android Jetpack libraries, Retrofit, Gson, Glide, Room and WorkManager. Each third-party component remains subject to its own licence and terms.
