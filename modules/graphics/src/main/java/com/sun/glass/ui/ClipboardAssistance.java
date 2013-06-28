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

public class ClipboardAssistance {

    private final HashMap <String, Object> cacheData  =  new HashMap <String, Object> ();
    private final Clipboard clipboard;
    private int supportedActions = Clipboard.ACTION_ANY;

    /**
     * Creates clipboard with mentioned name
     * @param cipboardName the clipboard name
     */
    public ClipboardAssistance (String cipboardName) {
        Application.checkEventThread();
        clipboard = Clipboard.get(cipboardName);
        clipboard.add(this);
    }

    /**
     * Close the notification channel
     */
    public void close () {
        Application.checkEventThread();
        clipboard.remove(this);
    }

    /**
     * Synchronize prepared dataset with local/global clipboard content.
     * An application becomes the clipboard owner.
     */
    public void flush () {
        Application.checkEventThread();
        clipboard.flush(this, cacheData, supportedActions);
    }

    /**
     * Cleans the local cache.
     */
    public void emptyCache () {
        Application.checkEventThread();
        cacheData.clear();
    }

    public boolean isCacheEmpty() {
        Application.checkEventThread();
        return cacheData.isEmpty();
    }

    /**
     * Fills the cache by a new (mime type, data) pair.
     * Performed by flush() call.
     * @param mimeType
     * @param data
     */
    //TODO: Auto-flush parameter?
    public void setData (String mimeType, Object data) {
        Application.checkEventThread();
        cacheData.put(mimeType, data);
    }

    /**
     * Returns the data from clipboard by mime type key.
     * That is always shared data.
     * @param mimeType
     * @return the shared data object
     */
    public Object getData (String mimeType) {
        Application.checkEventThread();
        return clipboard.getData(mimeType);
    }

    /**
     * Sets the actions that are supported by source.
     * Performed by flush() call.
     * @param supportedActions combination of Clipboard.ACTION_XXXX constants
     */
    public void setSupportedActions(int supportedActions) {
        Application.checkEventThread();
        this.supportedActions = supportedActions;
    }

    /**
     * Gets the actions that are supported by source.
     * @return combination of Clipboard.ACTION_XXXX constants
     */
    public int getSupportedSourceActions() {
        Application.checkEventThread();
        return clipboard.getSupportedSourceActions();
    }

    /**
     * Sets the action that clipboard target performed of DnD target can.
     * @param actionDone Clipboard.ACTION_COPY, or Clipboard.ACTION_MOVE, or Clipboard.ACTION_REFERENCE
     */
    public void setTargetAction (int actionDone) {
        Application.checkEventThread();
        clipboard.setTargetAction(actionDone);
    }

    /**
     * Called by system and notifies that data set in shared buffer were changed
     */
    public void contentChanged () {}

    /**
     * Called by system and notifies about successful data transfer.
     * Delete-on-move functionality should be implemented here.
     * @param action Clipboard.ACTION_COPY, or Clipboard.ACTION_MOVE, or Clipboard.ACTION_REFERENCE
     */
    public void actionPerformed (int action) {}

    public String[] getMimeTypes () {
        Application.checkEventThread();
        return clipboard.getMimeTypes();
    }

    @Override
    public String toString () {
        return "ClipboardAssistance[" + clipboard + "]" ;
    }
}
