/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl;

import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.ListIterator;

import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.locator.ConnectionHolder;
import com.sun.media.jfxmedia.events.MetadataListener;
import java.nio.charset.Charset;
import java.util.Collections;

public abstract class MetadataParserImpl extends Thread implements com.sun.media.jfxmedia.MetadataParser {
    private final List<WeakReference<MetadataListener>> listeners = new ArrayList<WeakReference<MetadataListener>>();
    private Map<String, Object> metadata = new HashMap<String, Object>();
    private Locator locator = null;
    private ConnectionHolder connectionHolder = null;
    private ByteBuffer buffer = null;
    private Map<String,ByteBuffer> rawMetaMap = null;
    protected ByteBuffer rawMetaBlob = null;
    private boolean parsingRawMetadata = false;
    private int length = 0;
    private int index = 0;
    private int streamPosition = 0;

    public MetadataParserImpl(Locator locator) {
        this.locator = locator;
    }

    public void addListener(MetadataListener listener) {
        synchronized (listeners) {
            if (listener != null) {
                listeners.add(new WeakReference<MetadataListener>(listener));
            }
        }
    }

    public void removeListener(MetadataListener listener) {
        synchronized (listeners) {
            if (listener != null) {
                for (ListIterator<WeakReference<MetadataListener>> it = listeners.listIterator(); it.hasNext();) {
                    MetadataListener l = it.next().get();
                    if (l == null || l == listener) {
                        it.remove();
                    }
                }
            }
        }
    }

    public void startParser() throws IOException {
        start();
    }

    public void stopParser() {
        if (connectionHolder != null) {
            connectionHolder.closeConnection();
        }
    }

    @Override
    public void run() {
        try {
            connectionHolder = locator.createConnectionHolder();
            parse();
        } catch (IOException e) {
        }
    }

    abstract protected void parse();

    protected void addMetadataItem(String tag, Object value) {
        metadata.put(tag, value);
    }

    protected void done() {
        synchronized (listeners) {
            if (!metadata.isEmpty()) {
                for (ListIterator<WeakReference<MetadataListener>> it = listeners.listIterator(); it.hasNext();) {
                    MetadataListener l = it.next().get();
                    if (l != null) {
                        l.onMetadata(metadata);
                    } else {
                        it.remove();
                    }
                }
            }
        }
    }

    protected int getStreamPosition() {
        if (parsingRawMetadata) {
            return rawMetaBlob.position();
        }
        return streamPosition;
    }

    protected void startRawMetadata(int sizeHint) {
        rawMetaBlob = ByteBuffer.allocate(sizeHint);
    }

    private void adjustRawMetadataSize(int addSize) {
        // resize if necessary (expensive!)
        if (rawMetaBlob.remaining() < addSize) {
            int pos = rawMetaBlob.position();
            int newSize = pos + addSize;
            ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
            rawMetaBlob.position(0);
            newBuffer.put(rawMetaBlob.array(), 0, pos);
            rawMetaBlob = newBuffer;
        }
    }

    /* Read from the source stream directly into the raw metadata blob */
    protected void readRawMetadata(int size) throws IOException {
        byte[] data = getBytes(size);
        adjustRawMetadataSize(size);
        if (null != data) {
            rawMetaBlob.put(data);
        }
    }

    /* Use this to put data that's already been read back into the metadata blob */
    protected void stuffRawMetadata(byte[] data, int offset, int size) {
        if (null != rawMetaBlob) {
            adjustRawMetadataSize(size);
            rawMetaBlob.put(data, offset, size);
        }
    }

    protected void disposeRawMetadata() {
        parsingRawMetadata = false;
        rawMetaBlob = null;
    }

    // Switch from reading from the soure to using the raw metadata blob
    protected void setParseRawMetadata(boolean state) {
        if (null == rawMetaBlob) {
            parsingRawMetadata = false;
            return;
        }

        if (state) {
            rawMetaBlob.position(0);
        }
        parsingRawMetadata = state;
    }

    // Add the current raw metadata blob to the metadata map
    protected void addRawMetadata(String type) {
        if (null == rawMetaBlob) {
            return;
        }

        if (null == rawMetaMap) {
            rawMetaMap = new HashMap<String,ByteBuffer>();
            // make sure the map we add to the metadata is read-only
            metadata.put(RAW_METADATA_TAG_NAME, Collections.unmodifiableMap(rawMetaMap));
        }
        rawMetaMap.put(type, rawMetaBlob.asReadOnlyBuffer());
    }

