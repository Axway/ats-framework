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
package com.axway.ats.action.s3;

import java.io.Serializable;
import java.util.Date;

import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.axway.ats.common.PublicAtsApi;

/**
 * Bean for basic properties of object like name, size and MD5
 *
 */
@PublicAtsApi
public class S3ObjectInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private S3ObjectSummary   s3Summary        = null;

    S3ObjectInfo( S3ObjectSummary s3Info ) {
        s3Summary = s3Info;
    }

    /**
     * Empty bean
     */
    public S3ObjectInfo() {
        s3Summary = new S3ObjectSummary();
    }

    /**
     * Get object name / key ID
     * @return name of the object
     */
    @PublicAtsApi
    public String getName() {

        return s3Summary.getKey();
    }

    public void setName( String name ) {

        s3Summary.setKey(name);
    }

    /**
     * Get bucket name
     */
    @PublicAtsApi
    public String getBucketName() {

        return s3Summary.getBucketName();
    }

    public void setBucketName( String bucketName ) {

        s3Summary.setBucketName(bucketName);
    }

    /**
     * Get HEX encoded MD5 checksum.
     * Alias to {@link S3ObjectSummary#getETag()}
     * @return hex encoded MD5 checksum
     */
    @PublicAtsApi
    public String getMd5() {

        return s3Summary.getETag();
    }

    public void setMd5( String md5 ) {

        s3Summary.setETag(md5);
    }

    @PublicAtsApi
    public long getSize() {

        return s3Summary.getSize();
    }

    public void setSize( long size ) {

        s3Summary.setSize(size);
    }

    @PublicAtsApi
    public Date getLastModified() {

        return s3Summary.getLastModified();
    }

    public void setLastModified( Date lastModified ) {

        s3Summary.setLastModified(lastModified);
    }

    /**
     * Get the owner's display name
     */
    public String getOwnerName() {
        Owner owner = s3Summary.getOwner();
        return owner != null ? owner.getDisplayName() : null;
    }

    /**
     * Get the owner's Id.
     */
    public String getOwnerId() {
        Owner owner = s3Summary.getOwner();
        return owner != null ? owner.getId() : null;
    }

    @Override
    public String toString() {

        return "S3ObjectInfo [name=" + getName() + ", bucket name=" + getBucketName() + ", MD5="
               + getMd5() + ", size=" + getSize() + ", last modified=" + getLastModified()
               + ", ownerName()=" + getOwnerName() + ", owner ID=" + getOwnerId() + "]";
    }


}
