#!/bin/bash
# Script para configurar módulos de WildFly en el sistema de archivos local
# Se ejecuta UNA VEZ en la máquina del desarrollador

echo "🔧 Configurando módulos locales de WildFly..."

# Crear estructura de directorios
mkdir -p wildfly-modules/io/jsonwebtoken/main
mkdir -p wildfly-modules/org/springframework/security/main
mkdir -p wildfly-modules/org/mongodb/main

# Copiar JARs desde el EAR construido
echo "📦 Copiando JARs de JWT..."
cp ear/target/hcen/lib/jjwt-api.jar wildfly-modules/io/jsonwebtoken/main/
cp ear/target/hcen/lib/jjwt-impl.jar wildfly-modules/io/jsonwebtoken/main/
cp ear/target/hcen/lib/jjwt-jackson.jar wildfly-modules/io/jsonwebtoken/main/

cat > wildfly-modules/io/jsonwebtoken/main/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="io.jsonwebtoken">
    <resources>
        <resource-root path="jjwt-api.jar"/>
        <resource-root path="jjwt-impl.jar"/>
        <resource-root path="jjwt-jackson.jar"/>
    </resources>
    <dependencies>
        <module name="com.fasterxml.jackson.core.jackson-databind" export="true"/>
        <module name="com.fasterxml.jackson.core.jackson-core" export="true"/>
        <module name="com.fasterxml.jackson.core.jackson-annotations" export="true"/>
        <module name="java.xml"/>
        <module name="java.logging"/>
        <module name="java.base"/>
    </dependencies>
</module>
EOF

echo "🔒 Copiando JARs de Spring Security..."
cp ear/target/hcen/lib/spring-security-crypto.jar wildfly-modules/org/springframework/security/main/

cat > wildfly-modules/org/springframework/security/main/module.xml << 'EOF'
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

echo "🍃 Copiando JARs de MongoDB..."
cp ear/target/hcen/lib/mongodb-driver-sync.jar wildfly-modules/org/mongodb/main/
cp ear/target/hcen/lib/mongodb-driver-core.jar wildfly-modules/org/mongodb/main/
cp ear/target/hcen/lib/bson.jar wildfly-modules/org/mongodb/main/
cp ear/target/hcen/lib/bson-record-codec.jar wildfly-modules/org/mongodb/main/

cat > wildfly-modules/org/mongodb/main/module.xml << 'EOF'
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

echo "✅ Módulos configurados en wildfly-modules/"
echo "💡 Estos módulos se montarán automáticamente en el contenedor de WildFly"
