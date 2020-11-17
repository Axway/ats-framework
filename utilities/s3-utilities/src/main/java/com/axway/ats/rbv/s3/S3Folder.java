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

package com.axway.ats.rbv.s3;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.s3.S3ObjectInfo;
import com.axway.ats.action.s3.S3Operations;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

public class S3Folder implements Matchable {

    private static final Logger       log = LogManager.getLogger(S3Folder.class);

    private boolean                   isOpen;
    private String                    path;
    private String                    fileName;
    private boolean                   searchRecursively;

    private S3Operations              s3Operations;
    private HashMap<String, MetaData> allMetaData;
    private List<MetaData>            newMetaData;

    S3Folder( S3SearchTerm s3SearchTerm ) {

        this.isOpen = false;
        this.path = s3SearchTerm.getDirectory();
        this.fileName = s3SearchTerm.getFileName();
        this.searchRecursively = s3SearchTerm.isRecursive();

        this.s3Operations = new S3Operations(s3SearchTerm.getEndpoint(), s3SearchTerm.getAccessKey(),
                                             s3SearchTerm.getSecretKey(), s3SearchTerm.getRegion(),
                                             s3SearchTerm.getBucketName());
        this.allMetaData = new HashMap<String, MetaData>();
        this.newMetaData = new ArrayList<MetaData>();
    }

    public void open() throws RbvStorageException {

        //first check if the folder is already open
        if (isOpen) {
            throw new MatchableAlreadyOpenException("S3 folder is already open");
        }

        isOpen = true;
    }

    public void close() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("S3 folder is not open");
        }

        isOpen = false;
    }

    public String getMetaDataCounts() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("S3 folder is not open");
        }

        return "Total objects: " + allMetaData.size() + ", new objects: " + newMetaData.size();
    }

    public List<MetaData> getAllMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("S3 folder is not open");
        }

        newMetaData.clear();

        if (fileName == null) {
            fileName = ".*";
        }

        HashMap<String, MetaData> tempMetaData = new HashMap<String, MetaData>();

        //fetch dir contents recursively
        List<S3ObjectInfo> s3ObjectsList;
        try {
            s3ObjectsList = this.s3Operations.list(path, fileName, searchRecursively);
        } catch (Exception e) {
            final String notExistMessageSuffix = "does not exist";
            String exMessage = e.getMessage();
            Throwable cause = e.getCause();
            if ( (exMessage != null && exMessage.endsWith(notExistMessageSuffix))
                 || (cause != null && cause.getMessage() != null
                     && cause.getMessage().endsWith(notExistMessageSuffix))) {

                log.warn(getDescription() + " does not exist, skipping to next poll attempt");
                return new ArrayList<MetaData>();
            }
            throw new RbvException("Unable to list the contents of " + path, e);
        }

        if (s3ObjectsList != null) {

            for (S3ObjectInfo s3Object : s3ObjectsList) {

                // The way files are compared is by combining their
                // name+path and modification time in a hash string
                String hashKey = getUniqueIdentifier(s3Object);
                MetaData metaData = new MetaData();
                metaData.putProperty(S3MetaData.BUCKET_NAME, s3Object.getBucketName());
                metaData.putProperty(S3MetaData.FILE_NAME, s3Object.getName());
                metaData.putProperty(S3MetaData.MD5, s3Object.getMd5());
                metaData.putProperty(S3MetaData.LAST_MODIFIED, s3Object.getLastModified());
                metaData.putProperty(S3MetaData.SIZE, s3Object.getSize());

                if (!allMetaData.containsKey(hashKey)) {
                    newMetaData.add(metaData);
                }

                tempMetaData.put(hashKey, metaData);
            }
        }

        allMetaData.clear();
        allMetaData.putAll(tempMetaData);

        return new ArrayList<MetaData>(allMetaData.values());
    }

    private String getUniqueIdentifier( S3ObjectInfo s3Object ) {

        Date modTime = s3Object.getLastModified();
        // TODO optionally add bucket name
        return new StringBuilder().append(s3Object.getName()).append(".").append(modTime).toString();

    }

    public List<MetaData> getNewMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("S3 folder is not open");
        }

        getAllMetaData();

        return newMetaData;
    }

    public String getDescription() {

        String description;
        if (StringUtils.isNullOrEmpty(fileName)) {
            description = "folder '" + path + "'";
        } else {
            description = "file '" + path + fileName + "'";
        }

        return description;
    }
}
