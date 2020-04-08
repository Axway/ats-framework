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
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.ExceptionUtils;
import com.axway.ats.core.utils.StringUtils;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobContainerListDetails;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobType;
import com.azure.storage.blob.models.ListBlobContainersOptions;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.PageBlobAsyncClient;

/**
 * Class, encapsulating Azure Blob Storage operations
 * */
@PublicAtsApi
public class BlobStorageOperations {

    private static final Logger log = Logger.getLogger(BlobStorageOperations.class);

    private BlobServiceClient   serviceClient;

    @PublicAtsApi
    public BlobStorageOperations( String connectionString, String sasToken ) {

        serviceClient = new BlobServiceClientBuilder().connectionString(connectionString)
                                                      .sasToken(sasToken)
                                                      .buildClient();
    }

    /**
     * Obtain list of the container's names<br>
     * Note that this method will return up to 5000 container names. If you want more than that, use {@link BlobStorageOperations#listContainers(int, String, long)}
     * @return list of container names
     * */
    @PublicAtsApi
    public List<String> listContainers() {

        return listContainers(null, 0);
    }

    /**
     * Obtain list of the container's names
     * @param containerNamePrefix - specify container name prefix or the full container name
     * @param retrieveTimeouts - the maximum amount of time (in milliseconds) to wait for the operation to complete. If the operation did not complete in that time {@link BlobStorageException} will be thrown/raised
     * @return list of container names
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
     */
    @PublicAtsApi
    public boolean doesContainerExist( String containerName ) {

        log.info("Checking if container '" + containerName + "' exists ...");

        try {
            return serviceClient.getBlobContainerClient(containerName).exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Create new container. If the container already exists, a {@link BlobStorageException} will be thrown<br>
     * Note that this method may fail if the container was recently deleted. So you may have to wait a little before invoking this method on already deleted container
     * 
     * @param containerName - the container name
     * */
    @PublicAtsApi
    public void createContainer( String containerName ) {

        log.info("Creating container '" + containerName + "' ...");

        serviceClient.getBlobContainerClient(containerName).create();

        log.info("Container '" + containerName + "' successfully created.");
    }

    /**
     * Create new container
     * @param containerName - the new container name
     * @param timeout - the maximum amount of time (in milliseconds) to wait for container to be created.
     * @throws BlobStorageException if the container was not created for up to timeout milliseconds
     * */
    @PublicAtsApi
    public void createContainer( String containerName, long timeout ) {

        long startTime = System.currentTimeMillis();

        boolean logBeingDeletedMessage = true;

        log.info("Creating container '" + containerName + "' while waiting up to " + timeout
                 + " milliseconds for the operation to complete ...");

        BlobStorageException lastException = null;
        while (System.currentTimeMillis() - startTime <= timeout) {

            try {
                serviceClient.getBlobContainerClient(containerName).create();
                log.info("Container '" + containerName + "' successfully created.");
                return;
            } catch (BlobStorageException bse) {
                if (ExceptionUtils.containsMessage("InvalidResourceName", bse, true)) {
                    throw bse;
                } else if (ExceptionUtils.containsMessage("ContainerBeingDeleted", bse, true)
                           && logBeingDeletedMessage) {
                    logBeingDeletedMessage = false;
                    log.warn("Container '" + containerName
                             + "' is currently being deleted!. This can lead to failure of createContainer method. You should increase the timeout for creation of this container if this error is persistent");
                }
                lastException = bse;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        throw new BlobStorageException("Could not create container '" + containerName + "' in " + timeout
                                            + " milliseconds",
                                            lastException);
    }

    /**
     * Delete existing container. If the container exists, a {@link BlobStorageException} will be thrown<br>
     * Note that there is a delay between calling this method and the container being deleted, so invoking {@code BlobOperations#createContainer(String)} with the same container name may fail
     * @param containerName - the container name
     * */
    @PublicAtsApi
    public void deleteContainer( String containerName ) {

        log.info("Deleting container '" + containerName + "' ...");

        serviceClient.getBlobContainerClient(containerName).delete();

        log.info("Container '" + containerName + "' successfully deleted.");
    }

    /**
     * Delete existing container
     * @param containerName - the container name
     * @param timeout - the maximum amount of time (in milliseconds) to wait for container to be deleted.
     * @throws BlobStorageException if the container was not deleted for up to timeout milliseconds
     * */
    @PublicAtsApi
    public void deleteContainer( String containerName, long timeout ) {

        long startTime = System.currentTimeMillis();

        BlobStorageException lastException = null;
        while (System.currentTimeMillis() - startTime <= timeout) {

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

        throw new BlobStorageException("Could not delete container '" + containerName + "' in " + timeout
                                            + " milliseconds",
                                            lastException);
    }

    /**
     * Delete all blobs and directories/folders for container. Essentially leaving the container empty
     * @param containerName - the containerName 
     * **/
    @PublicAtsApi
    public void purgeContainer( String containerName ) {

        log.info("Purging container '" + containerName + "' ...");

        List<BlobInfo> blobs = listBlobs(containerName);
        if (blobs == null || blobs.isEmpty()) {
            log.info("Cointaner '" + containerName + "' has no blobs inside. Nothing to purge.");
        } else {
            log.info("Cointaner '" + containerName + "' has " + blobs.size()
                     + " blobs inside. Begin purging of all blobs ...");
        }
        for (BlobInfo blob : blobs) {
            this.deleteBlob(containerName, blob.getBlobName());
        }

        if (!isContainerEmpty(containerName)) {
            blobs = listBlobs(containerName);
            //TODO: print blob names ?!?!
            throw new BlobStorageException("Container '" + containerName
                                                + "' could not be properly purged. Blobs left: "
                                                + blobs.size());
        }

        log.info("Container '" + containerName + "' successfully purged.");

    }

    /**
     * Check whether container has more than one blob
     * @param containerName - the container name
     * @return true or false
     * */
    @PublicAtsApi
    public boolean isContainerEmpty( String containerName ) {

        log.info("Checking if container '" + containerName + "' is empty ...");

        return this.serviceClient.getBlobContainerClient(containerName).listBlobs().stream().count() <= 0;
    }

    /**
     * List all blobs from container
     * @param containerName - the container name
     * @return list of {@link BlobInfo}
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName ) {

        return listBlobs(containerName, null, null, 0);
    }

    /**
     * List blobs, with specified name prefix, from container
     * @param containerName - the container name
     * @param prefix - blob names prefix. Example: prefix = "blob-", will return blobs with name, starting with blob-
     * @return list of {@link BlobInfo}
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix ) {

        return listBlobs(containerName, prefix, null, 0);

    }

    /**
     * List blobs from directory inside container
     * @param containerName - the container name
     * @param prefix - blob names prefix. Example: prefix = "blob-", will return blobs with name, starting with blob-
     * @param directory - the directory name. Could by a nested directory as well, like foo/bar/baz
     * @return list of {@link BlobInfo}
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix, String directory ) {

        return listBlobs(containerName, prefix, directory, 0);
    }

    /**
     * List blobs from container 
     * @param containerName - the container name
     * @param prefix - prefix for the blobs names or null for all blobs
     * @param directory - the directory which will be listed, or null to search the whole container
     * @param retrieveTimeout - the maximum amount of time (in seconds) to wait for the operation to complete. If the operation did not complete in that time {@link BlobStorageException} will be thrown/raised. Pass 0 (zero) to use the default value
     * @return list {@link BlobInfo}
     * */
    @PublicAtsApi
    public List<BlobInfo> listBlobs( String containerName, String prefix, String directory,
                                          long retrieveTimeout ) {

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
            sb.append(" inside container '" + containerName + "' ...");
            log.info(sb.toString());
            blobs = serviceClient.getBlobContainerClient(containerName)
                                 .listBlobsByHierarchy("/", lbops, Duration.ofSeconds(retrieveTimeout));
        } else {
            sb.append(" inside container '" + containerName + "' ...");
            log.info(sb.toString());
            blobs = serviceClient.getBlobContainerClient(containerName)
                                 .listBlobs(lbops, Duration.ofSeconds(retrieveTimeout));
        }

        if (blobs != null) {
            blobs.stream().forEach(new Consumer<BlobItem>() {
                public void accept( BlobItem blobItem ) {

                    BlobInfo info = new BlobInfo();

                    BlobItemProperties properties = blobItem.getProperties();

                    info.setAccessTier(properties.getAccessTier());
                    info.setBlobName(blobItem.getName());
                    info.setBlobType(properties.getBlobType());
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
    }

    /**
     * Check whether a blob in a container exists
     * @param containerName - the container name
     * @param blobName - the blob name
     * @return true if exists or false if not
     */
    @PublicAtsApi
    public boolean doesBlobExist( String containerName, String blobName ) {

        log.info("Checking if blob '" + blobName + "' from container '" + containerName + "' exists ...");

        return serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).exists();
    }

    /**
     * Get blob information/properties
     * @param containerName - the container name
     * @param blobName - the blob name
     * */
    @PublicAtsApi
    public BlobInfo getBlobInfo( String containerName, String blobName ) {

        log.info("Getting info for blob '" + blobName + "' from container '" + containerName + "' ...");

        try {
            BlobProperties props = serviceClient.getBlobContainerClient(containerName)
                                                .getBlobClient(blobName)
                                                .getProperties();
            return new BlobInfo(containerName, blobName, props);
        } catch (Exception e) {
            if (ExceptionUtils.containsMessage("Status code 404, (empty body)", e, true)) {
                throw new BlobStorageException("Blob '" + blobName + "' does not exist in container '"
                                                    + containerName
                                                    + "'", e);
            } else {
                throw e;
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
     * */
    @PublicAtsApi
    public void setBlobMetadata( String containerName, String blobName, Map<String, String> newMetadata,
                                 boolean overwriteExistingKeys, boolean includeOldMetadata, String valueDelimiter ) {

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

        log.info("Successfully set metadata for blob '" + blobName + "' from container '" + containerName + "' ...");
    }

    /**
     * Delete blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * */
    @PublicAtsApi
    public void deleteBlob( String containerName, String blobName ) {

        log.info("Deleting blob '" + blobName + "' from container '" + containerName + "' ...");

        serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).delete();

        log.info("Blob '" + blobName + "' successfully deleted from container '" + containerName + "' ...");

    }

    /*public void undeleteBlob( String containerName, String blobName ) {
    
        log.info("Undeleting blob '" + blobName + "' from container '" + containerName + "' ...");
    
        serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).undelete();
    
        log.info("Blob '" + blobName + "' successfully undeleted from container '" + containerName + "' ...");
    }*/

    /**
     * Rename existing blob<br>
     * Note that the creation and last modified time will be updated and only block blobs can be renamed
     * @param containerName - the container name
     * @param oldBlobName - the old blob name
     * @param newBlobName - the new blob name
     * *//*
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
     * Upload local file to a blob
     * @param containerName - the container name, where that blob will be uploaded
     * @param blobName - the blob name, which will be created by the upload. If null, the name of the local file will be used
     * @param localFilepath - path to the file to be uploaded
     * @param overwrite - whether to overwrite an existing blob with the same name or not
     * */
    @PublicAtsApi
    public void uploadBlob( String containerName, String blobName, String localFilepath, boolean overwrite ) {

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

        log.info("Successfully uploaded '" + localFilepath + "' to container '" + containerName + "' as a blob, named '"
                 + blobName + "'.");
    }

    /**
     * Download blob to a local file
     * @param containerName - the container name, containing the blob
     * @param blobName - the blob's name to be downloaded
     * @param localFilepath - path to the file where the blob will be saved
     * @param overwrite - whether to overwrite an existing local file with the blob's content or not
     * */
    @PublicAtsApi
    public void downloadBlob( String containerName, String blobName, String localFilepath, boolean overwrite ) {

        log.info("Downloading " + ( (overwrite)
                                                ? "(overwrite enabled)"
                                                : "")
                 + "blob '" + blobName + "' from container '" + containerName + "' to file '" + localFilepath
                 + "' ...");

        serviceClient.getBlobContainerClient(containerName)
                     .getBlobClient(blobName)
                     .downloadToFile(localFilepath, overwrite);

        log.info("Successfully downloaded blob '" + blobName + "' from container '" + containerName + "' to file '"
                 + localFilepath + "'.");

    }

    /**
     * Create page blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param size - the blob size. Note that it must be multiple of {@link PageBlobAsyncClient.PAGE_BYTES} (currently 512 bytes)
     * @param overwrite - whether to overwrite any existing blob with the same name
     * */
    @PublicAtsApi
    public void createPageBlob( String containerName, String blobName, long size, boolean overwrite ) {

        log.info("Creating " + ( (overwrite)
                                             ? "or overwriting existing"
                                             : "")
                 + " page blob '" + blobName + "' inside container '" + containerName + "' with size '" + size
                 + "' ...");

        try {
            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getPageBlobClient()
                         .create(size, overwrite);
        } catch (Exception e) {
            throw new BlobStorageException("Could not create block blob '" + blobName + "' inside container '"
                                                + containerName + "'", e);
        }

        log.info("Successfully created page blob '" + blobName + "' inside container '" + containerName + "' with size "
                 + size + ".");
    }

    /**
     * Create append blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param overwrite - whether to overwrite any existing blob with the same name
     * */
    @PublicAtsApi
    public void createAppendBlob( String containerName, String blobName, boolean overwrite ) {

        log.info("Creating " + ( (overwrite)
                                             ? "or overwriting existing"
                                             : "")
                 + " append blob '" + blobName + "' inside container '" + containerName + "' ...");

        try {
            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getAppendBlobClient()
                         .create(overwrite);
        } catch (Exception e) {
            throw new BlobStorageException("Could not create block blob '" + blobName + "' inside container '"
                                                + containerName + "'", e);
        }

        log.info("Successfully created append blob '" + blobName + "' inside container '" + containerName + "'.");
    }

    /**
     * Create a blob with specified type
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content of the blob
     * @param blobType - the blob type
     * @param overwrite - whether to overwrite any existing blob with the same name
     * */
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
                long size = calculatePageBlobSize(content.length);
                this.createPageBlob(containerName, blobName, size, overwrite);
                break;
            default:
                throw new IllegalArgumentException("Blob type '" + blobType.name() + "' is not supported");
        }

    }

    /**
     * Create a block blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content of the blob
     * @param overwrite - whether to overwrite any existing blob with the same name
     * */
    @PublicAtsApi
    public void createBlockBlob( String containerName, String blobName, byte[] content, boolean overwrite ) {

        ByteArrayInputStream bais = null;
        try {
            if (content == null) {
                throw new IllegalArgumentException("Content must not be null");
            }

            log.info("Creating " + ( (overwrite)
                                                 ? "or overwriting existing"
                                                 : "")
                     + " block blob '" + blobName + "' inside container '" + containerName + "' with size '"
                     + content.length
                     + "' ...");

            bais = new ByteArrayInputStream(content);
            this.serviceClient.getBlobContainerClient(containerName)
                              .getBlobClient(blobName)
                              .getBlockBlobClient()
                              .upload(bais, content.length, overwrite);

            log.info("Successfully created block blob '" + blobName + "' inside container '" + containerName
                     + "' with size "
                     + content.length + ".");

        } catch (Exception e) {
            throw new BlobStorageException("Could not create block blob '" + blobName + "' inside container '"
                                                + containerName + "'", e);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {}
            }
        }

    }

    /**
     * Append to an Append blob
     * @param containerName - the container name
     * @param blobName - the blob name
     * @param content - the content to append
     * */
    @PublicAtsApi
    public void appendToBlob( String containerName, String blobName, byte[] content ) {

        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }

        ByteArrayInputStream bais = null;
        try {

            log.info("Appending " + content.length + " bytes to append blob '" + blobName + "' from container '"
                     + containerName + "' ...");

            bais = new ByteArrayInputStream(content);
            serviceClient.getBlobContainerClient(containerName)
                         .getBlobClient(blobName)
                         .getAppendBlobClient()
                         .appendBlock(bais, content.length);

            log.info("Susscessfully appended " + content.length + " bytes to append blob '" + blobName
                     + "' from container '" + containerName + "'.");
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    log.error("Could not close byte array stream", e);
                }
            }
        }
    }

    private long calculatePageBlobSize( long length ) {

        int pageLength = PageBlobAsyncClient.PAGE_BYTES;
        int i = 0;
        while (pageLength < length) {
            pageLength = i++ * PageBlobAsyncClient.PAGE_BYTES;
        }

        return pageLength;
    }

    private PagedIterable<BlobContainerItem> listContainers( String containerNamePrefix,
                                                             long retrieveTimeoutMilliSeconds,
                                                             boolean retrieveMetadata ) {

        StringBuilder message = new StringBuilder();
        message.append("Listing");

        BlobContainerListDetails bcld = new BlobContainerListDetails();
        ListBlobContainersOptions lbco = new ListBlobContainersOptions();
        message.append(" containers");
        if (containerNamePrefix != null && !containerNamePrefix.isEmpty()) {
            message.append(" with prefix '" + containerNamePrefix + "'");
            lbco.setPrefix(containerNamePrefix);
        }
        message.append(" ...");
        log.info(message.toString());
        bcld.setRetrieveMetadata(retrieveMetadata);
        lbco.setDetails(bcld);
        if (retrieveTimeoutMilliSeconds <= 0) {
            retrieveTimeoutMilliSeconds = Integer.MAX_VALUE / 2; // just a little less than too much
        }
        PagedIterable<BlobContainerItem> blobContainers = serviceClient.listBlobContainers(lbco,
                                                                                           Duration.ofMillis(retrieveTimeoutMilliSeconds));

        log.info("Successfully listed " + blobContainers.stream().count() + " containers.");

        return blobContainers;
    }

}
