/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.glass.ui.delegate.ClipboardDelegate;

import java.lang.annotation.Native;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.nio.ByteBuffer;

public class Clipboard {
    /**
     * predefined mime types
     * Have to be synchronized with native realization.
     */
    @Native public final static String TEXT_TYPE = "text/plain";
    @Native public final static String HTML_TYPE = "text/html";
    @Native public final static String RTF_TYPE = "text/rtf";
    @Native public final static String URI_TYPE = "text/uri-list";//http://www.ietf.org/rfc/rfc2483.txt
    @Native public final static String FILE_LIST_TYPE = "application/x-java-file-list";
    @Native public final static String RAW_IMAGE_TYPE = "application/x-java-rawimage";
    @Native public final static String DRAG_IMAGE = "application/x-java-drag-image";
    @Native public final static String DRAG_IMAGE_OFFSET = "application/x-java-drag-image-offset";
    @Native public final static String IE_URL_SHORTCUT_FILENAME = "text/ie-shortcut-filename";

    /**
     * predefined drop-effect actions and combinations.
     */
    @Native public final static int ACTION_NONE = 0x0;
    @Native public final static int ACTION_COPY = 0x1;
    @Native public final static int ACTION_MOVE = 0x2;
    @Native public final static int ACTION_REFERENCE = 0x40000000;
    @Native public final static int ACTION_COPY_OR_MOVE = ACTION_COPY | ACTION_MOVE;
    @Native public final static int ACTION_ANY       = 0x4FFFFFFF;

    /**
     * predefined clipboard name for system shared buffers
     */
    @Native public final static String DND = "DND";
    @Native public final static String SYSTEM = "SYSTEM";
    @Native public final static String SELECTION = "SELECTION";

    private final static Map<String, Clipboard> clipboards = new HashMap<>();
    private final static ClipboardDelegate delegate = PlatformFactory.getPlatformFactory().createClipboardDelegate();

    private final HashSet<ClipboardAssistance> assistants = new HashSet<>();
    private final String name;
    private final Object localDataProtector = new Object();
    private HashMap <String, Object> localSharedData;
    private ClipboardAssistance dataSource;

    /**
     * combination of ACTION_XXXX constants
     */
    protected int supportedActions = ACTION_COPY;

    protected Clipboard (String name) {
        Application.checkEventThread();
        this.name = name;
    }

    public void add (ClipboardAssistance assistant) {
        Application.checkEventThread();
        synchronized(assistants) {
            assistants.add(assistant);
        }
    }

    public void remove (ClipboardAssistance assistant) {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            if (assistant==dataSource) {
                dataSource = null;
            }
        }
        boolean needClose;
        synchronized(assistants) {
            assistants.remove(assistant);
            needClose = assistants.isEmpty();
        }

        if (needClose) {
            synchronized(clipboards) {
                clipboards.remove(name);
            }
            close();
        }
    }

    protected void setSharedData (
            ClipboardAssistance dataSource,
            HashMap<String, Object> cacheData,
            int supportedActions)
    {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            localSharedData = (HashMap<String, Object>) cacheData.clone();
            this.supportedActions = supportedActions;
            this.dataSource = dataSource;
        }
    }

    /**
     *
     * @param cacheData
     * @param supportedActions combination of ACTION_XXXX consts
     */
    public void flush(
        ClipboardAssistance dataSource,
        HashMap<String, Object> cacheData,
        int supportedActions)
    {
        Application.checkEventThread();
        setSharedData(dataSource, cacheData, supportedActions);
        contentChanged();
    }

    public int getSupportedSourceActions() {
        Application.checkEventThread();
        return this.supportedActions;
    }

    public void setTargetAction(int actionDone) {
        Application.checkEventThread();
        actionPerformed(actionDone);
    }

    public void contentChanged() {
        Application.checkEventThread();
        final HashSet <ClipboardAssistance> _assistants;
        synchronized(assistants) {
            _assistants = (HashSet <ClipboardAssistance>)assistants.clone();
        }
        for (ClipboardAssistance assistant : _assistants) {
            assistant.contentChanged();
        }
    }

    /**
     * Called by system and notifies about successful data transfer.
     * Delete-on-move functionality should be implemented here.
     * @param action Clipboard.ACTION_COPY, or Clipboard.ACTION_MOVE, or Clipboard.ACTION_REFERENCE
     */
    public void actionPerformed(int action) {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            if (null!=dataSource) {
                dataSource.actionPerformed(action);
            }
        }
    }


    public Object getData (String mimeType) {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            if (localSharedData == null) {
                return null;
            }
            Object ret = localSharedData.get(mimeType);
            return (ret instanceof DelayedCallback)
                ? ((DelayedCallback)ret).providedData()
                : ret;
        }
    }

    public String[] getMimeTypes () {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            if (localSharedData == null) {
                return null;
            }
            Set<String> mimes = localSharedData.keySet();
            String [] ret = new String[mimes.size()];
            int i = 0;
            for (String mime : mimes) {
                ret[i++] = mime;
            }
            return ret;
        }
    }

    /* We have only one clipboard for each name.
     * but it can be used by several @code{ClipboardAssistance}s
     */
    protected static Clipboard get (String clipboardName) {
        Application.checkEventThread();
        /* return apropriate one*/
        synchronized(clipboards) {
            if (!clipboards.keySet().contains(clipboardName)) {
                Clipboard newClipboard = delegate.createClipboard(clipboardName);
                if (newClipboard == null) {
                    newClipboard = new Clipboard(clipboardName);
                }
                clipboards.put(clipboardName, newClipboard);
            }
            return clipboards.get(clipboardName);
        }
    }

    public Pixels getPixelsForRawImage(byte rawimage[]) {
        Application.checkEventThread();
        ByteBuffer size = ByteBuffer.wrap(rawimage, 0, 8);
        int width = size.getInt();
        int height = size.getInt();

        ByteBuffer pixels = ByteBuffer.wrap(rawimage, 8, rawimage.length - 8); // drop width+height
        return Application.GetApplication().createPixels(width, height, pixels.slice());
    }

    @Override public String toString () {
        return "Clipboard: " + name + "@" + hashCode();
    }

    protected void close() {
        Application.checkEventThread();
        synchronized(localDataProtector) {
            dataSource = null;
        }
    }

    public String getName() {
        Application.checkEventThread();
        return name;
    }

    public static String getActionString (int action) {
        Application.checkEventThread();
        StringBuilder ret = new StringBuilder("");
        int[] test = {
            ACTION_COPY,
            ACTION_MOVE,
            ACTION_REFERENCE};
        String[] canDo = {
            "copy",
            "move",
            "link"};
        for (int i =0; i < 3; ++i) {
            if ((test[i] & action) > 0) {
                if (ret.length() > 0) {
                    ret.append(",");
                }
                ret.append(canDo[i]);
            }
        }
        return ret.toString();
    }
}
