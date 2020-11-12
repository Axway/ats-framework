@ECHO OFF
:: AGENT_HOME must be stored here, "%~dp0 works correctly only when called before @SETLOCAL" or SHIFT being invoked. 
:: About ~dp0: Expand script being invoked variable (%0 ) to drive and path.
@SET AGENT_HOME=%~dp0
@SETLOCAL EnableDelayedExpansion

:: (Optional) Java and JVM runtime options

::Example -> set JAVA_OPTS=%JAVA_OPTS% -Dmy.prop1=abc -Dmy.prop2=cba

::Example -> set JAVA_OPTS=%JAVA_OPTS% "-Dmy.prop1=abc" "-Dmy.prop2=cba"

::Example -> set JAVA_OPTS=%JAVA_OPTS% -Dmy.prop1="abc" -Dmy.prop2="cba"

::Example added through CMD -> agent.bat start -java_opts "-Dprop1=abc"

::Example added through CMD -> agent.bat start -java_opts "-Dprop1=abc" -java_opts "-Dprop2=cba"
@SET JAVA_OPTS=%JAVA_OPTS%

:: the Agent port
@SET PORT=8089
:: the java executable
@SET JAVA_EXEC=java

:: folder containing the Agent actions
@SET COMPONENTS_FOLDER=ats-agent/actions
:: folder containing the Agent Template Actions
@SET TEMPLATE_ACTIONS_FOLDER=ats-agent/templateActions
:: set agent logging severity level
@SET LOGGING_SEVERITY=INFO
:: the amount of memory used by the service (in MB)
@SET MEMORY=256

:: allow remote connections for debug purposes
@SET DEBUG=0
@SET DEBUG_PORT=8000
REM Full DEBUG_OPTIONS line is constructed right before JVM start since it is dependent on the JRE version

:: enable monitoring the number of pending log events (true/false)
@SET MONITOR_EVENTS_QUEUE=false

:: allow JMX connections (0/1)
@SET JMX=0
@SET JMX_PORT=1099
@SET JMX_OPTIONS=
@IF %JMX% EQU 1 (
    SET JMX_OPTIONS=-Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false
)

:: set logging pattern
@SET LOGGING_PATTERN=
:: keep first argument to the script - start stop or so
@SET COMMAND=%1%

:: Parse the input arguments
:args_loop
IF NOT "%1"=="" (
    IF "%1"=="-port" (
        REM set port
        SET PORT=%2
        SHIFT
    )
    IF "%1"=="-java_exec" (
        REM set java executable
        SET JAVA_EXEC=%2
        SHIFT
    )
    IF "%1"=="-memory" (
        REM set memory
        SET MEMORY=%2
        SHIFT
    )
    IF "%1"=="-logging_pattern" (
        REM set logging pattern
        SET LOGGING_PATTERN=%2
        SHIFT
    )
    IF "%1"=="-logging_severity" (
        REM set logging severity
        SET LOGGING_SEVERITY=%2
        SHIFT
    )
    IF "%1"=="-java_opts" (
        REM set java opts
        SET "JAVA_OPTS=%JAVA_OPTS% %2"
        SHIFT
    )
    SHIFT
    GOTO :args_loop
)

@SET TITLE=ATS Agent on %PORT% port

REM Do not change next line without sync-ing with Agent-with-Java POM parts
cd /d %AGENT_HOME%

REM *************
REM *** Uncomment this and set correct paths if you will start Agent as a service
REM cd /d <AGENT_HOME_ABSOLUTE_PATH>
REM @SET PATH=<JAVA_HOME_DIR\bin>;%PATH%
REM *************

if "%COMMAND%"==""          GOTO:agent_start
if "%COMMAND%"=="start"     GOTO:agent_start
if "%COMMAND%"=="stop"      GOTO:agent_stop
if "%COMMAND%"=="restart"   GOTO:agent_restart
if "%COMMAND%"=="status"    GOTO:agent_status
if "%COMMAND%"=="version"   GOTO:agent_version

:: TODO print [-java_opts JAVA_OPTS] as a known command in the future
ECHO Usage:
ECHO    %~n0.bat start^|stop^|restart^|status^|version [-port PORT] [-java_exec JAVA_EXE_PATH] [-logging_pattern day^|hour^|minute^|30KB^|20MB^|10GB] [-memory MEMORY_IN_MB] [-logging_severity DEBUG^|INFO^|WARN^|ERROR]
GOTO:EOF




:agent_version
!JAVA_EXEC! -cp ats-agent/ats-agent-standalone-containerstarter.jar com.axway.ats.agentapp.standalone.utils.AtsVersionExtractor
GOTO:EOF




:agent_start

call :getStatusText STATUS_TEXT
if "!STATUS_TEXT!"=="AtsAgent is running" (
    echo AtsAgent is already started on port !PORT!
    GOTO:EOF
)
:: this check is made second time in case there is a process
:: listening on this port, but there is no PID file or running agent
call :checkIsPortBusy isPortBusy
if "!isPortBusy!"=="YES" (
    echo Port %PORT% is used by already started agent or another process ^^!
    GOTO:EOF
)

TITLE %TITLE%

rem check java version
!JAVA_EXEC! -version > java.version 2>&1

for /f "delims=" %%l in (java.version) do (
    set line=%%l
    del java.version
    goto next_java_version_check
)

SETLOCAL ENABLEDELAYEDEXPANSION
:next_java_version_check
echo %line% | FINDSTR /r 1\.[7-8]

IF %ERRORLEVEL%==0 (
    set JAVA_VERSION=8
) ELSE (
    set JAVA_VERSION=9
)

