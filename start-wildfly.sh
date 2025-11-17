#!/bin/bash
# Load environment variables from .env file and start WildFly
# Usage: ./start-wildfly.sh

echo "Loading environment variables from .env file..."

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please copy .env.example to .env and configure your credentials."
    exit 1
fi

# Load environment variables from .env file
export $(grep -v '^#' .env | xargs)

echo ""
echo "Starting WildFly with environment variables..."
echo "MONGODB_URI=$MONGODB_URI"
echo "MONGODB_DB=$MONGODB_DB"
echo ""

# Check if WILDFLY_HOME is set
if [ -z "$WILDFLY_HOME" ]; then
    echo "Error: WILDFLY_HOME environment variable is not set!"
    echo "Please set it to your WildFly installation directory."
    exit 1
fi

# Start WildFly
"$WILDFLY_HOME/bin/standalone.sh"
