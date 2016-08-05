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

package com.sun.javafx.font;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.sun.glass.utils.NativeLibLoader;

class DFontDecoder extends FontFileWriter {
    static {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            NativeLibLoader.loadLibrary("javafx_font");
            return null;
        });
    }
    private native static long createCTFont(String fontName);
    private native static void releaseCTFont(long font);
    private native static int getCTFontFormat(long font);
    private native static int[] getCTFontTags(long font);
    private native static byte[] getCTFontTable(long font, int tag);

    public DFontDecoder() {
        super();
    }

    public void decode(String fontName) throws IOException {
        if (fontName == null) {
            throw new IOException("Invalid font name");
        }
        long fontRef = 0;
        try {
            fontRef = DFontDecoder.createCTFont(fontName);
            if (fontRef == 0) {
                throw new IOException("Failure creating CTFont");
            }
            int format = DFontDecoder.getCTFontFormat(fontRef);
            if (format != trueTag && format != v1ttTag && format != ottoTag) {
                throw new IOException("Unsupported Dfont");
            }
            int[] tags = DFontDecoder.getCTFontTags(fontRef);
            short numTables = (short)tags.length;
            int size = TTCHEADERSIZE + (DIRECTORYENTRYSIZE * numTables);
            byte[][] tableData = new byte[numTables][];
            for (int i = 0; i < tags.length; i++) {
                int tag = tags[i];
                tableData[i] = DFontDecoder.getCTFontTable(fontRef, tag);
                int length = tableData[i].length;
                size += (length + 3) & ~3;
            }
            DFontDecoder.releaseCTFont(fontRef);
            fontRef = 0;

            /* write header */
            setLength(size);
            writeHeader(format, numTables);

            int dataOffset = TTCHEADERSIZE + (DIRECTORYENTRYSIZE * numTables);
            for (int i = 0; i < numTables; i++) {
                int tag = tags[i];
                byte[] data = tableData[i];

                /* write directory entry */
                writeDirectoryEntry(i, tag, 0, dataOffset, data.length);

                /* write table */
                seek(dataOffset);
                writeBytes(data);

                dataOffset += (data.length + 3) & ~3;
            }

        } finally {
            if (fontRef != 0) {
                DFontDecoder.releaseCTFont(fontRef);
            }
        }
    }
}
