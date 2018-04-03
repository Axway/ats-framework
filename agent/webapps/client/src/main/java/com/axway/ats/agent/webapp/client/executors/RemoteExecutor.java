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
package com.axway.ats.agent.webapp.client.executors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axway.ats.agent.core.action.ActionRequest;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.webapp.client.ActionWrapper;
import com.axway.ats.agent.webapp.client.AgentException_Exception;
import com.axway.ats.agent.webapp.client.AgentService;
import com.axway.ats.agent.webapp.client.AgentServicePool;
import com.axway.ats.agent.webapp.client.ArgumentWrapper;
import com.axway.ats.agent.webapp.client.InternalComponentException;
import com.axway.ats.agent.webapp.client.InternalComponentException_Exception;
import com.axway.ats.core.events.TestcaseStateEventsDispacher;
import com.axway.ats.core.utils.ExecutorUtils;
import com.axway.ats.core.utils.HostUtils;

/**
 * This executor will execute an action or a set of actions on
 * a remote ATS Agent
 */
public class RemoteExecutor extends AbstractClientExecutor {

    protected String atsAgent;
    protected String atsAgentSessionId;

    /**
     * @param atsAgent the remote agent address
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent ) throws AgentException {

        this(atsAgent, true);
    }

    /**
     * @param atsAgent the remote agent address
     * @param configureAgent whether we want to send the log configuration to the agent.</br>
     * Pass <i>false</i> when there is chance that the agent is not available - for example 
     * if it was just restarted.
     * @throws AgentException
     */
    public RemoteExecutor( String atsAgent, boolean configureAgent ) throws AgentException {

        // we assume the ATS Agent address here comes with IP and PORT
        this.atsAgent = atsAgent;
        this.atsAgentSessionId = ExecutorUtils.createExecutorId( atsAgent,
                                                                      Thread.currentThread().getName() );

        if (configureAgent) {
            //configure the remote executor(an ATS agent)
            try {
                TestcaseStateEventsDispacher.getInstance().onConfigureAtsAgents(Arrays.asList(atsAgent));
            } catch (Exception e) {
                // we know for sure this is an AgentException, but as the interface declaration is in Core library,
                // we could not declare the AgentException, but its parent - the regular java Exception
                throw(AgentException) e;
            }
        }
    }

    /**
     * Constructor to be used by inheriting classes
     */
    protected RemoteExecutor() {

        this.atsAgent = null;
    }

    @Override
    public Object executeAction( ActionRequest actionRequest ) throws AgentException {

        String actionName = actionRequest.getActionName();
        String componentName = actionRequest.getComponentName();
        Object[] arguments = actionRequest.getArguments();

        Object result = null;

        List<ArgumentWrapper> wrappedArguments = new ArrayList<ArgumentWrapper>();

        try {
            //wrap the arguments - each argument is serialized as
            //a byte stream
            for (Object argument : arguments) {
                ArgumentWrapper argWrapper = new ArgumentWrapper();

                ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
                objectOutStream.writeObject(argument);

                argWrapper.setArgumentValue(byteOutStream.toByteArray());
                wrappedArguments.add(argWrapper);
            }
        } catch (IOException ioe) {
            throw new AgentException("Could not serialize input arguments", ioe);
        }

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgentSessionId);

        try {
            //FIXME: swap with ActionWrapper
            byte[] resultAsBytes = agentServicePort.executeAction(componentName, actionName,
                                                                  wrappedArguments);

            //the result is returned as serialized stream
            //so we need to deserialize it
            ByteArrayInputStream byteInStream = new ByteArrayInputStream(resultAsBytes);
            ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);

