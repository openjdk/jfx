/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * TKStageListener - Listener for the Stage Peer TKStage to pass updates and events back to the stage
 *
 */
public interface TKStageListener {

    /**
     * The stages peer's location have changed so we need to update the scene
     *
     * @param x the new X
     * @param y The new Y
     */
    public void changedLocation(float x, float y);

    /**
     * The stages peer's size have changed so we need to update the scene
     *
     * @param width The new Width
     * @param height The new Height
     */
    public void changedSize(float width, float height);

    /**
     * The stage's peer should now be displayed with a new UI scale
     *
     * @param xScale the new recommended horizontal scale
     * @param yScale the new recommended vertical scale
     */
    public void changedScale(float xScale, float yScale);

    /**
     * The stages peer focused state has changed.
     *
     * @param focused True if the stage's peer now contains the focus
     * @param cause The cause of (de)activation
     */
    public void changedFocused(boolean focused, FocusCause cause);

    /**
     * The stages peer has become iconified or uniconified
     *
     * @param iconified True if the stage's peer is now iconified
     */
    public void changedIconified(boolean iconified);

    /**
     * The stages peer has become maximized or unmaximized
     *
     * @param maximized True if the stage's peer is now maximized
     */
    public void changedMaximized(boolean maximized);

    /**
     * The stages peer has changed it's "always on top" flag.
     * @param alwaysOnTop
     */
    public void changedAlwaysOnTop(boolean alwaysOnTop);

    /**
     * The stages peer has become resizable or nonresizable
     *
     * @param resizable True if the stage's peer is now resizable
     */
    public void changedResizable(boolean resizable);

    /**
     * The stages peer has changed its full screen status
     *
     * @param fs True if the stage's peer is now full screen, false otherwise
     */
    public void changedFullscreen(boolean fs);

    /**
     * The stage's peer has moved to another screen.
     *
     * @param from An object that identifies the old screen (may be null)
     * @param to An object that identifies the new screen
     */
    public void changedScreen(Object from, Object to);

    /**
     * Called if the window is closing do to something that has happened on the peer. For
     * example the user clicking the close button or choosing quit from the application menu
     * on a mac or right click close on the task bar on windows.
     */
    public void closing();

    /**
     * Called if the stages peer has closed. For example the platform closes the
     * window after user has clicked the close button on its parent window.
     */
    public void closed();

    /**
     * Focus grab has been reset for the stage peer.
     *
     * Called after a previous call to {@link TKStage#grabFocus} when the grab
     * is reset either by user action (e.g. clicking the titlebar of the
     * stage), or via a call to {@link TKStage#ungrabFocus}.
     */
    public void focusUngrab();
}
