/*
 * Copyright (c) 2010, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.foreign.MemorySegment;

/**
 * The drag-and-drop clipboard: the same peer as {@link WinSystemClipboard} wired to
 * {@code DoDragDrop} instead of {@code OleSetClipboard}. Its two former natives are
 * {@code gwin_dnd_push} and {@code gwin_dnd_dispose}; the other direction is the five id-less slots
 * of {@code GwinDndCallbacks} whose peer is {@link #getInstance()} - the static method the JNI
 * called by name, which <em>creates</em> the peer through {@code Clipboard.get(DND)} when none
 * exists yet and throws off the application thread. Both are load-bearing (a drag from another
 * application arrives before any Java code has asked for this clipboard), so the five
 * {@code dispatch*} statics call it every time and never cache the instance.
 */
final class WinDnDClipboard extends WinSystemClipboard {
    public WinDnDClipboard(String name) {
        super(name);
    }

    @Override protected void create() {}

    /**
     * {@code gwin_dnd_dispose}: {@code Release} and nothing else - no viewer chain, no flush; this
     * peer never joined the chain ({@link #create} is a no-op) and that asymmetry is deliberate.
     */
    @Override protected void dispose() {
        WinGlassNative.dndDispose(getPtr());
    }

    @Override protected boolean isOwner() {
        return getDragButton() != 0;
    }

    @Override protected void pushTargetActionToSystem(int actionDone) {
        throw new UnsupportedOperationException(
            "[Target Action] not supported! Override View.handleDragDrop instead.");
    }

    /*
     * public mime types to system clipboard: gwin_dnd_push publishes the new ClipboardData through
     * set_data_object (before pushCommit, setDragImage and DoDragDrop), then BLOCKS for the whole
     * drag in DoDragDrop's modal loop, during which the drag slots, fos_serialize and the dnd_*
     * slots fire nested inside this call, and finally fires drag_action_performed then
     * dnd_set_drag_button(0). The status is ignored, as the JNI ignored the HRESULT.
     */
    @Override protected void push(Object[] keys, int supportedActions) {
        int ignoredStatus = WinGlassNative.dndPush(getPtr(), nativeId(), keys, supportedActions);
    }

    /*
     * extract clipboard snap-shot
     */
    @Override protected boolean pop() {
        //The DnD buffer ownership coild not be suddenly changed
        //while active DnD operation.
        return !MemorySegment.NULL.equals(getPtr());
    }

    /*
     * called from native, through the five dispatch statics below
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
     * Called from native code, through dispatchSetDragButton
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
     * Called from native code, through dispatchSetSourceSupportedActions
     */
    private void setSourceSupportedActions(int sourceSupportedActions) {
        this.sourceSupportedActions = sourceSupportedActions;
    }

    /*
     * The dispatch half of the five id-less GwinDndCallbacks slots. Each resolves the peer with
     * getInstance() - creating it, or throwing off the application thread, exactly as the JNI's
     * CallStaticObjectMethod did - and the marshalling and exception barrier are WinGlassNative's.
     */

    /** {@code dnd_get_data_object}: the handle the drag clipboard holds, for the AddRef / Release in C. */
    static MemorySegment dispatchGetDataObject() {
        return getInstance().getPtr();
    }

    /** {@code dnd_set_data_object}: the object being dragged in, already AddRef'd by the C. */
    static void dispatchSetDataObject(MemorySegment dataObject) {
        getInstance().setPtr(dataObject);
    }

    /** {@code dnd_set_source_supported_actions}, from the prologue of every drag_enter / over / drop. */
    static void dispatchSetSourceSupportedActions(int actions) {
        getInstance().setSourceSupportedActions(actions);
    }

    /** {@code dnd_set_drag_button}: 0 from the tail of {@code gwin_dnd_push}; writes the static. */
    static void dispatchSetDragButton(int button) {
        getInstance().setDragButton(button);
    }

    /** {@code dnd_get_drag_button}: the static, read by the {@code GlassDropSource} constructor. */
    static int dispatchGetDragButton() {
        return getInstance().getDragButton();
    }
}
