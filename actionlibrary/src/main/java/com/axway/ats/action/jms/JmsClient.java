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
package com.axway.ats.action.jms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.action.exceptions.JmsMessageException;
import com.axway.ats.action.jms.model.connections.ManagedConnection;
import com.axway.ats.action.jms.model.sessions.ManagedSession;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.core.utils.StringUtils;

/**
 * A JMS client class
 *
 */
@PublicAtsApi
public class JmsClient {

    private static final Logger           log         = LogManager.getLogger(JmsClient.class);

    private static InitialContext         defaultStaticInitialcontext;
    private static String                 defaultStaticConnectionFactoryName;
    private InitialContext                context;
    private String                        defaultConnectionFactoryName;
    private String                        brokerURI;

    // single embedded usage
    private ManagedConnection             connection;
    private Session                       session;
    // JMS managed usage
    private final List<ManagedConnection> connections = new ArrayList<ManagedConnection>();

    // whether we will close the session on atomic user operation
    private boolean                       isKeepAlive = false;

    // Basic authentication info
    private String                        username;
    private String                        password;

    // list of topics we listen to
    private Map<String, TopicInfo>        topics      = new HashMap<String, TopicInfo>();

    public static void setDefaultStaticInitialContext( final InitialContext context ) {

        defaultStaticInitialcontext = context;
    }

    public static void setDefaultStaticConnectionFactoryName( final String factoryName ) {

        defaultStaticConnectionFactoryName = factoryName;
    }

    public JmsClient( final InitialContext context, final String defaultConnectionFactoryName,
                      final String brokerURI ) throws NamingException {

        this.context = new InitialContext(context.getEnvironment());
        this.defaultConnectionFactoryName = defaultConnectionFactoryName;
        this.brokerURI = brokerURI;
        if (this.brokerURI != null) {
            this.context.addToEnvironment(Context.PROVIDER_URL, this.brokerURI);
        }
    }

    public JmsClient( final String brokerURI ) throws NamingException {

        this(defaultStaticInitialcontext, defaultStaticConnectionFactoryName, brokerURI);
    }

    /**
     * Basic constructor
     *
     * @param context the initial context
     * @param defaultConnectionFactoryName default connection factory name
     * @throws NamingException
     */
    @PublicAtsApi
    public JmsClient( final InitialContext context,
                      final String defaultConnectionFactoryName ) throws NamingException {

        this(context, defaultConnectionFactoryName, null);
    }

    public JmsClient() throws NamingException {

        this(defaultStaticInitialcontext, defaultStaticConnectionFactoryName);
    }

    /**
     * Use basic authentication
     *
     * @param username the user name
     * @param password the user password
     */
    @PublicAtsApi
    public void setBasicAuthentication( String username, String password ) {

        this.username = username;
        this.password = password;
    }

    /**
     * This option must be used in cases where we must not close the session after executing some of the
     * available public methods.
     *
     * For example, if you subscribe to listen for topic messages and then send a message, the last step will
     * close the session which kills the listening process. In such case the session must not be closed.
     * @param isKeepAlive
     */
    @PublicAtsApi
    public void setKeepAlive( boolean isKeepAlive ) {

        this.isKeepAlive = isKeepAlive;
    }

    // pure JMS management
    private ConnectionFactory getConnectionFactory() {

        final Object connectionFactoryObject;
        try {
            connectionFactoryObject = context.lookup(defaultConnectionFactoryName);
            return (ConnectionFactory) connectionFactoryObject;
        } catch (Exception e) {
            throw new JmsMessageException("Error loading JMS connection factory '"
                                          + defaultConnectionFactoryName + "'", e);
        }
    }

    private ManagedConnection createManagedConnection() {

        final ManagedConnection connection;
        final ConnectionFactory connectionFactory = getConnectionFactory();
        try {
            if (!StringUtils.isNullOrEmpty(this.username)) {
                connection = ManagedConnection.create(connectionFactory.createConnection(this.username,
                                                                                         this.password));
            } else {
                connection = ManagedConnection.create(connectionFactory.createConnection());
            }
        } catch (JMSException e) {
            throw new JmsMessageException("Error creating JMS connection from connection factory '"
                                          + defaultConnectionFactoryName + "'", e);
        }
        return connection;
    }

