@ECHO OFF
REM Make sure you have in PATH locations to bin directories of JDK and Maven

REM Credentials to resolve Oracle JDBC driver
REM Oracle Maven repository is protected
if not "%MAVEN_ORACLE_USERNAME%" == "" goto userNameIsSet
ECHO Please set environment variables MAVEN_ORACLE_USERNAME and MAVEN_ORACLE_PASSWORD for access to Oracle Maven repository( https://maven.oracle.com). This is needed in order to retrieve the Oracle JDBC driver
exit /b 2

:userNameIsSet
if not "%MAVEN_ORACLE_PASSWORD%" == "" goto envIsSet
ECHO Please set environment variable MAVEN_ORACLE_PASSWORD for access to Oracle Maven repository.
exit /b 3

:envIsSet
SET ATS_PROJECT_HOME=%~dp0

REM Build and install artifacts locally
mvn -V -s %ATS_PROJECT_HOME%\settings.xml clean install %*
REM For other goals you may comment above line and uncomment next one
REM mvn -V -s settings.xml %*