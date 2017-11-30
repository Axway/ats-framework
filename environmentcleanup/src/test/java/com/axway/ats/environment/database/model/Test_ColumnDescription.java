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
package com.axway.ats.environment.database.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.core.dbaccess.ColumnDescription;
import com.axway.ats.environment.BaseTest;

public class Test_ColumnDescription extends BaseTest {

    @Test
    public void accessors() {

        ColumnDescription columnDescription = new ColumnDescription("name", "type");

        assertEquals("name", columnDescription.getName());
        assertEquals("type", columnDescription.getType());
    }

    @Test
    public void isTypeNumericPositive() {

        List<String> numericTypes = new ArrayList<String>();
        numericTypes.add("int");
        numericTypes.add("decimal");
        numericTypes.add("numeric");
        numericTypes.add("real");
        numericTypes.add("double");
        numericTypes.add("bit");

        for (String numericType : numericTypes) {
            ColumnDescription columnDescription = new ColumnDescription("name", numericType);
            assertTrue(columnDescription.isTypeNumeric());
        }
    }

    @Test
    public void isTypeNumericNegative() {

        ColumnDescription columnDescription = new ColumnDescription("name", "type");

        assertFalse(columnDescription.isTypeNumeric());
    }

    @Test
    public void isTypeBinaryPositive() {

        List<String> binaryTypes = new ArrayList<String>();
        binaryTypes.add("blob");
        binaryTypes.add("longblob");
        binaryTypes.add("binary");
        binaryTypes.add("binary(5)");
        binaryTypes.add("varbinary");
        binaryTypes.add("varbinary(5)");

        for (String numericType : binaryTypes) {
            ColumnDescription columnDescription = new ColumnDescription("name", numericType);
            assertTrue(columnDescription.isTypeBinary());
        }
    }

    @Test
    public void isTypeBinaryNegative() {

        ColumnDescription columnDescription = new ColumnDescription("name", "char binary");
        assertFalse(columnDescription.isTypeBinary());
        columnDescription = new ColumnDescription("name", "varchar binary(5)");
        assertFalse(columnDescription.isTypeBinary());
    }

    @Test
    public void isTypeBitPositive() {

        ColumnDescription columnDescription = new ColumnDescription("name", "bit");

        assertTrue(columnDescription.isTypeBit());
    }

    @Test
    public void isTypeBit() {

        ColumnDescription columnDescription = new ColumnDescription("name", "type");

        assertFalse(columnDescription.isTypeBit());
    }
}
