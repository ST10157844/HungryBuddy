# HungryBuddy – Project Documentation

**Module:** OPSC6312 – Open Source Code, PoE Part 3\
**Repository:** <https://github.com/ST10157844/HungryBuddy>\
**Implementation branch:** `part3-settings`\
**Application ID:** `com.example.hungrybuddy`

## 1. Project overview

HungryBuddy is an Android recipe-discovery application. It connects to TheMealDB to retrieve meal information and uses Firebase Authentication to identify users. The Part 3 implementation adds locally persisted favourites, per-user cloud synchronisation, settings persistence and notification support.

The application UI is implemented with Kotlin and traditional Android XML Views, Activities, ViewBinding and RecyclerView. Jetpack Compose is not part of the current UI implementation.

## 2. Functional scope

### 2.1 Authentication

- Users can register and sign in with email and password through Firebase Authentication.
- The app relies on Firebase Authentication to manage account credentials and the authenticated session.
- User-specific data operations use the authenticated Firebase UID.
- Logging out stops the scheduled favourite synchronisation and inactivity reminder work associated with the signed-in session.

The current implementation does not document Google Sign-In or another federated SSO provider as implemented. It also does not store a user's raw password in the Room favourites database or SharedPreferences.

### 2.2 Recipe discovery

- The app retrieves meal information from TheMealDB through Retrofit.
- Gson is used for JSON conversion.
- Glide loads recipe images into Android views.
- Users can browse and search meals, open recipe details and follow the available ingredients and cooking instructions.
- Where the recipe data provides a YouTube video link, the details screen can open it.

The recipe catalogue and its photographs are provided by TheMealDB. HungryBuddy does not host the source recipe images.

### 2.3 Favourites and local persistence

The local persistence layer is implemented in `data/local/`:

- `FavoriteRecipeEntity.kt` represents a saved favourite.
- `FavoriteRecipeDao.kt` provides database operations.
- `HungryBuddyDatabase.kt` creates/accesses the Room database.
- `FavoriteRecipeRepository.kt` connects the UI/data layer to local favourite operations.

Favourite records are associated with a Firebase user ID and meal ID. The stored data includes recipe information needed to display saved favourites locally, including a serialised recipe payload and image URL. Synchronisation status distinguishes saved, pending and synchronised changes. Pending deletion state is retained until the remote deletion has been completed.

Room is the local persistence layer. Firestore is the cloud synchronisation store; it is not a replacement for the local database.

### 2.4 Firestore structure and security

The application stores favourites below the authenticated user's document:

```text
users/{uid}/favourites/{mealId}
```

The user ID and meal ID identify the record. Security rules must enforce that a signed-in user can only read or change data belonging to their own UID. Firestore rules should validate the user and record identity; do not deploy unrestricted public read/write rules.

The project was configured with a Standard Cloud Firestore database in `africa-south1`. When configuring a different Firebase project, use the region selected for that project's database and publish matching security rules.

### 2.5 Background favourite synchronisation

The synchronisation components are in `data/sync/`:

- `FavoriteSyncScheduler.kt`
- `FavoriteSyncWorker.kt`
- `FavoriteSyncManager.kt`

The scheduler queues an initial one-time sync and a periodic sync. Both require a connected network. The periodic work is requested at a 15-minute interval, which is subject to Android WorkManager's scheduling guarantees and device conditions. The one-time work uses exponential backoff beginning at 30 seconds.

The worker checks that the Firebase user currently authenticated is the same user ID supplied to the work request. If the account has changed, it skips the operation rather than synchronising another user's data. WorkManager retries a failed sync.

The sync manager performs the following operations:

1. Retrieves pending local changes for the signed-in user.
2. Uploads locally added favourites marked for upload.
3. Deletes remote favourites for local pending deletions, then removes the matching local row after successful remote removal.
4. Retrieves the current remote favourites from Firestore.
5. Inserts remote favourites that are not yet stored locally.
6. Refreshes existing local records only when they are already synchronised, protecting pending local edits.
7. Removes stale local records only when they are synchronised and have no corresponding remote favourite.

The manager records counts for uploaded, remotely deleted, downloaded, updated and locally removed records. Work is performed on the IO dispatcher. Cancellation is rethrown; other exceptions cause WorkManager to retry.

On logout, the scheduler cancels its unique one-time and periodic work.

### 2.6 Settings and language resources

`SettingsPreferences.kt` stores settings in a private SharedPreferences file named `hungrybuddy_settings`.

- `dark_mode` stores the dark-mode preference; the default is `false`.
- `language` stores the selected language tag; the default is `en`.

The resource tree contains default `values/` resources, `values-night/` resources, `values-zu/` for isiZulu and `values-tn/` for Setswana. The selected preference is persisted across Activity/app restarts. The translated resource coverage should be reviewed against all user-facing strings before final assessment; the presence of a locale resource folder alone does not prove every screen is fully translated.

### 2.7 Local inactivity reminder

The inactivity reminder is implemented by:

- `InactivityReminderScheduler.kt`
- `InactivityReminderWorker.kt`
- lifecycle scheduling in `HungryBuddyApp.kt`

When the app moves out of the foreground for a signed-in user, a unique one-time WorkManager request is scheduled with a 24-hour initial delay. Scheduling uses a replace policy so a newer foreground/background cycle replaces the previous pending reminder.

