import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL, FIREBASE_API_KEY, FIREBASE_TEST_EMAIL, FIREBASE_TEST_PASSWORD } from '../k6-config.js';
import { authenticateFirebase } from '../helpers/auth.js';

export { options };

export function setup() {
    // Authenticate once per test run using Firebase Identity Toolkit
    console.log('Authenticating with Firebase...');
    const authData = authenticateFirebase(FIREBASE_API_KEY, FIREBASE_TEST_EMAIL, FIREBASE_TEST_PASSWORD);
    console.log(`Authenticated successfully as user: ${authData.userId}`);
    return authData;
}

export default function (data) {
    if (!data || !data.token) {
        throw new Error('No authentication data received from setup()');
    }

    if (!BASE_URL) {
        throw new Error('API_BASE_URL is not configured');
    }

    if (!/^https?:\/\//.test(BASE_URL)) {
        throw new Error(`Invalid API_BASE_URL: ${BASE_URL}`);
    }

    const authParams = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
    };

    // LOAD-101: Get Profile from Firestore
    // Expected endpoint: https://firestore.googleapis.com/v1/projects/{project}/databases/(default)/documents/users/{userId}
    let profileRes = http.get(`${BASE_URL}/users/${data.userId}`, authParams);
    check(profileRes, {
        'LOAD-101: profile status is 200': (r) => r.status === 200,
    });

    // LOAD-201: Get Marketplace Skills from Firestore
    let skillsRes = http.get(`${BASE_URL}/skills`, authParams);
    check(skillsRes, {
        'LOAD-201: skills status is 200': (r) => r.status === 200,
    });

    sleep(1);
}
