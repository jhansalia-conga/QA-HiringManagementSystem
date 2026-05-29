package com.hiring.commonMethods;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonMethod {

    // Default Excel file path
    public static final String EXCEL_FILE_PATH = "";

    // Column headers
    private static final String[] HEADERS = {
            "Sl No", "TestCaseId", "TestCaseName", "TestCaseDescription", "TestSteps", "Request Body", "Response Body"
    };

    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * Reads a JSON file and returns its content as a Map with String values.
     *
     * @param filePath path to the JSON file
     * @return HashMap containing the JSON key-value pairs as Strings
     */
    public static HashMap<String, String> readTestData(String filePath) {
        try {
            return objectMapper.readValue(new File(filePath), new TypeReference<HashMap<String, String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath + " | Error: " + e.getMessage());
        }
    }

    /**
     * Represents a single test case with multiple steps.
     * Each step contains: [stepDescription, requestBody, responseBody]
     */
    public static class TestCaseEntry {
        public String slNo;
        public String testCaseId;
        public String testCaseName;
        public String testCaseDescription;
        public List<List<String>> steps; // each step: [stepDescription, requestBody, responseBody]

        public TestCaseEntry(String slNo, String testCaseId, String testCaseName,
                             String testCaseDescription, List<List<String>> steps) {
            this.slNo = slNo;
            this.testCaseId = testCaseId;
            this.testCaseName = testCaseName;
            this.testCaseDescription = testCaseDescription;
            this.steps = steps;
        }
    }

    /**
     * Creates or appends to an Excel file.
     * Each TestCaseEntry writes its first step on the same row as the test case metadata.
     * Subsequent steps are written on new rows with the metadata columns left blank.
     *
     * @param filePath  Path where the Excel file will be saved
     * @param entries   List of TestCaseEntry objects
     * @throws IOException if file cannot be written
     */
    public static void createExcelAndAddData(String filePath, List<TestCaseEntry> entries) throws IOException {
        File file = new File(filePath);

        // Ensure parent directories exist
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        Workbook workbook;
        Sheet sheet;
        CellStyle wrapStyle;

        // Header cell style (bold)
        if (file.exists()) {
            System.out.println("[CommonMethod] Excel file already exists, appending data: " + file.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fis);
            }
            sheet = workbook.getSheet("TestCases");
            if (sheet == null) {
                sheet = workbook.createSheet("TestCases");
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("TestCases");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            System.out.println("[CommonMethod] Creating new Excel file: " + file.getName());
        }

        // Wrap-text style for Request Body and Response Body columns
        wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);

        // --- Collect existing TestCaseIds to avoid duplicates ---
        Set<String> existingTestCaseIds = new HashSet<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row existingRow = sheet.getRow(r);
            if (existingRow != null) {
                Cell idCell = existingRow.getCell(1);
                if (idCell != null && !getCellValueAsString(idCell).trim().isEmpty()) {
                    existingTestCaseIds.add(getCellValueAsString(idCell).trim());
                }
            }
        }

        int rowIndex = sheet.getLastRowNum() + 1;

        for (TestCaseEntry entry : entries) {
            if (existingTestCaseIds.contains(entry.testCaseId.trim())) {
                System.out.println("[CommonMethod] Skipping duplicate TestCaseId: " + entry.testCaseId);
                continue;
            }

            List<List<String>> steps = (entry.steps != null && !entry.steps.isEmpty())
                    ? entry.steps
                    : Collections.singletonList(Arrays.asList("", "", ""));

            for (int s = 0; s < steps.size(); s++) {
                Row row = sheet.createRow(rowIndex++);
                List<String> step = steps.get(s);

                if (s == 0) {
                    // First step row: fill in all metadata columns
                    row.createCell(0).setCellValue(entry.slNo);
                    row.createCell(1).setCellValue(entry.testCaseId);
                    row.createCell(2).setCellValue(entry.testCaseName);
                    row.createCell(3).setCellValue(entry.testCaseDescription);
                }

                // TestSteps (col 4) — plain style
                row.createCell(4).setCellValue(step.size() > 0 ? step.get(0) : "");

                // Request Body (col 5) — wrap-text so \n renders as real line breaks
                Cell reqCell = row.createCell(5);
                reqCell.setCellValue(step.size() > 1 ? step.get(1) : "");
                reqCell.setCellStyle(wrapStyle);

                // Response Body (col 6) — wrap-text so \n renders as real line breaks
                Cell resCell = row.createCell(6);
                resCell.setCellValue(step.size() > 2 ? step.get(2) : "");
                resCell.setCellStyle(wrapStyle);

                // Set a reasonable row height for multi-line content (in points * 20)
                int lineCount = Math.max(
                        countLines(step.size() > 1 ? step.get(1) : ""),
                        countLines(step.size() > 2 ? step.get(2) : "")
                );
                if (lineCount > 1) {
                    row.setHeight((short) Math.min(lineCount * 300, 8000));
                }
            }

            existingTestCaseIds.add(entry.testCaseId.trim());
        }

        // Fix column widths: auto-size metadata cols; set explicit width for body cols
        for (int i = 0; i <= 4; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setColumnWidth(5, 15000);  // Request Body ~120 chars wide
        sheet.setColumnWidth(6, 20000);  // Response Body ~160 chars wide

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("[CommonMethod] Excel file saved successfully at: " + file.getAbsolutePath());
    }

    /** Counts the number of lines in a value (split on \n) */
    private static int countLines(String value) {
        if (value == null || value.isEmpty()) return 1;
        return value.split("\n", -1).length;
    }

    /**
     * Reads all data rows (excluding the header) from the given Excel file.
     *
     * @param filePath Path to the existing Excel file
     * @return List of maps where each map's keys are column headers and values are cell contents
     * @throws IOException if the file cannot be read
     */
    public static List<Map<String, String>> readExcelData(String filePath) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                System.out.println("[CommonMethod] Excel file is empty.");
                return result;
            }

            // Build header list from the first row
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            // Iterate data rows
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(col), getCellValueAsString(cell));
                }
                result.add(rowMap);
            }
        }

        System.out.println("[CommonMethod] Read " + result.size() + " row(s) from: " + filePath);
        return result;
    }

    /**
     * Updates a specific cell in the Excel file identified by row number (1-based, excluding header)
     * and column name.
     *
     * @param filePath   Path to the existing Excel file
     * @param rowNumber  1-based data row number (row 1 = first data row after header)
     * @param columnName One of the header names: Sl No, TestCaseId, TestCaseName, etc.
     * @param newValue   New value to set in the cell
     * @throws IOException              if the file cannot be read or written
     * @throws IllegalArgumentException if the column name or row number is invalid
     */
    public static void updateExcelData(String filePath, int rowNumber, String columnName, String newValue)
            throws IOException {

        File file = new File(filePath);
        Workbook workbook;

        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = new XSSFWorkbook(fis);
        }

        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);

        // Find the target column index
        int targetColIndex = -1;
        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            if (headerRow.getCell(col).getStringCellValue().trim().equalsIgnoreCase(columnName.trim())) {
                targetColIndex = col;
                break;
            }
        }

        if (targetColIndex == -1) {
            workbook.close();
            throw new IllegalArgumentException("[CommonMethod] Column '" + columnName + "' not found in Excel.");
        }

        int actualRowIndex = rowNumber; // header is row 0, data starts at row 1
        Row row = sheet.getRow(actualRowIndex);
        if (row == null) {
            workbook.close();
            throw new IllegalArgumentException("[CommonMethod] Row number " + rowNumber + " does not exist.");
        }

        Cell cell = row.getCell(targetColIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(newValue);

        // Apply wrap-text for body columns so \n renders as real line breaks
        String colLower = columnName.trim().toLowerCase();
        if (colLower.contains("request") || colLower.contains("response") || colLower.contains("body")) {
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
            cell.setCellStyle(wrapStyle);
            int lineCount = countLines(newValue);
            if (lineCount > 1) {
                row.setHeight((short) Math.min(lineCount * 300, 8000));
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("[CommonMethod] Updated row " + rowNumber + ", column '" + columnName
                + "' with value: " + newValue);
    }

    // --------------- Helper ---------------

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(
                    (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue()))
                            ? (long) cell.getNumericCellValue()
                            : cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default:      return "";
        }
    }

}
