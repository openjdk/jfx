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

package com.sun.glass.ui.monocle;

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;

import java.util.HashMap;

/** There is no system clipboard on embedded Linux systems using a
 * framebuffer. For X11 and Android a different implementation will
 * be needed. */
final class MonocleSystemClipboard extends SystemClipboard {

    MonocleSystemClipboard() {
        super(Clipboard.SYSTEM);
    }

    protected boolean isOwner() {
        return true;
    }

    protected void pushToSystem(HashMap<String, Object> cacheData,
                                int supportedActions) {
    }

    protected void pushTargetActionToSystem(int actionDone) {
    }
    protected Object popFromSystem(String mimeType) {
        return null;
    }

    protected int supportedSourceActionsFromSystem() {
        return Clipboard.ACTION_NONE;
    }

    protected String[] mimesFromSystem() {
        return new String[0];
    }

}