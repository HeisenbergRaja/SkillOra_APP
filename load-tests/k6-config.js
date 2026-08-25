const isSmokeTest = __ENV.IS_SMOKE_TEST === 'true';
const targetVus = parseInt(__ENV.LOAD_VUS) || 100;
const targetDuration = __ENV.LOAD_DURATION || '1m';

export const options = {
    vus: isSmokeTest ? 1 : undefined,
    iterations: isSmokeTest ? 1 : undefined,
    stages: isSmokeTest ? undefined : [
        { duration: '10s', target: Math.max(1, Math.floor(targetVus * 0.05)) }, // 0 -> 5%
        { duration: '10s', target: Math.max(1, Math.floor(targetVus * 0.10)) }, // 5% -> 10%
        { duration: '10s', target: Math.max(1, Math.floor(targetVus * 0.25)) }, // 10% -> 25%
        { duration: '10s', target: Math.max(1, Math.floor(targetVus * 0.50)) }, // 25% -> 50%
        { duration: '10s', target: targetVus }, // 50% -> 100%
        { duration: targetDuration, target: targetVus }, // Hold
    ],
    thresholds: isSmokeTest ? {
        http_req_duration: ['p(95)<5000', 'p(99)<10000'],
        http_req_failed: ['rate<0.01'],
        firestore_429_rate: ['rate==0'], // No 429s allowed in smoke test
    } : {
        http_req_duration: ['p(95)<4000', 'p(99)<8000'],
        http_req_failed: ['rate<0.05'], // Max 5% application HTTP failure
        firestore_429_rate: ['rate<0.10'], // Allow max 10% of requests to hit quota during intense load
        api_failure_rate: ['rate<0.01'], // 1% actual application errors
        auth_failure_rate: ['rate==0'],
    },
};

export const BASE_URL = __ENV.API_BASE_URL;
