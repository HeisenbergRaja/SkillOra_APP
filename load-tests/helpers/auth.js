import http from 'k6/http';

export function authenticateFirebase(testToken) {
    if (!testToken) {
        throw new Error('FIREBASE_TEST_TOKEN is not configured. A valid test credential is required to authenticate with Firebase.');
    }

    // In a real load test environment, the CI pipeline should supply a valid Firebase ID Token
    // or an OAuth2 access token (e.g., via a Service Account) as FIREBASE_TEST_TOKEN.
    // This avoids needing FIREBASE_API_KEY, emails, passwords, or interactive Google logins.
    
    // We assume the test token contains the user context needed for Firestore.
    // We can extract a dummy userId from the token if it's a JWT, but for load testing
    // Firestore REST APIs, the token itself is what matters for the Authorization header.

    return {
        token: testToken,
        // Using a hardcoded test userId since decoding JWT in k6 requires external libs.
        // In a real scenario, this would be the UID associated with the test token.
        userId: 'load-test-user-123'
    };
}
