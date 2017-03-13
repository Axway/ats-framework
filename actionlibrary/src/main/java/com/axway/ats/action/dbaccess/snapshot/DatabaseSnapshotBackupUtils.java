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
package com.axway.ats.action.dbaccess.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotException;
import com.axway.ats.common.dbaccess.snapshot.DatabaseSnapshotUtils;
import com.axway.ats.common.dbaccess.snapshot.TableDescription;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Used to read/save database data from/to a backup file
 */
class DatabaseSnapshotBackupUtils {

    private static Logger log = Logger.getLogger( DatabaseSnapshotBackupUtils.class );

    /**
     * Save a snapshot into a file
     * @param snapshot the snapshot to save
     * @param backupFile the backup file name
     * @return the XML document
     */
    public Document saveToFile( DatabaseSnapshot snapshot, String backupFile ) {

        log.info( "Save database snapshot into file " + backupFile + " - START" );

        // create the directory if does not exist
        File dirPath = new File( IoUtils.getFilePath( backupFile ) );
        if( !dirPath.exists() ) {
            dirPath.mkdirs();
        }

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch( Exception e ) {
            throw new DatabaseSnapshotException( "Error creating DOM parser for " + backupFile, e );
        }

        // TODO - add DTD or schema for manual creation and easy validation
        Element dbNode = doc.createElement( DatabaseSnapshotUtils.NODE_DB_SNAPSHOT );
        dbNode.setAttribute( DatabaseSnapshotUtils.ATTR_SNAPSHOT_NAME, snapshot.name );
        // this timestamp comes when user takes a snapshot from database
        dbNode.setAttribute( DatabaseSnapshotUtils.ATTR_METADATA_TIME,
                             DatabaseSnapshotUtils.dateToString( snapshot.metadataTimestamp ) );
        // this timestamp now
        snapshot.contentTimestamp = System.currentTimeMillis();
        dbNode.setAttribute( DatabaseSnapshotUtils.ATTR_CONTENT_TIME,
                             DatabaseSnapshotUtils.dateToString( snapshot.contentTimestamp ) );
        doc.appendChild( dbNode );

        // append table descriptions
        for( TableDescription tableDescription : snapshot.tables ) {
            Element tableNode = doc.createElement( DatabaseSnapshotUtils.NODE_TABLE );
            dbNode.appendChild( tableNode );

            tableDescription.toXmlNode( doc, tableNode );

            List<String> valuesList = new ArrayList<String>();
            if( snapshot.skipTableContent.contains( tableDescription.getName() ) ) {
                // no rows have to be saved, so we skip this iteration
                continue;
            } else {
                valuesList.addAll( snapshot.loadTableData( snapshot.name,
                                                           tableDescription,
                                                           snapshot.skipRulesPerTable,
                                                           new HashMap<String, Map<String, String>>(),
                                                           null,
                                                           null ) );
            }
            for( String values : valuesList ) {
                Element rowNode = doc.createElement( DatabaseSnapshotUtils.NODE_ROW );
                if( !skipRow( snapshot.skipRows.get( tableDescription.getName() ),
                             values ) ) {
                    rowNode.setTextContent( values );
                }

                tableNode.appendChild( rowNode );
            }
        }

        // append skip rules
        for( String tableName : snapshot.skipRulesPerTable.keySet() ) {
            Element skipRuleNode = doc.createElement( DatabaseSnapshotUtils.NODE_SKIP_RULE );
            dbNode.appendChild( skipRuleNode );
            skipRuleNode.setAttribute( DatabaseSnapshotUtils.ATT_SKIP_RULE_TABLE, tableName );

            snapshot.skipRulesPerTable.get( tableName ).toXmlNode( doc, skipRuleNode );
        }

        // save the XML file
        try {
            OutputFormat format = new OutputFormat( doc );
            format.setIndenting( true );
            format.setIndent( 4 );
            format.setLineWidth( 1000 );

            XMLSerializer serializer = new XMLSerializer( new FileOutputStream( new File( backupFile ) ),
                                                          format );
            serializer.serialize( doc );
        } catch( Exception e ) {
            throw new DatabaseSnapshotException( "Error saving " + backupFile, e );
        }

        log.info( "Save database snapshot into file " + backupFile + " - END" );

        return doc;
    }
    
    private boolean skipRow(
                             Map<String, String> skipRows,
                             String rowValues ) {

        if( skipRows != null ) {
            for( Entry<String, String> skipRow : skipRows.entrySet() ) {
                if( rowValues.contains( skipRow.getKey() + "=" + skipRow.getValue() ) ) {
                    return true;
                }
            }
        }
        return false;
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

        log.info( "Load database snapshot from file " + sourceFile + " - START" );

        // first clean up the current instance, in case some snapshot was taken before
        snapshot.tables.clear();

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( new File( sourceFile ) );
            doc.getDocumentElement().normalize();
        } catch( Exception e ) {
            throw new DatabaseSnapshotException( "Error reading database snapshot backup file "
                                                 + sourceFile );
        }

        Element databaseNode = doc.getDocumentElement();
        if( !DatabaseSnapshotUtils.NODE_DB_SNAPSHOT.equals( databaseNode.getNodeName() ) ) {
            throw new DatabaseSnapshotException( "Bad backup file. Root node name is expeced to be '"
                                                 + DatabaseSnapshotUtils.NODE_DB_SNAPSHOT + "', but it is '"
                                                 + databaseNode.getNodeName() + "'" );
        }

        if( StringUtils.isNullOrEmpty( newSnapshotName ) ) {
            snapshot.name = databaseNode.getAttribute( DatabaseSnapshotUtils.ATTR_SNAPSHOT_NAME );
        } else {
            // user wants to change the snapshot name
            snapshot.name = newSnapshotName;
        }

        // the timestamps
        snapshot.metadataTimestamp = DatabaseSnapshotUtils.stringToDate( databaseNode.getAttribute( DatabaseSnapshotUtils.ATTR_METADATA_TIME ) );
        snapshot.contentTimestamp = DatabaseSnapshotUtils.stringToDate( databaseNode.getAttribute( DatabaseSnapshotUtils.ATTR_CONTENT_TIME ) );

        // the tables
        List<Element> tableNodes = DatabaseSnapshotUtils.getChildrenByTagName( databaseNode,
                                                                         DatabaseSnapshotUtils.NODE_TABLE );
        for( Element tableNode : tableNodes ) {
            snapshot.tables.add( TableDescription.fromXmlNode( snapshot.name, tableNode ) );
        }

        log.info( "Load database snapshot from file " + sourceFile + " - END" );

        return doc;
    }
}