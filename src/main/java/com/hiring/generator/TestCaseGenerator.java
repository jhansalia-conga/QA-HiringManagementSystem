package com.hiring.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiring.commonMethods.CommonMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic Test Case Generator.
 *
 * Reads test case data from:
 *   src/main/resources/TestCases/data/<UserStoryName>.json
 *
 * No code changes needed for new user stories — just add a JSON file.
 *
 * Usage from test class (single line):
 *   TestCaseGenerator.generate("HiringManagementSystem");
 *
 * Usage from Maven (no test needed):
 *   mvn compile exec:java -Dexec.args="HiringManagementSystem"
 */
public class TestCaseGenerator {

    private static final String DATA_DIR  = "src/main/resources/TestCases/data/";
    private static final String OUT_DIR   = "src/main/resources/TestCases/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Maven entry point */
    public static void main(String[] args) throws Exception {
        String userStoryName = (args != null && args.length > 0) ? args[0] : "HiringManagementSystem";
        generate(userStoryName);
    }

    /**
     * Generates the Excel file for the given user story.
     * Reads test cases from: src/main/resources/TestCases/data/<userStoryName>.json
     * Writes Excel to:       src/main/resources/TestCases/<userStoryName>.xlsx
     *
     * No code changes required when adding new user stories — just add a new JSON file.
     */
    public static void generate(String userStoryName) throws Exception {

        String jsonPath   = DATA_DIR + userStoryName + ".json";
        String excelPath  = OUT_DIR  + userStoryName + ".xlsx";

        System.out.println("[Generator] Story : " + userStoryName);
        System.out.println("[Generator] Input : " + jsonPath);
        System.out.println("[Generator] Output: " + excelPath);

        // STEP 1 — Load and parse JSON test case data
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            throw new IllegalArgumentException(
                "[Generator] JSON data file not found: " + jsonPath + "\n" +
                "Create the file at: " + jsonPath + "\n" +
                "Follow the schema in testcase-generation/SKILL.md"
            );
        }

        List<CommonMethod.TestCaseEntry> entries = parseTestCases(jsonFile);
        System.out.println("[Generator] Loaded " + entries.size() + " test cases from JSON.");

        // STEP 2a — createExcelAndAddData
        CommonMethod.createExcelAndAddData(excelPath, entries);
        System.out.println("[Generator] createExcelAndAddData complete.");

        // STEP 2b — readExcelData (verify)
        List<Map<String, String>> rows = CommonMethod.readExcelData(excelPath);
        System.out.println("[Generator] readExcelData — total rows: " + rows.size());

        // STEP 2c — updateExcelData: update any row that has an "updateResponseBody" set in JSON
        //           Also supports TC_API_011 default update for backward compatibility
        for (int i = 0; i < rows.size(); i++) {
            String tcId = rows.get(i).get("TestCaseId");
            String updated = getUpdateValue(jsonFile, tcId);
            if (updated != null) {
                CommonMethod.updateExcelData(excelPath, i + 1, "Response Body", updated);
                System.out.println("[Generator] updateExcelData — " + tcId + " Response Body updated.");
            }
        }

        System.out.println("[Generator] Done — " + userStoryName + ".xlsx ready.");
    }

    // -----------------------------------------------------------------------
    // Parses <UserStoryName>.json into List<TestCaseEntry>
    // JSON schema:
    // [
    //   {
    //     "slNo": "1",
    //     "testCaseId": "TC_API_001",
    //     "testCaseName": "...",
    //     "testCaseDescription": "...",
    //     "updateResponseBody": "optional — if set, this value is written to Response Body via updateExcelData",
    //     "steps": [
    //       { "step": "...", "requestBody": "...", "responseBody": "..." }
    //     ]
    //   }
    // ]
    // -----------------------------------------------------------------------
    private static List<CommonMethod.TestCaseEntry> parseTestCases(File jsonFile) throws Exception {
        JsonNode root = MAPPER.readTree(jsonFile);
        List<CommonMethod.TestCaseEntry> entries = new ArrayList<>();

        for (JsonNode tc : root) {
            String slNo        = tc.path("slNo").asText();
            String tcId        = tc.path("testCaseId").asText();
            String tcName      = tc.path("testCaseName").asText();
            String tcDesc      = tc.path("testCaseDescription").asText();

            List<List<String>> steps = new ArrayList<>();
            for (JsonNode s : tc.path("steps")) {
                List<String> row = new ArrayList<>();
                row.add(s.path("step").asText(""));
                row.add(s.path("requestBody").asText(""));
                row.add(s.path("responseBody").asText(""));
                steps.add(row);
            }
            entries.add(new CommonMethod.TestCaseEntry(slNo, tcId, tcName, tcDesc, steps));
        }
        return entries;
    }

    /** Returns the updateResponseBody value for a given testCaseId, or null if not set */
    private static String getUpdateValue(File jsonFile, String testCaseId) throws Exception {
        if (testCaseId == null || testCaseId.isEmpty()) return null;
        JsonNode root = MAPPER.readTree(jsonFile);
        for (JsonNode tc : root) {
            if (testCaseId.equals(tc.path("testCaseId").asText())) {
                JsonNode upd = tc.path("updateResponseBody");
                return upd.isMissingNode() ? null : upd.asText();
            }
        }
        return null;
    }
}
