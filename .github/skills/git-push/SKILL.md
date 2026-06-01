---
name: git-publish
description: "Exact git staging rules (which files to add, NEVER git add .), commit message format, push command with dynamic branch detection, PR creation via mcp_io_github_create_pull_request (auto-detect owner/repo), and Copilot review request. Use during Phase 8 Publish & Review."
user-invocable: false
---

# Git Publish — Commit, Push & Pull Request

## Project Repository

**GitHub URL:** `https://github.com/odeshpande-conga/QA-HiringManagementSystem`

---

## CRITICAL CONSTRAINT

**NEVER run `git add .` or `git add -A` or `git add src/`.**
Stage only the specific files listed below. Any deviation must be flagged to the user.

**ONLY stage files that were explicitly created or modified by the Agent during the current session.**
Before staging, cross-check every file against the Agent's own session activity log (files written, edited, or generated). If a file appears in `git status` but was **not** created or modified by the Agent in this session, **skip it** — do not stage it under any circumstances.

**NEVER stage or commit any `config.properties` file**, regardless of where it lives:
- `src/main/resources/testdata/config.properties`
- Any other `config.properties` found anywhere in the project

`config.properties` files contain environment-specific credentials and settings that must **never** be pushed to Git.
If `git status` shows any `config.properties` as modified or staged, skip it. Do **not** run `git add` on it under any circumstances.

---

## Step 1 — Stage Specific Files Only

```powershell
# Test data JSON (flat in testdata/ — only files added/modified by Agent)
git add src/main/resources/testdata/<filename>.json

# Test class (under com.hiring.tests)
git add src/test/java/com/hiring/tests/<ClassName>.java

# ActorHelper (only if a new helper method was added in this session)
git add src/main/java/com/hiring/helpers/ActorHelper.java

# CommonMethod (only if modified in this session)
git add src/main/java/com/hiring/commonMethods/CommonMethod.java

# URL registry (only if a new endpoint was added)
git add src/main/java/com/hiring/utils/URLGenerator.java

# POJO (only if newly created or modified by Agent)
git add src/main/java/com/hiring/pojo/<Entity>POJO.java

# Response model (only if newly created or modified by Agent)
git add src/main/java/com/hiring/response/<Entity>Response.java

# TestNG suite XMLs (project root — only if a class was added/modified)
git add testng.xml
git add testng-generate.xml
```

---

### File Inclusion Decision Table

| File | Include in `git add`? | Condition |
|------|-----------------------|-----------|
| Any file **not** created or modified by the Agent in this session | **NEVER** | Only stage files the Agent explicitly wrote or edited |
| `src/main/resources/testdata/<filename>.json` | YES | Test data added/modified **by Agent** |
| `src/test/java/com/hiring/tests/<ClassName>.java` | YES | New/modified test class **by Agent** |
| `src/main/java/com/hiring/helpers/ActorHelper.java` | YES | Only if Agent added a new helper method in this session |
| `src/main/java/com/hiring/commonMethods/CommonMethod.java` | YES | Only if Agent modified it in this session |
| `src/main/java/com/hiring/utils/URLGenerator.java` | YES | Only if Agent added a new URL variable in this session |
| `src/main/java/com/hiring/pojo/<Entity>POJO.java` | YES | Only if created or modified by Agent in this session |
| `src/main/java/com/hiring/response/<Entity>Response.java` | YES | Only if created or modified by Agent in this session |
| `testng.xml` | YES | Only if Agent added a class to this suite in this session |
| `testng-generate.xml` | YES | Only if Agent added a class to this suite in this session |
| `testng_temp_run.xml` | **NEVER** | Temp file — always deleted before staging |
| `**/config.properties` | **NEVER** | Contains environment credentials — must never be committed |

---

## Step 2 — Commit

```powershell
git commit -m "[<TC-ID>]: Add automated tests for <feature description>"
```

**Commit message rules:**
- `<TC-ID>` → the test case ID (e.g., `TC-101`) or issue key
- `<feature description>` → 3–8 word description from the test case title (lowercase, natural language)
- Examples:
  - `[TC-101]: Add automated tests for candidate registration`
  - `[TC-202]: Add automated tests for job creation by recruiter`
  - `[TC-303]: Add automated tests for apply job as candidate`

---

## Step 3 — Push (DO NOT CREATE A NEW BRANCH)

Detect the current branch dynamically and push to it. Never create a new branch.

```powershell
$currentBranch = git rev-parse --abbrev-ref HEAD
git push -u origin $currentBranch
```

---

## Step 4 — Create Pull Request

### Detect Owner and Repo

```powershell
$remoteUrl = git remote get-url origin
# Expected: https://github.com/odeshpande-conga/QA-HiringManagementSystem.git
# Owner: odeshpande-conga
# Repo:  QA-HiringManagementSystem
```

