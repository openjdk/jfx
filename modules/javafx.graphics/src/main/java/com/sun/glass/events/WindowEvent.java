/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.events;

import java.lang.annotation.Native;

public class WindowEvent {
    @Native final static public int RESIZE                = 511;
    @Native final static public int MOVE                  = 512;
    @Native final static public int RESCALE               = 513;

    @Native final static public int CLOSE                 = 521;
    @Native final static public int DESTROY               = 522;

    @Native final static public int MINIMIZE              = 531;
    @Native final static public int MAXIMIZE              = 532;
    @Native final static public int RESTORE               = 533;

    @Native final static public int _FOCUS_MIN            = 541;
    @Native final static public int FOCUS_LOST            = 541;
    @Native final static public int FOCUS_GAINED          = 542;
    @Native final static public int FOCUS_GAINED_FORWARD  = 543;
    @Native final static public int FOCUS_GAINED_BACKWARD = 544;
    @Native final static public int _FOCUS_MAX            = 544;

    @Native final static public int FOCUS_DISABLED        = 545;
    @Native final static public int FOCUS_UNGRAB          = 546;

    public static String getEventName(final int eventType) {
        switch(eventType) {
            case WindowEvent.RESIZE:
                return "RESIZE";
            case WindowEvent.MOVE:
                return "MOVE";
            case WindowEvent.RESCALE:
                return "RESCALE";
            case WindowEvent.CLOSE:
                return "CLOSE";
            case WindowEvent.DESTROY:
                return "DESTROY";
            case WindowEvent.MINIMIZE:
                return "MINIMIZE";
            case WindowEvent.MAXIMIZE:
                return "MAXIMIZE";
            case WindowEvent.RESTORE:
                return "RESTORE";
            case WindowEvent.FOCUS_LOST:
                return "FOCUS_LOST";
            case WindowEvent.FOCUS_GAINED:
                return "FOCUS_GAINED";
            case WindowEvent.FOCUS_GAINED_FORWARD:
                return "FOCUS_GAINED_FORWARD";
            case WindowEvent.FOCUS_GAINED_BACKWARD:
                return "FOCUS_GAINED_BACKWARD";
            case WindowEvent.FOCUS_DISABLED:
                return "FOCUS_DISABLED";
            case WindowEvent.FOCUS_UNGRAB:
                return "FOCUS_UNGRAB";
            default:
                return "UNKNOWN";
        }
    }
}
