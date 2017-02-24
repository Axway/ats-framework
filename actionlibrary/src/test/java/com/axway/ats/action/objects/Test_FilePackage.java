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
package com.axway.ats.action.objects;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;

public class Test_FilePackage extends BaseTest {

    private static final String  LOCAL_ATS_AGENT = "localhost:0000";

    private static String        fileEntryPath;
    private FileSystemOperations mockRemoteFileSystemOperations;
    private SystemOperations     mockRemoteOSOperations;

    @BeforeClass
    public static void setUpTest_FilePackage() {

        fileEntryPath = Test_FilePackage.class.getResource( "mail.msg" ).getPath();
    }

    @Before
    public void init() {

        mockRemoteFileSystemOperations = createMock( FileSystemOperations.class );
        mockRemoteOSOperations = createMock( SystemOperations.class );
    }

    @Test
    public void getGidWindows() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( ( long ) FilePackage.ATTRIBUTE_NOT_SUPPORTED, filePackage.getGid() );
    }

    @Test
    public void getGidUnix() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileGID( fileEntryPath ) ).andReturn( 42l );
        expectLastCall().once();

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( 42L, filePackage.getGid() );
        assertEquals( 42L, filePackage.getGid() );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = PackageException.class)
    public void getGidUnixNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileGID( fileEntryPath ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getGid();
        verify( mockRemoteFileSystemOperations );
    }

    @Test
    public void getUidWindows() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( ( long ) FilePackage.ATTRIBUTE_NOT_SUPPORTED, filePackage.getUid() );
    }

    @Test
    public void getUidUnix() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileUID( fileEntryPath ) ).andReturn( 42l );
        expectLastCall().once();

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( 42L, filePackage.getUid() );
        assertEquals( 42L, filePackage.getUid() );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = PackageException.class)
    public void getUidUnixNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileUID( fileEntryPath ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getUid();
    }

    @Test
    public void getPermissionsWindows() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( ( long ) FilePackage.ATTRIBUTE_NOT_SUPPORTED, filePackage.getPermissions() );
    }

    @Test
    public void getPermissionsUnix() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFilePermissions( fileEntryPath ) ).andReturn( "42" );
        expectLastCall().once();

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( 42L, filePackage.getPermissions() );
        assertEquals( 42L, filePackage.getPermissions() );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = PackageException.class)
    public void getPermissionsUnixNegativeInvalidPermissions() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFilePermissions( fileEntryPath ) ).andReturn( "invalid" );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getPermissions();
    }

    @Test(expected = PackageException.class)
    public void getPermissionsUnixNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFilePermissions( fileEntryPath ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getPermissions();
    }

    @Test
    public void getModTime() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileModificationTime( fileEntryPath ) ).andReturn( 132132465L );
        expectLastCall().once();

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( 132132465L, filePackage.getModTime() );
        assertEquals( 132132465L, filePackage.getModTime() );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = PackageException.class)
    public void getModTimeNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileModificationTime( fileEntryPath ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getModTime();
    }

    @Test
    public void getSize() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileSize( fileEntryPath ) ).andReturn( 132132465L );
        expectLastCall().once();

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( 132132465L, filePackage.getSize() );
        assertEquals( 132132465L, filePackage.getSize() );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = PackageException.class)
    public void getSizeNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.getFileSize( fileEntryPath ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getSize();
    }

    @Test
    public void getcomputeMd5Sum() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.computeMd5Sum( fileEntryPath,
                                                              Md5SumMode.BINARY ) ).andReturn( "0b9ffe289e38eb046a15699189db93b8" );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( "0b9ffe289e38eb046a15699189db93b8", filePackage.getMd5sum() );
    }

    @Test
    public void getcomputeMd5SumLong() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.computeMd5Sum( fileEntryPath,
                                                              Md5SumMode.ASCII ) ).andReturn( "0b9ffe289e38eb046a15699189db93b8" );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( "0b9ffe289e38eb046a15699189db93b8", filePackage.getMd5sum( false ) );
    }

    @Test(expected = PackageException.class)
    public void getcomputeMd5SumNegativeExceptionThrownByStaf() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.computeMd5Sum( fileEntryPath,
                                                              Md5SumMode.BINARY ) ).andThrow( new RuntimeException( "Test" ) );
        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getMd5sum();
    }

    @Test
    public void getAtsAgent() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( LOCAL_ATS_AGENT, filePackage.getAtsAgent() );
    }

    @Test
    public void getName() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( "mail.msg", filePackage.getName() );
    }

    @Test(expected = PackageException.class)
    public void getAllHeaders() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getAllHeaders().size();
    }

    @Test(expected = PackageException.class)
    public void getAllStreams() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getAllStreams().size();
    }

    @Test
    public void getDescription() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( IoUtils.normalizeUnixFile( fileEntryPath ), filePackage.getDescription() );
    }

    @Test(expected = PackageException.class)
    public void getHeaderValues() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getHeaderValues( "header1" );
    }

    @Test(expected = PackageException.class)
    public void getSubject() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getSubject();
    }

    @Test(expected = PackageException.class)
    public void getTag() throws ActionException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.getTag();
    }

    @Test(expected = PackageException.class)
    public void tag() throws ActionException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.tag();
    }

    @Test
    public void toStringPositive() throws PackageException {

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath,
                                                   OperatingSystemType.WINDOWS,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertEquals( fileEntryPath, filePackage.toString() );
    }

    @Test
    public void grep() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.fileGrep( fileEntryPath, "regexp",
                                                         true /* not RegExp */ ) ).andReturn( new String[]{ "regexp" } );

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        assertArrayEquals( new String[]{ "regexp" }, filePackage.grep( "regexp", false ) );

        verify( mockRemoteFileSystemOperations );
    }

    @Test(expected = Exception.class)
    public void grepNegative() throws Exception {

        //we expect only one call the auto service to be made (lazy initialization)
        expect( mockRemoteFileSystemOperations.fileGrep( fileEntryPath, "regexp",
                                                         true /* not RegExp */ ) ).andThrow( new Exception() );

        replay( mockRemoteFileSystemOperations );

        FilePackage filePackage = new FilePackage( LOCAL_ATS_AGENT, fileEntryPath, OperatingSystemType.LINUX,
                                                   mockRemoteFileSystemOperations, mockRemoteOSOperations );
        filePackage.grep( "regexp", false );

        verify( mockRemoteFileSystemOperations );
    }
}
