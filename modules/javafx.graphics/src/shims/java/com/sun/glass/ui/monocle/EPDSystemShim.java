/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

/** Exposes {@link EPDSystem}, its request codes and its {@code FbVarScreenInfo} to tests. */
public final class EPDSystemShim {

    public static final int FB_ACTIVATE_FORCE = EPDSystem.FB_ACTIVATE_FORCE;
    public static final int WAVEFORM_MODE_INIT = EPDSystem.WAVEFORM_MODE_INIT;
    public static final int WAVEFORM_MODE_DU = EPDSystem.WAVEFORM_MODE_DU;
    public static final int WAVEFORM_MODE_GC4 = EPDSystem.WAVEFORM_MODE_GC4;
    public static final int WAVEFORM_MODE_GC16 = EPDSystem.WAVEFORM_MODE_GC16;
    public static final int WAVEFORM_MODE_AUTO = EPDSystem.WAVEFORM_MODE_AUTO;
    public static final int UPDATE_MODE_PARTIAL = EPDSystem.UPDATE_MODE_PARTIAL;
    public static final int UPDATE_MODE_FULL = EPDSystem.UPDATE_MODE_FULL;
    public static final int TEMP_USE_AMBIENT = EPDSystem.TEMP_USE_AMBIENT;
    public static final int EPDC_FLAG_ENABLE_INVERSION = EPDSystem.EPDC_FLAG_ENABLE_INVERSION;
    public static final int AUTO_UPDATE_MODE_REGION_MODE = EPDSystem.AUTO_UPDATE_MODE_REGION_MODE;
    public static final int UPDATE_SCHEME_SNAPSHOT = EPDSystem.UPDATE_SCHEME_SNAPSHOT;

    private EPDSystemShim() {
    }

    public static void loadLibrary() {
        EPDSystem.getEPDSystem().loadLibrary();
    }

    public static int ioctl(long fd, int request, int value) {
        return EPDSystem.getEPDSystem().ioctl(fd, request, value);
    }

    public static int mxcfbSetWaveformModes() {
        return EPDSystem.getEPDSystem().MXCFB_SET_WAVEFORM_MODES;
    }

    public static int mxcfbSetTemperature() {
        return EPDSystem.getEPDSystem().MXCFB_SET_TEMPERATURE;
    }

    public static int mxcfbSetAutoUpdateMode() {
        return EPDSystem.getEPDSystem().MXCFB_SET_AUTO_UPDATE_MODE;
    }

    public static int mxcfbSendUpdate() {
        return EPDSystem.getEPDSystem().MXCFB_SEND_UPDATE;
    }

    public static int mxcfbWaitForUpdateComplete() {
        return EPDSystem.getEPDSystem().MXCFB_WAIT_FOR_UPDATE_COMPLETE;
    }

    public static int mxcfbSetPwrdownDelay() {
        return EPDSystem.getEPDSystem().MXCFB_SET_PWRDOWN_DELAY;
    }

    public static int mxcfbGetPwrdownDelay() {
        return EPDSystem.getEPDSystem().MXCFB_GET_PWRDOWN_DELAY;
    }

    public static int mxcfbSetUpdateScheme() {
        return EPDSystem.getEPDSystem().MXCFB_SET_UPDATE_SCHEME;
    }

    /** An {@code EPDSystem.FbVarScreenInfo} and every accessor it and its superclass give it, over its own address. */
    public static final class FbVarScreenInfoShim {

        private final EPDSystem.FbVarScreenInfo s = new EPDSystem.FbVarScreenInfo();

        public long address() {
            return s.p;
        }

        /** The direct buffer over the struct, in the byte order ByteBuffer.allocateDirect gave it. */
        public ByteBuffer buffer() {
            return s.b;
        }

        public int sizeof() {
            return s.sizeof();
        }

        public int getBitsPerPixel() {
            return s.getBitsPerPixel(s.p);
        }

        public int getXRes() {
            return s.getXRes(s.p);
        }

        public int getYRes() {
            return s.getYRes(s.p);
        }

        public int getXResVirtual() {
            return s.getXResVirtual(s.p);
        }

        public int getYResVirtual() {
            return s.getYResVirtual(s.p);
        }

        public int getOffsetX() {
            return s.getOffsetX(s.p);
        }

        public int getOffsetY() {
            return s.getOffsetY(s.p);
        }

        public int getGrayscale() {
            return s.getGrayscale(s.p);
        }

        public int getRedOffset() {
            return s.getRedOffset(s.p);
        }

