#!/bin/bash

# Navigate to the project root directory
cd "$(dirname "$0")"

echo "========================================="
echo " Building fno-oms project...             "
echo "========================================="
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed! Aborting startup."
    exit 1
fi

echo ""
echo "========================================="
echo " Starting Tomcat via Maven Cargo...      "
echo "========================================="
cd fno-oms-web
nohup mvn cargo:run > ../web-logs/tomcat-stdout.log 2>&1 &

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Tomcat successfully started in the background!"
    echo "🌍 Access the app at: http://localhost:8080/fno-oms"
else
    echo "❌ Failed to start Tomcat."
fi
