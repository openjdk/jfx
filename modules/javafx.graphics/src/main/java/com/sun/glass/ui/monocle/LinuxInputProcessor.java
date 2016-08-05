/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A com.sun.glass.ui.monocle.input.LinuxInputProcessor is registered with a
 * com.sun.glass.ui.monocle.input.LinuxInputDevice when the device
 * is created. The listener is then notified when events are waiting to be
 * processed on the device.
 */
interface LinuxInputProcessor {
    /**
     * Called when events are waiting on the input device to be processed.
     * Called on the runnable processor provided to the input device.
     *
     * @param device The device on which events are pending
     */
    void processEvents(LinuxInputDevice device);

    static class Logger implements LinuxInputProcessor {
        @Override
        public void processEvents(LinuxInputDevice device) {
            LinuxEventBuffer buffer = device.getBuffer();
            while (buffer.hasNextEvent()) {
                System.out.format("%1$ts.%1$tL %2$s: %3$s\n",
                                  new java.util.Date(),
                                  device, buffer.getEventDescription());
                buffer.nextEvent();
            }
        }
    }

}
