#!/bin/bash

mvn release:clean -Darguments="-DskipTests=true -Dgpg.skip=false"
mvn release:prepare -Darguments="-DskipTests=true -Dgpg.skip=false" 
mvn release:perform -Darguments="-DskipTests=true -Dgpg.skip=false"

