/*
 * Copyright 2020 Axway Software
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

package com.axway.ats.rbv.azure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.azure.BlobInfo;
import com.axway.ats.action.azure.BlobStorageOperations;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.model.MatchableAlreadyOpenException;
import com.axway.ats.rbv.model.MatchableNotOpenException;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.storage.Matchable;

/**
 * Class that encapsulates SINGLE Azure Blob Storage instance and retrieves {@link BlobStorageMetaData} for particular blob insde container<br>
 * All of the needed values (storage <strong>connection string</strong> and <strong>sas token</strong>, along with <strong>container name</strong> and <strong>blob name</strong>), are obtained from the {@link BlobStorageSearchTerm} object
 * 
 * */
public class BlobStorageFolder implements Matchable {

    private static final Logger        log = LogManager.getLogger(BlobStorageFolder.class);

    private boolean                    isOpen;
    private String                     containerName;
    private String                     blobName;
    private boolean                    searchRecursively;
    private boolean                    containerOperationsOnly;

    private BlobStorageOperations operations;
    private HashMap<String, MetaData>  allMetaData;
    private List<MetaData>             newMetaData;

    BlobStorageFolder( BlobStorageSearchTerm searchTerm ) {

        this.isOpen = false;
        this.containerName = searchTerm.getContainerName();
        this.blobName = searchTerm.getBlobName();
        this.searchRecursively = searchTerm.isRecursive();
        this.containerOperationsOnly = searchTerm.isContainerOperationsOnly();

        this.operations = new BlobStorageOperations(searchTerm.getConnectionString(), searchTerm.getSasToken());
        this.allMetaData = new HashMap<String, MetaData>();
        this.newMetaData = new ArrayList<MetaData>();
    }

    @Override
    public void open() throws RbvStorageException {

        //first check if the folder is already open
        if (isOpen) {
            throw new MatchableAlreadyOpenException("Azure Blob Storage folder is already open");
        }

        isOpen = true;

    }

    @Override
    public void close() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("Azure Blob Storage folder is not open");
        }

        isOpen = false;

    }

    @Override
    public List<MetaData> getAllMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("Azure Blob Storage folder is not open");
        }

        newMetaData.clear();

        HashMap<String, MetaData> tempMetaData = new HashMap<String, MetaData>();

        if (containerOperationsOnly) {
            MetaData md = getContainerAllMetaData();
            if (md != null) {
                tempMetaData.put(this.containerName, md);
            }
        } else {
            if (blobName == null) {
                blobName = ".*";
            }

            List<BlobInfo> blobs = null;

            String directory = null;
            String actualBlobName = blobName;
            if (blobName.contains("/")) {
                if (blobName.endsWith("/")) {
                    directory = blobName;
                    actualBlobName = ".*";
                } else {
                    directory = blobName.substring(0, blobName.lastIndexOf("/"));
                    actualBlobName = blobName.substring(blobName.lastIndexOf("/"));
                }
            }

            if (searchRecursively) {
                throw new RuntimeException("Not implemented for searchRecursively = " + searchRecursively);
            } else {
                if (StringUtils.isNullOrEmpty(directory)) {
                    blobs = this.operations.listBlobs(this.containerName, actualBlobName);
                } else {
                    blobs = this.operations.listBlobs(this.containerName, directory, actualBlobName);
                }

            }

            if (blobs != null) {
                for (BlobInfo blob : blobs) {
                    MetaData metaData = new BlobStorageMetaData();
                    metaData.putProperty(BlobStorageMetaData.BLOB_NAME, blob.getBlobName());
                    metaData.putProperty(BlobStorageMetaData.BLOB_TYPE, blob.getBlobType());
                    metaData.putProperty(BlobStorageMetaData.CONTAINER_NAME, blob.getContainerName());
                    metaData.putProperty(BlobStorageMetaData.CONTENT_TYPE, blob.getContentType());

                    metaData.putProperty(BlobStorageMetaData.CREATION_TIME, blob.getCreationTime());
                    metaData.putProperty(BlobStorageMetaData.E_TAG, blob.getETag());
                    metaData.putProperty(BlobStorageMetaData.LAST_MODIFIED, blob.getLastModified());
                    metaData.putProperty(BlobStorageMetaData.MD5, blob.getMd5());
                    metaData.putProperty(BlobStorageMetaData.META_DATA, blob.getMetadata());
                    metaData.putProperty(BlobStorageMetaData.SIZE, blob.getSize());

                    String hashKey = blob.toString();

                    if (!allMetaData.containsKey(hashKey)) {
                        newMetaData.add(metaData);
                    }

                    tempMetaData.put(hashKey, metaData);
                }
            }
        }

        allMetaData.clear();
        allMetaData.putAll(tempMetaData);
        return new ArrayList<MetaData>(allMetaData.values());

    }

    private MetaData getContainerAllMetaData() {

        if (operations.doesContainerExist(containerName)) {
            MetaData metadata = new MetaData();
            metadata.putProperty(BlobStorageMetaData.CONTAINER_NAME, this.containerName);
            return metadata;
        } else {
            return null;
        }

    }

    @Override
    public List<MetaData> getNewMetaData() throws RbvException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("Azure Blob Storage folder is not open");
        }

        getAllMetaData();

        return newMetaData;
    }

    @Override
    public String getDescription() {

        String description;

        String directory = null;
        if (!this.containerOperationsOnly && !StringUtils.isNullOrEmpty(blobName) && blobName.contains("/")) {
            if (blobName.endsWith("/")) {
                directory = blobName;
            } else {
                directory = blobName.substring(0, blobName.lastIndexOf("/"));
            }

            if (directory != null) {
                description = "folder '" + directory + "'";
            } else {
                description = "blob '" + blobName + "'";
            }

        } else {
            description = "container '" + containerName + "'";
        }

        return description;
    }

    @Override
    public String getMetaDataCounts() throws RbvStorageException {

        //first check if the folder is already open
        if (!isOpen) {
            throw new MatchableNotOpenException("Azure Blob Storage folder is not open");
        }

        return "Total objects: " + allMetaData.size() + ", new objects: " + newMetaData.size();
    }

}
