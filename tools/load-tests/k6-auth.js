import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 }, // Ramp-up to 50 users
        { duration: '1m', target: 500 }, // Ramp-up to 500 users
        { duration: '3m', target: 500 }, // Sustain 500 users
        { duration: '30s', target: 0 },  // Ramp-down to 0 users
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must complete below 500ms
        http_req_failed: ['rate<0.01'],   // Error rate must be less than 1%
    },
};

const BASE_URL = 'http://localhost:8080/api';

export default function () {
    // 1. Simulate user login
    const payload = JSON.stringify({
        username: 'test_student@example.com', // Ensure this user exists or use a dynamic user pool
        password: 'password123',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const loginRes = http.post(`${BASE_URL}/auth/login`, payload, params);

    check(loginRes, {
        'login status is 200': (r) => r.status === 200,
        'login has token': (r) => r.json('token') !== undefined,
    });

    sleep(1);
}