    private synchronized ManagedConnection doConnect() {

        releaseConnection();
        final ManagedConnection connection = createManagedConnection();
        try {
            connection.start();
        } catch (JMSException e) {
            connection.shutdown();
            throw new JmsMessageException("Error starting JMS connection from connection factory '"
                                          + defaultConnectionFactoryName + "'", e);
        }
        this.connection = connection;
        return connection;
    }

    /**
     * Shutdown the JMS client.
     * This closes all connections and sessions.
     */
    @PublicAtsApi
    public synchronized void shutdown() {

        releaseSession(true);

        releaseConnection();

        for (final ManagedConnection connection : connections) {
            connection.shutdown();
        }
        connections.clear();
    }

    private synchronized void releaseConnection() {

        if (connection == null) {
            return;
        }

        connection.shutdown();
        connection = null;
        session = null;
    }

    private synchronized ManagedConnection loadConnection() {

        final ManagedConnection c = connection;
        if (c != null) {
            return c;
        }

        return doConnect();
    }

    private synchronized Session loadSession( final boolean transacted,
                                              final int acknowledgeMode ) throws JMSException {

        final Session s = session;
        if (s != null) {
            if ( (s.getTransacted() == transacted) && (s.getAcknowledgeMode() == acknowledgeMode)) {
                return s;
            }
            s.close();
        }
        final Session newSession = loadConnection().createSession(transacted, acknowledgeMode);
        session = newSession;
        return newSession;
    }

    private synchronized void releaseSession( boolean closeEvenIfKeepAlive ) {

        if (session == null) {
            return;
        }

        if (isKeepAlive && !closeEvenIfKeepAlive) {
            return;
        }

        ((ManagedSession) session).shutdown();
        session = null;
    }

    private Destination getDestination( final String resourceName ) {

        Object destination;
        try {
            destination = context.lookup(resourceName);
        } catch (NamingException e) {
            throw new JmsMessageException("Error retrieving destination '" + resourceName + "'", e);
        }
        if (! (destination instanceof Destination)) {
            throw new JmsMessageException("Error retrieving resource '" + resourceName
                                          + "': invalide destination type " + (destination == null
                                                                                                   ? null
                                                                                                   : destination.getClass()));
        }

        return (Destination) destination;
    }

    /**
     * Send a text message
     *
     * @param jndiName
     * @param textMessage message content
     * @param properties message properties or null if none
     */
    @PublicAtsApi
    public void sendTextMessage( final String jndiName, final String textMessage,
                                 final Map<String, ?> properties ) {

        sendTextMessage(getDestination(jndiName), textMessage, properties);
    }

    /**
     * Send a text message
     *
     * @param destination message destination
     * @param textMessage message content
     * @param properties message properties or null if none
     */
    @PublicAtsApi
    public void sendTextMessage( final Destination destination, final String textMessage,
                                 final Map<String, ?> properties ) {

        sendTextMessage(loadConnection(), destination, textMessage, properties);
    }

