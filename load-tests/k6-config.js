export const options = {
    vus: 100,
    duration: '1m',
    thresholds: {
        http_req_duration: ['p(95)<600', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

export const BASE_URL = __ENV.API_BASE_URL;
export const FIREBASE_API_KEY = __ENV.FIREBASE_API_KEY;
export const FIREBASE_TEST_EMAIL = __ENV.FIREBASE_TEST_EMAIL;
export const FIREBASE_TEST_PASSWORD = __ENV.FIREBASE_TEST_PASSWORD;
