package org.slf4j.impl;

import io.odin.slf4j.ExternalLogger;

public class StaticLoggerBinder extends ExternalLogger {

    public static String REQUESTED_API_VERSION = "1.7";

    private static final StaticLoggerBinder _instance = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return _instance;
    }

}
