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
package com.axway.ats.agent.core.threading.data;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.agent.core.threading.data.config.ParameterProviderLevel;
import com.axway.ats.agent.core.threading.exceptions.ParameterDataProviderInitalizationException;

/**
 * Data provider for file names from a given folder - during initialization, the all file names
 * from a folder a put in a list. Upon request, the next value from the list is returned.
 */
public class FileNamesParameterDataProvider extends AbstractParameterDataProvider {

    private List<FileContainer> fileContainers;
    private List<FileContainer> containersArray;
    private final boolean       recursiveSearch;
    private final boolean       returnFullPath;
    private int                 containersCount;
    private int                 currentFileContainerIndex;

    FileNamesParameterDataProvider( String parameterName,
                                    List<FileContainer> fileContainers,
                                    boolean recursiveSearch,
                                    boolean returnFullPath,
                                    ParameterProviderLevel parameterProviderLevel ) {

        super(parameterName, parameterProviderLevel);

        this.fileContainers = fileContainers;
        this.recursiveSearch = recursiveSearch;
        this.returnFullPath = returnFullPath;
    }

    @Override
    protected void doInitialize() throws ParameterDataProviderInitalizationException {

        if (fileContainers != null && fileContainers.size() > 0) {

            createFileContainersArray();
        } else {

            throw new ParameterDataProviderInitalizationException("No folders are specified");
        }

        containersCount = containersArray.size();
        currentFileContainerIndex = 0;
    }

    @Override
    protected ArgumentValue generateNewValuePerInvocation(
                                                           List<ArgumentValue> alreadyResolvedValues ) {

        if (currentFileContainerIndex >= containersCount) {
            currentFileContainerIndex = 0;
        }

        return new ArgumentValue(parameterName,
                                 containersArray.get(currentFileContainerIndex++)
                                                .getFileName(alreadyResolvedValues));
    }

    @Override
    protected ArgumentValue generateNewValuePerThread(
                                                       long currentThreadId,
                                                       List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) {
            valueIndexPerThread++;
        } else {

            // we have a new thread started
            valueIndexPerThread = currentFileContainerIndex;
        }
        if (valueIndexPerThread >= containersCount) {
            valueIndexPerThread = 0;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName,
                                 containersArray.get(valueIndexPerThread)
                                                .getFileName(currentThreadId,
                                                             false,
                                                             alreadyResolvedValues));
    }

    @Override
    protected ArgumentValue generateNewValuePerThreadStatic(
                                                             long currentThreadId,
                                                             List<ArgumentValue> alreadyResolvedValues ) {

        Integer valueIndexPerThread = perThreadIndexes.get(currentThreadId);
        if (valueIndexPerThread != null) {
            return new ArgumentValue(parameterName,
                                     containersArray.get(valueIndexPerThread)
                                                    .getFileName(currentThreadId,
                                                                 true,
                                                                 alreadyResolvedValues));
        }
        // we have a new thread started
        if (currentFileContainerIndex >= containersCount) {
            currentFileContainerIndex = 0;
        }
        valueIndexPerThread = currentFileContainerIndex++;
        if (valueIndexPerThread >= containersCount) {
            valueIndexPerThread = 0;
        }
        perThreadIndexes.put(currentThreadId, valueIndexPerThread);
        return new ArgumentValue(parameterName,
                                 containersArray.get(valueIndexPerThread)
                                                .getFileName(currentThreadId,
                                                             true,
                                                             alreadyResolvedValues));
    }

    /**
     * Get all the files in a folder
     *
     * @param folder the folder to get the files from
     * @param recursiveSearch true if files in sub-folders should also be collected
     * @param useFileFilter use java standard file filter
     * @return list of file names
     */
    private List<String> getFileNamesList(
                                           File folder,
                                           final String reqex,
                                           boolean useFileFilter ) {

        FilenameFilter fnf = null;
        if (useFileFilter) {
            fnf = new FilenameFilter() {

                private Pattern pattern = Pattern.compile(reqex);

                @Override
                public boolean accept(
                                       File dir,
                                       String name ) {

                    return pattern.matcher(name).matches();
                }
            };
        }

        List<String> fileNames = new ArrayList<String>();
        File[] attachementFiles = folder.listFiles(fnf);
        if (attachementFiles != null) {
            for (File attachmentFile : attachementFiles) {
                if (attachmentFile.isFile()) {
                    if (returnFullPath) {
                        fileNames.add(attachmentFile.getPath());
                    } else {
                        fileNames.add(attachmentFile.getName());
                    }
                } else {
                    if (recursiveSearch) {
                        fileNames.addAll(getFileNamesList(attachmentFile, reqex, useFileFilter));
                    }
                }
            }
        }
        //shuffle the list
        Collections.shuffle(fileNames);

        return fileNames;
    }

    /**
     * Generate array of 100 elements with folders mentioned in configuration according to the percentage dispersion.
     * I.e. for
     * <pre>
     *   Folder1/f1, 20%
     *   Folder2/f2, 30%
     *   Folder3/f3, 50%
     *   Will generate:
     *   [f1, f3, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3, f3, f1, f2, f3, f2, f3, f1, f3, f2, f3]
     * </pre>
     * @throws ParameterDataProviderInitalizationException
     */
    private void createFileContainersArray() throws ParameterDataProviderInitalizationException {

        for (FileContainer container : fileContainers) {

            File folder = new File(container.getFolderName());
            if (!folder.exists() || !folder.isDirectory()) {
                throw new ParameterDataProviderInitalizationException(container.getFolderName()
                                                                      + " does not exist or is not a directory");
            }
            List<String> fileNames = getFileNamesList(folder,
                                                      container.getPattern(),
                                                      !container.isPatternContainingParams());
            if (fileNames.size() == 0) {
                throw new ParameterDataProviderInitalizationException("Directory '"
                                                                      + container.getFolderName()
                                                                      + "' is empty or doesn't have files matching regex '"
                                                                      + container.getPattern() + "'");
            }
            container.setFileList(fileNames);
        }

        containersArray = new ArrayList<FileContainer>(); //sorted array according to the percentage
        if (fileContainers.size() == 1) {
            containersArray.add(fileContainers.get(0));
            return;
        }

        boolean cantCalc = true;
        for (int i = 0; i < 100; i++) {
            cantCalc = true;
            // Order of dispersion depends on the order of declaration
            // TODO: sort FileContainers by percentages from bigger to lower
            for (FileContainer container : fileContainers) {
                int freq = Collections.frequency(containersArray, container);
                int perc = 0;
                if (freq != 0) {
                    perc = (int) ( ((double) freq / containersArray.size()) * 100);
                }
                if (perc < container.getPercentage()) {
                    containersArray.add(container);
                    cantCalc = false;
                    break;
                }
            }
            if (cantCalc) {
                containersArray.add(fileContainers.get(0));
            }
        }
    }
}
