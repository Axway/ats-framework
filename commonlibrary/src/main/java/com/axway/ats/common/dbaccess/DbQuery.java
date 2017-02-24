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
package com.axway.ats.common.dbaccess;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axway.ats.common.PublicAtsApi;

/**
 *  Wrapper, containing all the data needed to build up a {@link PreparedStatement}
 *  for the specific Database Provider. 
 */
@PublicAtsApi
public class DbQuery {

    private String       query     = "";
    private List<Object> arguments = new ArrayList<Object>();

    /**
     * Constructor 
     * @param q the SQL query to initialize this {@link DbQuery} with 
     */
    @PublicAtsApi
    public DbQuery( String q ) {

        this.query = q;
        this.arguments = new ArrayList<Object>();
    }

    /**
     * Constructor 
     * @param q the SQL query to initialize this {@link DbQuery} with 
     * @param args the WHERE arguments to initialize this {@link DbQuery} with
     */
    @PublicAtsApi
    public DbQuery( String q, List<Object> args ) {

        if( args == null ) {
            this.arguments = new ArrayList<Object>();
        } else {
            this.arguments = args;
        }

        this.query = q;
    }

    /**
     * Constructor 
     * @param q the SQL query to initialize this {@link DbQuery} with 
     * @param args the WHERE arguments to initialize this {@link DbQuery} with
     */
    @PublicAtsApi
    public DbQuery( String q, Object[] args ) {

        this.arguments = Arrays.asList( args );
        this.query = q;
    }

    /**
     * @return the SQL query
     */
    public String getQuery() {

        return this.query;
    }

    /**
     * Sets the contents of the query statement
     * @param query the new query
     */
    public void setQuery( String query ) {

        this.query = query;
    }

    /**
     * @return the {@link List} of arguments for the WHERE
     * clauses (if any)
     */
    public List<Object> getArguments() {

        return this.arguments;
    }

    /**
     * Sets the {@link List} of arguments for the where clauses
     * @param arguments the new list of arguments
     */
    public void setArguments( List<Object> arguments ) {

        this.arguments = arguments;
    }
}
