package com.fnooms.util;

import ch.qos.logback.core.PropertyDefinerBase;

public class MainClassPropertyDefiner extends PropertyDefinerBase {
    @Override
    public String getPropertyValue() {
        String execClass = System.getProperty("exec.mainClass");
        if (execClass != null) {
            int lastDot = execClass.lastIndexOf('.');
            return lastDot != -1 ? execClass.substring(lastDot + 1) : execClass;
        }
        
        // Fallback to sun.java.command if exec.mainClass is missing
        String sunCommand = System.getProperty("sun.java.command");
        if (sunCommand != null) {
            String[] parts = sunCommand.split(" ");
            if (parts.length > 0) {
                String mainClass = parts[0];
                if (!mainClass.contains("Launcher")) {
                    int lastDot = mainClass.lastIndexOf('.');
                    return lastDot != -1 ? mainClass.substring(lastDot + 1) : mainClass;
                }
            }
        }

        return "Process";
    }
}
