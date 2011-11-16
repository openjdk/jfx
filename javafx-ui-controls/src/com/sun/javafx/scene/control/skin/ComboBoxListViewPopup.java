/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.javafx.preview.control.ComboBox;
import com.sun.javafx.Utils;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;

public class ComboBoxListViewPopup<T> extends PopupControl {
    private ComboBox<T> comboBox;

    public ComboBoxListViewPopup(ComboBox<T> comboBox) {
        this.comboBox = comboBox;
        getStyleClass().add("combo-box-popup");
    }

    public ComboBox<T> getComboBox() {
        return comboBox;
    }

    public void show(Node parent) {
        if (getSkin() == null) {
            getScene().getRoot().impl_processCSS(true);
        }

        Point2D p = Utils.pointRelativeTo(parent, getSkin().getNode(), HPos.CENTER, VPos.BOTTOM, -7, -10, false);
        super.show(parent.getScene().getWindow(), p.getX(), p.getY());
    }
    
    public ListView<T> getListView() {
        if (getSkin() == null) {
            getScene().getRoot().impl_processCSS(true);
        }
        
        Skin s = getSkin();
        
        if (s instanceof ComboBoxListViewPopupSkin) {
            return ((ComboBoxListViewPopupSkin)s).getListView();
        }
        
        return null;
    }
    
    public double getPrefWidth(double height) {
        ListView lv = getListView();
        if (lv != null) {
            return lv.prefWidth(height);
        }
        
        return super.getPrefWidth();
    }
}
