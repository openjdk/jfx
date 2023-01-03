/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.NamedArg;


/**
 * A wrapper class for use by the column resize policies offered by
 * controls such as {@link TableView} and {@link TreeTableView}.
 * @since JavaFX 8.0
 */
public abstract class ResizeFeaturesBase<S> {
  private final TableColumnBase<S,?> column;
  private final Double delta;

  /**
   * Creates an instance of this class, with the provided TableColumnBase and
   * delta values being set and stored in this immutable instance.
   *
   * @param column The column upon which the resize is occurring, or null
   *      if this ResizeFeatures instance is being created as a result of a
   *      resize operation.
   * @param delta The amount of horizontal space added or removed in the
   *      resize operation.
   */
  public ResizeFeaturesBase(@NamedArg("column") TableColumnBase<S,?> column, @NamedArg("delta") Double delta) {
      this.column = column;
      this.delta = delta;
  }

  /**
   * Returns the width of the area available for columns.
   *
   * @return the width availabe for columns
   *
   * @since 20
   */
  public abstract double getContentWidth();

  /**
   * Returns the associated TreeView or TreeTableView control.
   *
   * @return the control in which the resize is occurring
   *
   * @since 20
   */
  public abstract Control getTableControl();

  /**
   * Returns the column upon which the resize is occurring, or null
   * if this ResizeFeatures instance was created as a result of a
   * resize operation.
   * @return the column upon which the resize is occurring
   */
  public TableColumnBase<S,?> getColumn() { return column; }

  /**
   * Returns the amount of horizontal space added or removed in the
   * resize operation.
   * @return the amount of horizontal space added or removed in the
   * resize operation
   */
  public Double getDelta() { return delta; }

  /**
   * Sets the column width during the resizing pass.
   *
   * @param col column being changed
   * @param width desired column width
   *
   * @since 20
   */
  public void setColumnWidth(TableColumnBase<S,?> col, double width) {
      col.doSetWidth(width);
  }
}
