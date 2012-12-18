/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
public class ExampleUsage {
    /*
     * Create a new logger -- note that you refer to them by name.
     * PlatformLogger keeps track of these for you so you do not need to carry
     * around references to objects.
     */
    private static final PlatformLogger weeLog = PlatformLogger
            .getLogger("wee");
    public static void main(String[] args) {
        /*
         * For now, we need to manually set the logging level up instead of
         * using a config file (e.g., -Djava.util.logging.config.file=foo.txt).
         * The reason is that we need the LoggingProxyImpl implementation in the
         * JDK in order for PlatformLogger to integrate properly with
         * java.util.logging.
         * 
         * Refer to
         * http://download.oracle.com/javase/6/docs/api/java/util/logging
         * /Level.html for a description of the various levels.
         */
        weeLog.setLevel(PlatformLogger.FINER);

        /*
         * There are a couple ways to output log messages. The first is very
         * straightforward and you typically use it if the message you are
         * outputting doesn't require much in the way of compute resources
         * (e.g., you're not building up some long string and calling lots of
         * methods to do so).
         */
        weeLog.finest("foo");

        /*
         * You should use this second method if the message you are going to
         * build up is going to do a lot of work.
         */
        if (weeLog.isLoggable(PlatformLogger.FINE)) {
            StringBuilder msg = new StringBuilder();
            msg.append("Some long");
            msg.append(" contrived message");
            msg.append(" that I am building up.");
            weeLog.fine(msg.toString());
        }
    }
}