            result = objectInStream.readObject();

        } catch (IOException ioe) {
            throw new AgentException("Could not deserialize returned result from agent at " + atsAgent,
                                     ioe);
        } catch (AgentException_Exception ae) {
            throw new AgentException("Error while executing action on agent at " + atsAgent
                                     + ". Exception message: " + ae.getMessage());
        } catch (InternalComponentException_Exception ice) {

            //we need to get internal component exception info from the soap fault
            InternalComponentException faultInfo = ice.getFaultInfo();

            //then construct and throw a real InternalComponentException (not the JAXB mapping type above)
            throw new com.axway.ats.agent.core.exceptions.InternalComponentException(faultInfo.getComponentName(),
                                                                                     faultInfo.getActionName(),
                                                                                     faultInfo.getExceptionMessage()
                                                                                                                + "\n["
                                                                                                                + HostUtils.getLocalHostIP()
                                                                                                                + " stacktrace]",
                                                                                     atsAgent);

        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public boolean isComponentLoaded( ActionRequest actionRequest ) throws AgentException {

        try {
            AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

            //FIXME: swap with ActionWrapper
            return agentServicePort.isComponentLoaded(actionRequest.getComponentName());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getAgentHome() throws AgentException {

        try {
            return AgentServicePool.getInstance().getClientForHost(atsAgent).getAgentHome();
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    public List<String> getClassPath() throws AgentException {

        return AgentServicePool.getInstance().getClientForHost(atsAgent).getClassPath();
    }

    public void logClassPath() throws AgentException {

        AgentServicePool.getInstance().getClientForHost(atsAgent).logClassPath();
    }

    public List<String> getDuplicatedJars() throws AgentException {

        return AgentServicePool.getInstance().getClientForHost(atsAgent).getDuplicatedJars();
    }

    public void logDuplicatedJars() throws AgentException {

        AgentServicePool.getInstance().getClientForHost(atsAgent).logDuplicatedJars();
    }

    @Override
    public int getNumberPendingLogEvents() throws AgentException {

        try {
            return AgentServicePool.getInstance().getClientForHost(atsAgent).getNumberPendingLogEvents();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void restore( String componentName, String environmentName,
                         String folderPath ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            agentServicePort.restoreEnvironment(componentName, environmentName, folderPath);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void restoreAll( String environmentName ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            //passing null will clean all components
            agentServicePort.restoreEnvironment(null, environmentName, null);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void backup( String componentName, String environmentName,
                        String folderPath ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            agentServicePort.backupEnvironment(componentName, environmentName, folderPath);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void backupAll( String environmentName ) throws AgentException {

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgent);

        try {
            //passing null will backup all components
            agentServicePort.backupEnvironment(null, environmentName, null);
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    @Override
    public void waitUntilQueueFinish() throws AgentException {

        /*
         * In real environment, this method is most likely never used
         * as this remote client is used for running a single action, so
         * no performance queues are available.
         *
         * TODO: maybe we can safely throw some error here to say that
         * it is not expected to call this method.
         * Or we can implement an empty body here or in the abstract parent.
         */

        //get the client
        AgentService agentServicePort = AgentServicePool.getInstance().getClientForHost(atsAgentSessionId);

        try {
            log.info("Waiting until all queues on host '" + atsAgent + "' finish execution");

            agentServicePort.waitUntilAllQueuesFinish();
        } catch (AgentException_Exception ae) {
            throw new AgentException(ae.getMessage());
        } catch (InternalComponentException_Exception ice) {
            throw new AgentException(ice.getMessage() + ", check server log for stack trace");
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    /**
     * Wrap the action request into an ActionWrapper so it is easily
     * passed to the web service
     *
     * @param actionRequest the action request to wrap
     * @return the action wrapper
     * @throws AgentException on error
     */
    protected final ActionWrapper wrapActionRequest( ActionRequest actionRequest ) throws AgentException {

        Object[] arguments = actionRequest.getArguments();

        List<ArgumentWrapper> wrappedArguments = new ArrayList<ArgumentWrapper>();

        try {
            //wrap the arguments - each argument is serialized as
            //a byte stream
            for (Object argument : arguments) {
                ArgumentWrapper argWrapper = new ArgumentWrapper();

                ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
                objectOutStream.writeObject(argument);

                argWrapper.setArgumentValue(byteOutStream.toByteArray());
                wrappedArguments.add(argWrapper);
            }
        } catch (IOException ioe) {
            throw new AgentException("Could not serialize input arguments", ioe);
        }

        //construct the action wrapper
        ActionWrapper actionWrapper = new ActionWrapper();
        actionWrapper.setComponentName(actionRequest.getComponentName());
        actionWrapper.setActionName(actionRequest.getActionName());
        actionWrapper.getArgs().addAll(wrappedArguments);

        return actionWrapper;
    }
}
