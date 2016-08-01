/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.jmx;

import org.junit.Test;

public class SGMXBean_state_Test {

    private final SGMXBean mxBean = new SGMXBeanImpl();

    @Test(expected=IllegalStateException.class)
    public void stepNotPausedTest() {
        mxBean.step();
    }

    @Test(expected=IllegalStateException.class)
    public void getCSSInfoNotPausedTest() {
        mxBean.getCSSInfo(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getBoundsNotPausedTest() {
        mxBean.getBounds(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getSGTreeNotPausedTest() {
        mxBean.getSGTree(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getWindowsNotPausedTest() {
        mxBean.getWindows();
    }

    @Test(expected=IllegalStateException.class)
    public void addHighlightedNodeNotPausedTest() {
        mxBean.addHighlightedNode(0);
    }

    @Test(expected=IllegalStateException.class)
    public void removeHighlightedNodeNotPausedTest() {
        mxBean.removeHighlightedNode(0);
    }

    @Test(expected=IllegalStateException.class)
    public void addHighlightedRegionNotPausedTest() {
        mxBean.addHighlightedRegion(0, 0, 0, 1, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void removeHighlightedRegionNotPausedTest() {
        mxBean.removeHighlightedRegion(0, 0, 0, 1, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void makeScreenShotNodeNotPausedTest() {
        mxBean.makeScreenShot(0);
    }

    @Test(expected=IllegalStateException.class)
    public void makeScreenShotRegionNotPausedTest() {
        mxBean.makeScreenShot(0, 0, 0, 1, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void stepNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.step();
    }

    @Test(expected=IllegalStateException.class)
    public void getCSSInfoNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.getCSSInfo(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getBoundsNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.getBounds(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getSGTreeNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.getSGTree(0);
    }

    @Test(expected=IllegalStateException.class)
    public void getWindowsNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.getWindows();
    }

    @Test(expected=IllegalStateException.class)
    public void addHighlightedNodeNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.addHighlightedNode(0);
    }

    @Test(expected=IllegalStateException.class)
    public void removeHighlightedNodeNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.removeHighlightedNode(0);
    }

    @Test(expected=IllegalStateException.class)
    public void addHighlightedRegionNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.addHighlightedRegion(0, 0, 0, 1, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void removeHighlightedRegionNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.removeHighlightedRegion(0, 0, 0, 1, 1);
    }

    @Test(expected=IllegalStateException.class)
    public void makeScreenShotNodeNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.makeScreenShot(0);
    }

    @Test(expected=IllegalStateException.class)
    public void makeScreenShotRegionNotPaused2Test() {
        mxBean.pause();
        mxBean.resume();
        mxBean.makeScreenShot(0, 0, 0, 1, 1);
    }
}
