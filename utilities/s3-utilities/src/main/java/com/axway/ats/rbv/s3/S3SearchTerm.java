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

import com.axway.ats.rbv.storage.SearchTerm;

public class S3SearchTerm implements SearchTerm {

    private String  endpoint;
    private String  secretKey;
    private String  accessKey;
    private String  region;
    private String  bucketName;
    private String  directory;
    private String  fileName;
    private boolean recursive;

    public S3SearchTerm( String accessKey, String secretKey,  String bucketName,
                         String directory, String fileName, boolean recursive ) {

        this( null, accessKey, secretKey, null, bucketName, directory, fileName, recursive );
    }


    public S3SearchTerm( String endpoint, String accessKey, String secretKey, String region,
                         String bucketName, String directory, String fileName, boolean recursive ) {

        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.bucketName = bucketName;
        this.directory = directory;
        this.fileName = fileName;
        this.recursive = recursive;
    }

    public String getEndpoint() {

        return endpoint;
    }


    public String getAccessKey() {

        return accessKey;
    }

    public String getSecretKey() {

        return secretKey;
    }

    public String getRegion() {

        return region;
    }

    public String getBucketName() {

        return bucketName;
    }


    public String getDirectory() {

        return directory;
    }

    public String getFileName() {

        return fileName;
    }

    public boolean isRecursive() {

        return this.recursive;
    }
}
