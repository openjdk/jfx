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

package com.sun.glass.ui.monocle;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/** Describes the capabilities of Linux input devices with absolute
 * coordinates. See /usr/include/linux/input.h */
class LinuxAbsoluteInputCapabilities {

    private int value;
    private int minimum;
    private int maximum;
    private int fuzz;
    private int flat;
    private int resolution;

    LinuxAbsoluteInputCapabilities(int value, int maximum, int minimum,
                                          int fuzz, int flat, int resolution) {
        this.value = value;
        this.maximum = maximum;
        this.minimum = minimum;
        this.fuzz = fuzz;
        this.flat = flat;
        this.resolution = resolution;
    }

    private LinuxAbsoluteInputCapabilities(LinuxSystem system,
                                           LinuxSystem.InputAbsInfo info,
                                           long fd, int axis) throws IOException {
        system.ioctl(fd, system.EVIOCGABS(axis), info.p);
        value = LinuxSystem.InputAbsInfo.getValue(info.p);
        minimum = LinuxSystem.InputAbsInfo.getMinimum(info.p);
        maximum = LinuxSystem.InputAbsInfo.getMaximum(info.p);
        fuzz = LinuxSystem.InputAbsInfo.getFuzz(info.p);
        flat = LinuxSystem.InputAbsInfo.getFlat(info.p);
        resolution = LinuxSystem.InputAbsInfo.getResolution(info.p);
    }

    /** Reads capabilities from a device node.
     *
     * @return A Map of capabilities for each supported axis, or null if no
     * capabilities are available.
     * @throws IOException if an error occured when reading capabilities
     * */
    static Map<Integer, LinuxAbsoluteInputCapabilities> getCapabilities(
            File devNode, BitSet axes) throws IOException {
        if (axes == null || axes.isEmpty()) {
            return null;
        }
        LinuxSystem system = LinuxSystem.getLinuxSystem();
        LinuxSystem.InputAbsInfo info = new LinuxSystem.InputAbsInfo();
        long fd = system.open(devNode.getPath(), LinuxSystem.O_RDONLY);
        if (fd == -1) {
            throw new IOException(system.getErrorMessage());
        }
        Map<Integer, LinuxAbsoluteInputCapabilities> caps =
                new HashMap<Integer, LinuxAbsoluteInputCapabilities>();
        for (int i = 0; (i = axes.nextSetBit(i)) != -1; i++) {
            caps.put(i, new LinuxAbsoluteInputCapabilities(system, info, fd, i));
        }
        system.close(fd);
        return caps;
    }


    int getValue() {
        return value;
    }

    int getMinimum() {
        return minimum;
    }

    int getMaximum() {
        return maximum;
    }

    int getFuzz() {
        return fuzz;
    }

    int getFlat() {
        return flat;
    }

    int getResolution() {
        return resolution;
    }


}
