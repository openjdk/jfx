/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui;

import java.util.HashMap;

public abstract class SystemClipboard extends Clipboard {
    protected SystemClipboard(String name) {
        super(name);
        Application.checkEventThread();
    }

    protected abstract boolean isOwner ();
    protected abstract void pushToSystem (HashMap<String, Object> cacheData, int supportedActions);
    protected abstract void pushTargetActionToSystem (int actionDone);
    protected abstract Object popFromSystem (String mimeType);
    protected abstract int supportedSourceActionsFromSystem ();
    protected abstract String[] mimesFromSystem ();

    @Override public void flush(
        ClipboardAssistance dataSource,
        HashMap<String, Object> cacheData,
        int supportedActions)
    {
        Application.checkEventThread();
        //Skip contentChanged() call in super.flush(cacheData, supportedActions).
        //We have get it from native instead!
        setSharedData(dataSource, cacheData, supportedActions);
        pushToSystem(cacheData, supportedActions);
    }

    @Override public int getSupportedSourceActions() {
        Application.checkEventThread();
        if (isOwner()) {
            return super.getSupportedSourceActions();
        }
        return supportedSourceActionsFromSystem ();
    }

    @Override public void setTargetAction(int actionDone) {
        Application.checkEventThread();
        pushTargetActionToSystem(actionDone);
    }

    public Object getLocalData(String mimeType) {
        return super.getData(mimeType);
    }

    @Override public Object getData(String mimeType) {
        Application.checkEventThread();
        if (isOwner()) {
            return getLocalData(mimeType);
        }
        return popFromSystem(mimeType);
    }

    @Override public String[] getMimeTypes() {
        Application.checkEventThread();
        if (isOwner()) {
            return super.getMimeTypes();
        }
        return mimesFromSystem();
    }

    @Override public String toString() {
        Application.checkEventThread();
        return "System Clipboard";
    }
}