        public int getRedLength() {
            return s.getRedLength(s.p);
        }

        public int getRedMsbRight() {
            return s.getRedMsbRight(s.p);
        }

        public int getGreenOffset() {
            return s.getGreenOffset(s.p);
        }

        public int getGreenLength() {
            return s.getGreenLength(s.p);
        }

        public int getGreenMsbRight() {
            return s.getGreenMsbRight(s.p);
        }

        public int getBlueOffset() {
            return s.getBlueOffset(s.p);
        }

        public int getBlueLength() {
            return s.getBlueLength(s.p);
        }

        public int getBlueMsbRight() {
            return s.getBlueMsbRight(s.p);
        }

        public int getTranspOffset() {
            return s.getTranspOffset(s.p);
        }

        public int getTranspLength() {
            return s.getTranspLength(s.p);
        }

        public int getTranspMsbRight() {
            return s.getTranspMsbRight(s.p);
        }

        public int getNonstd() {
            return s.getNonstd(s.p);
        }

        public int getActivate() {
            return s.getActivate(s.p);
        }

        public int getHeight() {
            return s.getHeight(s.p);
        }

        public int getWidth() {
            return s.getWidth(s.p);
        }

        public int getAccelFlags() {
            return s.getAccelFlags(s.p);
        }

        public int getPixclock() {
            return s.getPixclock(s.p);
        }

        public int getLeftMargin() {
            return s.getLeftMargin(s.p);
        }

        public int getRightMargin() {
            return s.getRightMargin(s.p);
        }

        public int getUpperMargin() {
            return s.getUpperMargin(s.p);
        }

        public int getLowerMargin() {
            return s.getLowerMargin(s.p);
        }

        public int getHsyncLen() {
            return s.getHsyncLen(s.p);
        }

        public int getVsyncLen() {
            return s.getVsyncLen(s.p);
        }

        public int getSync() {
            return s.getSync(s.p);
        }

        public int getVmode() {
            return s.getVmode(s.p);
        }

        public int getRotate() {
            return s.getRotate(s.p);
        }

        public void setRes(int x, int y) {
            s.setRes(s.p, x, y);
        }

        public void setVirtualRes(int x, int y) {
            s.setVirtualRes(s.p, x, y);
        }

        public void setOffset(int x, int y) {
            s.setOffset(s.p, x, y);
        }

        public void setActivate(int activate) {
            s.setActivate(s.p, activate);
        }

        public void setBitsPerPixel(int bpp) {
            s.setBitsPerPixel(s.p, bpp);
        }

        public void setRed(int length, int offset) {
            s.setRed(s.p, length, offset);
        }

        public void setGreen(int length, int offset) {
            s.setGreen(s.p, length, offset);
        }

        public void setBlue(int length, int offset) {
            s.setBlue(s.p, length, offset);
        }

        public void setTransp(int length, int offset) {
            s.setTransp(s.p, length, offset);
        }

        public void setGrayscale(int grayscale) {
            s.setGrayscale(s.p, grayscale);
        }

        public void setNonstd(int nonstd) {
            s.setNonstd(s.p, nonstd);
        }

        public void setHeight(int height) {
            s.setHeight(s.p, height);
        }

        public void setWidth(int width) {
            s.setWidth(s.p, width);
        }

        public void setAccelFlags(int accelFlags) {
            s.setAccelFlags(s.p, accelFlags);
        }

        public void setPixclock(int pixclock) {
            s.setPixclock(s.p, pixclock);
        }

        public void setLeftMargin(int leftMargin) {
            s.setLeftMargin(s.p, leftMargin);
        }

        public void setRightMargin(int rightMargin) {
            s.setRightMargin(s.p, rightMargin);
        }

        public void setUpperMargin(int upperMargin) {
            s.setUpperMargin(s.p, upperMargin);
        }

        public void setLowerMargin(int lowerMargin) {
            s.setLowerMargin(s.p, lowerMargin);
        }

        public void setHsyncLen(int hsyncLen) {
            s.setHsyncLen(s.p, hsyncLen);
        }

        public void setVsyncLen(int vsyncLen) {
            s.setVsyncLen(s.p, vsyncLen);
        }

        public void setSync(int sync) {
            s.setSync(s.p, sync);
        }

        public void setVmode(int vmode) {
            s.setVmode(s.p, vmode);
        }

        public void setRotate(int rotate) {
            s.setRotate(s.p, rotate);
        }
    }
}
