# Flow — Complete phone-first Android starter

## Honest status
This project contains a more complete local-first Android app and a GitHub Actions workflow to build a debug APK in the cloud from your phone. **No APK has been compiled or device-tested in this environment.**

Implemented in source:
- Multiple period records (start/end date, flow, symptoms, notes)
- Sex/conception log (date, ejaculation/contraception note, optional notes)
- Cycle history and estimated next period / fertile window when a cycle length is entered
- Local persistent storage on the phone
- Export/import JSON backup
- GitHub Actions APK build workflow

Not implemented:
- Secure shared couple-code pairing
- Cloud synchronization between two phones
- Push notifications
- App PIN/biometric lock
- End-to-end encryption

Cloud sync requires backend configuration and security rules. A shared code by itself is not secure enough for reproductive-health records. Do not upload real sensitive data until sync and permissions have been implemented and tested.

## Build APK from your Android phone
1. Extract this ZIP on your phone.
2. Create a **private** repository on https://github.com/
3. Upload all extracted project files to the repository. Make sure `.github/workflows/build-apk.yml` is included. If the mobile website hides dot-folders, create `.github/workflows` in GitHub's web editor or use GitHub's desktop-style site.
4. In the repository, open **Actions** and enable workflows if prompted.
5. Select **Build Flow APK** and tap **Run workflow**.
6. Wait for a green successful run.
7. Open that run, scroll to **Artifacts**, and download `Flow-debug-APK`.
8. Extract the artifact ZIP and install `app-debug.apk`. Android may ask you to allow installs from Chrome or your file manager.
9. Test with sample data first.

If a build fails, open the failed run and copy the first relevant error into ChatGPT so it can be corrected.

## Privacy
- Records are stored locally in the app's private SharedPreferences on this phone.
- Android backup is disabled in the manifest.
- Uninstalling the app or clearing its data may erase records. Export a backup periodically.
- Export files contain sensitive data; keep them private.
- This build is not a medical device. Calendar predictions are estimates, not proof of ovulation or contraception.
- Get your partner's consent before recording information about her.

## Add secure cloud sync later
Recommended implementation: Firebase Authentication + Cloud Firestore + Cloud Functions. The cloud function must issue random, expiring, single-use invitation codes, rate-limit guesses, verify membership, and support revocation. Firestore rules must deny all unpaired access and only allow workspace members. Do not deploy permissive rules.
