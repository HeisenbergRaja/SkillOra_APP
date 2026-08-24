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
            data.suites.forEach(suite => {
                const category = suite.title.split(' ')[0] || 'General';
                if (!categories[category]) categories[category] = { total: 0, passed: 0, failed: 0, skipped: 0 };
                
                suite.tests.forEach(test => {
                    totalTests++;
                    totalDuration += test.duration || 0;
                    categories[category].total++;
                    
                    const state = test.state || (test.passed ? 'passed' : 'failed');
                    
                    if (state === 'passed') {
                        passed++;
                        categories[category].passed++;
                    } else if (state === 'failed') {
                        failed++;
                        categories[category].failed++;
                        failures.push({
                            id: `APP-${String(totalTests).padStart(3, '0')}`,
                            name: test.title,
                            error: test.error || 'Assertion Failed'
                        });
                    } else {
                        skipped++;
                        categories[category].skipped++;
                    }
                    
                    allTests.push({
                        id: `APP-${String(totalTests).padStart(3, '0')}`,
                        category,
                        name: test.title,
                        status: state.toUpperCase(),
                        duration: test.duration,
                        error: state === 'failed' ? (test.error || 'Failed') : ''
                    });
                });
            });
        } catch (e) {
            console.error(`Error parsing ${file}:`, e);
        }
    });

    const passRate = totalTests === 0 ? '0%' : `${Math.round((passed / totalTests) * 100)}%`;

    // 1. Write Markdown Summary
    const md = `
# SkillOra Android Appium E2E Test Summary

| Metric | Result |
|---|---:|
| Total Tests | ${totalTests} |
| Passed | ${passed} |
| Failed | ${failed} |
| Skipped | ${skipped} |
| Pass Rate | ${passRate} |
| Device | Android Emulator |
| Execution Time | ${Math.round(totalDuration / 1000)}s |
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
        { metric: 'Passed', value: passed },
        { metric: 'Failed', value: failed },
        { metric: 'Skipped', value: skipped },
        { metric: 'Pass Rate', value: passRate },
        { metric: 'Execution Time (s)', value: Math.round(totalDuration / 1000) },
        { metric: 'Device', value: 'Android Emulator' }
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
    console.log(\`Report generated: \${outputExcel}\`);
}

generateReport().catch(console.error);
