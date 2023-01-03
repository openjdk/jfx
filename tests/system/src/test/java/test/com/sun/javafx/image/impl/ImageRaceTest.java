/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.image.impl;

import com.sun.javafx.image.impl.ByteArgb;
import com.sun.javafx.image.impl.ByteBgr;
import com.sun.javafx.image.impl.ByteBgra;
import com.sun.javafx.image.impl.ByteBgraPre;
import com.sun.javafx.image.impl.ByteGray;
import com.sun.javafx.image.impl.ByteGrayAlpha;
import com.sun.javafx.image.impl.ByteGrayAlphaPre;
import com.sun.javafx.image.impl.ByteRgb;
import com.sun.javafx.image.impl.ByteRgba;
import com.sun.javafx.image.impl.IntArgb;
import com.sun.javafx.image.impl.IntArgbPre;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static test.util.Util.TIMEOUT;

public class ImageRaceTest {
    static boolean verbose;
    static List<Initializer> initalizers = new ArrayList<>();
    static volatile boolean ready = false;

    static interface InitProc {
        public Object get();
    }

    static class Initializer extends Thread {
        private final InitProc init;
        private volatile boolean running;

        public Initializer(String classname, InitProc r) {
            super(classname+" Initializer");
            this.init = r;
        }

        public boolean isRunning() { return running; }

        @Override
        public void run() {
            if (verbose) System.err.println(getName()+" started");
            running = true;
            while (!ready) {
                try {
                    sleep(1);
                } catch (InterruptedException ex) {}
            }
            init.get();
            if (verbose) System.err.println(getName()+" done");
        }
    }

    void forkAndJoinInitializers() {
        long limit = System.currentTimeMillis() + TIMEOUT;
        for (Initializer i : initalizers) {
            i.start();
            while (!i.isRunning() && System.currentTimeMillis() < limit) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {}
            }
            if (!i.isRunning()) {
                throw new RuntimeException("Initializer "+i+" never started");
            }
        }

        if (verbose) System.err.println("\n[main] signal the threads to proceed\n");
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
        ready = true;

