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

package com.sun.glass.ui.lens;

import java.util.HashMap;

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;
import com.sun.glass.ui.Application;

import sun.util.logging.PlatformLogger;

final class LensDnDClipboard extends SystemClipboard {

    public LensDnDClipboard() {
        super(Clipboard.DND);
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("constructor called");
        }
    }

    /**
     * Should be called when drag operation completed by the 'system'
     *
     * @param action mask of actions from Clipboard
     */
    public void actionPerformed(int action) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("action =  ["+
                                         Integer.toHexString(action)+"]");
        }
        super.actionPerformed(action);
    }

    protected  boolean isOwner() {
        //called many time while the hovering over target node
        //So reduced log level to finest in order to reduce log clutter
        if (LensLogger.isLogging(PlatformLogger.FINEST)) {
            LensLogger.getLogger().finest("returns true");
        }
        return true;//For DnD its always true
    }
    /**
     * Here the magic happens.
     * When this method is called all input events should be grabbed and
     * appropriate drag notifications should be sent instead of regular input
     * events
     */
    protected  void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("handling drag");
        }

        if (LensLogger.isLogging(PlatformLogger.FINER)) {
            LensLogger.getLogger().finer("data =[cachedData = "+ cacheData+
                " supportedActions= "+Integer.toHexString(supportedActions));
        }

        LensApplication lensApp = (LensApplication)Application.GetApplication();
        lensApp.notifyDragStart();

        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("starting nested event loop");
        }

        lensApp.enterDnDEventLoop();
        // The loop is exited in LensApplication.LensDragEvent.dispatch()
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("nested event loop finished");
            LensLogger.getLogger().fine("Drag done - notifying actionPreformed");
        }

        actionPerformed(Clipboard.ACTION_COPY_OR_MOVE);
    }




    //rest of the functionis should not be called. Only applicable for
    //SystemClipboards
    //Must be overrided thue as they are abstract functions

    protected  void pushTargetActionToSystem(int actionDone) {
        LensLogger.getLogger().warning("Not supported");
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("actionDone = "+
                                         Integer.toHexString(actionDone));
        }

    }

    protected  Object popFromSystem(String mimeType) {
        LensLogger.getLogger().warning("Not supported");
        if (LensLogger.isLogging(PlatformLogger.FINE)) {
            LensLogger.getLogger().fine("mimeType="+mimeType);
        }
        return null;
    }
    protected  int supportedSourceActionsFromSystem() {
        LensLogger.getLogger().warning("Not supported");

        return Clipboard.ACTION_COPY_OR_MOVE;
    }

    protected  String[] mimesFromSystem() {
        LensLogger.getLogger().warning("Not supported");

        return null;
    }

}
