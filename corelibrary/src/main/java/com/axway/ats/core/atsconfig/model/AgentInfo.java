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
package com.axway.ats.core.atsconfig.model;

import java.util.Map;

import org.w3c.dom.Element;

import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;

public class AgentInfo extends AbstractApplicationInfo {

    private ShellCommandInfo restartCommandInfo;
    private ShellCommandInfo versionCommandInfo;

    public AgentInfo( String alias,
                      Element agentNode,
                      Map<String, String> defaultValues ) throws AtsConfigurationException {

        super(alias, agentNode, defaultValues);
    }

    private static final String AGENT_FOLDER_PARAM  = "<agentFolderPath>";
    private static final String AGENT_PORT_PARAM    = "<port>";
    private static final String AGENT_JAVAEXE_PARAM = "<java_exe>";
    private static final String AGENT_MEMORY_PARAM  = "<memory>";

    // List of supported agent commands per OS
    public class AgentCommands {
        class Windows {
            static final String START   = "cmd.exe /c start cmd.exe /k \"cd /d " + AGENT_FOLDER_PARAM
                                          + " && agent.bat start" + AGENT_PORT_PARAM + AGENT_JAVAEXE_PARAM
                                          + AGENT_MEMORY_PARAM + "\"";
            static final String STOP    = "cmd.exe /c \"cd /d " + AGENT_FOLDER_PARAM + " && agent.bat stop"
                                          + AGENT_PORT_PARAM + "\"";
            static final String RESTART = "cmd.exe /c start cmd.exe /k \"cd /d " + AGENT_FOLDER_PARAM
                                          + " && agent.bat restart" + AGENT_PORT_PARAM + AGENT_JAVAEXE_PARAM
                                          + AGENT_MEMORY_PARAM + "\"";
            static final String STATUS  = "cmd.exe /c \"cd /d " + AGENT_FOLDER_PARAM + " && agent.bat status"
                                          + AGENT_PORT_PARAM + "\"";

            static final String VERSION = "cmd.exe /c \"cd /d " + AGENT_FOLDER_PARAM + " && agent.bat version"
                                          + AGENT_JAVAEXE_PARAM + "\"";
        }
        class Linux {
            static final String START   = "/bin/bash -lc 'cd " + AGENT_FOLDER_PARAM + " && (./agent.sh start"
                                          + AGENT_PORT_PARAM + AGENT_JAVAEXE_PARAM + AGENT_MEMORY_PARAM
                                          + " > nohup.out 2>&1) && sleep 5 && cat nohup.out && rm nohup.out'";
            static final String STOP    = "/bin/bash -lc 'cd " + AGENT_FOLDER_PARAM + " && ./agent.sh stop"
                                          + AGENT_PORT_PARAM + "'";
            static final String RESTART = "/bin/bash -lc 'cd " + AGENT_FOLDER_PARAM
                                          + " && (./agent.sh restart" + AGENT_PORT_PARAM + AGENT_JAVAEXE_PARAM
                                          + AGENT_MEMORY_PARAM
                                          + " > nohup.out 2>&1) && sleep 5 && cat nohup.out && rm nohup.out'";
            static final String STATUS  = "/bin/bash -lc 'cd " + AGENT_FOLDER_PARAM + " && ./agent.sh status"
                                          + AGENT_PORT_PARAM + "'";

            static final String VERSION = "/bin/bash -lc 'cd " + AGENT_FOLDER_PARAM + " && ./agent.sh version"
                                          + AGENT_JAVAEXE_PARAM + "'";
        }
    }

    @Override
    protected void loadMoreInfo(
                                 Element applicationNode ) {

        if (isUnix()) {
            statusCommandInfo.command = AgentCommands.Linux.STATUS.replace(AGENT_FOLDER_PARAM, home)
                                                                  .replace(AGENT_PORT_PARAM,
                                                                           getPortToken());
        } else {
            statusCommandInfo.command = AgentCommands.Windows.STATUS.replace(AGENT_FOLDER_PARAM, home)
                                                                    .replace(AGENT_PORT_PARAM,
                                                                             getPortToken());
        }

        if (isUnix()) {
            startCommandInfo.command = AgentCommands.Linux.START.replace(AGENT_FOLDER_PARAM, home)
                                                                .replace(AGENT_PORT_PARAM, getPortToken())
                                                                .replace(AGENT_JAVAEXE_PARAM,
                                                                         getJavaExecutableToken())
                                                                .replace(AGENT_MEMORY_PARAM,
                                                                         getMemoryToken());
        } else {
            startCommandInfo.command = AgentCommands.Windows.START.replace(AGENT_FOLDER_PARAM, home)
                                                                  .replace(AGENT_PORT_PARAM, getPortToken())
                                                                  .replace(AGENT_JAVAEXE_PARAM,
                                                                           getJavaExecutableToken())
                                                                  .replace(AGENT_MEMORY_PARAM,
                                                                           getMemoryToken());
        }

        if (isUnix()) {
            stopCommandInfo.command = AgentCommands.Linux.STOP.replace(AGENT_FOLDER_PARAM, home)
                                                              .replace(AGENT_PORT_PARAM, getPortToken());
        } else {
            stopCommandInfo.command = AgentCommands.Windows.STOP.replace(AGENT_FOLDER_PARAM, home)
                                                                .replace(AGENT_PORT_PARAM, getPortToken());
        }

        restartCommandInfo = new ShellCommandInfo();
        if (isUnix()) {
            restartCommandInfo.command = AgentCommands.Linux.RESTART.replace(AGENT_FOLDER_PARAM, home)
                                                                    .replace(AGENT_PORT_PARAM,
                                                                             getPortToken())
                                                                    .replace(AGENT_JAVAEXE_PARAM,
                                                                             getJavaExecutableToken())
                                                                    .replace(AGENT_MEMORY_PARAM,
                                                                             getMemoryToken());
        } else {
            restartCommandInfo.command = AgentCommands.Windows.RESTART.replace(AGENT_FOLDER_PARAM, home)
                                                                      .replace(AGENT_PORT_PARAM,
                                                                               getPortToken())
                                                                      .replace(AGENT_JAVAEXE_PARAM,
                                                                               getJavaExecutableToken())
                                                                      .replace(AGENT_MEMORY_PARAM,
                                                                               getMemoryToken());
        }

        versionCommandInfo = new ShellCommandInfo();
        if (isUnix()) {
            versionCommandInfo.command = AgentInfo.AgentCommands.Linux.VERSION.replace(AGENT_FOLDER_PARAM,
                                                                                       home)
                                                                              .replace(AGENT_JAVAEXE_PARAM,
                                                                                       getJavaExecutableToken());
        } else {
            versionCommandInfo.command = AgentInfo.AgentCommands.Windows.VERSION.replace(AGENT_FOLDER_PARAM,
                                                                                         home)
                                                                                .replace(AGENT_JAVAEXE_PARAM,
                                                                                         getJavaExecutableToken());
        }
    }

    public String getRestartCommand() {

        return restartCommandInfo.command;
    }

    public String getVersionCommand() {

        return versionCommandInfo.command;
    }
}
