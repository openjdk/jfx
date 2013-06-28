/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.infrastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.chart.Axis;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Tab;

import com.sun.javafx.binding.ExpressionHelper;

public final class ControlTestUtils {
    private ControlTestUtils() { }
    /*********************************************************************
     * Following 2 methods are for Tab                                   *
     ********************************************************************/
    public static void assertStyleClassContains(Tab tab, String styleClass) {
        assertStyleClassContains(
                "The style class " + styleClass + " was not set on the Tab " + tab,
                tab, styleClass);
    }
    
    public static void assertStyleClassContains(String message, Tab tab, String styleClass) {
        assertTrue(message, tab.getStyleClass().contains(styleClass));
    }
    
    /*********************************************************************
     * Following 2 methods are for MenuItem                                   *
     ********************************************************************/
    public static void assertStyleClassContains(MenuItem mi, String styleClass) {
        assertStyleClassContains(
                "The style class " + styleClass + " was not set on the MenuItem " + mi,
                mi, styleClass);
    }
    
    public static void assertStyleClassContains(String message, MenuItem mi, String styleClass) {
        assertTrue(message, mi.getStyleClass().contains(styleClass));
    }
    
    /*********************************************************************
     * Following 2 methods are for Popup controls like Tooltip etc       *
     ********************************************************************/
    public static void assertStyleClassContains(PopupControl control, String styleClass) {
        assertStyleClassContains(
                "The style class " + styleClass + " was not set on the Popupcontrol " + control,
                control, styleClass);
    }
    
    public static void assertStyleClassContains(String message, PopupControl control, String styleClass) {
        assertTrue(message, control.getStyleClass().contains(styleClass));
    }
    
    /****************************************************************************
     * Following 2 methods are for normal controls like Button, TextField etc   *
     ****************************************************************************/
    public static void assertStyleClassContains(Control control, String styleClass) {
        assertStyleClassContains(
                "The style class " + styleClass + " was not set on the control " + control,
                control, styleClass);
    }
    
    public static void assertStyleClassContains(String message, Control control, String styleClass) {
        assertTrue(message, control.getStyleClass().contains(styleClass));
    }
    
    /****************************************************************************
     * Following 4 methods are for normal controls like Button, TextField etc   *
     ****************************************************************************/
    public static void assertPseudoClassExists(Control control, String pseudoClass) {
        assertPseudoClassExists(
                "The pseudo class " + pseudoClass + " was not set on control " + control,
                control, pseudoClass);
    }
    
    public static void assertPseudoClassExists(String message, Control control, String pseudoClass) {
        Set<PseudoClass> allStates = control.getPseudoClassStates();
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        assertTrue(message, allStates.contains(state));
    }
    
    public static void assertPseudoClassDoesNotExist(Control control, String pseudoClass) {
        assertPseudoClassDoesNotExist(
                "The pseudo class " + pseudoClass + " was unexpectedly set on control " + control,
                control, pseudoClass);
    }

    public static void assertPseudoClassDoesNotExist(String message, Control control, String pseudoClass) {
        Set<PseudoClass> allStates = control.getPseudoClassStates();
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        assertFalse(message, allStates.contains(state));
    }    

    /****************************************************************************
     * Following 4 methods are for axis type like ValueAxis, NumberAxis, CategoryAxis etc*
     ****************************************************************************/
    public static void assertPseudoClassExists(Axis axis, String pseudoClass) {
        assertPseudoClassExists(
                "The pseudo class " + pseudoClass + " was not set on axis " + axis,
                axis, pseudoClass);
    }
    
    public static void assertPseudoClassExists(String message, Axis axis, String pseudoClass) {
        Set<PseudoClass> allStates = axis.getPseudoClassStates();
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        assertTrue(message, allStates.contains(state));
    }
    
    public static void assertPseudoClassDoesNotExist(Axis axis, String pseudoClass) {
        assertPseudoClassDoesNotExist(
                "The pseudo class " + pseudoClass + " was unexpectedly set on axis " + axis,
                axis, pseudoClass);
    }

