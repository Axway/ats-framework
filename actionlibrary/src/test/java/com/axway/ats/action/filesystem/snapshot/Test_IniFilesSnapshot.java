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

public class Test_IniFilesSnapshot extends BaseTest {

    private static String FILES_ROOT;

    private static String getProjectRoot() {

        String root = Test_IniFilesSnapshot.class.getResource( "/" ).getPath();
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
                     + Test_IniFilesSnapshot.class.getPackage().getName().replace( '.', '/' )
                     + "/check_content/ini/";

        ActionLibraryConfigurator.getInstance().snapshots.setCheckModificationTime( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckPropertiesFilesContent( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckIniFilesContent( true );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckTextFilesContent( false );
        ActionLibraryConfigurator.getInstance().snapshots.setCheckXmlFilesContent( false );
    }

    @Test
    public void skipIniPropertyByKeyContainingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyContainingText( "F1", "short_ini_file.ini", "[Mail]", "OLEM" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByKeyContainingText_NoSuchDir() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyContainingText( "F1", "short_ini_file.ini", "[Mail]", "OLEM" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByKeyContainingText_NullOrEmptyKey() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyContainingText( "F1", "short_ini_file.ini", "[Mail]", "" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByKeyContainingText_NullOrEmptySection() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyContainingText( "F1", "short_ini_file.ini", "", "OLEM" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByKeyEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyEqualsText( "F1", "short_ini_file.ini", "[Mail]", "OLEMessaging" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByKeyEqualsText_KeyNotEqual() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyEqualsText( "F1", "short_ini_file.ini", "[Mail]",
                                                      "OLEMessaginggggggg" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByKeyMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyMatchingText( "F1", "short_ini_file.ini", "[Mail]", ".*Messa.*" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByKeyMatchingText_BadRegex() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByKeyMatchingText( "F1", "short_ini_file.ini", "[Mail]", "Messa.*" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir2" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByValueContainingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueContainingText( "F1", "short_ini_file.ini", "[Mail]", "mapi" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByValueContainingText_NullOrEmptyValue() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueContainingText( "F1", "short_ini_file.ini", "[Mail]", "" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByValueEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueEqualsText( "F1", "short_ini_file.ini", "[Mail]", "mapi32.dll" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByValueEqualsText_ValueNotEqual() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueEqualsText( "F1", "short_ini_file.ini", "[Mail]", "notEqual" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniPropertyByValueMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueMatchingText( "F1", "short_ini_file.ini", "[Mail]", "map.*" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniPropertyByValueMatchingText_BadRegex() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );
        snapshot1.ini.skipIniPropertyByValueMatchingText( "F1", "short_ini_file.ini", "[Mail]", "badRegex" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir3" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniSectionContainingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionContainingText( "F1", "short_ini_file.ini", "languages" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniSectionContainingText_NoSuchSection() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionContainingText( "F1", "short_ini_file.ini", "noSuchSection" );

        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniSectionEqualsText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionEqualsText( "F1", "short_ini_file.ini", "[languages]" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniSectionEqualsText_NotEqual() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionEqualsText( "F1", "short_ini_file.ini", "languagessssss" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test
    public void skipIniSectionMatchingText() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionMatchingText( "F1", "short_ini_file.ini", ".*gua.*" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }

    @Test(expected = FileSystemSnapshotException.class)
    public void skipIniSectionMatchingText_BadRegex() {

        FileSystemSnapshot snapshot1 = new FileSystemSnapshot( "snap1" );
        snapshot1.addDirectory( "F1", FILES_ROOT + "dir1" );

        snapshot1.ini.skipIniSectionMatchingText( "F1", "short_ini_file.ini", "badRegex" );
        snapshot1.takeSnapshot();

        FileSystemSnapshot snapshot2 = new FileSystemSnapshot( "snap2" );
        snapshot2.addDirectory( "F1", FILES_ROOT + "dir4" );
        snapshot2.takeSnapshot();

        snapshot1.compare( snapshot2 );
    }
}
