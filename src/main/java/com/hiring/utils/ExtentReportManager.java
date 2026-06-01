package com.hiring.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ExtentReportManager — generates a single HTML Extent Report for the entire suite.
 *
 * Implements BOTH:
 *   ISuiteListener  → initialises / flushes the report ONCE per suite run
 *   ITestListener   → logs each individual test result (pass / fail / skip)
 *
 * This guarantees that all test classes across all <test> blocks in testng.xml
 * are captured in one consolidated report at reports/ExtentReport.html.
 *
 * Registered in testng.xml:
 *   <listeners>
 *     <listener class-name="com.hiring.utils.ExtentReportManager"/>
 *   </listeners>
 */
public class ExtentReportManager implements ITestListener, ISuiteListener {

    // ── Single shared report instance for the whole suite ─────────────────────
    private static ExtentReports extentReports;

    // ── Per-thread test node (safe for parallel execution) ───────────────────
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    // ── Report output path — timestamped so every run creates a new file ─────
    private static final String REPORT_PATH =
            "reports/ExtentReport_" +
            new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date()) +
            ".html";

    // =========================================================================
    //  ISuiteListener — fires ONCE for the entire suite
    // =========================================================================

    /**
     * Called once when the suite starts (before any <test> block runs).
     * Initialises ExtentReports + SparkReporter.
     */
    @Override
    public void onStart(ISuite suite) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(REPORT_PATH);
        sparkReporter.config().setDocumentTitle("HireFlow API Test Report");
        sparkReporter.config().setReportName("QA Hiring Management System — API Test Results");
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
        sparkReporter.config().setEncoding("UTF-8");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Project",     "QA Hiring Management System");
        extentReports.setSystemInfo("Suite",       suite.getName());
        extentReports.setSystemInfo("Environment", "localhost:5000");
        extentReports.setSystemInfo("Framework",   "RestAssured + TestNG");
        extentReports.setSystemInfo("Run Date",    timestamp);
        extentReports.setSystemInfo("Author",      System.getProperty("user.name"));

        System.out.println("[ExtentReportManager] Suite started — report initialised → " + REPORT_PATH);
    }

    /**
     * Called once when the entire suite finishes.
     * Flushes all logged data to the HTML file.
     */
    @Override
    public void onFinish(ISuite suite) {
        if (extentReports != null) {
            extentReports.flush();
            System.out.println("[ExtentReportManager] Suite finished — report flushed → " + REPORT_PATH);
        }
    }

    // =========================================================================
    //  ITestListener (context-level) — no-ops: init/flush handled by ISuiteListener
    // =========================================================================

    @Override
    public void onStart(ITestContext context) {
        System.out.println("[ExtentReportManager] <test> block started: " + context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("[ExtentReportManager] <test> block finished: " + context.getName());
    }

    // =========================================================================
    //  ITestListener (test-level) — logs each @Test result into the report
    // =========================================================================

    /**
     * Called when a @Test method starts.
     * Creates a new ExtentTest node labelled with the method name, group and class.
     */
    @Override
    public void onTestStart(ITestResult result) {
        String testName        = result.getMethod().getMethodName();
        String testDescription = result.getMethod().getDescription();
        String className       = result.getTestClass().getName();

        String[] groups   = result.getMethod().getGroups();
        String groupLabel = (groups != null && groups.length > 0) ? String.join(", ", groups) : "—";

        ExtentTest test = extentReports
                .createTest(testName, testDescription != null ? testDescription : testName)
                .assignCategory(groupLabel)
                .assignDevice(className);

        extentTest.set(test);
        extentTest.get().log(Status.INFO, "Test started: <b>" + testName + "</b>");
    }

    /** Called when a @Test method passes. */
    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS,
                "✅ <b>PASSED</b> — " + result.getMethod().getMethodName());
    }

    /** Called when a @Test method fails. Logs message + full stack trace. */
    @Override
    public void onTestFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        extentTest.get().log(Status.FAIL,
                "❌ <b>FAILED</b> — " + result.getMethod().getMethodName());
        if (throwable != null) {
            extentTest.get().log(Status.FAIL, "<b>Cause:</b> " + throwable.getMessage());
            extentTest.get().fail(throwable);
        }
    }

    /** Called when a @Test method is skipped (e.g., upstream dependency failed). */
    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = extentTest.get();
        if (test == null) {
            // onTestStart was never called (config failure) — create a node now
            test = extentReports.createTest(result.getMethod().getMethodName())
                    .assignCategory("SKIPPED");
            extentTest.set(test);
        }
        test.log(Status.SKIP, "⚠️ <b>SKIPPED</b> — " + result.getMethod().getMethodName());
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            test.log(Status.SKIP, "<b>Reason:</b> " + throwable.getMessage());
        }
    }

    // =========================================================================
    //  Optional: inline logging from test methods
    // =========================================================================

    /**
     * Returns the ExtentTest node for the currently running thread.
     * Use inside any @Test method for custom log entries:
     *
     *   ExtentReportManager.getTest().info("Step: POST /api/jobs");
     *   ExtentReportManager.getTest().pass("Job created — ID: " + jobId);
     */
    public static ExtentTest getTest() {
        return extentTest.get();
    }
}
