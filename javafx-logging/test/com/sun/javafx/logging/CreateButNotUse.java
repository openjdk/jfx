/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.logging;
import com.sun.javafx.logging.PlatformLogger;
public class CreateButNotUse {
    private static final int QUANTITY = 1;
    public static void main(String[] args) {
        PlatformLogger[] logger = new PlatformLogger[QUANTITY];
        long beforeMem = Runtime.getRuntime().freeMemory();
        long beforeTime, deltaTime, minTime=1000000000, maxTime=0, totalTime=0;
        for (int i = 0; i < logger.length; i++) {
            beforeTime = System.nanoTime();
            logger[i] = PlatformLogger.getLogger(Integer.toString(i));
            deltaTime = System.nanoTime() - beforeTime;
            totalTime += deltaTime;
            minTime = deltaTime < minTime ? deltaTime : minTime;
            maxTime = deltaTime > maxTime ? deltaTime : maxTime;
        }
        System.out.println("Memory: " + (beforeMem - Runtime.getRuntime().freeMemory()));
        System.out.println("minTime="+minTime + " maxTime="+maxTime + " avgTime="+(totalTime/logger.length));
        logger[0].finest("foo");
    }
}