    /**
     * Send a text message to a queue
     *
     * @param queueName queue name
     * @param textMessage message content
     * @param properties message properties or null if none
     */
    @PublicAtsApi
    public void sendTextMessageToQueue( final String queueName, final String textMessage,
                                        final Map<String, ?> properties ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            doSendTextMessage(session, session.createQueue(queueName), textMessage, properties);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to send text message to queue " + queueName, e);
        }
    }

    /**
     * Send a text message to a topic
     *
     * @param topicName topic name
     * @param textMessage message content
     * @param properties message properties or null if none
     */
    @PublicAtsApi
    public void sendTextMessageToTopic( final String topicName, final String textMessage,
                                        final Map<String, ?> properties ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            doSendTextMessage(session, session.createTopic(topicName), textMessage, properties);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to send text message to topic " + topicName, e);
        }
    }

    private void sendTextMessage( final Connection connection, final Destination destination,
                                  final String textMessage, final Map<String, ?> properties ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            doSendTextMessage(session, destination, textMessage, properties);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to send message", e);
        }
    }

    // Send and close session
    private void doSendTextMessage( final Session session, final Destination destination,
                                    final String textMessage,
                                    final Map<String, ?> properties ) throws JMSException {

        try {
            final Message message = textMessage != null
                                                        ? session.createTextMessage(textMessage)
                                                        : session.createTextMessage();
            if (properties != null) {
                // Note: Setting any properties (including JMS fields) using
                // setObjectProperty might not be supported by all providers
                // Tested with: ActiveMQ
                for (final Entry<String, ?> property : properties.entrySet()) {
                    message.setObjectProperty(property.getKey(), property.getValue());
                }
            }
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
        } finally {
            releaseSession(false);
        }
    }

    /**
     * Send a binary message to a queue
     *
     * @param queueName queue name
     * @param bytes message content
     * @param properties message properties or null if none
     */
    @PublicAtsApi
    public void sendBinaryMessageToQueue( final String queueName, final byte[] bytes,
                                          final Map<String, ?> properties ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            doSendBinaryMessage(session, session.createQueue(queueName), bytes, properties);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to send binary message to queue " + queueName, e);
        }
    }

    // Send and close session
    private void doSendBinaryMessage( final Session session, final Destination destination,
                                      final byte[] bytes,
                                      final Map<String, ?> properties ) throws JMSException {

        try {
            BytesMessage message = session.createBytesMessage();
            message.writeBytes(bytes);
            if (properties != null) {
                // Note: Setting any properties (including JMS fields) using
                // setObjectProperty might not be supported by all providers
                // Tested with: ActiveMQ
                for (final Entry<String, ?> property : properties.entrySet()) {
                    message.setObjectProperty(property.getKey(), property.getValue());
                }
            }
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
        } finally {
            releaseSession(false);
        }
    }

    /**
     * Receive a message
     *
     * @param jndiName
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessage( final String jndiName ) {

        return receiveMessage(jndiName, 0);
    }

    /**
     * Receive a message
     *
     * @param jndiName
     * @param timeout timeout period in milliseconds
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessage( final String jndiName, final long timeout ) {

        return receiveMessage(getDestination(jndiName), timeout);
    }

    /**
     * Receive a message
     *
     * @param destination remote destination
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessage( final Destination destination ) {

        return receiveMessage(destination, 0);
    }

    /**
     * Receive a message
     *
     * @param destination remote destination
     * @param timeout timeout period in milliseconds
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessage( final Destination destination, final long timeout ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            return doReceiveMessage(session, destination, timeout);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to read message from " + destination, e);
        }
    }

    /**
     * Receive a message from queue. Waits indefinitely.
     *
     * @param queueName queue name
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessageFromQueue( final String queueName ) {

        return receiveMessageFromQueue(queueName, 0);
    }

    /**
     * Receive a message from queue for a period of time
     *
     * @param queueName queue name
     * @param timeout timeout period in milliseconds
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessageFromQueue( final String queueName, final long timeout ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            return doReceiveMessage(session, session.createQueue(queueName), timeout);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to read message message from queue " + queueName, e);
        }
    }

    /**
     * Receive a message from a topic. Waits indefinitely.
     *
     * @param topicName topic name
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessageFromTopic( final String topicName ) {

        return receiveMessageFromTopic(topicName, 0);
    }

    /**
     * Receive a message from topic for a period of time
     *
     * @param topicName topic name
     * @param timeout timeout period in milliseconds
     * @return the received message
     */
    @PublicAtsApi
    public Message receiveMessageFromTopic( final String topicName, final long timeout ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            return doReceiveMessage(session, session.createTopic(topicName), timeout);
        } catch (Exception e) {
            throw new JmsMessageException("Failed to read message message from topic " + topicName, e);
        }
    }

    // read and close session
    private Message doReceiveMessage( final Session session, final Destination destination,
                                      final long timeout ) throws JMSException {

        try {
            return session.createConsumer(destination).receive(timeout);
        } finally {
            releaseSession(false);
        }
    }

    /**
     * Read all messages from a queue
     *
     * @param queueName queue name
     * @throws JMSException
     */
    @PublicAtsApi
    public void cleanupMessagesQueue( final String queueName ) throws JMSException {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            doCleanupQueue(session, session.createQueue(queueName));

            log.info("Successfully cleanedup message queue " + queueName
                     + ". This is done by reading all existing messages.");
        } catch (Exception e) {
            throw new JmsMessageException("Failed to cleanup message queue " + queueName, e);
        }
    }

    private void doCleanupQueue( final Session session, final Destination destination ) throws JMSException {

        try {
            MessageConsumer consumer = session.createConsumer(destination);
            Message message = null;
            do {
                message = consumer.receiveNoWait();
                if (message != null) {
                    message.acknowledge();
                }
            } while (message != null);
        } finally {
            releaseSession(false);
        }
    }

    /**
     * Create a topic
     *
     * @param topicName the topic name
     */
    @PublicAtsApi
    public void createTopic(

                             final String topicName ) {

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            final Topic topic = session.createTopic(topicName);
            session.createConsumer(topic).close();
        } catch (JMSException e) {
            throw new JmsMessageException("Could not start listening for messages on topic " + topicName,
                                          e);
        } finally {
            releaseSession(false);
        }
    }

    /**
     * Start listening for messages on topic
     *
     * @param topicName the topic name
     */
    @PublicAtsApi
    public void startListeningToTopic(

                                       final String topicName ) {

        final TopicInfo topicInfo = getTopicInfo(topicName);
        if (topicInfo.isListening()) {
            throw new JmsMessageException("We are already listening for messages on topic " + topicName);
        }

        try {
            final Session session = loadSession(false, Session.AUTO_ACKNOWLEDGE);
            final Topic topic = session.createTopic(topicName);
            topicInfo.topicConsumer = session.createConsumer(topic);
            topicInfo.topicConsumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage( Message message ) {

                    topicInfo.addMessage(message);
                }
            });
        } catch (JMSException e) {
            throw new JmsMessageException("Could not start listening for messages on topic " + topicName,
                                          e);
        }
    }

    /**
     * Stop listening for messages on topic
     *
     * @param topicName the topic name
     */
    @PublicAtsApi
    public void stopListeningToTopic( final String topicName ) {

        TopicInfo topicInfo = getTopicInfo(topicName);
        if (!topicInfo.isListening()) {
            throw new JmsMessageException("We are not listening for messages on topic " + topicName);
        }

        try {
            topicInfo.topicConsumer.close();
        } catch (JMSException e) {
            throw new JmsMessageException("Could not stop listening for messages on topic " + topicName, e);
        } finally {
            topicInfo.topicConsumer = null;
        }
    }

    /**
     * Get the current list of collected messages on this topic.
     *
     * @param topicName the topic name
     * @return the messages.
     */
    @PublicAtsApi
    public Message[] getCollectedTopicMessages( final String topicName ) {

        return getTopicInfo(topicName).getMessages();
    }

    /**
     * A simple structure containing all needed info to listen on topics
     */
    private class TopicInfo {
        private List<Message>   topicMessages = new ArrayList<Message>();
        private MessageConsumer topicConsumer = null;

        boolean isListening() {

            return topicConsumer != null;
        }

        synchronized void addMessage( Message message ) {

            topicMessages.add(message);
        }

        synchronized Message[] getMessages() {

            final Message[] messagesArray = topicMessages.toArray(new Message[topicMessages.size()]);
            topicMessages.clear();
            return messagesArray;
        }
    }

    private TopicInfo getTopicInfo( final String topicName ) {

        TopicInfo topic = topics.get(topicName);
        if (topic == null) {
            topic = new TopicInfo();
            topics.put(topicName, topic);
        }
        return topic;
    }
}
