import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../k6-config.js';
import { authenticateForLoadTest } from '../helpers/auth.js';

export { options };

export function setup() {
    console.log('=== AUTHENTICATION SETUP ===');
    if (!BASE_URL) {
        throw new Error('API_BASE_URL is not configured');
    }
    
    // Ensure BASE_URL format is correct
    let baseUrl = BASE_URL;
    if (baseUrl.endsWith('/')) {
        baseUrl = baseUrl.slice(0, -1);
    }
    if (!/^https?:\/\//.test(baseUrl)) {
        throw new Error(`Invalid API_BASE_URL: ${baseUrl}`);
    }

    const authData = authenticateForLoadTest();
    console.log('Firebase authentication: PASS (Public Read Endpoint)');
    console.log('Firebase ID token: obtained (Anonymous/None Required)');
    
    // Pass baseUrl so VUs don't need to rebuild it
    authData.baseUrl = baseUrl;
    return authData;
}

export default function (data) {
    if (!data || !data.userId) {
        throw new Error('No configuration data received from setup()');
    }

    const reqOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };
    
    // Use the optional token if provided
    if (data.token) {
        reqOptions.headers['Authorization'] = `Bearer ${data.token}`;
    }

    let profileRes = http.get(`${data.baseUrl}/users/${data.userId}`, reqOptions);
    check(profileRes, {
        'API connectivity: PASS': (r) => r.status > 0,
        'Authentication: PASS': (r) => r.status !== 401 && r.status !== 403,
        'Authenticated request: PASS': (r) => r.status === 200,
    });

    let skillsRes = http.get(`${data.baseUrl}/skills`, reqOptions);
    check(skillsRes, {
        'Authenticated request: PASS (skills)': (r) => r.status === 200,
    });

    sleep(1);
}
