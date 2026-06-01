# Skill: Requirement Understanding

## Purpose
Read and analyze the user story, then break it down into **high-level, business-focused API requirements** and test scenarios. Output only test cases directly tied to the user story's acceptance criteria — do NOT include generic infrastructure tests like invalid login, wrong password, or missing fields for auth endpoints.

---

## Instructions

### 1. Read the User Story
- Read the user story provided by the user (file path, pasted text, or attached document).
- Extract only the **functional acceptance criteria** that describe what the user wants to achieve.
- Identify all actors (e.g., Admin, Recruiter, Candidate) and their permitted actions.
- **Ignore** generic pre-conditions like "user must be logged in" — assume authentication is already handled by the test setup (BaseTest).

---

### 2. Think from a High-Level API Perspective
For each **business action** described in the user story, determine:
- What **HTTP method** is needed (GET, POST, PUT, DELETE)?
- What **endpoint** will be called?
- What **request payload** is required?
- What **response** is expected (status code + business data)?
- What **pre-conditions** are specific to this story (not generic auth)?

> **Focus on what the user story says** — not on generic infrastructure concerns.

---

### 3. Derive Payload from POJOs
Navigate to `src/main/java/com/hiring/pojo/` and inspect the relevant POJO:

| POJO Class | Purpose |
|---|---|
| `ApplicationRequestPOJO.java` | Payload for applying to a job |
| `CandidatePOJO.java` | Candidate profile data |
| `JobRequestPOJO.java` | Payload for creating/updating a job |
| `LoginRequestPOJO.java` | Payload for user login |
| `RegisterRequestPOJO.java` | Payload for user registration |
| `UserProfileRequestPOJO.java` | Payload for updating user profile |

Map each user story action to the corresponding POJO to get exact field names — do NOT guess.

---

### 4. High-Level Test Case Categories (only these)

| Category | Include? | Description |
|----------|----------|-------------|
| **Happy Path** | ✅ Always | The main success flow described in the user story |
| **Business Negative** | ✅ Yes | Failures relevant to the story (e.g., apply for non-existent job, duplicate apply → 409) |
| **Authorization (role-based)** | ✅ If story mentions roles | e.g., Recruiter cannot apply, Candidate cannot post jobs |
| **E2E Flow** | ✅ Always | Comprehensive multi-step test covering the full user story — see Section 4a |
| **Invalid credentials** | ❌ Exclude | Login with wrong password, wrong email — NOT part of the user story |
| **Missing mandatory fields** | ❌ Exclude | Generic field validation — NOT part of the user story |
| **Auth token missing** | ❌ Exclude | Generic 401 tests — NOT part of the user story |

---

### 4a. E2E Test Case — Generation Mode Rules

The number of E2E test cases generated depends on **what the user asks for**:

---

#### Mode 1 — "Create high-level test cases" / "Create detailed E2E" / "One test case"
> Generate **exactly 1 E2E test case** that covers the complete user story end-to-end.

- Each step = one full business action (login → search → extract → apply → verify)
- Covers every AC in sequence in a single flow
- Named plainly: e.g., `E2E: Search, Apply and View Dashboard`
- ❌ Do NOT split into per-AC test cases

---

#### Mode 2 — "Cover all scenarios" / "Complex scenarios" / "All possible test cases"
> Generate **multiple high-level test cases** — one per AC + one E2E at the end.

- Each AC gets its own test case (e.g., Apply by Company, Apply by Title, Apply by Location, Apply by Salary)
- Each AC test case follows the **full apply flow**: login → search by that parameter → extract jobId → apply → verify dashboard
- Business negative cases included: duplicate apply → 409, apply for non-existent job → 404
- The **last test case is always E2E** — a single flow that chains all ACs together
- All test cases remain **high-level** — no field-level assertions

---

**E2E Step Structure (reference for US-HIREFLOW-001):**
```
Step 1: Login                                  → POST /api/auth/login
Step 2: Search jobs by parameter (AC-1..AC-4)  → GET  /api/jobs?<param>=...
Step 3: Extract jobId from search response     → (data extraction)
Step 4: Apply for the job                      → POST /api/applications
Step 5: Verify application in dashboard        → GET  /api/applications/my
```

---

### 5. Example — "Apply for a Job at CloudNine, Full Stack Developer, Bangalore, highest salary"

**What to include:**
- ✅ Search jobs filtered by company=CloudNine Technologies, title=Full Stack Developer, location=Bangalore, sorted by salary desc → 200 OK
- ✅ Apply for the top result → 201 Created, applicationId + status=SUBMITTED
- ✅ Apply for a job that is not in Bangalore (wrong location filter) → 200 OK empty array
- ✅ Apply for same job twice → 409 Conflict (business rule)
- ✅ View dashboard → applications list with jobName, title, companyName, status, appliedOn
- ✅ E2E: Search → Apply → View dashboard

**What to exclude:**
- ❌ Login with wrong password
- ❌ Register with duplicate email
- ❌ Send request without Authorization header → 401
- ❌ Missing required fields in request body → 400

---

### 6. Output Format
For each identified requirement, produce:

```
### REQ-XXX: [Requirement Title]
- **Actor**: [Admin / Recruiter / Candidate]
- **Action**: [What the user does — directly from the story]
- **API Endpoint**: [HTTP Method] /api/endpoint
- **Auth Required**: [Role / Token type]
- **Request Payload (from POJO)**: [POJO class name + key fields]
- **Expected Response**: [Status code + key business response fields]
- **Pre-conditions**: [Business pre-conditions only — not generic auth]
- **Acceptance Criteria**: [From user story]
- **Test Scenarios** (high-level only):
  - Happy path: ...
  - Business negative: ...
  - E2E step: ...
```

---

## Rules
- ✅ Always read the user-provided user story first.
- ✅ Always reference POJO classes for payload fields — never guess field names.
- ✅ Only generate test cases that are **directly described in or derived from the user story**.
- ✅ **Always include at least ONE E2E test case** as the last test case — covers all ACs in sequence.
- ✅ The E2E test case must be **high-level** — one step per business action, not per field or assertion.
- ✅ **If user asks for "high-level" or "one test case" → generate 1 E2E test case only.**
- ✅ **If user asks for "all scenarios", "complex scenarios", or "all possible test cases" → generate one test case per AC + business negatives + 1 E2E at the end.**
- ✅ All generated test cases (even in multi-mode) must remain **high-level positive flows** — no field-level assertion tests.
- ✅ Identify API dependencies (e.g., need jobId from search before applying).
- ❌ Do NOT generate: login with invalid password, register with duplicate email, missing auth token, missing mandatory fields.
- ❌ Do NOT generate test cases for features NOT mentioned in the user story.
- If the user story is ambiguous, list assumptions clearly.
