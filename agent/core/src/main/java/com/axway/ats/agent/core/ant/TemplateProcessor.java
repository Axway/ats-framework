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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;

import com.axway.ats.common.systemproperties.AtsSystemProperties;

/**
 * This class is used for processing a template file and replacing
 * the given placeholders in the template with actual values
 */
class TemplateProcessor {

    protected static final String     LINE_SEPARATOR = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

    //holds the place holder - actual value map
    protected HashMap<String, String> placeHolderValues;

    //the template
    private String                    template;

    public TemplateProcessor( InputStream templateStream,
                              String templateName ) throws IOException {

        if (templateStream == null) {
            throw new BuildException("Could not read template " + templateName);
        }

        //init the placeholders
        placeHolderValues = new HashMap<String, String>();

        //read the template
        int temlateLength = templateStream.available();
        byte[] templateBuffer = new byte[temlateLength];
        templateStream.read(templateBuffer);

        template = new String(templateBuffer);
    }

    public String processTemplate() {

        String processedTemplate = template;

        //replace the placeholders with the appropriate values
        Set<Map.Entry<String, String>> placeHolders = placeHolderValues.entrySet();
        for (Map.Entry<String, String> placeHolderEntry : placeHolders) {

            String placeHolder = placeHolderEntry.getKey();
            String value = placeHolderEntry.getValue();

            processedTemplate = processedTemplate.replace(placeHolder, value);
        }

        return processedTemplate;
    }
}