IF %JAVA_VERSION%==9 (
    REM Java 9 or newer detected

    REM Enable REMOTE debugging. By deault Java 9+ debugging is enabled only on localhost
    IF %DEBUG% EQU 1 (
        ECHO Enable remote debugging for Java 9+
        SET DEBUG_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,address=*:%DEBUG_PORT%,suspend=n
    )

    !JAVA_EXEC! -showversion ^
    -Dats.agent.default.port=!PORT! -Dats.agent.home="%AGENT_HOME:\=/%" ^
    %JMX_OPTIONS% ^
    -Dats.log.monitor.events.queue=%MONITOR_EVENTS_QUEUE% ^
    -Dats.agent.components.folder="%COMPONENTS_FOLDER%" -Dagent.template.actions.folder="%TEMPLATE_ACTIONS_FOLDER%" ^
    -Dlogging.severity="%LOGGING_SEVERITY%" ^
    -Xms!MEMORY!m -Xmx!MEMORY!m -Dlogging.pattern="!LOGGING_PATTERN!" ^
    %JAVA_OPTS% !DEBUG_OPTIONS! ^
    -jar ats-agent/ats-agent-standalone-containerstarter.jar

) ELSE (
    REM Java <= 8

    IF %DEBUG% EQU 1 (
        ECHO Enable remote debugging for Java 8
        SET DEBUG_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,address=%DEBUG_PORT%,suspend=n
    )

    !JAVA_EXEC! -showversion ^
    -Dats.agent.default.port=!PORT! -Dats.agent.home="%AGENT_HOME:\=/%" -Djava.endorsed.dirs=ats-agent/endorsed ^
    %JMX_OPTIONS% ^
    -Dats.log.monitor.events.queue=%MONITOR_EVENTS_QUEUE% ^
    -Dats.agent.components.folder="%COMPONENTS_FOLDER%" -Dagent.template.actions.folder="%TEMPLATE_ACTIONS_FOLDER%" ^
    -Dlogging.severity="%LOGGING_SEVERITY%" ^
    -Xms!MEMORY!m -Xmx!MEMORY!m -Dlogging.pattern="!LOGGING_PATTERN!" ^
    %JAVA_OPTS% !DEBUG_OPTIONS! ^
    -jar ats-agent/ats-agent-standalone-containerstarter.jar
)

ENDLOCAL
GOTO:EOF




:agent_stop

:: get the agent PID from the .pid file
call :getPid PID
if !PID! neq "" (
    :: get the CMD PID as a parent of the agent PID
    call :getParentPid CMD_PID

    :: stop the agent
    taskkill /f /t /PID %PID%

    :: delete the .pid file
    DEL "logs\atsAgent_%PORT%.pid"
)
if !CMD_PID! neq "" (
   :: close the CMD window, if still opened
   taskkill /f /t /PID %CMD_PID% >nul 2>&1
)

GOTO:EOF




:agent_restart

call :agent_stop
call :agent_start
GOTO:EOF




:agent_status

call :getStatusText STATUS_TEXT
echo !STATUS_TEXT!
GOTO:EOF




:checkIsPortBusy

:: check if there is started process on this port and is listening
SETLOCAL ENABLEEXTENSIONS
for /f "tokens=*" %%i in ('netstat -ano ^| findstr %PORT% ^| findstr "LISTENING"') do ( 
    if [%%i] neq [] (
        endlocal&set %1=YES&goto :eof
    )
)
endlocal&set %1=NO&goto :eof




:checkIfStarted

SETLOCAL ENABLEEXTENSIONS
:: the line to process, something like "PID:   1234"
@set Line=%*
:: check if the value the line starts with "PID:"
@set Value=!Line:~0,4!
if "!Value!"=="PID:" (
    endlocal&set agentStartedResult="YES"&goto :eof
)
endlocal&set agentStartedResult="NO"&goto :eof




:getPid

:: the pid file is created by the java process on start
SETLOCAL ENABLEEXTENSIONS
if exist logs/atsAgent_%PORT%.pid (
    FOR /F "tokens=*" %%i IN (logs/atsAgent_%PORT%.pid) DO (

        @if NOT !ERRORLEVEL! == 1 (
            REM echo Found PID "%%i"
            endlocal&set %1=%%i&goto :eof
        )
    )
)
::echo PID not found
endlocal&set %1=""&goto :eof




:getParentPid

:: the parent pid of the java process - the CMD window PID
@SETLOCAL ENABLEEXTENSIONS
FOR /F "tokens=*" %%i IN ('"wmic process where(processId="!PID!") get ParentProcessId /value"') DO (

     @SET "line=%%i"
     if "!line:ParentProcessId=!" NEQ "!line!" (
         @endlocal
         REM we need to set the 'line' value again(out of LOCAL)
         @SET "line=%%i"
         REM echo Found Parent PID !line:~16!
         @set %1=!line:~16!&goto :eof
     )
)
::echo parent PID not found
endlocal&set %1=""&goto :eof




:getStatusText

call :getPid PID
:: check the process by its PID and return the status text
@SETLOCAL ENABLEEXTENSIONS
if !PID! neq "" (
    for /F "delims=" %%a in ('tasklist /fo list /fi "pid eq !PID!"') do (

        set agentStartedResult=""
        REM pass this line
        call :checkIfStarted %%a
        if !agentStartedResult!=="YES" (
            call :checkIsPortBusy isPortBusy
            if "!isPortBusy!"=="YES" (
                endlocal&set %1=AtsAgent is running&goto :eof
                GOTO:EOF
            )
        ) 
    )
)
endlocal&set %1=AtsAgent seems not running.&goto :eof
