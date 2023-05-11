/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.stage;

import javafx.stage.Stage;


public class StagePeerListener extends WindowPeerListener {
    private final Stage stage;
    private final StageAccessor stageAccessor;

    public static interface StageAccessor {
        public void setIconified(Stage stage, boolean iconified);
        public void setMaximized(Stage stage, boolean maximized);
        public void setResizable(Stage stage, boolean resizable);
        public void setFullScreen(Stage stage, boolean fs);
        public void setAlwaysOnTop(Stage stage, boolean aot);
    }

    public StagePeerListener(Stage stage, StageAccessor stageAccessor) {
        super(stage);
        this.stage = stage;
        this.stageAccessor = stageAccessor;
    }


    @Override
    public void changedIconified(boolean iconified) {
        stageAccessor.setIconified(stage, iconified);
    }

    @Override
    public void changedMaximized(boolean maximized) {
        stageAccessor.setMaximized(stage, maximized);
    }

    @Override
    public void changedResizable(boolean resizable) {
        stageAccessor.setResizable(stage, resizable);
    }

    @Override
    public void changedFullscreen(boolean fs) {
        stageAccessor.setFullScreen(stage, fs);
    }

    @Override
    public void changedAlwaysOnTop(boolean aot) {
        stageAccessor.setAlwaysOnTop(stage, aot);
    }


}
