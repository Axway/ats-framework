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
package com.axway.ats.rbv.filesystem;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemFolder;
import com.axway.ats.rbv.filesystem.FileSystemFolderSearchTerm;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.filesystem.FileSystemStorage;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FilePackage.class,
                 FileSystemFolder.class,
                 FileSystemOperations.class,
                 SystemOperations.class })
public class Test_FileSystemFolder extends BaseTest {

    private static String               path               = "/tmp/test/";

    private static String               file1              = "/tmp/test/file1.txt";
    private static String               file1_hash         = "/tmp/test/file1.txt.1286977647540.1000.1001";
    private static String               file1_hash_changed = "/tmp/test/file1.txt.1333636901120.1000.1001";
    private static String               file2              = "/tmp/test/file2.dat";
    private static String               file2_hash         = "/tmp/test/file2.dat.1333636901488.1000.1001";
    private static String[]             fileList           = new String[]{ file1, file2 };

    private static FileSystemStorage    storage;

    private static FileSystemOperations fileSystemOperations;
    private static SystemOperations     systemOperations;

    @Before
    public void setUpTest_FileSystemFolder() throws Exception {

        fileSystemOperations = createMock( FileSystemOperations.class );
        systemOperations = createMock( SystemOperations.class );
        storage = new FileSystemStorage();
    }

    @Test
    public void open() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        replayAll();

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.open();

