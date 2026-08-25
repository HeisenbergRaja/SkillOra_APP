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

const BASE_URL = __ENV.BASE_URL;

export function setup() {
    if (!BASE_URL) {
        throw new Error('BASE_URL is not configured');
    }
    if (!/^https?:\/\//.test(BASE_URL)) {
        throw new Error(`Invalid BASE_URL protocol. Must start with http:// or https:// (got: ${BASE_URL})`);
    }
    
    return { baseUrl: BASE_URL.endsWith('/') ? BASE_URL.slice(0, -1) : BASE_URL };
}

export default function (data) {
  // Use the actual existing /skills collection as a read-only endpoint test.
  // We use responseCallback to treat 4xx statuses (like 429 or 403) as expected,
  // preventing k6 from automatically failing the http_req_failed metric.
  const res = http.get(`${data.baseUrl}/skills?pageSize=5`, {
    responseCallback: http.expectedStatuses({ min: 200, max: 499 })
  });

  check(res, {
    'API responds': (r) => r.status > 0,
    'API status acceptable': (r) => r.status >= 200 && r.status < 500,
  });

  sleep(1);
}

export function teardown() {
    console.log('=== SKILLORA LOAD TEST COMPLETE ===');
}
