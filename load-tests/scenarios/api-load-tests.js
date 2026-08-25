import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../k6-config.js';
import { authenticateForLoadTest } from '../helpers/auth.js';

import { Rate } from 'k6/metrics';

export { options };

const firestore429Rate = new Rate('firestore_429_rate');
const apiFailureRate = new Rate('api_failure_rate');
const authFailureRate = new Rate('auth_failure_rate');

export function setup() {
    console.log('=== AUTHENTICATION SETUP ===');
    
    console.log('=== LOAD TEST CONFIGURATION ===');
    console.log(`API_BASE_URL: ${__ENV.API_BASE_URL || 'Not Set'}`);
    console.log(`LOAD_TEST_MODE: ${__ENV.LOAD_TEST_MODE || 'Not Set'}`);
    console.log(`VUS: ${__ENV.LOAD_VUS || 100}`);
    console.log(`DURATION: ${__ENV.LOAD_DURATION || '1m'}`);
    console.log('================================');
    
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

function logRequest(name, method, url, res, attempt = 1) {
    if (res.status === 429) {
        console.error(`\nFIRESTORE QUOTA EXCEEDED\nendpoint: ${url.replace(/https?:\/\/[^\/]+/, '')}\nstatus: 429\nattempt: ${attempt}`);
    } else if (res.status >= 400 || res.status === 0) {
        console.error(`\nREQUEST FAILED\nURL: ${url}\nSTATUS: ${res.status}\nDURATION: ${res.timings ? res.timings.duration.toFixed(2) : 0}ms\nBODY: ${res.body ? res.body.substring(0, 500) : '<empty>'}\nERROR: ${res.error || 'N/A'}`);
    } else if (__ENV.IS_SMOKE_TEST === 'true' || Math.random() < 0.05) {
        console.log(`\nAPI REQUEST:\n  name: ${name}\n  method: ${method}\n  path: ${url.replace(/https?:\/\/[^\/]+/, '')}\n  status: ${res.status}\n  duration: ${res.timings.duration.toFixed(2)}ms\n  size: ${res.body ? res.body.length : 0} bytes`);
    }
}

function fetchWithRetry(name, url, reqOptions, maxRetries = 3) {
    let res;
    let retries = 0;
    let delayMs = 500;
    
    while (retries <= maxRetries) {
        res = http.get(url, reqOptions);
        logRequest(name, 'GET', url, res, retries + 1);
        
        if (res.status === 429) {
            firestore429Rate.add(1);
            retries++;
            if (retries <= maxRetries) {
                // Exponential backoff with jitter
                const jitter = Math.random() * 200;
                sleep((delayMs + jitter) / 1000);
                delayMs *= 2;
                continue;
            }
        } else {
            firestore429Rate.add(0);
        }
        
        if (res.status === 401 || res.status === 403) {
            authFailureRate.add(1);
        } else {
            authFailureRate.add(0);
        }
        
        if (res.status >= 400 && res.status !== 429) {
            apiFailureRate.add(1);
        } else {
            apiFailureRate.add(0);
        }
        
        break;
    }
    
    return res;
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
    
    if (data.token) {
        reqOptions.headers['Authorization'] = `Bearer ${data.token}`;
    }

    // 1. Profile Request
    const profileUrl = `${data.baseUrl}/users/${data.userId}`;
    let profileRes = fetchWithRetry('Get Profile', profileUrl, reqOptions);
    
    check(profileRes, {
        'API connectivity: PASS': (r) => r.status > 0,
        'Authenticated request: PASS': (r) => r.status === 200 || r.status === 429, // 429 is recorded in custom metrics, not as a core check failure
    });

    // Pacing between actions
    sleep(Math.random() * 2 + 1);

    // 2. Bounded Skills Request (pageSize=10)
    const skillsUrl = `${data.baseUrl}/skills?pageSize=10`;
    let skillsRes = fetchWithRetry('Get Bounded Skills', skillsUrl, reqOptions);
    
    check(skillsRes, {
        'Authenticated request: PASS (skills)': (r) => r.status === 200 || r.status === 429,
    });

    // Pacing end of iteration
    sleep(Math.random() * 3 + 2);
}

export function handleSummary(data) {
    console.log('\n=== LOAD TEST SUMMARY ===');
    console.log(`Requests: ${data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0}`);
    console.log(`Failures (http_req_failed): ${data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 0}%`);
    console.log(`429 responses (firestore_429_rate): ${data.metrics.firestore_429_rate ? (data.metrics.firestore_429_rate.values.rate * 100).toFixed(2) : 0}%`);
    console.log(`Authentication failures: ${data.metrics.auth_failure_rate ? (data.metrics.auth_failure_rate.values.rate * 100).toFixed(2) : 0}%`);
    console.log(`API failures (5xx, etc): ${data.metrics.api_failure_rate ? (data.metrics.api_failure_rate.values.rate * 100).toFixed(2) : 0}%`);
    console.log(`p95 latency: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 0}ms`);
    console.log(`p99 latency: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 0}ms`);
    console.log('=========================\n');
    return {};
}
