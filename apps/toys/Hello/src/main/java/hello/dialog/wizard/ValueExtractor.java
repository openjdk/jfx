/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package hello.dialog.wizard;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class ValueExtractor {

    @SuppressWarnings("rawtypes")
    private static final Map<Class, Callback> valueExtractors = new HashMap<>();
    static {
        addValueExtractor(CheckBox.class,       cb -> cb.isSelected());
        addValueExtractor(ChoiceBox.class,      cb -> cb.getValue());
        addValueExtractor(ComboBox.class,       cb -> cb.getValue());
        addValueExtractor(DatePicker.class,     dp -> dp.getValue());
        addValueExtractor(PasswordField.class,  pf -> pf.getText());
        addValueExtractor(RadioButton.class,    rb -> rb.isSelected());
        addValueExtractor(Slider.class,         sl -> sl.getValue());
        addValueExtractor(TextArea.class,       ta -> ta.getText());
        addValueExtractor(TextField.class,      tf -> tf.getText());
        
        addValueExtractor(ListView.class, lv -> {
            MultipleSelectionModel<?> sm = lv.getSelectionModel();
            return sm.getSelectionMode() == SelectionMode.MULTIPLE ? sm.getSelectedItems() : sm.getSelectedItem();
        });
        addValueExtractor(TreeView.class, tv -> {
            MultipleSelectionModel<?> sm = tv.getSelectionModel();
            return sm.getSelectionMode() == SelectionMode.MULTIPLE ? sm.getSelectedItems() : sm.getSelectedItem();
        });
        addValueExtractor(TableView.class, tv -> {
            MultipleSelectionModel<?> sm = tv.getSelectionModel();
            return sm.getSelectionMode() == SelectionMode.MULTIPLE ? sm.getSelectedItems() : sm.getSelectedItem();
        });
        addValueExtractor(TreeTableView.class, tv -> {
            MultipleSelectionModel<?> sm = tv.getSelectionModel();
            return sm.getSelectionMode() == SelectionMode.MULTIPLE ? sm.getSelectedItems() : sm.getSelectedItem();
        });
    }
    
    private ValueExtractor() {
        // no-op
    }
    
    public static <T> void addValueExtractor(Class<T> clazz, Callback<T, Object> extractor) {
        valueExtractors.put(clazz, extractor);
    }
    
    /**
     * Attempts to return a value for the given Node. This is done by checking
     * the map of value extractors, contained within this class. This
     * map contains value extractors for common UI controls, but more extractors
     * can be added by calling {@link #addValueExtractor(Class, javafx.util.Callback)}.
     * 
     * @param n The node from whom a value will hopefully be extracted.
     * @return The value of the given node.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object getValue(Node n) {
        Object value = null;
        
        if (value == null && valueExtractors.containsKey(n.getClass())) {
            Callback callback = valueExtractors.get(n.getClass());
            value = callback.call(n);
        }
        
        return value;
    }
}