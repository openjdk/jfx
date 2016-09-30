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

public class ViewEvent {
    @Native final static public int ADD                 = 411;
    @Native final static public int REMOVE              = 412;

    @Native final static public int REPAINT             = 421;
    @Native final static public int RESIZE              = 422;
    @Native final static public int MOVE                = 423; // a-la "insets changed"

    @Native final static public int FULLSCREEN_ENTER    = 431;
    @Native final static public int FULLSCREEN_EXIT     = 432;

    static public String getTypeString(int type) {
        String string = "UNKNOWN";
        switch (type) {
            case ADD: string = "ADD"; break;
            case REMOVE: string = "REMOVE"; break;

            case REPAINT: string = "REPAINT"; break;
            case RESIZE: string = "RESIZE"; break;
            case MOVE: string = "MOVE"; break;

            case FULLSCREEN_ENTER: string = "FULLSCREEN_ENTER"; break;
            case FULLSCREEN_EXIT: string = "FULLSCREEN_EXIT"; break;

            default:
                System.err.println("Unknown view event type: " + type);
                break;
        }
        return string;
    }
}
