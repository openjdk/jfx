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

package test.com.sun.javafx.binding;

import static org.junit.Assert.*;

import com.sun.javafx.logging.PlatformLogger.Level;

import com.sun.javafx.binding.Logging;
import com.sun.javafx.binding.Logging.ErrorLogger.ErrorLogRecord;
import com.sun.javafx.binding.Logging.ErrorLogger;

public class ErrorLoggingUtiltity {

    private static ErrorLogger errorLogger = Logging.getLogger();

    public static void reset() {
        errorLogger.setErrorLogRecord(null);
    }

    public static boolean isEmpty() {
        return errorLogger.getErrorLogRecord() == null;
    }

    /**
     * Convenience method for check(Level.FINE, expectedException)
     */
    public static void checkFine(Class<?> expectedException) {
        check(Level.FINE, expectedException);
    }

    /**
     * Convenience method for check(Level.WARNING, expectedException)
     */
    public static void checkWarning(Class<?> expectedException) {
        check(Level.WARNING, expectedException);
    }

    public static void check(Level expectedLevel, Class<?> expectedException) {
        ErrorLogRecord errorLogRecord = errorLogger.getErrorLogRecord();
        assertNotNull(errorLogRecord);
        assertEquals(expectedLevel, errorLogRecord.getLevel());
        assertTrue(expectedException.isAssignableFrom(errorLogRecord.getThrown().getClass()));
        reset();
    }
}
