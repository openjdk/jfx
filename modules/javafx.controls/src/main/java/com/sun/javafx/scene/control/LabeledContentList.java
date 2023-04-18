/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import javafx.collections.ObservableListBase;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import java.util.List;
import java.util.Objects;

/**
 * Specialized list that represents the content model of a {@link Labeled} control.
 * <p>
 * This list contains any combination or none of the following:
 * <ul>
 *     <li>A {@code String} object, representing {@link Labeled#textProperty()}
 *     <li>A {@code Node} object, representing {@link Labeled#graphicProperty()}
 * </ul>
 */
public final class LabeledContentList extends ObservableListBase<Object> {

    private String text;
    private Node graphic;

    public void setText(String value) {
        if (text == null && value == null || text != null && Objects.equals(text, value)) {
            return;
        }

        beginChange();

        if (text != null) {
            if (value != null) {
                nextReplace(0, 1, List.of(text));
                text = value;
            } else {
                nextRemove(0, text);
                text = null;
            }
        } else {
            nextAdd(0, 1);
            text = value;
        }

        endChange();
    }

    public void setGraphic(Node value) {
        if (graphic == value) {
            return;
        }

        beginChange();

        if (graphic != null) {
            if (value != null) {
                nextReplace(0, 1, List.of(graphic));
                graphic = value;
            } else {
                nextRemove(0, graphic);
                graphic = null;
            }
        } else {
            nextAdd(0, 1);
            graphic = value;
        }

        endChange();
    }

    @Override
    public Object get(int index) {
        Objects.checkIndex(index, size());
        return switch(index) {
            case 0 -> text != null ? text : graphic;
            case 1 -> graphic;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public int size() {
        if (text != null) {
            if (graphic != null) {
                return 2;
            }

            return 1;
        } else if (graphic != null) {
            return 1;
        }

        return 0;
    }

}
