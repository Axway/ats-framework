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

public class Test_PropertyFilesSnapshot extends BaseTest {

    private static String FILES_ROOT;

    private static String getProjectRoot() {

        String root = Test_PropertyFilesSnapshot.class.getResource( "/" ).getPath();
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
                     + Test_PropertyFilesSnapshot.class.getPackage().getName().replace( '.', '/' )
                     + "/check_content/properties/";

        ActionLibraryConfigurator.getInstance().snapshots.setCheckModificationTime( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckPropertiesFilesContent( true );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckXmlFilesContent( false );
    }

    @Test
    public void skipPropertyByKeyEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByKeyEqualsText( "F1", "sub-dir1/file1.properties",
                                                          "actionlibrary.packageloader.defaultbox" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyByKeyContainingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/file1.properties",
                                                              "defaultbox" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyByKeyMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByKeyMatchingText( "F1", "sub-dir1/file1.properties",
                                                            ".*packageloader.*" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyByValueEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByValueEqualsText( "F1", "sub-dir1/file1.properties",
                                                            "messagesbox" );

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
            assertEquals( 1, diffs.size() );

            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            assertEquals( 1, diff.getFirstSnapshotDifferencies().size() );
            assertTrue( diff.getFirstSnapshotDifferencies()
                            .toString()
                            .contains( "Presence of actionlibrary.packageloader.defaultbox=YES" ) );
            assertTrue( diff.getSecondSnapshotDifferencies()
                            .toString()
                            .contains( "Presence of actionlibrary.packageloader.defaultbox=NO" ) );
        }

    }

    @Test
    public void skipPropertyByValueContainingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByValueContainingText( "F1", "sub-dir1/file1.properties",
                                                                "messagesbox" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyByValueMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.properties.skipPropertyByValueMatchingText( "F1", "sub-dir1/file1.properties",
                                                              "messagesbox.*" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void overwriteSkipPropertyKey() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        // we set a skip matcher which would match ...
        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/file1.properties",
                                                              "defaultbox" );
        // but then overwrite it with a more strict matcher which will fail
        snapshot1.properties.skipPropertyByKeyEqualsText( "F1", "sub-dir1/file1.properties", "defaultbox" );

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
            assertEquals( 1, diffs.size() );

            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            assertEquals( 1, diff.getFirstSnapshotDifferencies().size() );
            assertTrue( diff.getFirstSnapshotDifferencies()
                            .toString()
                            .contains( "property key 'actionlibrary.packageloader.defaultbox'='messagesboxessss'" ) );
        }
    }

    @Test
    public void overwriteSkipPropertyValue() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        // we set a skip matcher which would match ...
        snapshot1.properties.skipPropertyByValueContainingText( "F1", "sub-dir1/file1.properties",
                                                                "messages" );
        // but then overwrite it with a more strict matcher which will fail
        snapshot1.properties.skipPropertyByValueEqualsText( "F1", "sub-dir1/file1.properties", "messages" );

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
            assertEquals( 1, diffs.size() );

            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            assertEquals( 1, diff.getFirstSnapshotDifferencies().size() );
            assertTrue( diff.getFirstSnapshotDifferencies()
                            .toString()
                            .contains( "property key 'actionlibrary.packageloader.defaultbox'='messagesboxessss'" ) );
        }
    }

    @Test
    public void skipProperty_negative() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        // the next 2 statements should make no effect at all
        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/no-such-file.properties",
                                                              "defaultbox" );
        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "no-such-dir/file1.properties",
                                                              "defaultbox" );

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
            assertEquals( 1, diffs.size() );

            FileTrace diff = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff.getDifferenceType() );

            assertEquals( 1, diff.getFirstSnapshotDifferencies().size() );
            assertTrue( diff.getFirstSnapshotDifferencies()
                            .toString()
                            .contains( "property key 'actionlibrary.packageloader.defaultbox'='messagesboxessss'" ) );
        }
    }

    @Test
    public void skipProperty_negative2() {

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
            assertEquals( 2, diffs.size() );

            FileTrace diff1 = diffs.get( 0 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff1.getDifferenceType() );

            assertEquals( 2, diff1.getFirstSnapshotDifferencies().size() );
            assertTrue( diff1.getFirstSnapshotDifferencies()
                             .toString()
                             .contains( "Presence of actionlibrary.filetransfer.connection.interval=NO, Presence of actionlibrary.filetransfer.connection.interval.tmp=YES" ) );

            FileTrace diff2 = diffs.get( 1 );
            assertEquals( DifferenceType.DIFFERENT_FILES, diff2.getDifferenceType() );

            assertEquals( 3, diff2.getFirstSnapshotDifferencies().size() );
            assertTrue( diff2.getFirstSnapshotDifferencies()
                             .toString()
                             .contains( "property key 'actionlibrary.filetransfer.connection.attempts'='500', property key 'actionlibrary.filetransfer.connection.initialdelay'='some value', property key 'actionlibrary.filetransfer.connection.interval'='1000000" ) );
        }
    }

    @Test
    public void matchersProvidedInSecondSnapshotOnly() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/file1.properties",
                                                              "defaultbox" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyInMoreFiles() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );

        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "file1.properties",
                                                              "actionlibrary.filetransfer.connection.interval" );
        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/file1.properties",
                                                              "actionlibrary.filetransfer.connection" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyInMoreFiles2() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir3" );

        snapshot1.properties.skipPropertyByKeyContainingText( "F1", "file1.properties",
                                                              "actionlibrary.filetransfer.connection.interval" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.properties.skipPropertyByKeyEqualsText( "F1", "sub-dir1/file1.properties",
                                                          "actionlibrary.filetransfer.connection.attempts" );
        snapshot2.properties.skipPropertyByKeyContainingText( "F1", "sub-dir1/file1.properties", "interval" );
        snapshot2.properties.skipPropertyByKeyMatchingText( "F1", "sub-dir1/file1.properties",
                                                            "actionlibrary.*initialdelay" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    private void thisShouldNotBeReached() {

        throw new IllegalStateException( "This is not expected" );
    }
}
