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
    @PublicAtsApi FTP,
    /** File Transfer Protocol over SSL type*/
    @PublicAtsApi FTPS,

    /** Hypertext Transfer Protocol type*/
    @PublicAtsApi HTTP,
    /** Custom Hypertext Transfer Protocol type*/
    @PublicAtsApi HTTP_CUSTOM,
    /** Hypertext Transfer Protocol over SSL type*/
    @PublicAtsApi HTTPS,
    /** Custom Hypertext Transfer Protocol Over SSL type*/
    @PublicAtsApi HTTPS_CUSTOM,

    /** SSH File Transfer Protocol type*/
    @PublicAtsApi SFTP,

    /** Custom PeSIT type*/
    @PublicAtsApi PESIT_CUSTOM,

    /** Custom file transfer type*/
    @PublicAtsApi CUSTOM;

    /**
     * @return the default port for the current protocol
     */
    public final int getDefaultPort() {

        switch( this ){
            case FTP:
                return 21;
            case HTTP:
            case HTTP_CUSTOM:
                return 80;
            case PESIT_CUSTOM:
                return 17617;

            case SFTP:
                return 22;
            case FTPS:
                return 21;
            case HTTPS:
            case HTTPS_CUSTOM:
                return 443;
            case CUSTOM:
                return -1;
            default:
                throw new RuntimeException( "No default port is set for the protocol" + this );
        }
    }
}
