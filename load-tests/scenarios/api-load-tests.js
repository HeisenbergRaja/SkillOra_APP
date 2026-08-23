import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL, TEST_EMAIL, TEST_PASSWORD } from '../k6-config.js';

export { options };

export default function () {
    // LOAD-001: Login
    const loginPayload = JSON.stringify({
        email: TEST_EMAIL,
        password: TEST_PASSWORD,
    });
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    let loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, params);
    
    check(loginRes, {
        'LOAD-001: login status is 200': (r) => r.status === 200,
        'LOAD-001: login returns token': (r) => r.json('token') !== undefined,
    });

    let token = loginRes.json('token');
    if (token) {
        const authParams = {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
        };

        // LOAD-101: Get Profile
        let profileRes = http.get(`${BASE_URL}/users/profile`, authParams);
        check(profileRes, {
            'LOAD-101: profile status is 200': (r) => r.status === 200,
        });

        // LOAD-201: Get Marketplace Skills
        let skillsRes = http.get(`${BASE_URL}/skills?page=1&limit=20`, authParams);
        check(skillsRes, {
            'LOAD-201: skills status is 200': (r) => r.status === 200,
        });
    }

    sleep(1);
}
