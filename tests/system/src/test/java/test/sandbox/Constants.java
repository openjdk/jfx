/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.sandbox;

/**
 * Global constants for sandbox tests.
 */
public class Constants {

    // Test timeout in milliseconds
    public static final int TIMEOUT = 30000;

    // Time in milliseconds to show the stage
    public static final int SHOWTIME = 2500;

    // Error exit codes. Note that 0 and 1 are reserved for normal exit and
    // failure to launch java, respectively
    public static final int ERROR_NONE = 2;

    public static final int ERROR_TIMEOUT = 3;
    public static final int ERROR_SECURITY_EXCEPTION = 4;
    public static final int ERROR_NO_SECURITY_EXCEPTION = 5;
    public static final int ERROR_UNEXPECTED_EXCEPTION = 6;

    // No need to ever create an instance of this class
    private Constants() {}

}
