/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package shutdowntest;

public class Constants {

    // Error exit codes. Note that 0 and 1 are reserved for normal exit and
    // failure to launch java, respectively
    public static final int ERROR_NONE = 2;

    public static final int ERROR_SOCKET = 3;

    // Socket handshake value used at initialization (8-bit value)
    public static final int SOCKET_HANDSHAKE = 126;

    // Status codes written to the socket (8-bit value).
    public static final int STATUS_OK = 1;
    public static final int STATUS_ILLEGAL_STATE = 2;
    public static final int STATUS_RUNNABLE_EXECUTED = 3;
    public static final int STATUS_UNEXPECTED_EXCEPTION = 4;
}
