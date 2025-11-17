FROM quay.io/wildfly/wildfly:37.0.0.Final-jdk17

USER root

RUN mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main

# Copy the PostgreSQL driver and module descriptor into the WildFly modules path
COPY drivers/postgresql-42.2.14.jar /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql-42.2.14.jar
COPY wildfly/module.xml /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml

# Copy the CLI file (will be executed in embedded mode by the entrypoint) and entrypoint
COPY wildfly/configure-wildfly.cli /opt/jboss/wildfly/configure-wildfly.cli
COPY wildfly/entrypoint.sh /opt/jboss/wildfly/entrypoint.sh
RUN chmod +x /opt/jboss/wildfly/entrypoint.sh

USER jboss

ENTRYPOINT ["/opt/jboss/wildfly/entrypoint.sh"]
