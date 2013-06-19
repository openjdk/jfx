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

package javafx.print;

/**
 * Class to enumerate the possible duplex (two-sided) printing modes.
 * @since JavaFX 8.0
 */
public enum PrintSides {

    /**
     * One sided printing. Duplex printing is off.
     */
    ONE_SIDED,

    /**
     * Two sided printing where the vertical edge of the paper is to be used
     * for binding such as in a book. In PORTAIT mode printing, the
     * vertical edge is usally the long edge but this is reversed if
     * LANDSCAPE printing is requested
     */
    DUPLEX,

    /**
     * Two sided printing where the horizontal edge of the paper is to be used
     * for binding such as in a notepad. In PORTAIT mode printing, the
     * horizontal edge is usally the short edge but this is reversed if
     * LANDSCAPE printing is requested
     */
    TUMBLE
}

