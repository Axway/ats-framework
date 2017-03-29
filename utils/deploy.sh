#!/bin/bash

mvn release:clean -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"
mvn release:prepare -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false" 
mvn release:perform -DautoVersionSubmodules=true -Darguments="-DskipTests=true -Dgpg.skip=false"

