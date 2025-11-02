#!/bin/bash
set -euo pipefail

WILDFLY_HOME=/opt/jboss/wildfly
JBOSS_CLI=$WILDFLY_HOME/bin/jboss-cli.sh

echo "Running embedded CLI to persist driver and datasource configuration (if present)..."
if [ -f "$WILDFLY_HOME/configure-wildfly.cli" ]; then
  # run the CLI file which is expected to use embed-server / stop-embedded-server
  $JBOSS_CLI --file=$WILDFLY_HOME/configure-wildfly.cli || echo "Warning: configure-wildfly.cli returned non-zero"
else
  echo "No configure-wildfly.cli found in image"
fi

echo "Starting WildFly server"
exec $WILDFLY_HOME/bin/standalone.sh -c standalone.xml -b 0.0.0.0
