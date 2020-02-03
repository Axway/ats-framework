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
package com.axway.ats.core.dbaccess.postgresql;

import com.axway.ats.core.dbaccess.ColumnDescription;

public class PostgreSqlColumnDescription extends ColumnDescription {

    public PostgreSqlColumnDescription(
                                    String name, String type ) {
        super(name.toLowerCase(), type.toLowerCase());
    }

    // for information about all data types in PostgreSQL check

    @Override
    public boolean isTypeBinary() {

        String normType = type.toLowerCase();
        // TODO: check SQL-92 types BINARY and VARBINARY
        return "bytea".equals(normType);
    }

    @Override
    public boolean isTypeNumeric() {

        String normType = type.toLowerCase();
        return normType.contains("smallint") || normType.contains("integer")
               || normType.contains("bigint") || normType.contains("decimal")
               || normType.contains("numeric")
               || normType.contains("serial") /* smallserial, serial, bigserial */
               || normType.contains("real") || normType.contains("double");
    }
}