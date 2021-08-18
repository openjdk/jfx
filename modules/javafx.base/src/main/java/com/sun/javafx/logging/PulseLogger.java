/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

public class PulseLogger {
    public static final boolean PULSE_LOGGING_ENABLED;

    private static final String [] DEFAULT_LOGGERS = {"com.sun.javafx.logging.PrintLogger", "com.sun.javafx.logging.jfr.JFRPulseLogger"};
    private static final Logger[] loggers;

    static {
        List<Logger> list = new ArrayList<>();
        for (String loggerClass : DEFAULT_LOGGERS) {
            Logger logger = loadLogger(loggerClass);
            if (logger != null) {
                list.add(logger);
            }
        }
        loggers = list.toArray(new Logger[list.size()]);
        PULSE_LOGGING_ENABLED = loggers.length > 0;
    }

    public static void pulseStart() {
        for (Logger logger: loggers) {
            logger.pulseStart();
        }
    }

    public static void pulseEnd() {
        for (Logger logger: loggers) {
            logger.pulseEnd();
        }
    }

    public static void renderStart() {
        for (Logger logger: loggers) {
            logger.renderStart();
        }
    }

    public static void renderEnd() {
        for (Logger logger: loggers) {
            logger.renderEnd();
        }
    }

    public static void addMessage(String message) {
        for (Logger logger: loggers) {
            logger.addMessage(message);
        }
    }

    public static void incrementCounter(String counter) {
        for (Logger logger: loggers) {
            logger.incrementCounter(counter);
        }
    }

    public static void newPhase(String name) {
        for (Logger logger: loggers) {
            logger.newPhase(name);
        }
    }

    public static void newInput(String name) {
        for (Logger logger: loggers) {
            logger.newInput(name);
        }
    }

    /**
     * @return true if the user requested pulse logging by setting the system
     *         property javafx.pulseLogger to true, false otherwise.
     */
    @SuppressWarnings("removal")
    public static boolean isPulseLoggingRequested() {
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("javafx.pulseLogger"));
    }

    // Loading known loggers reflectively, in case an expected module isn't available
    private static Logger loadLogger(String className) {
        try {
            Class<?> klass = Class.forName(className);
            if (klass != null) {
                Method method = klass.getDeclaredMethod("createInstance");
                return (Logger) method.invoke(null);
            }
        } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Ignore
        }
        return null;
    }
}
