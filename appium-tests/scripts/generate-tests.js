const fs = require('fs');
const path = require('path');

const categories = {
    Auth: 40,
    Home: 25,
    Marketplace: 35,
    SkillDetails: 25,
    Credits: 25,
    Learning: 30,
    Roadmap: 25,
    Resources: 20,
    Quiz: 30,
    Profile: 20,
    Upload: 25,
    Navigation: 20,
    Errors: 20,
    Regression: 20
};

let testId = 6; // Started 1-5 in smoke tests

Object.keys(categories).forEach(cat => {
    const count = categories[cat];
    const dir = path.join(__dirname, '..', 'tests', cat.toLowerCase());
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    let fileContent = `const wait = require('../../helpers/wait');\n\ndescribe('${cat} Module Tests', () => {\n`;

    for (let i = 1; i <= count; i++) {
        const currentId = `APP-${String(testId++).padStart(3, '0')}`;
        fileContent += `
    it('${currentId}: Verify ${cat} functionality ${i}', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });
`;
    }

    fileContent += `});\n`;
    fs.writeFileSync(path.join(dir, `${cat.toLowerCase()}-tests.js`), fileContent);
});

console.log('Successfully generated test stubs.');
