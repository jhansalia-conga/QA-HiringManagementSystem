# Skill: Test Case Generation

## Purpose
For every new user story received from `requirement-understanding`, this skill **creates a new JSON file** containing all test cases, then auto-generates the Excel file from it. The JSON file name must match the user story name exactly.

---

## Workflow

```
User Story Input
      ↓
[Step 1] requirement-understanding/SKILL.md → extract ACs + API contracts
      ↓
[Step 2] THIS SKILL → create <UserStoryName>.json → generate <UserStoryName>.xlsx
```

---

## Instructions

### 0. Read Java Source Classes First (MANDATORY)

Before generating any test case, **read every class** inside `src/main/java/com/hiring/` to extract the real API contract:

| Package | Class | What to extract |
|---------|-------|-----------------|
| `utils/` | `URLGenerator.java` | All endpoint constants → use as URLs in steps |
| `pojo/` | `*RequestPOJO.java` | All private fields → use as request body keys |
| `response/` | `*Response.java` | All private fields → use as response body keys |
| `helpers/` | `ActorHelper.java` | Method ↔ endpoint mapping + HTTP verb |

#### Extracted API Contract Reference

Use the table below as the **single source of truth** for all URLs, request payloads, and response shapes.

---

##### Authentication

| Method | URL | POJO / Fields | Notes |
|--------|-----|---------------|-------|
| `POST` | `http://localhost:5000/api/auth/register` | `RegisterRequestPOJO`: `email`, `password`, `fullName`, `phone`, `role` | CANDIDATE or RECRUITER |
| `POST` | `http://localhost:5000/api/auth/login` | `LoginRequestPOJO`: `email`, `password` | Returns `accessToken` |

**Sample login request (RegisterRequestPOJO):**
```json
{
  "email": "johndoe@test.com",
  "password": "Pass@123",
  "fullName": "John Doe",
  "phone": "9876543210",
  "role": "CANDIDATE"
}
```
**Sample login request (LoginRequestPOJO):**
```json
{
  "email": "johndoe@test.com",
  "password": "Pass@123"
}
```
**Sample login response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "id": 2,
    "email": "johndoe@test.com",
    "fullName": "John Doe",
    "role": "CANDIDATE"
  }
}
```

---

##### Jobs

| Method | URL | POJO / Fields | Notes |
|--------|-----|---------------|-------|
| `POST` | `http://localhost:5000/api/jobs` | `JobRequestPOJO`: `title`, `description`, `location`, `company`, `salary`, `type` | Recruiter only |
| `GET` | `http://localhost:5000/api/jobs` | — (no body) | List all active jobs |
| `GET` | `http://localhost:5000/api/jobs/{id}` | — (no body) | Get job by ID |
| `PUT` | `http://localhost:5000/api/jobs/{id}` | `JobRequestPOJO`: `title`, `description`, `location`, `company`, `salary`, `type` | Recruiter only |
| `DELETE` | `http://localhost:5000/api/jobs/{id}` | — (no body) | Recruiter only |

**Sample job request (JobRequestPOJO):**
```json
{
  "title": "Software Developer",
  "description": "Java backend developer role",
  "location": "Bangalore, India",
  "company": "TechCorp Solutions",
  "salary": "8,00,000 - 15,00,000",
  "type": "FULL_TIME"
}
```
**Sample GET /api/jobs response:**
```json
{
  "success": true,
  "message": "Jobs retrieved",
  "data": [
    {
      "id": 1,
      "title": "Software Developer",
      "description": "Java backend developer role",
      "location": "Bangalore, India",
      "company": "TechCorp Solutions",
      "salary": "8,00,000 - 15,00,000",
      "type": "FULL_TIME",
      "active": true
    }
  ]
}
```

---

##### Applications

| Method | URL | POJO / Fields | Notes |
|--------|-----|---------------|-------|
| `POST` | `http://localhost:5000/api/applications` | `ApplicationRequestPOJO`: `jobId` (int), `coverLetter` | Candidate only |
| `GET` | `http://localhost:5000/api/applications/my` | — (no body) | Candidate's own applications |
| `GET` | `http://localhost:5000/api/applications/job/{jobId}` | — (no body) | Recruiter views applicants for a job |
| `PUT` | `http://localhost:5000/api/applications/{id}/status?status=SHORTLISTED` | query param: `status` | Status values: PENDING, REVIEWED, SHORTLISTED, REJECTED, ACCEPTED |

