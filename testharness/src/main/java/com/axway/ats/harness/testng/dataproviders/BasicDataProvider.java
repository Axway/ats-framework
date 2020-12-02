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

package com.axway.ats.harness.testng.dataproviders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;

import com.axway.ats.config.exceptions.ConfigurationException;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.harness.TestHarnessConfigurator;
import com.axway.ats.harness.testng.TestOptions;
import com.axway.ats.harness.testng.exceptions.DataProviderException;

public class BasicDataProvider {

    private final String XLSX = ".xlsx";
    private final String XLS  = ".xls";

    /**
     * Returns the {@link InputStream} to the method input data file. The folder holding the file can be specified by
     * TestOptions annotation in the test class, method or in the harness properties file. If not specified in any of
     * these locations, search for the data file in the classpath.
     *
     * The name of data file can be specified by TestOptions annotation in the test method. If not - use the test class
     * name
     *
     * @param m the test method
     * @return data file {@link InputStream}
     * @throws DataProviderException
     * @throws ConfigurationException
     * @throws NoSuchPropertyException
     */
    protected InputStream getDataFileInputStream(
                                                  Method m ) throws DataProviderException,
                                                             NoSuchPropertyException, ConfigurationException {

        // Search data file folder in:
        // - test method TestOptions annotation
        // - test class TestOptions annotation
        // - test harness properties file
        String dataFileFolder = getDataFileFolder(m);
        // Search data file in(it will never return null):
        // - test class TestOptions annotation
        // - use the class name
        String dataFileName;

        if (dataFileFolder != null) {

            dataFileName = getDataFileName(m, XLSX);
            // We know the data file folder. Check if the file exists.
            File dataFile = new File(new File(dataFileFolder), dataFileName);

            if (!dataFile.exists()) {
                dataFileName = getDataFileName(m, XLS);
                dataFile = new File(new File(dataFileFolder), dataFileName);
            }

            try {
                return new FileInputStream(dataFile);
            } catch (FileNotFoundException fnfe) {
                throw new DataProviderException("Data file does not exist. Neither " + dataFile
                                                + " nor same name .xlsx file was found. ");
            }
        } else {

            dataFileName = getDataFileName(m, XLSX);
            // Data file folder is not specified.
            // Try to find the data file in the classpath.
            InputStream fileInputStream = m.getDeclaringClass().getResourceAsStream(dataFileName);

            if (fileInputStream != null) {
                return fileInputStream;
            }

            dataFileName = getDataFileName(m, XLS);
            // Data file folder is not specified.
            // Try to find the data file in the classpath.
            fileInputStream = m.getDeclaringClass().getResourceAsStream(dataFileName);

            if (fileInputStream == null) {
                throw new DataProviderException("Data file does not exist. Neither " + dataFileName
                                                + " nor same name .xlsx was found in classpath");
            }

            return fileInputStream;
        }
    }

    /**
     * Return the data sheet name. First search in TestOptions annotation on the test method, if not specified there,
     * use the test method name.
     *
     * @param m
     * @return
     */
    protected String getDataSheet(
                                   Method m ) {

        // if the method annotation does not specify the sheet name use the method name
        String dataSheet = m.getName();

        // search via TestOptions annotation applied on test method
        TestOptions testOptions = m.getAnnotation(TestOptions.class);
        if (testOptions != null) {
            if (testOptions.dataSheet().length() > 0) {
                dataSheet = testOptions.dataSheet();
            }
        }

        return dataSheet;
    }

    /**
     * Return the name of the data file. First search in TestOptions annotation on the test method, if not specified
     * there, use the test class name.
     *
     * NOTE: If the file name is provided by TestOptions annotation it is used as it comes. For example "myFile.xml" or
     * "com.axway.test.myFile.xml". If it is resolved from the test class name, it is constructed by package +
     * class name
     *
     * @param m
     * @return
     * @throws DataProviderException
     */
    private String getDataFileName(
                                    Method m,
                                    String fileExtension ) throws DataProviderException {

        // Data file folder is specified. Search for the file name.
        String dataFileName;

        // search thru a TestOptions annotation applied on test method
        TestOptions testOptions = m.getAnnotation(TestOptions.class);
        if (testOptions != null && testOptions.dataFile().length() > 0) {
            dataFileName = testOptions.dataFile();
        } else {
            //either there was no TestOptions annotation or it did not have the dataFile attribute set
            //we use the class name as name of the data file
            Class<?> declaringClass = m.getDeclaringClass();
            dataFileName = declaringClass.getName() + fileExtension;
        }

        return transformFileName(dataFileName);
    }

    private String transformFileName(
                                      String fileName ) throws DataProviderException {

        int indexLastDot = fileName.lastIndexOf('.');
        if (indexLastDot < 1) {
            throw new DataProviderException("No file extension found in file name: " + fileName);
        }

        String filePathAndName = fileName.substring(0, indexLastDot);
        String fileExtension = fileName.substring(indexLastDot + 1);

        //if this is a fully qualified name put a slash in front of it
        if (filePathAndName.indexOf('.') > -1) {
            filePathAndName = "/" + filePathAndName;
        }

        return filePathAndName.replaceAll("\\.", "/") + "." + fileExtension;
    }

    /**
     * Return the folder with the data file. First search in TestOptions annotation on the test method, then in the
     * TestOptions annotation on the test class and finally in the harness properties file.
     *
     * @param m
     * @return
     * @throws ConfigurationException
     * @throws NoSuchPropertyException
     */
    private String getDataFileFolder(
                                      Method m ) throws NoSuchPropertyException, ConfigurationException {

        String dataFileFolder;

        TestOptions testOptions;

        // search thru a TestOptions annotation applied on test method
        testOptions = m.getAnnotation(TestOptions.class);
        if (testOptions != null) {
            dataFileFolder = testOptions.dataFileFolder();
            if (dataFileFolder.length() > 0) {
                // the test method specifies the data file folder
                return dataFileFolder;
            }
        }

        // search thru a TestOptions annotation applied on test class
        testOptions = m.getDeclaringClass().getAnnotation(TestOptions.class);
        if (testOptions != null) {
            dataFileFolder = testOptions.dataFileFolder();
            if (dataFileFolder.length() > 0) {
                // the test class specifies the data file folder
                return dataFileFolder;
            }
        }

        // search thru a test harness properties file
        dataFileFolder = TestHarnessConfigurator.getInstance().getSuitesRootDirectory();
        if (dataFileFolder != null && !"".equals(dataFileFolder)) {
            return dataFileFolder;
        }

        // data file folder not found
        return null;
    }
}
