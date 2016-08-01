/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * An interface for embedded FX stage peer. It is used by HostInterface
 * object to send various notifications to the stage, for example,
 * when the size of embedding container is changed.
 *
 */
public interface EmbeddedStageInterface {

    public void setLocation(int x, int y);

    /*
     * A notification about the embedding container is resized.
     */
    public void setSize(int width, int height);

    /*
     * A notification about the embedding container received or lost
     * input focus because of various reasons, for example, it was
     * traversed forward in the focus chain in embedding app.
     */
    public void setFocused(boolean focused, int focusCause);

    /*
     * FOCUS_UNGRAB notification.
     */
    public void focusUngrab();
}
