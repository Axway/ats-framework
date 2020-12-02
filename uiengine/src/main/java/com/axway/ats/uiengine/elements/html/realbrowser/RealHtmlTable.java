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
package com.axway.ats.uiengine.elements.html.realbrowser;

import java.util.List;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.uiengine.AbstractRealBrowserDriver;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.html.HtmlElementLocatorBuilder;
import com.axway.ats.uiengine.elements.html.HtmlTable;
import com.axway.ats.uiengine.exceptions.SeleniumOperationException;
import com.axway.ats.uiengine.exceptions.VerifyEqualityException;
import com.axway.ats.uiengine.exceptions.VerifyNotEqualityException;
import com.axway.ats.uiengine.internal.driver.InternalObjectsEnum;
import com.axway.ats.uiengine.utilities.realbrowser.html.RealHtmlElementState;

/**
 * An HTML Table
 * @see RealHtmlElement
 */
@PublicAtsApi
public class RealHtmlTable extends HtmlTable {

    private WebDriver webDriver;

    public RealHtmlTable( UiDriver uiDriver, UiElementProperties properties ) {

        super(uiDriver, properties);
        String[] matchingRules = properties.checkTypeAndRules(this.getClass().getSimpleName(), "RealHtml",
                                                              RealHtmlElement.RULES_DUMMY);

        // generate the element locator of this HTML element
        String xpath = HtmlElementLocatorBuilder.buildXpathLocator(matchingRules, properties, new String[]{},
                                                                   "table");
        properties.addInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR, xpath);

