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
                        categories[category].total++;
                        
                        const state = String(test.state || (test.passed ? 'passed' : 'failed'));
                        
                        let statusStr = state.toUpperCase();
                        
                        // Extract test name
                        let testTitle = test.title || test.name || test.testName || '';
                        
                        if (!testTitle && (test.fullTitle || test.fullName)) {
                            // If title is missing but fullTitle exists, just use fullTitle entirely
                            // without prepending suite names to avoid duplication
                            testTitle = test.fullTitle || test.fullName;
                        }

                        let finalTestName = '';
                        if (testTitle && (test.fullTitle || test.fullName) === testTitle) {
                             // Sometimes fullTitle is the same as title and includes suite
                             finalTestName = testTitle;
                        } else if (testTitle && testTitle === (test.fullTitle || test.fullName)) {
                             finalTestName = testTitle;
                        } else {
                            let fullNameParts = [...currentTitles];
                            if (testTitle) fullNameParts.push(testTitle);
                            finalTestName = fullNameParts.join(' > ').trim();
                        }
                        
                        // Try fallback fields if empty or if it just matches the suite name
                        if (!finalTestName || finalTestName === currentTitles.join(' > ').trim()) {
                            finalTestName = test.fullTitle || test.fullName || (currentTitles.length > 0 ? currentTitles.join(' > ') + ' > Unknown Test' : 'Unknown Test');
                        }
                        
                        if (state === 'passed') {
                            passed++;
                            categories[category].passed++;
                            statusStr = 'EXECUTED/PASSED';
                        } else if (state === 'failed') {
                            failed++;
                            categories[category].failed++;
                            statusStr = 'EXECUTED/FAILED';
                            failures.push({
                                id: `APP-${String(totalTests).padStart(3, '0')}`,
                                name: finalTestName,
                                error: String(test.error || 'Assertion Failed')
                            });
                        } else {
                            skipped++;
                            categories[category].skipped++;
                            statusStr = 'BLOCKED';
                        }
                        
                        allTests.push({
                            id: `APP-${String(totalTests).padStart(3, '0')}`,
                            category,
                            name: finalTestName,
                            status: statusStr,
                            duration: test.duration || 0,
                            error: state === 'failed' ? String(test.error || 'Failed') : ''
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

    // 2. Write Excel Workbook
    const workbook = new ExcelJS.Workbook();
    
    // Sheet 1: Summary
    const sheetSummary = workbook.addWorksheet('Summary');
    sheetSummary.columns = [ { header: 'Metric', key: 'metric', width: 25 }, { header: 'Value', key: 'value', width: 25 } ];
    sheetSummary.addRows([
        { metric: 'Total Test Cases', value: totalTests },
        { metric: 'Executed', value: executed },
        { metric: 'Passed', value: passed },
        { metric: 'Failed', value: failed },
        { metric: 'Blocked', value: blocked },
        { metric: 'Not Executed', value: notExecuted },
        { metric: 'Pass Rate', value: passRate },
        { metric: 'Execution Time (s)', value: Math.round(totalDuration / 1000) },
        { metric: 'Device', value: 'Android Emulator' },
        { metric: 'Status', value: status }
    ]);

    // Sheet 2: Test Details
    const sheetDetails = workbook.addWorksheet('Test Details');
    sheetDetails.columns = [
        { header: 'Test ID', key: 'id', width: 15 },
        { header: 'Category', key: 'category', width: 20 },
        { header: 'Test Name', key: 'name', width: 50 },
        { header: 'Status', key: 'status', width: 15 },
        { header: 'Duration (ms)', key: 'duration', width: 15 },
        { header: 'Error', key: 'error', width: 50 }
    ];
    sheetDetails.addRows(allTests);

    // Sheet 3: Category Summary
    const sheetCats = workbook.addWorksheet('Category Summary');
    sheetCats.columns = [
        { header: 'Category', key: 'category', width: 20 },
        { header: 'Total', key: 'total', width: 10 },
        { header: 'Passed', key: 'passed', width: 10 },
        { header: 'Failed', key: 'failed', width: 10 },
        { header: 'Pass Rate', key: 'rate', width: 15 }
    ];
    Object.keys(categories).forEach(cat => {
        const c = categories[cat];
        sheetCats.addRow({
            category: cat, total: c.total, passed: c.passed, failed: c.failed, 
            rate: c.total > 0 ? `${Math.round((c.passed / c.total) * 100)}%` : '0%'
        });
    });

    // Sheet 4: Failures
    const sheetFails = workbook.addWorksheet('Failures');
    sheetFails.columns = [
        { header: 'Test ID', key: 'id', width: 15 },
        { header: 'Test Name', key: 'name', width: 50 },
        { header: 'Error', key: 'error', width: 50 }
    ];
    sheetFails.addRows(failures);

    await workbook.xlsx.writeFile(outputExcel);
    console.log(`Report generated: ${outputExcel}`);
}

generateReport().catch(console.error);
