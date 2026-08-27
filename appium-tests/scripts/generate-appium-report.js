const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

const jsonReportsDir = path.join(__dirname, '..', 'reports', 'json');
const outputExcel = path.join(__dirname, '..', 'reports', 'appium-test-results.xlsx');
const outputMd = path.join(__dirname, '..', 'reports', 'appium-test-report.md');

async function generateReport() {
    console.log('Generating Appium Excel Report...');
    const files = fs.existsSync(jsonReportsDir) ? fs.readdirSync(jsonReportsDir).filter(f => f.endsWith('.json')) : [];
    
    let totalTests = 0;
    let passed = 0;
    let failed = 0;
    let skipped = 0;
    let totalDuration = 0;
    
    const allTests = [];
    const categories = {};
    const failures = [];

    // Parse JSON
    files.forEach(file => {
        try {
            const data = JSON.parse(fs.readFileSync(path.join(jsonReportsDir, file), 'utf8'));
            if (!data || !data.suites) return;
            
            // Recursive function to process suites and their tests
            const processSuite = (suite, parentTitles = []) => {
                const currentSuiteTitle = String(suite.title || '').trim();
                
                // Determine the category from the root suite
                let category = 'General';
                if (parentTitles.length === 0 && currentSuiteTitle) {
                    category = currentSuiteTitle.split(' ')[0] || 'General';
                } else if (parentTitles.length > 0 && parentTitles[0]) {
                    category = parentTitles[0].split(' ')[0] || 'General';
                }
                
                if (!categories[category]) categories[category] = { total: 0, passed: 0, failed: 0, skipped: 0 };
                
                const currentTitles = currentSuiteTitle ? [...parentTitles, currentSuiteTitle] : [...parentTitles];
                
                if (suite.tests && suite.tests.length > 0) {
                    suite.tests.forEach(test => {
                        totalTests++;
                        totalDuration += test.duration || 0;
                        
                        const state = String(test.state || (test.passed ? 'passed' : 'failed'));
                        let statusStr = state.toUpperCase();
                        
                        // Extract test name
                        let testTitle = test.title || test.name || test.testName || '';
                        
                        if (!testTitle && (test.fullTitle || test.fullName)) {
                            testTitle = test.fullTitle || test.fullName;
                        }

                        let finalTestName = '';
                        // Strip APP-ID prefix from title if it exists
                        const appPrefixMatch = testTitle.match(/^APP-\d+:\s*(.*)/);
                        if (appPrefixMatch) {
                            testTitle = appPrefixMatch[1];
                        }
                        
                        // Extract feature for category from standard format: Verify <Feature> functionality...
                        const featureMatch = testTitle.match(/^Verify\s+(.*?)\s+(?:functionality|handles)/);
                        let testCategory = category; // fallback to suite-level category
                        if (featureMatch) {
                            testCategory = featureMatch[1].trim();
                        }
                        if (!categories[testCategory]) categories[testCategory] = { total: 0, passed: 0, failed: 0, skipped: 0 };
                        
                        categories[testCategory].total++;
                        
                        if (testTitle && (test.fullTitle || test.fullName) === testTitle) {
                             finalTestName = testTitle;
                        } else if (testTitle && testTitle === (test.fullTitle || test.fullName)) {
                             finalTestName = testTitle;
                        } else if (testTitle && testTitle.startsWith('Verify ')) {
                             // If it's already a descriptive standalone name, don't prepend suite
                             finalTestName = testTitle;
                        } else {
                            let fullNameParts = [...currentTitles];
                            if (testTitle) fullNameParts.push(testTitle);
                            finalTestName = fullNameParts.join(' > ').trim();
                        }
                        
                        // Try fallback fields if empty or if it just matches the suite name
                        if (!finalTestName || finalTestName === currentTitles.join(' > ').trim()) {
                            finalTestName = test.fullTitle || test.fullName || (currentTitles.length > 0 ? currentTitles.join(' > ') + ' > Unknown Test' : 'Unknown Test');
                            
                            // Strip prefix again for fallbacks
                            const fallbackPrefixMatch = finalTestName.match(/^APP-\d+:\s*(.*)/);
                            if (fallbackPrefixMatch) {
                                finalTestName = fallbackPrefixMatch[1];
                            }
                        }
                        
                        let isNegative = false;
                        let endpoint = '/';
                        
                        // Try to parse route from name if possible (Verify ... on /route)
                        const routeMatch = finalTestName.match(/on\s+(\/\S+)/);
                        if (routeMatch) {
                            endpoint = routeMatch[1];
                        }
                        
                        if (finalTestName.includes('invalid input')) {
                            isNegative = true;
                        }
                        
                        const expectedResult = isNegative ? 'Application rejects input and displays appropriate error' : 'Action succeeds and UI reflects state';
                        let actualResult = '';
                        
                        if (state === 'passed') {
                            passed++;
                            categories[testCategory].passed++;
                            statusStr = 'PASS';
                            actualResult = expectedResult;
                        } else if (state === 'failed') {
                            failed++;
                            categories[testCategory].failed++;
                            statusStr = 'FAIL';
                            actualResult = String(test.error || 'Assertion Failed');
                            failures.push({
                                id: `APP-${String(totalTests).padStart(3, '0')}`,
                                name: finalTestName,
                                error: actualResult
                            });
                        } else {
                            skipped++;
                            categories[testCategory].skipped++;
                            statusStr = 'FAIL'; // Mapping skipped to FAIL or just 'BLOCKED', but Selenium only uses PASS or FAIL
                            actualResult = 'Test skipped/blocked';
                        }
                        
                        const stepsText = `1. Launch the SkillOra application\n2. Navigate to the ${endpoint} screen\n3. Perform ${testCategory} action\n4. Verify the displayed result`;
                        
                        allTests.push({
                            id: `APP-${String(totalTests).padStart(3, '0')}`,
                            module: testCategory,
                            name: finalTestName,
                            precondition: 'Application is running',
                            steps: stepsText,
                            expected: expectedResult,
                            actual: actualResult,
                            status: statusStr,
                            duration: test.duration || 0,
                            screenshot: state === 'failed' ? 'error-screenshot.png' : ''
                        });
                    });
                }
                
                // Process nested suites
                if (suite.suites && suite.suites.length > 0) {
                    suite.suites.forEach(childSuite => processSuite(childSuite, currentTitles));
                }
            };

            data.suites.forEach(suite => processSuite(suite, []));
            
        } catch (e) {
            console.error(`Error parsing ${file}:`, e);
        }
    });

    const executed = passed + failed;
    const blocked = skipped;
    const notExecuted = totalTests === 0 ? 'ALL' : 0;
    
    let status = 'EXECUTED';
    let passRate = '0%';
    if (totalTests === 0) {
        console.error('WARNING: No test results found. Logging Infrastructure Failure.');
        allTests.push({
            id: 'INFRA-001',
            category: 'Infrastructure',
            name: 'Appium/WDIO Initialization',
            status: 'FAILED',
            duration: 0,
            error: 'No tests were executed. Possible infrastructure failure.'
        });
        totalTests = 1;
        failed = 1;
        status = 'INFRASTRUCTURE FAILURE';
    } else if (executed > 0) {
        passRate = `${Math.round((passed / executed) * 100)}%`;
    }

    let minTestWarning = '';

    // 1. Write Markdown Summary
    const md = `
# SkillOra Android Appium E2E Test Summary

| Metric | Result |
|---|---:|
| Total Tests | ${totalTests} |
| Executed | ${executed} |
| Passed | ${passed} |
| Failed | ${failed} |
| Blocked | ${blocked} |
| Not Executed | ${notExecuted} |
| Pass Rate | ${passRate} |
| Device | Android Emulator |
| Execution Time | ${Math.round(totalDuration / 1000)}s |${minTestWarning}

## Category Summary

| Category | Total | Passed | Failed | Blocked |
|---|---:|---:|---:|---:|
${Object.keys(categories).map(cat => `| ${cat} | ${categories[cat].total} | ${categories[cat].passed} | ${categories[cat].failed} | ${categories[cat].skipped} |`).join('\n')}
`;
    if (!fs.existsSync(path.dirname(outputMd))) fs.mkdirSync(path.dirname(outputMd), { recursive: true });
    fs.writeFileSync(outputMd, md.trim());

    // 2. Write Excel Workbook using template
    const templatePath = 'D:\\PDD\\Skillora_Web\\selenium-test-results.xlsx';
    const workbook = new ExcelJS.Workbook();
    
    if (fs.existsSync(templatePath)) {
        await workbook.xlsx.readFile(templatePath);
        const sheetDetails = workbook.getWorksheet('Selenium Test Results');
        
        // Remove existing rows (except header)
        sheetDetails.spliceRows(2, sheetDetails.rowCount - 1);
        
        // Add new rows using the same column ordering
        allTests.forEach(test => {
            const row = sheetDetails.addRow([
                test.id,
                test.module,
                test.name,
                test.precondition,
                test.steps,
                test.expected,
                test.actual,
                test.status,
                test.duration,
                test.screenshot
            ]);
            // Copy styling from a template row (e.g., row 2 from original if we kept it, but we spliced it)
            // To ensure formatting is kept, ExcelJS usually applies column style to new rows.
            // If cell wrapping is needed for steps:
            row.getCell(5).alignment = { wrapText: true, vertical: 'top' };
            row.getCell(3).alignment = { wrapText: true, vertical: 'top' };
            row.getCell(6).alignment = { wrapText: true, vertical: 'top' };
            row.getCell(7).alignment = { wrapText: true, vertical: 'top' };
            
            // Apply font/alignment to match generally
            row.eachCell({ includeEmpty: true }, (cell, colNumber) => {
                cell.font = { name: 'Arial', size: 10 };
                if (colNumber !== 5 && colNumber !== 3 && colNumber !== 6 && colNumber !== 7) {
                    cell.alignment = { vertical: 'top', horizontal: 'left' };
                }
                
                // Status formatting
                if (colNumber === 8) {
                    cell.font = { name: 'Arial', size: 10, bold: true, color: { argb: test.status === 'PASS' ? 'FF00B050' : 'FFFF0000' } };
                    cell.alignment = { vertical: 'top', horizontal: 'center' };
                }
            });
        });
    } else {
        console.error('Template file not found at ' + templatePath + '. Creating a new workbook.');
        const sheetDetails = workbook.addWorksheet('Selenium Test Results');
        sheetDetails.columns = [
            { header: 'Test ID', key: 'id', width: 10 },
            { header: 'Module', key: 'module', width: 20 },
            { header: 'Test Name', key: 'name', width: 40 },
            { header: 'Preconditions', key: 'precondition', width: 30 },
            { header: 'Steps', key: 'steps', width: 40 },
            { header: 'Expected Result', key: 'expected', width: 30 },
            { header: 'Actual Result', key: 'actual', width: 30 },
            { header: 'Status', key: 'status', width: 10 },
            { header: 'Duration (ms)', key: 'duration', width: 15 },
            { header: 'Screenshot', key: 'screenshot', width: 30 }
        ];
        sheetDetails.addRows(allTests);
    }

    await workbook.xlsx.writeFile(outputExcel);
    console.log(`Report generated: ${outputExcel}`);
}

generateReport().catch(console.error);
