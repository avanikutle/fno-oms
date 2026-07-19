package com.fnooms.util;
public class TestSysProp {
    public static void main(String[] args) {
        System.out.println("exec.mainClass = " + System.getProperty("exec.mainClass"));
        System.out.println("sun.java.command = " + System.getProperty("sun.java.command"));
    }
}
