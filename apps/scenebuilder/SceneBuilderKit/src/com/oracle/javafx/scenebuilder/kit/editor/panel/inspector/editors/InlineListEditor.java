/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Generic class for property editors based on a list of inline items.
 * 
 * 
 */
public abstract class InlineListEditor extends PropertyEditor implements EditorItemDelegate {
    private final VBox vbox = new VBox(1);
    private final List<EditorItem> editorItems = new ArrayList<>();

    public InlineListEditor(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(vbox);
    }

    @Override
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses) {
        reset(propMeta, selectedClasses, true);
    }
    
    public void reset(ValuePropertyMetadata propMeta, Set<Class<?>> selectedClasses, boolean removeAll) {
        super.reset(propMeta, selectedClasses);
        setLayoutFormat(PropertyEditor.LayoutFormat.DOUBLE_LINE);
        reset(removeAll);
    }

    @Override
    protected void valueIsIndeterminate() {
        // Set all the items as undeterminate, whathever is the value
        for (EditorItem editorItem : editorItems) {
            editorItem.setValueAsIndeterminate();
        }
    }

    @Override
    public void commit(EditorItem source) {
//        System.out.println("COMMIT");
        userUpdateValueProperty(getValue());
    }

    @Override
    public void editing(boolean editing, EventHandler<?> editingHandler) {
        editingProperty().setValue(editing);
        setCommitListener(editingHandler);
    }

    @Override
    public void add(EditorItem previousItem, EditorItem newItem) {
//        System.out.println("ADD");
        addItem(previousItem, newItem);
        userUpdateValueProperty(getValue());
    }

    @Override
    public void remove(EditorItem source) {
        // By default, keep 1 item
        remove(source, false);
    }
    
    public void remove(EditorItem source, boolean removeAll) {
//        System.out.println("REMOVE");
        removeItem(source, removeAll);
        userUpdateValueProperty(getValue());
    }

    @Override
    public void up(EditorItem source) {
//        System.out.println("UP");
        upItem(source);
        userUpdateValueProperty(getValue());
    }

    @Override
    public void down(EditorItem source) {
//        System.out.println("DOWN");
        downItem(source);
        userUpdateValueProperty(getValue());
    }

    final protected List<EditorItem> getEditorItems() {
        return editorItems;
    }
    
    final protected EditorItem addItem(EditorItem newItem) {
        return addItem(null, newItem);
    }

    final protected EditorItem addItem(EditorItem previousItem, EditorItem newItem) {
        int index = -1;
        if (previousItem != null) {
            // Add the new item under the source EditorItem
            index = editorItems.indexOf(previousItem) + 1;
        }
        if (index != -1) {
            editorItems.add(index, newItem);
            vbox.getChildren().add(index, newItem.getNode());
        } else {
            editorItems.add(newItem);
            vbox.getChildren().add(newItem.getNode());
        }
        updateMenuItems();
        return newItem;
    }

    protected void removeItem(EditorItem editorItem) {
        // By default, keep 1 item
        removeItem(editorItem, false);
    }
    
    protected void removeItem(EditorItem editorItem, boolean removeAll) {
        if (!removeAll && editorItems.size() == 1) {
            // Do not remove last item, but reset it
            editorItem.reset();
            if (editorItem.getRemoveMenuItem() != null) {
                editorItem.getRemoveMenuItem().setDisable(true);
            }
            return;
        }
        editorItems.remove(editorItem);
        vbox.getChildren().remove(editorItem.getNode());
        updateMenuItems();
    }

    protected void upItem(EditorItem editorItem) {
        int indexItem = editorItems.indexOf(editorItem);
        // item should be in the list, and not the 1st item
        assert (indexItem != -1) && (indexItem != 0);
        assert vbox.getChildren().indexOf(editorItem.getNode()) == indexItem;
        Collections.swap(editorItems, indexItem, indexItem - 1);
        EditorUtils.swap(vbox.getChildren(), indexItem, indexItem - 1);
        vbox.requestLayout();
        updateMenuItems();
    }

    protected void downItem(EditorItem editorItem) {
        int indexItem = editorItems.indexOf(editorItem);
        // item should be in the list, and not the last item
        assert (indexItem != -1) && (indexItem != editorItems.size() - 1);
        assert vbox.getChildren().indexOf(editorItem.getNode()) == indexItem;
        Collections.swap(editorItems, indexItem, indexItem + 1);
        EditorUtils.swap(vbox.getChildren(), indexItem, indexItem + 1);
        updateMenuItems();
    }

    protected void reset() {
        // By default, keep 1 item
        reset(false);
    }
    
    protected void reset(boolean removeAll) {
        List<EditorItem> items = new ArrayList<>(editorItems);
        for (EditorItem editorItem : items) {
            removeItem(editorItem, removeAll);
        }
        updateMenuItems();
    }

    private void updateMenuItems() {
        for (int ii = 0; ii < editorItems.size(); ii++) {
            EditorItem item = editorItems.get(ii);
            if (item.getMoveUpMenuItem() == null || item.getMoveDownMenuItem() == null) {
                continue;
            }
            if (ii == 0) {
                // first item
                item.getMoveUpMenuItem().setDisable(true);
            } else {
                item.getMoveUpMenuItem().setDisable(false);
            }
            if (ii == (editorItems.size() - 1)) {
                // last item
                item.getMoveDownMenuItem().setDisable(true);
            } else {
                item.getMoveDownMenuItem().setDisable(false);
            }
        }
    }

}
