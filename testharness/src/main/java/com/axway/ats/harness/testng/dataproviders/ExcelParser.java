/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.harness.testng.dataproviders;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import com.axway.ats.harness.testng.exceptions.DataProviderException;

/**
 *
 * Utility Class Responsible for parsing the Excel Files.
 *
 */
public class ExcelParser {

    // Class indicating that the given object from the data table should be skipped
    private class SkipObject {}

    /** The {@link String} used to indicate the beginning of the test case data */
    public static final String  TEST_CASE_START              = "TEST_CASE_START";

    /** The {@link String} used to indicate the end of the test case data */
    public static final String  TEST_CASE_END                = "TEST_CASE_END";

    /** The {@link String} used to indicate that the table rows are to be returned in Cartesian product */
    public static final String  MULTIPLY                     = "MULTIPLY";

    Logger                      log                          = LogManager.getLogger(ExcelParser.class);

    private static final String ERROR_LOCATING_STARTING_CELL = "Unable to find test data starting cell. Such should have a comment containing "
                                                               + TEST_CASE_START;

    private static final String ERROR_LOCATING_ENDING_CELL   = "Unable to find test data ending cell. Such should have a comment containing "
                                                               + TEST_CASE_END;

    private static final String UNABLE_TO_LOAD_DATA          = "The data provider was unable to load the test case data";

    private static final String DUPLICATE_END_CELL           = "Duplicate " + TEST_CASE_END + " comments";

    private static final String DUPLICATE_START_CELL         = "Duplicate " + TEST_CASE_START + " comments";

    private static final String UNABLE_TO_LOAD_SHEETS        = "Unable to load sheet by the name of ";

    private static final String FOUND_STARTING_CELL          = "Succefully found a starting cell";

    private static final String FOUND_ENDING_CELL            = "Succefully found an ending cell";

    private static final String FOUND_MULTIPLY_CELL          = "Succefully found a multiply cell";

    private static final String CREATING_DATA_BLOCK          = "Creating a data block with dimensions ";

    private static final String CREATING_MULTIPLY_DATA_BLOCK = "Creating a multiplied data block with dimensions ";

    private static final String WRONG_ORDER                  = "The " + TEST_CASE_START + " and "
                                                               + TEST_CASE_END + " tags are in wrong order";

    public static final String  STRING_TOKEN                 = "STRING";

    private static final String STRING_NULL                  = "NULL";

    // indicates that the data table will be a Cartesian product of the rows (their not empty cells)
    private boolean             isMultipliable               = false;

    private Workbook            excelFileWorkbook            = null;

    private String              sheetName;

    private Cell                startingCell                 = null;

    private Cell                endingCell                   = null;

    // in the form of [row][column]
    private Object[][]          workingObjectArray           = null;

    /**
     * Constructs a new instance of the {@link ExcelParser}
     *
     * @param excelFileInputStream {@link InputStream} to the excel file
     * @param sheetName the name of the excel sheet to use
     * @throws DataProviderException
     */
    public ExcelParser( InputStream excelFileInputStream,
                        String sheetName ) throws DataProviderException {

        try {
            this.excelFileWorkbook = WorkbookFactory.create(excelFileInputStream);
            this.sheetName = sheetName;
        } catch (IOException e) {
            throw new DataProviderException(UNABLE_TO_LOAD_DATA, e);
        }
    }

    /**
     * Sets the current {@link Sheet} that would be used to load the test case data.
     *
     * @param newSheetName The name of the {@link Sheet}
     */
    public void setSheetName(
                              String newSheetName ) {

        if (this.sheetName != null && !this.sheetName.equals(newSheetName)) {
            this.sheetName = newSheetName;
            this.workingObjectArray = null;
            this.startingCell = null;
            this.endingCell = null;
        }
    }

    /**
     * Returns the test case data in the form of a two dimensional Object array. The method makes sure the information
     * is loaded only the first time it is called.
     * @param method the test method which parameters will be used to determine the excel column data type.
     * @return Object[][] containing the test data.
     * @throws DataProviderException
     */
    public Object[][] getDataBlock(
                                    Method method ) throws DataProviderException {

        if (this.workingObjectArray == null) {
            try {
                loadDataBlock(method);
            } catch (Exception e) {
                throw new DataProviderException(UNABLE_TO_LOAD_DATA, e);
            }
        }

        return this.workingObjectArray;
    }

