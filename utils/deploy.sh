#!/bin/bash

mvn release:prepare
mvn clean install deploy -DskipTests=true -Dgpg.skip=false
mvn release:perform
