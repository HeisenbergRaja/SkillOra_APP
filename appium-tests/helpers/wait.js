async function waitForVisible(element, timeout = 15000) {
    await element.waitForDisplayed({
        timeout,
        timeoutMsg: `Element ${element.selector || 'unknown'} was not visible after ${timeout}ms`
    });
    return element;
}

async function waitForExist(element, timeout = 15000) {
    await element.waitForExist({
        timeout,
        timeoutMsg: `Element ${element.selector || 'unknown'} did not exist after ${timeout}ms`
    });
    return element;
}

async function waitAndClick(element, timeout = 15000) {
    await waitForVisible(element, timeout);
    await element.click();
}

module.exports = {
    waitForVisible,
    waitForExist,
    waitAndClick
};
