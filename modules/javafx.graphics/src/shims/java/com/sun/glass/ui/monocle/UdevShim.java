/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Exposes the socket and event-header methods of {@link Udev} to tests, without starting its monitor thread. */
public final class UdevShim {

    public static final int UDEV_MONITOR_GROUP = Udev.UDEV_MONITOR_GROUP;

    private UdevShim() {
    }

    public static long openMonitor() throws IOException {
        return Udev.openMonitor();
    }

    public static int receive(long fd, ByteBuffer buffer) throws IOException {
        return Udev.receive(fd, buffer);
    }

    public static void closeMonitor(long fd) {
        Udev.closeMonitor(fd);
    }

    public static int propertiesOffset(ByteBuffer event) {
        return Udev.propertiesOffset(event);
    }

    public static int propertiesLength(ByteBuffer event) {
        return Udev.propertiesLength(event);
    }

    /** Forgets the process-wide event format decision. */
    public static void resetEventFormat() {
        Udev.resetEventFormatForTesting();
    }
}