    public static void assertPseudoClassDoesNotExist(String message, Axis axis, String pseudoClass) {
        Set<PseudoClass> allStates = axis.getPseudoClassStates();
        PseudoClass state = PseudoClass.getPseudoClass(pseudoClass);
        assertFalse(message, allStates.contains(state));
    }    
    
    /*********************************************************************
     * Following 2 methods are for the Pagination controls               *
     ********************************************************************/
    public static void assertStyleClassContains(Pagination control, String styleClass) {
        assertStyleClassContains(
                "The style class " + styleClass + " was not set on the Pagination " + control,
                control, styleClass);
    }
    
    public static void assertStyleClassContains(String message, Pagination control, String styleClass) {
        assertTrue(message, control.getStyleClass().contains(styleClass));
    }
    
    public static void assertListenerListContains(ObservableList list, ListChangeListener listener) {
        assertListenerListContains("The listener " + listener + " was not contained in " + list, list, listener);
    }

    public static void assertListenerListContains(String message, ObservableList list, ListChangeListener listener) {
//        ListenerList listeners = getListenerList(list);
//        assertTrue(message, listeners != null && listeners.contains(listener));
    }

    public static void assertListenerListDoesNotContain(ObservableList list, ListChangeListener listener) {
        assertListenerListDoesNotContain("The listener " + listener + " was contained in " + list, list, listener);
    }

    public static void assertListenerListDoesNotContain(String message, ObservableList list, ListChangeListener listener) {
//        ListenerList listeners = getListenerList(list);
//        assertTrue(message, listeners == null || !listeners.contains(listener));
    }

    public static ListChangeListener getListChangeListener(Object bean, String fieldName) {
        return (ListChangeListener) getListener(bean, fieldName);
    }

    public static int getListenerCount(ObservableList list) {
//        ListenerList listeners = getListenerList(list);
//        return listeners == null ? 0 : listeners.size();
        return 0;
    }

    public static void assertValueListenersContains(ObservableValue value, ChangeListener listener) {
        assertValueListenersContains("The listener " + listener + " was not contained in " + value, value, listener);
    }

    public static void assertValueListenersContains(String message, ObservableValue value, ChangeListener listener) {
        List listeners = getObservableValueListeners(value);
        assertTrue(message, listeners != null && listeners.contains(listener));
    }

    public static void assertValueListenersContains(ObservableValue value, InvalidationListener listener) {
        assertValueListenersContains("The listener " + listener + " was not contained in " + value, value, listener);
    }

    public static void assertValueListenersContains(String message, ObservableValue value, InvalidationListener listener) {
        List listeners = getObservableValueListeners(value);
        assertTrue(message, listeners != null && listeners.contains(listener));
    }

    public static void assertValueListenersDoesNotContain(ObservableValue value, ChangeListener listener) {
        assertValueListenersDoesNotContain("The listener " + listener + " was contained in " + value, value, listener);
    }

    public static void assertValueListenersDoesNotContain(String message, ObservableValue value, ChangeListener listener) {
        List listeners = getObservableValueListeners(value);
        assertTrue(message, listeners == null || !listeners.contains(listener));
    }

    public static void assertValueListenersDoesNotContain(ObservableValue value, InvalidationListener listener) {
        assertValueListenersDoesNotContain("The listener " + listener + " was contained in " + value, value, listener);
    }

    public static void assertValueListenersDoesNotContain(String message, ObservableValue value, InvalidationListener listener) {
        List listeners = getObservableValueListeners(value);
        assertTrue(message, listeners == null || !listeners.contains(listener));
    }

    public static int getListenerCount(ObservableValue value) {
        return getObservableValueListeners(value).size();
    }

    public static ChangeListener getChangeListener(Object bean, String fieldName) {
        return (ChangeListener) getListener(bean, fieldName);
    }

    public static InvalidationListener getInvalidationListener(Object bean, String fieldName) {
        return (InvalidationListener) getListener(bean, fieldName);
    }

