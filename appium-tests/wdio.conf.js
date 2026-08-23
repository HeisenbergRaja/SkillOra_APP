exports.config = {
    runner: 'local',
    port: 4723,
    specs: [
        './tests/**/*.js'
    ],
    exclude: [
    ],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:automationName': 'UiAutomator2',
        'appium:deviceName': process.env.ANDROID_DEVICE_NAME || 'Android Emulator',
        'appium:appPackage': process.env.APP_PACKAGE || 'com.simats.skillora',
        'appium:appActivity': process.env.APP_ACTIVITY || 'com.simats.skillora.MainActivity',
        'appium:autoGrantPermissions': true
    }],
    logLevel: 'info',
    bail: 0,
    baseUrl: 'http://localhost',
    waitforTimeout: 10000,
    connectionRetryTimeout: 120000,
    connectionRetryCount: 3,
    framework: 'mocha',
    reporters: ['spec'],
    mochaOpts: {
        ui: 'bdd',
        timeout: 60000
    },
}