When the worker runs, it:

1. Checks that a valid user ID was supplied.
2. Checks that the same Firebase account is still signed in.
3. Checks the app-foreground preference and avoids notifying while the app is active.
4. Checks Android 13+ `POST_NOTIFICATIONS` permission.
5. Creates the recipe-reminder notification channel and displays a local notification.

This is a **local notification generated on the device**. It does not call a server and does not use FCM to determine inactivity.

### 2.8 Firebase Cloud Messaging

`HungryBuddyMessagingService.kt` extends `FirebaseMessagingService` and is registered for the `com.google.firebase.MESSAGING_EVENT` action in `AndroidManifest.xml`.

- `onNewToken()` receives a refreshed FCM registration token. The current implementation logs it only when the app is debuggable; it does not upload the token to a backend.
- `onMessageReceived()` reads notification title/body values or the corresponding `title`/`body` data keys, applies fallback text when necessary and displays an Android notification.
- Notifications use the `hungry_buddy_fcm_messages` channel and open `HomeActivity` when tapped.
- Android 13+ notification permission is checked before displaying the notification.

A remote FCM test notification was sent manually from the Firebase Console to the Samsung test device and was received successfully. This proves the configured app can receive a manually sent remote FCM message. It does **not** prove automated server-side inactivity detection, token registration with a server or scheduled FCM delivery; those are not implemented in the current scope.

## 3. High-level architecture

```text
XML Activities / RecyclerView
          |
          +---- RecipeViewModel
          |          |
          |          +---- RecipeRepository
          |                      |
          |                      +---- Retrofit / TheMealDB API
          |
          +---- Local favourites repository / Room DAO
                         |
                         +---- Room database
                         |
                         +---- WorkManager sync worker
                                      |
                                      +---- Firestore favourites repository

Firebase Authentication ---- authenticated UID for account-scoped data
SettingsPreferences -------- SharedPreferences
WorkManager ---------------- local inactivity reminder
FirebaseMessagingService --- remote FCM message display
```

### Main packages

| Package | Responsibility |
|---|---|
| `data/local/` | Room entity, DAO, database and local favourite repository |
| `data/remote/` | Retrofit API and Firestore favourite repository |
| `data/repository/` | Recipe repository |
| `data/sync/` | WorkManager scheduling, sync worker and sync manager |
| `data/notifications/` | Local inactivity worker/scheduler and FCM service |
| `ui/activities/` | Login, registration, home, search, recipe details, favourites and settings screens |
| `ui/adapters/` | RecyclerView adapters |
| `ui/navigation/` | Bottom navigation helper |
| `viewmodel/` | Recipe ViewModel |
| `utils/` | Validation and persistent settings preferences |
| `model/` | Recipe model |

## 4. External services and configuration

### TheMealDB

TheMealDB is the source of the meal search and recipe data displayed by the app. The app requires internet connectivity for live API retrieval. Images remain hosted by the external provider and are loaded from their URLs.

### Firebase

The application uses:

- Firebase Authentication for email/password accounts.
- Cloud Firestore for per-user favourites.
- Firebase Cloud Messaging for remote messages.

The app requires a matching `app/google-services.json`. This file is excluded from Git. The GitHub Actions workflow reconstructs it from the repository secret `GOOGLE_SERVICES_JSON_BASE64`.

## 5. Build configuration

| Setting | Value |
|---|---|
| Application ID | `com.example.hungrybuddy` |
| `minSdk` | 26 |
| `targetSdk` | 36 |
| `compileSdk` | 37 |
| Android Gradle Plugin | 9.3.1 |
| Gradle wrapper | 9.5.0 |
| Kotlin plugin | 2.3.20 |
| Java toolchain in CI | Temurin 17 |
| Build Tools in CI | 36.0.0 |

The app module uses ViewBinding and Java source/target compatibility 11. The CI job uses JDK 17 to run Gradle.

## 6. Security and data handling

- Keep `app/google-services.json` out of Git.
- Keep the release keystore and `keystore.properties` outside source control.
- Keep `GOOGLE_SERVICES_JSON_BASE64` in GitHub Actions repository secrets; Base64 is not encryption.
- Restrict Firestore reads/writes to the authenticated user's UID.
- Do not paste FCM registration tokens, keystore passwords, or service-account private keys into issues, source files or documentation.
- Keep the release keystore backed up securely. The same signing identity is needed for compatible future application updates.

## 7. Current limitations and deferred requirements

The following are not implemented or not yet evidenced as completed:

1. Cloud blob/object storage for user-uploaded images or other files.
2. Automated backend inactivity detection and server-triggered FCM notifications.
3. Google Sign-In or another federated SSO provider.
4. User recipe publishing, profile photo uploads and comments.
5. A signed Play Store App Bundle and an actual Play Console publication.

A local inactivity notification exists, and a manually sent remote FCM notification was received. These must not be described as one completed server-driven notification feature.

## 8. Implementation evidence references

- Unit tests: `app/src/test/java/com/example/hungrybuddy/`.
- GitHub Actions workflow: `.github/workflows/android.yml`.
- Room favourites: `data/local/`.
- Firestore repository: `data/remote/FirestoreFavoriteRepository.kt`.
- Favourite sync: `data/sync/`.
- Local reminder and FCM: `data/notifications/`.
- Settings persistence: `utils/SettingsPreferences.kt`.
