import http from 'k6/http';

export function authenticateFirebase(apiKey, email, password) {
    if (!apiKey) throw new Error('FIREBASE_API_KEY is not configured');
    if (!email || !password) throw new Error('Firebase test credentials are not configured');

    const authUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`;
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
        const safeUrl = authUrl.split('?key=')[0];
        console.error(
            `GOOGLE/FIREBASE AUTH FAILED: endpoint=${safeUrl}, status=${response.status}, body=${response.body || '<empty>'}`
        );
        throw new Error(`Authentication failed with HTTP ${response.status} at ${safeUrl}`);
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

    if (!data.idToken) {
        throw new Error('Authentication response did not contain an idToken');
    }

    return {
        token: data.idToken,
        userId: data.localId
    };
}
