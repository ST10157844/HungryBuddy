# HungryBuddy – Release Build and Signing Guide

This document explains how to reproduce the local signed APK build. It deliberately does not contain the release keystore or any passwords.

## 1. Signing assets

The current local signing setup uses:

| Item | Configuration |
|---|---|
| Keystore filename | `hungrybuddy-release.jks` |
| Local keystore location | Outside the repository, under the user's `.android` directory |
| Key alias | `hungrybuddy` |
| Key algorithm | RSA |
| Key size | 2048 bits |
| Certificate validity | 10,000 days from creation |
| Properties file | `keystore.properties` in the project root (ignored by Git) |

The keystore was generated on 4 November 2026. The certificate was reported valid until 22 March 2054.

**Keep the keystore and passwords safe.** Make an encrypted/offline backup. Losing the signing key or its passwords can prevent future updates from being signed with the same identity. Do not attach the keystore to an email, commit it, upload it to a public repository or paste its passwords into chat.

## 2. Local properties file

The project root contains a local `keystore.properties` file. Its contents follow this structure:

```properties
storeFile=C:/Users/<WindowsUser>/.android/hungrybuddy-release.jks
storePassword=<YOUR_KEYSTORE_PASSWORD>
keyAlias=hungrybuddy
keyPassword=<YOUR_KEY_PASSWORD>
```

Replace the example user folder and placeholders with the values for the local machine. The actual passwords must never be added to this documentation.

The Gradle build script loads this file when it exists and configures the release signing configuration. If the file is missing, the current build script does not assign the local release signing configuration. Do not assume a release APK is signed merely because `assembleRelease` completed; always run the signature verification step below.

## 3. Git protection

The project's `.gitignore` excludes:

```gitignore
keystore.properties
*.jks
*.keystore
*.p12
```

The local `keystore.properties` was checked with `git check-ignore` and confirmed ignored. Before every commit, verify that no signing file is staged:

```powershell
git status --short
git check-ignore -v keystore.properties
```

Never force-add the keystore or properties file.

## 4. Build the signed release APK

From the project root, run:

```powershell
.\gradlew.bat :app:assembleRelease
```

Expected output:

```text
app\build\outputs\apk\release\app-release.apk
```

The local build completed successfully on 4 November 2026. The generated APK was approximately 10.7 MB at the time of verification. File size may vary as dependencies or app content change.

## 5. Verify the APK signature

Use Android Build Tools 36.0.0:

```powershell
$apk = "app\build\outputs\apk\release\app-release.apk"
$apksigner = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat"
& $apksigner verify --verbose --print-certs $apk
```

A successful output includes:

```text
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
```

The recorded local APK passed verification with v2 enabled and one signer whose certificate subject was `CN=HungryBuddy Release`. Other optional signature schemes were reported false; v2 verification succeeded.

## 6. Optional: build an Android App Bundle

For Google Play distribution, Android App Bundles are commonly used. The project has not yet had a signed release App Bundle verified as part of the recorded work. After confirming the release signing configuration and reviewing Play Console requirements, the bundle task can be run with:

```powershell
.\gradlew.bat :app:bundleRelease
```

The expected output is normally under:

```text
app\build\outputs\bundle\release\app-release.aab
```

Verify the resulting bundle and its upload/signing configuration before using it for a Play Console submission. A successful APK build alone does not prove that Play Store publication is complete.

## 7. Versioning and Play Store preparation

The current application configuration reports:

- `versionCode = 1`
- `versionName = "1.0"`
- `applicationId = "com.example.hungrybuddy"`

Increase `versionCode` for each new Play Store release. Review `versionName`, target SDK requirements, app icon, screenshots, privacy policy, data safety declarations, content rating and Play Console testing requirements before publication.

The app currently targets SDK 36. Re-check current Play Console target API requirements at the time of submission rather than relying on this document as a permanent policy reference.

## 8. GitHub Actions and release signing

The current GitHub Actions workflow builds and tests the debug variant. It does not use the private release keystore and does not create a signed release artifact. This is intentional: do not add the JKS file or signing passwords to GitHub.

If release signing is later automated, use GitHub Actions secrets or a dedicated protected signing service and carefully restrict who can access the signing workflow. Do not place signing passwords in workflow YAML or ordinary repository variables.
