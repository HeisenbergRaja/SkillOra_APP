const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const ExcelJS = require('exceljs');

describe('generate-appium-report', () => {
    const jsonReportsDir = path.join(__dirname, '..', 'reports', 'json');
    const mockFile = path.join(jsonReportsDir, 'mock-report-test.json');
    const outputExcel = path.join(__dirname, '..', 'reports', 'appium-test-results.xlsx');

    before(() => {
        if (!fs.existsSync(jsonReportsDir)) {
            fs.mkdirSync(jsonReportsDir, { recursive: true });
        }
        
        // Remove existing reports to ensure clean state
        const files = fs.readdirSync(jsonReportsDir);
        for (const file of files) {
            if (file.endsWith('.json')) {
                fs.unlinkSync(path.join(jsonReportsDir, file));
            }
        }

        const mockData = {
            suites: [
                {
                    title: 'Authentication',
                    suites: [
                        {
                            title: 'Login Screen',
                            tests: [
                                { title: 'should open the login screen', state: 'passed', duration: 1500 },
                                { title: 'should display Google Sign-In button', state: 'failed', error: 'Element not found', duration: 2000 }
                            ]
                        }
                    ],
                    tests: [
                        { title: 'Normal Mocha Test Title', state: 'passed', duration: 1000 },
                        { title: '', fullTitle: 'Fallback Name', state: 'passed', duration: 500 },
                        { state: 'skipped' },
                        { state: 'passed' },
                        { title: 'APP-100: Verify Registration functionality on /login - Test 100', state: 'passed', duration: 1200 }
                    ]
                }
            ]
        };
        fs.writeFileSync(mockFile, JSON.stringify(mockData, null, 2));
    });

    it('should generate an excel report with correct test names including nested descriptions', async () => {
        // Run the script
        execSync('node ' + path.join(__dirname, 'generate-appium-report.js'), { stdio: 'inherit' });

        // Verify the excel output
        const workbook = new ExcelJS.Workbook();
        await workbook.xlsx.readFile(outputExcel);
        const sheet = workbook.getWorksheet('Test Details');
        
        const testNames = [];
        sheet.eachRow((row, rowNumber) => {
            if (rowNumber > 1) { // Skip header
                testNames.push(row.values[3]);
            }
        });

        const expectedNames = [
            'Authentication > Normal Mocha Test Title',
            'Fallback Name',
            'Authentication > Unknown Test',
            'Authentication > Unknown Test',
            'Verify Registration functionality on /login - Test 100',
            'Authentication > Login Screen > should open the login screen',
            'Authentication > Login Screen > should display Google Sign-In button'
        ];

        for (const expected of expectedNames) {
            if (!testNames.includes(expected)) {
                throw new Error(`Expected test name not found in excel: ${expected}. Found: ${testNames.join(', ')}`);
            }
        }
    });

    after(() => {
        // Cleanup mock data
        if (fs.existsSync(mockFile)) {
            fs.unlinkSync(mockFile);
        }
    });
});
