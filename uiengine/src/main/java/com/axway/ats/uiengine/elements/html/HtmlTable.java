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
package com.axway.ats.uiengine.elements.html;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.elements.UiElementProperties;
import com.axway.ats.uiengine.elements.UiTable;

/**
 * An Html Table
 */
@PublicAtsApi
public abstract class HtmlTable extends UiTable {

    public HtmlTable( UiDriver uiDriver,
                      UiElementProperties properties ) {

        super( uiDriver, properties );
    }

    private static String scriptForGettingTableContent;
    static {
        scriptForGettingTableContent = "var table = arguments[0]; var row = \"\"; var col = \"\"; ";
        scriptForGettingTableContent += "var data = new Array(table.rows.length); ";
        scriptForGettingTableContent += "for (row = 0; row < table.rows.length; row++) { ";
        scriptForGettingTableContent += "    data[row] = new Array(table.rows[row].cells.length); ";
        scriptForGettingTableContent += "    for (col = 0; col < table.rows[row].cells.length; col++) { ";
        scriptForGettingTableContent += "        data[row][col] = table.rows[row].cells[col]${inner_html}; ";
        scriptForGettingTableContent += "    } ";
        scriptForGettingTableContent += "} ";
        scriptForGettingTableContent += "return data; ";
    }

    protected String generateScriptForGettingTableContent(
                                                           String innerHtmlToken ) {

        return scriptForGettingTableContent.replace( "${inner_html}", innerHtmlToken );
    }
}
