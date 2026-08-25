import http from 'k6/http';

export function authenticateForLoadTest() {
    // This Android application's backend is Firebase Firestore.
    // The load tested collections (/users, /skills) are configured for public read access.
    // No credentials or Google ID tokens are required for these API load tests.
    // We simply return a valid test user ID so the test can simulate data access.
    
    return {
        token: null, // No token required for public reads
        userId: '63QlIosXcSQCLlCUS8h2hJ0ihXm1' // An existing user ID for load testing
    };
}
