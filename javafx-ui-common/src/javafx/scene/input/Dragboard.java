/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import java.util.Set;

import com.sun.javafx.tk.TKClipboard;

/**
 * A drag and drop specific {@link Clipboard}.
 * @profile common
 */
public final class Dragboard extends Clipboard {

    Dragboard(TKClipboard peer) {
        super(peer);
    }

    /**
     * Gets set of transport modes supported by source of this drag opeation.
     * @return set of supported transfer modes
     */
    public final Set<TransferMode> getTransferModes() {
        return peer.getTransferModes();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public TKClipboard impl_getPeer() {
        return peer;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static Dragboard impl_create(TKClipboard peer) {
        return new Dragboard(peer);
    }

    // TODO: DragView support
//    /**
//     * Visual representation of data being transfered in a drag and drop gesture.
//     * This will be shown to the side of the mouse cursor as it is moved around
//     * the screen.
//     */
//    public Node dragView;
//
//    /**
//     * Specifies the opacity of the dragView node as the drag occurs. If this
//     * is not specified, a default opacity of 0.65 will be applied.
//     */
//    public float dragViewOpacity = 0.65f;
}
