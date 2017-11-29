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
package com.axway.ats.action.dbaccess.model;

import com.axway.ats.common.PublicAtsApi;

/**
 * A simple representation of a DB cell.
 */
@PublicAtsApi
public class DatabaseCell {

    private String name;
    private String value;

    /**
     * A basic constructor
     * @param name cell name
     * @param value cell value
     */
    public DatabaseCell( String name,
                         String value ) {

        this.name = name;
        this.value = value;
    }

    /**
     * @return the cell name
     */
    @PublicAtsApi
    public String getName() {

        return name;
    }

    /**
     * @return the cell value
     */
    @PublicAtsApi
    public String getValue() {

        return value;
    }

    @Override
    public String toString() {

        return name + "=" + value;
    }
}