**Sample application request (ApplicationRequestPOJO):**
```json
{
  "jobId": 1,
  "coverLetter": "I am excited to apply for this role."
}
```
**Sample POST /api/applications response (ApplicationResponse fields: id, jobId, coverLetter, status, applicantEmail):**
```json
{
  "success": true,
  "message": "Application submitted",
  "data": {
    "id": 9,
    "job": {
      "id": 1,
      "title": "Software Developer",
      "location": "Bangalore, India",
      "company": "TechCorp Solutions",
      "salary": "8,00,000 - 15,00,000",
      "type": "FULL_TIME",
      "active": true
    },
    "candidate": {
      "id": 2,
      "email": "johndoe@test.com",
      "fullName": "John Doe",
      "role": "CANDIDATE"
    },
    "status": "PENDING",
    "coverLetter": "I am excited to apply for this role.",
    "appliedAt": "2026-05-28T21:49:36.493"
  }
}
```

---

##### User Profile

| Method | URL | POJO / Fields | Notes |
|--------|-----|---------------|-------|
| `GET` | `http://localhost:5000/api/users/profile` | — (no body) | Get current user profile |
| `PUT` | `http://localhost:5000/api/users/profile` | `UserProfileRequestPOJO`: `fullName`, `phone`, `skills`, `experience` | Update profile |

**Sample profile request (UserProfileRequestPOJO):**
```json
{
  "fullName": "John Doe",
  "phone": "9876543210",
  "skills": "Java, REST API, TestNG",
  "experience": "3 years"
}
```
**Sample GET /api/users/profile response (UserProfileResponse fields: id, email, fullName, phone, role, skills, experience):**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "email": "johndoe@test.com",
    "fullName": "John Doe",
    "phone": "9876543210",
    "role": "CANDIDATE",
    "skills": "Java, REST API, TestNG",
    "experience": "3 years"
  }
}
```

---

##### File Upload & Admin

| Method | URL | Notes |
|--------|-----|-------|
| `POST` | `http://localhost:5000/api/upload/resume` | Multipart form-data, field name: `resume` |
| `GET` | `http://localhost:5000/api/admin/users` | Admin only — list all users |
| `DELETE` | `http://localhost:5000/api/admin/users/{id}` | Admin only — delete user by ID |

---

> ✅ **Every time you generate test cases, re-read the class files** to pick up any newly added endpoints or fields.
> ✅ If a new POJO or endpoint is found that is not in the table above, add it to your test cases.

---

### 1. Read Requirements First
- Always read `.github/skills/requirement-understanding/SKILL.md` output first.
- Every acceptance criteria (AC) must map to at least one test case in the JSON.

---

### 2. Create the JSON File

Before creating the JSON file, **ensure the `data/` directory exists**. If it does not exist, create it:

```powershell
New-Item -ItemType Directory -Force "src\main\resources\TestCases\data"
```

Then create the JSON file at:
```
src/main/resources/TestCases/data/<UserStoryName>.json
```

> **The file name MUST be identical to the user story name** (no spaces, same case).
> **Always use `create_file` tool** (or equivalent) to write the JSON — this ensures the file and any missing parent directories are created automatically.

---

### 3. JSON Schema

```json
[
  {
    "slNo": "1",
    "testCaseId": "TC_XXX_001",
    "testCaseName": "Plain action-based name",
    "testCaseDescription": "Full sentence describing what is verified",
    "updateResponseBody": "Optional: prettified JSON to patch into Response Body after generation",
    "steps": [
      {
        "step": "Step description — action + endpoint (e.g. Login: POST http://localhost:5000/api/auth/login)",
        "requestBody": "{ \"email\": \"user@test.com\", \"password\": \"Pass@123\" }",
        "responseBody": "{\n  \"success\": true,\n  \"accessToken\": \"eyJhbGci...\"\n}"
      }
    ]
  }
]
```

---

### 4. Step Format — Critical Rules

Each step MUST follow this pattern:

| Field | What to put |
|-------|-------------|
| `step` | Plain English action + HTTP method + full API URL. e.g. `Login: POST http://localhost:5000/api/auth/login` or `Get list of available jobs: GET http://localhost:5000/api/jobs` |
| `requestBody` | **Prettified JSON payload** (use 2-space indented format). Use `""` if no body (e.g. GET requests). For GET, put the full URL with query params here if relevant. |
| `responseBody` | **Full prettified JSON response** matching real API output — include nested objects (job, candidate, status, appliedAt etc.). Do NOT use just "200 OK". |

> ✅ Use the base URL: `http://localhost:5000`
> ✅ Request/Response bodies must be **prettified JSON** (2-space indented)
> ✅ Responses must reflect **realistic API output** based on POJO structure

---

### 5. Sample Test Case (Reference — follow this style)

