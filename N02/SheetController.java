package com.psc.sw.website.controller;

import com.psc.sw.website.component.ProfileComponent;
import com.psc.sw.website.dto.CustomSheet;
import com.psc.sw.website.service.SheetService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Controller
@RequestMapping("/sheet")
public class

SheetController {

    private final ProfileComponent profileComponent;
    private final SheetService sheetService;

    /**
     * Handles the root request to "/sheet".
     * Retrieves all sheet names and adds them to the model for rendering.
     *
     * @param model Spring Model object to pass data to the view
     * @return The view name "sheet/index"
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @RequestMapping("")
    public String sheet(Model model) throws GeneralSecurityException, IOException {
        List<CustomSheet> sheetList = sheetService.getSheetNamesAsCustomSheets(profileComponent.getSheetId());
        model.addAttribute("sheetList", sheetList);
        log.debug(sheetList.toString());
        return "sheet/index";
    }

    /**
     * Retrieves data for a specific sheet.
     * Endpoint: GET /sheet/data
     *
     * @param sheetName Name of the sheet to retrieve data from
     * @param model     Spring Model object (not used in this method)
     * @return A map containing the sheet name as the key and its data as the value
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @GetMapping("/data")
    @ResponseBody
    public Map<String, List<List<Object>>> data(String sheetName, Model model) throws GeneralSecurityException, IOException {
        List<List<Object>> lists = sheetService.getSheetData(profileComponent.getSheetId(), sheetName);
        Map<String, List<List<Object>>> response = new HashMap<>();
        response.put(sheetName, lists == null ? new ArrayList<List<Object>>() : lists);
        return response;
    }

    /**
     * Moves a sheet to a new position.
     * Endpoint: POST /sheet/move
     *
     * @param sheetName      Name of the sheet to move
     * @param targetIndex    Target index to move the sheet to (0-based)
     * @param right          Direction to adjust the new index (0 = left, 1 = right)
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/move")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> move(String sheetName, int targetIndex, int right) throws GeneralSecurityException, IOException {
        sheetService.moveSheet(profileComponent.getSheetId(), sheetName, targetIndex, right);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sheet moved successfully.");
        return response;
    }

    /**
     * Renames an existing sheet.
     * Endpoint: POST /sheet/rename
     *
     * @param oldName Current name of the sheet
     * @param newName New name for the sheet
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/rename")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> rename(String oldName, String newName) throws GeneralSecurityException, IOException {
        sheetService.renameSheet(profileComponent.getSheetId(), oldName, newName);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sheet renamed successfully.");
        return response;
    }

    /**
     * Removes an existing sheet.
     * Endpoint: POST /sheet/remove
     *
     * @param sheetName Name of the sheet to remove
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/remove")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> remove(String sheetName) throws GeneralSecurityException, IOException {
        sheetService.removeSheet(profileComponent.getSheetId(), sheetName);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sheet removed successfully.");
        return response;
    }

    /**
     * Adds a new sheet and moves it to the specified position.
     * Endpoint: POST /sheet/add
     *
     * @param newSheetName     Name of the new sheet to add
     * @param currentSheetIndex Current index of the sheet from which to add
     * @param right             Direction to add the new sheet (0 = left, 1 = right)
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/add")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> add(String newSheetName, int currentSheetIndex, int right) throws GeneralSecurityException, IOException {
        sheetService.addSheet(profileComponent.getSheetId(), newSheetName);
        sheetService.moveSheet(profileComponent.getSheetId(), newSheetName, currentSheetIndex, right);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Sheet added and moved successfully.");
        return response;
    }

    /**
     * Removes a column from a specific sheet.
     * Endpoint: POST /sheet/column/remove
     *
     * @param sheetName Name of the sheet to remove the column from
     * @param colIndex  Index of the column to remove (0-based)
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/column/remove")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> removeColumn(String sheetName, int colIndex) throws GeneralSecurityException, IOException {
        sheetService.removeColumns(profileComponent.getSheetId(), sheetName, colIndex);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Column removed successfully.");
        return response;
    }

    /**
     * Adds a column to a specific sheet.
     * Endpoint: POST /sheet/column/add
     *
     * @param sheetName Name of the sheet to add the column to
     * @param startIndex Starting index to add the column (0-based)
     * @param right      Direction to add the column (0 = left, 1 = right)
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/column/add")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> addColumn(String sheetName, int startIndex, int right) throws GeneralSecurityException, IOException {
        log.debug("/column/add: {} {} {}", sheetName, startIndex, right);
        sheetService.addColumns(profileComponent.getSheetId(), sheetName, startIndex, right);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Column added successfully."); // Message modified
        return response;
    }

    /**
     * Endpoint to add a row to a specific sheet.
     * Endpoint: POST /sheet/row/add
     *
     * @param sheetName  Name of the sheet to add the row to
     * @param startIndex Starting index of the row to add (0-based)
     * @param below      Direction to add the row (0 = above, 1 = below)
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/row/add")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> addRow(
            @RequestParam String sheetName,
            @RequestParam int startIndex,
            @RequestParam int below) throws GeneralSecurityException, IOException {
        log.debug("/sheet/row/add: sheetName={}, startIndex={}, below={}", sheetName, startIndex, below);
        sheetService.addRows(sheetName, startIndex, below);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Row added successfully.");
        return response;
    }

    /**
     * Endpoint to delete rows from a specific sheet.
     * Endpoint: POST /sheet/row/delete
     *
     * @param sheetName Name of the sheet to delete rows from
     * @param startIndex Starting index of the rows to delete (0-based)
     * @param numRows    Number of rows to delete
     * @return A map containing the status and a success message
     * @throws GeneralSecurityException If there is a security-related error
     * @throws IOException              If there is a network or I/O error
     */
    @PostMapping("/row/delete")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> deleteRow(
            @RequestParam String sheetName,
            @RequestParam int startIndex,
            @RequestParam int numRows) throws GeneralSecurityException, IOException {
        log.debug("/sheet/row/delete: sheetName={}, startIndex={}, numRows={}", sheetName, startIndex, numRows);
        sheetService.deleteRows(sheetName, startIndex, numRows);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Row(s) deleted successfully.");
        return response;
    }

    /**
     * Endpoint to update a specific cell in a sheet.
     * URL: /sheet/updateCell
     * Method: POST
     * Parameters:
     * - sheetName: Name of the sheet
     * - rowIndex: Row index of the cell (0-based)
     * - colIndex: Column index of the cell (0-based)
     * - newValue: New value for the cell
     *
     * @param sheetName Name of the sheet containing the cell
     * @param rowIndex  Row index of the cell (0-based)
     * @param colIndex  Column index of the cell (0-based)
     * @param newValue  New value to set in the cell
     * @return A ResponseEntity containing a map with status and message
     */
    @PostMapping("/updateCell")
    public ResponseEntity<Map<String, Object>> updateCell(
            @RequestParam String sheetName,
            @RequestParam int rowIndex,
            @RequestParam int colIndex,
            @RequestParam String newValue
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            sheetService.updateCell(sheetName, rowIndex, colIndex, newValue);
            response.put("status", "success");
            response.put("message", "Cell updated successfully.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (GeneralSecurityException | IOException e) {
            response.put("status", "error");
            response.put("message", "An internal server error occurred.");
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred.");
            return ResponseEntity.status(500).body(response);
        }
    }

}
