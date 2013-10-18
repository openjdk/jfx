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

package com.sun.media.jfxmediaimpl.platform.java;

import com.sun.media.jfxmediaimpl.MetadataParserImpl;
import java.io.IOException;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmedia.logging.Logger;
import java.nio.charset.Charset;

final class FLVMetadataParser extends MetadataParserImpl {
    private int dataSize = 0;
    private static final String CHARSET_UTF_8 = "UTF-8";

    public FLVMetadataParser(Locator locator) {
        super(locator);
    }

    protected void parse() {
        try {
            // Check for header (FLV)
            if (getNextByte() == 0x46 && getNextByte() == 0x4C && getNextByte() == 0x56) {
                skipBytes(2);
                int dataOffset = getInteger();
                skipBytes(dataOffset - 9); // Skip rest of header

                int tagCount = 0; // process up to ten tags, then fail gracefully
                for (tagCount = 0; tagCount < 10; tagCount++) {
                    skipBytes(4); // previous tag size

                    byte tagType = getNextByte();
                    dataSize = getU24();
                    skipBytes(7); // Skip rest of tag header

                    if (tagType == 0x12) {
                        int expectedEndPosition = getStreamPosition() + dataSize;
                        // process SCRIPT_DATA tag
                        if (parseDataTag()) {
                            break;
                        }

                        // make sure we're aligned with the next tag (prev tag size)
                        if (getStreamPosition() < expectedEndPosition) {
                            skipBytes(expectedEndPosition - getStreamPosition());
                        }
                    } else {
                        skipBytes(dataSize);
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    /* returns true if it parsed onMetaData */
    private boolean parseDataTag() throws IOException {
        if (dataSize < 14) {
            return false; // not large enough for onMetaData
        }

        // read the tag header into memory
        byte[] header = new byte[14];
        for (int ii = 0; ii < 14; ii++) {
            header[ii] = getNextByte();
        }
        // validate header
        if (header[0] != 2) {
            return false;
        }

        int nameSize = (header[1] & 0xff) << 8 | (header[2] & 0xff);
        if (nameSize != 10) {
            return false; // "onMetaData" name length
        }
        if (!Charset.isSupported(CHARSET_UTF_8)) {
            return false;
        }
        String methodName = new String(header, 3, nameSize, Charset.forName(CHARSET_UTF_8));
        if (!methodName.equals("onMetaData")) {
            return false;
        }

        // check type, it must be ECMA_ARRAY
        if (header[13] != FlvDataValue.ECMA_ARRAY) {
            if (Logger.canLog(Logger.WARNING)) {
                Logger.logMsg(Logger.WARNING, "FLV metadata must be in an ECMA array");
            }
            return false;
        }

        // Now buffer the entire metadata tag and process it from memory
        // this will avoid buffer issues if the metadata is malformed or corrupt
        startRawMetadata(dataSize);
        if (null == rawMetaBlob) {
            if (Logger.canLog(Logger.DEBUG)) {
                Logger.logMsg(Logger.DEBUG, "Unable to allocate buffer for FLV metadata");
            }
            return false;
        }

        // load it up
        stuffRawMetadata(header, 0, 14);
        readRawMetadata(dataSize-14);

        // now parse from rawMetaBlob
        setParseRawMetadata(true);
        skipBytes(14); // reposition to the ECMA array size field

        // OMD is always an ECMA array, so just process it here
        try {
            FlvDataValue flvValue;
            int arrayCount = getInteger(); // ECMA array element count
            int parseCount = 0;
            boolean done = false;
            boolean warnMalformed = false;

            do {
                String attribute = getString(getShort(), Charset.forName(CHARSET_UTF_8));
                flvValue = readDataValue(false);
                parseCount++;
                String tag = convertTag(attribute);
                if (Logger.canLog(Logger.DEBUG) && !attribute.equals("")) {
                    Logger.logMsg(Logger.DEBUG, parseCount+": \""+attribute+"\" -> "
                            +(null == tag ? "(unsupported)" : ("\""+tag+"\"")));
                }
                if (tag != null) {
                    Object value = convertValue(attribute, flvValue.obj);
                    if (value != null) {
                        addMetadataItem(tag, value);
                    }
                }

                /* if the array contains an end of object marker, then it will be
                 * included in the count
                 * There are cases where the EOO marker is not present so we
                 * must track the number of objects we parse and terminate properly.
                 */

                // we have to decide whether to end or not
                if (parseCount >= arrayCount) {
                    // there may be more data, in spite of what the count was
                    if (getStreamPosition() < dataSize) {
                        if (!warnMalformed && Logger.canLog(Logger.WARNING)) {
                            Logger.logMsg(Logger.WARNING, "FLV Source has malformed metadata, invalid ECMA element count");
                            warnMalformed = true;
                        }
                    } else {
                        done = true;
                    }
                }
            } while (!done);
        } catch (Exception e) {
            // fail gracefully, probably just hit the end of the buffer
            if (Logger.canLog(Logger.WARNING)) {
                Logger.logMsg(Logger.WARNING, "Exception while processing FLV metadata: "+e);
            }
        } finally {
            if (null != rawMetaBlob) {
                setParseRawMetadata(false);
                addRawMetadata(RAW_FLV_METADATA_NAME);
                disposeRawMetadata();
            }
            done();
        }

        return true;
    }

    private FlvDataValue readDataValue(boolean hasName) throws IOException {
        FlvDataValue sdv = new FlvDataValue();

        if (hasName) {
            skipBytes(getShort()); // skip the property/element name
        }

        sdv.type = getNextByte();
        switch (sdv.type) {
            case FlvDataValue.NUMBER:
                sdv.obj = getDouble();
                break;
            case FlvDataValue.BOOLEAN:
                boolean b = (getNextByte() != 0);
                sdv.obj = b;
                break;
            case FlvDataValue.STRING:
                sdv.obj = getString(getShort(), Charset.forName(CHARSET_UTF_8));
                break;
            case FlvDataValue.OBJECT:
                skipObject();
                break;
            case FlvDataValue.MOVIE_CLIP:
                getString(getShort(), Charset.forName(CHARSET_UTF_8));
                break;
            case FlvDataValue.NULL:
                break;
            case FlvDataValue.UNDEFINED:
                break;
            case FlvDataValue.REFERENCE:
                skipBytes(2);
                break;
            case FlvDataValue.ECMA_ARRAY:
                skipArray();
                break;
            case FlvDataValue.END_OF_DATA:
                sdv.scriptDataObjectEnd = true;
                break;
            case FlvDataValue.STRICT_ARRAY:
                skipStrictArray();
                break;
            case FlvDataValue.DATE:
                sdv.obj = getDouble();
                skipBytes(2); // Skip LocalDateTimeOffset
                break;
            case FlvDataValue.LONG_STRING:
                sdv.obj = getString(getInteger(), Charset.forName(CHARSET_UTF_8));
                break;
            default:
                break;
        }

        return sdv;
    }

    private void skipObject() throws IOException {
        // Objects define a list of serialized properties, just skip them
        FlvDataValue value;
        do {
            value = readDataValue(true);
        } while (!value.scriptDataObjectEnd);
    }

    private void skipArray() throws IOException {
        // Some files have invalid ECMA arrays, they are not terminated with
        // an EOO marker, so we have to track the number of objects we're parsing
        int arrayCount = getInteger(); // ECMA array element count
        for (int parseCount = 0; parseCount < arrayCount; parseCount++) {
            readDataValue(true);
            // don't even bother processing the EOO marker, since it will be counted, usually
        }
    }

    private void skipStrictArray() throws IOException {
        long arrayLen = getInteger();

        for (int i = 0; i < arrayLen; i++) {
            readDataValue(false);
        }
    }

    private String convertTag(String tag) {
        if (tag.equals("duration")) {
            return MetadataParserImpl.DURATION_TAG_NAME;
        } else if (tag.equals("width")) {
            return MetadataParserImpl.WIDTH_TAG_NAME;
        } else if (tag.equals("height")) {
            return MetadataParserImpl.HEIGHT_TAG_NAME;
        } else if (tag.equals("framerate")) {
            return MetadataParserImpl.FRAMERATE_TAG_NAME;
        } else if (tag.equals("videocodecid")) {
            return MetadataParserImpl.VIDEOCODEC_TAG_NAME;
        } else if (tag.equals("audiocodecid")) {
            return MetadataParserImpl.AUDIOCODEC_TAG_NAME;
        } else if (tag.equals("creationdate")) {
            return MetadataParserImpl.CREATIONDATE_TAG_NAME;
        }

        return null;
    }

    private static class FlvDataValue {
        static final byte NUMBER = 0;
        static final byte BOOLEAN = 1;
        static final byte STRING = 2;
        static final byte OBJECT = 3;
        static final byte MOVIE_CLIP = 4;
        static final byte NULL = 5;
        static final byte UNDEFINED = 6;
        static final byte REFERENCE = 7;
        static final byte ECMA_ARRAY = 8;
        static final byte END_OF_DATA = 9; // Really only after null string
        static final byte STRICT_ARRAY = 10;
        static final byte DATE = 11;
        static final byte LONG_STRING = 12;
        boolean scriptDataObjectEnd = false;
        Object obj;
        byte type;
    }
}
