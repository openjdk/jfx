/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import java.util.HashMap;
import java.util.Map;

public class PrismTrace {

    private static final boolean enabled = PrismSettings.printAllocs;

    private static Map<Long, Long> texData;
    private static long texBytes;
    private static Map<Long, Long> rttData;
    private static long rttBytes;

    static {
        if (enabled) {
            texData = new HashMap<>();
            rttData = new HashMap<>();
            Runtime.getRuntime().addShutdownHook(new Thread("RTT printAlloc shutdown hook") {
                @Override
                public void run() {
                    System.out.println("Final Texture resources:"+
                        summary(SummaryType.TYPE_TEX)+
                        summary(SummaryType.TYPE_RTT)+
                        summary(SummaryType.TYPE_ALL));
                }
            });
        }
    }

    private static enum SummaryType { TYPE_TEX, TYPE_RTT, TYPE_ALL }
    private static String summary(long count, long size, String label) {
        return String.format("%s=%d@%,dKB", label, count, size >> 10);
    }
    private static String summary(SummaryType type) {
        switch (type) {
            case TYPE_TEX:
                return summary(texData.size(), texBytes, " tex");
            case TYPE_RTT:
                return summary(rttData.size(), rttBytes, " rtt");
            case TYPE_ALL:
                return summary(texData.size()+rttData.size(),
                               texBytes+rttBytes, " combined");
        }
        return "ERROR";
    }

    private static long computeSize(int w, int h, int bpp) {
        long size = w;
        size *= h;
        size *= bpp;
        return size;
    }

    public static void textureCreated(long id, int w, int h, long size) {
        if (!enabled) return;

        texData.put(id, size);
        texBytes += size;
        System.out.println("Created Texture: "+
            "id=" + id + ", " + w + "x" + h +" pixels, " + size + " bytes," +
            summary(SummaryType.TYPE_TEX) +
            summary(SummaryType.TYPE_ALL));
        //Thread.dumpStack();
    }

    public static void textureCreated(long id, int w, int h, int bytesPerPixel) {
        if (!enabled) return;

        long size = computeSize(w, h, bytesPerPixel);
        texData.put(id, size);
        texBytes += size;
        System.out.println("Created Texture: "+
            "id=" + id + ", " + w + "x" + h +" pixels, " + size + " bytes," +
            summary(SummaryType.TYPE_TEX) +
            summary(SummaryType.TYPE_ALL));
        //Thread.dumpStack();
    }

    public static void textureDisposed(long id) {
        if (!enabled) return;

        Long size = texData.remove(id);
        if (size == null) {
            throw new InternalError("Disposing unknown Texture " + id);
        }
        texBytes -= size;
        System.out.println("Disposed Texture: "+
            "id=" + id + ", " + size + " bytes" +
            summary(SummaryType.TYPE_TEX) +
            summary(SummaryType.TYPE_ALL));
    }

    public static void rttCreated(long id, int w, int h, long size) {
        if (!enabled) return;

        rttData.put(id, size);
        rttBytes += size;
        System.out.println("Created RTTexture: "+
            "id=" + id + ", " + w + "x" + h +" pixels, " + size + " bytes," +
            summary(SummaryType.TYPE_RTT) +
            summary(SummaryType.TYPE_ALL));
    }

    public static void rttCreated(long id, int w, int h, int bytesPerPixel) {
        if (!enabled) return;

        long size = computeSize(w, h, bytesPerPixel);
        rttData.put(id, size);
        rttBytes += size;
        System.out.println("Created RTTexture: "+
            "id=" + id + ", " + w + "x" + h +" pixels, " + size + " bytes," +
            summary(SummaryType.TYPE_RTT) +
            summary(SummaryType.TYPE_ALL));
    }

    public static void rttDisposed(long id) {
        if (!enabled) return;

        Long size = rttData.remove(id);
        if (size == null) {
            throw new InternalError("Disposing unknown RTTexture " + id);
        }
        rttBytes -= size;
        System.out.println("Disposed RTTexture: "+
            "id=" + id + ", " + size + " bytes" +
            summary(SummaryType.TYPE_RTT) +
            summary(SummaryType.TYPE_ALL));
    }

    private PrismTrace() {
    }
}
