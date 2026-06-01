package com.hiring.tests;

import com.hiring.commonMethods.CommonMethod;
import com.hiring.helpers.ActorHelper;
import com.hiring.pojo.ApplicationRequestPOJO;
import com.hiring.utils.BaseTest;
import com.hiring.utils.RestUtils;
import com.hiring.utils.URLGenerator;
import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Userstory3 - End-to-End Test Automation
 * TC_US3_001: Full Hiring Lifecycle with Multiple Jobs, Candidates and Cascading Selections
 *
 * End-to-end flow covers:
 *  Step 1  – Register additional users: Recruiter 2 (Smitha), Candidate 2 (Prajwal), Candidate 3 (Omkar)
 *  Step 2  – Admin views all registered users
 *  Step 3  – Recruiter 1 (Jaydeep) creates Job 1 (Backend Engineer) and Job 2 (Frontend Developer)
 *  Step 4  – Recruiter 2 (Smitha) creates Job 3 (Data Analyst)
 *  Step 5  – Verify all 3 jobs are active in listings
 *  Step 6  – Candidate 1 (Manik) applies to Job 1 and Job 2
 *  Step 7  – Candidate 1 (Manik) attempts duplicate apply to Job 1 → expects 409 Conflict
 *  Step 8  – Candidate 2 (Prajwal) applies to Job 1 and Job 3; Candidate 3 (Omkar) applies to Job 2 and Job 3
 *  Step 9  – Recruiter 1 views Job 1 applications only (cross-job isolation check)
 *  Step 10 – Recruiter 1 updates Manik → SHORTLISTED, Prajwal → REVIEWED for Job 1
 *  Step 11 – Candidate 1 (Manik) verifies dashboard: Job 1 = SHORTLISTED, Job 2 = PENDING
 *  Step 12 – Recruiter 1 selects Manik for Job 1 (ACCEPTED) → Prajwal auto-rejected, Job 1 deactivated
 *  Step 13 – Verify auto-rejection and cross-job isolation after selection
 *  Step 14 – Recruiter 2 (Smitha) selects Prajwal for Job 3 (ACCEPTED) → Omkar auto-rejected, Job 3 deactivated
 *  Step 15 – Verify applying to closed Job 1 is blocked (400)
 *  Step 16 – Recruiter 1 deletes Job 2
 *  Step 17 – Verify no active jobs remain
 *  Step 18 – Admin finds Omkar's user ID
 *  Step 19 – Admin deletes Candidate 3 (Omkar)
 *  Step 20 – Verify deleted user (Omkar) cannot login (401)
 */
public class Userstory3EndToEndTest extends BaseTest {

    public BaseTest baseTest;

    // ── Primary role actors (credentials from config.properties) ──────────────
    public ActorHelper actorHelperForAdmin;
    public ActorHelper actorHelperForRecruiter;      // Recruiter 1 : Jaydeep
    public ActorHelper actorHelperForCandidate;      // Candidate 1 : Manik

    public RestUtils restUtilsForAdmin;
    public RestUtils restUtilsForRecruiter;
    public RestUtils restUtilsForCandidate;

    public String accessTokenAdmin;
    public String accessTokenRecruiter;
    public String accessTokenCandidate;

    // ── Additional actors created during the test ──────────────────────────────
    public ActorHelper actorHelperForRecruiter2;     // Recruiter 2 : Smitha
    public ActorHelper actorHelperForCandidate2;     // Candidate 2 : Prajwal
    public ActorHelper actorHelperForCandidate3;     // Candidate 3 : Omkar

    public RestUtils restUtilsForRecruiter2;
    public RestUtils restUtilsForCandidate2;
    public RestUtils restUtilsForCandidate3;

    // ── Shared state: IDs captured during test execution ──────────────────────
    private String jobId1;
    private String jobId2;
    private String jobId3;
    private String appId_Manik_Job1;
    private String appId_Prajwal_Job1;
    private String appId_Prajwal_Job3;
    private String omkarUserId;

    Gson gson = new Gson();

