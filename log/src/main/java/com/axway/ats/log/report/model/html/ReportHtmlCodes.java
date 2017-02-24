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
package com.axway.ats.log.report.model.html;

/**
 * Some html tokens used when creating an html report
 */
public class ReportHtmlCodes {

    public static final String NEW_LINE                   = "\r\n";

    public static final String START_PAGE                 = "<html>" + NEW_LINE;
    public static final String END_PAGE                   = "</html>" + NEW_LINE;

    public static final String START_HEAD                 = "<head>" + NEW_LINE;
    public static final String END_HEAD                   = "</head>" + NEW_LINE;

    public static final String START_BODY                 = "<body>" + NEW_LINE;
    public static final String END_BODY                   = "</body>" + NEW_LINE;

    public static final String END_TBL                    = "</table>" + NEW_LINE;

    // table rows
    public static final String START_TBL_ROW_PASSED_RUN   = "<tr class=run_row>" + NEW_LINE;
    public static final String START_TBL_ROW_FAILED_RUN   = "<tr class=failed_run_row>" + NEW_LINE;

    public static final String START_TBL_ROW_FAILED_SUITE = "<tr class=failed_row>" + NEW_LINE;

    public static final String START_TBL_EVENROW          = "<tr class=even_row>" + NEW_LINE;
    public static final String START_TBL_ODDROW           = "<tr class=odd_row>" + NEW_LINE;

    public static final String START_TBL_HEADERROW        = "<tr class=header_row>" + NEW_LINE;

    public static final String EMPTY_TBL_ROW              = "<tr class=empty_row><td></td></tr>" + NEW_LINE;

    public static final String END_TBL_ROW                = "</tr>" + NEW_LINE;

    // table cells
    public static final String START_TBL_CELL             = "<td>";
    public static final String START_TBL_CELL_TWO_ROWS    = "<td rowspan=2>";
    public static final String END_TBL_CELL               = "</td>" + NEW_LINE;
    public static final String EMPTY_TBL_CELL             = START_TBL_CELL + END_TBL_CELL;

    public static final String LINE_BREAK                 = "<br>" + NEW_LINE;
}
