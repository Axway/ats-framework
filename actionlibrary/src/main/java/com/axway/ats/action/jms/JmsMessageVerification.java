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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import com.axway.ats.action.exceptions.JmsMessageException;
import com.axway.ats.common.PublicAtsApi;

/**
 * A utility class used to verify the content of a message received with the JMS client
 */
@PublicAtsApi
public class JmsMessageVerification {

    public static final String JMS_CORRELATION_ID = "JMSCorrelationID";
    public static final String JMS_DELIVERY_MODE  = "JMSDeliveryMode";
    public static final String JMS_EXPIRATION     = "JMSExpiration";
    public static final String JMS_PRIORITY       = "JMSPriority";
    public static final String JMS_REDELIVERED    = "JMSRedelivered";
    public static final String JMS_REPLY_TO       = "JMSReplyTo";
    public static final String JMS_TIMESTAMP      = "JMSTimestamp";
    public static final String JMS_TYPE           = "JMSType";
    public static final String JMS_DESTINATION    = "JMSDestination";
    public static final String JMS_MESSAGE_ID     = "JMSMessageId";

    /**
     * Provides an instance of this class. 
     * This methods is used instead of a constructor
     * 
     * @param message the message to work with
     * @return instance of this class
     */
    @PublicAtsApi
    public static JmsMessageVerification verifyThat(
                                                     final Message message ) {

        return new JmsMessageVerification( message );
    }

    private final Message actualMessage;

    private JmsMessageVerification( final Message message ) {

        this.actualMessage = message;
    }

    private JmsMessageVerification isInstanceof(
                                                 final Class<?> c ) {

        isNotNull();
        if( c.isInstance( actualMessage ) ) {
            return this;
        }
        throw new JmsMessageException( "Message not instance of " + c.getSimpleName() );
    }

    private Object findProperty(
                                 final String key ) {

        Exception jmsException = null;
        try {
            final Object o = actualMessage.getObjectProperty( key );
            if( o != null ) {
                return o;
            }
        } catch( final Exception e ) {
            jmsException = e;
        }

        try {
            if( key.equals( JMS_DESTINATION ) ) {
                return actualMessage.getJMSDestination();
            }
            if( key.equals( JMS_MESSAGE_ID ) ) {
                return actualMessage.getJMSMessageID();
            }
            // might be enhanced to support different JMS providers
        } catch( Exception e ) {
            if( jmsException == null ) {
                jmsException = e;
            }
        }

        if( jmsException != null ) {
            throw new JmsMessageException( "Failed to fetch property " + key, jmsException );
        }
        return null;
    }

    /**
     * Check the message is NULL
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isNull() {

        if( actualMessage == null ) {
            return this;
        }
        throw new JmsMessageException( "Message is not null" );
    }

    /**
     * Check the message is NOT NULL
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isNotNull() {

        if( actualMessage != null ) {
            return this;
        }
        throw new JmsMessageException( "Message is null" );
    }

    /**
     * It is a TEXT message
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isTextMessage() {

        return isInstanceof( TextMessage.class );
    }

    /**
     * It is a BYTES message
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isBytesMessage() {

        return isInstanceof( BytesMessage.class );
    }

    /**
     * It is a STREAM message
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isStreamMessage() {

        return isInstanceof( StreamMessage.class );
    }

    /**
     * It is a MAP message
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isMapMessage() {

        return isInstanceof( MapMessage.class );
    }

    /**
     * It is an OBJECT message
     * 
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification isObjectMessage() {

        return isInstanceof( ObjectMessage.class );
    }

    /**
     * Contains the provided text
     * 
     * @param expectedText the expected text
     * @return
     * @throws JMSException
     */
    @PublicAtsApi
    public JmsMessageVerification hasText(
                                           final String expectedText ) throws JMSException {

        isTextMessage();

        final String actualText = ( ( TextMessage ) actualMessage ).getText();
        if( !expectedText.equals( actualText ) ) {
            assertEqual( "message text", expectedText, actualText );
        }
        return this;
    }

