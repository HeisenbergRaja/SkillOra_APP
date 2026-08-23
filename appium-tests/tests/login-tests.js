describe('Authentication Module', () => {
    it('APP-001: Valid login should redirect to Home', async () => {
        const emailInput = await $('~Email Input'); // Update with actual accessibility id
        await emailInput.waitForDisplayed();
        await emailInput.setValue(process.env.TEST_EMAIL || 'test@example.com');
        
        const passwordInput = await $('~Password Input'); // Update with actual accessibility id
        await passwordInput.setValue(process.env.TEST_PASSWORD || 'password123');
        
        const loginButton = await $('~Login Button'); // Update with actual accessibility id
        await loginButton.click();
        
        const homeScreen = await $('~Home Screen'); // Update with actual accessibility id
        await expect(homeScreen).toBeDisplayed();
    });

    it('APP-002: Invalid password should show error', async () => {
        const emailInput = await $('~Email Input');
        await emailInput.waitForDisplayed();
        await emailInput.setValue(process.env.TEST_EMAIL || 'test@example.com');
        
        const passwordInput = await $('~Password Input');
        await passwordInput.setValue('wrongpassword');
        
        const loginButton = await $('~Login Button');
        await loginButton.click();
        
        const errorMessage = await $('~Error Message');
        await expect(errorMessage).toBeDisplayed();
    });
});