    /**
     * Parses the test case data (the data between TEST_CASE_START and TEST_CASE_END commented cells) from the
     * excel spreadsheet and loads it into a two-dimensional Object[][] array - workingObjectArray
     */
    private void loadDataBlock(
                                Method method ) throws DataProviderException {

        Sheet sheet = excelFileWorkbook.getSheet(this.sheetName);
        if (sheet == null) {
            throw new DataProviderException(UNABLE_TO_LOAD_SHEETS + this.sheetName);
        }

        //Get the starting cell coordinates
        Cell startingCell = getStartingCell(sheet);
        int startCol = startingCell.getColumnIndex();
        int startRow = startingCell.getRowIndex();

        //Get the ending cell coordinates
        Cell endingCell = getEndingCell(sheet);
        int endCol = endingCell.getColumnIndex();
        int endRow = endingCell.getRowIndex();

        if (method.getParameterTypes().length != (endCol - startCol) + 1) {
            throw new DataProviderException(" Expected " + method.getParameterTypes().length
                                            + " parameters in the test method while the table has "
                                            + ( (endCol - startCol) + 1));
        }
        // If the data table is to be returned as a Cartesian product of the rows (the not empty cells)
        if (isMultipliable) {
            makeCartesianProductTable(startCol, startRow, endCol, endRow, sheet, method);
        } else {
            // Initialize the object array
            int columns = endCol - startCol + 1;
            int rows = endRow - startRow + 1;
            this.log.debug(CREATING_DATA_BLOCK + columns + "/" + rows);
            this.workingObjectArray = new Object[rows][columns];

            //Fill the object array iterating the sheet column by column
            for (int col = startCol, parameterIndex = 0; col <= endCol; col++, parameterIndex++) {

                //Get the type of the method parameter at the current position
                Class<?> parameterType = getParameterTypeAt(method, parameterIndex);

                //Iterate over the current column to load the cells according to the parameter type
                for (int row = startRow; row <= endRow; row++) {
                    Row rowValue = sheet.getRow(row);
                    if (rowValue != null) {
                        Cell currentCell = rowValue.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        this.workingObjectArray[row - startRow][parameterIndex] = parseCellContents(currentCell,
                                                                                                    parameterType);
                    }
                }
            }
        }
    }

    private void makeCartesianProductTable(
                                            int startCol,
                                            int startRow,
                                            int endCol,
                                            int endRow,
                                            Sheet sheet,
                                            Method method ) throws DataProviderException {

        // List of columns
        ArrayList<ArrayList<Object>> productTable = new ArrayList<ArrayList<Object>>();

        // Read the data sheet and fill the list of columns
        for (int x = 0, col = startCol; col <= endCol; ++col, ++x) {
            productTable.add(new ArrayList<Object>());

            Class<?> parameterType = getParameterTypeAt(method, x);

            for (int row = startRow; row <= endRow; ++row) {
                Row rowValue = sheet.getRow(row);
                if (rowValue != null) {
                    Cell currentCell = rowValue.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    Object currentObject = parseCellContents(currentCell, parameterType);

                    if (! (currentObject instanceof SkipObject)) {
                        productTable.get(x).add(currentObject);
                    }
                }
            }
        }

        // Multiplication:
        int fields = productTable.size();

        int[] counts = new int[fields];

        int totalRows = 1;

        for (int i = 0; i < fields; i++) {
            counts[i] = productTable.get(i).size();
            totalRows *= counts[i];
        }

        int columns = endCol - startCol + 1;

        this.workingObjectArray = new Object[totalRows][columns];

        this.log.debug(CREATING_MULTIPLY_DATA_BLOCK + columns + "/" + totalRows);

        // Fill the new data table
        for (int t = 0; t < totalRows; t++) { // iterate through all the rows that will be in the new table after multiplication
            ArrayList<Object> buff = new ArrayList<Object>();

            for (int x = 0; x < fields; x++) { // iterate through the columns of the extracted table
                int pos = t;

                // calculating the column index of the element to be added in the current row
                for (int y = 0; y < x; y++) {
                    pos /= counts[y];
                }
                pos %= counts[x];

                Object[] members = productTable.get(x).toArray(); // get the current column
                Object member = members[pos]; // get the appropriate element from the column

                buff.add(member);
            }

            this.workingObjectArray[t] = buff.toArray(); // add the row to the resulting table
        }
    }

