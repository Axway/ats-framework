/*
 * Copyright 2017-2020 Axway Software
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
package com.axway.ats.action.dbaccess.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.action.dbaccess.snapshot.rules.SkipColumns;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipContent;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipIndexAttributes;
import com.axway.ats.action.dbaccess.snapshot.rules.SkipRows;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotException;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Used to read/save database data from/to a backup file
 */
class DatabaseSnapshotBackupUtils {

    private static Logger log = LogManager.getLogger(DatabaseSnapshotBackupUtils.class);

    /**
     * Save a snapshot into a file
     * @param snapshot the snapshot to save
     * @param backupFile the backup file name
     * @return the XML document
     */
    public Document saveToFile( DatabaseSnapshot snapshot, String backupFile ) {

        log.info("Save database snapshot into file " + backupFile + " - START");

        // create the directory if does not exist
        File dirPath = new File(IoUtils.getFilePath(backupFile));
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new DatabaseSnapshotException("Error creating DOM parser for " + backupFile, e);
        }

        // TODO - add DTD or schema for manual creation and easy validation
        Element dbNode = doc.createElement(DatabaseSnapshotUtils.NODE_DB_SNAPSHOT);
        dbNode.setAttribute(DatabaseSnapshotUtils.ATTR_SNAPSHOT_NAME, snapshot.name);
        // the timestamp comes when user takes a snapshot from database
        dbNode.setAttribute(DatabaseSnapshotUtils.ATTR_METADATA_TIME,
                            DatabaseSnapshotUtils.dateToString(snapshot.metadataTimestamp));
        // the timestamp now
        snapshot.contentTimestamp = System.currentTimeMillis();
        dbNode.setAttribute(DatabaseSnapshotUtils.ATTR_CONTENT_TIME,
                            DatabaseSnapshotUtils.dateToString(snapshot.contentTimestamp));
        doc.appendChild(dbNode);

        // append all table data
        for (TableDescription tableDescription : snapshot.tables) {
            Element tableNode = doc.createElement(DatabaseSnapshotUtils.NODE_TABLE);

            // append table meta data
            dbNode.appendChild(tableNode);
            tableDescription.toXmlNode(doc, tableNode);

            // check if table content is to be skipped
            SkipContent skipTableContentOption = snapshot.skipContentPerTable.get(tableDescription.getName()
                                                                                                  .toLowerCase());
            if (skipTableContentOption != null) {
                // we skip the table content
                if (skipTableContentOption.isRememberNumberOfRows()) {
                    // ... but we want to persist the number of rows
                    int numberRows = snapshot.loadTableLength(snapshot.name, tableDescription, null, null);
                    tableNode.setAttribute(DatabaseSnapshotUtils.ATTR_TABLE_NUMBER_ROWS,
                                           String.valueOf(numberRows));
                }

                continue;
            }

            // append table content
            List<String> valuesList = snapshot.loadTableData(snapshot.name, tableDescription,
                                                             snapshot.skipColumnsPerTable,
                                                             snapshot.skipRowsPerTable, null, null);
            for (String values : valuesList) {
                Element rowNode = doc.createElement(DatabaseSnapshotUtils.NODE_ROW);
                rowNode.setTextContent(StringUtils.escapeNonPrintableAsciiCharacters(values));

                tableNode.appendChild(rowNode);
            }
        }

        // append any skip table content rules
        for (SkipContent skipContent : snapshot.skipContentPerTable.values()) {
            skipContent.toXmlNode(doc, dbNode);
        }

        // append any skip table column rules
        for (SkipColumns skipColumns : snapshot.skipColumnsPerTable.values()) {
            skipColumns.toXmlNode(doc, dbNode);
        }

        // append any skip index attribute rules
        for (SkipIndexAttributes skipIndexAttributes : snapshot.skipIndexAttributesPerTable.values()) {
            skipIndexAttributes.toXmlNode(doc, dbNode);
        }

        // append any skip table row rules
        for (SkipRows skipRows : snapshot.skipRowsPerTable.values()) {
            skipRows.toXmlNode(doc, dbNode);
        }

        // save the XML file
        OutputStream fos = null;
        try {
            OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(4);
            format.setLineWidth(1000);

            fos = new FileOutputStream(new File(backupFile));
            XMLSerializer serializer = new XMLSerializer(fos, format);

            serializer.serialize(doc);
        } catch (Exception e) {
            throw new DatabaseSnapshotException("Error saving " + backupFile, e);
        } finally {
            IoUtils.closeStream(fos, "Error closing IO stream to file used for database snapshot backup "
                                     + backupFile);
        }

