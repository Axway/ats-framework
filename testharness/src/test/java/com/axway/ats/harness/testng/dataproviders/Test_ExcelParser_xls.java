/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.harness.testng.dataproviders;

import java.lang.reflect.Method;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.harness.BaseTest;
import com.axway.ats.harness.testng.TestOptions;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

public class Test_ExcelParser_xls extends BaseTest {

    private static final String EXCEL_FILE_PATH = "src/test/resources/ExcelParser.xls";

    public static final String  TEST_CASE_START = "TEST_CASE_START";

    public static final String  TEST_CASE_END   = "TEST_CASE_END";

    public static final String  STRING_TOKEN    = "STRING";

    public static final String  MULTIPLY        = "MULTIPLY";

    private ExcelParser         parser;

    /**
     * Test class used through reflection
     */
    @SuppressWarnings( "unused")
    private static class Methods {

        public void method(
                            Object parameter1 ) {

        }

        //these are the methods that we are going to use for test purposes
        public void stringParseMethod(

                                       String parameter1 ) {

        }

        public void booleanParseMethod(

                                        boolean parameter1 ) {

        }

        public void doubleParseMethod(
                                       double parameter1 ) {

        }

        public void intParseMethod(
                                    int parameter1 ) {

        }

        public void longParseMethod(
                                     long parameter1 ) {

        }

        public void shortParseMethod(
                                      short parameter1 ) {

        }

        public void byteParseMethod(
                                     byte parameter1 ) {

        }

        public void charParseMethod(
                                     char parameter1 ) {

        }

        public void floatParseMethod(
                                      float parameter1 ) {

        }

        public void numberParseMethod(
                                       Number parameter1 ) {

        }

        public void dateParseMethod(
                                     Date parameter1 ) {

        }

        public void nullParseMethod(
                                     Object parameter1 ) {

        }

        public void nullParseMethodNegative(
                                             int parameter1 ) {

        }

        public void cartesianMethod(
                                     int parameter1,
                                     float parameter2,
                                     String parameter3 ) {

        }

        public void multiParamMethod(
                                      int parameter1,
                                      float parameter2,
                                      String parameter3,
                                      long parameter4 ) {

        }

        public void setSheetName(
                                  String parameter1,
                                  int parameter2 ) {

        }

        public void paramTypeDiffers(
                                      int parameter1,
                                      String parameter2 ) {

        }

    }

    private Method findMethodByNameOnly(
                                         String methodName ) throws NoSuchMethodException {

        for (Method classMethod : Methods.class.getDeclaredMethods()) {
            if (classMethod.getName().equals(methodName)) {
                return classMethod;
            }
        }

        throw new NoSuchMethodException(methodName);
    }

    //BOOLEAN
    @Test
    @TestOptions( dataSheet = "TestBoolean")
    public void booleanCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestBoolean");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("booleanParseMethod"));

        Assert.assertEquals(false, data[0][0]);
        Assert.assertEquals(false, data[1][0]);
        Assert.assertEquals(false, data[2][0]);
        Assert.assertEquals(true, data[3][0]);
        Assert.assertEquals(true, data[4][0]);
        Assert.assertEquals(true, data[5][0]);
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestBooleanNegative")
    public void booleanCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestBooleanNegative");

