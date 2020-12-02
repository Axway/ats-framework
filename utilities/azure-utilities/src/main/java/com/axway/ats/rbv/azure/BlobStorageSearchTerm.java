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

import com.axway.ats.rbv.storage.SearchTerm;

public class BlobStorageSearchTerm implements SearchTerm {

    String  connectionString;
    String  sasToken;
    String  containerName;
    String  blobName;
    boolean recursive;
    boolean containerOperationsOnly;

    public BlobStorageSearchTerm( String connectionString, String sasToken, String containerName,
                                       String blobName, boolean recursive, boolean containerOperationsOnly ) {

        this.connectionString = connectionString;
        this.sasToken = sasToken;
        this.containerName = containerName;
        this.blobName = blobName;
        this.recursive = recursive;
        this.containerOperationsOnly = containerOperationsOnly;
    }

    public String getConnectionString() {

        return connectionString;
    }

    public String getSasToken() {

        return sasToken;
    }

    public String getContainerName() {

        return containerName;
    }

    public String getBlobName() {

        return blobName;
    }

    public boolean isRecursive() {

        return recursive;
    }

    public boolean isContainerOperationsOnly() {

        return containerOperationsOnly;
    }

}
