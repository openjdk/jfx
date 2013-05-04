/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import javafx.scene.Camera;
import javafx.scene.SubScene;

public abstract class SubSceneAccess {
    private static SubSceneAccess access;

    public static synchronized void setSubSceneAccess(SubSceneAccess acc) {
        if (access == null) {
            access = acc;
        }
    }

    /**
     * Return the accessor created by the Node class when its loaded.
     */
    public static synchronized SubSceneAccess getSubSceneAccess() {
        return access;
    }

    public abstract boolean isDepthBuffer(SubScene subScene);
    public abstract Camera getEffectiveCamera(SubScene subScene);
}
