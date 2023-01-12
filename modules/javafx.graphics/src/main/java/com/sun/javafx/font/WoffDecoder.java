/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.zip.Inflater;
import com.sun.javafx.font.FontFileReader.Buffer;

class WoffDecoder extends FontFileWriter {
    WoffHeader woffHeader;
    WoffDirectoryEntry[] woffTableDirectory;

    public WoffDecoder() {
        super();
    }

    public void decode(FontFileReader input) throws Exception {
        input.reset();
        initWoffTables(input);
        if (woffHeader == null || woffTableDirectory == null) {
            throw new Exception("WoffDecoder: failure reading header");
        }
        int format = woffHeader.flavor;
        if (format != v1ttTag && format != trueTag && format != ottoTag) {
            throw new Exception("WoffDecoder: invalid flavor");
        }

        /* write header */
        short numTables = woffHeader.numTables;
        setLength(woffHeader.totalSfntSize);
        writeHeader(format, numTables);

        /* Tables should be written in the same order as the original file */
        Arrays.sort(woffTableDirectory, (o1, o2) -> o1.offset - o2.offset);

        Inflater decompressor = new Inflater();
        int offset = TTCHEADERSIZE + numTables * DIRECTORYENTRYSIZE;
        for (int i = 0; i < woffTableDirectory.length; i++) {
            WoffDirectoryEntry table = woffTableDirectory[i];

            /* write directory entry (in the same order as it appeared in the
             * woff file).
             */
            writeDirectoryEntry(table.index, table.tag, table.origChecksum,
                                offset, table.origLength);

            /* write table */
            Buffer buffer = input.readBlock(table.offset, table.comLength);
            byte[] bytes = new byte[table.comLength];
            buffer.get(0, bytes, 0, table.comLength);
            if (table.comLength != table.origLength) {
                decompressor.setInput(bytes);
                byte[] output = new byte[table.origLength];
                int length = decompressor.inflate(output);
                if (length != table.origLength) {
                    throw new Exception("WoffDecoder: failure expanding table");
                }
                decompressor.reset();
                bytes = output;
            }
            seek(offset);
            writeBytes(bytes);

            offset += (table.origLength + 3) & ~3;
        }
        decompressor.end();
    }

    void initWoffTables(FontFileReader input) throws Exception {
        long filesize = input.getLength();
        if (filesize < WOFFHEADERSIZE) {
            throw new Exception("WoffDecoder: invalid filesize");
        }
        Buffer buffer = input.readBlock(0, WOFFHEADERSIZE);
        WoffHeader header = new WoffHeader(buffer);
        short numTables = header.numTables;
        if (header.signature != woffTag) {
            throw new Exception("WoffDecoder: invalid signature");
        }
        if (header.reserved != 0) {
            throw new Exception("WoffDecoder: invalid reserved != 0");
        }
        if (filesize < WOFFHEADERSIZE + numTables * WOFFDIRECTORYENTRYSIZE) {
            throw new Exception ("WoffDecoder: invalid filesize");
        }
        WoffDirectoryEntry table;
        WoffDirectoryEntry[] tableDirectory = new WoffDirectoryEntry[numTables];
        int headerOffset = WOFFHEADERSIZE + numTables * WOFFDIRECTORYENTRYSIZE;
        int size = TTCHEADERSIZE + numTables * DIRECTORYENTRYSIZE;
        buffer = input.readBlock(WOFFHEADERSIZE, numTables * WOFFDIRECTORYENTRYSIZE);
        int prevTag = 0;
        for (int i=0; i<numTables; i++) {
            tableDirectory[i] = table = new WoffDirectoryEntry(buffer, i);
            if (table.tag <= prevTag) {
                throw new Exception("WoffDecoder: table directory " +
                                    "not ordered by tag");
            }
            int startOffset = table.offset;
            int endOffset = table.offset + table.comLength;
            if (!(headerOffset <= startOffset && startOffset <= filesize)) {
                throw new Exception("WoffDecoder: invalid table offset");
            }
            if (!(startOffset <= endOffset && endOffset <= filesize)) {
                throw new Exception("WoffDecoder: invalid table offset");
            }
            if (table.comLength > table.origLength) {
                throw new Exception("WoffDecoder: invalid compressed length");
            }
            size += (table.origLength + 3) & ~3;
            if (size > header.totalSfntSize) {
                throw new Exception("WoffDecoder: invalid totalSfntSize");
            }
        }
        if (size != header.totalSfntSize) {
            throw new Exception("WoffDecoder: invalid totalSfntSize");
        }
        this.woffHeader = header;
        this.woffTableDirectory = tableDirectory;
    }

    static class WoffHeader {
        int signature;
        int flavor;
        int length;
        short numTables;
        short reserved;
        int totalSfntSize;
        short majorVersion;
        short minorVersion;
        int metaOffset;
        int metaLength;
        int metaOrigLength;
        int privateOffset;
        int privateLength;
        WoffHeader(Buffer buffer) {
            signature = buffer.getInt();
            flavor = buffer.getInt();
            length = buffer.getInt();
            numTables = buffer.getShort();
            reserved = buffer.getShort();
            totalSfntSize = buffer.getInt();
            majorVersion = buffer.getShort();
            minorVersion = buffer.getShort();
            metaOffset = buffer.getInt();
            metaLength = buffer.getInt();
            metaOrigLength = buffer.getInt();
            privateOffset = buffer.getInt();
            privateLength = buffer.getInt();
        }
    }

    static class WoffDirectoryEntry {
        int tag;
        int offset;
        int comLength;
        int origLength;
        int origChecksum;
        int index;//not part of the spec
        WoffDirectoryEntry(Buffer buffer, int index) {
            tag   =  buffer.getInt();
            offset = buffer.getInt();
            comLength = buffer.getInt();
            origLength = buffer.getInt();
            origChecksum = buffer.getInt();
            this.index = index;
        }
    }

}
