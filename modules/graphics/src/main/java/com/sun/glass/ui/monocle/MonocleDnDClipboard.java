/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

final class MonocleDnDClipboard extends SystemClipboard {

    public MonocleDnDClipboard() {
        super(Clipboard.DND);
    }

    /**
     * Should be called when drag operation completed by the 'system'
     *
     * @param action mask of actions from Clipboard
     */
    public void actionPerformed(int action) {
        super.actionPerformed(action);
    }

    protected  boolean isOwner() {
        return true;
    }
    /**
     * Here the magic happens.
     * When this method is called all input events should be grabbed and
     * appropriate drag notifications should be sent instead of regular input
     * events
     */
    protected  void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        // TODO: implement drag and nested loops in MonocleApplication
        // MonocleApplication app = (MonocleApplication)Application.GetApplication();
        // app.notifyDragStart();
        // app.enterDnDEventLoop();
        // The loop is exited in LensApplication.LensDragEvent.dispatch()
        actionPerformed(Clipboard.ACTION_COPY_OR_MOVE);
    }

    protected void pushTargetActionToSystem(int actionDone) {
    }

    protected  Object popFromSystem(String mimeType) {
        return null;
    }
    protected  int supportedSourceActionsFromSystem() {
        return Clipboard.ACTION_COPY_OR_MOVE;
    }

    protected  String[] mimesFromSystem() {
        return new String[0];
    }

}