    protected void skipBytes(int num) throws IOException, EOFException {
        if (parsingRawMetadata) {
            rawMetaBlob.position(rawMetaBlob.position()+num);
            return;
        }

        for (int i = 0; i < num; i++) {
            getNextByte();
        }
    }

    protected byte getNextByte() throws IOException, EOFException {
        if (parsingRawMetadata) {
            // read from the raw metadata blob, not from the stream
            return rawMetaBlob.get();
        }

        if (buffer == null) {
            buffer = connectionHolder.getBuffer();
            length = connectionHolder.readNextBlock();
        }

        if (index >= length) {
            length = connectionHolder.readNextBlock();
            if (length < 1) {
                throw new EOFException();
            }
            index = 0;
        }

        byte b = buffer.get(index);
        index++;
        streamPosition++;
        return b;
    }

    protected byte[] getBytes(int size) throws IOException, EOFException {
        byte[] bytes = new byte[size];

        if (parsingRawMetadata) {
            rawMetaBlob.get(bytes);
            return bytes;
        }

        for (int i = 0; i < size; i++) {
            bytes[i] = getNextByte();
        }

        return bytes;
    }

    protected long getLong() throws IOException, EOFException {
        if (parsingRawMetadata) {
            return rawMetaBlob.getLong();
        }

        long value = 0;

        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);

        return value;
    }

    protected int getInteger() throws IOException, EOFException {
        if (parsingRawMetadata) {
            return rawMetaBlob.getInt();
        }

        int value = 0;

        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);
        value = value << 8;
        value |= (getNextByte() & 0xFF);

        return value;
    }

    protected short getShort() throws IOException, EOFException {
        if (parsingRawMetadata) {
            return rawMetaBlob.getShort();
        }

        short value = 0;

        value |= (getNextByte() & 0xFF);
        value = (short) (value << 8);
        value |= (getNextByte() & 0xFF);

        return value;
    }

    protected double getDouble() throws IOException, EOFException {
        if (parsingRawMetadata) {
            return rawMetaBlob.getDouble();
        }

        long bits = getLong();
        return Double.longBitsToDouble(bits);
    }

    protected String getString(int length, Charset charset) throws IOException, EOFException {
        byte[] bytes = getBytes(length);
        return new String(bytes, 0, length, charset);
    }

    protected int getU24() throws IOException, EOFException {
        int value = 0;

        value |= (getNextByte() & 0xFF);
        value = (int) (value << 8);
        value |= (getNextByte() & 0xFF);
        value = (int) (value << 8);
        value |= (getNextByte() & 0xFF);

        return value;
    }

    // XXX Change hard-coded strings to constants defined in MetadataParser.
    protected Object convertValue(String tag, Object value) {
        if (tag.equals("duration") && value instanceof Double) {
            Double v = ((Double) value * 1000);
            return v.longValue();
        } else if (tag.equals("duration") && value instanceof String) {
            String v = (String) value;
            return Long.valueOf(v.trim());
        } else if (tag.equals("audiocodecid")) {
            // XXX hard-coded
            return "MPEG 1 Audio";
        } else if (tag.equals("creationdate")) {
            return ((String) value).trim();
        } else if (tag.equals("track number") || tag.equals("disc number")) {
            String[] v = ((String) value).split("/");
            if (v.length == 2) {
                return Integer.valueOf(v[0].trim());
            }
        } else if (tag.equals("track count") || tag.equals("disc count")) {
            String[] tc = ((String) value).split("/");
            if (tc.length == 2) {
                return Integer.valueOf(tc[1].trim());
            }
        } else if (tag.equals("album")) {
            return value;
        } else if (tag.equals("artist")) {
            return value;
        } else if (tag.equals("genre")) {
            return value;
        } else if (tag.equals("title")) {
            return value;
        } else if (tag.equals("album artist")) {
            return value;
        } else if (tag.equals("comment")) {
            return value;
        } else if (tag.equals("composer")) {
            return value;
        } else if (tag.equals("year")) {
            String v = (String) value;
            return Integer.valueOf(v.trim());
        }

        return null;
    }
}
