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
package com.axway.ats.agent.core.ant;

import java.io.File;
import java.util.HashMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ACGen extends Task {

    private String                  descriptor;

    private File                    sourcedir;

    private File                    destdir;

    private String                  targetPackage;

    private String                  sourcePackage;

    private HashMap<String, String> customTemplates = new HashMap<String, String>();

    @Override
    public void execute() {

        //check if all the required properites are set
        if (descriptor == null) {
            throw new BuildException("Property 'descriptor' is required - set it to the location of the Agent descriptor file");
        }

        if (sourcedir == null) {
            throw new BuildException("Property 'sourcedir' is required - set it to the folder containing the source code of the action classes");
        }

        if (destdir == null) {
            throw new BuildException("Property 'destdir' is required - set it to the destination folder");
        }

        if (targetPackage == null) {
            throw new BuildException("Property 'targetPackage' is required - set it to the target package for the generated stubs");
        }

        ActionClassGenerator actionClassGen = new ActionClassGenerator(descriptor,
                                                                       sourcedir.getAbsolutePath(),
                                                                       destdir.getAbsolutePath(),
                                                                       sourcePackage,
                                                                       targetPackage,
                                                                       customTemplates);

        try {
            actionClassGen.generate();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public String getDescriptor() {

        return descriptor;
    }

    public void setDescriptor(
                               String descriptor ) {

        this.descriptor = descriptor;
    }

    public File getDestdir() {

        return destdir;
    }

    public void setDestdir(
                            File destdir ) {

        this.destdir = destdir;
    }

    public File getSorucedir() {

        return sourcedir;
    }

    public void setSourcedir(
                              File sourcedir ) {

        this.sourcedir = sourcedir;
    }

    public String getSourcePackage() {

        return sourcePackage;
    }

    public void setSourcePackage(
                                  String sourcePackage ) {

        this.sourcePackage = sourcePackage;
    }

    public String getPackage() {

        return targetPackage;
    }

    public void setPackage(
                            String targetPackage ) {

        this.targetPackage = targetPackage;
    }

    /**
     * Method for adding an inner <template> element
     * 
     * @param template  the CustomTemplate instance
     */
    public void addConfiguredClassTemplate(
                                            CustomClassTemplate template ) {

        customTemplates.put(template.getActionClass(), template.getTemplate());
    }
}
