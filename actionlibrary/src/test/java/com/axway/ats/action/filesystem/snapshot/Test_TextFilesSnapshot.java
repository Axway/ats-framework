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

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.action.BaseTest;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;

public class Test_TextFilesSnapshot extends BaseTest {

    private static String FILES_ROOT;

    private static String getProjectRoot() {

        String root = Test_TextFilesSnapshot.class.getResource( "/" ).getPath();
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
                     + Test_TextFilesSnapshot.class.getPackage().getName().replace( '.', '/' )
                     + "/check_content/text/";

        ActionLibraryConfigurator.getInstance().snapshots.setCheckModificationTime( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckPropertiesFilesContent( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckTextFilesContent( true );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckXmlFilesContent( false );
    }

    @Test
    public void skipTextLineByEqualsText() {

        String textToSkip = "<column_description>name=certPath, type=BLOB, auto increment=NO, "
                            + "default=null, nullable=NO, size=2147483647</column_description>";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineEqualsText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipPropertyByKeyMatchingText() {

        String textToSkip = ".*certPath, type=BLOB, auto increment=NO.*";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineMatchingText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipLineByContainingText_NoSuchDir() {

        String textToSkip = "<column_description>name=certPath, type=BLOB, auto increment=NO";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "fakeDir" );
        snapshot1.text.skipTextLineContainingText( "F1", "sub-dir1/fakeFile.txt", textToSkip );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipLineByContainingText_EmptyString() {

        String textToSkip = "";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineContainingText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        snapshot1.toLocalFile( "D:\\file" );

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipTextLineEqualsText_EmptyString() {

        String textToSkip = "";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineEqualsText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        snapshot1.toLocalFile( "D:\\file" );

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipTextLineMatchingText_EmptyString() {

        String textToSkip = "";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineMatchingText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        snapshot1.toLocalFile( "D:\\file" );

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipPropertyByKeyMatchingText_WrongRegex() {

        String textToSkip = "WRONG.*";

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.text.skipTextLineMatchingText( "F1", "sub-dir1/file1.txt", textToSkip );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }
}
