#!/bin/bash

# Navigate to the fno-oms-batch directory
cd /Users/birsa/dev/sm/fno-oms/fno-oms-batch || { echo "Failed to find fno-oms-batch directory"; exit 1; }

read -p "Do you want to login to mstock? (y/n): " mstock_ans
if [[ "$mstock_ans" =~ ^[Yy]$ ]]; then
    echo "Logging into mstock..."
    mvn exec:java -Dexec.mainClass="com.fnooms.algo.login.MStockLoginMain"
fi

read -p "Do you want to login to AngelOne? (y/n): " angel_ans
if [[ "$angel_ans" =~ ^[Yy]$ ]]; then
    echo "Logging into AngelOne..."
    mvn exec:java -Dexec.mainClass="com.fnooms.algo.login.AngelOneLoginMain"
fi

read -p "Do you want to login to Dhan? (y/n): " dhan_ans
if [[ "$dhan_ans" =~ ^[Yy]$ ]]; then
    echo "Logging into Dhan..."
    mvn exec:java -Dexec.mainClass="com.fnooms.algo.login.DhanLoginMain"
fi

read -p "Do you want to login to Groww? (y/n): " groww_ans
if [[ "$groww_ans" =~ ^[Yy]$ ]]; then
    echo "Logging into Groww..."
    mvn exec:java -Dexec.mainClass="com.fnooms.algo.login.GrowwLoginMain"
fi

echo "Login process completed."
