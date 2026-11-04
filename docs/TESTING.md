# HungryBuddy – Testing and Verification Record

This document records the tests and manual checks that have been reported for the current HungryBuddy Part 3 work. It distinguishes automated build/test results from manual device checks and from features whose complete end-to-end behaviour has not yet been demonstrated.

## 1. Automated unit tests

### Test source files

```text
app/src/test/java/com/example/hungrybuddy/
├── ExampleUnitTest.kt
├── ValidationUtilsTest.kt
└── data/repository/RecipeRepositoryTest.kt
```

Run the local unit-test task from the repository root:

**Windows PowerShell**

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

**Linux/macOS**

```bash
./gradlew :app:testDebugUnitTest
```

The task completed successfully in the local verification run on 4 November 2026 and in GitHub Actions workflow run #6.

## 2. Build verification

### Debug build

```powershell
.\gradlew.bat :app:assembleDebug
```

**Recorded result:** `BUILD SUCCESSFUL` on 4 November 2026.

### Combined debug build and unit tests

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

**Recorded result:** `BUILD SUCCESSFUL` on 4 November 2026.

### Release build

```powershell
.\gradlew.bat :app:assembleRelease
```

**Recorded result:** `BUILD SUCCESSFUL` on 4 November 2026. Gradle printed a non-fatal message that it could not strip `libdatastore_shared_counter.so` and packaged the library as-is.

The resulting APK was found at:

```text
app/build/outputs/apk/release/app-release.apk
```

## 3. Release signature verification

The release APK was checked with Android Build Tools 36.0.0:

```powershell
$apk = "app\build\outputs\apk\release\app-release.apk"
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat"
& $apksigner verify --verbose --print-certs $apk
```

**Recorded result:** `Verifies`. APK Signature Scheme v2 verification returned `true`. One signer was reported, with the certificate subject `CN=HungryBuddy Release`. The v1, v3, v3.1 and v4 schemes were reported as false; v2 verification succeeded, so the APK signature check passed.

## 4. GitHub Actions CI

Workflow file: `.github/workflows/android.yml`

The successful recorded run was:

| Item | Result |
|---|---|
| Workflow | HungryBuddy Android CI |
| Run | #6 |
| Commit | `45cff29` – Fix Android 17 SDK setup in CI |
| Branch | `part3-settings` |
| Runner | `ubuntu-latest` |
| Java | Temurin 17 |
| Gradle | 9.5.0 |
| Unit-test task | `:app:testDebugUnitTest` – passed |
| Debug APK task | `:app:assembleDebug` – passed |
| Duration | 3 minutes 34 seconds |
| Overall result | Success |

Run link: <https://github.com/ST10157844/HungryBuddy/actions/runs/41828700752>

The workflow restores Firebase app configuration from the `GOOGLE_SERVICES_JSON_BASE64` repository secret. It currently builds the debug APK but does not upload the APK as an Actions artifact.

## 5. Manual and device verification reported

| Feature | Reported verification | Evidence status |
|---|---|---|
| XML screen migration | XML screens and navigation were built and exercised; search state was checked while switching tabs | Previously tested during migration |
| Firebase Authentication | Registration/login session and logout flow were exercised | Previously tested |
| Recipe search/navigation | Search query/results were retained while switching between Home, Search and Settings | Previously tested |
| Settings | Language selection and dark-mode preference persistence were exercised | Previously tested |
| Room favourites | Favourite records were visible in the app and stored locally | Previously tested |
| Firestore favourites | A favourite document was observed under the signed-in user's favourites collection | Verified in Firebase Console |
| Background favourite sync | One-time and periodic connected-network work is implemented; WorkManager retries failures | Implementation present; run a controlled offline-to-online sync test for additional evidence |
| Local inactivity reminder | Notification permission prompt was shown and permission was granted; the 24-hour WorkManager reminder is implemented | Permission verified; actual 24-hour elapsed delivery has not been separately confirmed in the recorded evidence |
| Remote FCM | A Firebase Console test message was received on the connected Samsung device | Verified manually |
| Signed release APK | `apksigner` verified the APK using signature scheme v2 | Verified locally |

### Important notification distinction

The WorkManager inactivity reminder is generated locally on the Android device after a 24-hour delay. The FCM test was sent manually through Firebase Console. No automated backend currently detects user inactivity and sends a remote FCM message.

## 6. Instrumentation testing

The earlier Part 2 README recorded one successful instrumentation test on a Pixel 6 Android Virtual Device. That is historical migration evidence. Rerun the instrumentation suite on the current Part 3 branch before presenting it as a fresh Part 3 result.

To run instrumentation tests with a connected emulator/device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 7. Recommended additional test cases before final submission

The following checks would strengthen the evidence package. They are recommendations, not claimed completed tests.

| Test | Suggested procedure | Expected result |
|---|---|---|
| Offline favourite access | Save a favourite, disable network, reopen Favourites and a saved recipe | Saved data remains available from Room |
| Upload after reconnection | Save a favourite offline, restore internet, observe WorkManager and Firestore | Pending favourite appears in the correct user's Firestore collection |
| Remote deletion sync | Remove a favourite, restore network if offline, inspect Firestore | Matching remote favourite is deleted |
| Account isolation | Sign in as user A and save a favourite; sign in as user B | User B does not see user A's favourites |
| Logout cancellation | Log out while sync/reminder work is pending | User-scoped scheduled work is cancelled or safely skipped |
| Local inactivity reminder | Grant notification permission, background the app and leave it inactive for the scheduled delay | Local reminder appears after the 24-hour delay if the account remains signed in and the app is not foregrounded |
| FCM message handling | Send a Firebase Console test notification to a current device token | App displays the remote test notification |
| Notification permission denied | Deny notification permission and trigger a notification path | App does not crash; notification is not displayed |
| Language coverage | Visit each screen in English, isiZulu and Setswana | All intended visible strings are translated and readable |
| Dark mode | Toggle dark mode, navigate between Activities and restart the app | Theme preference remains consistent |
| Release install | Install the signed release build on a test device and exercise login, recipes, favourites and notifications | Release build runs without debug-only dependencies |

## 8. Evidence to capture for submission

Capture genuine screenshots or recordings from the running app and GitHub/Firebase consoles. Useful items include:

- Login and registration screens.
- Home/search screen with recipe results.
- Recipe details with ingredients and instructions.
- Favourites screen with saved recipes.
- Settings screen showing language and theme controls.
- Firestore user/favourites document view (hide user identifiers where appropriate).
- Android notification permission and an actual notification display.
- Firebase Console FCM test and the received notification on the device.
- GitHub Actions run #6 showing both tasks passed.
- Release APK signature verification output.

Do not include passwords, FCM registration tokens, service-account private keys or keystore files in screenshots or the submission repository.
