const isSmokeTest = __ENV.IS_SMOKE_TEST === 'true';

export const options = {
    vus: isSmokeTest ? 1 : 100,
    duration: isSmokeTest ? undefined : '1m',
    iterations: isSmokeTest ? 1 : undefined,
    thresholds: isSmokeTest ? {
        http_req_failed: ['rate<0.01'],
    } : {
        http_req_duration: ['p(95)<1200', 'p(99)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

export const BASE_URL = __ENV.API_BASE_URL;
