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

import java.io.InputStream;
import java.util.List;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.filesystem.RemoteFileSystemOperations;
import com.axway.ats.action.model.ActionException;
import com.axway.ats.action.objects.model.Package;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.objects.model.PackageHeader;
import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filesystem.Md5SumMode;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.filesystem.LocalFileSystemOperations;
import com.axway.ats.core.filesystem.model.IFileSystemOperations;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.HostUtils;

/**
 * FIXME We should consider re-factoring this piece of code
 *
 * The {@link FilePackage} class implements the {@link Package} interface focusing
 * on file operations.
 *
 * In reality a {@link FilePackage} is either a file or a folder on a specific machine.
 */
@PublicAtsApi
public class FilePackage implements Package {

    public static final Long     ATTRIBUTE_NOT_SUPPORTED = Long.MIN_VALUE;

    private String               path;
    private String               name;
    private String               ownerName;
    private String               groupName;
    private Long                 uid;
    private Long                 gid;
    private Long                 permissions;
    private Long                 modtime;
    private Long                 size;
    private String               atsAgent;
    private OperatingSystemType  osType;

    private FileSystemOperations fileSystemOperations;
    private SystemOperations     systemOperations;

    /**
     * Constructor. Extracts the operation system by itself.
     *
     * @param absolutePath the absolute path and name of the entity, e.g. /root/file.ext
     */
    public FilePackage( String absolutePath ) throws PackageException {

        this(null, absolutePath);
    }

    /**
     * Constructor
     *
     * @param absolutePath the absolute path and name of the entity, e.g. /root/file.ext
     * @param osType the operating system of the machine
     */
    public FilePackage( String absolutePath, OperatingSystemType osType ) {

        this(null, absolutePath, osType);
    }

    /**
     * Constructor. Extracts the operation system by itself.
     *
     * @param atsAgent the address of the remote ATS agent where the entity is located on
     * @param absolutePath the absolute path and name of the entity, e.g. /root/file.ext
     */
    public FilePackage( String atsAgent, String absolutePath ) throws PackageException {

        this(atsAgent, absolutePath, null);
    }

    /**
     * Constructor
     *
     * @param atsAgent the address of the remote ATS agent where the entity is located on
     * @param absolutePath the absolute path and name of the entity, e.g. /root/file.ext
     * @param osType the operating system of the machine
     */
    public FilePackage( String atsAgent, String absolutePath, OperatingSystemType osType ) {

        this.atsAgent = atsAgent;
        this.path = IoUtils.getFilePath(absolutePath);
        this.name = IoUtils.getFileName(absolutePath);

        if (this.atsAgent == null) {
            this.fileSystemOperations = new FileSystemOperations();
            this.systemOperations = new SystemOperations();
        } else {
            this.fileSystemOperations = new FileSystemOperations(this.atsAgent);
            this.systemOperations = new SystemOperations(this.atsAgent);
        }

        if (osType == null) {
            this.osType = this.systemOperations.getOperatingSystemType();
        } else {
            this.osType = osType;
        }
    }

    /**
     * Constructor used for unit testing and mocking
     *
     * @param atsAgent the address of the remote ATS agent where the entity is located on
     * @param absolutePath the absolute path and name of the entity, e.g. /root/file.ext
     * @param osType the operating system of the machine
     */
    FilePackage( String atsAgent, String absolutePath, OperatingSystemType osType,
                 FileSystemOperations fileSystemOperations, SystemOperations systemOperations ) {

        this.atsAgent = atsAgent;
        this.path = IoUtils.getFilePath(absolutePath);
        this.name = IoUtils.getFileName(absolutePath);
        this.osType = osType;

        this.fileSystemOperations = fileSystemOperations;
        this.systemOperations = systemOperations;
    }

