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

import sun.util.logging.PlatformLogger.Level;

/**
 * Current supported embedded system doesn't have native clipboard, therefore
 * this class is a stub implementation of SystemClipboard that will provide
 * Clipboard functionality for a single FX application.
 */

final class LensSystemClipboard extends SystemClipboard {

    public LensSystemClipboard() {
        super(Clipboard.SYSTEM);
        if (LensLogger.getLogger().isLoggable(Level.FINE)) {
            LensLogger.getLogger().fine("LensSystemClipboard created");
        }
    }

    protected boolean isOwner() {
        //LensSystemClipboard doesn't integrate with platform clipboard as
        //it not available, so we always return true

        return true;
    }

    protected void pushToSystem(HashMap<String, Object> cacheData,
                                int supportedActions) {
        //no-op as there is no system clipboard

        if (LensLogger.getLogger().isLoggable(Level.FINE)) {
            LensLogger.getLogger().fine("LensSystemClipboard::pushToSystem " +
                                        "cacheData = " + cacheData +
                                        "supportedActions: " +
                                        getActionString(supportedActions));
        }

    }

    protected void pushTargetActionToSystem(int actionDone) {
        //no-op as there is no system clipboard
        if (LensLogger.getLogger().isLoggable(Level.FINE)) {
            LensLogger.getLogger().fine("LensSystemClipboard::pushTargetActionToSystem "
                                        + "actionDone: " +
                                        getActionString(actionDone));
        }

    }
    protected Object popFromSystem(String mimeType) {
        //this method should not be called as we are the owner of the data
        //see SystemClipboard::getData(String mimeType) for more information
        LensLogger.getLogger().warning("LensSystemClipboard::popFromSystem was called "
                                       + "mimType = " + mimeType);
        return null;
    }

    protected int supportedSourceActionsFromSystem() {
        //this method should not be called as we are the owner of the data
        //see SystemClipboard::getSupportedSourceActions() for more information
        LensLogger.getLogger().warning("LensSystemClipboard::supportedSourceActionsFromSystem "
                                       + "was called ");
        return Clipboard.ACTION_NONE;
    }

    protected String[] mimesFromSystem() {
        //this method should not be called as we are the owner of the data
        //see SystemClipboard::getMimeTypes() for more information
        LensLogger.getLogger().warning("LensSystemClipboard::mimesFromSystem "
                                       + "was called ");

        return new String[0];
    }

}