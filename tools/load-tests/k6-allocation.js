import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 5 },  // Ramp-up to 5 admins
        { duration: '30s', target: 10 }, // Ramp-up to 10 admins
        { duration: '1m', target: 10 },  // Sustain 10 admins
        { duration: '10s', target: 0 },  // Ramp-down to 0 admins
    ],
    thresholds: {
        http_req_duration: ['p(95)<5000'], // Allocation might take longer, 5s threshold
        http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
    },
};

const BASE_URL = 'http://localhost:8080/api';

export default function () {
    // 1. Admin Login
    const loginPayload = JSON.stringify({
        username: 'admin@example.com',
        password: 'adminpassword',
    });

    const loginParams = {
        headers: { 'Content-Type': 'application/json' },
    };

    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, loginParams);
    const token = loginRes.json('token');

    check(loginRes, {
        'admin login successful': (r) => r.status === 200,
    });

    if (!token) {
        console.error('Admin login failed');
        return;
    }

    const authHeaders = {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };

    // 2. Trigger Batch Allocation (simulated)
    // This assumes an endpoint exists for triggering allocation manually
    // Adjust endpoint as per actual implementation
    const allocationRes = http.post(`${BASE_URL}/allocations/batch`, {}, authHeaders);

    check(allocationRes, {
        'allocation triggered': (r) => r.status === 200 || r.status === 202,
    });

    sleep(5); // Wait between triggers
}
