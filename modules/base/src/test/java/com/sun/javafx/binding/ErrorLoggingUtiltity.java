/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class ErrorLoggingUtiltity {

    static {
        // initialize PlatformLogger
        Logging.getLogger();
    }

    // getLogManager will redirect existing PlatformLogger to the Logger
    private static final Logger logger = LogManager.getLogManager().getLogger("javafx.beans");

    Level level;
    LogRecord lastRecord;

    Handler handler = new Handler() {

        @Override
        public void publish(LogRecord record) {
            lastRecord = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    };

    public void start() {
        reset();
        level = logger.getLevel();
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    public void stop() {
        logger.setLevel(level);
        logger.removeHandler(handler);
    }
    
    public void reset() {
        lastRecord = null;
    }

    public void checkFine(Class expectedException) {
        check(Level.FINE, expectedException);        
    }

    public void check(Level expectedLevel, Class expectedException) {
        assertNotNull(lastRecord);
        assertEquals(expectedLevel, lastRecord.getLevel());
        assertEquals(expectedException, lastRecord.getThrown().getClass());
        reset();
    }

    public boolean isEmpty() {
        return lastRecord == null;
    }
}
