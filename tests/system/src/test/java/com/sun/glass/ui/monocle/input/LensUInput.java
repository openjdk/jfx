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

import com.sun.glass.ui.monocle.linux.Input;
import com.sun.glass.ui.monocle.linux.LinuxSystem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

public class LensUInput extends NativeUInput {

    private LinuxSystem system = LinuxSystem.getLinuxSystem();

    private long fd = -1;
    private long devFD = -1;
    private String deviceName;
    private short bustype, product, vendor, version;
    private String devNode;
    private static int devNodeSuffix = 0;

    private static final String INPUT_PATH = "/tmp/testInput";
    private static final String DEVNODE_PREFIX = "/tmp/input";

    private static ByteBuffer byteBuffer = ByteBuffer.allocateDirect(256)
            .order(ByteOrder.nativeOrder());
    private static boolean isSetup;

    @Override
    public void setup() {
        if (isSetup) {
            return;
        }
        try {
            system.loadLibrary();
            system.setenv("LENS_TEST_INPUT", INPUT_PATH, true);
            File pipe = new File(INPUT_PATH);
            if (pipe.exists()) {
                pipe.delete();
            }
            pipe.deleteOnExit();
            if (system.mkfifo(pipe.getPath(), LinuxSystem.S_IRWXU) != 0) {
                throw new IOException(system.getErrorMessage());
            }
            isSetup = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        super.init();
        fd = -1;
        devFD = -1;
        deviceName = "Test Input";
        bustype = 0x18; // BUS_USB
        product = 0x01;
        vendor = 0x01;
        version = 0x01;
    }

    @Override
    public void dispose() {
    }

    private void writeBytes(int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int bytesWritten = (int) system.write(fd, byteBuffer, offset, length);
            if (bytesWritten < 0) {
                throw new IOException(system.getErrorMessage());
            } else {
                offset += bytesWritten;
            }
        }
    }

    private void writeInt(int i) throws IOException {
        byteBuffer.putInt(0, i);
        writeBytes(4);
    }

    private void writeShort(short s) throws IOException {
        byteBuffer.putShort(0, s);
        writeBytes(2);
    }

    private void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes("UTF-8");
        int offset = 0;
        while (offset < bytes.length) {
            int length = Math.min(byteBuffer.capacity(), bytes.length - offset);
            byteBuffer.clear();
            byteBuffer.put(bytes, offset, length);
            writeBytes(length);
            offset += length;
        }
        // add a terminating null byte
        byteBuffer.put(0, (byte) 0);
        writeBytes(1);
    }

    @Override
    protected void createDevice() throws IOException {
        devNode = DEVNODE_PREFIX + (++devNodeSuffix);
        File devNodeFile = new File(devNode);
        if (devNodeFile.exists()) {
            devNodeFile.delete();
        }
        if (system.mkfifo(devNode, LinuxSystem.S_IRWXU) != 0) {
            throw new IOException(system.getErrorMessage());
        }
        writeInt(1); // attach device
        writeShort(bustype);
        writeShort(product);
        writeShort(vendor);
        writeShort(version);
        writeString(deviceName);
        writeString(devNode);
        writeString("Test Input Device");
        BitSet evBits = capabilities.get("ev");
        if (evBits != null) {
            for (int i = 0; i < Input.EV_MAX; i++) {
                if (evBits.get(i)) {
                    writeInt(i);
                }
            }
        }
        writeInt(-1);
        BitSet keyBits = capabilities.get("key");
        if (keyBits != null) {
            for (int i = 0; i < Input.KEY_MAX; i++) {
                if (keyBits.get(i)) {
                    writeInt(i);
                }
            }
        }
        writeInt(-1);
        BitSet relBits = capabilities.get("rel");
        if (relBits != null) {
            for (int i = 0; i < Input.REL_MAX; i++) {
                if (relBits.get(i)) {
                    writeInt(i);
                }
            }
        }
        writeInt(-1);
        BitSet absBits = capabilities.get("abs");
        if (absBits != null) {
            for (int i = 0; i < Input.ABS_MAX; i++) {
                if (absBits.get(i)) {
                    writeInt(i);
                    int[] caps = absCaps.get(i);
                    if (caps == null) {
                        caps = new int[6];
                    }
                    for (int val : caps) {
                        writeInt(val);
                    }
                }
            }
        }
        writeInt(-1);
        for (String key : udevManifest.keySet()) {
            writeString(key);
            writeString(udevManifest.get(key));
        }
        writeString("");
        devFD = openPipe(devNode);
    }

    @Override
    protected void destroyDevice() throws IOException {
        if (devFD != -1l) {
            system.close(devFD);
            devFD = -1;
        }
        new File(devNode).delete();
        writeInt(2); // detach device
        writeString(devNode);
    }

    private long openPipe(String path) throws IOException {
        long timeOut = System.currentTimeMillis() + 10000l;
        while (System.currentTimeMillis() < timeOut) {
            long pipeFD = system.open(path,
                                      LinuxSystem.O_WRONLY | LinuxSystem.O_NONBLOCK);
            if (pipeFD < 0l) {
                if (system.errno() == LinuxSystem.ENXIO) { // no reader on pipe
                    try {
                        Thread.sleep(100l);
                    } catch (InterruptedException e) { }
                } else {
                    break;
                }
            } else {
                return pipeFD;
            }
        }
        throw new IOException(system.getErrorMessage());
    }

    @Override
    protected void openConnection() throws IOException {
        fd = openPipe(INPUT_PATH);
    }

    @Override
    protected void closeConnection() {
        if (fd >= 0l) {
            system.close(fd);
            fd = -1l;
        }
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.limit()) {
            int bytesWritten = (int) system.write(devFD, buffer, offset, buffer.limit());
            if (bytesWritten < 0) {
                if (system.errno() == LinuxSystem.EAGAIN) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new IOException(system.getErrorMessage());
                }
            } else {
                offset += bytesWritten;
            }
        }
        system.ioctl(devFD, LinuxSystem.I_FLUSH, LinuxSystem.FLUSHRW);
    }
}
