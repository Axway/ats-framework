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
package com.axway.ats.action.filesystem.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.BaseTest;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.DifferenceType;
import com.axway.ats.common.filesystem.snapshot.equality.FileSystemEqualityState;
import com.axway.ats.common.filesystem.snapshot.equality.FileTrace;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;

public class Test_XmlFilesSnapshot extends BaseTest {

    private static String FILES_ROOT;

    private static String getProjectRoot() {

        String root = Test_XmlFilesSnapshot.class.getResource( "/" ).getPath();
        do {
            root = IoUtils.normalizeDirPath( root );
            if( new File( root + "pom.xml" ).exists() ) {
                return root;
            }
        } while( ( root = new File( root ).getParent() ) != null );

        throw new RuntimeException( "Uable to determine the project root path." );
    }

    @BeforeClass
    public static void beforeClass() {

        String projectRoot = getProjectRoot();
        if( OperatingSystemType.getCurrentOsType().isWindows() ) {
            // remove the leading "/"
            if( projectRoot.startsWith( "/" ) ) {
                projectRoot = projectRoot.substring( 1 );
            }
        }

        FILES_ROOT = IoUtils.normalizeUnixDir( projectRoot ) + "src/test/resources/"
                     + Test_XmlFilesSnapshot.class.getPackage().getName().replace( '.', '/' )
                     + "/check_content/xml/";

        ActionLibraryConfigurator.getInstance().snapshots.setCheckModificationTime( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckXmlFilesContent( true );
    }

    @Test
    public void skipByAttributeValueContainsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot1.xml.skipNodeByAttributeValueContainingText( "F1", "sub-dir1/file1.xml",
                                                              "//TABLE[@name='AccountLocalKey']",
                                                              "primaryKey", "alias" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipByAttributeValueEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot1.xml.skipNodeByAttributeValueEqualsText( "F1", "sub-dir1/file1.xml",
                                                          "//TABLE[@name='AccountLocalKey']", "primaryKey",
                                                          "alias" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.xml.skipNodeByAttributeValueEqualsText( "F1", "sub-dir1/file1.xml",
                                                          "//TABLE[@name='AccountLocalKey']", "primaryKey",
                                                          "aliasssssss" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipByAttributeValueMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot1.xml.skipNodeByAttributeValueMatchingText( "F1", "sub-dir1/file1.xml",
                                                            "//TABLE[@name='AccountLocalKey']", "primaryKey",
                                                            "alias.*" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipByValueContainsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.xml.skipNodeByValueContainingText( "F1", "sub-dir1/file1.xml",
                                                     "//TABLE[@name='AccountLocalKey']/column_descriptions/column_description",
                                                     "name=certPath" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.xml.skipNodeByValueContainingText( "F1", "sub-dir1/file1.xml",
                                                     "//TABLE[@name='AddressBookAccountSource']/column_descriptions/column_description",
                                                     "name=id" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipByValueEqualsTextAndMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.xml.skipNodeByValueEqualsText( "F1", "sub-dir1/file1.xml",
                                                 "//TABLE[@name='AccountLocalKey']/column_descriptions/column_description",
                                                 "name=certPath, type=BLOB, auto increment=NO, default=null, nullable=NO, size=2147483647" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.xml.skipNodeByValueMatchingText( "F1", "sub-dir1/file1.xml",
                                                   "//TABLE[@name='AddressBookAccountSource']/column_descriptions/column_description",
                                                   "name=id, .*" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipByValueContainsText_reversedMatchers() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.xml.skipNodeByValueContainingText( "F1", "sub-dir1/file1.xml",
                                                     "//TABLE[@name='AddressBookAccountSource']/column_descriptions/column_description",
                                                     "name=id" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.xml.skipNodeByValueContainingText( "F1", "sub-dir1/file1.xml",
                                                     "//TABLE[@name='AccountLocalKey']/column_descriptions/column_description",
                                                     "name=certPath" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void differenceInValue() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare( snapshot2 );
            thisShouldNotBeReached();
        } catch( FileSystemSnapshotException se ) {
        	FileSystemEqualityState equality = se.getEqualityState();

            List<FileTrace> diffs = equality.getDifferences();

            // there is differences in 1 file
            assertEquals( 1, diffs.size() );
            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            // check the differences in the first snapshot file
            assertEquals( "snap1", diff.getFirstSnapshot() );
            assertTrue( diff.getFirstEntityPath().endsWith( "dir1/sub-dir1/file1.xml" ) );
            Map<String, String> firstSnapshotDifferencies = diff.getFirstSnapshotDifferencies();

            assertEquals( 2, firstSnapshotDifferencies.size() );
            Object[] keys = firstSnapshotDifferencies.keySet().toArray();
            Object[] values = firstSnapshotDifferencies.values().toArray();
            assertTrue( keys[0].toString().contains( "name=certPath" ) );
            assertTrue( values[0].toString().contains( "YES" ) );
            assertTrue( keys[1].toString().contains( "name=id" ) );
            assertTrue( values[1].toString().contains( "NO" ) );

            // check the differences in the second snapshot file
            assertTrue( diff.getSecondEntityPath().endsWith( "dir2/sub-dir1/file1.xml" ) );
            assertEquals( "snap2", diff.getSecondSnapshot() );
            Map<String, String> secondSnapshotDifferencies = diff.getSecondSnapshotDifferencies();

            assertEquals( 2, secondSnapshotDifferencies.size() );
            keys = secondSnapshotDifferencies.keySet().toArray();
            values = secondSnapshotDifferencies.values().toArray();
            assertTrue( keys[0].toString().contains( "name=certPath" ) );
            assertTrue( values[0].toString().contains( "NO" ) );
            assertTrue( keys[1].toString().contains( "name=id" ) );
            assertTrue( values[1].toString().contains( "YES" ) );
        }
    }

    @Test
    public void differenceInAttribute() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        try {
            snapshot1.compare( snapshot2 );
            thisShouldNotBeReached();
        } catch( FileSystemSnapshotException se ) {
        	FileSystemEqualityState equality = se.getEqualityState();

            List<FileTrace> diffs = equality.getDifferences();

            // there is differences in 1 file
            assertEquals( 1, diffs.size() );
            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            // check the differences in the first snapshot file
            assertEquals( "snap1", diff.getFirstSnapshot() );
            assertTrue( diff.getFirstEntityPath().endsWith( "dir3/sub-dir1/file1.xml" ) );
            Map<String, String> firstSnapshotDifferencies = diff.getFirstSnapshotDifferencies();

            assertEquals( 2, firstSnapshotDifferencies.size() );
            Object[] keys = firstSnapshotDifferencies.keySet().toArray();
            Object[] values = firstSnapshotDifferencies.values().toArray();
            assertTrue( keys[0].toString().contains( "primaryKey=\"alias\"" ) );
            assertTrue( values[0].toString().contains( "YES" ) );
            assertTrue( keys[1].toString().contains( "primaryKey=\"aliasssssss\"" ) );
            assertTrue( values[1].toString().contains( "NO" ) );

            // check the differences in the second snapshot file
            assertTrue( diff.getSecondEntityPath().endsWith( "dir4/sub-dir1/file1.xml" ) );
            assertEquals( "snap2", diff.getSecondSnapshot() );
            Map<String, String> secondSnapshotDifferencies = diff.getSecondSnapshotDifferencies();

            assertEquals( 2, secondSnapshotDifferencies.size() );
            keys = secondSnapshotDifferencies.keySet().toArray();
            values = secondSnapshotDifferencies.values().toArray();
            assertTrue( keys[0].toString().contains( "primaryKey=\"alias\"" ) );
            assertTrue( values[0].toString().contains( "NO" ) );
            assertTrue( keys[1].toString().contains( "primaryKey=\"aliasssssss\"" ) );
            assertTrue( values[1].toString().contains( "YES" ) );
        }
    }

    private void thisShouldNotBeReached() {

        throw new IllegalStateException( "This is not expected" );
    }
}
