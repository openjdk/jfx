/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Collation specifies whether or not media sheets of each copy
 * of a printed document in a job are to be in sequence.
 * It has no effect or relevance unless multiple job copies are printed.
 *
 * @since JavaFX 8.0
 */
public enum Collation {

    /**
     * The same numbered pages are consecutive in the output,
     * For example: 2 copies of a document with 2 pages is printed :
     * Page 1, Page 1, Page 2, Page 2.
     */
    UNCOLLATED,

    /**
     * Each copy of a document is printed together.
     * FOr example: 2 copies of a document with 2 pages is printed :
     * Page 1, Page 2, Page 1, Page 2.
     */
    COLLATED
}
