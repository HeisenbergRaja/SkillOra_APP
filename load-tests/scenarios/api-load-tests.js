import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL, FIREBASE_TEST_TOKEN } from '../k6-config.js';
import { authenticateFirebase } from '../helpers/auth.js';

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
    if (!FIREBASE_TEST_TOKEN) {
        throw new Error('FIREBASE_TEST_TOKEN is not configured');
    }
    if (!BASE_URL) {
        throw new Error('API_BASE_URL is not configured');
    }
    if (!/^https?:\/\//.test(BASE_URL)) {
        throw new Error(`Invalid API_BASE_URL: ${BASE_URL}`);
    }

    const authData = authenticateFirebase(FIREBASE_TEST_TOKEN);
    console.log(`Test token: obtained (User: ${authData.userId})`);
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

    // LOAD-101: Get Profile from Firestore
    let profileRes = http.get(`${BASE_URL}/users/${data.userId}`, authParams);
    check(profileRes, {
        'HTTP request: PASS (profile)': (r) => r.status === 200,
        'Authentication: PASS (profile)': (r) => r.status !== 401 && r.status !== 403,
    });

    // LOAD-201: Get Marketplace Skills from Firestore
    let skillsRes = http.get(`${BASE_URL}/skills`, authParams);
    check(skillsRes, {
        'HTTP request: PASS (skills)': (r) => r.status === 200,
        'Authentication: PASS (skills)': (r) => r.status !== 401 && r.status !== 403,
    });

    sleep(1);
}
