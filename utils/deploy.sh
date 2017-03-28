#!/bin/bash

mvn release:prepare -DautoVersionSubmodules=true
mvn clean install deploy -DskipTests=true -Dgpg.skip=false
mvn release:perform
