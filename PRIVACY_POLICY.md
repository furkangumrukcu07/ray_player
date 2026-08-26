# Privacy Policy for Ray IPTV Player

**Last Updated:** August 26, 2026

Welcome to **Ray IPTV Player** ("we", "our", or "the App"). We are committed to protecting your privacy and ensuring you have a positive experience while using our application.

This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile and Android TV application **Ray IPTV Player** (Package: `com.ray.iptv.player`).

---

## 1. Information We Collect

### A. Google Account Information (Optional)
When you choose to sign in with Google:
- **Email Address & Display Name:** Used to identify your account and associate your cloud backups.
- **Profile Picture URL:** Used locally and within the app to personalize your profile and settings chip.
- **Google User ID (UID):** A unique identifier used to securely store and retrieve your personal cloud backup on Firebase Firestore.

### B. User-Provided Playlist & Playback Data
Ray IPTV Player is a media player that plays content provided by you. We do not provide, host, or stream any media content. When you use the app:
- **Playlists & Server Credentials:** Xtream Codes login information (server URL, username, password) and M3U playlist URLs are stored locally on your device in an encrypted Room SQLite database and DataStore.
- **Favorites & Watch History:** Your favorite channels, movies, series, and playback progress (continue watching timestamps) are stored locally.
- **Cloud Backup:** If you use the "Cloud Backup" feature, this data is encrypted and saved strictly to your private Google UID path in Google Firebase Firestore.

### C. Analytics and Diagnostic Data
We use **Google Firebase Analytics** and **Firebase Crashlytics** to monitor app health, performance, and crash reports. This data may include:
- Anonymous device model, Android OS version, and app version.
- Crash stack traces and non-personal diagnostic logs.
- General aggregate app usage metrics.

---

## 2. How We Use Your Information

We use the collected information exclusively to:
- Enable seamless cross-device synchronization and cloud backup restoration.
- Provide, maintain, and enhance the app's media playback and EPG features.
- Troubleshoot bugs, investigate crashes, and release performance updates.
- Protect against unauthorized access and ensure security.

**We never sell, rent, or trade your personal data or playlist credentials with any third parties.**

---

## 3. Data Storage, Security, and Retention

- **Local Storage:** All sensitive data (including playlists and passwords) is stored locally on your device.
- **Cloud Security:** Cloud backups are hosted on Google Cloud Firebase Firestore with strict user-level access rules (only the authenticated Google UID can read or write their own data).
- **Data Retention:** Your data is retained as long as you use the application. You can delete your local playlists or cloud backups at any time directly through the app settings.

---

## 4. Permissions Requested

The App requests the following device permissions only when necessary:
- **INTERNET / ACCESS_NETWORK_STATE:** Required to stream playback, fetch EPG data, and communicate with Firebase.
- **FOREGROUND_SERVICE / WAKE_LOCK:** Used for continuous background audio/video playback and Picture-in-Picture (PiP) mode.
- **READ/WRITE STORAGE (Optional):** Used when exporting or importing local backup files or subtitle files chosen by the user.

---

## 5. Third-Party Services

The App integrates trusted third-party services that may collect information used to identify you according to their respective privacy policies:
- [Google Play Services](https://policies.google.com/privacy)
- [Firebase Analytics & Crashlytics](https://firebase.google.com/support/privacy)
- [The Movie Database (TMDb)](https://www.themoviedb.org/privacy-policy) (for fetching public media posters and metadata)
- [OpenSubtitles.com](https://www.opensubtitles.com/privacy-policy) (for fetching community subtitles)

---

## 6. Children's Privacy (COPPA Compliance)

Ray IPTV Player does not knowingly collect personally identifiable information from children under the age of 13. The App includes an optional "Kids Profile" feature with PIN protection that filters adult categories locally based on parent preferences.

---

## 7. Data Deletion and User Rights (GDPR / CCPA)

You have full control over your data:
- **Delete Local Data:** You can delete any playlist, profile, or history item in Settings. Clearing the app storage completely removes all local data.
- **Delete Cloud Backups:** You can sign out or request the deletion of your cloud backup at any time.
- **Contact for Deletion:** To request immediate deletion of your Google account data from our Firebase servers, email us at: **furkangumrukcu07@gmail.com**.

---

## 8. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page with an updated "Last Updated" date.

---

## 9. Contact Us

If you have any questions, concerns, or requests regarding this Privacy Policy, please contact us:

- **Developer:** Furkan Gümrükçü
- **Email:** [furkangumrukcu07@gmail.com](mailto:furkangumrukcu07@gmail.com)
- **Application:** Ray IPTV Player (`com.ray.iptv.player`)
- **Website / Repository:** [https://github.com/furkangumrukcu07/ray_player](https://github.com/furkangumrukcu07/ray_player)
