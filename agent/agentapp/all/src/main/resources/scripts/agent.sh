#!/bin/sh

# (Optional) Java and JVM runtime options
#Example : export JAVA_OPTS="$JAVA_OPTS -Dmy.prop1=ABC -Dmy.prop2=CBA"
JAVA_OPTS="$JAVA_OPTS"
# the Agent port
PORT=8089
# the java executable
JAVA_EXEC=java

# folder containing the Agent actions
COMPONENTS_FOLDER=ats-agent/actions
# folder containing the Agent template actions
TEMPLATE_ACTIONS_FOLDER=ats-agent/templateActions
# set agent logging severity level
LOGGING_SEVERITY=INFO
# the amount of memory used by the service (in MB)
MEMORY=256

# allow remote connections for debug purposes (true/false)
DEBUG=false
DEBUG_PORT=8000
# DEBUG_OPTIONS details are defined later
DEBUG_OPTIONS=""

# enable monitoring the number of pending log events (true/false)
MONITOR_EVENTS_QUEUE=false

# allow JMX connections (true/false)
JMX=false
JMX_PORT=1099
JMX_OPTIONS=""
if $JMX
then
    JMX_OPTIONS="-Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
fi

# set logging pattern
LOGGING_PATTERN=

# the program name
PROG_NAME=AtsAgent

# get the absolute path to the script, e.g. /home/user/ats_agent/agent.sh
SCRIPTPATH=$(cd "$(dirname "$0")" && pwd)

# Do not change next line without sync-ing with Agent-with-Java POM parts.
# parse the input arguments
# remember the main command for later use
COMMAND="$1"
FOUND_TOKEN=""

for key in "$@" ; do

    if [ "$FOUND_TOKEN" = "-port" ]; then
        # set port
        PORT=$key
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-java_exec" ]; then
        # set java executable
        JAVA_EXEC=$key
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-memory" ]; then
        # set memory
        MEMORY=$key
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-logging_pattern" ]; then
        # set logging pattern
        LOGGING_PATTERN=$key
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-logging_severity" ]; then
        # set logging severity
        LOGGING_SEVERITY=$key
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-java_opts" ]; then
        # append java_opts to existing ones
        JAVA_OPTS="$JAVA_OPTS $key"
        FOUND_TOKEN=""
    elif [ "$FOUND_TOKEN" = "-debug" ]; then
        # debug mode
        DEBUG=true
        DEBUG_PORT=$key
        FOUND_TOKEN=""
    else
        case $key in
            -port)
                FOUND_TOKEN="-port"
            ;;
             -java_exec)
                FOUND_TOKEN="-java_exec"
            ;;
             -memory)
                FOUND_TOKEN="-memory"
            ;;
             -logging_pattern)
                FOUND_TOKEN="-logging_pattern"
            ;;
             -logging_severity)
                FOUND_TOKEN="-logging_severity"
            ;;
            -java_opts)
                FOUND_TOKEN="-java_opts"
            ;;
            -debug)
                FOUND_TOKEN="-debug"
            ;;
              *)
            # unknown option
            ;;
        esac
    fi

    shift
done

agent_start() {

    echo "Starting $PROG_NAME ..."

    if [ "$(agent_status)" = "$PROG_NAME is running" ]
    then
        echo "$PROG_NAME is already started on port $PORT"
        exit
    fi

    ENDORSED_OPTIONS=""
    JAVA_VERSION=$($JAVA_EXEC -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    case "$JAVA_VERSION" in
        "1.8"*)
            # Java 8 (lower not supported) - use endorsed dir
            if $DEBUG
            then
                DEBUG_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,address=$DEBUG_PORT,suspend=n"
            fi
            ENDORSED_OPTIONS=-Djava.endorsed.dirs=ats-agent/endorsed
            ;;
        "1."*)
            echo "Unsupported Java version - $JAVA_VERSION. It looks to be less than 1.8!"
            exit 5
            ;;
        *) # Java 9 or newer
            if $DEBUG
            then
                # Listen on all network interfaces - use "address=*:port" Java 9+ format.
                # Since Java 9 by default debug session listens on localhost only but this is not usable
                # as ATS agents by default are remote
                DEBUG_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,address=*:$DEBUG_PORT,suspend=n"
            fi
            # No endorsed mechanism
            ENDORSED_OPTIONS=""
            ;;
    esac

    nohup $JAVA_EXEC -showversion -Dats.agent.default.port=$PORT -Dats.agent.home="$SCRIPTPATH" \
       $ENDORSED_OPTIONS $JMX_OPTIONS \
       -Dats.log.monitor.events.queue=$MONITOR_EVENTS_QUEUE \
       -Dats.agent.components.folder="$COMPONENTS_FOLDER" -Dagent.template.actions.folder="$TEMPLATE_ACTIONS_FOLDER" \
       -Xms${MEMORY}m -Xmx${MEMORY}m \
       -Dlogging.severity="$LOGGING_SEVERITY" -Dlogging.pattern="$LOGGING_PATTERN" \
       $JAVA_OPTS $DEBUG_OPTIONS \
       -jar ats-agent/ats-agent-standalone-containerstarter.jar > logs/nohup_$PORT.out 2>&1&

    JVM_PID=$!
    if $DEBUG
    then
        echo "started in DEBUG mode on $DEBUG_PORT with PID: $JVM_PID"
    else
        echo "started with PID: $JVM_PID"
    fi
}

