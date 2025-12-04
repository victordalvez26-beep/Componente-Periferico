#!/bin/bash
# Script de inicialización de módulos globales de WildFly
# Se ejecuta al iniciar el contenedor para configurar JWT, Spring Security y MongoDB

echo "🔧 Configurando módulos globales de WildFly..."

# Mover temporalmente el EAR para evitar auto-despliegue durante la configuración
DEPLOY_DIR="/opt/jboss/wildfly/standalone/deployments"
EAR_FILE="$DEPLOY_DIR/hcen.ear"
TEMP_EAR="/tmp/hcen.ear"

if [ -f "$EAR_FILE" ]; then
  echo "📦 Moviendo EAR temporalmente..."
  mv "$EAR_FILE" "$TEMP_EAR"
fi

# Extraer librerías del EAR
echo "📂 Extrayendo librerías del EAR..."
cd /tmp
unzip -q "$TEMP_EAR" -d /tmp/ear-content 2>/dev/null || true

# Verificar que las librerías existen
if [ ! -f /tmp/ear-content/lib/jjwt-api.jar ]; then
  echo "❌ Error: No se encontraron las librerías en el EAR"
  mv "$TEMP_EAR" "$EAR_FILE"
  exit 1
fi

# Crear directorios de módulos
echo "📁 Creando directorios de módulos..."
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/springframework/security/main
mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main

# Copiar JARs de JWT
echo "🔐 Configurando módulo JWT..."
cp /tmp/ear-content/lib/jjwt-api.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/
cp /tmp/ear-content/lib/jjwt-impl.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/
cp /tmp/ear-content/lib/jjwt-jackson.jar /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/

cat > /opt/jboss/wildfly/modules/system/layers/base/io/jsonwebtoken/main/module.xml << 'EOF'
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

# Copiar JARs de Spring Security
echo "🔒 Configurando módulo Spring Security..."
cp /tmp/ear-content/lib/spring-security-crypto.jar /opt/jboss/wildfly/modules/system/layers/base/org/springframework/security/main/

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

# Copiar JARs de MongoDB
echo "🍃 Configurando módulo MongoDB..."
cp /tmp/ear-content/lib/mongodb-driver-sync.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
cp /tmp/ear-content/lib/mongodb-driver-core.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
cp /tmp/ear-content/lib/bson.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/
cp /tmp/ear-content/lib/bson-record-codec.jar /opt/jboss/wildfly/modules/system/layers/base/org/mongodb/main/

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

echo "✅ Módulos globales configurados correctamente"

# Devolver el EAR al directorio de deployment
echo "🔄 Restaurando EAR para despliegue..."
mv "$TEMP_EAR" "$EAR_FILE"

# Limpiar archivos temporales
rm -rf /tmp/ear-content

# Iniciar WildFly
echo "🚀 Iniciando WildFly..."
exec /opt/jboss/wildfly/bin/standalone.sh -c standalone.xml -b 0.0.0.0
