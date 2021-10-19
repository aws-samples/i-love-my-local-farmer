FROM tomcat:9.0.52

RUN mv /usr/local/tomcat/webapps.dist/* /usr/local/tomcat/webapps/
COPY provman.war /usr/local/tomcat/webapps/

EXPOSE 8080