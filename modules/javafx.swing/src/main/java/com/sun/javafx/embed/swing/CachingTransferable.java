/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

/**
 * A Transferable implementation backed by a Map.
 * The data can be populated either from AWT Transferable
 * or from FX Clipboard.
 */
public class CachingTransferable implements Transferable {

    @Override
    public Object getTransferData(final DataFlavor flavor) throws UnsupportedEncodingException
    {
        String mimeType = DataFlavorUtils.getFxMimeType(flavor);
        return DataFlavorUtils.adjustFxData(
                flavor, getData(mimeType));
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        final String mimeTypes[] = getMimeTypes();
        return DataFlavorUtils.getDataFlavors(mimeTypes);
    }

    @Override
    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        return isMimeTypeAvailable(
                DataFlavorUtils.getFxMimeType(flavor));
    }

    private Map<String, Object> mimeType2Data = Collections.EMPTY_MAP;

    public void updateData(Transferable t, boolean fetchData) {
        final Map<String, DataFlavor> mimeType2DataFlavor =
                DataFlavorUtils.adjustSwingDataFlavors(
                t.getTransferDataFlavors());

        // If we keep reference to source Transferable in SwingDragSource and
        // call Transferable#getTransferData() on it from
        // SwingDragSource#getData() we may run into
        // "java.awt.dnd.InvalidDnDOperationException" issue as
        // SwingDragSource#getData() is called from FX user code and from
        // QuantumClipboard#getContent() (sik!). These calls usually take
        // place in the context of
        // EmbeddedSceneDTInterface#handleDragDrop() method as the
        // normal handling of DnD.
        // Instead of keeping reference to source Transferable we just read
        // all its data while in the context safe for calling
        // Transferable#getTransferData().
        //
        // This observation is true for standard AWT Transferable-s.
        // Things may be totally broken for custom Transferable-s though.

        // For performance reasons, the DRAG_ENTERED and DRAG_OVER event
        // handlers pass fetchData == false so as to update the set of
        // available MIME types only. The DRAG_DROPPED handler passes
        // fetchData == true which also fetches all the data.
        // NOTE: Due to JDK-8028585 this code won't be able to fetch data
        // when invoked from handlers other than DROPPED in any case.

        try {
            mimeType2Data = DataFlavorUtils.readAllData(t, mimeType2DataFlavor,
                    fetchData);
        } catch (Exception e) {
            mimeType2Data = Collections.EMPTY_MAP;
        }
    }

    public void updateData(Clipboard cb, boolean fetchData) {
        mimeType2Data = new HashMap<>();
        for (DataFormat f : cb.getContentTypes()) {
            mimeType2Data.put(DataFlavorUtils.getMimeType(f),
                    fetchData ? cb.getContent(f) : null);
        }
    }

    public Object getData(final String mimeType) {
        return mimeType2Data.get(mimeType);
    }

    public String[] getMimeTypes() {
        return mimeType2Data.keySet().toArray(new String[0]);
    }

    public boolean isMimeTypeAvailable(final String mimeType) {
        return Arrays.asList(getMimeTypes()).contains(mimeType);
    }
}
