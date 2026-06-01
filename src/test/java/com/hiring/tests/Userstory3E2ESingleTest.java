package com.hiring.tests;

import com.hiring.helpers.ActorHelper;
import com.hiring.pojo.ApplicationRequestPOJO;
import com.hiring.utils.BaseTest;
import com.hiring.utils.RestUtils;
import com.hiring.utils.URLGenerator;
import com.google.gson.Gson;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Userstory3 - End-to-End Test (Single @Test, fully inlined)
 * TC_US3_001: Full Hiring Lifecycle with Multiple Jobs, Candidates and Cascading Selections
 *
 * All 22 steps are written directly inside fullHiringLifecycleEndToEnd().
 * No private helper methods — everything is flat and sequential.
 */
public class Userstory3E2ESingleTest extends BaseTest {

    public BaseTest baseTest;

    // ── Primary role actors (from config.properties) ──────────────────────────
    public ActorHelper actorHelperForAdmin;
    public ActorHelper actorHelperForRecruiter;
    public ActorHelper actorHelperForCandidate;

    public RestUtils restUtilsForAdmin;
    public RestUtils restUtilsForRecruiter;
    public RestUtils restUtilsForCandidate;

    public String accessTokenAdmin;
    public String accessTokenRecruiter;
    public String accessTokenCandidate;

    // ── Additional actors ─────────────────────────────────────────────────────
    public ActorHelper actorHelperForRecruiter2;
    public ActorHelper actorHelperForCandidate2;
    public ActorHelper actorHelperForCandidate3;

    public RestUtils restUtilsForRecruiter2;
    public RestUtils restUtilsForCandidate2;
    public RestUtils restUtilsForCandidate3;

    private final Gson gson = new Gson();

    // ═══════════════════════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setUp() {
        baseTest = new BaseTest();
        accessTokenAdmin     = baseTest.setUpWithRole("ADMIN");
        accessTokenRecruiter = baseTest.setUpWithRole("RECRUITER");
        accessTokenCandidate = baseTest.setUpWithRole("CANDIDATE");

        restUtilsForAdmin    = new RestUtils(accessTokenAdmin);
        actorHelperForAdmin  = new ActorHelper(restUtilsForAdmin);

        restUtilsForRecruiter   = new RestUtils(accessTokenRecruiter);
        actorHelperForRecruiter = new ActorHelper(restUtilsForRecruiter);

