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
package com.axway.ats.action.mail.model;

import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;

/**
 * Capture java mail message delivery events
 */
public class MailTransportListener implements TransportListener {

    private final Object   messageSendingMutex;

    private DELIVERY_STATE deliveryState;

    public enum DELIVERY_STATE {
        PARTIALLY_DELIVERED("partially delivered"), DELIVERED("delivered"), ERROR_DELIVERING(
                "error delivering");

        private final String value;

        DELIVERY_STATE( String value ) {

            this.value = value;
        }

        @Override
        public String toString() {

            return value;
        }
    }

    public MailTransportListener( Object messageSendingMutex ) {

        this.deliveryState = DELIVERY_STATE.PARTIALLY_DELIVERED;
        this.messageSendingMutex = messageSendingMutex;
    }

    public void messageDelivered(
                                  TransportEvent e ) {

        processResult( DELIVERY_STATE.DELIVERED );
    }

    public void messageNotDelivered(
                                     TransportEvent e ) {

        processResult( DELIVERY_STATE.ERROR_DELIVERING );
    }

    public void messagePartiallyDelivered(
                                           TransportEvent e ) {

        processResult( DELIVERY_STATE.ERROR_DELIVERING );
    }

    private void processResult(
                                DELIVERY_STATE deliveryState ) {

        // remember the delivery state, the mail sender thread will ask about this state
        this.deliveryState = deliveryState;
        //log.debug( deliveryState );

        synchronized( messageSendingMutex ) {
            // inform the mail sender thread we are done with mail sending
            messageSendingMutex.notifyAll();
        }
    }

    public DELIVERY_STATE getDeliveryState() {

        return deliveryState;
    }
}
