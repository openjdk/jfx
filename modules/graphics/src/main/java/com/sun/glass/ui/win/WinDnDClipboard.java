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
package com.sun.glass.ui.win;

final class WinDnDClipboard extends WinSystemClipboard {
    public WinDnDClipboard(String name) {
        super(name);
    }

    @Override protected void create() {}

    @Override protected native void dispose();

    @Override protected boolean isOwner() {
        return getDragButton() != 0;
    }

    @Override protected void pushTargetActionToSystem(int actionDone) {
        throw new UnsupportedOperationException(
            "[Target Action] not supported! Override View.handleDragDrop instead.");
    }

    /*
     * public mime types to system clipboard
     */
    @Override protected native void push(Object[] keys, int supportedActions);

    /*
     * extract clipboard snap-shot
     */
    @Override protected boolean pop() {
        //The DnD buffer ownership coild not be suddenly changed
        //while active DnD operation.
        return getPtr() != 0L;
    }

    /*
     * called from native
     */
    private static WinDnDClipboard getInstance() {
        return (WinDnDClipboard)get(DND);
    }

    @Override public String toString() {
        return "Windows DnD Clipboard";
    }

    /*
     * The MouseEvent.BUTTON_XXXX const if Java is DnD source.
     *
     * This field is static because at any point of time there may be only one
     * active DnD operation in the system, let alone a single Glass
     * application instance. The setter and getter methods should be static.
     */
    private static int dragButton = 0;

    public int getDragButton() {
        return dragButton;
    }

    /*
     * Called from native code
     */
    private void setDragButton(int dragButton) {
        this.dragButton = dragButton;
    }

    /*
     * The Clipboard.ACTION_XXXX const if Java is DnD target.
     */
    private int sourceSupportedActions = 0;

    @Override protected final int supportedSourceActionsFromSystem() {
        return sourceSupportedActions != 0
            ? sourceSupportedActions  //an old style (from DnD call back)
            : super.supportedSourceActionsFromSystem(); //new Explorer-like style
    }

    /*
     * Called from native code
     */
    private void setSourceSupportedActions(int sourceSupportedActions) {
        this.sourceSupportedActions = sourceSupportedActions;
    }

}
