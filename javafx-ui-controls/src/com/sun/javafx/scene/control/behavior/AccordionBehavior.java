/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.FocusModel;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;

public class AccordionBehavior extends BehaviorBase<Accordion> {

    private AccordionFocusModel focusModel;
    
    public AccordionBehavior(Accordion accordion) {
        super(accordion);
        focusModel = new AccordionFocusModel(accordion);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    private static final String HOME = "Home";
    private static final String END = "End";
    private static final String PAGE_UP = "Page_Up";
    private static final String PAGE_DOWN = "Page_Down";
    private static final String CTRL_PAGE_UP = "Ctrl_Page_Up";
    private static final String CTRL_PAGE_DOWN = "Ctrl_Page_Down";
    private static final String CTRL_TAB = "Ctrl_Tab";
    private static final String CTRL_SHIFT_TAB = "Ctrl_Shift_Tab";

    protected static final List<KeyBinding> ACCORDION_BINDINGS = new ArrayList<KeyBinding>();
    static {
        ACCORDION_BINDINGS.addAll(TRAVERSAL_BINDINGS);
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.HOME, HOME));
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.END, END));
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.PAGE_UP, PAGE_UP));
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.PAGE_DOWN, PAGE_DOWN));
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.PAGE_UP, CTRL_PAGE_UP).ctrl());
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.PAGE_DOWN, CTRL_PAGE_DOWN).ctrl());
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.TAB, CTRL_TAB).ctrl());
        ACCORDION_BINDINGS.add(new KeyBinding(KeyCode.TAB, CTRL_SHIFT_TAB).shift().ctrl());
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return ACCORDION_BINDINGS;
    }

    @Override protected void callAction(String name) {   
        Accordion accordion = getControl();
        if ("TraverseLeft".equals(name) || "TraverseUp".equals(name) || PAGE_UP.equals(name)) {
            if (focusModel.getFocusedIndex() != -1 && accordion.getPanes().get(focusModel.getFocusedIndex()).isFocused()) {
                focusModel.focusPrevious();
                int next = focusModel.getFocusedIndex();
                accordion.getPanes().get(next).requestFocus();
                if (PAGE_UP.equals(name)) {
                    accordion.getPanes().get(next).setExpanded(true);
                }
            }
        } else if ("TraverseRight".equals(name) || "TraverseDown".equals(name) || PAGE_DOWN.equals(name)) {
            if (focusModel.getFocusedIndex() != -1 && accordion.getPanes().get(focusModel.getFocusedIndex()).isFocused()) {
                focusModel.focusNext();
                int next = focusModel.getFocusedIndex();
                accordion.getPanes().get(next).requestFocus();
                if (PAGE_DOWN.equals(name)) {
                    accordion.getPanes().get(next).setExpanded(true);
                }
            }            
        } else if (CTRL_TAB.equals(name) || CTRL_PAGE_DOWN.equals(name)) {
            focusModel.focusNext();
            if (focusModel.getFocusedIndex() != -1) {
                int next = focusModel.getFocusedIndex();
                accordion.getPanes().get(next).requestFocus();
                accordion.getPanes().get(next).setExpanded(true);
            }
        } else if (CTRL_SHIFT_TAB.equals(name) || CTRL_PAGE_UP.equals(name)) {
            focusModel.focusPrevious();
            if (focusModel.getFocusedIndex() != -1) {
                int next = focusModel.getFocusedIndex();            
                accordion.getPanes().get(next).requestFocus();
                accordion.getPanes().get(next).setExpanded(true);
            }
        } else if (HOME.equals(name)) {
            if (focusModel.getFocusedIndex() != -1 && accordion.getPanes().get(focusModel.getFocusedIndex()).isFocused()) {
                TitledPane tp = accordion.getPanes().get(0);
                tp.requestFocus();
                tp.setExpanded(!tp.isExpanded());
            }
        } else if (END.equals(name)) {
            if (focusModel.getFocusedIndex() != -1 && accordion.getPanes().get(focusModel.getFocusedIndex()).isFocused()) {
                TitledPane tp = accordion.getPanes().get(accordion.getPanes().size() - 1);
                tp.requestFocus();
                tp.setExpanded(!tp.isExpanded());
            }
        } else {
            super.callAction(name);
        }
    }

    static class AccordionFocusModel extends FocusModel<TitledPane> {

        private final Accordion accordion;
        
        public AccordionFocusModel(final Accordion accordion) {
            if (accordion == null) {
                throw new IllegalArgumentException("Accordion can not be null");
            }
            this.accordion = accordion;
            this.accordion.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        if (accordion.getExpandedPane() != null) {
                            accordion.getExpandedPane().requestFocus();
                        } else {
                            // TODO need to detect the focus direction
                            // to selected the first panel when TAB is pressed
                            // or select the last panel when SHIFT TAB is pressed.
                            accordion.getPanes().get(0).requestFocus();
                        }
                    }
                }
            });
            this.accordion.getPanes().addListener(new ListChangeListener<TitledPane>() {
                @Override public void onChanged(Change<? extends TitledPane> c) {
                    // TODO need to remove the listeners.
                    while (c.next()) {
                        if (c.wasAdded()) {
                            for (final TitledPane tp: c.getAddedSubList()) {
                                tp.focusedProperty().addListener(new ChangeListener<Boolean>() {
                                    @Override
                                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                                        if (newValue) {
                                            focus(accordion.getPanes().indexOf(tp));
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });

            for (final TitledPane tp: this.accordion.getPanes()) {
                tp.focusedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            focus(accordion.getPanes().indexOf(tp));
                        }
                    }
                });
            }
        }

        @Override
        protected int getItemCount() {
            final ObservableList<TitledPane> panes = accordion.getPanes();
            return panes == null ? 0 : panes.size();
        }

        @Override
        protected TitledPane getModelItem(int row) {
            final ObservableList<TitledPane> panes = accordion.getPanes();
            if (panes == null) return null;
            if (row < 0) return null;
            return panes.get(row%panes.size());
        }

        @Override public void focusPrevious() {
            if (getFocusedIndex() <= 0) {
                focus(accordion.getPanes().size() - 1);
            } else {
                focus((getFocusedIndex() - 1)%accordion.getPanes().size());
            }
        }

        @Override public void focusNext() {
            if (getFocusedIndex() == -1) {
                focus(0);
            } else {
                focus((getFocusedIndex() + 1)%accordion.getPanes().size());
            }
        }
    }
}
