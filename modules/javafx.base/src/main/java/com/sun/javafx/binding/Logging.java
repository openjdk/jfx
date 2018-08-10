/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.logging.PlatformLogger;

public class Logging {

    public static ErrorLogger getLogger() {
        return ErrorLogger.INSTANCE;
    }

    /**
     * A PlatformLogger that keeps a record ({@code ErrorLogRecord}) of the last error ({@code Throwable}) logged.
     */
    public static class ErrorLogger extends PlatformLogger {

        ErrorLogger() {
            super(System.getLogger("javafx.beans"));
        }

        private static final ErrorLogger INSTANCE = new ErrorLogger();

        public static class ErrorLogRecord {
            private final Level level;
            private final Throwable thrown;

            public ErrorLogRecord(Level level, Throwable thrown) {
                this.level = level;
                this.thrown = thrown;
            }

            public Throwable getThrown() {
                return thrown;
            }

            public Level getLevel() {
                return level;
            }
        }

        private ErrorLogRecord errorLogRecord;

        public ErrorLogRecord getErrorLogRecord() {
            return errorLogRecord;
        }

        public void setErrorLogRecord(ErrorLogRecord errorLogRecord) {
            this.errorLogRecord = errorLogRecord;
        }

        /* Some of the following logging methods are unused and thus commented-out,
           but are kept here anyway on purpose. See JDK-8195974 */

/*        @Override
        public void severe(String msg, Throwable t) {
            super.severe(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.SEVERE, t);
        }*/

        @Override
        public void warning(String msg, Throwable t) {
            super.warning(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.WARNING, t);
        }

/*        @Override
        public void info(String msg, Throwable t) {
            super.info(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.INFO, t);
        }*/

        @Override
        public void fine(String msg, Throwable t) {
            super.fine(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.FINE, t);
        }

/*        @Override
        public void finer(String msg, Throwable t) {
            super.finer(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.FINER, t);
        }*/

/*        @Override
        public void finest(String msg, Throwable t) {
            super.finest(msg, t);
            errorLogRecord = new ErrorLogRecord(Level.FINEST, t);
        }*/
    }
}
