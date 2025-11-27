#!/bin/bash
set -euo pipefail

WILDFLY_HOME=/opt/jboss/wildfly
JBOSS_CLI=$WILDFLY_HOME/bin/jboss-cli.sh

# Render proporciona el puerto en la variable de entorno PORT
# Si no está definida, usar 8080 por defecto
PORT=${PORT:-8080}

echo "========================================="
echo "🚀 Iniciando WildFly para Render"
echo "========================================="
echo "PORT: $PORT"

# Parsear DATABASE_URL de Render si está disponible
# Formato: postgresql://user:password@host:port/database
if [ -n "${DATABASE_URL:-}" ]; then
    echo "DATABASE_URL encontrada, parseando..."
    # Extraer componentes de la connection string
    # postgresql://user:pass@host:port/db -> user, pass, host, port, db
    DB_URL=$(echo "$DATABASE_URL" | sed 's|postgresql://||')
    DB_USER=$(echo "$DB_URL" | cut -d: -f1)
    DB_PASS=$(echo "$DB_URL" | cut -d: -f2 | cut -d@ -f1)
    DB_HOST_PORT=$(echo "$DB_URL" | cut -d@ -f2 | cut -d/ -f1)
    DB_HOST=$(echo "$DB_HOST_PORT" | cut -d: -f1)
    DB_PORT=$(echo "$DB_HOST_PORT" | cut -d: -f2)
    DB_NAME=$(echo "$DB_URL" | cut -d/ -f2 | cut -d? -f1)
    
    DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
    DATASOURCE_USER="$DB_USER"
    DATASOURCE_PASSWORD="$DB_PASS"
    
    echo "✅ PostgreSQL configurado desde DATABASE_URL:"
    echo "   Host: $DB_HOST"
    echo "   Port: $DB_PORT"
    echo "   Database: $DB_NAME"
    echo "   User: $DB_USER"
elif [ -n "${DATASOURCE_URL:-}" ] && [ -n "${DATASOURCE_USER:-}" ] && [ -n "${DATASOURCE_PASSWORD:-}" ]; then
    echo "✅ Usando variables DATASOURCE_URL, DATASOURCE_USER, DATASOURCE_PASSWORD"
else
    echo "⚠️  WARNING: No se encontró configuración de PostgreSQL"
    echo "   Usando valores por defecto (puede fallar)"
    DATASOURCE_URL="${DATASOURCE_URL:-jdbc:postgresql://localhost:5432/hcen_db}"
    DATASOURCE_USER="${DATASOURCE_USER:-postgres}"
    DATASOURCE_PASSWORD="${DATASOURCE_PASSWORD:-password}"
fi

# Verificar configuración de MongoDB
if [ -z "${MONGODB_URI:-}" ]; then
    echo "⚠️  WARNING: MONGODB_URI no está configurada"
else
    echo "✅ MONGODB_URI configurada"
fi

# Crear archivo CLI temporal con las variables sustituidas
CLI_TEMP="$WILDFLY_HOME/configure-wildfly.cli.temp"
cat > "$CLI_TEMP" <<EOF
embed-server --std-out=echo --server-config=standalone.xml

# 1. Registrar el driver PostgreSQL
/subsystem=datasources/jdbc-driver=postgres:add(driver-name="postgres", driver-module-name="org.postgresql", driver-class-name=org.postgresql.Driver)

# 2. Crear el DataSource principal con valores parseados
/subsystem=datasources/data-source=MyMainDataSource:add(
    jndi-name="java:/jdbc/MyMainDataSource",
    driver-name="postgres",
    connection-url="${DATASOURCE_URL}",
    user-name="${DATASOURCE_USER}",
    password="${DATASOURCE_PASSWORD}",
    use-java-context=true,
    enabled=true,
    max-pool-size=20,
    flush-strategy=IdleConnections
)

# 3. Enable logging (INFO level para producción)
/subsystem=logging/logger=org.hibernate:add(level=INFO)
/subsystem=logging/logger=org.hibernate.persister.entity:add(level=INFO)
/subsystem=logging/logger=org.hibernate.loader:add(level=INFO)
/subsystem=logging/logger=org.jboss.as.txn:add(level=INFO)
/subsystem=logging/logger=org.jboss.as.ejb3.tx:add(level=INFO)
/subsystem=logging/logger=org.jboss.as.jpa:add(level=INFO)

# 4. Persistir cambios
/:write-configuration
stop-embedded-server
EOF

echo "========================================="
echo "Configurando WildFly..."
echo "========================================="

# Configurar WildFly para usar el puerto proporcionado por Render
# Reemplazar el puerto por defecto en standalone.xml
sed -i "s/socket-binding name=\"http\" port=\"8080\"/socket-binding name=\"http\" port=\"${PORT}\"/" \
    $WILDFLY_HOME/standalone/configuration/standalone.xml || true

# Ejecutar configuración de WildFly (datasource, driver, etc.)
if [ -f "$CLI_TEMP" ]; then
    echo "Ejecutando configuración CLI..."
    $JBOSS_CLI --file="$CLI_TEMP" || echo "Warning: configure-wildfly.cli.temp returned non-zero"
    rm -f "$CLI_TEMP"
else
    echo "No configure-wildfly.cli.temp found"
fi

echo "========================================="
echo "✅ Configuración completada"
echo "Iniciando WildFly en puerto $PORT..."
echo "========================================="

# Iniciar WildFly
# -b 0.0.0.0 permite conexiones desde cualquier IP (necesario para Render)
# -bmanagement 0.0.0.0 permite management desde cualquier IP
exec $WILDFLY_HOME/bin/standalone.sh \
    -c standalone.xml \
    -b 0.0.0.0 \
    -bmanagement 0.0.0.0 \
    -Djboss.http.port=$PORT \
    -Djboss.management.http.port=9990
