/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Describes the mode of data transfer with respect
 * to a drag and drop gesture.
 * @since JavaFX 2.0
 */
public enum
        TransferMode {

    /**
     * Indicates copying of data is supported or intended.
     */
    COPY,

    /**
     * Indicates moving of data is supported or intended.
     */
    MOVE,

    /**
     * Indicates linking of data is supported or intended.
     */
    LINK;


    /**
     * Array containing all transfer modes. This is a convenience constant
     * intended to be used in {@code startDragAndDrop} and
     * {@code DragEvent.acceptTransferModes()} calls.
     */
    public static final TransferMode[] ANY = { COPY, MOVE, LINK };

    /**
     * Array containing transfer modes COPY and MOVE. This is a convenience
     * constant intended to be used in {@code startDragAndDrop} and
     * {@code DragEvent.acceptTransferModes()} calls.
     */
    public static final TransferMode[] COPY_OR_MOVE = { COPY, MOVE };

    /**
     * Empty array of transfer modes. This is a convenience constant
     * intended to be used in {@code startDragAndDrop} and
     * {@code DragEvent.acceptTransferModes()} calls.
     */
    public static final TransferMode[] NONE = { };
}
