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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.action.system.SystemOperations;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

public class FileSystemFolder implements Matchable {

    private static final Logger       log = LogManager.getLogger(FileSystemFolder.class);

    private boolean                   isOpen;
    private String                    atsAgent;
    private String                    path;
    private String                    fileName;
    private boolean                   isRegExp;
    private OperatingSystemType       osType;
    private boolean                   includeSubDirs;

    private FileSystemOperations      fileSystemOperations;
    private SystemOperations          systemOperations;

    private HashMap<String, MetaData> allMetaData;
    private List<MetaData>            newMetaData;

    FileSystemFolder( String atsAgent, String path, String fileName, boolean isRegExp,
                      boolean includeSubDirs ) {

        this.isOpen = false;
        this.atsAgent = atsAgent;
        this.path = path;
        this.fileName = fileName;
        this.isRegExp = isRegExp;
        this.includeSubDirs = includeSubDirs;
        this.fileSystemOperations = new FileSystemOperations(this.atsAgent);
        this.systemOperations = new SystemOperations(this.atsAgent);
        this.allMetaData = new HashMap<String, MetaData>();
        this.newMetaData = new ArrayList<MetaData>();
    }

    public void open() throws RbvStorageException {

        //first check if the folder is already open
        if (isOpen) {
            throw new MatchableAlreadyOpenException("File system folder is already open");
        }

        try {
            //get the name of the OS
            this.osType = this.systemOperations.getOperatingSystemType();

            // provide path valid for the target host
            this.path = IoUtils.normalizeDirPath(path, this.osType);
        } catch (Exception e) {
            throw new RbvStorageException("Could not open " + getDescription(), e);
        }

        isOpen = true;
    }

    public void close() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("File system folder is not open");
        }

        isOpen = false;
    }

    public String getMetaDataCounts() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("File system folder is not open");
        }

        return "Total files: " + allMetaData.size() + ", new files: " + newMetaData.size();
    }

    public List<MetaData> getAllMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("File system folder is not open");
        }

        newMetaData.clear();

        if (fileName == null) {
            fileName = ".*";
            isRegExp = true;
        }

        HashMap<String, MetaData> tempMetaData = new HashMap<String, MetaData>();

        //fetch dir contents recursively
        String[] fileList;
        try {
            fileList = this.fileSystemOperations.findFiles(path, fileName, isRegExp, true, includeSubDirs);
        } catch (Exception e) {
            final String notExistMessageSuffix = "does not exist or is not a folder";
            if ( (e.getMessage() != null && e.getMessage().endsWith(notExistMessageSuffix))
                 || (e.getCause() != null && e.getCause().getMessage() != null && e.getCause()
                                                                                   .getMessage()
                                                                                   .endsWith(notExistMessageSuffix))) {

                log.warn(getDescription() + " does not exist, skipping to next poll attempt");
                return new ArrayList<MetaData>();
            }
            throw new RbvException("Unable to list the contents of " + path, e);
        }

        if (fileList != null) {

            for (String fileName : fileList) {

                try {
                    FilePackage file = new FilePackage(atsAgent, fileName.trim(), osType);
                    MetaData metaData = new FileSystemMetaData(file);

                    // The way files are compared is by combining their name+path,
                    // modification time, user and group ID in a hash string
                    String hashKey = file.getUniqueIdentifier();

                    if (!allMetaData.containsKey(hashKey)) {
                        newMetaData.add(metaData);
                    }

                    tempMetaData.put(hashKey, metaData);
                } catch (PackageException e) {
                    // the creation of the package somehow failed - a simple explanation would be that
                    // the filed was removed during the execution of this method or something similar;
                    log.warn("Unable to build up metadata for " + fileName, e);
                    // either way we need not throw an exception but only continue iterating
                }
            }
        }

        allMetaData.clear();
        allMetaData.putAll(tempMetaData);

        return new ArrayList<MetaData>(allMetaData.values());
    }

    public List<MetaData> getNewMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("File system folder is not open");
        }

        getAllMetaData();

        return newMetaData;
    }

    public String getDescription() {

        String description;
        if (StringUtils.isNullOrEmpty(fileName)) {
            description = "folder '" + path + "'";
        } else {
            if (path.endsWith("\\") || path.endsWith("/")) {
                description = "file '" + path + fileName + "'";
            } else {
                description = "file '" + path + IoUtils.normalizeDirPath(File.separator) + fileName + "'";
            }
        }

        if (atsAgent.equals(FileSystemStorage.LOCAL_AGENT)) {
            return description;
        } else {
            return description + " on '" + atsAgent + "'";
        }
    }
}
