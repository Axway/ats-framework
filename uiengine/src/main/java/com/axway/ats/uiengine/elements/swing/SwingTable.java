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
package com.axway.ats.uiengine.elements.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JTable;

import org.fest.swing.data.TableCell;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTableHeaderFixture;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiTable;
import com.axway.ats.uiengine.exceptions.NotSupportedOperationException;
import com.axway.ats.uiengine.exceptions.UiElementException;
import com.axway.ats.uiengine.exceptions.VerificationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.utilities.swing.SwingElementState;

/**
 * A Swing Table
 * <p>
 * Can be identified by:
 * <li>name
 * <li>label
 * </p>
 */
@PublicAtsApi
public class SwingTable extends UiTable {

    private static final String[] RULES = { "label,visible", "label", "name,visible", "name", "index" };

    static {
        SwingElementLocator.componentsMap.put(SwingTable.class, JTable.class);
    }

    public SwingTable( UiDriver uiDriver,
                       UiElementProperties properties ) {

        super(uiDriver, properties);
        checkTypeAndRules("Swing", RULES);
    }

    /**
     * Set table field value
     *
     * @param value the value to set
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public void setFieldValue(
                               String value,
                               int row,
                               int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        try {

            TableCell tableCell = new TableCell(row, column) {};
            tableFixture.selectCell(tableCell); // if the cell coordinates are wrong, the exception will be thrown
            if (tableFixture.component().isCellEditable(row, column)) {

                tableFixture.enterValue(tableCell, value);
            } else {

                throw new NotSupportedOperationException("The table cell [" + row + "," + column
                                                         + "] is not editable. " + toString());
            }
        } catch (IndexOutOfBoundsException ioobe) {

            throw new UiElementException(ioobe.getMessage(), this);
        }
    }

    /**
     * Get table field value
     *
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the element doesn't exist
     */
    @Override
    @PublicAtsApi
    public String getFieldValue(
                                 int row,
                                 int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        try {
            return ((JTableFixture) SwingElementLocator.findFixture(this)).valueAt(new TableCell(row,
                                                                                                 column) {});
        } catch (IndexOutOfBoundsException ioobe) {

            throw new UiElementException(ioobe.getMessage(), this);
        }
    }

    /**
     * Get table row count
     *
     * @throws VerificationException if the table element doesn't exist
     */
    @Override
    @PublicAtsApi
    public int getRowCount() {

        new SwingElementState(this).waitToBecomeExisting();

        return ((JTableFixture) SwingElementLocator.findFixture(this)).rowCount();
    }

    /**
     * Get table column count
     *
     * @return how many columns this table has
     * @throws VerificationException if the table element doesn't exist
     */
    @Override
    @PublicAtsApi
    public int getColumnCount() {

        throw new NotSupportedOperationException("Not implemented");
    }

    /**
     * Verify the field value is as specified
     *
     * @param expectedValue
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyEqualityException if the verification fails
     */
    @PublicAtsApi
    public void verifyFieldValue(
                                  String expectedValue,
                                  int row,
                                  int column ) {

        String actualValue = getFieldValue(row, column);

        if (!actualValue.equals(expectedValue)) {
            throw new VerifyEqualityException(expectedValue, actualValue, this);
        }
    }

    /**
     * Verify the field value is NOT as specified
     *
     * @param notExpectedValue
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyNotEqualityException if the verification fails
     */
    @PublicAtsApi
    public void verifyNotFieldValue(
                                     String notExpectedValue,
                                     int row,
                                     int column ) {

        String actualValue = getFieldValue(row, column);

        if (actualValue.equals(notExpectedValue)) {
            throw new VerifyNotEqualityException(notExpectedValue, this);
        }
    }

    /**
     * Verify the field value matches the specified java regular expression
     *
     * @param expectedValueRegex a java regular expression
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     * @throws VerificationException if the element doesn't exist
     * @throws VerifyEqualityException if the verification fails
     */
    @PublicAtsApi
    public void verifyFieldValueRegex(
                                       String expectedValueRegex,
                                       int row,
                                       int column ) {

        String actualValue = getFieldValue(row, column);

        if (!Pattern.matches(expectedValueRegex, actualValue)) {
            throw new VerifyEqualityException(expectedValueRegex, actualValue, this);
        }
    }

