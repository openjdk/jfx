/*
 * Copyright (c) 2012, Oracle  and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;


final class DataFlavorUtils {
    static String getFxMimeType(final DataFlavor flavor) {
        return flavor.getPrimaryType() + "/" + flavor.getSubType();
    }

    static Object adjustFxData(final DataFlavor flavor, final Object fxData)
            throws UnsupportedEncodingException {
        // TBD: Handle more data types!!!
        if (fxData instanceof String) {
            if (flavor.isRepresentationClassInputStream()) {
                final String encoding = flavor.getParameter("charset");
                return new ByteArrayInputStream(encoding != null
                        ? ((String) fxData).getBytes(encoding)
                        : ((String) fxData).getBytes());
            }
        }
        return fxData;
    }

    static Object adjustSwingData(final DataFlavor flavor,
                                  final Object swingData) {
        if (flavor.isFlavorJavaFileListType()) {
            // RT-12663
            final List<File> fileList = (List<File>)swingData;
            final String[] paths = new String[fileList.size()];
            int i = 0;
            for (File f : fileList) {
                paths[i++] = f.getPath();
            }
            return paths;
        }
        return swingData;
    }

    static Map<String, DataFlavor> adjustSwingDataFlavors(
            final DataFlavor[] flavors) {

        //
        // Group data flavors by FX mime type.
        //
        final Map<String, Set<DataFlavor>> mimeType2Flavors =
                new HashMap<String, Set<DataFlavor>>(flavors.length);
        for (DataFlavor flavor : flavors) {
            final String mimeType = getFxMimeType(flavor);
            if (mimeType2Flavors.containsKey(mimeType)) {
                final Set<DataFlavor> mimeTypeFlavors = mimeType2Flavors.get(
                        mimeType);
                try {
                    mimeTypeFlavors.add(flavor);
                } catch (UnsupportedOperationException e) {
                    // List of data flavors corresponding to FX mime
                    // type has been finalized already.
                }
            } else {
                Set<DataFlavor> mimeTypeFlavors = new HashSet<DataFlavor>();

                // If this is text data flavor use DataFlavor representing
                // a Java Unicode String class. This is what FX expects from
                // clipboard.
                if (flavor.isFlavorTextType()) {
                    mimeTypeFlavors.add(DataFlavor.stringFlavor);
                    mimeTypeFlavors = Collections.unmodifiableSet(
                            mimeTypeFlavors);
                } else {
                    mimeTypeFlavors.add(flavor);
                }

                mimeType2Flavors.put(mimeType, mimeTypeFlavors);
            }
        }

        //
        // Choose the best data flavor corresponding to the given FX mime type
        //
        final Map<String, DataFlavor> mimeType2Flavor =
                new HashMap<String, DataFlavor>();
        for (String mimeType : mimeType2Flavors.keySet()) {
            final DataFlavor[] mimeTypeFlavors = mimeType2Flavors.get(mimeType).
                    toArray(new DataFlavor[0]);
            if (mimeTypeFlavors.length == 1) {
                mimeType2Flavor.put(mimeType, mimeTypeFlavors[0]);
            } else {
                // TBD: something better!!!
                mimeType2Flavor.put(mimeType, mimeTypeFlavors[0]);
            }
        }

        return mimeType2Flavor;
    }

    static Map<String, Object> readAllData(final Transferable t) {
        final Map<String, DataFlavor> fxMimeType2DataFlavor =
                adjustSwingDataFlavors(t.getTransferDataFlavors());
        return readAllData(t, fxMimeType2DataFlavor);
    }
    
    static Map<String, Object> readAllData(final Transferable t,
                                           final Map<String, DataFlavor> fxMimeType2DataFlavor) {
        final Map<String, Object> fxMimeType2Data =
                new HashMap<String, Object>();
        
        for (Map.Entry<String, DataFlavor> e: fxMimeType2DataFlavor.entrySet()) {
            Object obj = null;
            try {
                obj = t.getTransferData(e.getValue());
            } catch (UnsupportedFlavorException ex) {
                // FIXME: report error
            } catch (IOException ex) {
                // FIXME: report error
            }

            if (obj != null) {
                obj = adjustSwingData(e.getValue(), obj);
                fxMimeType2Data.put(e.getKey(), obj);
            }
        }
        return fxMimeType2Data;
    }
}
