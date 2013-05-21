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

package com.sun.javafx.stage;

import javafx.scene.Scene;
import javafx.stage.Window;

import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.tk.Toolkit;

/**
 * User: Artem
 * Date: Dec 21, 2010
 * Time: 4:30:56 PM
 */
public class EmbeddedWindow extends Window {

    private HostInterface host;

    public EmbeddedWindow(HostInterface host) {
        this.host = host;
    }

    /**
     * Specify the scene to be used on this stage.
     */
    @Override public final void setScene(Scene value) {
        super.setScene(value);
    }

    /**
     * Specify the scene to be used on this stage.
     */
    @Override public final void show() {
        super.show();
    }

    @Override
    protected void impl_visibleChanging(boolean visible) {
        super.impl_visibleChanging(visible);
        Toolkit toolkit = Toolkit.getToolkit();
        if (visible && (impl_peer == null)) {
            // Setup the peer
            impl_peer = toolkit.createTKEmbeddedStage(host);
            peerListener = new WindowPeerListener(this);
        }
    }

}
