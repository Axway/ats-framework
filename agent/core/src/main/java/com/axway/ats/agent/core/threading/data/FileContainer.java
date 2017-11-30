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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axway.ats.agent.core.action.ArgumentValue;
import com.axway.ats.core.utils.IoUtils;

public class FileContainer implements Serializable {

    private static final long    serialVersionUID  = 1L;

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{([\\d\\w]+)\\}");

    private String               folderName;

    private int                  percentage;

    private String               pattern;

    private Set<String>          patternParameters;

    private List<String>         fileList          = new ArrayList<String>();

    private Map<Long, Integer>   threadIndexes     = new HashMap<Long, Integer>();

    private int                  currentIndex      = 0;

    /**
     *
     * @param folderName folder name
     * @param percentage percentage of file usage from the folder
     * @param pattern file names pattern
     */
    public FileContainer( String folderName, int percentage, String pattern ) {

        this.folderName = folderName;
        this.percentage = percentage;
        this.pattern = pattern;
        this.patternParameters = extractPatternParameters();
    }

    /**
     *
     * @param currentThreadId current thread id
     * @param isStaticValue if the wanted value is static
     * @param previousValues previous data config parameters with their values
     * @return the next/same file name for the current thread
     */
    public String getFileName( Long currentThreadId, Boolean isStaticValue,
                               List<ArgumentValue> previousValues ) {

        int index = getNextFileIndex(currentThreadId, isStaticValue);
        String fileName = fileList.get(index);
        if (this.patternParameters != null) {

            String currentPattern = this.pattern;
            for (ArgumentValue arg : previousValues) {
                if (this.patternParameters.contains(arg.getName())) {
                    currentPattern = currentPattern.replace("${" + arg.getName() + "}",
                                                            (String) arg.getValue());
                }
            }
            int startIndex = index;
            Pattern p = Pattern.compile(currentPattern);
            while (!p.matcher(IoUtils.getFileName(fileName)).matches()) {

                // if (isStaticValue) then this is a case when the file name didn't match the pattern and
                // getNextFileIndex() will retrieve the same index every time, so we have to force change the
                // current thread index => isStaticValue = false
                index = getNextFileIndex(currentThreadId, false);
                if (index == startIndex) {

                    //we check all files in the directory and there is no file matching our pattern
                    throw new RuntimeException("No files matching regex pattern '" + currentPattern
                                               + "' in directory: " + this.folderName);
                }
                fileName = fileList.get(index);
            }
        }
        return fileName;
    }

    /**
     *
     * @param previousValues previous data config parameters with their values
     * @return the next file name
     */
    public String getFileName( List<ArgumentValue> previousValues ) {

        return getFileName(null, null, previousValues);
    }

    /**
     *
     * @param currentThreadId current thread id
     * @param isStaticValue is the wanted value static or not
     * @return the next file index if isStaticValue = false otherwise the same index
     */
    private int getNextFileIndex( Long currentThreadId, Boolean isStaticValue ) {

        if (currentThreadId == null) {
            // PER_INVOCATION
            // return current global (folder) index
            if (currentIndex >= fileList.size()) {
                currentIndex = 0;
            }
            return currentIndex++;
        }

        // return the file index for the current thread
        Integer index = threadIndexes.get(currentThreadId);
        if (isStaticValue) { // THREAD_STATIC
            if (index != null) {
                return index;
            } else {
                // first run for THREAD_STATIC
                if (currentIndex >= fileList.size()) {
                    currentIndex = 0;
                }
                index = currentIndex++;
            }
        } else {
            // isStaticValue = false, THREADED parameter level
            if (index != null) {
                index++;
            } else {
                index = currentIndex;
            }
        }

        if (index >= fileList.size()) {
            index = 0;
        }
        threadIndexes.put(currentThreadId, index);
        return index;
    }

    /**
     * Retrieve data config parameters from the regex pattern. For example if the pattern is "${username}_1234.*"
     * the parameters list will be { "username" }
     * @return data config parameters
     */
    private Set<String> extractPatternParameters() {

        Set<String> parameters = new HashSet<String>();
        Matcher matcher = PARAMETER_PATTERN.matcher(pattern);
        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
        if (parameters.size() == 0) {
            return null;
        }
        return parameters;
    }

    /**
     *
     * @return folder name
     */
    public String getFolderName() {

        return folderName;
    }

    /**
     *
     * @return the percentage of file usage from the folder
     */
    public int getPercentage() {

        return percentage;
    }

    /**
     *
     * @return file regex pattern
     */
    public String getPattern() {

        return pattern;
    }

    /**
     *
     * @param fileList the files list
     */
    public void setFileList( List<String> fileList ) {

        this.fileList = fileList;
    }

    /**
     *
     * @return if the regex pattern contains data parameters eg. "${username}_1234.*"
     */
    public boolean isPatternContainingParams() {

        return patternParameters != null;
    }

}
