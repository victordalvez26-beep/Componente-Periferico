#!/bin/bash
set -euo pipefail
WILDFLY_HOME=/opt/jboss/wildfly
JBOSS_CLI=$WILDFLY_HOME/bin/jboss-cli.sh

# Start WildFly in background
echo "Starting WildFly..."
$WILDFLY_HOME/bin/standalone.sh -c standalone.xml -b 0.0.0.0 &

# Wait for management interface to become available
echo "Waiting for WildFly management interface (127.0.0.1:9990)..."
COUNT=0
until $JBOSS_CLI --connect --controller=127.0.0.1:9990 --commands=":read-attribute(name=server-state)" > /dev/null 2>&1; do
  sleep 1
  COUNT=$((COUNT+1))
  if [ "$COUNT" -ge 120 ]; then
    echo "Timed out waiting for WildFly management interface"
    exit 1
  fi
done

# Apply CLI configuration (module + driver + datasource)
if [ -f "$WILDFLY_HOME/configure-wildfly.cli" ]; then
  echo "Applying configure-wildfly.cli..."
  if ! $JBOSS_CLI --connect --file=$WILDFLY_HOME/configure-wildfly.cli; then
    echo "CLI execution failed — continuing to polling phase (it may have partially applied)."
  fi
else
  echo "Warning: configure-wildfly.cli not found at $WILDFLY_HOME/configure-wildfly.cli"
fi

# Wait for the datasource to be visible in the management model before deploying the EAR.
# This avoids deployment race where the persistence unit validates a missing JNDI datasource.
DS_NAME=MyMainDataSource
DS_MGMT_PATH="/subsystem=datasources/data-source=${DS_NAME}:read-resource"
echo "Waiting for datasource ${DS_NAME} to be present in the management model..."
COUNT=0
MAX_WAIT=180
while true; do
  if $JBOSS_CLI --connect --controller=127.0.0.1:9990 --commands="$DS_MGMT_PATH" > /tmp/ds-check.out 2>&1; then
    # quick verification for success string in CLI output
    if grep -q '"outcome"\s*:\s*"success"' /tmp/ds-check.out || grep -q "WFLYJCA" /tmp/ds-check.out; then
      echo "Datasource ${DS_NAME} found."
      break
    fi
  fi
  sleep 1
  COUNT=$((COUNT+1))
  if [ "$COUNT" -ge "$MAX_WAIT" ]; then
    echo "Timed out waiting for datasource ${DS_NAME} after ${MAX_WAIT}s. Showing last CLI output:" >&2
    sed -n '1,200p' /tmp/ds-check.out >&2 || true
    echo "Proceeding anyway; the EAR deployment may fail if the datasource is missing." >&2
    break
  fi
done

# Deploy the EAR only after datasource check
if [ -f /tmp/hcen.ear ]; then
  echo "Deploying EAR..."
  cp /tmp/hcen.ear $WILDFLY_HOME/standalone/deployments/hcen.ear
  # ensure dodeploy marker exists to trigger deployment
  touch $WILDFLY_HOME/standalone/deployments/hcen.ear.dodeploy || true
else
  echo "No EAR found at /tmp/hcen.ear; skipping deploy step (if you mount direct to deployments, adjust compose)."
fi

# Follow the server log
exec tail -F $WILDFLY_HOME/standalone/log/server.log
