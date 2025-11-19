package com.iftakher.passwordmanager;

/**
 * Launcher class for JavaFX application.
 * This is required when packaging JavaFX apps in a fat JAR.
 * The launcher doesn't extend Application to avoid module issues.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