agent_start_in_container() {

    echo "Starting $PROG_NAME in container mode ... "

    if [ "$(agent_status)" = "$PROG_NAME is running" ]
    then
        echo "$PROG_NAME is already started on port $PORT"
        exit
    fi

    ENDORSED_OPTIONS=""
    JAVA_VERSION=$($JAVA_EXEC -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    case "$JAVA_VERSION" in
        "1.8"*)
            # Java 8 (lower not supported) - use endorsed dir
            if $DEBUG
            then
                DEBUG_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,address=$DEBUG_PORT,suspend=n"
            fi
            ENDORSED_OPTIONS=-Djava.endorsed.dirs=ats-agent/endorsed
            ;;
        "1."*)
            echo "Unsupported Java version - $JAVA_VERSION! It looks to be less than 1.8."
            exit 16
            ;;
        *)
            # Java 9 or newer
            if $DEBUG
            then
                # Listen on all network interfaces - use "address=*:port" Java 9+ format. Since Java 9 by default
                # debug session listens on localhost only but this is not usable as ATS agents by default are remote
                DEBUG_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,address=*:$DEBUG_PORT,suspend=n"
            fi
            # No endorsed mechanism
            ENDORSED_OPTIONS=""
            ;;
    esac

    $JAVA_EXEC -showversion -Dats.agent.default.port=$PORT -Dats.agent.home="$SCRIPTPATH" \
    $ENDORSED_OPTIONS $JMX_OPTIONS \
    -Dats.log.monitor.events.queue=$MONITOR_EVENTS_QUEUE \
    -Dats.agent.components.folder="$COMPONENTS_FOLDER" -Dagent.template.actions.folder="$TEMPLATE_ACTIONS_FOLDER" \
    -Xms${MEMORY}m -Xmx${MEMORY}m  \
    $JAVA_OPTS \
    $DEBUG_OPTIONS \
    -Dlogging.enable.log4j.file=true \
    -jar ats-agent/ats-agent-standalone-containerstarter.jar
}

agent_stop() {
    if [ -f logs/atsAgent_"$PORT".pid ]
    then
        read JVM_PID < logs/atsAgent_"$PORT".pid
        echo "Stopping $PROG_NAME with PID: $JVM_PID"
        # try to exit gracefully
        kill "$JVM_PID" 2>/dev/null
        rm logs/atsAgent_"$PORT".pid
    else
        echo "$PROG_NAME seems not running. No such .pid file."
    fi
}


agent_status() {

    if [ -f logs/atsAgent_$PORT.pid ]
    then
        read JVM_PID < logs/atsAgent_$PORT.pid
        # kill -0 gives false also for permission reasons so we expect that either root or the current user had started the process before
        if kill -0 $JVM_PID 2>/dev/null >&- ;
        then
            # Has PID file. Probably running;
            if ps -fp $JVM_PID | grep ats.agent.default.port 1>/dev/null
            then
                echo $PROG_NAME is running
            else
                echo "$PROG_NAME is not running. Another process with such PID"
                rm -f logs/atsAgent_$PORT.pid
            fi
        else
            echo "$PROG_NAME seems not running."
            rm -f logs/atsAgent_$PORT.pid
        fi
    else
        echo "$PROG_NAME seems not running. No such .pid file."
    fi
}


agent_version() {
    $JAVA_EXEC -cp ats-agent/ats-agent-standalone-containerstarter.jar com.axway.ats.agentapp.standalone.utils.AtsVersionExtractor
}

cd "$SCRIPTPATH"
mkdir -p -m 775 logs

case $COMMAND in
    'start')
        agent_start
    ;;
    'container')
        agent_start_in_container
    ;;
    'stop')
        agent_stop
    ;;
    'restart')
        agent_stop
        sleep 2
        agent_start
    ;;
    'status')
        agent_status
    ;;
    'version')
        agent_version
    ;;
    *)
        # TODO print [-java_opts JAVA_OPTS] as a known command in the future
        echo "Usage:"
        echo "$0 start|stop|restart|status|version|container [-port PORT] [-java_exec PATH_TO_JAVA_EXECUTABLE] [-logging_pattern day|hour|minute|30KB|20MB|10GB] [-memory MEMORY_IN_MB] [-logging_severity DEBUG|INFO|WARN|ERROR]"
    ;;
esac
