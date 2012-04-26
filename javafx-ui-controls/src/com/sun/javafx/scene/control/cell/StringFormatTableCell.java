/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.cell;

import java.text.Format;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.text.TextAlignment;

/**
 * A class containing a {@link TableCell} implementation that draws a 
 * {@link Label} node inside the cell. This Label is exposed to the developer to 
 * allow for easy customization, either via code or using the APIs exposed in 
 * the {@link CSSTableCell} class, from which this class extends.
 * 
 * @param <T> The type of the elements contained within the TableColumn.
 */
public class StringFormatTableCell<S,T> extends TableCell<S,T> {
    
    private Format format;
    
    /**
     * Creates a default {@link LabelTableCell} instance with the label position 
     * set to {@link TextAlignment#LEFT}.
     */
    public StringFormatTableCell() {
        this(TextAlignment.LEFT);
    }
    
    public StringFormatTableCell(Format format) {
        this(TextAlignment.LEFT, format);
    }
    
    /**
     * Creates a {@link LabelTableCell} instance with the label position set
     * to the provided {@link TextAlignment} value.
     */
    public StringFormatTableCell(TextAlignment align) {
        this(align, null);
    }
    
    public StringFormatTableCell(TextAlignment align, Format format) {
        this.getStyleClass().add("label-cell");
        setTextAlignment(align);
        setFormat(format);
    }
    
    // --- Format
    public Format getFormat() { 
        return format; 
    }
    public void setFormat(Format format) { 
        this.format = format;
        doUpdate();
    }
    
    /**
     * A method that can be overridden by the developer to specify an alternate
     * formatting of the given item argument. By default, this just calls 
     * toString() on the item, or returns an empty string if the item is null.
     * 
     * @param item The item for which a String representation is required.
     * @return An empty string if item is null, or a string representation if it 
     *      is not.
     */
    public String toString(T item) {
        return item == null ? "" : item.toString();
    }
    
    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        if (item == getItem()) return;

        super.updateItem(item, empty);

        doUpdate();
    }
    
    private void doUpdate() {
        T item = getItem();
        StringFormatCell.doUpdate(this, item, isEmpty(), getFormat(), toString(item));
    }
}