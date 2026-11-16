/*
 * Copyright (c) 2008, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import com.sun.glass.events.WindowEvent;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Window;
import com.sun.glass.ui.Window.Level;

import com.sun.javafx.tk.FocusCause;
import com.sun.javafx.tk.TKStageListener;

import java.util.function.Supplier;

class GlassWindowEventHandler extends Window.EventHandler implements Supplier<Void> {

    private final WindowStage stage;

    private Window window;
    private int type;

    public GlassWindowEventHandler(WindowStage stage) {
        this.stage = stage;
    }

    @Override
    public Void get() {
        if (stage == null) {
            return null;
        }

        // Read the listener once as it can be cleared while this event is
        // being processed, e.g. if an application listener hides the stage
        TKStageListener listener = stage.stageListener;
        if (listener == null) {
            return null;
        }

        switch (type) {
            case WindowEvent.MINIMIZE:
                listener.changedIconified(true);
                break;
            case WindowEvent.MAXIMIZE:
                listener.changedIconified(false);
                listener.changedMaximized(true);
                forceRepaint();
                break;
            case WindowEvent.RESTORE:
                listener.changedIconified(false);
                listener.changedMaximized(false);
                forceRepaint();
                break;
            case WindowEvent.MOVE: {
                float wx = window.getX();
                float wy = window.getY();
                Screen screen = window.getScreen();
                float newx, newy;
                if (screen != null) {
                    float pScaleX = screen.getPlatformScaleX();
                    float pScaleY = screen.getPlatformScaleY();
                    float sx = screen.getX();
                    float sy = screen.getY();
                    float px = screen.getPlatformX();
                    float py = screen.getPlatformY();
                    newx = sx + (wx - px) / pScaleX;
                    newy = sy + (wy - py) / pScaleY;
                } else {
                    newx = wx;
                    newy = wy;
                }
                listener.changedLocation(newx, newy);
                //We need to sync the new x,y for painting
                if (!Application.GetApplication().hasWindowManager()) {
                    QuantumToolkit.runWithRenderLock(() -> {
                        GlassScene scene = stage.getScene();
                        if (scene != null) {
                            scene.updateSceneState();
                        }
                        return null;
                    });
                }
                break;
            }
            case WindowEvent.RESIZE: {
                float pScaleX = window.getPlatformScaleX();
                float pScaleY = window.getPlatformScaleY();
                listener.changedSize(window.getWidth()  / pScaleX,
                                     window.getHeight() / pScaleY);
                 break;
            }
            case WindowEvent.RESCALE: {
                float outScaleX = window.getOutputScaleX();
                float outScaleY = window.getOutputScaleY();
                listener.changedScale(outScaleX, outScaleY);
                forceRepaint();
                break;
            }
            case WindowEvent.FOCUS_GAINED:
                WindowStage.addActiveWindow(stage);
                listener.changedFocused(true, FocusCause.ACTIVATED);
                break;
            case WindowEvent.FOCUS_LOST:
                listener.changedFocused(false, FocusCause.DEACTIVATED);
                break;
            case WindowEvent.FOCUS_UNGRAB:
                listener.focusUngrab();
                break;
            case WindowEvent.FOCUS_GAINED_FORWARD:
                WindowStage.addActiveWindow(stage);
                listener.changedFocused(true, FocusCause.TRAVERSED_FORWARD);
                break;
            case WindowEvent.FOCUS_GAINED_BACKWARD:
                WindowStage.addActiveWindow(stage);
                listener.changedFocused(true, FocusCause.TRAVERSED_BACKWARD);
                break;
            case WindowEvent.FOCUS_DISABLED:
                stage.handleFocusDisabled();
                break;
            case WindowEvent.DESTROY:
                stage.setPlatformWindowClosed();
                WindowStage.removeActiveWindow(stage);
                listener.closed();
                break;
            case WindowEvent.CLOSE:
                listener.closing();
                break;
            default:
                if (QuantumToolkit.verbose) {
                    System.err.println("GlassWindowEventHandler: unknown type: " + type);
                }
                break;
        }
        return null;
    }

    /**
     * Used to trigger a repaint after a window leaves an iconified state.
     */
    private void forceRepaint() {
        QuantumToolkit.runWithRenderLock(() -> {
            GlassScene scene = stage.getScene();
            if (scene != null) {
                scene.entireSceneNeedsRepaint();
                scene.updateSceneState();
            }
            return null;
        });
    }

    @Override
    public void handleLevelEvent(int level) {
        QuantumToolkit.runWithoutRenderLock(() -> {
            TKStageListener listener = stage.stageListener;
            if (listener != null) {
                listener.changedAlwaysOnTop(level != Level.NORMAL);
            }
            return null;
        });
    }

    @Override
    public void handleWindowEvent(final Window window, final long time, final int type) {
        this.window = window;
        this.type = type;

        QuantumToolkit.runWithoutRenderLock(this);
    }

    @Override
    public void handleScreenChangedEvent(Window window, long time, Screen oldScreen, Screen newScreen) {
        GlassScene scene = stage.getScene();
        if (scene != null) {
            QuantumToolkit.runWithRenderLock(() -> {
                scene.entireSceneNeedsRepaint();
                scene.updateSceneState();
                return null;
            });
        }

        QuantumToolkit.runWithoutRenderLock(() -> {
            TKStageListener listener = stage.stageListener;
            if (listener != null) {
                listener.changedScreen(oldScreen, newScreen);
            }
            return null;
        });
    }
}
