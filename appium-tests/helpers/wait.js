class WaitHelper {
    /**
     * Wait for an element to be visible
     * @param {WebdriverIO.Element} element - The element to wait for
     * @param {number} timeout - Timeout in ms
     */
    async waitForVisible(element, timeout = 15000) {
        await element.waitForDisplayed({
            timeout,
            timeoutMsg: \`Element \${element.selector} was not visible after \${timeout}ms\`
        });
    }

    /**
     * Wait for an element to be clickable
     * @param {WebdriverIO.Element} element - The element to wait for
     * @param {number} timeout - Timeout in ms
     */
    async waitForClickable(element, timeout = 15000) {
        await element.waitForClickable({
            timeout,
            timeoutMsg: \`Element \${element.selector} was not clickable after \${timeout}ms\`
        });
    }

    /**
     * Wait for an element to exist in DOM
     * @param {WebdriverIO.Element} element - The element to wait for
     * @param {number} timeout - Timeout in ms
     */
    async waitForExist(element, timeout = 15000) {
        await element.waitForExist({
            timeout,
            timeoutMsg: \`Element \${element.selector} did not exist after \${timeout}ms\`
        });
    }
}

module.exports = new WaitHelper();