        log.info("Save database snapshot into file " + backupFile + " - END");

        return doc;
    }

    /**
     * Load a snapshot from a file
     * 
     * @param newSnapshotName the name of the new snapshot
     * @param snapshot the snapshot instance to fill with new data
     * @param sourceFile the backup file name
     * @return the XML document
     */
    public Document loadFromFile( String newSnapshotName, DatabaseSnapshot snapshot, String sourceFile ) {

        log.info("Load database snapshot from file " + sourceFile + " - START");

        // first clean up the current instance, in case some snapshot was taken before
        snapshot.tables.clear();

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(sourceFile));
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new DatabaseSnapshotException("Error reading database snapshot backup file "
                                                + sourceFile, e);
        }

        Element databaseNode = doc.getDocumentElement();
        if (!DatabaseSnapshotUtils.NODE_DB_SNAPSHOT.equals(databaseNode.getNodeName())) {
            throw new DatabaseSnapshotException("Bad backup file. Root node name is expeced to be '"
                                                + DatabaseSnapshotUtils.NODE_DB_SNAPSHOT + "', but it is '"
                                                + databaseNode.getNodeName() + "'");
        }

        if (StringUtils.isNullOrEmpty(newSnapshotName)) {
            snapshot.name = databaseNode.getAttribute(DatabaseSnapshotUtils.ATTR_SNAPSHOT_NAME);
        } else {
            // user wants to change the snapshot name
            snapshot.name = newSnapshotName;
        }

        // the timestamps
        snapshot.metadataTimestamp = DatabaseSnapshotUtils.stringToDate(databaseNode.getAttribute(DatabaseSnapshotUtils.ATTR_METADATA_TIME));
        snapshot.contentTimestamp = DatabaseSnapshotUtils.stringToDate(databaseNode.getAttribute(DatabaseSnapshotUtils.ATTR_CONTENT_TIME));

        // the tables
        List<Element> tableNodes = DatabaseSnapshotUtils.getChildrenByTagName(databaseNode,
                                                                              DatabaseSnapshotUtils.NODE_TABLE);
        for (Element tableNode : tableNodes) {
            snapshot.tables.add(TableDescription.fromXmlNode(snapshot.name, tableNode));
        }

        // any skip table content rules
        snapshot.skipContentPerTable.clear();
        List<Element> skipContentNodes = DatabaseSnapshotUtils.getChildrenByTagName(databaseNode,
                                                                                    DatabaseSnapshotUtils.NODE_SKIP_CONTENT);
        for (Element skipContentNode : skipContentNodes) {
            SkipContent skipContent = SkipContent.fromXmlNode(skipContentNode);
            snapshot.skipContentPerTable.put(skipContent.getTable().toLowerCase(), skipContent);
        }

        // any skip table column rules
        snapshot.skipColumnsPerTable.clear();
        List<Element> skipColumnNodes = DatabaseSnapshotUtils.getChildrenByTagName(databaseNode,
                                                                                   DatabaseSnapshotUtils.NODE_SKIP_COLUMNS);
        for (Element skipColumnNode : skipColumnNodes) {
            SkipColumns skipColumns = SkipColumns.fromXmlNode(skipColumnNode);
            snapshot.skipColumnsPerTable.put(skipColumns.getTable().toLowerCase(), skipColumns);
        }

        // any skip index attribute rules
        snapshot.skipIndexAttributesPerTable.clear();
        List<Element> skipIndexAttributesNodes = DatabaseSnapshotUtils.getChildrenByTagName(databaseNode,
                                                                                            DatabaseSnapshotUtils.NODE_SKIP_INDEX_ATTRIBUTES);
        for (Element skipIndexAttributesNode : skipIndexAttributesNodes) {
            SkipIndexAttributes skipIndexAttributes = SkipIndexAttributes.fromXmlNode(skipIndexAttributesNode);
            snapshot.skipIndexAttributesPerTable.put(skipIndexAttributes.getTable().toLowerCase(),
                                                     skipIndexAttributes);
        }

        // any skip table row rules
        snapshot.skipRowsPerTable.clear();
        List<Element> skipRowNodes = DatabaseSnapshotUtils.getChildrenByTagName(databaseNode,
                                                                                DatabaseSnapshotUtils.NODE_SKIP_ROWS);
        for (Element skipRowNode : skipRowNodes) {
            SkipRows skipRows = SkipRows.fromXmlNode(skipRowNode);
            snapshot.skipRowsPerTable.put(skipRows.getTable().toLowerCase(), skipRows);
        }

        log.info("Load database snapshot from file " + sourceFile + " - END");

        return doc;
    }
}
