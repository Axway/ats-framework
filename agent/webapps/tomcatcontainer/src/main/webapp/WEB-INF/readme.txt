This folder contains the necessary descriptors for
building the Agent web application for both JBoss and Tomcat

The web descriptor for the JBoss app is web-jboss.xml (it is renamed to web.xml by the ant script)
The following files are included in the JBoss WAR:
    - agent.properties
    - jboss-web.xml
    
The web descriptor for the Tomcay app is web-tomcat.xml (it is renamed to web.xml by the ant script)
The following files are included in the Tomcat WAR:
    - agent.properties
    - sun-jaxws.xml