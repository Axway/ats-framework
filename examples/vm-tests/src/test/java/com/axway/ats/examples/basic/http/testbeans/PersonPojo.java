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
package com.axway.ats.examples.basic.http.testbeans;

/**
 * Simple java bean - POJO (Plain Old Java Object)
 *
 * Used for serialization/deserialization over the network when sending/receiving data
 *
 * NOTE: This class must be kept same as the one in the ats-all-in-one-test-rest-server
 * We have not made a shared project to avoid this
 */
public class PersonPojo {

    private int id;

    private String firstName;

    private String lastName;

    private int age;

    public PersonPojo() {

    }

    public int getId() {

        return id;
    }

    public void setId(
            int id ) {

        this.id = id;
    }

    public String getFirstName() {

        return firstName;
    }

    public void setFirstName(
            String firstName ) {

        this.firstName = firstName;
    }

    public String getLastName() {

        return lastName;
    }

    public void setLastName(
            String lastName ) {

        this.lastName = lastName;
    }

    public int getAge() {

        return age;
    }

    public void setAge(
            int age ) {

        this.age = age;
    }

    @Override
    public String toString() {

        return "Person with name " + firstName + " " + lastName + " at " + age + " years, id " + id;
    }

    ;
}
