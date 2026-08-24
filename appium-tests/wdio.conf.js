const fs = require("fs");
const path = require("path");

// Completely disable WebdriverIO's internal ESM loader injection for workers
process.env.WDIO_LOAD_TS_NODE = '0';
process.env.NODE_OPTIONS = '';

exports.config = {
    // Appium connection details
    port: 4723,
    path: '/',

    // Disable auto-compilation to prevent ts-node/esm loader errors entirely
    autoCompileOpts: {
        autoCompile: false
    },
    
    runner: "local",

    // Override the Node arguments for workers to strip any injected ts-node --loader flags
    execArgv: [],

    specs: [
        "./tests/**/*.js"
    ],

    maxInstances: 1,

    capabilities: [
        {
            maxInstances: 1,
            platformName: "Android",
            "appium:automationName": "UiAutomator2",
            "appium:deviceName": process.env.ANDROID_DEVICE || "Android Emulator",
            "appium:appPackage": process.env.ANDROID_APP_PACKAGE || "com.simats.skillora",
            "appium:appActivity": process.env.ANDROID_APP_ACTIVITY || "com.simats.skillora.MainActivity",
            "appium:platformVersion": process.env.ANDROID_PLATFORM_VERSION || "35",
            "appium:app": process.env.APK_PATH,
            "appium:autoGrantPermissions": true,
            "appium:noReset": false,
            "appium:newCommandTimeout": 180,
            "appium:appWaitActivity": "*",
            "appium:adbExecTimeout": 120000
        }
    ],

    logLevel: "info",

    framework: "mocha",

    mochaOpts: {
        timeout: 120000,
        ui: "bdd"
    },

    reporters: [
        "spec",
        [
            "json",
            {
                outputDir: "./reports/json"
            }
        ]
    ],

    afterTest: async function (test, context, { error, result, duration, passed, retries }) {
        if (!passed) {
            const timestamp = new Date().getTime();
            const safeTitle = test.title.replace(/[^a-z0-9]/gi, '_').toLowerCase();
            
            // Capture screenshot
            const screenshotDir = path.join(__dirname, 'reports', 'screenshots');
            if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir, { recursive: true });
            await browser.saveScreenshot(path.join(screenshotDir, `fail_${safeTitle}_${timestamp}.png`));
            
            // Capture page source
            const sourceDir = path.join(__dirname, 'reports', 'page-source');
            if (!fs.existsSync(sourceDir)) fs.mkdirSync(sourceDir, { recursive: true });
            const source = await browser.getPageSource();
            fs.writeFileSync(path.join(sourceDir, `fail_${safeTitle}_${timestamp}.xml`), source);
            
            // Capture logcat
            try {
                const logsDir = path.join(__dirname, 'reports', 'logcat');
                if (!fs.existsSync(logsDir)) fs.mkdirSync(logsDir, { recursive: true });
                const logs = await browser.getLogs('logcat');
                fs.writeFileSync(path.join(logsDir, `fail_${safeTitle}_${timestamp}.txt`), JSON.stringify(logs, null, 2));
            } catch (e) {
                // Ignore logcat errors
            }
        }
    }
};
