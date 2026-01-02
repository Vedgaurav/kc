package com.one.kc.common.utils;

import org.slf4j.Logger;

public class LoggerUtils {

    public static void info(Logger logger, String message, Object... params){
        logger.info(LogSanitizer.sanitizeMessage(message), sanitizeParams(params));

    }
    public static void info(Logger logger, String message){
        logger.info(LogSanitizer.sanitizeMessage(message));

    }
    public static void error(Logger logger, String message, Throwable t, Object... params) {
        logger.error(
                LogSanitizer.sanitizeMessage(message),
                sanitizeParams(params),
                t
        );
    }

    public static void error(Logger logger, String message) {
        logger.error(
                LogSanitizer.sanitizeMessage(message)
        );
    }


    private static Object[] sanitizeParams(Object[] params) {
        if (params == null) return null;

        Object[] sanitized = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            sanitized[i] = LogSanitizer.sanitizeParam(params[i]);
        }
        return sanitized;
    }


}
