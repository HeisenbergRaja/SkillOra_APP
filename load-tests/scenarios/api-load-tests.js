import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL, FIREBASE_API_KEY, FIREBASE_TEST_EMAIL, FIREBASE_TEST_PASSWORD } from '../k6-config.js';
import { authenticateForLoadTest } from '../helpers/auth.js';

export { options };

function safeJson(response) {
    if (!response || !response.body) {
        return null;
    }
    try {
        return response.json();
    } catch (error) {
        return null;
    }
}

export function setup() {
    console.log('=== AUTHENTICATION SETUP ===');
    if (!BASE_URL) {
        throw new Error('API_BASE_URL is not configured');
    }
    if (!/^https?:\/\//.test(BASE_URL)) {
        throw new Error(`Invalid API_BASE_URL: ${BASE_URL}`);
    }

    const authData = authenticateForLoadTest(FIREBASE_API_KEY, FIREBASE_TEST_EMAIL, FIREBASE_TEST_PASSWORD);
    console.log('Firebase authentication: PASS');
    console.log('Firebase ID token: obtained');
    return authData;
}

export default function (data) {
    if (!data || !data.token) {
        throw new Error('No authentication data received from setup()');
    }

    const authParams = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
    };

    let profileRes = http.get(`${BASE_URL}/users/${data.userId}`, authParams);
    check(profileRes, {
        'API connectivity: PASS': (r) => r.status > 0,
        'Authentication: PASS': (r) => r.status !== 401 && r.status !== 403,
        'Authenticated request: PASS': (r) => r.status === 200,
    });

    let skillsRes = http.get(`${BASE_URL}/skills`, authParams);
    check(skillsRes, {
        'Authenticated request: PASS (skills)': (r) => r.status === 200,
    });

    sleep(1);
}