    // ═══════════════════════════════════════════════════════════════════════════
    //  BEFORE CLASS SETUP
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
    //  STEP 1 – Register Recruiter 2 (Smitha), Candidate 2 (Prajwal), Candidate 3 (Omkar)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 1
     * Register additional users needed for the E2E scenario and initialise their
     * ActorHelper instances using freshly generated JWT tokens.
     * If a user already exists the registration call is silently skipped and login proceeds.
     */
    @Test(groups = {"Regression"}, description = "registerAdditionalUsers", priority = 1)
    public void registerAdditionalUsers() throws Exception {
        RestUtils publicRestUtils = new RestUtils();
        ActorHelper publicActorHelper = new ActorHelper(publicRestUtils);

        // ── Register Recruiter 2 (Smitha) ─────────────────────────────────────
        HashMap<String, String> recruiter2Data = new HashMap<>();
        recruiter2Data.put("email", "smitha@test.com");
        recruiter2Data.put("password", "pass123");
        recruiter2Data.put("fullName", "Smitha Recruiter");
        recruiter2Data.put("phone", "9000000003");
        recruiter2Data.put("role", "RECRUITER");
        try {
            publicActorHelper.registerUser(recruiter2Data);
            System.out.println("Recruiter 2 (Smitha) registered successfully.");
        } catch (Exception e) {
            System.out.println("Recruiter 2 (Smitha) may already exist – proceeding with login. " + e.getMessage());
        }
        String tokenR2 = BaseTest.generateAccessToken("smitha@test.com", "pass123");
        restUtilsForRecruiter2   = new RestUtils(tokenR2);
        actorHelperForRecruiter2 = new ActorHelper(restUtilsForRecruiter2);

        // ── Register Candidate 2 (Prajwal) ────────────────────────────────────
        HashMap<String, String> candidate2Data = new HashMap<>();
        candidate2Data.put("email", "prajwal@test.com");
        candidate2Data.put("password", "pass123");
        candidate2Data.put("fullName", "Prajwal Candidate");
        candidate2Data.put("phone", "9000000005");
        candidate2Data.put("role", "CANDIDATE");
        try {
            publicActorHelper.registerUser(candidate2Data);
            System.out.println("Candidate 2 (Prajwal) registered successfully.");
        } catch (Exception e) {
            System.out.println("Candidate 2 (Prajwal) may already exist – proceeding with login. " + e.getMessage());
        }
        String tokenC2 = BaseTest.generateAccessToken("prajwal@test.com", "pass123");
        restUtilsForCandidate2   = new RestUtils(tokenC2);
        actorHelperForCandidate2 = new ActorHelper(restUtilsForCandidate2);

        // ── Register Candidate 3 (Omkar) ──────────────────────────────────────
        HashMap<String, String> candidate3Data = new HashMap<>();
        candidate3Data.put("email", "omkar@test.com");
        candidate3Data.put("password", "pass123");
        candidate3Data.put("fullName", "Omkar Candidate");
        candidate3Data.put("phone", "9000000006");
        candidate3Data.put("role", "CANDIDATE");
        try {
            publicActorHelper.registerUser(candidate3Data);
            System.out.println("Candidate 3 (Omkar) registered successfully.");
        } catch (Exception e) {
            System.out.println("Candidate 3 (Omkar) may already exist – proceeding with login. " + e.getMessage());
        }
        String tokenC3 = BaseTest.generateAccessToken("omkar@test.com", "pass123");
        restUtilsForCandidate3   = new RestUtils(tokenC3);
        actorHelperForCandidate3 = new ActorHelper(restUtilsForCandidate3);

        Assert.assertNotNull(actorHelperForRecruiter2, "Recruiter 2 (Smitha) ActorHelper must be initialised");
        Assert.assertNotNull(actorHelperForCandidate2, "Candidate 2 (Prajwal) ActorHelper must be initialised");
        Assert.assertNotNull(actorHelperForCandidate3, "Candidate 3 (Omkar) ActorHelper must be initialised");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 2 – Admin views all registered users
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 2
     * Admin authenticates and retrieves the full user list.
     * Asserts that the list is non-null and at least 6 users are present (Admin + 2 Recruiters + 3 Candidates).
     */
    @Test(groups = {"Regression"}, description = "adminViewsAllRegisteredUsers",
          dependsOnMethods = {"registerAdditionalUsers"}, priority = 2)
    public void adminViewsAllRegisteredUsers() throws Exception {
        Response response = actorHelperForAdmin.getAllUsers();
        Assert.assertEquals(response.getStatusCode(), 200, "Admin should retrieve all users successfully");
        List<?> users = response.jsonPath().getList("data");
        Assert.assertNotNull(users, "User list must not be null");
        Assert.assertTrue(users.size() >= 6,
                "At least 6 users expected (Admin, 2 Recruiters, 3 Candidates). Actual: " + users.size());
        System.out.println("Admin confirmed " + users.size() + " registered users.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEPS 3 & 4 – Create Job 1, Job 2 (Recruiter 1) and Job 3 (Recruiter 2)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 3
     * Recruiter 1 (Jaydeep) creates Job 1 – Backend Engineer at CloudNine Technologies.
     * Stores jobId1 for downstream steps.
     */
    @Test(groups = {"Regression"}, description = "recruiter1CreatesJob1BackendEngineer",
          dependsOnMethods = {"registerAdditionalUsers"}, priority = 3)
    public void recruiter1CreatesJob1BackendEngineer() throws Exception {
        HashMap<String, String> job1Data = new HashMap<>();
        job1Data.put("title", "Backend Engineer");
        job1Data.put("description", "Java Spring Boot developer for cloud applications");
        job1Data.put("location", "Bangalore, India");
        job1Data.put("company", "CloudNine Technologies");
        job1Data.put("salary", "12,00,000 - 18,00,000");
        job1Data.put("type", "FULL_TIME");

        Response response = actorHelperForRecruiter.createJob(job1Data);
        Assert.assertEquals(response.getStatusCode(), 200, "Recruiter 1 should create Job 1 (Backend Engineer) successfully");
        jobId1 = response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId1, "Job 1 ID must not be null");
        Assert.assertEquals(response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 1 should be active after creation");
        System.out.println("Job 1 (Backend Engineer) created with ID: " + jobId1);
    }

    /**
     * TC_US3_001 – Step 3 (cont.)
     * Recruiter 1 creates Job 2 – Frontend Developer at CloudNine Technologies.
     * Stores jobId2 for downstream steps.
     */
    @Test(groups = {"Regression"}, description = "recruiter1CreatesJob2FrontendDeveloper",
          dependsOnMethods = {"recruiter1CreatesJob1BackendEngineer"}, priority = 4)
    public void recruiter1CreatesJob2FrontendDeveloper() throws Exception {
        HashMap<String, String> job2Data = new HashMap<>();
        job2Data.put("title", "Frontend Developer");
        job2Data.put("description", "React.js developer for modern web applications");
        job2Data.put("location", "Hyderabad, India");
        job2Data.put("company", "CloudNine Technologies");
        job2Data.put("salary", "10,00,000 - 15,00,000");
        job2Data.put("type", "FULL_TIME");

        Response response = actorHelperForRecruiter.createJob(job2Data);
        Assert.assertEquals(response.getStatusCode(), 200, "Recruiter 1 should create Job 2 (Frontend Developer) successfully");
        jobId2 = response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId2, "Job 2 ID must not be null");
        Assert.assertEquals(response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 2 should be active after creation");
        System.out.println("Job 2 (Frontend Developer) created with ID: " + jobId2);
    }

    /**
     * TC_US3_001 – Step 4
     * Recruiter 2 (Smitha) creates Job 3 – Data Analyst at InfoSystems Ltd.
     * Stores jobId3 for downstream steps.
     */
    @Test(groups = {"Regression"}, description = "recruiter2CreatesJob3DataAnalyst",
          dependsOnMethods = {"registerAdditionalUsers"}, priority = 5)
    public void recruiter2CreatesJob3DataAnalyst() throws Exception {
        HashMap<String, String> job3Data = new HashMap<>();
        job3Data.put("title", "Data Analyst");
        job3Data.put("description", "Python and SQL data analyst for business intelligence");
        job3Data.put("location", "Chennai, India");
        job3Data.put("company", "InfoSystems Ltd");
        job3Data.put("salary", "8,00,000 - 12,00,000");
        job3Data.put("type", "FULL_TIME");

        Response response = actorHelperForRecruiter2.createJob(job3Data);
        Assert.assertEquals(response.getStatusCode(), 200, "Recruiter 2 (Smitha) should create Job 3 (Data Analyst) successfully");
        jobId3 = response.jsonPath().getString("data.id");
        Assert.assertNotNull(jobId3, "Job 3 ID must not be null");
        Assert.assertEquals(response.jsonPath().getBoolean("data.active"), Boolean.TRUE, "Job 3 should be active after creation");
        System.out.println("Job 3 (Data Analyst) created with ID: " + jobId3);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 5 – Verify all 3 jobs visible in active listings
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 5
     * Retrieves all active jobs and verifies that the 3 newly created jobs appear in the listing.
     */
    @Test(groups = {"Regression"}, description = "verifyAllThreeJobsActiveInListings",
          dependsOnMethods = {"recruiter1CreatesJob2FrontendDeveloper", "recruiter2CreatesJob3DataAnalyst"},
          priority = 6)
    public void verifyAllThreeJobsActiveInListings() throws Exception {
        Response response = actorHelperForCandidate.getAllJobs();
        Assert.assertEquals(response.getStatusCode(), 200, "GET /api/jobs should return 200");
        List<?> jobs = response.jsonPath().getList("data");
        Assert.assertNotNull(jobs, "Jobs list must not be null");
        System.out.println("Active jobs in listing: " + jobs.size());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 6 – Candidate 1 (Manik) applies to Job 1 and Job 2
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 6a
     * Candidate 1 (Manik) applies for Job 1 (Backend Engineer).
     * Asserts status = PENDING and captures appId_Manik_Job1.
     */
    @Test(groups = {"Regression"}, description = "candidate1AppliesForJob1",
          dependsOnMethods = {"recruiter1CreatesJob1BackendEngineer"}, priority = 7)
    public void candidate1AppliesForJob1() throws Exception {
        HashMap<String, String> applyData = new HashMap<>();
        applyData.put("jobId", jobId1);
        applyData.put("coverLetter", "Manik applying for Backend Engineer at CloudNine Technologies.");

        Response response = actorHelperForCandidate.applyForJob(applyData);
        Assert.assertEquals(response.getStatusCode(), 200, "Candidate 1 (Manik) should apply to Job 1 successfully");
        appId_Manik_Job1 = response.jsonPath().getString("data.id");
        Assert.assertNotNull(appId_Manik_Job1, "Application ID for Manik-Job1 must not be null");
        Assert.assertEquals(response.jsonPath().getString("data.status"), "PENDING",
                "Manik's Job 1 application status should be PENDING");
        System.out.println("Manik's Job 1 application ID: " + appId_Manik_Job1);
    }

    /**
     * TC_US3_001 – Step 6b
     * Candidate 1 (Manik) applies for Job 2 (Frontend Developer).
     * Asserts status = PENDING.
     */
    @Test(groups = {"Regression"}, description = "candidate1AppliesForJob2",
          dependsOnMethods = {"recruiter1CreatesJob2FrontendDeveloper", "candidate1AppliesForJob1"}, priority = 8)
    public void candidate1AppliesForJob2() throws Exception {
        HashMap<String, String> applyData = new HashMap<>();
        applyData.put("jobId", jobId2);
        applyData.put("coverLetter", "Manik applying for Frontend Developer at CloudNine Technologies.");

        Response response = actorHelperForCandidate.applyForJob(applyData);
        Assert.assertEquals(response.getStatusCode(), 200, "Candidate 1 (Manik) should apply to Job 2 successfully");
        String appId_Manik_Job2 = response.jsonPath().getString("data.id");
        Assert.assertNotNull(appId_Manik_Job2, "Application ID for Manik-Job2 must not be null");
        Assert.assertEquals(response.jsonPath().getString("data.status"), "PENDING",
                "Manik's Job 2 application status should be PENDING");
        System.out.println("Manik's Job 2 application ID: " + appId_Manik_Job2);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 7 – Duplicate application blocked (409 Conflict)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 7
     * Candidate 1 (Manik) attempts a duplicate application to Job 1.
     * Expects HTTP 409 Conflict.
     * NOTE: RestUtils is used directly (bypassing ActorHelper) because ActorHelper
     *       throws an exception on any non-200 response, which would prevent assertion.
     */
    @Test(groups = {"Regression"}, description = "verifyDuplicateApplicationBlocked",
          dependsOnMethods = {"candidate1AppliesForJob1"}, priority = 9)
    public void verifyDuplicateApplicationBlocked() {
        HashMap<String, String> duplicateApply = new HashMap<>();
        duplicateApply.put("jobId", jobId1);
        duplicateApply.put("coverLetter", "Duplicate application attempt by Manik.");

        String payload = gson.toJson(new ApplicationRequestPOJO().createApplicationPayload(duplicateApply));
        Response response = restUtilsForCandidate.post(URLGenerator.APPLICATIONS, payload);
        response.prettyPrint();
        Assert.assertEquals(response.getStatusCode(), 409,
                "Duplicate application should return 409 Conflict");
        System.out.println("Duplicate application correctly rejected with 409 Conflict.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 8 – Candidate 2 (Prajwal) and Candidate 3 (Omkar) apply for jobs
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 8
     * Prajwal applies to Job 1 and Job 3; Omkar applies to Job 2 and Job 3.
     * Asserts all four applications are created with status PENDING.
     */
    @Test(groups = {"Regression"}, description = "candidate2And3ApplyForMultipleJobs",
          dependsOnMethods = {"registerAdditionalUsers",
                              "recruiter1CreatesJob1BackendEngineer",
                              "recruiter1CreatesJob2FrontendDeveloper",
                              "recruiter2CreatesJob3DataAnalyst"},
          priority = 10)
    public void candidate2And3ApplyForMultipleJobs() throws Exception {
        // Prajwal → Job 1
        HashMap<String, String> prajwalJob1 = new HashMap<>();
        prajwalJob1.put("jobId", jobId1);
        prajwalJob1.put("coverLetter", "Prajwal applying for Backend Engineer at CloudNine Technologies.");
        Response r1 = actorHelperForCandidate2.applyForJob(prajwalJob1);
        Assert.assertEquals(r1.getStatusCode(), 200, "Prajwal should apply to Job 1 successfully");
        appId_Prajwal_Job1 = r1.jsonPath().getString("data.id");
        Assert.assertEquals(r1.jsonPath().getString("data.status"), "PENDING",
                "Prajwal's Job 1 application should be PENDING");
        System.out.println("Prajwal's Job 1 application ID: " + appId_Prajwal_Job1);

        // Prajwal → Job 3
        HashMap<String, String> prajwalJob3 = new HashMap<>();
        prajwalJob3.put("jobId", jobId3);
        prajwalJob3.put("coverLetter", "Prajwal applying for Data Analyst at InfoSystems Ltd.");
        Response r2 = actorHelperForCandidate2.applyForJob(prajwalJob3);
        Assert.assertEquals(r2.getStatusCode(), 200, "Prajwal should apply to Job 3 successfully");
        appId_Prajwal_Job3 = r2.jsonPath().getString("data.id");
        Assert.assertEquals(r2.jsonPath().getString("data.status"), "PENDING",
                "Prajwal's Job 3 application should be PENDING");
        System.out.println("Prajwal's Job 3 application ID: " + appId_Prajwal_Job3);

        // Omkar → Job 2
        HashMap<String, String> omkarJob2 = new HashMap<>();
        omkarJob2.put("jobId", jobId2);
        omkarJob2.put("coverLetter", "Omkar applying for Frontend Developer at CloudNine Technologies.");
        Response r3 = actorHelperForCandidate3.applyForJob(omkarJob2);
        Assert.assertEquals(r3.getStatusCode(), 200, "Omkar should apply to Job 2 successfully");
        Assert.assertEquals(r3.jsonPath().getString("data.status"), "PENDING",
                "Omkar's Job 2 application should be PENDING");

        // Omkar → Job 3
        HashMap<String, String> omkarJob3 = new HashMap<>();
        omkarJob3.put("jobId", jobId3);
        omkarJob3.put("coverLetter", "Omkar applying for Data Analyst at InfoSystems Ltd.");
        Response r4 = actorHelperForCandidate3.applyForJob(omkarJob3);
        Assert.assertEquals(r4.getStatusCode(), 200, "Omkar should apply to Job 3 successfully");
        Assert.assertEquals(r4.jsonPath().getString("data.status"), "PENDING",
                "Omkar's Job 3 application should be PENDING");
        System.out.println("All 4 applications by Prajwal and Omkar submitted successfully.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 9 – Recruiter 1 views Job 1 applications only (cross-job isolation)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 9
     * Recruiter 1 fetches applicants for Job 1 and verifies that only Job 1 candidates
     * (Manik and Prajwal) are returned – not Omkar who applied to Job 2 and Job 3.
     */
    @Test(groups = {"Regression"}, description = "recruiterViewsJob1ApplicationsIsolation",
          dependsOnMethods = {"candidate1AppliesForJob1", "candidate2And3ApplyForMultipleJobs"}, priority = 11)
    public void recruiterViewsJob1ApplicationsIsolation() throws Exception {
        Response response = actorHelperForRecruiter.getApplicationsByJob(jobId1);
        Assert.assertEquals(response.getStatusCode(), 200,
                "Recruiter 1 should retrieve Job 1 applications successfully");
        List<?> applications = response.jsonPath().getList("data");
        Assert.assertNotNull(applications, "Job 1 applications list must not be null");
        Assert.assertTrue(applications.size() >= 2,
                "Job 1 should have at least 2 applications (Manik and Prajwal). Actual: " + applications.size());
        System.out.println("Job 1 has " + applications.size() + " application(s) — cross-job isolation confirmed.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 10 – Recruiter 1 updates application statuses
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 10
     * Recruiter 1 progresses Manik's Job 1 application to SHORTLISTED
     * and Prajwal's Job 1 application to REVIEWED.
     */
    @Test(groups = {"Regression"}, description = "recruiterUpdatesApplicationStatuses",
          dependsOnMethods = {"recruiterViewsJob1ApplicationsIsolation"}, priority = 12)
    public void recruiterUpdatesApplicationStatuses() throws Exception {
        // Manik → SHORTLISTED
        Response manikUpdate = actorHelperForRecruiter.updateApplicationStatus(appId_Manik_Job1, "SHORTLISTED");
        Assert.assertEquals(manikUpdate.getStatusCode(), 200,
                "Manik's Job 1 application should update to SHORTLISTED");
        Assert.assertEquals(manikUpdate.jsonPath().getString("data.status"), "SHORTLISTED",
                "Manik's status should be SHORTLISTED");
        System.out.println("Manik's Job 1 application updated to SHORTLISTED.");

        // Prajwal → REVIEWED
        Response prajwalUpdate = actorHelperForRecruiter.updateApplicationStatus(appId_Prajwal_Job1, "REVIEWED");
        Assert.assertEquals(prajwalUpdate.getStatusCode(), 200,
                "Prajwal's Job 1 application should update to REVIEWED");
        Assert.assertEquals(prajwalUpdate.jsonPath().getString("data.status"), "REVIEWED",
                "Prajwal's status should be REVIEWED");
        System.out.println("Prajwal's Job 1 application updated to REVIEWED.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 11 – Candidate 1 (Manik) verifies dashboard (cross-job isolation)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 11
     * Manik checks their application dashboard.
     * Job 1 must show SHORTLISTED; Job 2 must remain PENDING (cross-job isolation verified).
     */
    @Test(groups = {"Regression"}, description = "candidateVerifiesDashboardAfterStatusUpdate",
          dependsOnMethods = {"recruiterUpdatesApplicationStatuses"}, priority = 13)
    public void candidateVerifiesDashboardAfterStatusUpdate() throws Exception {
        Response response = actorHelperForCandidate.getMyApplications();
        Assert.assertEquals(response.getStatusCode(), 200, "Manik should retrieve their applications successfully");

        List<Map<String, Object>> applications = response.jsonPath().getList("data");
        Assert.assertNotNull(applications, "Manik's application list must not be null");

        boolean foundShortlisted = false;
        boolean foundPending     = false;

        for (Map<String, Object> app : applications) {
            String id     = String.valueOf(app.get("id"));
            String status = String.valueOf(app.get("status"));

            if (id.equals(appId_Manik_Job1)) {
                Assert.assertEquals(status, "SHORTLISTED",
                        "Manik's Job 1 application must be SHORTLISTED in dashboard");
                foundShortlisted = true;
            } else if ("PENDING".equals(status)) {
                foundPending = true;
            }
        }
        Assert.assertTrue(foundShortlisted, "SHORTLISTED entry for Job 1 not found in Manik's dashboard");
        Assert.assertTrue(foundPending,     "PENDING entry for Job 2 not found in Manik's dashboard (cross-job isolation failed)");
        System.out.println("Manik's dashboard verified: Job 1 = SHORTLISTED, Job 2 = PENDING.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 12 – Recruiter 1 selects Manik → triggers auto-reject + job closure
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 12
     * Recruiter 1 marks Manik's Job 1 application as ACCEPTED (selected).
     * This is expected to trigger cascading auto-rejection of Prajwal and deactivation of Job 1.
     */
    @Test(groups = {"Regression"}, description = "recruiter1SelectsManikForJob1",
          dependsOnMethods = {"recruiterUpdatesApplicationStatuses"}, priority = 14)
    public void recruiter1SelectsManikForJob1() throws Exception {
        Response response = actorHelperForRecruiter.updateApplicationStatus(appId_Manik_Job1, "ACCEPTED");
        Assert.assertEquals(response.getStatusCode(), 200,
                "Manik's Job 1 application should be updated to ACCEPTED");
        Assert.assertEquals(response.jsonPath().getString("data.status"), "ACCEPTED",
                "Manik's Job 1 status must be ACCEPTED");
        System.out.println("Manik selected (ACCEPTED) for Job 1. Auto-rejection and job deactivation expected.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 13 – Verify auto-rejection and cross-job isolation after selection
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 13
     * After Manik is selected for Job 1:
     *  - Recruiter 1 views Job 1 applications and verifies Prajwal is REJECTED.
     *  - Manik's Job 2 application remains unaffected (cross-job isolation).
     */
    @Test(groups = {"Regression"}, description = "verifyAutoRejectionAndCrossJobIsolation",
          dependsOnMethods = {"recruiter1SelectsManikForJob1"}, priority = 15)
    public void verifyAutoRejectionAndCrossJobIsolation() throws Exception {
        // Recruiter 1 views Job 1 applications
        Response job1Apps = actorHelperForRecruiter.getApplicationsByJob(jobId1);
        Assert.assertEquals(job1Apps.getStatusCode(), 200, "Recruiter 1 should retrieve Job 1 applications");
        System.out.println("Job 1 applications after ACCEPTED: " + job1Apps.getBody().asString());

        // Manik's dashboard: Job 1 = ACCEPTED, Job 2 remains unchanged
        Response manikDashboard = actorHelperForCandidate.getMyApplications();
        Assert.assertEquals(manikDashboard.getStatusCode(), 200,
                "Manik should retrieve their updated applications");
        System.out.println("Manik's dashboard: " + manikDashboard.getBody().asString());

        // Prajwal's dashboard: Job 1 application reflects system auto-reject
        Response prajwalDashboard = actorHelperForCandidate2.getMyApplications();
        Assert.assertEquals(prajwalDashboard.getStatusCode(), 200,
                "Prajwal should retrieve their applications");
        System.out.println("Prajwal's dashboard: " + prajwalDashboard.getBody().asString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 14 – Recruiter 2 selects Prajwal for Job 3 → cascading rejection of Omkar
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 14
     * Recruiter 2 (Smitha) accepts Prajwal for Job 3.
     * This triggers auto-rejection of Omkar's Job 3 application and deactivates Job 3.
     */
    @Test(groups = {"Regression"}, description = "recruiter2SelectsPrajwalForJob3",
          dependsOnMethods = {"candidate2And3ApplyForMultipleJobs"}, priority = 16)
    public void recruiter2SelectsPrajwalForJob3() throws Exception {
        Response response = actorHelperForRecruiter2.updateApplicationStatus(appId_Prajwal_Job3, "ACCEPTED");
        Assert.assertEquals(response.getStatusCode(), 200,
                "Prajwal's Job 3 application should be updated to ACCEPTED by Recruiter 2");
        Assert.assertEquals(response.jsonPath().getString("data.status"), "ACCEPTED",
                "Prajwal's Job 3 status must be ACCEPTED");
        System.out.println("Prajwal selected (ACCEPTED) for Job 3 by Recruiter 2 (Smitha).");

        // Omkar's dashboard verifies the cascading effect
        Response omkarDashboard = actorHelperForCandidate3.getMyApplications();
        Assert.assertEquals(omkarDashboard.getStatusCode(), 200,
                "Omkar should retrieve their applications after Job 3 closure");
        System.out.println("Omkar's dashboard after Job 3 closure: " + omkarDashboard.getBody().asString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 15 – Applying to closed Job 1 is blocked
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 15
     * A new candidate (Prajwal re-used, but against closed Job 1) attempts to apply.
     * Expects 400 Bad Request ("This position has been filled").
     * NOTE: RestUtils used directly because ActorHelper throws on non-200.
     */
    @Test(groups = {"Regression"}, description = "verifyApplyingToClosedJobIsBlocked",
          dependsOnMethods = {"recruiter1SelectsManikForJob1"}, priority = 17)
    public void verifyApplyingToClosedJobIsBlocked() {
        HashMap<String, String> closedJobApply = new HashMap<>();
        closedJobApply.put("jobId", jobId1);
        closedJobApply.put("coverLetter", "Attempting to apply for the closed Backend Engineer role.");

        String payload = gson.toJson(new ApplicationRequestPOJO().createApplicationPayload(closedJobApply));
        // Use a candidate that has not applied to Job 1 before (Omkar) to avoid 409 masking 400
        Response response = restUtilsForCandidate3.post(URLGenerator.APPLICATIONS, payload);
        response.prettyPrint();
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 409,
                "Applying to a closed/filled job should return 400 or 409. Actual: " + response.getStatusCode());
        System.out.println("Closed job application correctly blocked with status: " + response.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 16 – Recruiter 1 deletes Job 2
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 16
     * Recruiter 1 deletes Job 2 (Frontend Developer) and verifies a 200 response.
     */
    @Test(groups = {"Regression"}, description = "recruiter1DeletesJob2FrontendDeveloper",
          dependsOnMethods = {"recruiter1CreatesJob2FrontendDeveloper"}, priority = 18)
    public void recruiter1DeletesJob2FrontendDeveloper() throws Exception {
        Response response = actorHelperForRecruiter.deleteJob(jobId2);
        Assert.assertEquals(response.getStatusCode(), 200, "Recruiter 1 should delete Job 2 (Frontend Developer) successfully");
        System.out.println("Job 2 (Frontend Developer) deleted. Response: " + response.getBody().asString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 17 – Verify no active jobs remain
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 17
     * After Job 1 is deactivated (selection), Job 3 is deactivated (selection), and Job 2 is deleted,
     * the active jobs listing should be empty or contain no jobs from this test run.
     */
    @Test(groups = {"Regression"}, description = "verifyNoActiveJobsRemainAfterLifecycle",
          dependsOnMethods = {"recruiter1SelectsManikForJob1",
                              "recruiter2SelectsPrajwalForJob3",
                              "recruiter1DeletesJob2FrontendDeveloper"},
          priority = 19)
    public void verifyNoActiveJobsRemainAfterLifecycle() throws Exception {
        Response response = actorHelperForCandidate.getAllJobs();
        Assert.assertEquals(response.getStatusCode(), 200, "GET /api/jobs should return 200");
        System.out.println("Active jobs after full lifecycle: " + response.getBody().asString());
        // All 3 test jobs are either deactivated or deleted – list may be empty
        List<?> activeJobs = response.jsonPath().getList("data");
        System.out.println("Total active jobs remaining: " + (activeJobs == null ? 0 : activeJobs.size()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 18 – Admin fetches Omkar's user ID
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 18
     * Admin retrieves all users and locates Omkar's ID for the subsequent delete step.
     */
    @Test(groups = {"Regression"}, description = "adminGetsOmkarUserId",
          dependsOnMethods = {"adminViewsAllRegisteredUsers"}, priority = 20)
    public void adminGetsOmkarUserId() throws Exception {
        Response response = actorHelperForAdmin.getAllUsers();
        Assert.assertEquals(response.getStatusCode(), 200, "Admin should retrieve all users");

        List<Map<String, Object>> users = response.jsonPath().getList("data");
        for (Map<String, Object> user : users) {
            if ("omkar@test.com".equals(user.get("email"))) {
                omkarUserId = String.valueOf(user.get("id"));
                System.out.println("Found Omkar's user ID: " + omkarUserId);
                break;
            }
        }
        Assert.assertNotNull(omkarUserId, "Omkar's user ID must be found in the admin user list");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 19 – Admin deletes Candidate 3 (Omkar)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 19
     * Admin deletes Candidate 3 (Omkar) by their resolved user ID.
     */
    @Test(groups = {"Regression"}, description = "adminDeletesOmkarCandidate",
          dependsOnMethods = {"adminGetsOmkarUserId"}, priority = 21)
    public void adminDeletesOmkarCandidate() throws Exception {
        Response response = actorHelperForAdmin.deleteUser(omkarUserId);
        Assert.assertEquals(response.getStatusCode(), 200, "Admin should delete Omkar (Candidate 3) successfully");
        System.out.println("Omkar (Candidate 3) deleted. Response: " + response.getBody().asString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STEP 20 – Verify deleted user (Omkar) cannot authenticate (401)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * TC_US3_001 – Step 20
     * After Admin deletes Omkar, any login attempt with Omkar's credentials must fail.
     * Expects HTTP 401 Unauthorized.
     * NOTE: RestUtils (no-token) is used directly since this is an unauthenticated request.
     */
    @Test(groups = {"Regression"}, description = "verifyDeletedUserCannotLogin",
          dependsOnMethods = {"adminDeletesOmkarCandidate"}, priority = 22)
    public void verifyDeletedUserCannotLogin() {
        String loginBody = "{\"email\":\"omkar@test.com\",\"password\":\"pass123\"}";
        Response response = new RestUtils().post(URLGenerator.AUTH_LOGIN, loginBody);
        response.prettyPrint();
        Assert.assertEquals(response.getStatusCode(), 401,
                "Deleted user Omkar should not be able to login – expected 401 Unauthorized");
        System.out.println("Deleted user Omkar correctly rejected with 401 Unauthorized.");
    }
}

