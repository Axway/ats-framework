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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.axway.ats.action.azure.BlobInfo;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.azure.rules.BlobFolderRule;
import com.axway.ats.rbv.azure.rules.BlobMd5BlobStorageRule;
import com.axway.ats.rbv.azure.rules.BlobModtimeBlobStorageRule;
import com.axway.ats.rbv.azure.rules.BlobSizeBlobStorageRule;
import com.axway.ats.rbv.clients.VerificationSkeleton;
import com.axway.ats.rbv.executors.MetaExecutor;
import com.axway.ats.rbv.model.RbvException;
import com.azure.storage.blob.models.BlobType;

/**
*
* Azure Blob Storage verification client.
* For examples and details refer to ATS framework guide
* <a href="https://axway.github.io/ats-framework/Common-test-verifications.html">here</a>
*
*/
@PublicAtsApi
public class BlobStorageVerifications extends VerificationSkeleton {

    private String monitorName = "azure_blob_storage_monitor_";

    /**
     * Construct object verification client for an Azure Blob Storage.<br>
     * Note that instance, created by this constructor can only work with containers! For blobs use {@link BlobStorageVerifications#BlobStorageVerifications(String, String, String, String, boolean)}
     * @param connectionString
     * @param sasToken
     * @param containerName  - the container name
     */
    public BlobStorageVerifications(
                                     String connectionString,
                                     String sasToken,
                                     String containerName ) {

        super();

        Validator argsValidator = new Validator();

        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, connectionString);
        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, sasToken);
        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, containerName);

        this.monitorName += "_$containerName: " + containerName;

        BlobStorage storage = new BlobStorage();
        folder = storage.getFolder(new BlobStorageSearchTerm(connectionString, sasToken, containerName, null,
                                                             false, true));
        this.executor = new MetaExecutor();
    }

    /**
     * Construct object verification client for an Azure Blob Storage, and additionally check particular blob inside a particular container
     * @param connectionString
     * @param sasToken
     * @param containerName  - the container name
     * @param recursive - should search for match be done recursively
     */
    public BlobStorageVerifications(
                                     String connectionString,
                                     String sasToken,
                                     String containerName,
                                     String blobName,
                                     boolean recursive ) {

        super();

        Validator argsValidator = new Validator();

        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, connectionString);
        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, sasToken);
        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, containerName);
        argsValidator.validate(ValidationType.STRING_NOT_EMPTY, blobName);

        //this.monitorName += "_connectionString: " + connectionString + "_sasToken: " + sasToken;

        this.monitorName += "_$containerName: " + containerName + "_$blobName: " + blobName;

        BlobStorage storage = new BlobStorage();
        folder = storage.getFolder(new BlobStorageSearchTerm(connectionString, sasToken, containerName, blobName,
                                                             recursive, false));
        this.executor = new MetaExecutor();
    }

    @PublicAtsApi
    public void checkBlobSize( long size ) {

        BlobSizeBlobStorageRule rule = new BlobSizeBlobStorageRule(size, "checkSize", true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkBlobSizeDifferent( long size ) {

        BlobSizeBlobStorageRule rule = new BlobSizeBlobStorageRule(size, "checkSizeDifferent", false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkBlobModificationTime( long modTime ) {

        BlobModtimeBlobStorageRule rule = new BlobModtimeBlobStorageRule(modTime, "checkModificationTime",
                                                                         true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkBlobModificationTimeDifferent( long modTime ) {

        BlobModtimeBlobStorageRule rule = new BlobModtimeBlobStorageRule(modTime,
                                                                         "checkModificationTimeDifferent",
                                                                         false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkBlobMd5( String md5 ) {

        BlobMd5BlobStorageRule rule = new BlobMd5BlobStorageRule(md5, "checkMd5", true);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public void checkBlobMd5Different( String md5 ) {

        BlobMd5BlobStorageRule rule = new BlobMd5BlobStorageRule(md5, "checkMd5Different", false);
        rootRule.addRule(rule);
    }

    @PublicAtsApi
    public BlobInfo[] verifyBlobExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyExists();

        return constructMatchedObjects(matchedMetaData);

    }

    @PublicAtsApi
    public BlobInfo[] verifyBlobAlwaysExists() throws RbvException {

        addFileCheckRule();

        List<MetaData> matchedMetaData = verifyAlwaysExists();

        return constructMatchedObjects(matchedMetaData);

    }

    @PublicAtsApi
    public void verifyBlobNeverExist() throws RbvException {

        addFileCheckRule();

        verifyNeverExists();
    }

    @PublicAtsApi
    public void verifyBlobDoesNotExist() throws RbvException {

        addFileCheckRule();

        verifyDoesNotExist();
    }

    @PublicAtsApi
    public void verifyContainerExists() throws RbvException {

        addContainerCheckRule();

        verifyExists();

    }

    @PublicAtsApi
    public void verifyContainerDoesNotExist() throws RbvException {

        addContainerCheckRule();

        verifyDoesNotExist();

    }

    @PublicAtsApi
    public void verifyContainerNeverExist() throws RbvException {

        addContainerCheckRule();

        verifyNeverExists();
    }

    @PublicAtsApi
    public void verifyContainerAlwaysExists() throws RbvException {

        addContainerCheckRule();

        verifyAlwaysExists();

    }

    private void addFileCheckRule() {

        // set the second highest priority for this rule - if the file path is correct the second most
        // important thing is to check if the entity is a file
        BlobFolderRule rule = new BlobFolderRule(true,
                                                 BlobFolderRule.CHECK_IS_BLOB_RULE_NAME,
                                                 true,
                                                 Integer.MIN_VALUE);
        rootRule.addRule(rule);
    }

    private void addContainerCheckRule() {

        // set the second highest priority for this rule - if the file path is correct the second most
        // important thing is to check if the entity is a file
        BlobFolderRule rule = new BlobFolderRule(false,
                                                 BlobFolderRule.CHECK_IS_CONTAINER_RULE_NAME,
                                                 true,
                                                 Integer.MIN_VALUE);
        rootRule.addRule(rule);
    }

    @Override
    protected String getMonitorName() {

        return monitorName;
    }

    private BlobInfo[] constructMatchedObjects( List<MetaData> matchedMetaData ) {

        BlobInfo[] matchedObjects = new BlobInfo[matchedMetaData.size()];
        for (int i = 0; i < matchedMetaData.size(); i++) {
            BlobInfo newMatchedObject = new BlobInfo();
            matchedObjects[i] = newMatchedObject;
            MetaData currentMetaData = matchedMetaData.get(i);
            newMatchedObject.setBlobName((String) currentMetaData.getProperty(BlobStorageMetaData.BLOB_NAME));
            newMatchedObject.setBlobType((com.axway.ats.action.azure.BlobInfo.BlobType) currentMetaData.getProperty(BlobStorageMetaData.BLOB_TYPE));
            newMatchedObject.setContainerName((String) currentMetaData.getProperty(BlobStorageMetaData.CONTAINER_NAME));
            newMatchedObject.setContentType((String) currentMetaData.getProperty(BlobStorageMetaData.CONTENT_TYPE));
            newMatchedObject.setCreationTime((Date) currentMetaData.getProperty(BlobStorageMetaData.CREATION_TIME));
            newMatchedObject.setETag((String) currentMetaData.getProperty(BlobStorageMetaData.E_TAG));
            newMatchedObject.setLastModified((Date) currentMetaData.getProperty(BlobStorageMetaData.LAST_MODIFIED));
            newMatchedObject.setMd5((String) currentMetaData.getProperty(BlobStorageMetaData.BLOB_NAME));
            newMatchedObject.setMetadata((Map<String, String>) currentMetaData.getProperty(BlobStorageMetaData.META_DATA));
            newMatchedObject.setSize((long) currentMetaData.getProperty(BlobStorageMetaData.SIZE));
        }

        return matchedObjects;
    }

}