    public List<PackageHeader> getAllHeaders() throws PackageException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");
    }

    public List<InputStream> getAllStreams() throws PackageException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");
    }

    public String getDescription() throws PackageException {

        //the path to the file
        return getAbsolutePath();
    }

    public String[] getHeaderValues( String headerName ) throws PackageException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");

    }

    public String getSubject() throws PackageException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");

    }

    public String getTag() throws ActionException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");
    }

    public InputStream getWholePackage() throws PackageException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");
    }

    public void tag() throws ActionException {

        // FIXME either the interface should not define such a method or we need to somehow fit it into the abstraction
        throw new PackageException("Not available for file packages");
    }

    public String getName() {

        return name;
    }

    /**
     * Get the name of the owner of the entity. 
     * </br>Note: It returns null on Windows.
     * 
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getOwnerName() throws PackageException {

        //attribute not supported on windows
        if (osType == OperatingSystemType.WINDOWS) {
            return null;
        }

        //lazy initialization
        if (ownerName == null) {
            try {
                ownerName = this.fileSystemOperations.getFileOwner(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract owner name for file " + getAbsolutePath(), e);
            }
        }

        return ownerName;
    }

    /**
     * Get the name of the group of the entity. 
     * </br>Note: It returns null on Windows.
     * 
     * @return
     * @throws PackageException
     */
    @PublicAtsApi
    public String getGroupName() throws PackageException {

        //attribute not supported on windows
        if (osType == OperatingSystemType.WINDOWS) {
            return null;
        }

        //lazy initialization
        if (groupName == null) {
            try {
                groupName = this.fileSystemOperations.getFileGroup(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract group name for file " + getAbsolutePath(), e);
            }
        }

        return groupName;
    }

    @PublicAtsApi
    public long getGid() throws PackageException {

        //attribute not supported on windows
        if (osType == OperatingSystemType.WINDOWS) {
            return ATTRIBUTE_NOT_SUPPORTED;
        }

        //lazy initialization
        if (gid == null) {
            try {
                gid = this.fileSystemOperations.getFileGID(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract GID for file " + getAbsolutePath(), e);
            }
        }

        return gid;
    }

    @PublicAtsApi
    public long getUid() throws PackageException {

        //attribute not supported on windows
        if (osType == OperatingSystemType.WINDOWS) {
            return ATTRIBUTE_NOT_SUPPORTED;
        }

        //lazy initialization
        if (uid == null) {
            try {
                uid = this.fileSystemOperations.getFileUID(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract UID for file " + getAbsolutePath(), e);
            }
        }

        return uid;
    }

    /**
     * Returns the line of the {@link FilePackage} containing the specified expression.
     *
     * @param expression the expression to search for
     * @param isRegExp true if the expression provided is a regular expression
     * @return the lines, containing the expression
     * @throws PackageException
     */
    public String[] grep( String expression, boolean isRegExp ) throws PackageException {

        String[] contents;

        try {
            contents = this.fileSystemOperations.fileGrep(getAbsolutePath(), expression, !isRegExp);
        } catch (Exception e) {
            throw new PackageException("Could not extract contents for file " + getAbsolutePath(), e);
        }

        return contents;
    }

    /**
     * @return true if the entity represented by this package is a file and false
     * if the entity is a folder
     */
    public boolean isFile() throws PackageException {

        String fileList[];

        // extract the list of files with this name - since we are now specifically asking for
        // files then if the entity is a directory it would not come up in the list
        try {
            fileList = getFileSystemOperationsImplementationFor(atsAgent).findFiles(path, name, false,
                                                                                    false, false);

        } catch (Exception e) {
            throw new PackageException("Unable to get file list. " + e.getMessage(), e);
        }

        // check if the list is empty (no such file was found)
        if (fileList.length == 0) {
            return false;
        }

        return true;
    }

    @PublicAtsApi
    public long getPermissions() throws PackageException {

        //attribute not supported on windows
        if (osType == OperatingSystemType.WINDOWS) {
            return ATTRIBUTE_NOT_SUPPORTED;
        }

        //lazy initialization
        if (permissions == null) {
            try {
                permissions = Long.parseLong(this.fileSystemOperations.getFilePermissions(getAbsolutePath()));
            } catch (NumberFormatException e) {
                throw new PackageException("Could not convert permissions to Long for file "
                                           + getAbsolutePath(), e);
            } catch (Exception e) {
                throw new PackageException("Could not extract permissions for file " + getAbsolutePath(),
                                           e);
            }
        }

        return permissions;
    }

    @PublicAtsApi
    public long getModTime() throws PackageException {

        //lazy initialization
        if (modtime == null) {
            try {
                modtime = this.fileSystemOperations.getFileModificationTime(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract modification time for file "
                                           + getAbsolutePath(), e);
            }
        }

        return modtime;
    }

    public String getUniqueIdentifier() throws PackageException {

        try {
            return this.fileSystemOperations.getFileUniqueId(getAbsolutePath());
        } catch (Exception e) {
            throw new PackageException("Could not extract unique identifier for file " + getAbsolutePath(),
                                       e);
        }
    }

    @PublicAtsApi
    public long getSize() throws PackageException {

        //lazy initialization
        if (size == null) {
            try {
                size = this.fileSystemOperations.getFileSize(getAbsolutePath());
            } catch (Exception e) {
                throw new PackageException("Could not extract size for file '" + getAbsolutePath() + "'",
                                           e);
            }
        }

        return size;
    }

    @PublicAtsApi
    public String getMd5sum() throws PackageException {

        return getMd5sum(true);
    }

    @PublicAtsApi
    public String getMd5sum( boolean binaryMode ) throws PackageException {

        try {
            return this.fileSystemOperations.computeMd5Sum(getAbsolutePath(), binaryMode
                                                                                         ? Md5SumMode.BINARY
                                                                                         : Md5SumMode.ASCII);
        } catch (Exception e) {
            throw new PackageException(e);
        }
    }

    /**
     * @return the absolute path of the {@link FilePackage}
     */
    @PublicAtsApi
    public String getAbsolutePath() {

        return new StringBuilder().append(path).append(name).toString();
    }

    /**
     * @return the address of the remote ATS agent
     */
    public String getAtsAgent() {

        return this.atsAgent;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return getAbsolutePath();
    }

    private IFileSystemOperations getFileSystemOperationsImplementationFor( String atsAgent ) throws AgentException {

        if (HostUtils.isLocalAtsAgent(atsAgent)) {
            return new LocalFileSystemOperations();
        } else {
            return new RemoteFileSystemOperations(atsAgent);
        }
    }
}
