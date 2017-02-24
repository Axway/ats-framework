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
package com.axway.ats.common.agent.templateactions;

public class TemplateActionsXmlDefinitions {

    public static final String   TOKEN_HTTP_ACTIONS                = "HTTP_ACTIONS";

    public static final String   TOKEN_HTTP_ACTION                 = "HTTP_ACTION";

    public static final String   TOKEN_HTTP_REQUEST                = "HTTP_REQUEST";
    public static final String   TOKEN_HTTP_REQUEST_URL            = "HTTP_REQUEST_URL";
    public static final String   TOKEN_HTTP_REQUEST_METHOD         = "method";

    public static final String   TOKEN_HTTP_RESPONSE               = "HTTP_RESPONSE";
    public static final String   TOKEN_HTTP_RESPONSE_RESULT        = "HTTP_RESPONSE_RESULT";

    public static final String   TOKEN_HTTP_HEADER                 = "HTTP_HEADER";

    public static final String   TOKEN_HTTP_RESOURCE_FILE          = "HTTP_RESOURCE_FILE";

    public static final String   TOKEN_COLLECTION_STRING           = "COLLECTION_STRING";

    public static final String   TOKEN_HTTP_RESPONSE_FILE_EXPECTED = "HTTP_FILE_";

    public static final String   TOKEN_HEADER_NAME_ATTRIBUTE       = "name";
    public static final String   TOKEN_HEADER_VALUE_ATTRIBUTE      = "value";

    public static final String[] NON_SIGNIFICANT_HEADERS           = new String[]{ "Date",
            "Expires",
            "Keep-Alive",
            "Cache-Control",
            "Last-Modified",
            "Connection",
            "Accept-Ranges",
            "Pragma",
            "ETag" /* Used for detecting modified resources */    };
}
