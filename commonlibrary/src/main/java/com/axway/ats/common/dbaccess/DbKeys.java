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
     * <strong>Applicable to SQL Server only.</strong><br>
     * Use this property to specify which driver to be used for DB IO.<br>
     * The available values are {@link DbKeys#SQL_SERVER_DRIVER_JTDS} and {@link DbKeys#SQL_SERVER_DRIVER_MICROSOFT}
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
    
    /**
     * <strong>Applicable to MySQL only and for MySQL JDBC connector version 8.xx.xx</strong><br>
     * Most of the time ATS automatically sets the server's time zone, but if something is wrong with the returned time stamps/dates, this property/key is your friend.<br>
     * Use this property to tell the MySQL connection what is the server's time zone.<br>
     * Available formats are:
     * <ul>
     * <li>Named time zone, like Europe/Sofia</li>
     * <li> GMT offset, like GMT[+|-][h]h:mm<br>
     * <li>UTC (without offset)</li>
     * If you have the offset in UTC, you can still use it, just replace UTC with GMT, so UTC+3 becomes GMT+03:00</li>
     * </ul>
     * If not sure, set it to UTC and see if everything, regarding TimeZone/DateTime SQL types, is working as expected.
     * */
    @PublicAtsApi
    public static final String SERVER_TIMEZONE = "SERVER_TIMEZONE";

}
