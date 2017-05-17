#!/bin/sh
# Make sure you have in PATH locations to bin directories of JDK and Maven

# Credentials needed
# These credentials are needed to resolve Oracle JDBC driver. Oracle Maven repository is protected: https://maven.oracle.com
if [ -z "$MAVEN_ORACLE_USERNAME" ]; then
   echo "Please set environment variables MAVEN_ORACLE_USERNAME and MAVEN_ORACLE_PASSWORD for access to Oracle Maven repository( https://maven.oracle.com). This is needed in order to retrieve the Oracle JDBC driver"
   return 2
fi

if [ -z "$MAVEN_ORACLE_PASSWORD" ]; then
   echo "Please set environment variable MAVEN_ORACLE_PASSWORD for access to Oracle Maven repository( https://maven.oracle.com). This is needed in order to retrieve the Oracle JDBC driver"
   return 3
fi

# get the absolute path to the script, e.g. /home/user/git/ats-framework/
SCRIPTPATH=`cd "\`dirname \"$0\"\`" && pwd`


# Build and install artifacts locally
mvn -V -s $SCRIPTPATH/settings.xml clean install $*
# For other goals you may comment above line and uncomment next one
#mvn -V -s $SCRIPTPATH/settings.xml $*