        verifyAll();
    }

    @Test
    public void openNegativeInvalidPath() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        replayAll();

        //should pass - only warning should be logged
        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( ";;;",
                                                                                                          null,
                                                                                                          true ) );
        folder.open();

        verifyAll();
    }

    @Test(expected = InvalidInputArgumentsException.class)
    public void openNegativeInvalidHost() throws Exception {

        expectNew( SystemOperations.class, "invalid hosttt" ).andThrow( new RbvStorageException( "Could not open File system folder '/tmp/test/' on server 'invalid hosttt'" ) );

        replayAll();

        FileSystemStorage invalidStorage = new FileSystemStorage( "invalid hosttt" );
        FileSystemFolder folder = ( FileSystemFolder ) invalidStorage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                                 null,
                                                                                                                 true ) );
        folder.open();

        verifyAll();
    }

    @Test
    public void close() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        replayAll();

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.open();
        folder.close();

        verifyAll();
    }

    @Test
    public void getMetadataCountsWithoutSubdirs() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations ).times( 5 );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        // now we call getAllMetaData() 2 times
        expectNew( FileSystemOperations.class, "localhost:0000" ).andReturn( fileSystemOperations ).times( 5 );
        expect( fileSystemOperations.findFiles( path, ".*", true, true, false ) ).andReturn( fileList )
                                                                                 .times( 2 );
        expect( fileSystemOperations.getFileUniqueId( file1 ) ).andReturn( file1_hash ).times( 2 );
        expect( fileSystemOperations.getFileUniqueId( file2 ) ).andReturn( file2_hash ).times( 2 );

        replayAll();

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.open();
        folder.getAllMetaData();
        assertEquals( "Total files: " + 2 + ", new files: " + 2, folder.getMetaDataCounts() );
        folder.getAllMetaData();
        assertEquals( "Total files: " + 2 + ", new files: " + 0, folder.getMetaDataCounts() );

        verifyAll();
    }

    @Test
    public void getNewMetadataChangeLastModified() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations ).times( 7 );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        // now we call getNewMetaData() 3 times
        expect( fileSystemOperations.findFiles( path, ".*", true, true, false ) ).andReturn( fileList )
                                                                                 .times( 3 );
        expectNew( FileSystemOperations.class, "localhost:0000" ).andReturn( fileSystemOperations ).times( 7 ); // 3 times x 2 files
        expect( fileSystemOperations.getFileUniqueId( file1 ) ).andReturn( file1_hash ).times( 2 );
        expect( fileSystemOperations.getFileUniqueId( file2 ) ).andReturn( file2_hash ).times( 3 );
        // modify the modification timestamp of file1 for the last getNewMetaData() call
        expect( fileSystemOperations.getFileUniqueId( file1 ) ).andReturn( file1_hash_changed );

        replayAll();

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.open();
        List<MetaData> list = folder.getNewMetaData();
        assertEquals( 2, list.size() );

        for( MetaData metaData : list ) {
            ( ( FileSystemMetaData ) metaData ).getFilePackage();
        }

        assertEquals( 0, folder.getNewMetaData().size() );

        assertEquals( 1, folder.getNewMetaData().size() );

        verifyAll();
    }

    @Test
    public void getAllMetadataNoSuchEntity() throws Exception {

        // constructors
        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expectNew( FileSystemOperations.class, "localhost:0000" ).andReturn( fileSystemOperations );

        // open()
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.SOLARIS );
        // getAllMetaData()
        expect( fileSystemOperations.findFiles( "some.path/", ".*", true, true, false ) ).andReturn( new String[0] );
        replayAll();

        FileSystemFolder folder = new FileSystemFolder( "localhost:0000", "some.path", null, true, false );
        folder.open();
        assertEquals( folder.getAllMetaData(), new ArrayList<MetaData>() );

        verifyAll();
    }

    @Test(expected = RbvException.class)
    public void getAllMetadataExceptionExists() throws Exception {

        // constructor
        expectNew( FileSystemOperations.class, "localhost:0000" ).andThrow( new RbvException( "Test" ) );
        replayAll();

        FileSystemFolder folder = new FileSystemFolder( "localhost:0000", "some.path", null, true, false );
        folder.open();
        assertEquals( folder.getAllMetaData(), new ArrayList<MetaData>() );

        verifyAll();
    }

    @Test
    public void getAllMetadataExceptionFetchingModTimePositive() throws Exception {

        // constructors
        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expectNew( FileSystemOperations.class, "localhost:0000" ).andReturn( fileSystemOperations );

        // open()
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.SOLARIS );
        // getAllMetaData()
        expect( fileSystemOperations.findFiles( "some.path/", ".*", true, true, false ) ).andReturn( new String[]{ "some.path/some.file1",
                "some.path/some.file2" } );

        FilePackage pack1 = createMock( FilePackage.class );
        expectNew( FilePackage.class, "localhost:0000", "some.path/some.file1", OperatingSystemType.SOLARIS ).andReturn( pack1 );
        expect( pack1.getUniqueIdentifier() ).andThrow( new PackageException( "" ) );

        FilePackage pack2 = createMock( FilePackage.class );
        expectNew( FilePackage.class, "localhost:0000", "some.path/some.file2", OperatingSystemType.SOLARIS ).andReturn( pack2 );
        expect( pack2.getUniqueIdentifier() ).andReturn( "some.path/some.file1.1.1.1" );

        replayAll();

        FileSystemFolder folder = new FileSystemFolder( "localhost:0000", "some.path", null, true, false );
        folder.open();
        assertEquals( folder.getAllMetaData().size(), 1 );

        verifyAll();
    }

    @Test
    public void getDescription() {

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        assertEquals( "File system folder '" + path + "' on the local machine", folder.getDescription() );
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getAllMetaDataNegativeFolderNotOpen() throws Exception {

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.getAllMetaData();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getNewMetaDataNegativeFolderNotOpen() throws Exception {

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.getNewMetaData();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void closeNegativeFolderNotOpen() throws Exception {

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.close();
    }

    @Test(expected = MatchableNotOpenException.class)
    public void getMetaDataCountsNegativeFolderNotOpen() throws Exception {

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.getMetaDataCounts();
    }

    @Test(expected = MatchableAlreadyOpenException.class)
    public void openNegativeFolderAlreadyOpen() throws Exception {

        expectNew( SystemOperations.class, "localhost:0000" ).andReturn( systemOperations );
        expect( systemOperations.getOperatingSystemType() ).andReturn( OperatingSystemType.LINUX );

        replayAll();

        FileSystemFolder folder = ( FileSystemFolder ) storage.getFolder( new FileSystemFolderSearchTerm( path,
                                                                                                          null,
                                                                                                          true,
                                                                                                          false ) );
        folder.open();
        folder.open();

        verifyAll();
    }
}
