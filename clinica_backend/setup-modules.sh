#!/bin/bash
echo "🔧 Configurando módulos globales de WildFly..."

# Crear directorios
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/springframework/security/main
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main

# Copiar JARs desde el EAR
if [ -f "/deployments/hcen.ear" ]; then
    cd /tmp
    unzip -q /deployments/hcen.ear -d /tmp/ear-extract
    
    # JWT
    cp /tmp/ear-extract/lib/jjwt-api.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/
    cp /tmp/ear-extract/lib/jjwt-impl.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/
    cp /tmp/ear-extract/lib/jjwt-jackson.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/
    
    # Spring Security
    cp /tmp/ear-extract/lib/spring-security-crypto.jar /opt/jboss/wildfly/modules/system/layers/base/org/springframework/security/main/
    
    # MongoDB
    cp /tmp/ear-extract/lib/mongodb-driver-sync.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
    cp /tmp/ear-extract/lib/mongodb-driver-core.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
    cp /tmp/ear-extract/lib/bson.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
    cp /tmp/ear-extract/lib/bson-record-codec.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
fi

# Crear module.xml para JWT
cat > /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="io.jsonwebtoken">
    <resources>
        <resource-root path="jjwt-api.jar"/>
        <resource-root path="jjwt-impl.jar"/>
        <resource-root path="jjwt-jackson.jar"/>
    </resources>
    <dependencies>
        <module name="com.fasterxml.jackson.core.jackson-databind"/>
        <module name="java.xml"/>
        <module name="java.logging"/>
    </dependencies>
</module>
EOF

# Crear module.xml para Spring Security
cat > /opt/jboss/wildfly/modules/system/layers/base/org/springframework/security/main/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.springframework.security">
    <resources>
        <resource-root path="spring-security-crypto.jar"/>
    </resources>
    <dependencies>
        <module name="java.logging"/>
        <module name="java.base"/>
        <module name="org.apache.commons.logging"/>
    </dependencies>
</module>
EOF

# Crear module.xml para MongoDB
cat > /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.mongodb">
    <resources>
        <resource-root path="mongodb-driver-sync.jar"/>
        <resource-root path="mongodb-driver-core.jar"/>
        <resource-root path="bson.jar"/>
        <resource-root path="bson-record-codec.jar"/>
    </resources>
    <dependencies>
        <module name="java.logging"/>
        <module name="java.management"/>
        <module name="java.naming"/>
        <module name="javax.api"/>
    </dependencies>
</module>
EOF

echo "✅ Módulos configurados correctamente"

