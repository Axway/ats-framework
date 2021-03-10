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
package com.axway.ats.action.azure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.azure.BlobInfo.BlobType;
import com.axway.ats.action.filesystem.FileSystemOperations;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobContainerListDetails;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobContainersOptions;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.specialized.AppendBlobAsyncClient;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobAsyncClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

/**
 * Class, encapsulating Azure Blob Storage operations
 * */
@PublicAtsApi
public class BlobStorageOperations {

    private static final Logger log                    = LogManager.getLogger(BlobStorageOperations.class);
    private static final long   DEFAULT_TIMEOUT_IN_SEC = 5 * 60;                                       // in seconds

    private BlobServiceClient   serviceClient;

    @PublicAtsApi
    public BlobStorageOperations( String connectionString, String sasToken ) {

        serviceClient = new BlobServiceClientBuilder().connectionString(connectionString)
                                                      .sasToken(sasToken)
                                                      .buildClient();
    }

    /**
     * Obtain list of the container's names<br>
     * Uses default timeout of {@link #DEFAULT_TIMEOUT_IN_SEC} seconds.
     * @see #listContainers(String, long)
     * @return list of container names
     * */
    @PublicAtsApi
    public List<String> listContainers() {

        return listContainers(null, DEFAULT_TIMEOUT_IN_SEC);
    }

    /**
     * Obtain list of the container's names
     * @param containerNamePrefix - specify container name prefix or the full container name
     * @param retrieveTimeout - the maximum amount of time (in seconds) to wait for the operation to complete.
     *                        If the operation did not complete in that time {@link AtsBlobStorageException} will
     *                        be thrown/raised
     * @return list of container names
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public List<String> listContainers( String containerNamePrefix,
                                        long retrieveTimeout ) {

        final List<String> containerNames = new ArrayList<String>();
        PagedIterable<BlobContainerItem> items = listContainers(containerNamePrefix,
                                                                retrieveTimeout, false); // or true!?!
        items.stream().forEach(new Consumer<BlobContainerItem>() {
            public void accept( BlobContainerItem t ) {

                containerNames.add(t.getName());
            }
        });
        return containerNames;
    }

    /**
     * Check whether a container with the specified name exists
     * @param containerName
     * @return true if exists of false if not
     * @throws AtsBlobStorageException - if exception occurred
     */
    @PublicAtsApi
    public boolean doesContainerExist( String containerName ) {

        log.info("Checking if container '" + containerName + "' exists ...");

        try {
            return serviceClient.getBlobContainerClient(containerName).exists();
        } catch (Exception e) {
            log.warn("Error while looking for container named '" + containerName + "'", e);
            return false;
        }
    }

    /**
     * Create new container. If the container already exists, a {@link AtsBlobStorageException} will be thrown.<br>
     * Note that this method may fail if the container was recently deleted. So you may have to wait a little before
     * invoking this method on already deleted container.
     *
     * @param containerName - the container name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void createContainer( String containerName ) {

        try {
            log.info("Creating container '" + containerName + "' ...");
            serviceClient.getBlobContainerClient(containerName).create();
            log.info("Container '" + containerName + "' successfully created.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not create container '" + containerName + "'", e);
        }

    }

    /**
     * Create new container
     * @param containerName - the new container name
     * @param timeoutSec - the maximum amount of time (in seconds) to wait for container to be created.
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void createContainer( String containerName, long timeoutSec ) {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSec * 1000; // convert to Ms

        boolean logBeingDeletedMessage = true;

        log.info("Creating container '" + containerName + "' while waiting up to " + timeoutSec
                 + " seconds for the operation to complete ...");

        Exception lastException = null;
        while (System.currentTimeMillis() - startTime <= timeoutMs) {

            try {
                serviceClient.getBlobContainerClient(containerName).create();
                log.info("Container '" + containerName + "' successfully created.");
                return;
            } catch (Exception e) {
                if (ExceptionUtils.containsMessage("InvalidResourceName", e, true)) {
                    throw new AtsBlobStorageException("Could not create container '" + containerName
                                                      + "' due to container name, having invalid characters", e);
                } else if (ExceptionUtils.containsMessage("ContainerBeingDeleted", e, true)
                           && logBeingDeletedMessage) {
                               logBeingDeletedMessage = false;
                               log.warn("Container '" + containerName
                                        + "' is currently being deleted!. This can lead to failure "
                                        + "of createContainer method. You should increase the timeout for creation of this "
                                        + "container if this error is persistent");
                           }
                lastException = e;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        throw new AtsBlobStorageException("Could not create container '" + containerName + "' in " + timeoutSec
                                          + " seconds", lastException);
    }

    /**
     * Delete existing container. If the container exists, a {@link BlobStorageException} will be thrown<br>
     * Note that there is a delay between calling this method and the container being deleted, so invoking
     * {@code BlobOperations#createContainer(String)} with the same container name may fail.
     * @param containerName - the container name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void deleteContainer( String containerName ) {

        try {
            log.info("Deleting container '" + containerName + "' ...");

            serviceClient.getBlobContainerClient(containerName).delete();

            log.info("Container '" + containerName + "' successfully deleted.");

        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not delete container '" + containerName + "'", e);
        }

    }

    /**
     * Delete existing container
     * @param containerName - the container name
     * @param timeoutSec - the maximum amount of time (in seconds) to wait for container to be deleted.
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void deleteContainer( String containerName, long timeoutSec ) {

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSec * 1000;

        BlobStorageException lastException = null;
        while (System.currentTimeMillis() - startTime <= timeoutMs) {

            try {
                this.deleteContainer(containerName);
                if (!this.doesContainerExist(containerName)) {
                    return;
                }
            } catch (BlobStorageException bse) {
                lastException = bse;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        throw new AtsBlobStorageException("Could not delete container '" + containerName + "' in " + timeoutSec
                                          + " seconds", lastException);
    }

    @PublicAtsApi
    public void deleteContainers( List<String> containerNames ) {

        this.deleteContainers(containerNames, DEFAULT_TIMEOUT_IN_SEC);
    }

    @PublicAtsApi
    public void deleteContainers( List<String> containerNames, long timeoutSec ) {

        try {
            if (containerNames == null || containerNames.isEmpty()) {
                throw new IllegalArgumentException("Container names must not be null/empty!");
            }

            for (String containerName : containerNames) {
                this.deleteContainer(containerName, timeoutSec);
            }
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not delete containers", e);
        }
    }

    /**
     * Delete all blobs and directories/folders for container. Essentially leaving the container empty
     * @param containerName - the containerName 
     *@throws AtsBlobStorageException - if exception occurred
     * **/
    @PublicAtsApi
    public void purgeContainer( String containerName ) {

        try {
            log.info("Purging container '" + containerName + "' ...");

            List<BlobInfo> blobs = listBlobs(containerName);
            if (blobs == null || blobs.isEmpty()) {
                log.info("Container '" + containerName + "' has no blobs inside. Nothing to purge.");
            } else {
                log.info("Container '" + containerName + "' has " + blobs.size() + " blobs inside. Begin purging of "
                         + "all blobs ...");
            }
            for (BlobInfo blob : blobs) {
                this.deleteBlob(containerName, blob.getBlobName());
            }

            if (!isContainerEmpty(containerName)) {
                blobs = listBlobs(containerName);
                // if needed, left objects could be listed after exception is caught
                throw new AtsBlobStorageException("Container '" + containerName
                                                  + "' could not be properly purged. Blobs left: " + blobs.size());
            }
            log.info("Container '" + containerName + "' successfully purged.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not purge container '" + containerName + "'", e);
        }

    }