    /**
     * Reads the cell content and try to convert it to the specified Java type
     * @param cell the cell which content will be parsed
     * @param methodParameterType the type to which the cell content should be converted
     * @return Object of type methodParameterType, null if the cell contents equals "NULL" or SkipObject if ...
     */
    private Object parseCellContents(
                                      Cell cell,
                                      Class<?> methodParameterType ) throws DataProviderException {

        String cellValue = "";
        if (!methodParameterType.equals(Date.class)) {
            cell.setCellType(CellType.STRING);
            cellValue = cell.getStringCellValue();
        }

        // This was left for backward compatibility with the previous approach where
        // the type was determined based on cell comments and type instead of the method parameter type
        if (hasComments(cell) && checkCellComment(cell, STRING_TOKEN)) {
            if (methodParameterType.equals(String.class)) {
                return cellValue;
            } else {
                throw new DataProviderException("The cell has 'STRING' comment but the method parameter is of type '"
                                                + methodParameterType.getSimpleName());
            }
        }

        // Return null if the cell value is "NULL"
        if (cellValue.equalsIgnoreCase(STRING_NULL)) {
            if (methodParameterType.isPrimitive()) {
                throw new DataProviderException("Can not pass 'null' to parameter of primitive type '"
                                                + methodParameterType.getSimpleName() + "'");
            } else {
                return null;
            }
        }

        if (isMultipliable) {
            // In the Cartesian product mode indicate that empty cells which don't have the STRING_TOKEN comment
            // will be skipped
            if (! (hasComments(cell) && checkCellComment(cell, STRING_TOKEN)) && "".equals(cellValue)) {
                return new SkipObject();
            }
        }

        // Try to convert the cell value to the methodParameterType Java type
        try {

            // DATE
            if (methodParameterType.equals(Date.class)) {

                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    throw new DataProviderException("Can not parse '"
                                                    + cell.getStringCellValue()
                                                    + "' as a valid date as the cell is not formatted as type DATE.");
                }

            }

            // BYTE
            if (methodParameterType.equals(byte.class) || methodParameterType.equals(Byte.class)) {

                return Byte.parseByte(cellValue.trim());
            }

            // CHAR
            if (methodParameterType.equals(char.class) || methodParameterType.equals(Character.class)) {
                if (cellValue.length() > 1) {
                    throw new ParseException("The value '" + cellValue
                                             + "' can't be converted to Char as it is longer than 1.", 2);
                }
                return cellValue.charAt(0);
            }

            // SHORT
            if (methodParameterType.equals(short.class) || methodParameterType.equals(Short.class)) {

                return Short.parseShort(cellValue.trim());
            }

            // INT
            if (methodParameterType.equals(int.class) || methodParameterType.equals(Integer.class)) {

                return Integer.parseInt(cellValue.trim());
            }
            // LONG
            if (methodParameterType.equals(long.class) || methodParameterType.equals(Long.class)) {

                return Long.parseLong(cellValue.trim());
            }

            //FLOAT
            if (methodParameterType.equals(float.class) || methodParameterType.equals(Float.class)) {

                return Float.parseFloat(cellValue.trim().replace(',', '.'));

            }
            //DOUBLE
            if (methodParameterType.equals(double.class) || methodParameterType.equals(Double.class)) {

                return Double.parseDouble(cellValue.trim().replace(',', '.'));

            }

            //BOOLEAN
            if (methodParameterType.equals(boolean.class) || methodParameterType.equals(Boolean.class)) {

                if (cellValue.trim().equalsIgnoreCase(Boolean.TRUE.toString())
                    || cellValue.trim().equalsIgnoreCase(Boolean.FALSE.toString())) {
                    return Boolean.parseBoolean(cellValue.trim());
                } else {
                    throw new ParseException("The value '" + cellValue.trim()
                                             + "' can't be converted to type Boolean.", 1);
                }

            }

            //NUMBER - If the parameter type is Number then convert the value to Double
            if (methodParameterType.equals(Number.class)) {

                return Double.parseDouble(cellValue.trim().replace(',', '.'));
            }

        } catch (NumberFormatException nfe) {
            throw new DataProviderException(nfe);
        } catch (ParseException pe) {
            throw new DataProviderException(pe);
        }

