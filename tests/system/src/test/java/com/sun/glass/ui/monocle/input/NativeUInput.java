/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.ui.monocle.linux.AbsoluteInputCapabilities;
import com.sun.glass.ui.monocle.linux.Input;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public abstract class NativeUInput {

    static final int EVENT_STRUCT_SIZE = 16;
    private static final int EVENT_STRUCT_TYPE_INDEX = 8;
    private static final int EVENT_STRUCT_CODE_INDEX = 10;
    private static final int EVENT_STRUCT_VALUE_INDEX = 12;
    private static final int INDEX_VALUE = 0;
    private static final int INDEX_MIN = 1;
    private static final int INDEX_MAX = 2;
    private static final int INDEX_FUZZ = 3;
    private static final int INDEX_FLAT = 4;
    private static final int INDEX_RESOLUTION = 5;
    private static final int INDEX_COUNT = 6;
    protected final Map<String, BitSet> capabilities;
    protected final Map<Integer, int[]> absCaps;
    protected final Map<String, String> udevManifest;
    protected final Map<String, String> uevent;
    protected final ByteBuffer event;

    public NativeUInput() {
        event = ByteBuffer.allocateDirect(EVENT_STRUCT_SIZE);
        event.order(ByteOrder.nativeOrder());
        absCaps = new HashMap<Integer, int[]>();
        uevent = new HashMap<String, String>();
        capabilities = new HashMap<String, BitSet>();
        udevManifest = new HashMap<String, String>();
    }

    public void processLine3(String line, String typeString,
                      String codeString, String valueString) {
        switch (typeString) {
            case "PROPERTY": udevManifest.put(codeString, valueString); break;
            case "ABSMIN": addAbsCap(INDEX_MIN, codeString, valueString); break;
            case "ABSMAX": addAbsCap(INDEX_MAX, codeString, valueString); break;
            case "ABSFUZZ": addAbsCap(INDEX_FUZZ, codeString, valueString); break;
            case "ABSFLAT": addAbsCap(INDEX_FLAT, codeString, valueString); break;
            default:
                try {
                    short type = stringToShort(typeString);
                    short code = stringToShort(codeString);
                    int value = valueToInt(valueString);
                    event.rewind();
                    event.limit(event.capacity());
                    event.putShort(EVENT_STRUCT_TYPE_INDEX, type);
                    event.putShort(EVENT_STRUCT_CODE_INDEX, code);
                    event.putInt(EVENT_STRUCT_VALUE_INDEX, value);
                    write(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public abstract void setup();

    public void init() {
        capabilities.clear();
        absCaps.clear();
        uevent.clear();
        uevent.put("ID_INPUT", "1");
    }

    public abstract void dispose();

    private static short stringToShort(String s) {
        try {
            if (s.startsWith("0x")) {
                return Short.parseShort(s.substring(2), 16);
            } else if (s.length() > 0 && s.charAt(0) >= '0' && s.charAt(0) <= '9') {
                return Short.parseShort(s);
            }
            Field f = Input.class.getField(s);
            return f.getShort(null);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int valueToInt(String s) {
        try {
            if (s.startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            } else {
                return Integer.parseInt(s);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void addCapability(String cap, String codeString) {
        short code = stringToShort(codeString);
        BitSet bitSet = capabilities.get(cap);
        if (bitSet == null) {
            bitSet = new BitSet();
            capabilities.put(cap, bitSet);
        }
        bitSet.set(code);
    }

    private void addAbsCap(int index, String codeString, String valueString) {
        int axis = stringToShort(codeString);
        int[] absCap = absCaps.get(axis);
        if (absCap == null) {
            absCap = new int[INDEX_COUNT];
            absCaps.put(axis, absCap);
        }
        absCap[index] = valueToInt(valueString);
    }

    public Map<Integer, AbsoluteInputCapabilities> createAbsCapsMap() {
        Map<Integer, AbsoluteInputCapabilities> map =
                new HashMap<Integer, AbsoluteInputCapabilities>();
        for (Integer axis : absCaps.keySet()) {
            int[] a = absCaps.get(axis);
            if (a != null) {
                AbsoluteInputCapabilities absCap = new AbsoluteInputCapabilities(
                        a[INDEX_VALUE],
                        a[INDEX_MAX],
                        a[INDEX_MIN],
                        a[INDEX_FUZZ],
                        a[INDEX_FLAT],
                        a[INDEX_RESOLUTION]);
                map.put(axis, absCap);
            }
        }
        return map;
    }

    protected abstract void createDevice() throws IOException;

    protected abstract void destroyDevice() throws IOException;

    protected abstract void openConnection() throws IOException;

    protected abstract void closeConnection() throws IOException;

    public void processLine1(String line, String typeString) {
        try {
            switch (typeString) {
                case "OPEN":
                    openConnection();
                    break;
                case "CREATE":
                    createDevice();
                    break;
                case "DESTROY":
                    destroyDevice();
                    break;
                case "CLOSE":
                    closeConnection();
                    break;
                default: processLine2(line, typeString, "0");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processLine2(String line, String typeString, String codeString) {
        switch (typeString) {
            case "ABSBIT": addCapability("abs", codeString); break;
            case "EVBIT": addCapability("ev", codeString); break;
            case "FFBIT": addCapability("ff", codeString); break;
            case "KEYBIT": addCapability("key", codeString); break;
            case "LEDBIT": addCapability("led", codeString); break;
            case "MSCBIT": addCapability("msc", codeString); break;
            case "RELBIT": addCapability("rel", codeString); break;
            case "SNDBIT": addCapability("snd", codeString); break;
            case "SWBIT": addCapability("sw", codeString); break;
            case "PROPBIT":
            case "VENDOR":
            case "PRODUCT":
            case "VERSION":
                break; // ignore
            default: processLine3(line, typeString, codeString, "0");
        }
    }

    public int writeTime(byte[] data, int offset) {
        Arrays.fill(data, offset, offset + 8, (byte) 0);
        return offset + 8;
    }

    public int writeCode(byte[] data, int offset, String code) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 2);
        bb.order(ByteOrder.nativeOrder());
        bb.putShort(stringToShort(code));
        return offset + 2;
    }

    public int writeValue(byte[] data, int offset, String value) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 4);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(valueToInt(value));
        return offset + 4;
    }

    public int writeValue(byte[] data, int offset, int value) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 4);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(value);
        return offset + 4;
    }

    public abstract void write(ByteBuffer buffer) throws IOException;

    public void write(byte[] data, int offset, int length) {
        try {
            while (length > 0) {
                event.clear();
                int count = Math.min(length, event.capacity());
                event.put(data, offset, count);
                event.flip();
                write(event);
                offset += count;
                length -= count;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
