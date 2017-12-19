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

import com.axway.ats.common.PublicAtsApi;

/**
 * Exception while executing some file system operations
 */
@PublicAtsApi
public class S3OperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param message
     */
    public S3OperationException( String message ) {

        super(message);
    }

    /**
     * Constructor
     *
     * @param message
     * @param e
     */
    public S3OperationException( String message, Exception e ) {

        super(message, e);
    }

    /**
     * Constructor
     *
     * @param e
     */
    public S3OperationException( Exception e ) {

        super(e);
    }
}
