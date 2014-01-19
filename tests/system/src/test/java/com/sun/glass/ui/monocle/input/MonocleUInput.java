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

import com.sun.glass.ui.Application;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.glass.ui.monocle.linux.AbsoluteInputCapabilities;
import com.sun.glass.ui.monocle.linux.Input;
import com.sun.glass.ui.monocle.linux.LinuxInputDevice;
import com.sun.glass.ui.monocle.linux.LinuxInputDeviceRegistry;
import javafx.application.Platform;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

class MonocleUInput implements NativeUInput {

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

    private Pipe pipe;
    private final Map<String, BitSet> capabilities;
    private final Map<Integer, int[]> absCaps;
    private final Map<String, String> udevManifest;
    private final Map<String, String> uevent;
    private final ByteBuffer event;
    private LinuxInputDevice device;
    private final LinuxInputDeviceRegistry registry;

    MonocleUInput() {
        capabilities = new HashMap<String, BitSet>();
        absCaps = new HashMap<Integer, int[]>();
        udevManifest = new HashMap<String, String>();
        uevent = new HashMap<String, String>();
        event = ByteBuffer.allocate(EVENT_STRUCT_SIZE);
        event.order(ByteOrder.nativeOrder());
        registry = (LinuxInputDeviceRegistry)
                NativePlatformFactory.getNativePlatform().getInputDeviceRegistry();
    }

    private void reset() {
        capabilities.clear();
        absCaps.clear();
        uevent.clear();
        uevent.put("ID_INPUT", "1");
    }

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

    private Map<Integer, AbsoluteInputCapabilities> createAbsCapsMap() {
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

    @Override
    public void processLine0(String line) {
    }

    @Override
    public void processLine1(String line, String typeString) {
        switch (typeString) {
            case "OPEN":
                try {
                    pipe = Pipe.open();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "CREATE":
                Application.invokeAndWait(() -> {
                    device = registry.addDevice(
                    new LinuxInputDevice(capabilities,
                                         createAbsCapsMap(),
                                         pipe.source(),
                                         udevManifest,
                                         uevent),
                    "Simulated Linux Input Device");
                });
                break;
            case "DESTROY":
                try {
                    if (pipe != null) {
                        pipe.sink().close();
                        pipe.source().close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pipe = null;
                if (device != null) {
                    final LinuxInputDevice d = device;
                    Platform.runLater(() -> registry.removeDevice(d));
                    device = null;
                }
                break;
            case "CLOSE":
                break;
            default: processLine2(line, typeString, "0");
        }
    }

    @Override
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

    @Override
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
                    pipe.sink().write(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void setup() {
    }

    @Override
    public void init(boolean verbose) {
        reset();
    }

    @Override
    public void dispose() {
    }

    @Override
    public int writeTime(byte[] data, int offset) {
        Arrays.fill(data, offset, offset + 8, (byte) 0);
        return offset + 8;
    }

    @Override
    public int writeCode(byte[] data, int offset, String code) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 2);
        bb.order(ByteOrder.nativeOrder());
        bb.putShort(stringToShort(code));
        return offset + 2;
    }

    @Override
    public int writeValue(byte[] data, int offset, String value) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 4);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(valueToInt(value));
        return offset + 4;
    }

    @Override
    public int writeValue(byte[] data, int offset, int value) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 4);
        bb.order(ByteOrder.nativeOrder());
        bb.putInt(value);
        return offset + 4;
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        try {
            pipe.sink().write(ByteBuffer.wrap(data, offset, length));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
