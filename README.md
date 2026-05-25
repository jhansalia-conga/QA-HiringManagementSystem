# QA - Hiring Management System

## Overview

This is a **QA Automation Project** that performs the **End-to-End QA Lifecycle** — from reading requirements to automating and executing API test cases. It automates the testing of the [Hiring Management System](https://github.com/odeshpande-conga/HiringManagementSystem) application.

## Application Under Test

- **Repository**: [https://github.com/odeshpande-conga/HiringManagementSystem](https://github.com/odeshpande-conga/HiringManagementSystem)
- **Type**: REST API Application
- **Domain**: Hiring/Recruitment Management

## QA Lifecycle Automation

This project leverages an **AI-powered API Automation Agent** that orchestrates the complete QA lifecycle in sequence:

| Step | Skill | Description |
|------|-------|-------------|
| 1 | **Requirement Understanding** | Reads user stories and extracts API contracts, acceptance criteria, and test scenarios |
| 2 | **Test Case Generation** | Generates API-driven technical test cases (positive, negative, edge cases) |
| 3 | **POJO Evaluation** | Creates/updates Java POJO classes for request/response serialization |
| 4 | **Code Generation** | Generates automated test code using RestAssured + TestNG |
| 5 | **Test Execution** | Executes tests via Maven and validates all pass |
| 6 | **Git Push** | Creates a feature branch and raises a Pull Request |

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 11 |
| Build Tool | Maven |
| Test Framework | TestNG |
| API Library | RestAssured |
| Serialization | Jackson |
| Reporting | Extent Reports |
| Logging | Log4j2 |

## Project Structure

```
QA-HiringManagementSystem/
├── pom.xml                                  # Maven dependencies & build config
├── testng.xml                               # TestNG suite configuration
├── README.md
├── .gitignore
├── .github/
│   └── agents/
│       ├── api-automation-agent.md          # Main orchestrator agent
│       └── skills/
│           ├── requirement-understanding/   # Step 1: Requirement analysis
│           ├── testcase-generation/         # Step 2: Test case design
│           ├── pojo-evaluation/             # Step 3: POJO creation
│           ├── code-generation/             # Step 4: Test code generation
│           ├── test-execution/              # Step 5: Test execution
│           └── git-push/                    # Step 6: PR creation
├── src/
│   ├── main/java/com/hiring/              # Main source (if needed)
│   └── test/
│       ├── java/com/hiring/
│       │   ├── base/                       # BaseTest with common setup
│       │   ├── tests/                      # Test classes
│       │   ├── endpoints/                  # API endpoint constants
│       │   ├── pojo/                       # Request/Response POJOs
│       │   └── utils/                      # Utility classes
│       └── resources/
│           ├── config/                     # Environment configuration
│           ├── testdata/                   # Test data (JSON files)
│           ├── schemas/                    # JSON schema validation
│           └── log4j2.xml                  # Logging configuration
```

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Git

## Setup & Installation

```bash
# Clone the repository
git clone https://github.com/odeshpande-conga/QA-HiringManagementSystem.git
cd QA-HiringManagementSystem

# Install dependencies
mvn clean install -DskipTests

# Update config with your API base URL
# Edit: src/test/resources/config/config.properties
```

## Running Tests

```bash
# Run full test suite
mvn clean test

# Run specific test class
mvn test -Dtest=com.hiring.tests.CandidateTest

# Run specific test method
mvn test -Dtest=com.hiring.tests.CandidateTest#testCreateCandidate

# Generate surefire report
mvn surefire-report:report
```

## Configuration

Update `src/test/resources/config/config.properties`:

```properties
base.url=http://localhost:8080/api
```

## How the Agent Works

1. Provide a **user story** or requirement.
2. The `api-automation-agent` reads the requirement and sequentially invokes each skill.
3. Tests are generated, executed, and — if all pass — a PR is automatically created.

> **No manual intervention required** — the agent handles the full lifecycle autonomously.

## Reports

- **TestNG Reports**: `test-output/`
- **Surefire Reports**: `target/surefire-reports/`
- **Logs**: `logs/test-execution.log`

## Contributing

1. Create a feature branch: `feature/<story-id>-<description>`
2. Implement and run tests locally.
3. Ensure all tests pass before raising a PR.
4. Follow conventional commit messages.

## License

Internal use only — Conga Automation Team.

