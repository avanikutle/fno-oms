#!/bin/bash

# Navigate to the project root directory
cd "$(dirname "$0")"

echo "========================================="
echo " Running Broker Benchmark                "
echo "========================================="

cd fno-oms-batch
mvn exec:java -Dexec.mainClass="com.fnooms.algo.benchmark.BrokerBenchmarkMain"
