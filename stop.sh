#!/bin/bash

# Navigate to the project root directory
cd "$(dirname "$0")"

echo "========================================="
echo " Stopping Tomcat...                      "
echo "========================================="
cd fno-oms-web

# Try graceful stop first
mvn cargo:stop

echo "Cleaning up hanging processes and ports..."

# Kill any processes still listening on port 8080
PORT_PIDS=$(lsof -t -i :8080)
if [ ! -z "$PORT_PIDS" ]; then
    echo "Force killing processes on port 8080: $PORT_PIDS"
    kill -9 $PORT_PIDS 2>/dev/null
fi

# Kill any lingering maven cargo run/stop processes
CARGO_PIDS=$(ps -ef | grep 'cargo:run\|cargo:stop' | grep -v grep | awk '{print $2}')
if [ ! -z "$CARGO_PIDS" ]; then
    echo "Force killing lingering Maven cargo processes: $CARGO_PIDS"
    kill -9 $CARGO_PIDS 2>/dev/null
fi

echo "✅ Tomcat successfully stopped and cleaned up!"