```
TC_HMS_001 | Apply for a Job Based on Location
Steps:
  1. Login: POST http://localhost:5000/api/auth/login
     Request:  { "fullName": "John Doe", "email": "johndoe@test.com", "password": "Pass@123", "phone": "9876543210", "role": "CANDIDATE" }
     Response: { "success": true, "accessToken": "eyJhbGci..." }

  2. Get list of available jobs: GET http://localhost:5000/api/jobs
     Request:  http://localhost:5000/api/jobs
     Response: { "success": true, "data": [ { "id": 1, "title": "Software Developer", "location": "Bangalore, India", ... } ] }

  3. Get the jobId of the job based on location (Bangalore) from step 2 response
     Request:  ""
     Response: jobId = 1 (extracted from GET /api/jobs where location = Bangalore)

  4. Apply for the job: POST http://localhost:5000/api/applications
     Request:  { "jobId": 1, "coverLetter": "Test" }
     Response: {
                 "success": true,
                 "message": "Application submitted",
                 "data": {
                   "id": 9,
                   "job": { "id": 1, "title": "Software Developer / Software Engineer", "location": "Bangalore, India", "company": "TechCorp Solutions", "salary": "8,00,000 - 15,00,000", "type": "FULL_TIME", "active": true },
                   "candidate": { "id": 2, "email": "johndoe@test.com", "fullName": "John Doe", "role": "CANDIDATE" },
                   "status": "PENDING",
                   "coverLetter": "Test",
                   "appliedAt": "2026-05-28T21:49:36.493"
                 }
               }

  5. Verify the status of the applied job: GET http://localhost:5000/api/applications
     Request:  ""
     Response: {
                 "success": true,
                 "message": "Applications retrieved",
                 "data": [
                   {
                     "id": 9,
                     "job": { "id": 1, "title": "Software Developer / Software Engineer", "location": "Bangalore, India", "company": "TechCorp Solutions" },
                     "candidate": { "id": 2, "email": "johndoe@test.com", "fullName": "John Doe", "role": "CANDIDATE" },
                     "status": "PENDING",
                     "appliedAt": "2026-05-28T21:49:36.493"
                   }
                 ]
               }
```

---

### 6. Excel Column Format

| Column | Description | When Filled |
|--------|-------------|-------------|
| `Sl No` | Sequential number | First step row only |
| `TestCaseId` | Unique ID | First step row only |
| `TestCaseName` | Short action-based name | First step row only |
| `TestCaseDescription` | What the test verifies | First step row only |
| `TestSteps` | Action + endpoint (e.g. `Login: POST http://localhost:5000/api/auth/login`) | Every row |
| `Request Body` | Prettified JSON payload or full URL with params | Every row |
| `Response Body` | Full prettified JSON response | Every row |

---

### 7. Coverage Categories

| Category | Min Tests |
|----------|-----------|
| Happy Path | 1 per business action (multi-step flow) |
| Business Negative | 1 per business rule (e.g. duplicate apply → 409, invalid jobId → 404) |
| Role-based | When story mentions roles |
| E2E | 1 complete multi-step flow per user story |

---

### 8. Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| JSON file | Exact match to user story name | `HiringManagementSystem.json` |
| `testCaseId` | `TC_` + story prefix + 3-digit sequence | `TC_HMS_001` |
| `testCaseName` | Plain action-based — **no category suffixes** | `Apply for a Job Based on Location` |
| `step` | Action label + method + full URL | `Apply for the job: POST http://localhost:5000/api/applications` |

> ❌ Do NOT use suffixes like `- Positive`, `- Negative`, `- Happy Path`, `- No Auth`, `- E2E`
> ❌ Do NOT put just "200 OK" or "201 Created" as the full response — always include the JSON body

---

## Output Summary

| File | Location | Created By |
|------|----------|-----------|
| `<UserStoryName>.json` | `src/main/resources/TestCases/data/` | **This skill — you create this** |
| `<UserStoryName>.xlsx` | `src/main/resources/TestCases/` | `TestCaseGenerator.generate()` — auto |

---

## Rules
- ✅ **Always ensure `src/main/resources/TestCases/data/` directory exists** before writing the JSON — create it if missing using `New-Item -ItemType Directory -Force`.
- ✅ Create a **new JSON file** for every new user story — never reuse another story's JSON.
- ✅ JSON file name **must exactly match** the user story name.
- ✅ Every AC from `requirement-understanding` must have at least one test case in the JSON.
- ✅ Steps must include actual API endpoint URLs (`http://localhost:5000/...`).
- ✅ Request and Response bodies must be **prettified JSON** — not one-liner strings.
- ✅ Response bodies must reflect **realistic nested API output** (job object, candidate object, status etc.).
- ✅ Each test case should cover a **complete business flow** (multi-step).
- ✅ Use real field names from POJO classes in `src/main/java/com/hiring/pojo/`.
- ❌ NEVER put just "200 OK" or "201 Created" as the full response — always include the JSON body.
- ❌ NEVER put `TestCaseEntry` objects in `SampleTest.java` or any test file.
- ❌ NEVER modify `TestCaseGenerator.java` for new user stories — just create the JSON.
- ❌ NEVER hardcode row numbers in `updateExcelData` — rows are looked up by `TestCaseId`.
- ❌ NEVER duplicate `testCaseId` values.
