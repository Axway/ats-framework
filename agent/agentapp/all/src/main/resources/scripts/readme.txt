To start the ATS Agent service use:
 - agent.bat file on Windows ( Usage: agent.bat start|stop|restart|status|version [-port PORT] [-java_exec <PATH TO JAVA EXECUTABLE>] [-logging_pattern day|hour|minute|30KB|20MB|10GB] )
 - agent.sh file on Unix ( Usage: ./agent.sh start|stop|restart|status|version [-port PORT] [-java_exec <PATH TO JAVA EXECUTABLE>] [-logging_pattern day|hour|minute|30KB|20MB|10GB] )

The service runs by default on port 8089. If you need to change the default port or in case you need some changes in the start commands, you have to edit the "agent.XYZ" files.
Currently supported Java versions are JRE/JDK 8 or newer.
