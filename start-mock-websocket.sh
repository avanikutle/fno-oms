#!/bin/bash

# Navigate to the project root directory
cd "$(dirname "$0")"

echo "========================================="
echo " Starting Mock WebSocket Server (8082)   "
echo "========================================="

cd fno-oms-batch
mvn exec:java -Dexec.mainClass="com.fnooms.amock.MockWebSocketServerMain"
