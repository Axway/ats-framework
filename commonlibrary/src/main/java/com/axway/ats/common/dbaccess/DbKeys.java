/*
 * Copyright 2017-2019 Axway Software
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

package com.axway.ats.common.dbaccess;

import com.axway.ats.common.PublicAtsApi;

/**
 * <p>Keys for additional DB connection properties</p>
 * <p>For additional Oracle properties, see {@link OracleKeys} class
 * */
@PublicAtsApi
public class DbKeys {

    /**
     * The key for the port custom property
     */
    @PublicAtsApi
    public static final String PORT_KEY                    = "PORT";

    /**
     * Use this property with value 'true' to use the admin credentials
     */
    @PublicAtsApi
    public static final String USE_ADMIN_CREDENTIALS       = "USE_ADMIN_CREDENTIALS";

    /**
     * Use this property with value 'true' to use SSL connection
     */
    public static final String USE_SECURE_SOCKET           = "USE_SECURE_SOCKET";

    @PublicAtsApi
    public static final String KEY_STORE_FULL_PATH         = "javax.net.ssl.trustStore";

    @PublicAtsApi
    public static final String KEY_STORE_TYPE              = "javax.net.ssl.trustStoreType";

    @PublicAtsApi
    public static final String KEY_STORE_PASSWORD          = "javax.net.ssl.trustStorePassword";

    /**
     * Use this property to specify which driver to be used for DB IO.<br>
     * Currently it is only applicable for SQL Server connections and the available values are {@link DbKeys#SQL_SERVER_DRIVER_JTDS} and {@link DbKeys#SQL_SERVER_DRIVER_MICROSOFT}
     * */
    @PublicAtsApi
    public static final String DRIVER                      = "DRIVER";

    /**
     * Used when specifying/configuring the underlying SQL server driver to be JTDS (default one)
     * */
    @PublicAtsApi
    public static final String SQL_SERVER_DRIVER_JTDS      = "JTDS";

    /**
     * Used when specifying/configuring the underlying sql server driver to be Microsoft JDBC
     * */
    @PublicAtsApi
    public static final String SQL_SERVER_DRIVER_MICROSOFT = "MSSQL";

}
