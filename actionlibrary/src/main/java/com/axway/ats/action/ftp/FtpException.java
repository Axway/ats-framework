/*
 * Copyright 2017-2023 Axway Software
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
package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;

/**
 * Wrapper exception class returned from FTPClient methods.
 * This exception wraps exceptions returned from 3rd party APIs. This allows the FTPClient
 * API user to use the API without explicitly importing exceptions thrown by 3rd party APIs.
 */
@PublicAtsApi
public class FtpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @PublicAtsApi
    public FtpException(String message, Throwable cause) {
        super(message, cause);
    }

    @PublicAtsApi
    public FtpException(Throwable cause) {
        super(cause);
    }

    @PublicAtsApi
    public FtpException(String message) {
        super(message);
    }
}
