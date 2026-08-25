# SkillOra Load Testing

# SkillOra Load Tests

This directory contains a simple, lightweight k6 load test designed to verify that the SkillOra API backend is reachable and healthy under basic concurrent load.

## Purpose

- Smoke testing API health (e.g. `/health` endpoint)
- Validating infrastructure and network connectivity in CI
- Quick load sanity check (5 VUs for 30s)

## Running Locally

To run the load test locally:

```bash
export BASE_URL="https://api.skillora.example.com"
npm run test:load
```

## CI Configuration

These load tests are executed via GitHub Actions.

Required GitHub Actions Secrets:
- `BASE_URL` (The absolute base URL of the deployed API you wish to test)

The workflow includes a preflight check to ensure `BASE_URL` is configured before running.

## Test Configuration

The test executes using a lightweight, relaxed configuration suitable for CI environments without causing excessive quota limits or latency failures:

- **VUs**: 5
- **Duration**: 30s
- **Thresholds**: 
  - `http_req_failed`: `< 10%`
  - `http_req_duration`: `p(95) < 5000ms`

*Note: This test no longer depends on Firebase Authentication or direct Firestore queries. It tests generic API endpoints to prevent hitting direct database quotas.*
