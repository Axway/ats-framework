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
package com.axway.ats.log.report.model.html.css;

import static com.axway.ats.log.report.model.html.ReportHtmlCodes.*;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.log.report.model.html.css.FontStyle;
import com.axway.ats.log.report.model.html.css.FontWeight;
import com.axway.ats.log.report.model.html.css.TextHorizontalAlign;
import com.axway.ats.log.report.model.html.css.TextVerticalAlign;

/**
 * CSS definitions for the different tables rows we use when creating an html report
 */
public class CssSettings {

    private static TableRowCssSettings       headerRow;

    private static TableRowCssSettings       oddRow;
    private static TableRowCssSettings       evenRow;
    private static TableRowCssSettings       failedRow;

    private static TableRowCssSettings       runRow;
    private static TableRowCssSettings       failedRunRow;

    private static TableRowCssSettings       emptyRow;

    private static List<TableRowCssSettings> rows = new ArrayList<TableRowCssSettings>();

    static {
        // header row
        headerRow = new TableRowCssSettings("header_row");
        headerRow.setFontFamily("Verdana, Arial");
        headerRow.setFontSize(13);
        headerRow.setFontStyle(FontStyle.NORMAL);
        headerRow.setFontWeight(FontWeight.BOLD);

        headerRow.setTextColor("#000000");
        headerRow.setBackgroundColor("#DBDEC7");

        headerRow.setTextHorizontalAlign(TextHorizontalAlign.CENTER);
        headerRow.setTextVerticalAlign(TextVerticalAlign.MIDDLE);
        rows.add(headerRow);

        // odd row
        oddRow = new TableRowCssSettings("odd_row");
        oddRow.setFontFamily("Verdana, Arial");
        oddRow.setFontSize(11);
        oddRow.setFontStyle(FontStyle.NORMAL);
        oddRow.setFontWeight(FontWeight.NORMAL);

        oddRow.setTextColor("#000000");
        oddRow.setBackgroundColor("#F7F9EE");

        oddRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        oddRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(oddRow);

        // even row
        evenRow = new TableRowCssSettings("even_row");
        evenRow.setFontFamily("Verdana, Arial");
        evenRow.setFontSize(11);
        evenRow.setFontStyle(FontStyle.NORMAL);
        evenRow.setFontWeight(FontWeight.NORMAL);

        evenRow.setTextColor("#000000");
        evenRow.setBackgroundColor("#ECEDE1");

        evenRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        evenRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(evenRow);

        // failed row
        failedRow = new TableRowCssSettings("failed_row");
        failedRow.setFontFamily("Verdana, Arial");
        failedRow.setFontSize(11);
        failedRow.setFontStyle(FontStyle.NORMAL);
        failedRow.setFontWeight(FontWeight.NORMAL);

        failedRow.setTextColor("#000000");
        failedRow.setBackgroundColor("#FFAAAA");

        failedRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        failedRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(failedRow);

        // run row
        runRow = new TableRowCssSettings("run_row");
        runRow.setFontFamily("Verdana, Arial");
        runRow.setFontSize(13);
        runRow.setFontStyle(FontStyle.NORMAL);
        runRow.setFontWeight(FontWeight.NORMAL);

        runRow.setTextColor("#000000");
        runRow.setBackgroundColor("#CCFFCC");

        runRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        runRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(runRow);

        // failed run row
        failedRunRow = new TableRowCssSettings("failed_run_row");
        failedRunRow.setFontFamily("Verdana, Arial");
        failedRunRow.setFontSize(13);
        failedRunRow.setFontStyle(FontStyle.NORMAL);
        failedRunRow.setFontWeight(FontWeight.NORMAL);

        failedRunRow.setTextColor("#000000");
        failedRunRow.setBackgroundColor("#FFAAAA");

        failedRunRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        failedRunRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(failedRunRow);

        // empty row
        emptyRow = new TableRowCssSettings("empty_row");
        emptyRow.setFontFamily("Verdana, Arial");
        emptyRow.setFontSize(13);
        emptyRow.setFontStyle(FontStyle.NORMAL);
        emptyRow.setFontWeight(FontWeight.NORMAL);

        emptyRow.setTextColor("#000000");
        emptyRow.setBackgroundColor("#FFFFFF");

        emptyRow.setTextHorizontalAlign(TextHorizontalAlign.LEFT);
        emptyRow.setTextVerticalAlign(TextVerticalAlign.TOP);
        rows.add(emptyRow);
    }

    /**
     * Get all supported CSS styles as html text
     * @return
     */
    public static String getCssStyle() {

        StringBuilder sb = new StringBuilder();

        sb.append("<style type=\"text/css\">" + NEW_LINE);
        sb.append("<!--" + NEW_LINE);
        for (TableRowCssSettings row : rows) {
            sb.append(row.toHtml() + NEW_LINE);
        }
        sb.append("--></style>" + NEW_LINE);

        return sb.toString();
    }
}
