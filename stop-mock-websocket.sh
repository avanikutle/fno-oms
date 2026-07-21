#!/bin/bash

echo "========================================="
echo " Stopping Mock WebSocket Server          "
echo "========================================="

# Find and kill the Mock WebSocket process on port 8082
PIDS=$(lsof -t -i :8082)
if [ ! -z "$PIDS" ]; then
    echo "Killing Mock WebSocket processes: $PIDS"
    kill -9 $PIDS 2>/dev/null
    echo "✅ Mock WebSocket stopped!"
else
    echo "No Mock WebSocket process found running on port 8082."
fi
