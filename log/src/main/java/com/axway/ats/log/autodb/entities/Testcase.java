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
package com.axway.ats.log.autodb.entities;

public class Testcase extends DbEntity {

    private static final long serialVersionUID = 1L;

    public String             testcaseId;
    public String             scenarioId;
    public String             suiteId;
    
    public String             name;
    private String            alias;

    public int                result;
    public String             state;

    public String             userNote;

    //path
    public String             runName;
    public String             suiteName;
    public String             scenarioName;

    public String getAlias() {

        if( alias == null ) {
            alias = name;
        }
        return alias;
    }

    public void setAlias(
                          String alias ) {

        this.alias = alias;
    }

    @Override
    public int hashCode() {

        return testcaseId.hashCode();
    }

    @Override
    public boolean equals(
                           Object obj ) {

        if( obj == null || ! ( obj instanceof Testcase ) ) {
            return false;
        }
        Testcase testcase = ( Testcase ) obj;
        return this.testcaseId.equals( testcase.testcaseId );
    }

    @Override
    public String toString() {

        return "Testcase: id=" + testcaseId + ", testcaseName=" + name;
    }

    public void setPath(
                         String path ) {

        if( path != null ) {
            String[] parts = path.split( "[\\/]+" );
            for( int i = 0; i < parts.length; i++ ) {
                if( i == 0 ) {
                    runName = parts[0];
                } else if( i == 1 ) {
                    suiteName = parts[1];
                } else if( i == 2 ) {
                    scenarioName = parts[2];
                }
            }
        }
    }

}