    /**
     * Click table cell
     *
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void clickCell(
                           int row,
                           int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            tableFixture.cell(new TableCell(row, column) {}).click();
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Double click table cell
     *
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void doubleClickCell(
                                 int row,
                                 int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            tableFixture.cell(new TableCell(row, column) {}).doubleClick();
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Right click on table cell
     *
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void rightClickCell(
                                int row,
                                int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            tableFixture.cell(new TableCell(row, column) {}).rightClick();
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Get table cell coordinates by cell value
     *
     * @param value cell value to search for
     * @param isRegEx if the value is a regular expression
     * @return an {@link ArrayList} with cell coordinates(indexes) represented by arrays [ row, column ] which contains
     *  the searched value
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public List<Integer[]> getCellIndexesByValue(
                                                  String value,
                                                  boolean isRegEx ) {

        new SwingElementState(this).waitToBecomeExisting();

        List<Integer[]> results = new ArrayList<Integer[]>();
        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            if (value == null) {
                isRegEx = false;
            }
            Pattern regexPattern = null;
            if (isRegEx) {
                regexPattern = Pattern.compile(value);
            }

            for (int row = 0; row < tableFixture.target.getRowCount(); row++) {
                for (int column = 0; column < tableFixture.target.getColumnCount(); column++) {

                    String cellValue = null;
                    try {
                        cellValue = tableFixture.valueAt(new TableCell(row, column) {});
                    } catch (NullPointerException npe) {
                        // valueAt() throws NPE if the cell is null
                    }
                    if (cellValue == null && value != null) {
                        continue;
                    }
                    if ( (cellValue == null && value == null)
                         || (isRegEx && regexPattern.matcher(cellValue).matches())
                         || (!isRegEx && cellValue.equals(value))) {

                        results.add(new Integer[]{ row, column });
                    }
                }
            }

        } catch (Exception e) {

            throw new UiElementException(e.getMessage(), this);
        }
        return results;
    }

    /**
     * Select table cell
     *
     * @param row the row number
     * @param column the column number
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void selectCell(
                            int row,
                            int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            tableFixture.selectCell(new TableCell(row, column) {});
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Select table cells
     *
     * @param cells the cells coordinates (eg. new int[][]{ { 1, 1 }, { 1, 2 }, { 2, 2 } )
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void selectCells(
                             int[][] cells ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        try {

            TableCell[] cellsToSelect = new TableCell[cells.length];
            for (int i = 0; i < cells.length; i++) {
                int row = cells[i][0];
                int column = cells[i][1];
                cellsToSelect[i] = new TableCell(row, column) {};
            }
            tableFixture.selectCells(cellsToSelect);
        } catch (Exception e) {

            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Select table rows
     *
     * @param rows row numbers to select
     * @throws VerificationException if the element doesn't exist
     */
    @PublicAtsApi
    public void selectRow(
                           int... rows ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            tableFixture.selectRows(rows);
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Get selected rows in the table
     *
     * @return an array with the selected rows
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public int[] getSelectedRows() {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);

        try {
            return tableFixture.component().getSelectedRows();
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Click table header by column index
     *
     * @param columnIndex the column index
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void clickHeader(
                             int columnIndex ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        JTableHeaderFixture tableHeaderFixture = tableFixture.tableHeader();

        try {
            tableHeaderFixture.clickColumn(columnIndex);
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Click table header by column name
     *
     * @param columnName the column name
     * @throws VerificationException if the table element doesn't exist
     */
    @PublicAtsApi
    public void clickHeader(
                             String columnName ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        JTableHeaderFixture tableHeaderFixture = tableFixture.tableHeader();

        try {
            tableHeaderFixture.clickColumn(columnName);
        } catch (Exception e) {
            throw new UiElementException(e.getMessage(), this);
        }
    }

    /**
     * Simulates a user dragging a cell from this table
     *
     * @param row the row number
     * @param column the column number
     */
    @PublicAtsApi
    public void drag(
                      int row,
                      int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTableFixture) SwingElementLocator.findFixture(this)).drag(new TableCell(row, column) {});
    }

    /**
     * Simulates a user dropping an item into a specific table cell
     *
     * @param row the row number
     * @param column the column number
     */
    @PublicAtsApi
    public void drop(
                      int row,
                      int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        ((JTableFixture) SwingElementLocator.findFixture(this)).drop(new TableCell(row, column) {});
    }

    /**
     * Returns the table cell background {@link Color}
     *
     * @param row the row number
     * @param column the column number
     */
    @PublicAtsApi
    public Color getCellBackgroundColor(
                                         int row,
                                         int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        return tableFixture.backgroundAt(new TableCell(row, column) {}).target();
    }

    /**
     * Gets table cell backgrounds (as {@link Color}) of all table cells.
     *
     * @return array of java.awt.Color objects one for each cell. First index is
     * table row and second is the column in this row.
     */
    @PublicAtsApi
    public Color[][] getCellBackgroundColors() {

        new SwingElementState(this).waitToBecomeExisting();

        final JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        int rowCount = tableFixture.rowCount();
        // SwingUtilities.
        int columnCount = GuiActionRunner.execute(new GuiQuery<Integer>() {

            @Override
            protected Integer executeInEDT() throws Throwable {

                return tableFixture.component().getColumnCount();
            }

        });
        Color[][] resultArr = new Color[rowCount][columnCount];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                resultArr[i][j] = tableFixture.backgroundAt(new TableCell(i, j) {}).target();
            }
        }
        return resultArr;
    }

    /**
     * Returns the table cell foreground {@link Color}
     *
     * @param row the row number
     * @param column the column number
     */
    @PublicAtsApi
    public Color getCellForegroundColor(
                                         int row,
                                         int column ) {

        new SwingElementState(this).waitToBecomeExisting();

        JTableFixture tableFixture = (JTableFixture) SwingElementLocator.findFixture(this);
        return tableFixture.foregroundAt(new TableCell(row, column) {}).target();
    }

}