Use the detected `owner` and `repo` values as inputs to the PR creation tool. Do **not** hardcode them.

Detect current head branch:
```powershell
$headBranch = git rev-parse --abbrev-ref HEAD
```

**Base branch is always: `main`**

### PR Creation Tool

Use: `mcp_io_github_create_pull_request`

Inputs:
- `owner`: detected from `git remote get-url origin` (typically `odeshpande-conga`)
- `repo`: `QA-HiringManagementSystem`
- `title`: `[<TC-ID>]: Add automated tests for <feature description>`
- `head`: `$headBranch` (detected above)
- `base`: `main`
- `body`: (use the PR Body Template below)

### PR Body Template

```markdown
## Summary

**Test Case ID:** <TC-ID>
**Feature:** <Feature description from test case title>

## Changes

| File | Change Type |
|------|-------------|
| `src/main/resources/testdata/<filename>.json` | New test data added |
| `src/test/java/com/hiring/tests/<ClassName>.java` | New test method(s) added |
| `ActorHelper.java` | New helper method(s) added (if applicable) |
| `URLGenerator.java` | New URL variable(s) added (if applicable) |
| `testng.xml` / `testng-generate.xml` | Test class registered (if applicable) |

## LOC Summary

| Metric | Value |
|--------|-------|
| Modified files (existing) | <N> — `<filename1>`, `<filename2>` |
| New files added | <N> — `<filename>` (or `0 — —` if none) |
| Lines added to existing files | <N> |
| Lines added in new files | <N> (or `—` if none) |

## Test Results

All generated tests PASS via temp TestNG XML run.

## Test Suites Registered

- [ ] `testng.xml`
- [ ] `testng-generate.xml`
```

---

## Step 5 — Request Copilot Review

After the PR is created, request a Copilot review.

Use: `mcp_io_github_request_copilot_review`

Inputs:
- `owner`: same owner detected above
- `repo`: `QA-HiringManagementSystem`
- `pullNumber`: the PR number returned by the PR creation tool in Step 4

---

## Project File Structure Reference

```
QA-HiringManagementSystem/
├── pom.xml
├── testng.xml                                     ← Main TestNG suite
├── testng-generate.xml                            ← Generator TestNG suite
├── src/
│   ├── main/
│   │   ├── java/com/hiring/
│   │   │   ├── commonMethods/
│   │   │   │   └── CommonMethod.java              ← Shared utility methods
│   │   │   ├── generator/
│   │   │   │   └── TestCaseGenerator.java
│   │   │   ├── helpers/
│   │   │   │   └── ActorHelper.java               ← Main orchestration helper
│   │   │   ├── pojo/
│   │   │   │   ├── ApplicationRequestPOJO.java
│   │   │   │   ├── CandidatePOJO.java
│   │   │   │   ├── JobRequestPOJO.java
│   │   │   │   ├── LoginRequestPOJO.java
│   │   │   │   ├── RegisterRequestPOJO.java
│   │   │   │   └── UserProfileRequestPOJO.java
│   │   │   ├── response/
│   │   │   │   ├── ApplicationResponse.java
│   │   │   │   └── UserProfileResponse.java
│   │   │   └── utils/
│   │   │       ├── BaseTest.java                  ← Base test setup
│   │   │       ├── RestUtils.java                 ← Token management, REST Assured
│   │   │       └── URLGenerator.java              ← All API endpoint templates
│   │   └── resources/
│   │       ├── testdata/
│   │       │   ├── config.properties              ← NEVER commit this file
│   │       │   ├── apply-job.json
│   │       │   ├── candidate.json
│   │       │   ├── create-job.json
│   │       │   ├── register-candidate.json
│   │       │   ├── register-recruiter.json
│   │       │   └── update-profile.json
│   │       ├── TestCases/
│   │       │   ├── HiringManagementSystem.xlsx
│   │       │   └── data/
│   │       │       └── HiringManagementSystem.json
│   │       └── UserStory/
│   │           └── HiringManagementSystem.doc
│   └── test/
│       └── java/com/hiring/tests/               ← Test classes
│           └── SampleTest.java
```

---

## Error Handling

| Failure | Action |
|---------|--------|
| `git status` shows files not modified by Agent | Skip those files entirely — do not stage them. Inform the user which files were skipped. |
| `git add` fails for a specific file | Warn user: file may not exist or path may differ. List remaining files to add manually. |
| `git commit` fails (nothing to commit) | Warn user; verify all files were written and staged correctly |
| `git push` fails (non-fast-forward) | Warn user: `git pull --rebase` required before pushing again |
| PR creation tool fails | Provide fallback: output the PR URL structure `https://github.com/odeshpande-conga/QA-HiringManagementSystem/compare/main...<headBranch>` for manual PR creation |
| Copilot review fails | Warn user but do not block — PR is already created |
