/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.components.system.operations.clients.MachineDescriptionOperations;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;
import com.axway.ats.log.autodb.io.DbAccessFactory;
import com.axway.ats.log.autodb.io.SQLServerDbWriteAccess;

/**
 * Allows retrieving static info about a machine
 */
@PublicAtsApi
public class MachineInfoAgent {

    private static final Logger log = LogManager.getLogger(MachineInfoAgent.class);

    /**
     * Retrieves static info about a machine and stores this info into the
     * log DB
     *
     * @param atsAgent the address of the ATS Agent running on the machine of interest
     * @param dbMachineName the name of the machine as it appears in the DB
     * @throws Exception
     */
    @PublicAtsApi
    public void updateMachineInfo(
                                   @Validate( name = "atsAgent", type = ValidationType.STRING_SERVER_WITH_PORT) String atsAgent,
                                   @Validate( name = "dbMachineName", type = ValidationType.STRING_NOT_EMPTY) String dbMachineName ) throws Exception {

        // validate input parameters
        atsAgent = HostUtils.getAtsAgentIpAndPort(atsAgent);
        new Validator().validateMethodParameters(new Object[]{ atsAgent, dbMachineName });

        log.info("Retrieving info about " + dbMachineName + " from " + atsAgent);

        MachineDescriptionOperations mm = new MachineDescriptionOperations(atsAgent);
        String machineDescriptionString = mm.getDescription();

        log.info("Saving retrieved info about " + dbMachineName + " into the Test Explorer database");
        SQLServerDbWriteAccess dbAccess = new DbAccessFactory().getNewDbWriteAccessObject();
        dbAccess.updateMachineInfo(dbMachineName, machineDescriptionString, true);

        log.info("Successfully updated the info about " + dbMachineName);
    }
}
