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

import com.axway.ats.common.PublicAtsApi;

/**
 * POJO class for keeping sincle testcase meta info as name:value pair  
 * 
 */
@PublicAtsApi
public class TestcaseMetainfo extends DbEntity {

    private static final long serialVersionUID = 1L;

    public int                metaInfoId;
    public int                testcaseId;
    public String             name;
    public String             value;

    @Override
    public String toString() {

        return "TestcaseMetainfo [name=" + name + ", value=" + value + ", meta info ID =" + metaInfoId + ", testcase ID=" + testcaseId + "]";
    }
    
}
