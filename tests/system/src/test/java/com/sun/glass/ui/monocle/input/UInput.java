/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

public class UInput {

    private static final boolean isMonocle;
    /** Log events? */
    private static final Boolean verbose = Boolean.getBoolean("verbose");
    /** If sync is set then we dispatch events on the application thread */
    private static final Boolean sync = Boolean.getBoolean("sync");
    private final NativeUInput nativeUInput = createNativeUInput();

    static {
        if (System.getProperty("glass.platform") == null) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            System.setProperty("prism.order", "sw");
            System.setProperty("com.sun.javafx.gestures.zoom", "true");
            System.setProperty("com.sun.javafx.gestures.rotate", "true");
            System.setProperty("com.sun.javafx.gestures.scroll", "true");
        }
        isMonocle = "Monocle".equals(System.getProperty("glass.platform"));
        setup();
    }

    public UInput() {
        nativeUInput.setup();
        nativeUInput.init();
    }

    private static NativeUInput createNativeUInput() {
        if (isMonocle) {
            return new MonocleUInput();
        } else {
            return new LensUInput();
        }
    }

    public void dispose() {
        nativeUInput.dispose();
    }

    public static void setup() {
        createNativeUInput().setup();
    }

    public void processLines(String[] lines) {
        for (String line : lines) {
            processLine(line);
        }
    }

    public void waitForQuiet() throws InterruptedException {
        nativeUInput.waitForQuiet();
    }

    public void write(byte[] data, int offset, int length) {
        if (sync && !Application.isEventThread()) {
            Application.invokeAndWait(() -> nativeUInput.write(data, offset, length));
        } else {
            nativeUInput.write(data, offset, length);
        }
    }

    public int writeTime(byte[] data, int offset) {
        return nativeUInput.writeTime(data, offset);
    }

    public int writeCode(byte[] data, int offset, String code) {
        return nativeUInput.writeCode(data, offset, code);
    }

    public int writeValue(byte[] data, int offset, String value) {
        return nativeUInput.writeValue(data, offset, value);
    }

    public int writeValue(byte[] data, int offset, int value) {
        return nativeUInput.writeValue(data, offset, value);
    }

    public int writeLine(byte[] data, int offset, String line) {
        String[] args = line.split(" ");
        offset = writeTime(data, offset);
        offset = writeCode(data, offset, args[0]);
        if (args.length >= 1) {
            offset = writeCode(data, offset, args[1]);
        } else {
            offset = writeCode(data, offset, "0");
        }
        if (args.length >= 2) {
            offset = writeValue(data, offset, args[2]);
        } else {
            offset = writeValue(data, offset, 0);
        }
        return offset;
    }

    public void processLine(String line) {
        if (sync && !Application.isEventThread()) {
            Application.invokeAndWait(() -> processLineImpl(line));
        } else {
            processLineImpl(line);
        }
    }

    private void processLineImpl(String line) {
        if (verbose) {
            System.out.println(line);
        }
        // ignore anything after a '#'
        int i = line.indexOf('#');
        if (i >= 0) {
            line = line.substring(0, i);
        }
        // ignore anything before the last colon (to strip out logging noise)
        line = line.substring(line.lastIndexOf(':') + 1).trim();
        if (line.length() > 0) {
            String[] args = line.split(" ");
            switch (args.length) {
                case 0: break;
                case 1: nativeUInput.processLine1(line, args[0]); break;
                case 2: nativeUInput.processLine2(line, args[0], args[1]); break;
                case 3: nativeUInput.processLine3(line, args[0], args[1], args[2]); break;
                default:
                    throw new IllegalArgumentException(line);
            }
        }
    }

}