    /**
     * Has a property with specified name and at lease one of the values
     * 
     * @param name property name
     * @param values property value(s)
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification hasProperty(
                                               final String name,
                                               final Object... values ) {

        isNotNull();
        final Object o = findProperty( name );
        if( o == null ) {
            throw new JmsMessageException( "Property '" + name + "' not found" );
        }
        if( ( values == null ) || ( values.length == 0 ) ) {
            return this;
        }
        for( final Object value : values ) {
            if( o.equals( value ) ) {
                return this;
            }
        }

        if( values.length == 1 ) {
            throw new JmsMessageException( "Invalid value of property '" + name + "': expected '" + values[0]
                                           + "', found '" + o + "'" );
        } else {
            throw new JmsMessageException( "Invalid value of property '" + name + "': '" + o + "' not in "
                                           + Arrays.asList( values ) + "" );
        }
    }

    /**
     * Has all properties with specified name and value 
     * 
     * @param properties the expected properties
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification hasProperties(
                                                 Map<String, ?> properties ) {

        isNotNull();
        for( final Entry<String, ?> entry : properties.entrySet() ) {
            final String key = entry.getKey();
            final Object actualObject = findProperty( key );
            if( actualObject == null ) {
                throw new JmsMessageException( "Property '" + key + "' not found" );
            }

            Object expectedObject = entry.getValue();
            if( !expectedObject.equals( actualObject ) ) {
                throw new JmsMessageException( "Invalid value for property '" + key + "'" );
            }
        }
        return this;
    }

    /**
     * Includes all properties defined by their names
     * 
     * @param names property names
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification includesProperties(
                                                      final String... names ) {

        isNotNull();
        for( final String key : names ) {
            final Object o = findProperty( key );
            if( o == null ) {
                throw new JmsMessageException( "Property '" + key + "' not found" );
            }
        }
        return this;
    }

    /**
     * Does not include properties defined by their names
     * 
     * @param names property names
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification excludesProperties(
                                                      final String... names ) {

        isNotNull();
        if( names != null ) {
            for( final String key : names ) {
                if( findProperty( key ) != null ) {
                    throw new JmsMessageException( "Property '" + key + "' found" );
                }
            }
        }
        return this;
    }

    private byte[] getBodyHash(
                                final String algorithm ) {

        isNotNull();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance( algorithm );
        } catch( final NoSuchAlgorithmException e ) {
            throw new JmsMessageException( "Failed to load " + algorithm + " algorithm: " + e );
        }

        try {
            if( actualMessage instanceof TextMessage ) {
                digest.update( ( ( TextMessage ) actualMessage ).getText().getBytes() );
            } else if( actualMessage instanceof BytesMessage ) {
                final BytesMessage m = ( BytesMessage ) actualMessage;
                final byte[] tmp = new byte[2048];
                int r;
                while( ( r = m.readBytes( tmp ) ) >= 0 ) {
                    if( r != 0 ) {
                        digest.update( tmp, 0, r );
                    }
                }
            } else if( actualMessage instanceof StreamMessage ) {
                final StreamMessage m = ( StreamMessage ) actualMessage;
                final byte[] tmp = new byte[2048];
                int r;
                while( ( r = m.readBytes( tmp ) ) >= 0 ) {
                    if( r != 0 ) {
                        digest.update( tmp, 0, r );
                    }
                }
            } else {
                throw new JmsMessageException( "Cannot determind content hash for message type : "
                                               + actualMessage.getClass() );
            }
        } catch( final JMSException e ) {
            throw new JmsMessageException( "Failed to determine message " + algorithm + " hash", e );
        }
        return digest.digest();
    }

    /**
     * @return the body MD5 sum
     */
    @PublicAtsApi
    public byte[] getBodyMD5() {

        return getBodyHash( "MD5" );
    }

    /**
     * Has body MD5 sum
     * 
     * @param expectedDigest expected MD5 sum
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification hasBodyMD5(
                                              byte[] expectedDigest ) {

        assertNotNull( "MD5 parameter", expectedDigest );
        final byte[] result = getBodyHash( "MD5" );
        assertArrayEquals( "MD5 content differs", expectedDigest, result );
        return this;
    }

    /**
     * Has body MD5 sum
     * 
     * @param expectedHexValue expected MD5 sum
     * @return
     */
    @PublicAtsApi
    public JmsMessageVerification hasBodyMD5(
                                              final String expectedHexValue ) {

        assertNotNull( "MD5 string parameter", expectedHexValue );
        final int len = expectedHexValue.length();
        final byte[] data = new byte[len / 2];
        for( int i = 0; i < len; i += 2 ) {
            data[i / 2] = ( byte ) ( ( Character.digit( expectedHexValue.charAt( i ), 16 ) << 4 ) + Character.digit( expectedHexValue.charAt( i + 1 ),
                                                                                                                     16 ) );
        }
        return hasBodyMD5( data );
    }

    private void assertEqual(
                              String description,
                              String expected,
                              String actual ) {

        if( !expected.equals( actual ) ) {
            throw new JmsMessageException( "Expected " + description + " is '" + expected + "', but got '"
                                           + actual + "'" );
        }
    }

    private void assertNotNull(
                                String userMessage,
                                Object expectedDigest ) {

        if( expectedDigest == null ) {
            throw new JmsMessageException( userMessage + " is NULL" );
        }
    }

    private void assertArrayEquals(
                                    String userMessage,
                                    byte[] actualArray,
                                    byte[] expectedArray ) {

        if( !Arrays.equals( actualArray, expectedArray ) ) {
            throw new JmsMessageException( userMessage );
        }
    }
}
