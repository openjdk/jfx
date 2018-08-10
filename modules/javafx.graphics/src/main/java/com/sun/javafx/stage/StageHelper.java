/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Utility class class used for accessing certain implementation-specific
 * runtime functionality.
 */
public class StageHelper extends WindowHelper {

    private static final StageHelper theInstance;
    private static StageAccessor stageAccessor;

    static {
        theInstance = new StageHelper();
        Utils.forceInit(Stage.class);
    }

    private static WindowHelper getInstance() {
        return theInstance;
    }

    public static void initHelper(Stage stage) {
        setHelper(stage, getInstance());
    }

    @Override
    protected void visibleChangingImpl(Window window, boolean visible) {
        super.visibleChangingImpl(window, visible);
        stageAccessor.doVisibleChanging(window, visible);
    }

    @Override
    protected void visibleChangedImpl(Window window, boolean visible) {
        super.visibleChangedImpl(window, visible);
        stageAccessor.doVisibleChanged(window, visible);
    }

    public static void initSecurityDialog(Stage stage, boolean securityDialog) {
        stageAccessor.initSecurityDialog(stage, securityDialog);
    }

    public static void setPrimary(Stage stage, boolean primary) {
        stageAccessor.setPrimary(stage, primary);
    }

    public static void setImportant(Stage stage, boolean important) {
        stageAccessor.setImportant(stage, important);
    }

    public static void setStageAccessor(StageAccessor a) {
        if (stageAccessor != null) {
            System.out.println("Warning: Stage accessor already set: " + stageAccessor);
            Thread.dumpStack();
        }
        stageAccessor = a;
    }

    public static StageAccessor getStageAccessor() {
        return stageAccessor;
    }

    public static interface StageAccessor {
        void doVisibleChanging(Window window, boolean visible);
        void doVisibleChanged(Window window, boolean visible);
        public void initSecurityDialog(Stage stage, boolean securityDialog);
        public void setPrimary(Stage stage,  boolean primary);
        public void setImportant(Stage stage,  boolean important);
    }
}
