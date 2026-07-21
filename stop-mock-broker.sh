#!/bin/bash

echo "========================================="
echo " Stopping Mock Broker Server             "
echo "========================================="

# Find and kill the Mock Broker process on port 9090
PIDS=$(lsof -t -i :9090)
if [ ! -z "$PIDS" ]; then
    echo "Killing Mock Broker processes: $PIDS"
    kill -9 $PIDS 2>/dev/null
    echo "✅ Mock Broker stopped!"
else
    echo "No Mock Broker process found running on port 9090."
fi
