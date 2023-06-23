/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>The <code>javafx.scene.control.cell</code> package is where all cell-related
 * classes are located, other than the core classes such as
 * {@link javafx.scene.control.Cell Cell}, {@link javafx.scene.control.IndexedCell IndexedCell},
 * {@link javafx.scene.control.ListCell ListCell}, {@link javafx.scene.control.TreeCell TreeCell},
 * and {@link javafx.scene.control.TableCell TableCell}. At present this package
 * is relatively bare, but it is where future cell-related classes will be located.</p>
 *
 * <p>It is important to note that whilst most cells in this package are editable,
 *     for a cells editing functionality to be enabled it is required that all
 *     related classes have editing enabled. For example, in a TableView, both
 *     the TableView and the relevant TableColumn must have setEditing(true) called.
 */
package javafx.scene.control.cell;
