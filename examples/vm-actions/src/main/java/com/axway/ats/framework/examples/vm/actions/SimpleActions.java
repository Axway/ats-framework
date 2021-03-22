/*
 * Copyright 2018-2019 Axway Software
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
package com.axway.ats.framework.examples.vm.actions;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A basic ATS Action
 *
 * Creating actions is presented at:
 *      https://axway.github.io/ats-framework/Creating-ATS-actions.html
 *
 * This class is the one presented at:
 *      https://axway.github.io/ats-framework/Creating-an-Agent-Component.html
 *
 * Note that our action classes can have as many as needed action methods
 * which will do whatever code you've put in them.
 */
public class SimpleActions {

    // we can use this logger in order to log some messages
    private static final Logger log = LogManager.getLogger(SimpleActions.class);

    // Used to demonstrate how can call many actions and they can all operated on same data(session data alike)
    private Person person;

    /**
     * A simple action which receives some input parameters and returns back some output.
     *
     * This action can run any java code you want.
     *
     * @param myName
     * @param myAge
     * @return
     */
    @Action()
    public String sayHi( @Parameter( name = "myName" ) String myName, @Parameter( name = "myAge" ) int myAge ) {

        // we log some message from this action
        log.info("Remote action message: Called by '" + myName + "' who is " + myAge + " years old");

        // this action returns some data back
        return "Hello " + myName + ". You are " + myAge + " years old";
    }

    /**
     * Create the session data - we create a person entity using the provided information
     *
     * @param firstName
     * @param lastName
     * @param age
     */
    @Action()
    public void createPerson( @Parameter( name = "firstName" ) String firstName,
                              @Parameter( name = "lastName" ) String lastName,
                              @Parameter( name = "age" ) int age ) {

        person = new Person(firstName, lastName, age);

        // inform about the happy event in the Agent log file
        log.info("Created a person " + person.toString());
    }

    /**
     * Change the session data created by previous action call
     */
    @Action()
    public void registerBirthdayTick() {

        person.registerBirthdayTick();
    }

    /**
     * @return some session data
     */
    @Action()
    public int getAge() {

        return person.getAge();
    }

    class Person {
        private String firstName;
        private String lastName;
        private int    age;

        Person( String firstName, String lastName, int age ) {

            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        void registerBirthdayTick() {

            this.age++;

            log.info("It is birthday time for " + age + "th time!");
        }

        int getAge() {

            return this.age;
        }

        @Override
        public String toString() {

            return firstName + " " + lastName + " at the age of " + age;
        }
    }
}
