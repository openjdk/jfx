/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Platform;

class AndroidInputProcessor {

    private final AndroidInputDevice device;
    final TouchPipeline touchPipeline;
    private final KeyInput keyInput = new KeyInput();

    AndroidInputProcessor(AndroidInputDevice device) {
        this.device = device;
        touchPipeline = new TouchPipeline();
        touchPipeline.add(TouchInput.getInstance().getBasePipeline());
    }

    void pushEvent(TouchState state) {
        touchPipeline.pushState(state);
    }

    /**
     * Called when events are waiting on the input device to be processed.
     * Called on the runnable processor provided to the input device.
     *
     * @param device The device on which events are pending
     */
    void processEvents(AndroidInputDevice device) {
        touchPipeline.pushState(null);
    }

    synchronized void pushKeyEvent(KeyState keyState) {
        keyInput.setState(keyState);
    }

    synchronized void dispatchKeyEvent(int type, int key, char[] chars, int modifiers) {
        Platform.runLater( () -> {
            MonocleWindow window = (MonocleWindow) MonocleWindowManager.getInstance().getFocusedWindow();
            if (window == null) {
                return;
            }
            MonocleView view = (MonocleView) window.getView();
            if (view == null) {
                return;
            }
            RunnableProcessor.runLater( () ->  view.notifyKey(type, key, chars, modifiers));
        });
    }

}
