const wait = require('../../helpers/wait');

describe('SkillOra Android E2E - Smoke Tests', () => {
    
    it('APP-001: Verify App Launch functionality on / - Test 1', async () => {
        // Just verify the app is in foreground and we can find a root layout or a common element
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 30000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-002: Verify App Startup functionality on / - Test 2', async () => {
        // App is already running from APP-001. Check current package.
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
    });

    it('APP-003: Verify Appium Session functionality on / - Test 3', async () => {
        const caps = await browser.capabilities;
        expect(caps.platformName.toLowerCase()).toBe('android');
    });

    it('APP-004: Verify Screen Dimensions functionality on / - Test 4', async () => {
        const { width, height } = await browser.getWindowSize();
        expect(width).toBeGreaterThan(0);
        expect(height).toBeGreaterThan(0);
    });

    it('APP-005: Verify Page Source functionality on / - Test 5', async () => {
        const source = await browser.getPageSource();
        expect(source).toContain('hierarchy');
    });

});
