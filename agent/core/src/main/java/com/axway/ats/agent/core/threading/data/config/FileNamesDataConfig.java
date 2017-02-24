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
package com.axway.ats.agent.core.threading.data.config;

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.agent.core.threading.data.FileContainer;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

/**
 * A class for generating input data for parameter which expects a file name.
 * This class is initialized using a folder - the input parameters will be generated
 * using the file names in this folder. The folder can be scanned recursively.
 */
@SuppressWarnings("serial")
public class FileNamesDataConfig extends AbstractParameterDataConfig {

    private static final String MATCH_ALL_PATTERN = ".*";
    private static final int    HUNDRED_PERCENTS  = 100;

    private List<FileContainer> fileContainers    = new ArrayList<FileContainer>();
    private boolean             recursiveSearch;
    private boolean             returnFullPath;

    /**
     * Constructor - values are generated per thread
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     */
    public FileNamesDataConfig( String parameterName,
                                String folderName ) {

        super( parameterName, ParameterProviderLevel.PER_THREAD );

        this.fileContainers.add( new FileContainer( folderName, HUNDRED_PERCENTS, MATCH_ALL_PATTERN ) );
        this.recursiveSearch = true;
        this.returnFullPath = true;
    }

    /**
     * Constructor - values are generated per thread
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     * @param returnFullPath return the absolute file path or just the name of the file
     */
    public FileNamesDataConfig( String parameterName,
                                String folderName,
                                boolean returnFullPath ) {

        super( parameterName, ParameterProviderLevel.PER_THREAD );

        this.fileContainers.add( new FileContainer( folderName, HUNDRED_PERCENTS, MATCH_ALL_PATTERN ) );
        this.recursiveSearch = true;
        this.returnFullPath = returnFullPath;
    }

    /**
     * Constructor - values are generated per thread
     *
     * @param parameterName the name of the parameter to generate data for
     */
    public FileNamesDataConfig( String parameterName ) {

        super( parameterName, ParameterProviderLevel.PER_THREAD );

        this.recursiveSearch = true;
        this.returnFullPath = true;
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     * @param parameterProviderLevel the level at which new values will be generated
     */
    public FileNamesDataConfig( String parameterName,
                                String folderName,
                                ParameterProviderLevel parameterProviderLevel ) {

        super( parameterName, parameterProviderLevel );

        this.fileContainers.add( new FileContainer( folderName, HUNDRED_PERCENTS, MATCH_ALL_PATTERN ) );
        this.recursiveSearch = true;
        this.returnFullPath = true;
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     * @param recursiveSearch whether to get the files from the sub-folders of the given folder as well
     * @param parameterProviderLevel the level at which new values will be generated
     */
    public FileNamesDataConfig( String parameterName,
                                String folderName,
                                boolean recursiveSearch,
                                ParameterProviderLevel parameterProviderLevel ) {

        super( parameterName, parameterProviderLevel );

        this.fileContainers.add( new FileContainer( folderName, HUNDRED_PERCENTS, MATCH_ALL_PATTERN ) );
        this.recursiveSearch = recursiveSearch;
        this.returnFullPath = true;
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     * @param recursiveSearch whether to get the files from the sub-folders of the given folder as well
     * @param returnFullPath return the absolute file path or just the name of the file
     * @param parameterProviderLevel the level at which new values will be generated
     */
    public FileNamesDataConfig( String parameterName,
                                String folderName,
                                boolean recursiveSearch,
                                boolean returnFullPath,
                                ParameterProviderLevel parameterProviderLevel ) {

        super( parameterName, parameterProviderLevel );

        this.fileContainers.add( new FileContainer( folderName, HUNDRED_PERCENTS, MATCH_ALL_PATTERN ) );
        this.recursiveSearch = recursiveSearch;
        this.returnFullPath = returnFullPath;
    }

    /**
     * Constructor
     *
     * @param parameterName the name of the parameter to generate data for
     * @param folderName the name of the folder to take the files from - the folder is scanned
     * recursively
     * @param recursiveSearch whether to get the files from the sub-folders of the given folder as well
     * @param returnFullPath return the absolute file path or just the name of the file
     * @param parameterProviderLevel the level at which new values will be generated
     */
    public FileNamesDataConfig( String parameterName,
                                List<FileContainer> fileContainers,
                                boolean recursiveSearch,
                                boolean returnFullPath,
                                ParameterProviderLevel parameterProviderLevel ) {

        super( parameterName, parameterProviderLevel );

        this.fileContainers = fileContainers;
        this.recursiveSearch = recursiveSearch;
        this.returnFullPath = returnFullPath;
    }

    /**
     * Should the folder scanning include sub-folders or not
     *
     * @return true if the folder scanning should include sub-folders as well
     */
    public boolean getRecursiveSearch() {

        return recursiveSearch;
    }

    public List<FileContainer> getFileContainers() {

        return fileContainers;
    }

    public void addFolder(
                           String folderName,
                           int percent ) {

        addFolder( folderName, percent, MATCH_ALL_PATTERN );
    }

    public void addFolder(
                           String folderName,
                           int percent,
                           String pattern ) {

        this.fileContainers.add( new FileContainer( folderName, percent, pattern ) );
    }

    public void setRecursiveSearch(
                                    boolean recursiveSearch ) {

        this.recursiveSearch = recursiveSearch;
    }

    public void setReturnFullPath(
                                   boolean returnFullPath ) {

        this.returnFullPath = returnFullPath;
    }

    /**
     * Should the provider return the full path or just the file name
     *
     * @return true - the provider will return the absolute path to the file, false - just the file name
     */
    public boolean getReturnFullPath() {

        return returnFullPath;
    }

    @Override
    List<ParameterDataConfig> distribute(
                                          int agents ) {

        // currently we do not split (but we clone) this data configurator
        List<ParameterDataConfig> distributedParameterProviders = new ArrayList<ParameterDataConfig>();
        for( int i = 0; i < agents; i++ ) {
            distributedParameterProviders.add( new FileNamesDataConfig( this.parameterName,
                                                                        this.fileContainers,
                                                                        this.recursiveSearch,
                                                                        this.returnFullPath,
                                                                        this.parameterProviderLevel ) );
        }
        return distributedParameterProviders;
    }

    @Override
    public void verifyDataConfig() throws ParameterDataProviderInitalizationException {

        int fileContainerPercents = 0;
        for( FileContainer container : getFileContainers() ) {
            fileContainerPercents += container.getPercentage();
        }
        if( fileContainerPercents != 100 ) {
            throw new ParameterDataProviderInitalizationException( "The sum of percentages of all folders for the file data configurator '"
                                                                   + this.parameterName
                                                                   + "' is "
                                                                   + fileContainerPercents
                                                                   + "% (different than 100)" );
        }
    }

}
