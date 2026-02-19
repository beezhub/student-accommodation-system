import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 }, // Ramp-up to 20 users
        { duration: '1m', target: 100 }, // Ramp-up to 100 users
        { duration: '2m', target: 100 }, // Sustain 100 users
        { duration: '30s', target: 0 },  // Ramp-down to 0 users
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95% of requests must complete below 2s
        http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
    },
};

const BASE_URL = 'http://localhost:8080/api';

export default function () {
    // 1. Login to get token
    const loginPayload = JSON.stringify({
        username: 'test_student@example.com',
        password: 'password123',
    });

    const loginParams = {
        headers: { 'Content-Type': 'application/json' },
    };

    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, loginParams);
    const token = loginRes.json('token');

    check(loginRes, {
        'login successful': (r) => r.status === 200,
    });

    if (!token) {
        console.error('Login failed');
        return;
    }

    const authHeaders = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // 2. Fetch reference data (simulating frontend load)
    // Assuming endpoints for institutions/year-of-study exist and are public or protected
    // http.get(`${BASE_URL}/institutions`, authHeaders); 
    // http.get(`${BASE_URL}/year-of-study`, authHeaders);

    sleep(2);

    // 3. Submit application
    const applicationPayload = JSON.stringify({
        institutionId: 1,
        yearOfStudyId: 1,
        accommodationType: 'SINGLE_ROOM',
        specialNeeds: 'None',
    });

    // Note: This endpoint might fail if application already exists for user.
    // Ideally, use unique users for each iteration or handle 409 Conflict.
    const appRes = http.post(`${BASE_URL}/applications`, applicationPayload, authHeaders);

    check(appRes, {
        'application submitted or conflict': (r) => r.status === 201 || r.status === 409,
    });

    sleep(1);
}
