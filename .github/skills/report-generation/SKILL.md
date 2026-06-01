# Skill: Extent Report Generation

## Purpose
Integrate **ExtentReports 5.x** with the existing TestNG test suite so that every test run automatically
produces a rich HTML report at `reports/ExtentReport.html`.  
The report shows test suite name, test method name, pass / fail / skip status, failure messages,
stack traces, and an overall summary dashboard — all without changing any existing test logic.

> ✅ `extentreports 5.1.1` is **already declared** in `pom.xml` — no new dependency is needed.

---

## Workflow

```
mvn test  (or IDE TestNG run)
       ↓
ExtentReportManager (ITestListener) fires automatically
       ↓
reports/ExtentReport.html  ← open in any browser
```

---

## Files to Create / Modify

| Action | File |
|---|---|
| **CREATE** | `src/main/java/com/hiring/utils/ExtentReportManager.java` |
| **MODIFY** | `testng.xml` — add listener |
| **VERIFY** | `pom.xml` — `extentreports 5.1.1` already present ✅ |

---

## Step 1 — Create `ExtentReportManager.java`

**Location**: `src/main/java/com/hiring/utils/ExtentReportManager.java`

```java
package com.hiring.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ExtentReportManager — TestNG ITestListener that auto-generates an HTML Extent Report
 * for every test run without modifying any test class.
 *
 * Report output : reports/ExtentReport.html
 * Theme         : DARK
 * Triggered by  : testng.xml <listeners> entry
 */
public class ExtentReportManager implements ITestListener {

    // ── Shared report instance (one per suite run) ────────────────────────────
    private static ExtentReports extentReports;

    // ── Per-thread test node (safe for parallel execution) ───────────────────
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    // ── Report output path ────────────────────────────────────────────────────
    private static final String REPORT_PATH = "reports/ExtentReport.html";

    // =========================================================================
    //  Suite-level hooks
    // =========================================================================

    /**
     * Called once before the suite starts.
     * Initialises the ExtentReports instance and the SparkReporter.
     */
    @Override
    public void onStart(ITestContext context) {
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
        extentReports.setSystemInfo("Suite",       context.getSuite().getName());
        extentReports.setSystemInfo("Environment", "localhost:5000");
        extentReports.setSystemInfo("Framework",   "RestAssured + TestNG");
        extentReports.setSystemInfo("Run Date",    timestamp);
        extentReports.setSystemInfo("Author",      System.getProperty("user.name"));

        System.out.println("[ExtentReportManager] Report initialised → " + REPORT_PATH);
    }

    /**
     * Called once after the entire suite finishes.
     * Flushes all logged data to the HTML file.
     */
    @Override
    public void onFinish(ITestContext context) {
        if (extentReports != null) {
            extentReports.flush();
            System.out.println("[ExtentReportManager] Report flushed → " + REPORT_PATH);
        }
    }

    // =========================================================================
    //  Test-level hooks
    // =========================================================================

    /**
     * Called when a test method starts.
     * Creates a new ExtentTest node for the method.
     */
    @Override
    public void onTestStart(ITestResult result) {
        String testName        = result.getMethod().getMethodName();
        String testDescription = result.getMethod().getDescription();
        String className       = result.getTestClass().getName();

        String[] groups = result.getMethod().getGroups();
        String groupLabel = (groups != null && groups.length > 0) ? String.join(", ", groups) : "—";

        ExtentTest test = extentReports
                .createTest(testName, testDescription != null ? testDescription : testName)
                .assignCategory(groupLabel)
                .assignDevice(className);

        extentTest.set(test);
        extentTest.get().log(Status.INFO, "Test started: <b>" + testName + "</b>");
    }

    /**
     * Called when a test method passes.
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS,
                "✅ <b>PASSED</b> — " + result.getMethod().getMethodName());
    }

    /**
     * Called when a test method fails.
     * Logs the failure message and full stack trace.
     */
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

    /**
     * Called when a test method is skipped (e.g., dependency failure).
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        extentTest.get().log(Status.SKIP,
                "⚠️ <b>SKIPPED</b> — " + result.getMethod().getMethodName());
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            extentTest.get().log(Status.SKIP, "<b>Reason:</b> " + throwable.getMessage());
        }
    }

    // =========================================================================
    //  Public static accessor (optional — for logging inside test methods)
    // =========================================================================

    /**
     * Returns the ExtentTest node for the currently running thread.
     * Call this from a test method to add custom log entries:
     *
     * <pre>
     *   ExtentReportManager.getTest().info("Custom log message");
     *   ExtentReportManager.getTest().pass("Assertion passed");
     * </pre>
     *
     * @return ExtentTest node for the current thread
     */
    public static ExtentTest getTest() {
        return extentTest.get();
    }
}
```

