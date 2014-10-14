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
package com.oracle.javafx.scenebuilder.kit.editor.panel.css;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

/**
 * CSS panel selection path handling.
 *
 * @treatAsPrivate
 */
// TODO : should have its UI defined in an fxml file.
public class SelectionPath extends HBox {

    private Path currentPath;
    private String selector;
    private final ObjectProperty<Item> selected = new SimpleObjectProperty<>();

    public SelectionPath() {
        setSpacing(5);
    }

    public void select(Object obj) {
        assert currentPath != null && !currentPath.getItems().isEmpty();
        Path newPath = new Path(lookupPath(currentPath.getItems().get(0), obj));
        setSelectionPath(newPath);
        int lastIndex = newPath.getItems().size() - 1;
        selected.set(newPath.getItems().get(lastIndex));
    }

    public static List<Item> lookupPath(Item current, Object obj) {
        List<Item> ret = new ArrayList<>();
        if (current.getItem() == obj) {
            ret.add(current);
        } else {
            for (Item c : current.getChildren()) {
                List<Item> branch = lookupPath(c, obj);
                if (!branch.isEmpty()) {
                    branch.add(0, current);
                    ret = branch;
                    break;
                }
            }
        }
        return ret;
    }

    public void setSelectionPath(Path selection) {
        assert selection != null;
        currentPath = selection;
        getChildren().clear();
        final List<Item> iterationPath = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selection.getItems().size(); i++) {
            final Item item = selection.getItems().get(i);
            iterationPath.add(item);
            final Hyperlink label = new Hyperlink(item.getName());
            label.setMaxHeight(Double.MAX_VALUE);
            builder.append(item.getName());
            final List<Item> myPath = new ArrayList<>(iterationPath);
            final EventHandler<ActionEvent> eh = event -> {
                setSelectionPath(new Path(myPath));
                select(item.getItem());
                // Set the focus to the css panel
                requestFocus();
            };
            label.setOnAction(eh);

            String optional = item.getOptional();
            if (optional != null) {
                HBox hbox = new HBox(0);
                hbox.getChildren().add(label);
                final Hyperlink opt = new Hyperlink(" " + optional);//NOI18N
                opt.setOnAction(eh);
                hbox.setOnMouseEntered(new MouseEnterListener(opt, label));
                hbox.setOnMouseExited(new MouseExitedListener(opt, label));
                opt.setMaxHeight(Double.MAX_VALUE);
                opt.getStyleClass().add("styleable-path-optional-label");//NOI18N
                hbox.getChildren().add(opt);
                getChildren().add(hbox);
            } else {
                getChildren().add(label);
            }
            if (!item.getChildren().isEmpty()) {
                // Do we have a next child in the selection path?
                Item next = null;
                if (i < selection.getItems().size() - 1) {
                    next = selection.getItems().get(i + 1);
                }
                ChildButton mb = new ChildButton(item.getChildren(), new ArrayList<>(iterationPath), next);
                getChildren().add(mb);
            }
            if (i < selection.getItems().size() - 1) {
                builder.append(" ");//NOI18N
            }
        }
        selector = builder.toString();
    }

    private static class MouseEnterListener implements EventHandler<MouseEvent> {

        Hyperlink opt, label;

        public MouseEnterListener(Hyperlink opt, Hyperlink label) {
            this.opt = opt;
            this.label = label;
        }

        @Override
        public void handle(MouseEvent event) {
            opt.setUnderline(true);
            label.setUnderline(true);
        }
    }

    private static class MouseExitedListener implements EventHandler<MouseEvent> {

        Hyperlink opt, label;

        public MouseExitedListener(Hyperlink opt, Hyperlink label) {
            this.opt = opt;
            this.label = label;
        }

        @Override
        public void handle(MouseEvent event) {
            opt.setUnderline(false);
            label.setUnderline(false);
        }
    }

    public ObjectProperty<Item> selected() {
        return selected;
    }

    @Override
    public String toString() {
        return selector;
    }

    // TODO : should have an fxml file
    private class ChildButton extends StackPane {

        private double yOffset;
        private Button pathButton = new Button();

        private ChildButton(List<Item> children, final List<Item> childrenPath, final Item selectedChild) {
            
            Region pathButtonGraphic = new Region();
            pathButtonGraphic.getStyleClass().add("styleable-path-button-shape");//NOI18N
            pathButton.setGraphic(pathButtonGraphic);
            
            getChildren().add(pathButton);
            final ContextMenu menu = new ContextMenu();
            for (final Item c : children) {
                CheckMenuItem mi = new ChildMenuItem(c);
                final List<Item> childPath = new ArrayList<>(childrenPath);
                childPath.add(c);
                mi.setOnAction(event -> {
                    setSelectionPath(new Path(childPath));
                    selected.set(c);
                });
                menu.getItems().add(mi);
            }
            menu.setOnShowing(event -> {
                for (MenuItem m : menu.getItems()) {
                    assert m instanceof ChildMenuItem;
                    ChildMenuItem cm = (ChildMenuItem) m;
                    cm.setSelected(cm.item == selectedChild);
                }
                positionMenu(menu);
            });

            menu.heightProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                if (newValue.doubleValue() <= 0) {
                    return;
                }
                double singleHeight = newValue.doubleValue() / menu.getItems().size();
                int index = 0;
                boolean found = false;
                for (MenuItem m : menu.getItems()) {
                    assert m instanceof ChildMenuItem;
                    ChildMenuItem cm = (ChildMenuItem) m;
                    index += 1;
                    if (cm.isSelected()) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    yOffset = singleHeight * (index - 1);
                    positionMenu(menu);
                }
            });

            pathButton.setOnMouseClicked(event -> {
                // Show popup. We can't compute the delta now...
                if (!menu.isShowing()) {
                    menu.show(ChildButton.this, Side.RIGHT, 0, 0);
                }
            });
        }

        private void positionMenu(ContextMenu menu) {
            if (yOffset != 0) {
                menu.setY(menu.getY() - yOffset);
            }
        }

        private class ChildMenuItem extends CheckMenuItem {

            private final Item item;

            ChildMenuItem(Item item) {
                super(item.getName());
                this.item = item;
            }
        }
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class Item {

        private final Object item;
        private final String name;
        private final String optional;
        private final List<Item> children = new ArrayList<>();

        public Item(Object item, String name, String optional) {
            this.item = item;
            this.name = name;
            this.optional = optional;
        }

        /**
         * @return the item
         */
        public Object getItem() {
            return item;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the name
         */
        public String getOptional() {
            return optional;
        }

        /**
         * @return the children
         */
        public List<Item> getChildren() {
            return children;
        }
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class Path {

        private final List<Item> items;

        public Path(List<Item> items) {
            this.items = items;
        }

        /**
         * @return the items
         */
        public List<Item> getItems() {
            return items;
        }
    }
}
