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

package com.axway.ats.action.filetransfer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.axway.ats.config.AbstractConfigurator;
import com.axway.ats.config.exceptions.NoSuchPropertyException;
import com.axway.ats.core.utils.StringUtils;

/**
 * This class is used to read configuration properties defining custom file transfer clients
 */
public class FileTransferConfigurator extends AbstractConfigurator {

    private static final String             ATS_ADAPTERS_FILE           = "/ats-adapters.properties";

    private static final String             CUSTOM_FILE_TRANSFER_CLIENT = "actionlibrary.filetransfer.client.";

    private Map<String, String>             fileTransferClientsMap      = new HashMap<String, String>();

    /**
     * The singleton instance for this configurator
     */
    private static FileTransferConfigurator instance;

    static {
        // singleton effect in this way. Not lazily loaded but not so expensive resource
        instance = getNewInstance();
    }

    private FileTransferConfigurator() {

        super();
    }

    /**
     * @return an instance of this configurator
     */
    static FileTransferConfigurator getInstance() {

        return instance; // for sure not null
    }

    /**
     * @return an instance of this configurator
     */
    private static FileTransferConfigurator getNewInstance() {

        FileTransferConfigurator newInstance = new FileTransferConfigurator();

        newInstance.addConfigFileFromClassPath(ATS_ADAPTERS_FILE, true, false);
        return newInstance;
    }

    @Override
    protected void reloadData() {

        // We load all properties as optional. Error will be thrown when a needed property is requested, but not present.
        Map<String, String> transferClientCustomProps = null;
        try {
            transferClientCustomProps = getProperties(CUSTOM_FILE_TRANSFER_CLIENT);
        } catch (NoSuchPropertyException e) {}

        if (transferClientCustomProps != null) {
            for (Entry<String, String> entry : transferClientCustomProps.entrySet()) {
                fileTransferClientsMap.put(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * @return the custom class for the given transfer protocol.
     * @throws FileTransferConfiguratorException if could not load the needed custom class name from the properties file.
     */
    public String getFileTransferClient( String customProtocol ) {

        for (Entry<String, String> entry : fileTransferClientsMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(CUSTOM_FILE_TRANSFER_CLIENT + customProtocol)) {
                if (StringUtils.isNullOrEmpty(entry.getValue())) {
                    throw new FileTransferConfiguratorException("Uknown custom client for " + customProtocol
                                                                + " protocol. Either " + ATS_ADAPTERS_FILE
                                                                + " file is not in the classpath or "
                                                                + entry.getKey()
                                                                + " property is missing/empty!");
                }
                log.info("Transfers over '" + customProtocol + "' will be served by " + entry.getValue());

                return entry.getValue();
            }
        }

        throw new FileTransferConfiguratorException("No custom client implementation for " + customProtocol
                                                    + " protocol");
    }
}
