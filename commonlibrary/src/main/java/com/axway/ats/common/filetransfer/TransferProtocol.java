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
package com.axway.ats.common.filetransfer;

import com.axway.ats.common.PublicAtsApi;

/**
 * Contains all the transfer protocol types
 */
@PublicAtsApi
public enum TransferProtocol {

    /** File Transfer Protocol type*/
    @PublicAtsApi
    FTP,
    /** File Transfer Protocol over SSL type*/
    @PublicAtsApi
    FTPS,

    /** Hypertext Transfer Protocol type*/
    @PublicAtsApi
    HTTP,
    /** Hypertext Transfer Protocol over SSL type*/
    @PublicAtsApi
    HTTPS,

    /** SSH File Transfer Protocol type*/
    @PublicAtsApi
    SFTP;

    /**
     * @return the default port for the current protocol
     */
    public final int getDefaultPort() {

        switch (this) {
            case FTP:
                return 21;
            case HTTP:
                return 80;
            case SFTP:
                return 22;
            case FTPS:
                return 21;
            case HTTPS:
                return 443;
            default:
                throw new RuntimeException("No default port is set for the protocol" + this);
        }
    }
}