        parser.getDataBlock(findMethodByNameOnly("booleanParseMethod"));
    }

    //BYTE
    @Test
    @TestOptions( dataSheet = "TestByte")
    public void byteCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestByte");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("byteParseMethod"));

        Assert.assertEquals((byte) 0, data[0][0]);
        Assert.assertEquals((byte) 123, data[1][0]);
        Assert.assertEquals((byte) 127, data[2][0]);
        Assert.assertEquals((byte) 126, data[3][0]);
        Assert.assertEquals((byte) -128, data[4][0]);
        Assert.assertEquals((byte) -127, data[5][0]);
        Assert.assertEquals((byte) 2, data[6][0]);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestByteNegative")
    public void byteCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestByteNegative");

        parser.getDataBlock(findMethodByNameOnly("byteParseMethod"));
    }

    //SHORT
    @Test
    @TestOptions( dataSheet = "TestShort")
    public void shortCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestShort");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("shortParseMethod"));

        Assert.assertEquals(data[0][0], (short) 0);
        Assert.assertEquals(data[1][0], (short) 12345);
        Assert.assertEquals(data[2][0], (short) 32767);
        Assert.assertEquals(data[3][0], (short) 32766);
        Assert.assertEquals(data[4][0], (short) -32768);
        Assert.assertEquals(data[5][0], (short) -32767);
        Assert.assertEquals(data[6][0], (short) 2);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestShortNegative")
    public void shortCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestShortNegative");

        parser.getDataBlock(findMethodByNameOnly("shortParseMethod"));
    }

    //INT
    @Test
    @TestOptions( dataSheet = "TestInt")
    public void intCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestInt");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("intParseMethod"));

        Assert.assertEquals(data[0][0], 0);
        Assert.assertEquals(data[1][0], 1234567890);
        Assert.assertEquals(data[2][0], 2147483647);
        Assert.assertEquals(data[3][0], 2147483646);
        Assert.assertEquals(data[4][0], -2147483648);
        Assert.assertEquals(data[5][0], -2147483647);
        Assert.assertEquals(data[6][0], 2);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestIntNegative")
    public void intCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestIntNegative");

        parser.getDataBlock(findMethodByNameOnly("intParseMethod"));
    }

    //LONG
    @Test
    @TestOptions( dataSheet = "TestLong")
    public void longCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestLong");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("longParseMethod"));

        Assert.assertEquals(data[0][0], 0L);
        Assert.assertEquals(data[1][0], 1234567890123456789L);
        Assert.assertEquals(data[2][0], 9223372036854775807L);
        Assert.assertEquals(data[3][0], 9223372036854775806L);
        Assert.assertEquals(data[4][0], -9223372036854775808L);
        Assert.assertEquals(data[5][0], -9223372036854775807L);
        Assert.assertEquals(data[6][0], 2L);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestLongNegative")
    public void longCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestLongNegative");

        parser.getDataBlock(findMethodByNameOnly("longParseMethod"));
    }

    //FLOAT
    @Test
    @TestOptions( dataSheet = "TestFloat")
    public void floatCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestFloat");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("floatParseMethod"));

        Assert.assertEquals((float) 0, data[0][0]);
        Assert.assertEquals(Float.NaN, data[1][0]);
        Assert.assertEquals((float) 1.0123456789, data[2][0]);
        Assert.assertEquals((float) 1.3, data[3][0]);
        Assert.assertEquals(Float.MAX_VALUE, data[4][0]);
        Assert.assertEquals(Float.MIN_VALUE, data[5][0]);
        Assert.assertEquals(-1 * Float.MAX_VALUE, data[6][0]);
        Assert.assertEquals(-1 * Float.MIN_VALUE, data[7][0]);
        Assert.assertEquals((float) 2, data[8][0]);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestFloatNegative")
    public void floatCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestFloatNegative");

        parser.getDataBlock(findMethodByNameOnly("floatParseMethod"));
    }

    //DOUBLE
    @Test
    @TestOptions( dataSheet = "TestDouble")
    public void doubleCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestDouble");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("doubleParseMethod"));

        Assert.assertEquals((double) 0, data[0][0]);
        Assert.assertEquals(Double.NaN, data[1][0]);
        Assert.assertEquals(1.0123456789, data[2][0]);
        Assert.assertEquals(1.3, data[3][0]);
        Assert.assertEquals(Double.MAX_VALUE, data[4][0]);
        Assert.assertEquals(Double.MIN_VALUE, data[5][0]);
        Assert.assertEquals(-1 * Double.MAX_VALUE, data[6][0]);
        Assert.assertEquals(-1 * Double.MIN_VALUE, data[7][0]);
        Assert.assertEquals((double) 2, data[8][0]);

    }

    //DOUBLE
    @Test
    @TestOptions( dataSheet = "TestNumber")
    public void numberCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestNumber");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("numberParseMethod"));

        Assert.assertEquals((double) 0, data[0][0]);
        Assert.assertEquals(Double.NaN, data[1][0]);
        Assert.assertEquals(1.0123456789, data[2][0]);
        Assert.assertEquals(1.1, data[3][0]);
        Assert.assertEquals((double) 1, data[4][0]);
        Assert.assertEquals((double) 123, data[5][0]);
        Assert.assertEquals((double) 2, data[6][0]);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestDoubleNegative")
    public void doubleCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestDoubleNegative");

        parser.getDataBlock(findMethodByNameOnly("doubleParseMethod"));
    }

    //CHAR
    @Test
    @TestOptions( dataSheet = "TestChar")
    public void charCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestChar");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("charParseMethod"));

        Assert.assertEquals('a', data[0][0]);
        Assert.assertEquals('0', data[1][0]);
        Assert.assertEquals(' ', data[2][0]);
        Assert.assertEquals('\uFEFC', data[3][0]);
        Assert.assertEquals('\n', data[4][0]);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestCharNegative")
    public void charCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestCharNegative");

        parser.getDataBlock(findMethodByNameOnly("charParseMethod"));
    }

    //STRING
    @Test
    @TestOptions( dataSheet = "TestString")
    public void stringCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestString");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("stringParseMethod"));

        Assert.assertEquals("", data[0][0]);
        Assert.assertEquals("a", data[1][0]);
        Assert.assertEquals("NULL", data[2][0]);
        Assert.assertEquals("line1\nline2\nline3", data[3][0]);
        Assert.assertEquals("q~!@#$%^&*()_+{}:\"|;'\\[]-=<>?,./", data[4][0]);
        //        Assert.assertEquals( "\u0389\u038A\u038C\u0410\0411\0412\u05D0\u05D1\u05D2\u0630\u0631\u0632", data[5][0] );
    }

    //DATE
    @Test
    @TestOptions( dataSheet = "TestDate")
    public void dateCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestDate");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("dateParseMethod"));
        Assert.assertTrue(data[0][0] instanceof Date);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestDateNegative")
    public void dateCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestDateNegative");

        parser.getDataBlock(findMethodByNameOnly("dateParseMethod"));
    }

    @Test
    @TestOptions( dataSheet = "TestNull")
    public void nullCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestNull");

        Object[][] data = parser.getDataBlock(findMethodByNameOnly("nullParseMethod"));
        Assert.assertEquals(null, data[0][0]);
        Assert.assertEquals(null, data[1][0]);
        Assert.assertEquals(null, data[2][0]);
        Assert.assertEquals(null, data[3][0]);
        Assert.assertEquals(null, data[4][0]);
        Assert.assertEquals(null, data[5][0]);
        Assert.assertEquals(null, data[6][0]);
        Assert.assertEquals(null, data[7][0]);
        Assert.assertEquals(null, data[8][0]);
        Assert.assertEquals(null, data[9][0]);
        Assert.assertEquals(null, data[10][0]);
        Assert.assertEquals(null, data[11][0]);
        Assert.assertEquals(null, data[12][0]);
        Assert.assertEquals(null, data[13][0]);
        Assert.assertEquals(null, data[14][0]);
        Assert.assertEquals(null, data[15][0]);

    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "TestNull")
    public void nullCellParsingNegative() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "TestNull");

        parser.getDataBlock(findMethodByNameOnly("nullParseMethodNegative"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "DuplicateStartCell")
    public void duplicateStartCell() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "DuplicateStartCell");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "DuplicateEndCell")
    public void duplicateEndCell() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "DuplicateEndCell");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "NoStartCell")
    public void noStartCell() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "NoStartCell");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "NoEndCell")
    public void noEndCell() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "NoEndCell");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "WrongOrderStartEnd")
    public void wrongOrderOnStartingAndEndingCells() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "WrongOrderStartEnd");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test
    @TestOptions( dataSheet = "SingleCell")
    public void singleCell() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "SingleCell");
        Object data[][] = parser.getDataBlock(findMethodByNameOnly("method"));
        Assert.assertEquals(1, data.length);
        Assert.assertEquals(1, data[0].length);
        Assert.assertEquals("Value", data[0][0]);
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "Sheet1")
    public void nonExistingSheet() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "Sheet1");
        parser.getDataBlock(findMethodByNameOnly("method"));
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "DiffParamNumber")
    public void differentNumberOfParameters() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "DiffParamNumber");
        parser.getDataBlock(findMethodByNameOnly("multiParamMethod"));
    }

    @Test
    @TestOptions( dataSheet = "CartesianCellParsing")
    public void cartesianCellParsing() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "CartesianCellParsing");
        Object data[][] = parser.getDataBlock(findMethodByNameOnly("cartesianMethod"));

        Assert.assertEquals(1, data[0][0]);
        Assert.assertEquals(3.0f, data[0][1]);
        Assert.assertEquals("Four", data[0][2]);

        Assert.assertEquals(2, data[1][0]);
        Assert.assertEquals(3.0f, data[1][1]);
        Assert.assertEquals("Four", data[1][2]);

        Assert.assertEquals(3, data[2][0]);
        Assert.assertEquals(3.0f, data[2][1]);
        Assert.assertEquals("Four", data[2][2]);

        Assert.assertEquals(1, data[3][0]);
        Assert.assertEquals(4.0f, data[3][1]);
        Assert.assertEquals("Four", data[3][2]);

        Assert.assertEquals(2, data[4][0]);
        Assert.assertEquals(4.0f, data[4][1]);
        Assert.assertEquals("Four", data[4][2]);

        Assert.assertEquals(3, data[5][0]);
        Assert.assertEquals(4.0f, data[5][1]);
        Assert.assertEquals("Four", data[5][2]);

        Assert.assertEquals(1, data[6][0]);
        Assert.assertEquals(3.0f, data[6][1]);
        Assert.assertEquals("Five", data[6][2]);

        Assert.assertEquals(2, data[7][0]);
        Assert.assertEquals(3.0f, data[7][1]);
        Assert.assertEquals("Five", data[7][2]);

        Assert.assertEquals(3, data[8][0]);
        Assert.assertEquals(3.0f, data[8][1]);
        Assert.assertEquals("Five", data[8][2]);

        Assert.assertEquals(1, data[9][0]);
        Assert.assertEquals(4.0f, data[9][1]);
        Assert.assertEquals("Five", data[9][2]);

        Assert.assertEquals(2, data[10][0]);
        Assert.assertEquals(4.0f, data[10][1]);
        Assert.assertEquals("Five", data[10][2]);

        Assert.assertEquals(3, data[11][0]);
        Assert.assertEquals(4.0f, data[11][1]);
        Assert.assertEquals("Five", data[11][2]);
    }

    @Test
    @TestOptions( dataSheet = "SetSheetName1")
    public void setSheetName() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "SetSheetName1");
        Object data[][] = parser.getDataBlock(findMethodByNameOnly("setSheetName"));
        Assert.assertEquals("sheet1 start", data[0][0]);
        Assert.assertEquals(111, data[0][1]);
        //CHANGE THE SHEET
        parser.setSheetName("SetSheetName2");
        data = parser.getDataBlock(findMethodByNameOnly("setSheetName"));
        Assert.assertEquals("sheet2 start", data[0][0]);
        Assert.assertEquals(222, data[0][1]);
    }

    @Test( expected = DataProviderException.class)
    @TestOptions( dataSheet = "ParameterTypeDiffers")
    public void parameterTypeDiffers() throws Exception {

        parser = new ExcelParser(IoUtils.readFile(EXCEL_FILE_PATH), "ParameterTypeDiffers");
        parser.getDataBlock(findMethodByNameOnly("paramTypeDiffers"));
    }

}
