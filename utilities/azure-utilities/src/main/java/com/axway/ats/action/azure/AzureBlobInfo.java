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
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobType;

@PublicAtsApi
public class AzureBlobInfo {

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

    AzureBlobInfo( BlobProperties blobProperties ) {

        this(null, null, blobProperties);
    }

    @PublicAtsApi
    public AzureBlobInfo() {

    }

    AzureBlobInfo( String containerName, String blobName, BlobProperties properties ) {

        this(containerName, blobName, properties.getContentMd5(),
             properties.getBlobSize(), properties.getETag(), properties.getContentType(), properties.getBlobType(),
             Date.from(properties.getLastModified().toInstant()),
             Date.from(properties.getCreationTime().toInstant()), properties.getMetadata(), properties.getAccessTier());
    }

    @PublicAtsApi
    public AzureBlobInfo( String containerName, String blobName, byte[] md5, long size, String eTag, String contentType,
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

        final String delimiter = "|_|";

        StringBuilder sb = new StringBuilder();

        sb.append(containerName + delimiter)
          .append(blobName + delimiter)
          .append(md5 + delimiter)
          .append(creationTime.getTime() + delimiter);

        return sb.toString();

    }

}
