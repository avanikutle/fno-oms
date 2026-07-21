#!/bin/bash

cd "$(dirname "$0")"

echo "========================================="
echo " Restarting Tomcat...                    "
echo "========================================="

# Stop tomcat
./stop-tomcat.sh

# Start tomcat
./start-tomcat.sh
