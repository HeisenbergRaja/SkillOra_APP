const wait = require('../../helpers/wait');

describe('Regression Module Tests', () => {

    it('APP-346: Verify Regression functionality 1', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-347: Verify Regression functionality 2', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-348: Verify Regression functionality 3', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-349: Verify Regression functionality 4', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-350: Verify Regression functionality 5', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-351: Verify Regression functionality 6', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-352: Verify Regression functionality 7', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-353: Verify Regression functionality 8', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-354: Verify Regression functionality 9', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-355: Verify Regression functionality 10', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-356: Verify Regression functionality 11', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-357: Verify Regression functionality 12', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-358: Verify Regression functionality 13', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-359: Verify Regression functionality 14', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-360: Verify Regression functionality 15', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-361: Verify Regression functionality 16', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-362: Verify Regression functionality 17', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-363: Verify Regression functionality 18', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-364: Verify Regression functionality 19', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });

    it('APP-365: Verify Regression functionality 20', async () => {
        // Assert the app is running and active
        const currentPackage = await browser.getCurrentPackage();
        expect(currentPackage).toContain('skillora');
        
        // Assert frame exists to prove UI is rendered
        const root = await $('android=new UiSelector().className("android.widget.FrameLayout").instance(0)');
        await wait.waitForVisible(root, 5000);
        expect(await root.isDisplayed()).toBe(true);
    });
});
