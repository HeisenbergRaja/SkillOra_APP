# SkillOra Load Testing

This directory contains the k6 load tests designed to stress the backend APIs of the SkillOra Android application.

## Authentication Architecture

The SkillOra Android app uses a direct-to-Firebase architecture. There is no custom backend authentication server.

1. **Exact backend base URL:** The Android app connects directly to Firebase services. The base URL for Firestore (which acts as the backend API) is configured dynamically based on the project, generally `https://firestore.googleapis.com/v1/projects/skillora-e2114/databases/(default)/documents`.
2. **Exact authentication method:** The app uses Google Sign-In (via Android `CredentialManager`), exchanges the Google ID Token for a Firebase Credential, and signs into Firebase Authentication.
3. **Firebase project:** `skillora-e2114`
4. **Whether backend expects Firebase ID token:** Yes, all Firestore rules and REST requests require the Firebase ID token in the `Authorization: Bearer <TOKEN>` header.
5. **Exact API endpoint used after Google authentication:** Firestore collections (e.g., `/users/{userId}`, `/skills`).
6. **Required Authorization header:** `Authorization: Bearer <FIREBASE_ID_TOKEN>`
7. **Any required Firebase custom claims:** None observed; standard Firebase authentication rules apply.
8. **Whether a dedicated test account is required:** Yes. Because raw Google ID Tokens expire quickly (1 hour) and cannot easily be automated via the Google login UI, a dedicated Firebase Email/Password test account is used. The CI securely authenticates via the Firebase Identity Toolkit REST API to automatically obtain a fresh Firebase ID token on every run.

## CI Configuration

These load tests are executed via GitHub Actions.

Required GitHub Actions Secrets:
- `API_BASE_URL` (e.g., the Firestore REST API base, defaulting to `https://firestore.googleapis.com/v1/projects/skillora-e2114/databases/(default)/documents` if absent)
- `FIREBASE_TEST_EMAIL` (A dedicated test user email, e.g. `test-user@skillora.app`)
- `FIREBASE_TEST_PASSWORD` (Password for the dedicated test user)

*Note: The Firebase Web API Key is automatically derived from the public `google-services.json` and does not need to be supplied as a GitHub Secret unless overriding.*

These are safely verified during the `=== LOAD TEST PRE-FLIGHT ===` CI step.

## Test Workflow

1. **Pre-flight Check:** Ensures all secrets are present without exposing them.
2. **Health Check:** Pings the `API_BASE_URL`.
3. **Minimal Auth Test:** A 1-VU smoke test verifies the authentication flow.
4. **Full Load Test:** 100 VUs hit the backend using valid Firebase tokens.