        limit = System.currentTimeMillis() + TIMEOUT;
        try {
            for (Initializer i : initalizers) {
                long now = System.currentTimeMillis();
                if (now < limit) {
                    i.join(limit - now);
                }
                if (i.isAlive()) {
                    throw new RuntimeException("Initializer "+i+" never finished");
                }
            }
        } catch (InterruptedException ex) {}
    }

    static void init(String[] args) {
        boolean getters, setters, converters;
        if (args.length == 0) {
            getters = setters = converters = true;
        } else {
            getters = setters = converters = false;
            for (String arg : args) {
                if (arg.equalsIgnoreCase("getters")) {
                    getters = true;
                } else if (arg.equalsIgnoreCase("setters")) {
                    setters = true;
                } else if (arg.equalsIgnoreCase("converters")) {
                    converters = true;
                } else if (arg.equalsIgnoreCase("-verbose")) {
                    verbose = true;
                } else {
                    System.err.println("Unrecognized argument: "+arg);
                    System.exit(-1);
                }
            }
        }
        if (getters) {
            initalizers.add(new Initializer("ByteArgb.getter",         () -> { return ByteArgb.getter; } ));
            initalizers.add(new Initializer("ByteBgr.getter",          () -> { return ByteBgr.getter; } ));
            initalizers.add(new Initializer("ByteBgra.getter",         () -> { return ByteBgra.getter; } ));
            initalizers.add(new Initializer("ByteBgraPre.getter",      () -> { return ByteBgraPre.getter; } ));
            initalizers.add(new Initializer("ByteGray.getter",         () -> { return ByteGray.getter; } ));
            initalizers.add(new Initializer("ByteGrayAlpha.getter",    () -> { return ByteGrayAlpha.getter; } ));
            initalizers.add(new Initializer("ByteGrayAlphaPre.getter", () -> { return ByteGrayAlphaPre.getter; } ));
//            initalizers.add(new Initializer("ByteIndexed.getter",    /* Has no .getter */ ));
            initalizers.add(new Initializer("ByteRgb.getter",          () -> { return ByteRgb.getter; } ));
            initalizers.add(new Initializer("ByteRgba.getter",         () -> { return ByteRgba.getter; } ));
            initalizers.add(new Initializer("IntArgb.getter",          () -> { return IntArgb.getter; } ));
            initalizers.add(new Initializer("IntArgbPre.getter",       () -> { return IntArgbPre.getter; } ));
        }
        if (setters) {
            initalizers.add(new Initializer("ByteArgb.setter",         () -> { return ByteArgb.setter; } ));
            initalizers.add(new Initializer("ByteBgr.setter",          () -> { return ByteBgr.setter; } ));
            initalizers.add(new Initializer("ByteBgra.setter",         () -> { return ByteBgra.setter; } ));
            initalizers.add(new Initializer("ByteBgraPre.setter",      () -> { return ByteBgraPre.setter; } ));
            initalizers.add(new Initializer("ByteGray.setter",         () -> { return ByteGray.setter; } ));
            initalizers.add(new Initializer("ByteGrayAlpha.setter",    () -> { return ByteGrayAlpha.setter; } ));
            initalizers.add(new Initializer("ByteGrayAlphaPre.setter", () -> { return ByteGrayAlphaPre.setter; } ));
//            initalizers.add(new Initializer("ByteIndexed.setter",    /* Has no .setter */ ));
//            initalizers.add(new Initializer("ByteRgb.setter",          /* Has no .setter */ ));
            initalizers.add(new Initializer("ByteRgba.setter",         () -> { return ByteRgba.setter; } ));
            initalizers.add(new Initializer("IntArgb.setter",          () -> { return IntArgb.setter; } ));
            initalizers.add(new Initializer("IntArgbPre.setter",       () -> { return IntArgbPre.setter; } ));
        }
        if (converters) {
            initalizers.add(new Initializer("ByteBgr.ToByteArgb", () ->
                                    { return ByteBgr.ToByteArgbConverter(); } ));
            initalizers.add(new Initializer("ByteBgr.ToByteBgr", () ->
                                    { return ByteBgr.ToByteBgrConverter(); } ));
            initalizers.add(new Initializer("ByteBgr.ToByteBgra", () ->
                                    { return ByteBgr.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteBgr.ToByteBgraPre", () ->
                                    { return ByteBgr.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteBgr.ToIntArgb", () ->
                                    { return ByteBgr.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("ByteBgr.ToIntArgbPre", () ->
                                    { return ByteBgr.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("ByteBgra.ToByteBgra", () ->
                                    { return ByteBgra.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteBgra.ToByteBgraPre", () ->
                                    { return ByteBgra.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteBgra.ToIntArgb",  () ->
                                    { return ByteBgra.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("ByteBgra.ToIntArgbPre",  () ->
                                    { return ByteBgra.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("ByteBgraPre.ToByteBgra", () ->
                                    { return ByteBgraPre.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteBgraPre.ToByteBgraPre", () ->
                                    { return ByteBgraPre.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteBgraPre.ToIntArgb", () ->
                                    { return ByteBgraPre.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("ByteBgraPre.ToIntArgbPre", () ->
                                    { return ByteBgraPre.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToByteBgr", () ->
                                    { return ByteGray.ToByteBgrConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToByteBgra", () ->
                                    { return ByteGray.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToByteBgraPre", () ->
                                    { return ByteGray.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToByteGray", () ->
                                    { return ByteGray.ToByteGrayConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToIntArgb", () ->
                                    { return ByteGray.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("ByteGray.ToIntArgbPre", () ->
                                    { return ByteGray.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("ByteGrayAlpha.ToByteBgra", () ->
                                    { return ByteGrayAlpha.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteGrayAlpha.ToByteGrayAlphaPre", () ->
                                    { return ByteGrayAlpha.ToByteGrayAlphaPreConverter(); } ));
            initalizers.add(new Initializer("ByteGrayAlphaPre.ToByteBgraPre", () ->
                                    { return ByteGrayAlphaPre.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToByteArgb", () ->
                                    { return ByteRgb.ToByteArgbConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToByteBgr", () ->
                                    { return ByteRgb.ToByteBgrConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToByteBgra", () ->
                                    { return ByteRgb.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToByteBgraPre", () ->
                                    { return ByteRgb.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToIntArgb", () ->
                                    { return ByteRgb.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("ByteRgb.ToIntArgbPre", () ->
                                    { return ByteRgb.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("ByteRgba.ToByteBgra", () ->
                                    { return ByteRgba.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("ByteRgba.ToByteRgba", () ->
                                    { return ByteRgba.ToByteRgbaConverter(); } ));
            initalizers.add(new Initializer("IntArgb.ToByteBgra", () ->
                                    { return IntArgb.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("IntArgb.ToByteBgraPre", () ->
                                    { return IntArgb.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("IntArgb.ToIntArgb", () ->
                                    { return IntArgb.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("IntArgb.ToIntArgbPre", () ->
                                    { return IntArgb.ToIntArgbPreConverter(); } ));
            initalizers.add(new Initializer("IntArgbPre.ToByteBgra", () ->
                                    { return IntArgbPre.ToByteBgraConverter(); } ));
            initalizers.add(new Initializer("IntArgbPre.ToByteBgraPre", () ->
                                    { return IntArgbPre.ToByteBgraPreConverter(); } ));
            initalizers.add(new Initializer("IntArgbPre.ToIntArgb", () ->
                                    { return IntArgbPre.ToIntArgbConverter(); } ));
            initalizers.add(new Initializer("IntArgbPre.ToIntArgbPre", () ->
                                    { return IntArgbPre.ToIntArgbPreConverter(); } ));
        }
    }

    @Test
    public void testImageInitializationRaceCondition() {
        init(new String[0]);
        forkAndJoinInitializers();
    }
}