    /**
     * Check whether container has more than one blob
     * @param containerName - the container name
     * @return true or false
     * */
    @PublicAtsApi
    public boolean isContainerEmpty( String containerName ) {

        try {
            log.info("Checking if container '" + containerName + "' is empty ...");

            return this.serviceClient.getBlobContainerClient(containerName).listBlobs().stream().count() <= 0;
        } catch (Exception e) {
            log.warn("Error while checking if container'" + containerName + "' is empty", e);
            return false;
        }

    }

    /**
     * List all blobs from container
     * @param containerName - the container name
     * @return list of {@link BlobInfo}
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName ) {

        return listBlobs(containerName, null, null, DEFAULT_TIMEOUT_IN_SEC);
    }

    /**
     * List blobs, with specified name prefix, from container
     * @param containerName - the container name
     * @param prefix - blob names prefix. Example: prefix = "blob-", will return blobs with name, starting with blob-
     * @return list of {@link BlobInfo}
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix ) {

        return listBlobs(containerName, prefix, null, DEFAULT_TIMEOUT_IN_SEC);

    }

    /**
     * List blobs from directory inside container
     * @param containerName - the container name
     * @param prefix - blob names prefix. Example: prefix = "blob-", will return blobs with name, starting with blob-
     * @param directory - the directory name. Could by a nested directory as well, like foo/bar/baz
     * @return list of {@link BlobInfo}
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix, String directory ) {

        return listBlobs(containerName, prefix, directory, DEFAULT_TIMEOUT_IN_SEC);
    }

    /**
     * List blobs from container 
     * @param containerName - the container name
     * @param prefix - prefix for the blobs names or null for all blobs
     * @param directory - the directory which will be listed, or null to search the whole container
     * @param retrieveTimeout - the maximum amount of time (in seconds) to wait for the operation to complete. If the operation did not complete in that time {@link BlobStorageException} will be thrown/raised. Pass 0 (zero) to use the default value
     * @return list {@link BlobInfo}
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix, String directory, long retrieveTimeout ) {

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Listing blobs");
            log.info(sb.toString());

            final List<BlobInfo> infos = new ArrayList<BlobInfo>();
            PagedIterable<BlobItem> blobs = null;

            ListBlobsOptions lbops = new ListBlobsOptions();
            if (!StringUtils.isNullOrEmpty(prefix)) {
                lbops.setPrefix(prefix);
                sb.append(" with prefix '" + prefix + "'");
            }

            if (retrieveTimeout <= 0) {
                retrieveTimeout = Integer.MAX_VALUE / 2; // just a little less than too much
            }

            if (!StringUtils.isNullOrEmpty(directory)) {
                String newPrefix = directory + "/" + prefix;
                lbops.setPrefix(newPrefix);
                sb.append(" from directory '" + directory + "'");
                sb.append(" in container '" + containerName + "' ...");
                log.info(sb.toString());
                blobs = serviceClient.getBlobContainerClient(containerName)
                                     .listBlobsByHierarchy("/", lbops, Duration.ofSeconds(retrieveTimeout));
            } else {
                sb.append(" in container '" + containerName + "' ...");
                log.info(sb.toString());
                blobs = serviceClient.getBlobContainerClient(containerName)
                                     .listBlobs(lbops, Duration.ofSeconds(retrieveTimeout));
            }

            if (blobs != null) {
                blobs.stream().forEach(new Consumer<BlobItem>() {
                    public void accept( BlobItem blobItem ) {

                        BlobInfo info = new BlobInfo();
                        BlobItemProperties properties = blobItem.getProperties();

                        info.setAccessTier(BlobInfo.toAtsAccessTier(properties.getAccessTier()));
                        info.setBlobName(blobItem.getName());
                        info.setBlobType(BlobInfo.toAtsBlobType(properties.getBlobType()));
                        info.setContainerName(containerName);
                        info.setContentType(properties.getContentType());

                        if (properties.getCreationTime() != null) {
                            info.setCreationTime(Date.from(properties.getCreationTime().toInstant()));
                        } else {
                            info.setCreationTime(null);
                        }

                        info.setETag(properties.getETag());

                        if (properties.getLastModified() != null) {
                            info.setLastModified(Date.from(properties.getLastModified().toInstant()));
                        } else {
                            info.setLastModified(null);
                        }

                        if (properties.getContentMd5() != null) {
                            info.setMd5(java.util.Base64.getEncoder().encodeToString(properties.getContentMd5()));
                        } else {
                            info.setMd5(null);
                        }

                        info.setMetadata(blobItem.getMetadata());
                        info.setSize(properties.getContentLength());

                        infos.add(info);
                    }
                });
            }

            return infos;
        } catch (Exception e) {
            String errorMessage = "Could not list blobs from container '" + containerName + "'"
                                  + (!StringUtils.isNullOrEmpty(prefix)
                                                                        ? " with prefix '" + prefix + "'"
                                                                        : "")
                                  + (!StringUtils.isNullOrEmpty(directory)
                                                                           ? " in directory '" + directory + "'"
                                                                           : "")
                                  + " in " + retrieveTimeout + " seconds";
            throw new AtsBlobStorageException(errorMessage, e);
        }

    }

    /**
     * Check whether a blob in a container exists
     * @param containerName - the container name
     * @param blobName - the blob name
     * @return true if exists or false if not
     */
    @PublicAtsApi
    public boolean doesBlobExist( String containerName, String blobName ) {

        try {
            log.info("Checking if blob '" + blobName + "' from container '" + containerName + "' exists ...");

            return serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).exists();
        } catch (Exception e) {
            log.warn("Error while checking if blob '" + blobName + "' in container '" + containerName + "' exists", e);
            return false;
        }

    }

    /**
     * Get blob information/properties
     * @param containerName - the container name
     * @param blobName - the blob name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public BlobInfo getBlobInfo( String containerName, String blobName ) {

        try {
            log.info("Getting info for blob '" + blobName + "' from container '" + containerName + "' ...");
            BlobProperties props = serviceClient.getBlobContainerClient(containerName)
                                                .getBlobClient(blobName)
                                                .getProperties();
            return new BlobInfo(containerName, blobName, props);
        } catch (Exception e) {
            if (ExceptionUtils.containsMessage("Status code 404, (empty body)", e, true)) {
                throw new AtsBlobStorageException("Blob '" + blobName + "' does not exist in container '"
                                                  + containerName
                                                  + "'", e);
            } else {
                throw new AtsBlobStorageException("Could not get blob info for blob '" + blobName + "' in container '"
                                                  + containerName + "'", e);
            }
        }

    }

    /**
     * Set metadata for blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param newMetadata - the new metadata to be added
     * @param overwriteExistingKeys - whether to overwrite already existing metadata key
     * @param includeOldMetadata - whether to include the old metadata, along with the new one
     * @param valueDelimiter - specify metadata value delimiter. 
     * Used when both overwriteExistingKeys and includeOldMetadata are true and there are metadata keys in both the old and new data.
     * The final metadata value will be &lt;key&gt;=&lt;old_value&gt;&lt;valueDelimiter&gt;&lt;new_value&gt;
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void setBlobMetadata( String containerName, String blobName, Map<String, String> newMetadata,
                                 boolean overwriteExistingKeys, boolean includeOldMetadata, String valueDelimiter ) {

        try {
            log.info("Setting metadata for blob '" + blobName + "' from container '" + containerName + "' ...");

            Map<String, String> metadataToBeSent = new HashMap<String, String>();
            if (includeOldMetadata) {
                // keep the old metadata
                metadataToBeSent.putAll(this.getBlobInfo(containerName, blobName).getMetadata());
            }

            if (overwriteExistingKeys) {
                // overwrite the old metadata where appropriate
                metadataToBeSent.putAll(newMetadata);
            } else {
                // we should append the new values where appropriate
                // traverse only the keys, where overwrite is possible 
                for (String key : metadataToBeSent.keySet()) {
                    if (newMetadata.containsKey(key)) {
                        String oldValue = metadataToBeSent.get(key);
                        String newValue = newMetadata.get(key);
                        metadataToBeSent.put(key, oldValue + valueDelimiter + newValue);
                    }
                }

                for (String key : newMetadata.keySet()) {
                    if (!metadataToBeSent.containsKey(key)) {
                        // set only keys that will not overwrite anything in the old metadata
                        metadataToBeSent.put(key, newMetadata.get(key));
                    }

                }
            }

            serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).setMetadata(metadataToBeSent);

            log.info("Successfully set metadata for blob '" + blobName + "' from container '" + containerName
                     + "' ...");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not set metadata for blob '" + blobName + "' in container '"
                                              + containerName + "'", e);
        }

    }

    /**
     * Delete blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void deleteBlob( String containerName, String blobName ) {

        try {
            log.info("Deleting blob '" + blobName + "' from container '" + containerName + "' ...");

            serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).delete();

            log.info("Blob '" + blobName + "' successfully deleted from container '" + containerName + "' ...");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not delete blob '" + blobName + "' from container '"
                                              + containerName
                                              + "'", e);
        }

    }

    @PublicAtsApi
    public void deleteBlobs( String containerName, List<String> blobNames ) {

        try {
            if (blobNames == null || blobNames.isEmpty()) {
                throw new IllegalArgumentException("Blob names must not be null/empty!");
            }

            for (String blobName : blobNames) {
                this.deleteBlob(containerName, blobName);
            }

        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not delete blobs from container '" + containerName + "'", e);
        }

    }

    /*public void undeleteBlob( String containerName, String blobName ) {
    
        log.info("Undeleting blob '" + blobName + "' from container '" + containerName + "' ...");
    
        serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).undelete();
    
        log.info("Blob '" + blobName + "' successfully undeleted from container '" + containerName + "' ...");
    }*/

    /*
     * Rename existing blob<br>
     * Note that the creation and last modified time will be updated and only block blobs can be renamed
     * @param containerName - the container name
     * @param oldBlobName - the old blob name
     * @param newBlobName - the new blob name
     *
             @PublicAtsApi
             public void renameBlob( String containerName, String oldBlobName, String newBlobName ) {
    
             log.info("Renaming blob '" + oldBlobName + "' to '" + newBlobName + "' inside container '" + containerName
                     + "' ...");
    
             try {
                String oldBlobUrl = this.serviceClient.getBlobContainerClient(containerName)
                                                      .getBlobClient(oldBlobName)
                                                      .getBlobUrl()
                                    + this.sasToken;
    
                AzureBlobInfo origInfo = this.getBlobInfo(containerName, oldBlobName);
    
                if (origInfo.getBlobType() != BlobType.BLOCK_BLOB) {
                    throw new AzureBlobStorageException("Only block blobs can be renamed!");
                }
    
                this.serviceClient.getBlobContainerClient(containerName)
                                  .getBlobClient(newBlobName)
                                  .copyFromUrlWithResponse(oldBlobUrl,
                                                           origInfo.getMetadata(),
                                                           origInfo.getAccessTier(),
                                                           null,
                                                           null,
                                                           null, null);
    
                this.deleteBlob(containerName, oldBlobName);
             } catch (Exception e) {
                throw new AzureBlobStorageException("Unable to rename blob from '" + oldBlobName + "' to '" + newBlobName
                                           + "' inside container '" + containerName + "'", e);
             }
    
             log.info("Successfully renamed blob from '" + oldBlobName + "' to '" + newBlobName + "' inside container '"
                     + containerName + "'.");
    
             }*/

    /**
     * Upload local file to a block blob
     * @param containerName - the container name, where that blob will be uploaded
     * @param blobName - the blob name, which will be created by the upload. If null, the name of the local file will be used
     * @param localFilepath - path to the file to be uploaded
     * @param overwrite - whether to overwrite an existing blob with the same name or not
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi

    public void upload( String containerName, String blobName, String localFilepath, boolean overwrite ) {

        try {
            if (blobName == null || blobName.isEmpty()) {
                String[] tokens = localFilepath.replace("\\", "/").split("/");
                blobName = tokens[tokens.length - 1];
            }

            log.info("Uploading " + ( (overwrite)
                                                  ? "(overwrite enabled)"
                                                  : "")
                     + " '" + localFilepath + "' to container '" + containerName + "' as a blob, named '" + blobName
                     + "' ...");

            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .uploadFromFile(localFilepath, overwrite);

            log.info("Successfully uploaded '" + localFilepath + "' to container '" + containerName
                     + "' as a blob, named '"
                     + blobName + "'.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not upload file '" + localFilepath + "' as blob '" + blobName
                                              + " in container '" + containerName + "'", e);
        }
    }

    /**
     * Upload local file to a blob with specified type
     * @param containerName - the container name, where that blob will be uploaded
     * @param blobName - the blob name, which will be created by the upload. If null, the name of the local file will be used
     * @param blobType - specify what will be the type of the uploaded blob
     * @param localFilepath - path to the file to be uploaded
     * @param overwrite - whether to overwrite an existing blob with the same name or not
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void upload( String containerName, String blobName, BlobType blobType, String localFilepath,
                        boolean overwrite ) {

        try {

            if (blobType == null) {
                throw new IllegalArgumentException("Blob type must not be null!");
            }

            if (blobName == null || blobName.isEmpty()) {
                String[] tokens = localFilepath.replace("\\", "/").split("/");
                blobName = tokens[tokens.length - 1];
            }

            log.info("Uploading " + ( (overwrite)
                                                  ? "(overwrite enabled)"
                                                  : "")
                     + " '" + localFilepath + "' to container '" + containerName + "' as a/an "
                     + blobType.name().split("_")[0] + " blob, named '" + blobName
                     + "' ...");

            switch (blobType) {
                case APPEND_BLOB:
                    uploadFileAsAppendBlob(containerName, blobName, localFilepath, overwrite);
                    break;
                case BLOCK_BLOB:
                    uploadFileAsBlockBlob(containerName, blobName, localFilepath, overwrite);
                    break;
                case PAGE_BLOB:
                    uploadFileAsPageBlob(containerName, blobName, localFilepath, overwrite);
                    break;
                default:
                    throw new IllegalArgumentException("Blob type '" + blobType.name() + "' is not supported");
            }

            log.info("Successfully uploaded '" + localFilepath + "' to container '" + containerName
                     + "' as a/an " + blobType.name().split("_")[0] + " blob, named '"
                     + blobName + "'.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not upload file '" + localFilepath + "' as a/an "
                                              + blobType.name().split("_")[0] + " blob '" + blobName
                                              + " in container '" + containerName + "'", e);
        }

    }

    /**
     * Upload stream to a blob with specified type. Note that you are responsible to close the provided stream, regardless of the exit status of this method
     * @param containerName - the container name, where that blob will be uploaded
     * @param blobName - the blob name, which will be created by the upload. If null, the name of the local file will be used
     * @param blobType - specify what will be the type of the uploaded blob
     * @param contentStream - the stream that will be uploaded as a blob
     * @param contentLength - exact number of bytes to be read for the content stream (underlying implementation limitation). If the blob type is {@link BlobType#APPEND_BLOB} this argument has no effect (pass whatever you want)
     * @param overwrite - whether to overwrite an existing blob with the same name or not
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void upload( String containerName, String blobName, BlobType blobType, InputStream contentStream,
                        long contentLength,
                        boolean overwrite ) {

        try {
            if (blobType == null) {
                throw new IllegalArgumentException("Blob type must not be null!");
            }

            if (contentStream == null) {
                throw new IllegalArgumentException("Content stream must not be null");
            }

            log.info("Uploading " + ( (overwrite)
                                                  ? "(overwrite enabled)"
                                                  : "")
                     + contentStream.getClass().getName() + " stream to container '" + containerName + "' as a/an "
                     + blobType.name().split("_")[0] + " blob, named '" + blobName
                     + "' ...");

            switch (blobType) {
                case APPEND_BLOB:
                    uploadStreamAsAppendBlob(containerName, blobName, contentStream, contentLength, overwrite);
                    break;
                case BLOCK_BLOB:
                    uploadStreamAsBlockBlob(containerName, blobName, contentStream, contentLength, overwrite);
                    break;
                case PAGE_BLOB:
                    uploadStreamAsPageBlob(containerName, blobName, contentStream, contentLength, overwrite);
                    break;
                default:
                    throw new IllegalArgumentException("Blob type '" + blobType.name() + "' is not supported");
            }

            log.info("Successfully uploaded " + contentStream.getClass().getName() + " to container '" + containerName
                     + "' as a/an " + blobType.name().split("_")[0] + " blob, named '"
                     + blobName + "'.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not upload stream as a/an blob '" + blobName + "' in container '"
                                              + containerName + "'", e);
        }

    }

    private void uploadStreamAsPageBlob( String containerName, String blobName, InputStream contentStream,
                                         long contentLength,
                                         boolean overwrite ) throws IOException {

        long finalPageBlockSize = calculatePageBlobSize(contentLength);
        PageRange pageRange = new PageRange();
        long startPageRange = 0;
        long endPageRange = finalPageBlockSize - 1;
        pageRange.setStart(startPageRange);
        pageRange.setEnd(endPageRange);

        if (overwrite) {
            if (this.doesBlobExist(containerName, blobName)) {
                this.deleteBlob(containerName, blobName);
            }
        }

        this.createPageBlob(containerName, blobName, finalPageBlockSize, overwrite);

        try (BlobOutputStream bos = this.serviceClient.getBlobContainerClient(containerName)
                                                      .getBlobClient(blobName)
                                                      .getPageBlobClient()
                                                      .getBlobOutputStream(pageRange)) {

            // create the append blob
            long bytesTransferred = 0;
            int defaultBufferSize = PageBlobAsyncClient.MAX_PUT_PAGES_BYTES; // well the maximum value for single put page operation
            byte[] buffer = new byte[defaultBufferSize];
            int readBytes = 0;
            while ( (readBytes = contentStream.read(buffer)) != -1) {
                // append the next block of data
                if (readBytes < defaultBufferSize) {
                    buffer = Arrays.copyOf(buffer, readBytes);
                }
                bos.write(buffer);
                bytesTransferred += readBytes;
                double uploadedPercentage = ((double) (bytesTransferred) / (double) (contentLength)) * 100.0;
                log.info(String.format("Bytes transferred: %d (%.2f %%)", bytesTransferred,
                                       uploadedPercentage));
            }
            bos.flush();
        }

    }

    private void uploadStreamAsBlockBlob( String containerName, String blobName, InputStream contentStream,
                                          long contentLength,
                                          boolean overwrite ) throws IOException {

        try (BlobOutputStream bos = this.serviceClient.getBlobContainerClient(containerName)
                                                      .getBlobClient(blobName)
                                                      .getBlockBlobClient()
                                                      .getBlobOutputStream(overwrite)) {

            // create the append blob
            long bytesTransferred = 0;
            int defaultBufferSize = 1024 * 1024 * 10; // 10 MB
            byte[] buffer = new byte[defaultBufferSize];
            int readBytes = 0;
            while ( (readBytes = contentStream.read(buffer)) != -1) {
                // append the next block of data
                if (readBytes < defaultBufferSize) {
                    buffer = Arrays.copyOf(buffer, readBytes);
                }
                bos.write(buffer);
                bytesTransferred += readBytes;
                double uploadedPercentage = ((double) (bytesTransferred) / (double) (contentLength)) * 100.0;
                log.info(String.format("Bytes transferred: %d (%.2f %%)", bytesTransferred,
                                       uploadedPercentage));
            }
            bos.flush();;
        }

    }

    private void uploadStreamAsAppendBlob( String containerName, String blobName, InputStream contentStream,
                                           long contentLength,
                                           boolean overwrite ) throws IOException {

        if (overwrite) {
            if (this.doesBlobExist(containerName, blobName)) {
                this.deleteBlob(containerName, blobName);
            }
        }

        // create the append blob
        this.createAppendBlob(containerName, blobName, overwrite);
        long bytesTransferred = 0;
        int defaultBufferSize = AppendBlobAsyncClient.MAX_APPEND_BLOCK_BYTES; // well the maximum value for single append operation
        byte[] buffer = new byte[defaultBufferSize];
        int readBytes = 0;
        while ( (readBytes = contentStream.read(buffer)) != -1) {
            // append the next block of data
            if (readBytes < defaultBufferSize) {
                buffer = Arrays.copyOf(buffer, readBytes);
            }
            this.appendToBlob(containerName, blobName, buffer);
            bytesTransferred += readBytes;
            double uploadedPercentage = ((double) (bytesTransferred) / (double) (contentLength)) * 100.0;
            log.info(String.format("Bytes transferred: %d (%.2f %%)", bytesTransferred,
                                   uploadedPercentage));
        }

    }

    private void uploadFileAsPageBlob( String containerName, String blobName, String localFilepath,
                                       boolean overwrite ) throws FileNotFoundException, IOException {

        try (FileInputStream fis = new FileInputStream(new File(localFilepath))) {
            this.uploadStreamAsPageBlob(containerName, blobName, fis,
                                        new FileSystemOperations().getFileSize(localFilepath),
                                        overwrite);
        }

    }

    private void uploadFileAsAppendBlob( String containerName, String blobName, String localFilepath,
                                         boolean overwrite ) throws FileNotFoundException, IOException {

        try (FileInputStream fis = new FileInputStream(new File(localFilepath))) {
            this.uploadStreamAsAppendBlob(containerName, blobName, fis,
                                          new FileSystemOperations().getFileSize(localFilepath),
                                          overwrite);
        }

    }

    private void uploadFileAsBlockBlob( String containerName, String blobName, String localFilepath,
                                        boolean overwrite ) throws FileNotFoundException, IOException {

        /*//requestConditions = new BlobRequestConditions().setIfNoneMatch(Constants.HeaderConstants.ETAG_WILDCARD);
        //uploadFromFile(filePath, null, null, null, null, requestConditions, null);
        
        this.serviceClient.getBlobContainerClient(containerName)
                          .getBlobClient(blobName)
                          .uploadFromFile(filePath, parallelTransferOptions, headers, metadata, tier, requestConditions, timeout);*/

        try (FileInputStream fis = new FileInputStream(new File(localFilepath))) {
            this.uploadStreamAsBlockBlob(containerName, blobName, fis,
                                         new FileSystemOperations().getFileSize(localFilepath),
                                         overwrite);
        }

    }

    /**
     * Download blob to a local file
     * @param containerName - the container name, containing the blob
     * @param blobName - the blob's name to be downloaded
     * @param localFilepath - path to the file where the blob will be saved
     * @param overwrite - whether to overwrite an existing local file with the blob's content or not
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void download( String containerName, String blobName, String localFilepath, boolean overwrite ) {

        try {
            log.info("Downloading " + ( (overwrite)
                                                    ? "(overwrite enabled)"
                                                    : "")
                     + " blob '" + blobName + "' from container '" + containerName + "' to file '" + localFilepath
                     + "' ...");

            final long actualSize = this.getBlobInfo(containerName, blobName).getSize();

            Thread monitorThread = createDownloadMonitorThread(containerName, blobName, localFilepath, actualSize);

            log.info("Starting monitoring (download) thread - " + monitorThread.getName());

            monitorThread.start();

            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .downloadToFile(localFilepath, overwrite);

            log.info("Stopping monitoring (download) thread - " + monitorThread.getName());

            monitorThread.interrupt();

            log.info("Successfully downloaded blob '" + blobName + "' from container '" + containerName + "' to file '"
                     + localFilepath + "'.");

        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not download blob '" + blobName + "' from container '"
                                              + containerName
                                              + "' to file '" + localFilepath + "'", e);
        }

    }

    private Thread createDownloadMonitorThread( String containerName, String blobName, String localFilepath,
                                                long actualSize ) {

        return new Thread(new Runnable() {

            @Override
            public void run() {

                FileSystemOperations fso = new FileSystemOperations();

                long oldSize = 0;
                long newSize = 0;
                boolean fileExists = false;
                boolean aboutToExit = false;
                long aboutToExitStartTime = 0;
                int waitMinutes = 10;
                while (newSize < actualSize) {
                    if (!fileExists) {
                        fileExists = fso.doesFileExist(localFilepath);
                    }
                    if (fileExists) {
                        newSize = fso.getFileSize(localFilepath);
                        double downloadedPercentage = ((double) (newSize) / (double) (actualSize)) * 100.0;
                        log.info(String.format("Bytes downloaded: %d (%.2f %%)", newSize - oldSize,
                                               downloadedPercentage));;

                        if (aboutToExit) {
                            if (System.currentTimeMillis()
                                - aboutToExitStartTime > TimeUnit.MINUTES.toMillis(waitMinutes)) {
                                log.error("Exitting monitor thread, as there was no bytes downloaded from blob to file '"
                                          + localFilepath + "' in the last " + waitMinutes + " minute(s)!");
                                break;
                            }
                        } else {
                            if (oldSize == newSize && newSize != 0) { // != so we ignore the initial tick, where both sizes are zero
                                if (newSize < actualSize) {
                                    log.warn("No tranfer of bytes! Waiting " + waitMinutes
                                             + " minutes, before exitting thread.");
                                    aboutToExit = true;
                                    aboutToExitStartTime = System.currentTimeMillis();
                                }
                            } else {
                                // clear flags, due to bytes recently being transfered(downloaded)
                                aboutToExit = false;
                                aboutToExitStartTime = 0;
                            }
                        }
                        oldSize = newSize;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}

                }

            }
        }, "azure-blob-storage-download-file-monitor-thread-" + containerName + "-" + blobName);
    }

    /**
     * Create page blob. If the size is not multiple of {@link PageBlobAsyncClient#PAGE_BYTES}, this method will automatically align the user-provided size to the nearest (greater) appropriate one and return the actual blob size
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param size - the blob size. Note that it must be multiple of {@link PageBlobAsyncClient#PAGE_BYTES}
     *             (currently 512 bytes)
     * @param overwrite - whether to overwrite any existing blob with the same name
     * @return - the actual size of the created blob
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public long createPageBlob( String containerName, String blobName, long size, boolean overwrite ) {

        try {

            log.info("Creating " + ( (overwrite)
                                                 ? "or overwriting existing"
                                                 : "")
                     + " page blob '" + blobName + "' in container '" + containerName + "' with size '" + size
                     + "' ...");

            if (size % PageBlobAsyncClient.PAGE_BYTES != 0) {
                log.warn("Page blobs expect size, multiple of " + PageBlobAsyncClient.PAGE_BYTES
                         + ", but the provided size is not (" + size
                         + "). ATS will transform it to the nearest (greather) value!");
                size = calculatePageBlobSize(size);
                log.warn("New size for page blob '" + blobName + "' in container '" + containerName + "' will be "
                         + size + " bytes");
            }

            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getPageBlobClient()
                         .create(size, overwrite);

            log.info("Successfully created page blob '" + blobName + "' in container '" + containerName + "' with size "
                     + size + ".");

            return size;
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not create page blob '" + blobName + "' in container '"
                                              + containerName + "'", e);
        }

    }

    /**
     * Create append blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param overwrite - whether to overwrite any existing blob with the same name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void createAppendBlob( String containerName, String blobName, boolean overwrite ) {

        try {

            log.info("Creating " + ( (overwrite)
                                                 ? "or overwriting existing"
                                                 : "")
                     + " append blob '" + blobName + "' in container '" + containerName + "' ...");

            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getAppendBlobClient()
                         .create(overwrite);

            log.info("Successfully created append blob '" + blobName + "' in container '" + containerName + "'.");

        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not create block blob '" + blobName + "' in container '"
                                              + containerName + "'", e);
        }

    }

    /**
     * Create a block blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content of the blob
     * @param overwrite - whether to overwrite any existing blob with the same name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void createBlockBlob( String containerName, String blobName, byte[] content, boolean overwrite ) {

        ByteArrayInputStream bais = null;
        try {
            if (content == null) {
                throw new IllegalArgumentException("Content must not be null");
            }
            bais = new ByteArrayInputStream(content);
            this.createBlockBlob(containerName, blobName, bais, content.length, overwrite);
        } finally {
            IoUtils.closeStream(bais, "Could not close byte array stream");
        }
    }

    /**
     * Create a block blob, using {@link InputStream} for its content.<br>Note that you are responsible to close the stream, regardless of the exit status of this method
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param contentStream - stream, representing the blob's content
     * @param contentLength - the exact length of bytes that will be read from the content stream (the underlying implementation requires that argument)
     * @param overwrite - whether to overwrite any existing blob with the same name
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void createBlockBlob( String containerName, String blobName, InputStream contentStream, long contentLength,
                                 boolean overwrite ) {

        try {
            if (contentStream == null) {
                throw new IllegalArgumentException("Content stream must not be null");
            }

            log.info("Creating " + ( (overwrite)
                                                 ? "or overwriting existing"
                                                 : "")
                     + " block blob '" + blobName + "' in container '" + containerName + "' with size '"
                     + contentLength
                     + "' ...");

            this.serviceClient.getBlobContainerClient(containerName)
                              .getBlobClient(blobName)
                              .getBlockBlobClient()
                              .upload(contentStream, contentLength, overwrite);

            log.info("Successfully created block blob '" + blobName + "' in container '" + containerName
                     + "' with size " + contentLength + ".");

        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not create block blob '" + blobName + "' in container '"
                                              + containerName + "'", e);
        }
    }

    /**
     * Create a blob with specified type
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content of the blob
     * @param blobType - the blob type
     * @param overwrite - whether to overwrite any existing blob with the same name
     * @throws AtsBlobStorageException - if exception occurred
     * *//*
                                                                                                                                   @PublicAtsApi
                                                                                                                                   public void createBlob( String containerName, String blobName, byte[] content, BlobType blobType,
                                                                                                                                                      boolean overwrite ) {
                                                                                                                                   
                                                                                                                                   if (content == null) {
                                                                                                                                      throw new IllegalArgumentException("Content must not be null");
                                                                                                                                   }
                                                                                                                                   
                                                                                                                                   switch (blobType) {
                                                                                                                                      case APPEND_BLOB:
                                                                                                                                          this.createAppendBlob(containerName, blobName, overwrite);
                                                                                                                                          this.appendToBlob(containerName, blobName, content);
                                                                                                                                          break;
                                                                                                                                      case BLOCK_BLOB:
                                                                                                                                          this.createBlockBlob(containerName, blobName, content, overwrite);
                                                                                                                                          break;
                                                                                                                                      case PAGE_BLOB:
                                                                                                                                          long size = calculatePageBlobs(content.length);
                                                                                                                                          this.createPageBlob(containerName, blobName, size, overwrite);
                                                                                                                                          break;
                                                                                                                                      default:
                                                                                                                                          throw new IllegalArgumentException("Blob type '" + blobType.name() + "' is not supported");
                                                                                                                                   }
                                                                                                                                   }*/

    /**
     * Append to an Append blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content to append
     * @throws AtsBlobStorageException - if exception occurred
     * */
    @PublicAtsApi
    public void appendToBlob( String containerName, String blobName, byte[] content ) {

        ByteArrayInputStream bais = null;
        try {
            if (content == null) {
                throw new IllegalArgumentException("Content must not be null");
            }
            log.info("Appending " + content.length + " bytes to append blob '" + blobName + "' from container '"
                     + containerName + "' ...");

            bais = new ByteArrayInputStream(content);
            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getAppendBlobClient()
                         .appendBlock(bais, content.length);

            log.info("Successfully appended " + content.length + " bytes to append blob '" + blobName
                     + "' from container '" + containerName + "'.");
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not append " + content.length + " bytes to blob '" + blobName
                                              + " in container '" + containerName + "'", e);
        } finally {
            IoUtils.closeStream(bais, "Could not close byte array stream");
        }
    }

    /**
     * Calculate ceil - number of pages/blocks to hold length number of bytes
     * @param length total bytes needed
     * @return number of pages/blocks needed, each having {@link PageBlobAsyncClient#PAGE_BYTES} bytes
     */
    private long calculatePageBlobSize( long length ) {

        /*int pageLength = PageBlobAsyncClient.PAGE_BYTES;
        int i = 0;
        while (pageLength < length) {
            pageLength = i++ * PageBlobAsyncClient.PAGE_BYTES;
        }
        return pageLength;
        */
        long tmpResult = (length + PageBlobAsyncClient.PAGE_BYTES - 1) / PageBlobAsyncClient.PAGE_BYTES;
        return tmpResult * PageBlobAsyncClient.PAGE_BYTES;
    }

    private PagedIterable<BlobContainerItem> listContainers( String containerNamePrefix,
                                                             long retrieveTimeoutSeconds,
                                                             boolean retrieveMetadata ) {

        try {
            StringBuilder message = new StringBuilder();
            message.append("Listing");

            BlobContainerListDetails bcld = new BlobContainerListDetails();
            ListBlobContainersOptions lbco = new ListBlobContainersOptions();
            message.append(" containers");
            if (containerNamePrefix != null && !containerNamePrefix.isEmpty()) {
                message.append(" with prefix '" + containerNamePrefix + "'");
                lbco.setPrefix(containerNamePrefix);
            }
            if (log.isInfoEnabled()) {
                message.append(" ...");
                log.info(message.toString());
            }
            bcld.setRetrieveMetadata(retrieveMetadata);
            lbco.setDetails(bcld);
            if (retrieveTimeoutSeconds <= 0) {
                retrieveTimeoutSeconds = Integer.MAX_VALUE / 2; // just a little less than too much
            }
            PagedIterable<BlobContainerItem> blobContainers = serviceClient.listBlobContainers(lbco,
                                                                                               Duration.ofSeconds(
                                                                                                                  retrieveTimeoutSeconds));

            log.info("Successfully listed " + blobContainers.stream().count() + " containers.");

            return blobContainers;
        } catch (Exception e) {
            throw new AtsBlobStorageException("Could not list containers "
                                              + (!StringUtils.isNullOrEmpty(containerNamePrefix)
                                                                                                 ? "with prefix '"
                                                                                                   + containerNamePrefix
                                                                                                   + "'"
                                                                                                 : "")
                                              + " in " + retrieveTimeoutSeconds + " seconds",
                                              e);
        }

    }

}
