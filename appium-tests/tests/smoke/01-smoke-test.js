const wait = require('../../helpers/wait');

describe('SkillOra Android E2E - Smoke Tests', () => {
    
    it('APP-001: App should launch successfully', async () => {
        // Just verify the app is in foreground and we can find a root layout or a common element
        const root = await $('android.widget.FrameLayout');
        await wait.waitForVisible(root, 30000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-002: App should not crash on startup', async () => {
        // App is already running from APP-001. Check current package.
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
    });

    it('APP-003: Verify Appium session capabilities', async () => {
        const caps = await browser.capabilities;
        expect(caps.platformName.toLowerCase()).toBe('android');
    });

    it('APP-004: Verify screen dimensions are valid', async () => {
        const { width, height } = await browser.getWindowSize();
        expect(width).toBeGreaterThan(0);
        expect(height).toBeGreaterThan(0);
    });

    it('APP-005: Can read page source without errors', async () => {
        const source = await browser.getPageSource();
        expect(source).toContain('hierarchy');
    });

});
