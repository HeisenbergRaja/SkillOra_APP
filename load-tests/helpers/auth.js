import http from 'k6/http';

export function authenticateForLoadTest(apiKey, email, password) {
    if (!email || !password) {
        throw new Error('Firebase test credentials (FIREBASE_TEST_EMAIL, FIREBASE_TEST_PASSWORD) are not configured. A dedicated test user is required.');
    }

    const authUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey || 'AIzaSyBMcAmunSsEIIeo-sWCUPLNJVTwoueCusg'}`;
    const payload = JSON.stringify({
        email: email,
        password: password,
        returnSecureToken: true,
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const response = http.post(authUrl, payload, params);

    if (!response) {
        throw new Error('Authentication returned no response');
    }

    if (response.status < 200 || response.status >= 300) {
        console.error(
            `=== FIREBASE AUTHENTICATION FAILED ===\nHTTP status: ${response.status}\nResponse: ${response.body ? response.body.substring(0, 500) : '<empty>'}`
        );
        throw new Error(`Authentication failed with HTTP ${response.status}`);
    }

    if (!response.body) {
        throw new Error('Authentication returned an empty response body');
    }

    let data;
    try {
        data = response.json();
    } catch (error) {
        throw new Error(`Authentication returned invalid JSON: ${error}`);
    }

    if (!data || !data.idToken) {
        throw new Error('Authentication response did not contain an idToken');
    }

    return {
        token: data.idToken,
        userId: data.localId
    };
}