        // The method parameter type was different from the examined types so return the String value of the cell
        return cellValue;

    }

    private Cell getStartingCell(
                                  Sheet sheet ) throws DataProviderException {

        if (this.startingCell == null) {
            determineTestDataFrame(sheet);
        }

        return this.startingCell;
    }

    private Cell getEndingCell(
                                Sheet sheet ) throws DataProviderException {

        if (this.endingCell == null) {
            determineTestDataFrame(sheet);
        }

        return this.endingCell;
    }

    /**
     * Iterates through the sheet and determines the cells that are between START_TEST_CASE and END_TEST_CASE comments.
     * Also determines if the data table is to be returned as a Cartesian product of the rows.
     * @param sheet
     * @throws DataProviderException
     */
    private void determineTestDataFrame(
                                         Sheet sheet ) throws DataProviderException {

        int rows = sheet.getLastRowNum() + 1;
        int columns = sheet.getRow(sheet.getLastRowNum()).getLastCellNum();
        // iterate throughout the spreadsheet's cells
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Row rowValue = sheet.getRow(y);
                if (rowValue != null) {

                    if (rowValue.getLastCellNum() > columns) {
                        columns = rowValue.getLastCellNum();
                    }

                    Cell current = rowValue.getCell(x, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    if (hasComments(current)) {
                        if (isStartingCell(current)) {
                            this.startingCell = current;
                        }
                        if (isEndingCell(current)) {
                            this.endingCell = current;
                        }
                        if (isMultiplyCell(current)) {
                            this.isMultipliable = true;
                        }
                    }
                }
            }
        }

        if (this.startingCell == null) {
            throw new DataProviderException(ERROR_LOCATING_STARTING_CELL);
        } else if (this.endingCell == null) {
            throw new DataProviderException(ERROR_LOCATING_ENDING_CELL);
        }

        if (this.startingCell.getRowIndex() <= this.endingCell.getRowIndex()) {
            if (this.startingCell.getColumnIndex() <= this.endingCell.getColumnIndex()) {
                return;
            }
        }

        throw new DataProviderException(WRONG_ORDER);
    }

    private boolean isStartingCell(
                                    Cell cell ) throws DataProviderException {

        if (checkCellComment(cell, TEST_CASE_START)) {
            if (this.startingCell == null) {
                this.log.debug(FOUND_STARTING_CELL);
                return true;
            }

            throw new DataProviderException(DUPLICATE_START_CELL);
        }

        return false;
    }

    private boolean isEndingCell(
                                  Cell cell ) throws DataProviderException {

        if (checkCellComment(cell, TEST_CASE_END)) {
            if (this.endingCell == null) {
                this.log.debug(FOUND_ENDING_CELL);
                return true;
            }
            throw new DataProviderException(DUPLICATE_END_CELL);
        }

        return false;
    }

    private boolean isMultiplyCell(
                                    Cell current ) {

        if (checkCellComment(current, MULTIPLY)) {
            if (!isMultipliable) {
                this.log.debug(FOUND_MULTIPLY_CELL);
                return true;
            }
        }

        return false;
    }

    private boolean hasComments(
                                 Cell cell ) {

        if (cell != null && cell.getCellComment() != null) {
            if (cell.getCellComment() != null) {
                return true;
            }
        }

        return false;
    }

    private boolean checkCellComment(
                                      Cell cell,
                                      String comment ) {

        if (cell.getCellComment().getString().toString().contains(comment)) {
            return true;
        }

        return false;
    }

    private Class<?> getParameterTypeAt(
                                         Method method,
                                         int parameterIndex ) {

        return method.getParameterTypes()[parameterIndex];
    }

}
