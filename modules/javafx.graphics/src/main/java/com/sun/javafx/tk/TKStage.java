/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk;

import java.security.AccessControlContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.ModifierValue;

/**
 * TKStage - Peer interface for a Stage
 *
 */
public interface TKStage {
    /**
     * Listener for this stage peer to pass updates and events back to the stage
     *
     * @param listener The listener provided by the stage
     */
    public void setTKStageListener(TKStageListener listener);

    /**
     * Creates a Scene peer that can be displayed in this Stage peer.
     *
     * @return scenePeer The peer of the scene to be displayed
     */
    public TKScene createTKScene(boolean depthBuffer, boolean msaa, @SuppressWarnings("removal") AccessControlContext acc);

    /**
     * Set the scene to be displayed in this stage
     *
     * @param scene The peer of the scene to be displayed
     */
    public void setScene(TKScene scene);

    /**
     * Sets the window bounds to the specified values.
     *
     * Gravity values specify how to correct window location if only its size
     * changes (for example when stage decorations are added). User initiated
     * resizing should be ignored and must not influence window location through
     * this mechanism.
     *
     * The corresponding correction formulas are:
     *
     * {@code x -= xGravity * deltaW}
     * {@code y -= yGravity * deltaH}
     *
     * @param x the new window horizontal position, ignored if xSet is set to
     *          false
     * @param y the new window vertical position, ignored if ySet is set to
     *          false
     * @param xSet indicates whether the x parameter is valid
     * @param ySet indicates whether the y parameter is valid
     * @param w the new window width, ignored if set to -1
     * @param h the new window height, ignored if set to -1
     * @param cw the new window content width, ignored if set to -1
     * @param ch the new window content height, ignored if set to -1
     * @param xGravity the xGravity coefficient
     * @param yGravity the yGravity coefficient
     * @param renderScaleX new rendering scale, ignored if < 1
     * @param renderScaleY new rendering scale, ignored if < 1
     */
    public void setBounds(float x, float y, boolean xSet, boolean ySet,
                          float w, float h, float cw, float ch,
                          float xGravity, float yGravity,
                          float renderScaleX, float renderScaleY);

    public float getPlatformScaleX();
    public float getPlatformScaleY();
    public float getOutputScaleX();
    public float getOutputScaleY();

    public void setIcons(java.util.List icons);

    public void setTitle(String title);

    /**
     * Set if the stage is visible on screen
     *
     * @param visible True if the stage should be visible
     */
    public void setVisible(boolean visible);

    public void setOpacity(float opacity);

    public void setIconified(boolean iconified);

    public void setMaximized(boolean maximized);

    public void setAlwaysOnTop(boolean alwaysOnTop);

    public void setResizable(boolean resizable);

    public void setImportant(boolean important);

    public void setMinimumSize(int minWidth, int minHeight);

    public void setMaximumSize(int maxWidth, int maxHeight);

    public void setFullScreen(boolean fullScreen);

    // =================================================================================================================
    // Functions

    public void requestFocus();
    public void toBack();
    public void toFront();
    public void close();

    public default void postponeClose() {}
    public default void closePostponed() {}

    public void requestFocus(FocusCause cause);

    /**
     * Grabs focus on this window.
     *
     * All mouse clicks that occur in this window's client area or client-areas
     * of any of its unfocusable owned windows are delivered as usual. Whenever
     * a click occurs on another app's window (not related via the ownership
     * relation with this one, or a focusable owned window), or on non-client
     * area of any window (titlebar, etc.), or any third-party app's window, or
     * native OS GUI (e.g. a taskbar), the grab is automatically reset, and the
     * window that held the grab receives the FOCUS_UNGRAB event.
     *
     * Note that for this functionality to work correctly, the window must have
     * a focus upon calling this method. All owned popup windows that should be
     * operable during the grabbed focus state (e.g. nested popup menus) must
     * be unfocusable (see {@link #setFocusable}). Clicking a focusable owned
     * window will reset the grab due to a focus transfer.
     *
     * The click that occurs in another window and causes resetting of the grab
     * may or may not be delivered to that other window depending on the native
     * OS behavior.
     *
     * If any of the application's windows already holds the grab, it is reset
     * prior to grabbing the focus for this window. The method may be called
     * multiple times for one window. Subsequent calls do not affect the grab
     * status unless it is reset between the calls, in which case the focus
     * is grabbed again.
     *
     * Note that grabbing the focus on an application window may prevent
     * delivering certain events to other applications until the grab is reset.
     * Therefore, if the application has finished showing popup windows based
     * on a user action (e.g. clicking a menu item), and doesn't require the
     * grab any more, it should call the {@link #ungrabFocus} method. The
     * FOCUS_UNGRAB event signals that the grab has been reset.
     *
     * A user event handler associated with a menu item must be invoked after
     * resetting the grab. Otherwise, if a developer debugs the application and
     * has installed a breakpoint in the event handler, the debugger may become
     * unoperable due to events blocking for other applications on some
     * platforms.
     *
     * @return {@code true} if the operation is successful
     * @throws IllegalStateException if the window isn't focused currently
     */
    public boolean grabFocus();

    /**
     * Manually ungrabs focus grabbed on this window previously.
     *
     * This method resets the grab, and forces sending of the FOCUS_UNGRAB
     * event. It should be used when popup windows (such as menus) should be
     * dismissed manually, e.g. when a user clicks a menu item which usually
     * causes the menus to hide.
     *
     * @see #grabFocus
     */
    public void ungrabFocus();

    /**
     * Requests text input in form of native keyboard for text component
     * contained by this Window. Native text input component is drawn on the place
     * of JavaFX component to cover it completely and to provide native text editing
     * techniques. Any change of text is immediately reflected in JavaFX text component.
     *
     * @param text text to be shown in the native text input component
     * @param type type of text input component @see com.sun.javafx.scene.control.behavior.TextInputTypes
     * @param width width of JavaFX text input component
     * @param height height of JavaFX text input component
     * @param M standard transformation matrix for drawing the native text component derived from JavaFX component
     */
    void requestInput(String text, int type, double width, double height,
                        double Mxx, double Mxy, double Mxz, double Mxt,
                        double Myx, double Myy, double Myz, double Myt,
                        double Mzx, double Mzy, double Mzz, double Mzt);

    /**
     * Native keyboard for text input is no longer necessary.
     * Keyboard will be hidden and native text input component too.
     */
    void releaseInput();

    public void setRTL(boolean b);

    /**
     * Whether mouse/keyboard events should be sent to this window.
     * @param whether this stage should receive events/focus
     */
    public void setEnabled(boolean enabled);

    /**
     * Return a handle to the native platform window id.
     * @return platform window id, or 0L if there is none.
     */
    public long getRawHandle();

    public static final KeyCodeCombination defaultFullScreenExitKeycombo =
            new KeyCodeCombination(KeyCode.ESCAPE,
            ModifierValue.UP,
            ModifierValue.UP,
            ModifierValue.UP,
            ModifierValue.UP,
            ModifierValue.UP);
}
