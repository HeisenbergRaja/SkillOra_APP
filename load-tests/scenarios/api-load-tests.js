import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<5000'],
  },
};

export function setup() {
    console.log('=== SKILLORA LOAD TEST ===');
    const baseUrl = __ENV.BASE_URL;
    
    if (!baseUrl) {
        throw new Error('BASE_URL is not configured');
    }
    if (!/^https?:\/\//.test(baseUrl)) {
        throw new Error(`Invalid BASE_URL protocol. Must start with http:// or https:// (got: ${baseUrl})`);
    }
    
    console.log('BASE_URL configured: YES');
    console.log('Starting k6...');
    return { baseUrl: baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl };
}

export default function (data) {
  const res = http.get(`${data.baseUrl}/health`);

  check(res, {
    'API responds': (r) => r.status > 0,
    'API status acceptable': (r) => r.status >= 200 && r.status < 500,
  });

  sleep(1);
}

export function teardown() {
    console.log('=== SKILLORA LOAD TEST COMPLETE ===');
}
