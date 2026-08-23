#!/bin/bash

# Security Scanning Script for Skillora Backend
echo "Running SAST with Semgrep..."
semgrep scan --config auto --json > semgrep-results.json

echo "Running Secret Scanning with Gitleaks..."
gitleaks detect --source . -v --report-path gitleaks-report.json

echo "Running Dependency Scanning with Trivy..."
trivy fs . --format json --output trivy-results.json

echo "Security scans completed."
