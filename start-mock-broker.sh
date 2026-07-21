#!/bin/bash

# Navigate to the project root directory
cd "$(dirname "$0")"

echo "========================================="
echo " Starting Mock Broker Server (Port 9090) "
echo "========================================="

cd fno-oms-batch
mvn exec:java -Dexec.mainClass="com.fnooms.amock.MockBrokerServerMain"