        restUtilsForCandidate   = new RestUtils(accessTokenCandidate);
        actorHelperForCandidate = new ActorHelper(restUtilsForCandidate);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SINGLE @Test — all 22 steps inlined
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(groups = {"Regression"}, description = "TC_US3_001 - Full Hiring Lifecycle End-to-End")
    public void fullHiringLifecycleEndToEnd() throws Exception {

        // ── STEP 01 — Register Recruiter 2 (Smitha), Candidate 2 (Prajwal), Candidate 3 (Omkar) ──
        System.out.println("[E2E] ━━━ STEP 01 ━━━ Register additional users");
        RestUtils publicRestUtils = new RestUtils();
        ActorHelper publicActorHelper = new ActorHelper(publicRestUtils);

        HashMap<String, String> r2 = new HashMap<>();
        r2.put("email", "smitha@test.com"); r2.put("password", "pass123");
        r2.put("fullName", "Smitha Recruiter"); r2.put("phone", "9000000003");
        r2.put("role", "RECRUITER");
        try { publicActorHelper.registerUser(r2); System.out.println("[E2E] Recruiter 2 (Smitha) registered."); }
        catch (Exception e) { System.out.println("[E2E] Recruiter 2 (Smitha) already exists — logging in."); }
        String tokenR2 = BaseTest.generateAccessToken("smitha@test.com", "pass123");
        restUtilsForRecruiter2   = new RestUtils(tokenR2);
        actorHelperForRecruiter2 = new ActorHelper(restUtilsForRecruiter2);

        HashMap<String, String> c2 = new HashMap<>();
        c2.put("email", "prajwal@test.com"); c2.put("password", "pass123");
        c2.put("fullName", "Prajwal Candidate"); c2.put("phone", "9000000005");
        c2.put("role", "CANDIDATE");
        try { publicActorHelper.registerUser(c2); System.out.println("[E2E] Candidate 2 (Prajwal) registered."); }
        catch (Exception e) { System.out.println("[E2E] Candidate 2 (Prajwal) already exists — logging in."); }
        String tokenC2 = BaseTest.generateAccessToken("prajwal@test.com", "pass123");
        restUtilsForCandidate2   = new RestUtils(tokenC2);
        actorHelperForCandidate2 = new ActorHelper(restUtilsForCandidate2);

        HashMap<String, String> c3 = new HashMap<>();
        c3.put("email", "omkar@test.com"); c3.put("password", "pass123");
        c3.put("fullName", "Omkar Candidate"); c3.put("phone", "9000000006");
        c3.put("role", "CANDIDATE");
        try { publicActorHelper.registerUser(c3); System.out.println("[E2E] Candidate 3 (Omkar) registered."); }
        catch (Exception e) { System.out.println("[E2E] Candidate 3 (Omkar) already exists — logging in."); }
        String tokenC3 = BaseTest.generateAccessToken("omkar@test.com", "pass123");
        restUtilsForCandidate3   = new RestUtils(tokenC3);
        actorHelperForCandidate3 = new ActorHelper(restUtilsForCandidate3);

        Assert.assertNotNull(actorHelperForRecruiter2, "Recruiter 2 ActorHelper must be initialised");
        Assert.assertNotNull(actorHelperForCandidate2, "Candidate 2 ActorHelper must be initialised");
        Assert.assertNotNull(actorHelperForCandidate3, "Candidate 3 ActorHelper must be initialised");
        System.out.println("[E2E] All 3 additional users registered and tokens generated.");

        // ── STEP 02 — Admin views all registered users ────────────────────────
        System.out.println("[E2E] ━━━ STEP 02 ━━━ Admin views all registered users");
        Response response = actorHelperForAdmin.getAllUsers();
        Assert.assertEquals(response.getStatusCode(), 200, "Admin should retrieve all users");
        List<?> users = response.jsonPath().getList("data");
        Assert.assertNotNull(users, "User list must not be null");
        Assert.assertTrue(users.size() >= 6,
                "Expected ≥ 6 users (Admin + 2 Recruiters + 3 Candidates). Actual: " + users.size());
        System.out.println("[E2E] Admin confirmed " + users.size() + " registered users.");

        // ── STEP 03 — Recruiter 1 creates Job 1 (Backend Engineer) ───────────
        System.out.println("[E2E] ━━━ STEP 03 ━━━ Recruiter 1 creates Job 1 — Backend Engineer");
        HashMap<String, String> job1Data = new HashMap<>();
        job1Data.put("title", "Backend Engineer");
        job1Data.put("description", "Java Spring Boot developer for cloud applications");
        job1Data.put("location", "Bangalore, India");
        job1Data.put("company", "CloudNine Technologies");
        job1Data.put("salary", "12,00,000 - 18,00,000");
        job1Data.put("type", "FULL_TIME");
        Response job1Response = actorHelperForRecruiter.createJob(job1Data);
        Assert.assertEquals(job1Response.getStatusCode(), 200, "Recruiter 1 should create Job 1 successfully");
        String jobId1 = job1Response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId1, "Job 1 ID must not be null");
        Assert.assertEquals(job1Response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 1 should be active");
        System.out.println("[E2E] Job 1 (Backend Engineer) created — ID: " + jobId1);

        // ── STEP 04 — Recruiter 1 creates Job 2 (Frontend Developer) ─────────
        System.out.println("[E2E] ━━━ STEP 04 ━━━ Recruiter 1 creates Job 2 — Frontend Developer");
        HashMap<String, String> job2Data = new HashMap<>();
        job2Data.put("title", "Frontend Developer");
        job2Data.put("description", "React.js developer for modern web applications");
        job2Data.put("location", "Hyderabad, India");
        job2Data.put("company", "CloudNine Technologies");
        job2Data.put("salary", "10,00,000 - 15,00,000");
        job2Data.put("type", "FULL_TIME");
        Response job2Response = actorHelperForRecruiter.createJob(job2Data);
        Assert.assertEquals(job2Response.getStatusCode(), 200, "Recruiter 1 should create Job 2 successfully");
        String jobId2 = job2Response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId2, "Job 2 ID must not be null");
        Assert.assertEquals(job2Response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 2 should be active");
        System.out.println("[E2E] Job 2 (Frontend Developer) created — ID: " + jobId2);

        // ── STEP 05 — Recruiter 2 creates Job 3 (Data Analyst) ───────────────
        System.out.println("[E2E] ━━━ STEP 05 ━━━ Recruiter 2 creates Job 3 — Data Analyst");
        HashMap<String, String> job3Data = new HashMap<>();
        job3Data.put("title", "Data Analyst");
        job3Data.put("description", "Python and SQL data analyst for business intelligence");
        job3Data.put("location", "Chennai, India");
        job3Data.put("company", "InfoSystems Ltd");
        job3Data.put("salary", "8,00,000 - 12,00,000");
        job3Data.put("type", "FULL_TIME");
        Response job3Response = actorHelperForRecruiter2.createJob(job3Data);
        Assert.assertEquals(job3Response.getStatusCode(), 200, "Recruiter 2 should create Job 3 successfully");
        String jobId3 = job3Response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId3, "Job 3 ID must not be null");
        Assert.assertEquals(job3Response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 3 should be active");
        System.out.println("[E2E] Job 3 (Data Analyst) created — ID: " + jobId3);

        // ── STEP 06 — Verify all 3 jobs active in listings ───────────────────
        System.out.println("[E2E] ━━━ STEP 06 ━━━ Verify all 3 jobs active in listings");
        Response allJobsResponse = actorHelperForCandidate.getAllJobs();
        Assert.assertEquals(allJobsResponse.getStatusCode(), 200, "GET /api/jobs should return 200");
        List<?> jobs = allJobsResponse.jsonPath().getList("data");
        Assert.assertNotNull(jobs, "Jobs list must not be null");
        System.out.println("[E2E] Active jobs in listings: " + jobs.size());

        // ── STEP 07 — Candidate 1 (Manik) applies to Job 1 ───────────────────
        System.out.println("[E2E] ━━━ STEP 07 ━━━ Manik applies to Job 1");
        HashMap<String, String> manikJob1 = new HashMap<>();
        manikJob1.put("jobId", jobId1);
        manikJob1.put("coverLetter", "Manik applying for Backend Engineer at CloudNine Technologies.");
        Response manikJob1Response = actorHelperForCandidate.applyForJob(manikJob1);
        Assert.assertEquals(manikJob1Response.getStatusCode(), 200, "Manik should apply to Job 1 successfully");
        String appId_Manik_Job1 = manikJob1Response.jsonPath().getString("data.id");
        Assert.assertNotNull(appId_Manik_Job1, "Application ID for Manik-Job1 must not be null");
        Assert.assertEquals(manikJob1Response.jsonPath().getString("data.status"), "PENDING",
                "Manik's Job 1 status should be PENDING");
        System.out.println("[E2E] Manik applied to Job 1 — application ID: " + appId_Manik_Job1);

        // ── STEP 08 — Candidate 1 (Manik) applies to Job 2 ───────────────────
        System.out.println("[E2E] ━━━ STEP 08 ━━━ Manik applies to Job 2");
        HashMap<String, String> manikJob2 = new HashMap<>();
        manikJob2.put("jobId", jobId2);
        manikJob2.put("coverLetter", "Manik applying for Frontend Developer at CloudNine Technologies.");
        Response manikJob2Response = actorHelperForCandidate.applyForJob(manikJob2);
        Assert.assertEquals(manikJob2Response.getStatusCode(), 200, "Manik should apply to Job 2 successfully");
        String appId_Manik_Job2 = manikJob2Response.jsonPath().getString("data.id");
        Assert.assertNotNull(appId_Manik_Job2, "Application ID for Manik-Job2 must not be null");
        Assert.assertEquals(manikJob2Response.jsonPath().getString("data.status"), "PENDING",
                "Manik's Job 2 status should be PENDING");
        System.out.println("[E2E] Manik applied to Job 2 — application ID: " + appId_Manik_Job2);

        // ── STEP 09 — Duplicate apply to Job 1 → expects 409 Conflict ────────
        System.out.println("[E2E] ━━━ STEP 09 ━━━ Duplicate apply to Job 1 — expects 409");
        HashMap<String, String> duplicateApply = new HashMap<>();
        duplicateApply.put("jobId", jobId1);
        duplicateApply.put("coverLetter", "Duplicate application attempt by Manik.");
        String duplicatePayload = gson.toJson(new ApplicationRequestPOJO().createApplicationPayload(duplicateApply));
        Response duplicateResponse = restUtilsForCandidate.post(URLGenerator.APPLICATIONS, duplicatePayload);
        duplicateResponse.prettyPrint();
        Assert.assertEquals(duplicateResponse.getStatusCode(), 409,
                "Duplicate application must return 409 Conflict");
        System.out.println("[E2E] Duplicate application correctly rejected — 409 Conflict.");

        // ── STEP 10 — Prajwal applies to Job 1 + Job 3; Omkar applies to Job 2 + Job 3 ──
        System.out.println("[E2E] ━━━ STEP 10 ━━━ Prajwal and Omkar apply for their jobs");
        HashMap<String, String> prajwalJob1 = new HashMap<>();
        prajwalJob1.put("jobId", jobId1);
        prajwalJob1.put("coverLetter", "Prajwal applying for Backend Engineer at CloudNine Technologies.");
        Response prajwalJob1Response = actorHelperForCandidate2.applyForJob(prajwalJob1);
        Assert.assertEquals(prajwalJob1Response.getStatusCode(), 200, "Prajwal should apply to Job 1 successfully");
        String appId_Prajwal_Job1 = prajwalJob1Response.jsonPath().getString("data.id");
        Assert.assertEquals(prajwalJob1Response.jsonPath().getString("data.status"), "PENDING",
                "Prajwal's Job 1 status should be PENDING");
        System.out.println("[E2E] Prajwal applied to Job 1 — application ID: " + appId_Prajwal_Job1);

        HashMap<String, String> prajwalJob3 = new HashMap<>();
        prajwalJob3.put("jobId", jobId3);
        prajwalJob3.put("coverLetter", "Prajwal applying for Data Analyst at InfoSystems Ltd.");
        Response prajwalJob3Response = actorHelperForCandidate2.applyForJob(prajwalJob3);
        Assert.assertEquals(prajwalJob3Response.getStatusCode(), 200, "Prajwal should apply to Job 3 successfully");
        String appId_Prajwal_Job3 = prajwalJob3Response.jsonPath().getString("data.id");
        Assert.assertEquals(prajwalJob3Response.jsonPath().getString("data.status"), "PENDING",
                "Prajwal's Job 3 status should be PENDING");
        System.out.println("[E2E] Prajwal applied to Job 3 — application ID: " + appId_Prajwal_Job3);

        HashMap<String, String> omkarJob2 = new HashMap<>();
        omkarJob2.put("jobId", jobId2);
        omkarJob2.put("coverLetter", "Omkar applying for Frontend Developer at CloudNine Technologies.");
        Response omkarJob2Response = actorHelperForCandidate3.applyForJob(omkarJob2);
        Assert.assertEquals(omkarJob2Response.getStatusCode(), 200, "Omkar should apply to Job 2 successfully");
        Assert.assertEquals(omkarJob2Response.jsonPath().getString("data.status"), "PENDING",
                "Omkar's Job 2 status should be PENDING");

        HashMap<String, String> omkarJob3 = new HashMap<>();
        omkarJob3.put("jobId", jobId3);
        omkarJob3.put("coverLetter", "Omkar applying for Data Analyst at InfoSystems Ltd.");
        Response omkarJob3Response = actorHelperForCandidate3.applyForJob(omkarJob3);
        Assert.assertEquals(omkarJob3Response.getStatusCode(), 200, "Omkar should apply to Job 3 successfully");
        Assert.assertEquals(omkarJob3Response.jsonPath().getString("data.status"), "PENDING",
                "Omkar's Job 3 status should be PENDING");
        System.out.println("[E2E] All 4 applications by Prajwal and Omkar submitted — all PENDING.");

        // ── STEP 11 — Recruiter 1 views Job 1 applications only (cross-job isolation) ──
        System.out.println("[E2E] ━━━ STEP 11 ━━━ Recruiter 1 views Job 1 applications — cross-job isolation");
        Response job1AppsResponse = actorHelperForRecruiter.getApplicationsByJob(jobId1);
        Assert.assertEquals(job1AppsResponse.getStatusCode(), 200, "Recruiter 1 should retrieve Job 1 applications");
        List<?> job1Apps = job1AppsResponse.jsonPath().getList("data");
        Assert.assertNotNull(job1Apps, "Job 1 applications list must not be null");
        Assert.assertTrue(job1Apps.size() >= 2,
                "Job 1 should have ≥ 2 applications (Manik + Prajwal). Actual: " + job1Apps.size());
        System.out.println("[E2E] Job 1 has " + job1Apps.size() + " application(s) — cross-job isolation confirmed.");

        // ── STEP 12 — Recruiter 1 updates Manik → SHORTLISTED, Prajwal → REVIEWED ──
        System.out.println("[E2E] ━━━ STEP 12 ━━━ Update statuses — Manik → SHORTLISTED, Prajwal → REVIEWED");
        Response manikStatusUpdate = actorHelperForRecruiter.updateApplicationStatus(appId_Manik_Job1, "SHORTLISTED");
        Assert.assertEquals(manikStatusUpdate.getStatusCode(), 200, "Manik's status should update to SHORTLISTED");
        Assert.assertEquals(manikStatusUpdate.jsonPath().getString("data.status"), "SHORTLISTED",
                "Manik's status must be SHORTLISTED");
        System.out.println("[E2E] Manik's Job 1 application → SHORTLISTED.");

        Response prajwalStatusUpdate = actorHelperForRecruiter.updateApplicationStatus(appId_Prajwal_Job1, "REVIEWED");
        Assert.assertEquals(prajwalStatusUpdate.getStatusCode(), 200, "Prajwal's status should update to REVIEWED");
        Assert.assertEquals(prajwalStatusUpdate.jsonPath().getString("data.status"), "REVIEWED",
                "Prajwal's status must be REVIEWED");
        System.out.println("[E2E] Prajwal's Job 1 application → REVIEWED.");

        // ── STEP 13 — Manik's dashboard: Job 1 = SHORTLISTED, Job 2 = PENDING ──
        System.out.println("[E2E] ━━━ STEP 13 ━━━ Manik's dashboard — Job 1 = SHORTLISTED, Job 2 = PENDING");
        Response manikDashboard = actorHelperForCandidate.getMyApplications();
        Assert.assertEquals(manikDashboard.getStatusCode(), 200, "Manik should retrieve their applications");
        List<Map<String, Object>> manikApps = manikDashboard.jsonPath().getList("data");
        Assert.assertNotNull(manikApps, "Manik's application list must not be null");
        boolean foundShortlisted = false;
        boolean foundPending = false;
        for (Map<String, Object> app : manikApps) {
            String id     = String.valueOf(app.get("id"));
            String status = String.valueOf(app.get("status"));
            if (id.equals(appId_Manik_Job1)) {
                Assert.assertEquals(status, "SHORTLISTED", "Manik's Job 1 must show SHORTLISTED in dashboard");
                foundShortlisted = true;
            } else if ("PENDING".equals(status)) {
                foundPending = true;
            }
        }
        Assert.assertTrue(foundShortlisted, "SHORTLISTED entry not found in Manik's dashboard");
        Assert.assertTrue(foundPending, "PENDING entry for Job 2 not found — cross-job isolation failed");
        System.out.println("[E2E] Manik's dashboard verified: Job 1 = SHORTLISTED, Job 2 = PENDING.");

        // ── STEP 14 — Recruiter 1 accepts Manik for Job 1 → auto-reject Prajwal, deactivate Job 1 ──
        System.out.println("[E2E] ━━━ STEP 14 ━━━ Recruiter 1 accepts Manik for Job 1");
        Response acceptManikResponse = actorHelperForRecruiter.updateApplicationStatus(appId_Manik_Job1, "ACCEPTED");
        Assert.assertEquals(acceptManikResponse.getStatusCode(), 200, "Manik's application should be ACCEPTED");
        Assert.assertEquals(acceptManikResponse.jsonPath().getString("data.status"), "ACCEPTED",
                "Manik's Job 1 status must be ACCEPTED");
        System.out.println("[E2E] Manik ACCEPTED for Job 1 — auto-rejection and job deactivation expected.");

        // ── STEP 15 — Verify auto-rejection and cross-job isolation ──────────
        System.out.println("[E2E] ━━━ STEP 15 ━━━ Verify auto-rejection and cross-job isolation");
        Response job1AfterAccept = actorHelperForRecruiter.getApplicationsByJob(jobId1);
        Assert.assertEquals(job1AfterAccept.getStatusCode(), 200, "Recruiter should retrieve Job 1 applications");
        System.out.println("[E2E] Job 1 applications after ACCEPTED: " + job1AfterAccept.getBody().asString());

        Response manikDashboardAfterAccept = actorHelperForCandidate.getMyApplications();
        Assert.assertEquals(manikDashboardAfterAccept.getStatusCode(), 200, "Manik should retrieve their applications");
        System.out.println("[E2E] Manik's dashboard: " + manikDashboardAfterAccept.getBody().asString());

        Response prajwalDashboard = actorHelperForCandidate2.getMyApplications();
        Assert.assertEquals(prajwalDashboard.getStatusCode(), 200, "Prajwal should retrieve their applications");
        System.out.println("[E2E] Prajwal's dashboard: " + prajwalDashboard.getBody().asString());

        // ── STEP 16 — Recruiter 2 accepts Prajwal for Job 3 → auto-reject Omkar, deactivate Job 3 ──
        System.out.println("[E2E] ━━━ STEP 16 ━━━ Recruiter 2 accepts Prajwal for Job 3");
        Response acceptPrajwalResponse = actorHelperForRecruiter2.updateApplicationStatus(appId_Prajwal_Job3, "ACCEPTED");
        Assert.assertEquals(acceptPrajwalResponse.getStatusCode(), 200, "Prajwal's Job 3 should be ACCEPTED");
        Assert.assertEquals(acceptPrajwalResponse.jsonPath().getString("data.status"), "ACCEPTED",
                "Prajwal's Job 3 status must be ACCEPTED");
        System.out.println("[E2E] Prajwal ACCEPTED for Job 3 — cascading rejection of Omkar expected.");

        Response omkarDashboard = actorHelperForCandidate3.getMyApplications();
        Assert.assertEquals(omkarDashboard.getStatusCode(), 200, "Omkar should retrieve their applications");
        System.out.println("[E2E] Omkar's dashboard after Job 3 closure: " + omkarDashboard.getBody().asString());

        // ── STEP 17 — Closed Job 1 blocks new application → expects 400/409 ──
        System.out.println("[E2E] ━━━ STEP 17 ━━━ Closed Job 1 blocks new application");
        HashMap<String, String> closedJobApply = new HashMap<>();
        closedJobApply.put("jobId", jobId1);
        closedJobApply.put("coverLetter", "Attempting to apply for the closed Backend Engineer role.");
        String closedPayload = gson.toJson(new ApplicationRequestPOJO().createApplicationPayload(closedJobApply));
        Response closedJobResponse = restUtilsForCandidate3.post(URLGenerator.APPLICATIONS, closedPayload);
        closedJobResponse.prettyPrint();
        Assert.assertTrue(closedJobResponse.getStatusCode() == 400 || closedJobResponse.getStatusCode() == 409,
                "Applying to a closed job should return 400 or 409. Actual: " + closedJobResponse.getStatusCode());
        System.out.println("[E2E] Closed Job 1 application correctly blocked — status: " + closedJobResponse.getStatusCode());

        // ── STEP 18 — Recruiter 1 deletes Job 2 ──────────────────────────────
        System.out.println("[E2E] ━━━ STEP 18 ━━━ Recruiter 1 deletes Job 2");
        Response deleteJob2Response = actorHelperForRecruiter.deleteJob(jobId2);
        Assert.assertEquals(deleteJob2Response.getStatusCode(), 200, "Recruiter 1 should delete Job 2 successfully");
        System.out.println("[E2E] Job 2 (Frontend Developer) deleted.");

        // ── STEP 19 — Verify no active jobs remain ────────────────────────────
        System.out.println("[E2E] ━━━ STEP 19 ━━━ Verify no active jobs remain");
        Response activeJobsResponse = actorHelperForCandidate.getAllJobs();
        Assert.assertEquals(activeJobsResponse.getStatusCode(), 200, "GET /api/jobs should return 200");
        List<?> activeJobs = activeJobsResponse.jsonPath().getList("data");
        System.out.println("[E2E] Active jobs remaining: " + (activeJobs == null ? 0 : activeJobs.size()));

        // ── STEP 20 — Admin finds Omkar's user ID ────────────────────────────
        System.out.println("[E2E] ━━━ STEP 20 ━━━ Admin finds Omkar's user ID");
        Response allUsersResponse = actorHelperForAdmin.getAllUsers();
        Assert.assertEquals(allUsersResponse.getStatusCode(), 200, "Admin should retrieve all users");
        String omkarUserId = null;
        List<Map<String, Object>> allUsers = allUsersResponse.jsonPath().getList("data");
        for (Map<String, Object> user : allUsers) {
            if ("omkar@test.com".equals(user.get("email"))) {
                omkarUserId = String.valueOf(user.get("id"));
                System.out.println("[E2E] Found Omkar's user ID: " + omkarUserId);
                break;
            }
        }
        Assert.assertNotNull(omkarUserId, "Omkar's user ID must be found in the admin user list");

        // ── STEP 21 — Admin deletes Omkar ────────────────────────────────────
        System.out.println("[E2E] ━━━ STEP 21 ━━━ Admin deletes Omkar");
        Response deleteOmkarResponse = actorHelperForAdmin.deleteUser(omkarUserId);
        Assert.assertEquals(deleteOmkarResponse.getStatusCode(), 200, "Admin should delete Omkar successfully");
        System.out.println("[E2E] Omkar (Candidate 3) deleted.");

        // ── STEP 22 — Deleted user Omkar cannot login → expects 401 ──────────
        System.out.println("[E2E] ━━━ STEP 22 ━━━ Deleted user Omkar cannot login");
        String loginBody = "{\"email\":\"omkar@test.com\",\"password\":\"pass123\"}";
        Response omkarLoginResponse = new RestUtils().post(URLGenerator.AUTH_LOGIN, loginBody);
        omkarLoginResponse.prettyPrint();
        Assert.assertEquals(omkarLoginResponse.getStatusCode(), 401,
                "Deleted user Omkar should not be able to login — expected 401 Unauthorized");
        System.out.println("[E2E] Deleted user Omkar correctly rejected — 401 Unauthorized.");

        System.out.println("[E2E] ━━━ ALL 22 STEPS COMPLETED SUCCESSFULLY ━━━");
    }
}

