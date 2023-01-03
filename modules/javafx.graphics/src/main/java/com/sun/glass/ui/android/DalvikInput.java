/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.android;

public class DalvikInput {
/*

    public static void onMultiTouchEvent(final int count, final int[] actions,
            final int[] ids, final int[] touchXs, final int[] touchYs) {
        Platform.runLater(new Runnable() {
            public void run() {
                onMultiTouchEventNative(count, actions, ids, touchXs, touchYs);
            }
        });

    }

    private static native void onMultiTouchEventNative(int count, int[] actions,
            int[] ids, int[] touchXs, int[] touchYs);

    public static void onKeyEvent(final int action, final int keycode, final String characters){
        Platform.runLater(new Runnable() {
            public void run() {
                onKeyEventNative(action, keycode, characters);
            }
        });
    };

    private static Node activeNode;

    public static void onGlobalLayoutChanged() {
        if (activeNode != null) {
            Platform.runLater(() -> activeNode.getParent().requestFocus());
        }
    }

    public static void setActiveNode (Node n) {
        activeNode = n;
    }

    private static native void onKeyEventNative(int action, int keycode, String characters);

    public static native void onSurfaceChangedNative();

    public static native void onSurfaceChangedNative(int format, int width, int height);

    public static native void onSurfaceRedrawNeededNative();

    public static native void onConfigurationChangedNative(int flag);
*/
}