        webDriver = (WebDriver) ((AbstractRealBrowserDriver) super.getUiDriver()).getInternalObject(InternalObjectsEnum.WebDriver.name());
    }

    /**
     * Get the value of the specified table field
     *
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     * @return the value
     */
    @Override
    @PublicAtsApi
    public String getFieldValue( int row, int column ) {

        new RealHtmlElementState(this).waitToBecomeExisting();

        WebElement table = RealHtmlElementLocator.findElement(this);

        String script = "var table = arguments[0]; var row = arguments[1]; var col = arguments[2];"
                        + "if (row > table.rows.length) { return \"Cannot access row \" + row + \" - table has \" + table.rows.length + \" rows\"; }"
                        + "if (col > table.rows[row].cells.length) { return \"Cannot access column \" + col + \" - table row has \" + table.rows[row].cells.length + \" columns\"; }"
                        + "return table.rows[row].cells[col];";

        Object value = ((JavascriptExecutor) webDriver).executeScript(script, table, row, column);
        if (value instanceof WebElement) {
            return ((WebElement) value).getText().trim();
        }
        return null;
    }

    /**
     * Get the values of all table cells.<br>
     * 
     * <b>Note:</b> If a table cell contains a checkbox - we will return 'checked' or 'notchecked' value.
     * 
     * @return a two dimensional array containing all table cell values
     */
    @PublicAtsApi
    public String[][] getAllValues() {

        new RealHtmlElementState(this).waitToBecomeExisting();

        WebElement table = RealHtmlElementLocator.findElement(this);

        String scriptForHtml = generateScriptForGettingTableContent(".innerHTML");
        Object returnedHtmlValue = ((JavascriptExecutor) webDriver).executeScript(scriptForHtml, table);

        String scriptForObjects = generateScriptForGettingTableContent("");
        Object returnedObjectsValue = ((JavascriptExecutor) webDriver).executeScript(scriptForObjects,
                                                                                     table);

        String[][] tableData = null;
        if (returnedHtmlValue != null && returnedHtmlValue instanceof List && returnedObjectsValue != null
            && returnedObjectsValue instanceof List) {
            List<?> htmlTable = (List<?>) returnedHtmlValue;
            List<?> objectsTable = (List<?>) returnedObjectsValue;

            // allocate space for a number of rows
            tableData = new String[htmlTable.size()][];
            for (int iRow = 0; iRow < htmlTable.size(); iRow++) {
                if (htmlTable.get(iRow) instanceof List) {
                    List<?> htmlRow = (List<?>) htmlTable.get(iRow);
                    List<?> objectsRow = (List<?>) objectsTable.get(iRow);

                    // allocate space for the cells of the current row
                    tableData[iRow] = new String[htmlRow.size()];
                    for (int iColumn = 0; iColumn < htmlRow.size(); iColumn++) {

                        Object htmlWebElement = htmlRow.get(iColumn);
                        Object objectWebElement = objectsRow.get(iColumn);

                        // some data cannot be presented in textual way - for example a checkbox 
                        String htmlValueString = htmlWebElement.toString()
                                                               .toLowerCase()
                                                               .replace("\r", "")
                                                               .replace("\n", "");
                        if (htmlValueString.matches(".*<input.*type=.*[\"|']checkbox[\"|'].*>.*")) {
                            // We assume this is a checkbox inside a table cell.
                            // We will return either 'checked' or 'notchecked'
                            tableData[iRow][iColumn] = htmlValueString.contains("checked")
                                                                                           ? "checked"
                                                                                           : "notchecked";
                        } else {
                            // proceed in the regular way by returning the data visible to the user
                            tableData[iRow][iColumn] = ((RemoteWebElement) objectWebElement).getText();
                        }
                    }
                }
            }
        } else {
            log.warn("We could not get the content of table declared as: " + this.toString());
        }

        return tableData;
    }

    /**
     * Set value of specified table field
     *
     * @param value the new table cell value
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @Override
    @PublicAtsApi
    public void setFieldValue( String value, int row, int column ) {

        new RealHtmlElementState(this).waitToBecomeExisting();

        WebElement table = RealHtmlElementLocator.findElement(this);

        String script = "var table = arguments[0]; var row = arguments[1]; var col = arguments[2];"
                        + "if (row > table.rows.length) { return \"Cannot access row \" + row + \" - table has \" + table.rows.length + \" rows\"; }"
                        + "if (col > table.rows[row].cells.length) { return \"Cannot access column \" + col + \" - table row has \" + table.rows[row].cells.length + \" columns\"; }"
                        + "table.rows[row].cells[col].textContent = '" + value + "';";

        ((JavascriptExecutor) webDriver).executeScript(script, table, row, column);
    }

    /**
     * @return how many rows this table has
     */
    @Override
    @PublicAtsApi
    public int getRowCount() {

        new RealHtmlElementState(this).waitToBecomeExisting();

        String css = this.getElementProperty("_css");

        List<WebElement> elements;

        if (!StringUtils.isNullOrEmpty(css)) {
            css += " tr";
            elements = webDriver.findElements(By.cssSelector(css));
        } else {
            // get elements matching the following xpath
            elements = webDriver.findElements(By.xpath(properties.getInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR)
                                                       + "/tr | "
                                                       + properties.getInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR)
                                                       + "/*/tr"));
        }

        return elements.size();
    }

    /**
     * @return how many columns this table has
     */
    @PublicAtsApi
    public int getColumnCount() {

        new RealHtmlElementState(this).waitToBecomeExisting();

        String css = this.getElementProperty("_css");

        try {
            if (!StringUtils.isNullOrEmpty(css)) {
                StringBuilder sb = new StringBuilder(css);
                sb.append(" tr:nth-child(1) td");
                int count = webDriver.findElements(By.cssSelector(sb.toString())).size();
                sb = new StringBuilder(css);
                sb.append(" tr:nth-child(1) th");
                count += webDriver.findElements(By.cssSelector(sb.toString())).size();
                return count;
            } else {
                // get elements matching the following xpath
                return this.webDriver.findElements(By.xpath("("
                                                            + properties.getInternalProperty(HtmlElementLocatorBuilder.PROPERTY_ELEMENT_LOCATOR)
                                                            + "//tr[th or td])[1]/*"))
                                     .size();
            }
        } catch (Exception e) {
            throw new SeleniumOperationException(this, "getColumnsCount", e);
        }
    }

    /**
     * Verify the field value is as specified
     *
     * @param expectedValue expected value to be checked
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public void verifyFieldValue( String expectedValue, int row, int column ) {

        String actualValue = getFieldValue(row, column);

        if (!actualValue.equals(expectedValue)) {
            throw new VerifyEqualityException(expectedValue, actualValue, this);
        }
    }

    /**
     * Verify the field value is NOT as specified.
     * Currently check is not case-insensitive
     *
     * @param notExpectedValue value which should not match
     * @param row the field row starting at 0
     * @param column the field column starting at 0
     */
    @PublicAtsApi
    public void verifyNotFieldValue( String notExpectedValue, int row, int column ) {

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
     */
    @PublicAtsApi
    public void verifyFieldValueRegex( String expectedValueRegex, int row, int column ) {

        String actualValue = getFieldValue(row, column);

        if (!Pattern.matches(expectedValueRegex, actualValue)) {
            throw new VerifyEqualityException(expectedValueRegex, actualValue, this);
        }
    }
}