---

## Step 2 — Register the Listener in `testng.xml`

Add the `<listeners>` block **inside** `<suite>`, before any `<test>` elements:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="HiringManagementSystem API Test Suite" verbose="1">

    <!-- ── Extent Report Listener ───────────────────────────── -->
    <listeners>
        <listener class-name="com.hiring.utils.ExtentReportManager"/>
    </listeners>
    <!-- ──────────────────────────────────────────────────────── -->

    <test name="Smoke">
        <classes>
            <class name="com.hiring.tests.SampleTest"/>
        </classes>
    </test>

    <test name="Userstory3 - End-to-End">
        <classes>
            <class name="com.hiring.tests.Userstory3EndToEndTest"/>
        </classes>
    </test>

</suite>
```

> ✅ The listener fires automatically for **every** test in every `<test>` block — no changes to test classes needed.

---

## Step 3 — (Optional) Add Custom Inline Logging in Test Methods

After adding the listener you can optionally enrich specific steps with inline log entries
by calling the static accessor:

```java
import com.hiring.utils.ExtentReportManager;
import com.aventstack.extentreports.Status;

// Inside any @Test method:
ExtentReportManager.getTest().info("Sending POST /api/jobs with payload: " + payload);
ExtentReportManager.getTest().pass("Job created — ID: " + jobId);
ExtentReportManager.getTest().fail("Unexpected status: " + response.getStatusCode());
```

> ✅ This is **optional** — the listener captures pass / fail / skip automatically without any inline calls.

---

## Step 4 — Run and View the Report

### Run via Maven
```bash
mvn test
```

### Run via IntelliJ / IDE
Right-click `testng.xml` → **Run**

### Open the Report
```
reports/ExtentReport.html
```
Open in any browser — Chrome, Firefox, Edge.

---

## Report Output Reference

| Property | Value |
|---|---|
| Output file | `reports/ExtentReport.html` |
| Report title | `HireFlow API Test Report` |
| Theme | Dark |
| Columns | Test Name · Status · Category (group) · Class · Duration · Timestamp |
| System info | Project, Suite, Environment, Framework, Run Date, Author |

### Report Structure

```
ExtentReport.html
├── Dashboard          ← pass/fail/skip pie chart + totals
├── Tests
│   ├── [PASS] postNewJob                  ← Smoke
│   ├── [PASS] registerAdditionalUsers     ← Regression
│   ├── [PASS] adminViewsAllRegisteredUsers
│   ├── [FAIL] recruiter1CreatesJob1...    ← failure message + stack trace
│   └── [SKIP] verifyAutoRejection...      ← skip reason
└── Exceptions         ← aggregated failure list
```

---

## Rules

- ✅ `ExtentReportManager` must implement `org.testng.ITestListener` — no other interface needed.
- ✅ Always use `ThreadLocal<ExtentTest>` to support parallel test execution safely.
- ✅ Register the listener in `testng.xml` `<listeners>` — **never** use `@Listeners` annotation on test classes (would duplicate entries).
- ✅ Call `extentReports.flush()` in `onFinish` — without this the HTML file is incomplete.
- ✅ The `reports/` directory is created automatically by `ExtentSparkReporter` — no manual `mkdir` needed.
- ✅ Place `ExtentReportManager.java` in `src/main/java/com/hiring/utils/` alongside `BaseTest`, `RestUtils`, `URLGenerator`.
- ❌ Do NOT create a separate `@BeforeSuite` / `@AfterSuite` in a base class for report init — the `ITestListener` `onStart` / `onFinish` hooks handle this.
- ❌ Do NOT hardcode absolute paths for the report output — use a relative path so it works on any machine.
- ❌ Do NOT call `extentReports.flush()` after every test — only call it once in `onFinish`.

---

## Verification Checklist

After implementation, confirm:

- [ ] `src/main/java/com/hiring/utils/ExtentReportManager.java` exists and compiles (`mvn compile`)
- [ ] `testng.xml` contains the `<listeners>` block with `com.hiring.utils.ExtentReportManager`
- [ ] `mvn test` completes without errors
- [ ] `reports/ExtentReport.html` is created in the project root
- [ ] Opening the HTML file shows the Dashboard with correct pass/fail/skip counts
- [ ] Failed tests show the exception message and stack trace
- [ ] Skipped tests show the dependency-failure reason

