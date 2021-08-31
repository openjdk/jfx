/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.glass.ui.monocle.AcceleratedScreen;
import com.sun.prism.paint.Color;
import java.security.AccessController;
import java.security.PrivilegedAction;

class MonocleGLDrawable extends GLDrawable {

    @SuppressWarnings("removal")
    private static final boolean transparentFramebuffer =
            AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean.getBoolean("com.sun.javafx.transparentFramebuffer"));

    AcceleratedScreen accScreen;

    MonocleGLDrawable(GLPixelFormat pixelFormat, AcceleratedScreen accScreen) {

        super(0L, pixelFormat);
        this.accScreen = accScreen;
    }

    MonocleGLDrawable(long nativeWindow, GLPixelFormat pixelFormat,
                      AcceleratedScreen accScreen) {
        super(nativeWindow, pixelFormat);
        this.accScreen = accScreen;
    }

    @Override
    boolean swapBuffers(GLContext glCtx) {

       boolean retval = accScreen.swapBuffers();
       // boolean retval = nSwapBuffers(getNativeDrawableInfo());

        // TODO: This looks hacky. Need to find a better approach.
        // For Monocle, we are painting in Z-order from the back,
        // possibly (likely) with an app that does not cover the
        // full screen. We need to start each paint with an empty canvas.
        // The assumption here was that we would do that by clearing the buffer.
        glCtx.clearBuffers(
                transparentFramebuffer ? Color.TRANSPARENT : Color.BLACK,
                true, true, true);
        return retval;

    }
}
