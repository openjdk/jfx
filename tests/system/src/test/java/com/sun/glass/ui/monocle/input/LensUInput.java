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

public class LensUInput implements NativeUInput {

    private long p;

    static {
        System.loadLibrary("uinput");
    }

    @Override
    public void setup() {
        nSetup();
    }

    @Override
    public void init(boolean verbose) {
        p = nInit(verbose);
    }

    @Override
    public void dispose() {
        nDispose(p);
        p = 0l;
    }

    @Override
    public void processLine0(String line) {
        nProcessLine0(p);
    }

    @Override
    public void processLine1(String line, String cmd) {
        nProcessLine1(p, line, cmd);
    }

    @Override
    public void processLine2(String line, String cmd, String arg1) {
        nProcessLine2(p, line, cmd, arg1);
    }

    @Override
    public void processLine3(String line, String cmd, String arg1,
                             String arg2) {
        nProcessLine3(p, line, cmd, arg1, arg2);
    }

    @Override
    public int writeTime(byte[] data, int offset) {
        return nWriteTime(data, offset);
    }

    @Override
    public int writeCode(byte[] data, int offset, String code) {
        return nWriteCode(data, offset, code);
    }

    @Override
    public int writeValue(byte[] data, int offset, String value) {
        return nWriteValue(data, offset, value);
    }

    @Override
    public int writeValue(byte[] data, int offset, int value) {
        return nWriteValue(data, offset, value);
    }

    @Override
    public void write(byte[] data, int offset, int length) {
        nWrite(p, data, offset, length);
    }

    private static native void nSetup();
    private native long nInit(boolean version);
    private native void nDispose(long p);
    private static native void nProcessLine0(long p);
    private static native void nProcessLine1(long p, String line, String cmd);
    private static native void nProcessLine2(long p, String line, String cmd, String arg1);
    private static native void nProcessLine3(long p, String line, String cmd, String arg1, String arg2);
    private static native int nWriteTime(byte[] data, int offset);
    private static native int nWriteCode(byte[] data, int offset, String code);
    private static native int nWriteValue(byte[] data, int offset, String value);
    private static native int nWriteValue(byte[] data, int offset, int value);
    private static native void nWrite(long p, byte[] data, int offset, int length);

}