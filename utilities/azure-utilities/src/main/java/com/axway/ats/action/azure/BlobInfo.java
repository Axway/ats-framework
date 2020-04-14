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

import java.util.Date;
import java.util.Map;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;
import com.azure.storage.blob.models.BlobProperties;

@PublicAtsApi
public class BlobInfo {

    /**
     * Wrapper around the Azure Blob Type {@link com.azure.storage.blob.models.BlobType}
     * */
    public enum BlobType {
        APPEND_BLOB, BLOCK_BLOB, PAGE_BLOB;

    }

    /**
     * Wrapper around the Azure Access Tier {@link com.azure.storage.blob.models.AccessTier}
     * */
    public enum AccessTier {
        P4, P6, P10, P15, P20, P30, P40, P50, P60, P70, P80, HOT, COOL, ARCHIVE
    };

    private String              containerName;
    private String              blobName;
    private String              md5;
    private long                size;
    private String              eTag;
    private String              contentType;
    private BlobType            blobType;
    private Date                lastModified;
    private Date                creationTime;
    private Map<String, String> metadata;
    private AccessTier          accessTier;

    BlobInfo( BlobProperties blobProperties ) {

        this(null, null, blobProperties);
    }

    @PublicAtsApi
    public BlobInfo() {

    }

    BlobInfo( String containerName, String blobName, BlobProperties properties ) {

        this(containerName, blobName, properties.getContentMd5(),
             properties.getBlobSize(), properties.getETag(), properties.getContentType(),
             toAtsBlobType(properties.getBlobType()),
             Date.from(properties.getLastModified().toInstant()),
             Date.from(properties.getCreationTime().toInstant()), properties.getMetadata(),
             toAtsAccessTier(properties.getAccessTier()));
    }

    @PublicAtsApi
    public BlobInfo( String containerName, String blobName, byte[] md5, long size, String eTag, String contentType,
                     BlobType blobType, Date lastModified, Date creationTime, Map<String, String> metadata,
                     AccessTier accessTier ) {

        this.containerName = containerName;
        this.blobName = blobName;
        if (md5 != null) {
            this.md5 = java.util.Base64.getEncoder().encodeToString(md5);
        }
        this.size = size;
        this.eTag = eTag;
        this.contentType = contentType;
        this.blobType = blobType;
        this.lastModified = lastModified;
        this.creationTime = creationTime;
        this.metadata = metadata;
        this.accessTier = accessTier;
    }

    public static BlobType toAtsBlobType( com.azure.storage.blob.models.BlobType azureBlobType ) {

        if (azureBlobType == null || StringUtils.isNullOrEmpty(azureBlobType.name())) {
            return null;
        }

        return BlobType.valueOf(azureBlobType.name());
    }

    public static AccessTier toAtsAccessTier( com.azure.storage.blob.models.AccessTier azureAccessTier ) {

        if (azureAccessTier == null || StringUtils.isNullOrEmpty(azureAccessTier.toString())) {
            return null;
        }

        return AccessTier.valueOf(azureAccessTier.toString().toUpperCase());
    }

    public String getContainerName() {

        return containerName;
    }

    public void setContainerName( String containerName ) {

        this.containerName = containerName;
    }

    public String getBlobName() {

        return blobName;
    }

    public void setBlobName( String blobName ) {

        this.blobName = blobName;
    }

    /**
     * @return Base64 encoded MD5
     * */
    public String getMd5() {

        return md5;
    }

    /** Get blob content's checksum<br> 
     * Note that Append blobs <strong>DO NOT</strong> have MD5 checksum, so this method will return null
     * @param md5 - Base64 encoded MD5. Example java.util.Base64.getEncoder().encodeToString(byte[] md5);
     * */
    public void setMd5( String md5 ) {

        this.md5 = md5;
    }

    public long getSize() {

        return size;
    }

    public void setSize( long size ) {

        this.size = size;
    }

    public String getETag() {

        return eTag;
    }

    public void setETag( String eTag ) {

        this.eTag = eTag;
    }

    public String getContentType() {

        return contentType;
    }

    public void setContentType( String contentType ) {

        this.contentType = contentType;
    }

    public BlobType getBlobType() {

        return blobType;
    }

    public void setBlobType( BlobType blobType ) {

        this.blobType = blobType;
    }

    public Date getLastModified() {

        return lastModified;
    }

    public void setLastModified( Date lastModified ) {

        this.lastModified = lastModified;
    }

    public Date getCreationTime() {

        return creationTime;
    }

    public void setCreationTime( Date creationTime ) {

        this.creationTime = creationTime;
    }

    public Map<String, String> getMetadata() {

        return metadata;
    }

    public void setMetadata( Map<String, String> metadata ) {

        this.metadata = metadata;
    }

    public AccessTier getAccessTier() {

        return accessTier;
    }

    public void setAccessTier( AccessTier accessTier ) {

        this.accessTier = accessTier;
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(this.getClass().getSimpleName())
          .append(" [")
          .append("name=" + this.blobName + ", ")
          .append("container name=" + this.containerName + ", ")
          .append("type=" + this.blobType + ", ")
          .append("access tier=" + this.accessTier + ", ")
          .append("eTag=" + this.eTag + ", ")
          .append("content type=" + this.contentType + ", ")
          .append("MD5=" + this.md5 + ", ")
          .append("size=" + this.size + ", ")
          .append("creation time=" + this.creationTime + ", ")
          .append("last mod time=" + this.lastModified + "")
          .append("]");

        return sb.toString();

    }

}
