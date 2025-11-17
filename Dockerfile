FROM quay.io/wildfly/wildfly:latest-jdk17

COPY myapp.ear /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]