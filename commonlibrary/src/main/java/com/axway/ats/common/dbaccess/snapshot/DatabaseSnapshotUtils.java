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
package com.axway.ats.common.dbaccess.snapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DatabaseSnapshotUtils {

    private static final SimpleDateFormat DATE_FORMAT                = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    // XML nodes used when saving/loading snapshots from files
    public static final String            NODE_DB_SNAPSHOT           = "DB_SNAPSHOT";
    public static final String            ATTR_SNAPSHOT_NAME         = "name";
    public static final String            ATTR_METADATA_TIME         = "metadata_timestamp";
    public static final String            ATTR_CONTENT_TIME          = "content_timestamp";

    public static final String            NODE_TABLE                 = "TABLE";
    public static final String            ATTR_TABLE_NAME            = "name";
    public static final String            ATTR_TABLE_PRIMARY_KEY     = "primaryKey";
    public static final String            ATTR_TABLE_NUMBER_ROWS     = "numberRows";

    public static final String            NODE_SKIP_CONTENT          = "SKIP_CONTENT";
    public static final String            NODE_SKIP_COLUMNS          = "SKIP_COLUMNS";
    public static final String            NODE_SKIP_ROWS             = "SKIP_ROWS";
    public static final String            NODE_SKIP_INDEX_ATTRIBUTES = "SKIP_INDEX_ATTRIBUTES";
    public static final String            ATT_SKIP_RULE_TABLE        = "table";

    public static final String            NODE_COLUMN_DESCRIPTIONS   = "column_descriptions";
    public static final String            NODE_COLUMN_DESCRIPTION    = "column_description";

    public static final String            NODE_INDEXES               = "indexes";
    public static final String            NODE_INDEX                 = "index";
    public static final String            ATTR_NODE_INDEX_NAME       = "name";

    public static final String            NODE_ROW                   = "row";

    public static String dateToString(
                                       long timeInMillis ) {

        return DATE_FORMAT.format(new Date(timeInMillis));
    }

    public static long stringToDate(
                                     String timeString ) {

        try {
            return DATE_FORMAT.parse(timeString).getTime();
        } catch (ParseException e) {
            throw new DatabaseSnapshotException("Cannot parse date '" + timeString + "'");
        }
    }

    public static List<Element> getChildrenByTagName(
                                                      Node parent,
                                                      String name ) {

        List<Element> nodeList = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }

        return nodeList;
    }
}
