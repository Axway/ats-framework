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
package com.axway.ats.core.filesystem.snapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.EqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.core.utils.IoUtils;

public class SnapshotUtils {

    private static final Logger           log         = Logger.getLogger( SnapshotUtils.class );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSSZ" );

    static String dateToString( long timeInMillis ) {

        return DATE_FORMAT.format( new Date( timeInMillis ) );
    }

    static long stringToDate( String timeString ) {

        try {
            return DATE_FORMAT.parse( timeString ).getTime();
        } catch( ParseException e ) {
            throw new FileSystemSnapshotException( "Cannot parse date '" + timeString + "'" );
        }
    }

    static String getDirPathLastToken( String path ) {

        String[] tokens = path.split( IoUtils.FORWARD_SLASH );
        return tokens[tokens.length - 1];
    }

    static void checkDirSnapshotsTopLevel( String thisSnapshotName,
                                           Map<String, DirectorySnapshot> thisDirSnapshots,
                                           String thatSnapshotName,
                                           Map<String, DirectorySnapshot> thatDirSnapshots,
                                           EqualityState equality ) {

        // we will make another HashMap, because in checkDirSnapshotsInternal() we are removing
        // map keys using map.keySet().remove( key ), which actually removes an entry from the Map,
        // which we don't want
        thisDirSnapshots = new HashMap<String, DirectorySnapshot>( thisDirSnapshots );
        thatDirSnapshots = new HashMap<String, DirectorySnapshot>( thatDirSnapshots );

        // all directories from first snapshot are searched in the second
        // the found directories are removed from the internal lists
        checkDirSnapshotsInternal( thisSnapshotName, thisDirSnapshots, thatSnapshotName, thatDirSnapshots,
                                   false, equality, false );

        // now do the same, but replace the snapshots
        checkDirSnapshotsInternal( thatSnapshotName, thatDirSnapshots, thisSnapshotName, thisDirSnapshots,
                                   false, equality, true );
    }

    static void checkDirSnapshotsDeepLevel( String thisSnapshotName,
                                            Map<String, DirectorySnapshot> thisDirSnapshots,
                                            String thatSnapshotName,
                                            Map<String, DirectorySnapshot> thatDirSnapshots,
                                            EqualityState equality ) {

        // we will make another HashMap, because in checkDirSnapshotsInternal() we are removing
        // map keys using map.keySet().remove( key ), which actually removes an entry from the Map,
        // which we don't want
        thisDirSnapshots = new HashMap<String, DirectorySnapshot>( thisDirSnapshots );
        thatDirSnapshots = new HashMap<String, DirectorySnapshot>( thatDirSnapshots );

        // all directories from first snapshot are searched in the second
        // the found directories are removed from the internal lists
        checkDirSnapshotsInternal( thisSnapshotName, thisDirSnapshots, thatSnapshotName, thatDirSnapshots,
                                   true, equality, false );

        // now do the same, but replace the snapshots
        checkDirSnapshotsInternal( thatSnapshotName, thatDirSnapshots, thisSnapshotName, thisDirSnapshots,
                                   true, equality, true );
    }

    private static void checkDirSnapshotsInternal( String firstSnapshotName,
                                                   Map<String, DirectorySnapshot> firstDirSnapshots,
                                                   String secondSnapshotName,
                                                   Map<String, DirectorySnapshot> secondDirSnapshots,
                                                   boolean checkDirName, EqualityState equality,
                                                   boolean areSnapshotsReversed ) {

        Set<String> firstDirAliases = firstDirSnapshots.keySet();
        Set<String> secondDirAliases = secondDirSnapshots.keySet();
        Set<String> processedDirAliases = new HashSet<String>();

        // search for FIRST dirs in SECOND dirs
        for( String dirAlias : firstDirAliases ) {
            log.debug( "Check directories with alias \"" + dirAlias + "\"" );

            DirectorySnapshot firstDir = firstDirSnapshots.get( dirAlias );
            DirectorySnapshot secondDir = secondDirSnapshots.get( dirAlias );

            if( secondDir == null ) {
                // Second snapshot does not have a directory to match
                if( !areSnapshotsReversed ) {
                    equality.addDifference( new FileTrace( firstSnapshotName, firstDir.getPath(),
                                                           secondSnapshotName, null, false ) );
                } else {
                    equality.addDifference( new FileTrace( secondSnapshotName, null, firstSnapshotName,
                                                           firstDir.getPath(), false ) );
                }
            } else {
                log.debug( "Compare [" + firstSnapshotName + "] " + firstDir.getPath() + " and ["
                           + secondSnapshotName + "] " + secondDir.getPath() );
                if( !areSnapshotsReversed ) {
                    firstDir.compare( firstSnapshotName, secondSnapshotName, secondDir, checkDirName,
                                      equality );
                } else {
                    firstDir.compare( secondSnapshotName, firstSnapshotName, secondDir, checkDirName,
                                      equality );
                }
            }

            processedDirAliases.add( dirAlias );
        }

        // remove the processed dirs from the internal lists, we do not want to deal with them anymore
        for( String checkedDirAlias : processedDirAliases ) {
            firstDirAliases.remove( checkedDirAlias );
            secondDirAliases.remove( checkedDirAlias );
        }
    }

    static void checkFileSnapshots( String firstSnapshotName,
                                    final Map<String, FileSnapshot> firstFileSnapshots,
                                    String secondSnapshotName,
                                    final Map<String, FileSnapshot> secondFileSnapshots,
                                    EqualityState equality ) {

        // compare each FIRST file with its counterpart in SECOND files
        for( Entry<String, FileSnapshot> fileSnapshotEntry : firstFileSnapshots.entrySet() ) {
            log.debug( "Check file \"" + fileSnapshotEntry.getKey() + "\"" );

            FileSnapshot firstFile = fileSnapshotEntry.getValue();
            FileSnapshot secondFile = secondFileSnapshots.get( fileSnapshotEntry.getKey() );

            if( secondFile == null ) {
                equality.addDifference( new FileTrace( firstSnapshotName, firstFile.getPath(),
                                                       secondSnapshotName, null ) );
            } else {
                firstFile.compare( firstSnapshotName, secondSnapshotName, secondFile, equality );
            }
        }

        // now all compares here are done, but we want to check if SECOND has files that are not present in FIRST
        // if we find such file - it is a problem
        for( Entry<String, FileSnapshot> fileEntry : secondFileSnapshots.entrySet() ) {
            FileSnapshot firstFile = firstFileSnapshots.get( fileEntry.getKey() );
            FileSnapshot secondFile = fileEntry.getValue();

            if( firstFile == null ) {
                equality.addDifference( new FileTrace( firstSnapshotName, null, secondSnapshotName,
                                                       secondFile.getPath() ) );
            }
        }
    }

    static boolean isFileFromThisDirectory( String dirPath, String filePath ) {

        if( filePath.startsWith( dirPath ) ) {
            // the file is from this dir or a sub-dir

            String filePathRest = filePath.substring( dirPath.length() );
            return !filePathRest.contains( IoUtils.FORWARD_SLASH );
        }

        return false;
    }

    static List<Element> getChildrenByTagName( Element parent, String name ) {

        List<Element> nodeList = new ArrayList<Element>();
        for( Node child = parent.getFirstChild(); child != null; child = child.getNextSibling() ) {
            if( child.getNodeType() == Node.ELEMENT_NODE && name.equals( child.getNodeName() ) ) {
                nodeList.add( ( Element ) child );
            }
        }

        return nodeList;
    }
}
