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

package com.sun.glass.ui.ios;

import java.util.HashMap;

/**
 * Java class encapsulating access to native iOS UIPasteboard API.
 */
final class IosPasteboard {

    final static public int General = 1;

    final static public int UtfIndex = 0;
    final static public int ObjectIndex = 1;

    final static public String UtfString = "public.utf8-plain-text";
    final static public String UtfPdf = "com.adobe.pdf";
    final static public String UtfTiff = "public.tiff";
    final static public String UtfPng = "public.png";
    final static public String UtfRtf = "public.rtf";
    final static public String UtfRtfd = "com.apple.flat-rtfd";
    final static public String UtfHtml = "public.html";
    final static public String UtfTabularText = "public.utf8-tab-separated-values-text";
    final static public String UtfFont = "com.apple.cocoa.pasteboard.character-formatting";
    final static public String UtfColor = "com.apple.cocoa.pasteboard.color";
    final static public String UtfSound = "com.apple.cocoa.pasteboard.sound";
    final static public String UtfMultipleTextSelection = "com.apple.cocoa.pasteboard.multiple-text-selection";
    final static public String UtfFindPanelSearchOptions = "com.apple.cocoa.pasteboard.find-panel-search-options";
    final static public String UtfUrl = "public.url";
    final static public String UtfFileUrl = "public.file-url";

    // Native object handle (UIPasteboard*, etc.)
    private long ptr = 0L;

    // user pasteboard should be released when done (system one, however, must not be released)
    private boolean user;

    // Creates system platform pasteboard of the given type;
    // System pasteboard is used e.g. for common Copy/Paste operations
    // IosPasteboard.General is the only supported type value
    private native long _createSystemPasteboard(int type);

    /**
     * Creates IosPasteboard wrapper object for UIPasteboard's generalPasteboard.
     */
    public IosPasteboard(int type) {
        this.user = false;
        this.ptr = _createSystemPasteboard(type);
    }

    // creates user platform pasteboard for the given name
    private native long _createUserPasteboard(String name);

    /**
     * Creates custom IosPasteboard for given name. E.g. for drag and drop purpose
     * we create user UIPasterboard with special name DND.
     * See [UIPasteboard pasteboardWithName:create:] documentation.
     */
    public IosPasteboard(String name) {
        this.user = true;
        this.ptr = _createUserPasteboard(name);
    }

    /**
     * Returns native pasteboard pointer (UIPasteboard*)
     */
    public long getNativePasteboard() {
        assertValid();
        return this.ptr;
    }

    // returns name of the native UIPasteboard
    private native String _getName(long ptr);

    /**
     * Native pasteboard name getter;
     * @return name
     */
    public String getName() {
        assertValid();
        return _getName(this.ptr);
    }

    private native String[][] _getUTFs(long ptr);

    /**
     * @return an array of String arrays with multiple UTF types for each item
     * on the Pasteboard (allows for non homogenous items)
     * the length of the array is the count of available items
     */
    public String[][] getUTFs() {
        assertValid();
        return _getUTFs(this.ptr);
    }

    private native byte[] _getItemAsRawImage(long ptr, int index);

    /**
     * Get the item representation for the given index as raw pixels bytes suitable for Pixels
     * the platform will try to find the best representation
     */
    public byte[] getItemAsRawImage(int index) {
        assertValid();
        return _getItemAsRawImage(this.ptr, index);
    }


    private native String _getItemAsString(long ptr, int index);

    /**
     * get the item representation for the given index as String
     * the platform will try to find the best representation
     */
    public String getItemAsString(int index) {
        assertValid();
        return _getItemAsString(this.ptr, index);
    }


    private native String _getItemStringForUTF(long ptr, int index, String utf);

    /**
     * get the item representation for the given utf type as String
     */
    public String getItemStringForUTF(int index, String utf) {
        assertValid();
        return _getItemStringForUTF(this.ptr, index, utf);
    }


    private native byte[] _getItemBytesForUTF(long ptr, int index, String utf);

    /**
     * get the item representation for the given utf type as byte array
     */
    public byte[] getItemBytesForUTF(int index, String utf) {
        assertValid();
        return _getItemBytesForUTF(this.ptr, index, utf);
    }


    private native long _getItemForUTF(long ptr, int index, String utf);

    /**
     * get the item representation for the given utf type as native Objective-C object id
     * the item retain count will increase by 1
     * requires client to drop into Obj-C to do anything useful with the item
     */
    public long getItemForUTF(int index, String utf) {
        assertValid();
        return _getItemForUTF(this.ptr, index, utf);
    }

    // paste the items with their corresponding representations
    // returns seed
    //
    // ex of 2 items:
    // 1st item has 2 representations
    // 2nd item has 1 representation
    //  {
    //      {
    //          {UtfString, "/images/image.png"},
    //          {UtfFileUrl, "file:/images/image.png"}
    //      },
    //      {
    //          {UtfString, "text"}
    //      },
    //  }
    private native long _putItemsFromArray(long ptr, Object[] items, int supportedActions);
    public long putItemsFromArray(Object[] items, int supportedActions) {
        return _putItemsFromArray(this.ptr, items, supportedActions);
    }

    private Object[] hashMapToArray(HashMap hashmap) {
        Object[] array = null;
        if ((hashmap != null) && (hashmap.size() > 0)) {
            array = new Object[hashmap.size()];
            java.util.Set keys = hashmap.keySet();
            java.util.Iterator iterator = keys.iterator();
            int index = 0;
            while (iterator.hasNext() == true) {
                Object item[] = new Object[2];
                String utf = (String)iterator.next();
                item[IosPasteboard.UtfIndex] = utf;
                item[IosPasteboard.ObjectIndex] = hashmap.get(utf);
                array[index++] = item;
            }
        }
        return array;
    }

    public long putItems(HashMap<String,Object>[] items, int supportedActions) {
        assertValid();
        Object array[] = null;
        if (items.length > 0) {
            array = new Object[items.length];
            for (int i=0; i<items.length; i++) {
                array[i] = hashMapToArray(items[i]);
            }
        }
        return putItemsFromArray(array, supportedActions);
    }

    private native long _clear(long ptr);

    /**
     * clears pasteboard
     * @return seed
     */
    public long clear() {
        assertValid();
        return _clear(this.ptr);
    }


    private native long _getSeed(long ptr);

    /**
     * retrieve pasteboard seed
     */
    public long getSeed() {
        assertValid();
        return _getSeed(this.ptr);
    }

    // synthesized API
    private native int _getAllowedOperation(long ptr);

    /**
     * Check pasteboard-operation allowed in the current pasteboard state.
     * @return allowed operation
     */
    public int getAllowedOperation() {
        assertValid();
        return _getAllowedOperation(this.ptr);
    }

    private native void _release(long ptr);

    /**
     * Release native pasteboard instance.
     */
    public void release() {
        assertValid();
        if ((this.ptr != 0L) && (this.user == true)) {
            _release(ptr);
        }
        this.ptr = 0L;
    }

    private void assertValid() {
        if (this.ptr == 0L) {
            throw new IllegalStateException("The IosPasteboard is not valid");
        }
    }
}
