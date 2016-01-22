/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed;

import com.sun.javafx.cursor.CursorFrame;

/*
 * An interface for embedding container. All the methods in this
 * interface are to be used by embedded FX application to request
 * or notify embedding application about various changes, for
 * example, when embedded FX scene changes is painted, it calls
 * HostInterface.repaint() to notify that the container should
 * be eventually repainted to reflect new scene pixels.
 *
 */
public interface HostInterface {

    public void setEmbeddedStage(EmbeddedStageInterface embeddedStage);
    public void setEmbeddedScene(EmbeddedSceneInterface embeddedScene);

    /*
     * Called by embedded FX scene to request focus to this container
     * in an embedding app.
     */
    public boolean requestFocus();

    /*
     * Called by embedded FX scene to traverse focus to a component
     * which is next/previous to this container in an emedding app.
     */
    public boolean traverseFocusOut(boolean forward);

    /*
     * Called by embedded FX scene when its opacity is changed, so
     * embedding container will later draw the scene pixels with
     * a new opacity value.
     */
/*
    public void setOpacity(float opacity);
*/

    /*
     * Called by embedded FX scene when it is repainted, so embedding
     * container will eventually repaint itself to reflect the changes.
     */
    public void repaint();

    /*
     * Called by embedded FX stage when its size is changed, so
     * embedding container will later report the size as the preferred size.
     */
    public void setPreferredSize(int width, int height);

    /*
     * Called by embedded FX stage when FX enables/disables the stage.
     */
    public void setEnabled(boolean enabled);

    /*
     * Called by embedded FX scene when its cursor is changed.
     */
    public void setCursor(CursorFrame cursorFrame);

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
}
