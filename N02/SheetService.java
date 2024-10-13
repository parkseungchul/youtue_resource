package com.psc.sw.website.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.psc.sw.website.component.ProfileComponent;
import com.psc.sw.website.dto.CustomSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetService {

    private final ProfileComponent profileComponent;
    private static final String VALUE_INPUT_OPTION = "RAW";
    private static final String TITLE_FIELD = "title";

    private Sheets sheetsService;

    /**
     * Initializes the Google Sheets service object if it hasn't been initialized yet.
     * Returns the existing service object if already initialized.
     *
     * @return Initialized Sheets service object
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is an I/O error
     */
    private synchronized Sheets getSheetsService() throws GeneralSecurityException, IOException {
        if (sheetsService == null) {
            final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            ClassPathResource resource = new ClassPathResource(profileComponent.getCredentialsFilePath());
            final GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                    .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
            sheetsService = new Sheets.Builder(httpTransport, JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                    .setApplicationName(profileComponent.getApplicationName())
                    .build();
            log.info("Google Sheets service initialized.");
        }
        return sheetsService;
    }

    /**
     * Retrieves the sheet ID by its name within a specific spreadsheet.
     *
     * @param service       Sheets service object
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet
     * @return ID of the sheet
     * @throws IOException              If there is a network or I/O error
     * @throws IllegalArgumentException If the sheet is not found
     */
    private int getSheetIdByName(Sheets service, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        return spreadsheet.getSheets().stream()
                .filter(sheet -> sheet.getProperties().getTitle().equals(sheetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + sheetName + "' not found"))
                .getProperties().getSheetId();
    }

    /**
     * Retrieves all sheet information from a specific spreadsheet ID.
     *
     * @param spreadsheetId ID of the spreadsheet to retrieve sheets from
     * @return List of Sheet objects containing sheet information
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public List<Sheet> getAllSheets(String spreadsheetId) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
            log.info("Retrieved all sheets for spreadsheet ID: {}", spreadsheetId);
            return spreadsheet.getSheets();
        } catch (GoogleJsonResponseException e) {
            log.error("Error retrieving sheets: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving sheets: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves data from a specific sheet within a spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet to retrieve data from
     * @param sheetName     Name of the sheet to retrieve data from
     * @return List of rows, where each row is a list of cell values
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public List<List<Object>> getSheetData(String spreadsheetId, String sheetName) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            String range = sheetName; // Specify sheet name to retrieve all data
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            log.info("Retrieved data for sheet: {} in spreadsheet ID: {}", sheetName, spreadsheetId);
            return response.getValues();
        } catch (GoogleJsonResponseException e) {
            log.error("Error retrieving sheet data: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving sheet data: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Adds a new sheet to a specific spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet to add the sheet to
     * @param sheetName     Name of the new sheet to be added
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void addSheet(String spreadsheetId, String sheetName) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            AddSheetRequest addSheetRequest = new AddSheetRequest().setProperties(new SheetProperties().setTitle(sheetName));
            BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(new Request().setAddSheet(addSheetRequest)));
            service.spreadsheets().batchUpdate(spreadsheetId, request).execute();
            log.info("Added new sheet: {} to spreadsheet ID: {}", sheetName, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Error adding sheet: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding sheet: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Renames an existing sheet within a spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param oldSheetName  Current name of the sheet
     * @param newSheetName  New name to rename the sheet to
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void renameSheet(String spreadsheetId, String oldSheetName, String newSheetName) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();

            // Find the existing sheet by name
            Sheet sheetToRename = spreadsheet.getSheets().stream()
                    .filter(sheet -> sheet.getProperties().getTitle().equals(oldSheetName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + oldSheetName + "' not found"));

            // Create an UpdateSheetPropertiesRequest to change the title
            UpdateSheetPropertiesRequest updateSheetPropertiesRequest = new UpdateSheetPropertiesRequest()
                    .setProperties(new SheetProperties()
                            .setSheetId(sheetToRename.getProperties().getSheetId())
                            .setTitle(newSheetName))
                    .setFields(TITLE_FIELD); // Specify that only the title field should be updated

            // Create a BatchUpdateSpreadsheetRequest containing the update request
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(new Request().setUpdateSheetProperties(updateSheetPropertiesRequest)));

            // Execute the BatchUpdate API call
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Renamed sheet from '{}' to '{}' in spreadsheet ID: {}", oldSheetName, newSheetName, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Error renaming sheet: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error renaming sheet: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Removes a sheet from a spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to be removed
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void removeSheet(String spreadsheetId, String sheetName) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
            Sheet sheetToDelete = spreadsheet.getSheets().stream()
                    .filter(sheet -> sheet.getProperties().getTitle().equals(sheetName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + sheetName + "' not found"));

            DeleteSheetRequest deleteRequest = new DeleteSheetRequest()
                    .setSheetId(sheetToDelete.getProperties().getSheetId());

            BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(new Request().setDeleteSheet(deleteRequest)));
            service.spreadsheets().batchUpdate(spreadsheetId, request).execute();
            log.info("Deleted sheet: {} from spreadsheet ID: {}", sheetName, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Error deleting sheet: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting sheet: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Converts sheet names to a list of CustomSheet DTOs.
     *
     * @return List of CustomSheet objects
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public List<CustomSheet> getSheetNamesAsCustomSheets(String spreadsheetId) throws IOException, GeneralSecurityException {
        try {
            List<Sheet> sheets = getAllSheets(spreadsheetId);
            List<CustomSheet> customSheets = new ArrayList<>();
            for (Sheet sheet : sheets) {
                String sheetName = sheet.getProperties().getTitle();
                int sheetId = sheet.getProperties().getSheetId();
                customSheets.add(new CustomSheet(sheetId, sheetName));
            }
            log.info("Converted sheets to CustomSheet DTOs for spreadsheet ID: {}", spreadsheetId);
            return customSheets;
        } catch (Exception e) {
            log.error("Error converting sheets to CustomSheet DTOs: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Moves a sheet to a new index within the spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to be moved
     * @param newIndex      New index to move the sheet to (0-based)
     * @param right         Direction to adjust the new index (0 = left, 1 = right)
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     * @throws IllegalArgumentException If the sheet name or index is invalid
     */
    public void moveSheet(String spreadsheetId, String sheetName, int newIndex, int right) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
            List<Sheet> sheets = spreadsheet.getSheets();
            int totalSheets = sheets.size();

            // Adjust totalSheets based on the direction
            totalSheets = totalSheets + right;
            newIndex = newIndex + right;

            if (newIndex < 0 || newIndex >= totalSheets) { // newIndex must be >= 0 and < totalSheets
                throw new IllegalArgumentException("newIndex must be between 0 and " + (totalSheets - 1));
            }

            // Find the sheet to move
            Sheet sheetToMove = sheets.stream()
                    .filter(sheet -> sheet.getProperties().getTitle().equals(sheetName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + sheetName + "' not found"));

            int currentIndex = sheetToMove.getProperties().getIndex();

            log.debug("Move sheet current : {} -> new : {}", currentIndex, newIndex);
            if (currentIndex == newIndex) {
                log.info("Sheet '{}' is already at index {}", sheetName, newIndex);
                return;
            }

            // Create an UpdateSheetPropertiesRequest to change the index
            SheetProperties newProperties = new SheetProperties()
                    .setSheetId(sheetToMove.getProperties().getSheetId())
                    .setIndex(newIndex);

            UpdateSheetPropertiesRequest updateRequest = new UpdateSheetPropertiesRequest()
                    .setProperties(newProperties)
                    .setFields("index");

            // Create a BatchUpdateSpreadsheetRequest containing the update request
            Request request = new Request().setUpdateSheetProperties(updateRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            // Execute the BatchUpdate API call
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Moved sheet '{}' from index {} to {} in spreadsheet ID: {}", sheetName, currentIndex, newIndex, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Error moving sheet: {}", e.getDetails());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error moving sheet: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Adds columns to a specific sheet in a spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to add columns to
     * @param referenceIndex Index at which to add the new columns
     * @param right         Direction to add columns (0 = left, 1 = right)
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void addColumns(String spreadsheetId, String sheetName, int referenceIndex, int right) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            int sheetId = getSheetIdByName(service, spreadsheetId, sheetName);
            int numColumns = 1;
            Direction direction = right == 1 ? Direction.RIGHT : Direction.LEFT;
            int startIndex;

            if (direction == Direction.LEFT) {
                startIndex = referenceIndex;
            } else {
                startIndex = referenceIndex; // Modification: When adding to the right, do not increment by 1
            }

            // Get the current number of columns in the sheet
            int columnCount = getSheetColumnCount(service, spreadsheetId, sheetName);
            log.debug("Current column count for sheet '{}': {}", sheetName, columnCount);
            log.debug("Attempting to add {} column(s) {} at startIndex {}", numColumns, direction, startIndex);

            if (startIndex < 0 || startIndex > columnCount) {
                throw new IllegalArgumentException("startIndex " + startIndex + " is out of bounds for sheet '" + sheetName + "' with " + columnCount + " columns.");
            }

            // Determine if the new columns should inherit properties from the previous columns
            boolean inheritFromBefore = !(direction == Direction.LEFT && startIndex == 0);

            InsertDimensionRequest insertRequest = new InsertDimensionRequest()
                    .setRange(new DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(startIndex)
                            .setEndIndex(startIndex + numColumns))
                    .setInheritFromBefore(inheritFromBefore);

            log.debug("InsertDimensionRequest: startIndex={}, endIndex={}, inheritFromBefore={}", startIndex, startIndex + numColumns, inheritFromBefore);

            Request request = new Request().setInsertDimension(insertRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            // Execute the API request
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Added {} column(s) to sheet '{}' {} starting at index {} in spreadsheet ID: {}",
                    numColumns, sheetName, direction, startIndex, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Google API Error adding columns: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding columns: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the number of columns in a specific sheet.
     *
     * @param service       Sheets service object
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to retrieve the column count from
     * @return Number of columns in the sheet
     * @throws IOException If there is a network or I/O error
     */
    private int getSheetColumnCount(Sheets service, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        Sheet sheet = spreadsheet.getSheets().stream()
                .filter(s -> s.getProperties().getTitle().equals(sheetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + sheetName + "' not found"));
        GridProperties gridProperties = sheet.getProperties().getGridProperties();
        return gridProperties != null ? gridProperties.getColumnCount() : 100; // Default to 100 columns if not specified
    }

    /**
     * Removes columns from a specific sheet in a spreadsheet.
     *
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to remove columns from
     * @param startIndex    Starting index of the columns to remove (0-based)
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void removeColumns(String spreadsheetId, String sheetName, int startIndex) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            int sheetId = getSheetIdByName(service, spreadsheetId, sheetName);

            int numColumns = 1;

            DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
                    .setRange(new DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("COLUMNS")
                            .setStartIndex(startIndex)
                            .setEndIndex(startIndex + numColumns));

            Request request = new Request().setDeleteDimension(deleteRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Deleted {} columns from sheet '{}' starting at index {} in spreadsheet ID: {}", numColumns, sheetName, startIndex, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Error deleting columns: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting columns: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Adds rows to a specific sheet in a spreadsheet.
     *
     * @param sheetName  Name of the sheet to add rows to
     * @param startIndex Index at which to add the new rows (0-based)
     * @param below      Direction to add rows (0 = above, 1 = below)
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void addRows(String sheetName, int startIndex, int below) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            String spreadsheetId = profileComponent.getSheetId();
            int sheetId = getSheetIdByName(service, spreadsheetId, sheetName);
            int numRows = 1;
            Direction direction = below == 1 ? Direction.BOTTOM : Direction.TOP;
            int insertIndex;

            if (direction == Direction.TOP) {
                insertIndex = startIndex;
            } else {
                insertIndex = startIndex + 1;
            }

            // Get the current number of rows in the sheet
            int rowCount = getSheetRowCount(service, spreadsheetId, sheetName);
            log.debug("Current row count for sheet '{}': {}", sheetName, rowCount);
            log.debug("Attempting to add {} row(s) {} at insertIndex {}", numRows, direction, insertIndex);

            if (insertIndex < 0 || insertIndex > rowCount) {
                throw new IllegalArgumentException("insertIndex " + insertIndex + " is out of bounds for sheet '" + sheetName + "' with " + rowCount + " rows.");
            }

            // Determine if the new rows should inherit properties from the previous rows
            boolean inheritFromBefore = !(direction == Direction.TOP && insertIndex == 0);

            InsertDimensionRequest insertRequest = new InsertDimensionRequest()
                    .setRange(new DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(insertIndex)
                            .setEndIndex(insertIndex + numRows))
                    .setInheritFromBefore(inheritFromBefore);

            log.debug("InsertDimensionRequest: startIndex={}, endIndex={}, inheritFromBefore={}", insertIndex, insertIndex + numRows, inheritFromBefore);

            Request request = new Request().setInsertDimension(insertRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            // Execute the API request
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Added {} row(s) to sheet '{}' {} starting at index {} in spreadsheet ID: {}",
                    numRows, sheetName, direction, insertIndex, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Google API Error adding rows: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding rows: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes rows from a specific sheet in a spreadsheet.
     *
     * @param sheetName Name of the sheet to delete rows from
     * @param startIndex Starting index of the rows to delete (0-based)
     * @param numRows    Number of rows to delete
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void deleteRows(String sheetName, int startIndex, int numRows) throws GeneralSecurityException, IOException {
        try {
            Sheets service = getSheetsService();
            String spreadsheetId = profileComponent.getSheetId();
            int sheetId = getSheetIdByName(service, spreadsheetId, sheetName);

            // Get the current number of rows in the sheet
            int rowCount = getSheetRowCount(service, spreadsheetId, sheetName);
            log.debug("Current row count for sheet '{}': {}", sheetName, rowCount);

            // Verify that the row range to delete is valid
            if (startIndex < 0 || (startIndex + numRows) > rowCount) {
                throw new IllegalArgumentException("Row range " + startIndex + " to " + (startIndex + numRows) + " is out of bounds for sheet '" + sheetName + "' with " + rowCount + " rows.");
            }

            // Create a DeleteDimensionRequest to remove the rows
            DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
                    .setRange(new DimensionRange()
                            .setSheetId(sheetId)
                            .setDimension("ROWS")
                            .setStartIndex(startIndex) // inclusive
                            .setEndIndex(startIndex + numRows)); // exclusive

            Request request = new Request().setDeleteDimension(deleteRequest);
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                    .setRequests(Collections.singletonList(request));

            // Execute the API request
            service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
            log.info("Deleted {} row(s) from sheet '{}' starting at index {} in spreadsheet ID: {}",
                    numRows, sheetName, startIndex, spreadsheetId);
        } catch (GoogleJsonResponseException e) {
            log.error("Google API Error deleting rows: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting rows: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the current number of rows in a specific sheet.
     *
     * @param service       Sheets service object
     * @param spreadsheetId ID of the spreadsheet containing the sheet
     * @param sheetName     Name of the sheet to retrieve the row count from
     * @return Number of rows in the sheet
     * @throws IOException              If there is a network or I/O error
     * @throws IllegalArgumentException If the sheet is not found
     */
    private int getSheetRowCount(@NotNull Sheets service, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .setIncludeGridData(false) // Exclude GridData for faster response
                .execute();
        Sheet sheet = spreadsheet.getSheets().stream()
                .filter(s -> s.getProperties().getTitle().equals(sheetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sheet with name '" + sheetName + "' not found"));

        GridProperties gridProperties = sheet.getProperties().getGridProperties();
        if (gridProperties != null && gridProperties.getRowCount() != null) {
            return gridProperties.getRowCount();
        } else {
            // Return default row count if not specified (e.g., 1000)
            return 1000;
        }
    }

    /**
     * Updates a specific cell in a sheet with a new value.
     *
     * @param sheetName Name of the sheet containing the cell
     * @param rowIndex  Row index of the cell (0-based)
     * @param colIndex  Column index of the cell (0-based)
     * @param newValue  New value to set in the cell
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    public void updateCell(String sheetName, int rowIndex, int colIndex, String newValue) throws GeneralSecurityException, IOException {

        log.debug("=====>{} {} {} {}", sheetName, rowIndex, colIndex, newValue);

        Sheets service = getSheetsService();
        String spreadsheetId = profileComponent.getSheetId(); // Retrieve spreadsheet ID from ProfileComponent

        // Calculate the A1 notation for the cell
        String cellAddress = convertToA1Notation(rowIndex, colIndex);
        String range = sheetName + "!" + cellAddress;

        // Create a ValueRange object with the new value
        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(newValue)));

        try {
            // Update the cell value
            service.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption(VALUE_INPUT_OPTION)
                    .execute();
            log.info("Updated cell {} in sheet '{}' with value '{}'", cellAddress, sheetName, newValue);
        } catch (GoogleJsonResponseException e) {
            log.error("Google API Error updating cell: {}", e.getDetails());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating cell: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Converts row and column indices to A1 notation for cell addresses.
     *
     * @param rowIndex Row index (0-based)
     * @param colIndex Column index (0-based)
     * @return A1 notation of the cell address
     */
    private String convertToA1Notation(int rowIndex, int colIndex) {
        String column = convertToColumnName(colIndex);
        int row = rowIndex + 1; // A1 notation is 1-based
        return column + row;
    }

    /**
     * Converts a column index to its corresponding column name in A1 notation.
     * For example, 0 -> A, 1 -> B, ..., 25 -> Z, 26 -> AA, etc.
     *
     * @param colIndex Column index (0-based)
     * @return Column name in A1 notation
     */
    private String convertToColumnName(int colIndex) {
        StringBuilder columnName = new StringBuilder();
        colIndex++;
        while (colIndex > 0) {
            int rem = (colIndex - 1) % 26;
            columnName.insert(0, (char) ('A' + rem));
            colIndex = (colIndex - 1) / 26;
        }
        return columnName.toString();
    }

    // It is recommended to separate test methods into a separate test class.
    /**
     * Reads data from a specified range in the spreadsheet.
     *
     * @param range Range in A1 notation to read data from
     * @return List of rows, where each row is a list of cell values
     * @throws IOException              If there is a network or I/O error
     * @throws GeneralSecurityException If there is a security-related error
     */
    public List<List<Object>> readData(String range) throws IOException, GeneralSecurityException {
        try {
            ValueRange response = getSheetsService().spreadsheets().values()
                    .get(profileComponent.getSheetId(), range)
                    .execute();
            log.info("Read data from range: {}", range);
            return response.getValues();
        } catch (Exception e) {
            log.error("Error reading data: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Enumeration for specifying directions when adding rows or columns.
     */
    public enum Direction {
        LEFT,
        RIGHT,
        BOTTOM,
        TOP
    }
}