    private static Object getListener(Object bean, String fieldName) {
        try {
            Class clazz = bean.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(bean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    private static ListenerList getListenerList(ObservableList list) {
//        try {
//            Class clazz = ObservableListWrapper.class;
//            Field field = clazz.getDeclaredField("observers");
//            field.setAccessible(true);
//            return (ListenerList) field.get(list);
//        } catch (Exception e) {
//            try {
//                Class clazz = ReadOnlyUnbackedObservableList.class;
//                Field field = clazz.getDeclaredField("observers");
//                field.setAccessible(true);
//                return (ListenerList) field.get(list);
//            } catch (Exception ee) {
//                e.printStackTrace();
//                assertTrue(false);
//            }
//        }
//        return null;
//    }

    private static List getObservableValueListeners(ObservableValue value) {
        // Try to look for the ExpressionHelper "helper" field
        // Depending on the type of the helper, I have to look for different fields. If
        // the helper is a Single* type, then I look for the field and if it is not null,
        // then the count is 1, otherwise the count is 0. If it is instead a Multiple*
        // type then I check size, or changeSize/invalidationSize fields.
        try {
            // TODO need to support more than just ObjectPropertyBase
            Field helperField = getExpressionHelperField(value);
            helperField.setAccessible(true);
            ExpressionHelper helper = (ExpressionHelper) helperField.get(value);
            if (helper == null) return Collections.emptyList();

            Class singleInvalidationClass = Class.forName("com.sun.javafx.binding.ExpressionHelper$SingleInvalidation");
            if (singleInvalidationClass.isAssignableFrom(helper.getClass())) {
                Field field = singleInvalidationClass.getDeclaredField("listener");
                field.setAccessible(true);
                Object listener = field.get(helper);
                return listener == null ? Collections.emptyList() : Arrays.asList(listener);
            }

            Class singleChangeClass = Class.forName("com.sun.javafx.binding.ExpressionHelper$SingleChange");
            if (singleChangeClass.isAssignableFrom(helper.getClass())) {
                Field field = singleChangeClass.getDeclaredField("listener");
                field.setAccessible(true);
                Object listener = field.get(helper);
                return listener == null ? Collections.emptyList() : Arrays.asList(listener);
            }

            Class genericClass = Class.forName("com.sun.javafx.binding.ExpressionHelper$Generic");
            if (genericClass.isAssignableFrom(helper.getClass())) {
                List results = new ArrayList();
                Field field = genericClass.getDeclaredField("invalidationListeners");
                field.setAccessible(true);
                InvalidationListener[] invalidationListeners = (InvalidationListener[])field.get(helper);
                if (invalidationListeners != null) {
                    results.addAll(Arrays.asList(invalidationListeners));
                }

                field = genericClass.getDeclaredField("changeListeners");
                field.setAccessible(true);
                ChangeListener[] changeListeners = (ChangeListener[])field.get(helper);
                if (changeListeners != null) {
                    results.addAll(Arrays.asList(changeListeners));
                }
                return results;
            }
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }

        return Collections.emptyList();
    }

    private static List getListenersFromExpressionHelper(ExpressionHelper helper, Class clazz, String sizeName, String listenersName) throws Exception {
        Field sizeField = clazz.getDeclaredField(sizeName);
        sizeField.setAccessible(true);
        Field listenersField = clazz.getDeclaredField(listenersName);
        listenersField.setAccessible(true);
        int size = (Integer)sizeField.get(helper);
        Object[] listeners = (Object[])listenersField.get(helper);
        Object[] results = new Object[size];
        System.arraycopy(listeners, 0, results, 0, size);
        return Arrays.asList(results);
    }

    private static Field getExpressionHelperField(ObservableValue value) throws Exception {
        Class clazz = value.getClass();
        while (clazz != Object.class) {
            try {
                return clazz.getDeclaredField("helper");
            } catch (NoSuchFieldException ex) { }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
