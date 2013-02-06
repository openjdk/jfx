/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;


public class ExpressionHelperUtility {
    
    private static final String EXPRESSION_HELPER_SINGLE_INVALIDATION      = "com.sun.javafx.binding.ExpressionHelper$SingleInvalidation";
    private static final String EXPRESSION_HELPER_SINGLE_CHANGE            = "com.sun.javafx.binding.ExpressionHelper$SingleChange";
    private static final String EXPRESSION_HELPER_GENERIC                  = "com.sun.javafx.binding.ExpressionHelper$Generic";
    private static final String LIST_EXPRESSION_HELPER_SINGLE_INVALIDATION = "com.sun.javafx.binding.ListExpressionHelper$SingleInvalidation";
    private static final String LIST_EXPRESSION_HELPER_SINGLE_CHANGE       = "com.sun.javafx.binding.ListExpressionHelper$SingleChange";
    private static final String LIST_EXPRESSION_HELPER_SINGLE_LIST_CHANGE  = "com.sun.javafx.binding.ListExpressionHelper$SingleListChange";
    private static final String LIST_EXPRESSION_HELPER_GENERIC             = "com.sun.javafx.binding.ListExpressionHelper$Generic";
    private static final String MAP_EXPRESSION_HELPER_SINGLE_INVALIDATION  = "com.sun.javafx.binding.MapExpressionHelper$SingleInvalidation";
    private static final String MAP_EXPRESSION_HELPER_SINGLE_CHANGE        = "com.sun.javafx.binding.MapExpressionHelper$SingleChange";
    private static final String MAP_EXPRESSION_HELPER_SINGLE_MAP_CHANGE    = "com.sun.javafx.binding.MapExpressionHelper$SingleMapChange";
    private static final String MAP_EXPRESSION_HELPER_GENERIC              = "com.sun.javafx.binding.MapExpressionHelper$Generic";
    private static final String SET_EXPRESSION_HELPER_SINGLE_INVALIDATION  = "com.sun.javafx.binding.SetExpressionHelper$SingleInvalidation";
    private static final String SET_EXPRESSION_HELPER_SINGLE_CHANGE        = "com.sun.javafx.binding.SetExpressionHelper$SingleChange";
    private static final String SET_EXPRESSION_HELPER_SINGLE_SET_CHANGE    = "com.sun.javafx.binding.SetExpressionHelper$SingleSetChange";
    private static final String SET_EXPRESSION_HELPER_GENERIC              = "com.sun.javafx.binding.SetExpressionHelper$Generic";
    
    private ExpressionHelperUtility() {}
    
    
    public static List<InvalidationListener> getInvalidationListeners(Observable observable) {
        final Object helper = getExpressionHelper(observable);
        if (helper == null) {
            return Collections.emptyList();
        }
        final Class helperClass = helper.getClass();

        try {
            final Class clazz = Class.forName(EXPRESSION_HELPER_SINGLE_INVALIDATION);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromSingleInvalidationClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_SINGLE_INVALIDATION);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromSingleInvalidationClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_SINGLE_INVALIDATION);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromSingleInvalidationClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_SINGLE_INVALIDATION);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromSingleInvalidationClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }

        
        
        try {
            final Class clazz = Class.forName(EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return getInvalidationListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        return Collections.emptyList();
    }
    
    public static <T> List<ChangeListener<? super T>> getChangeListeners(ObservableValue<T> observable) {
        final Object helper = getExpressionHelper(observable);
        if (helper == null) {
            return Collections.emptyList();
        }
        final Class helperClass = helper.getClass();

        try {
            final Class clazz = Class.forName(EXPRESSION_HELPER_SINGLE_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromSingleChangeClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_SINGLE_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromSingleChangeClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_SINGLE_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromSingleChangeClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_SINGLE_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromSingleChangeClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }

        
        
        try {
            final Class clazz = Class.forName(EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                return (List<ChangeListener<? super T>>)(Object)getChangeListenerFromGenericClass(clazz, helper);
            }
        } catch (ClassNotFoundException ex) { }
        
        return Collections.emptyList();
    }
    
    public static <E> List<ListChangeListener<? super E>> getListChangeListeners(ObservableList<E> observable) {
        final Object helper = getExpressionHelper(observable);
        if (helper == null) {
            return Collections.emptyList();
        }
        final Class helperClass = helper.getClass();

        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_SINGLE_LIST_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("listener");
                    field.setAccessible(true);
                    final ListChangeListener<? super E> listener = (ListChangeListener)field.get(helper);
                    return Arrays.<ListChangeListener<? super E>>asList(listener);
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(LIST_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("listChangeListeners");
                    field.setAccessible(true);
                    final ListChangeListener<? super E>[] listeners = (ListChangeListener[])field.get(helper);
                    if (listeners != null) {
                        final Field sizeField = clazz.getDeclaredField("listChangeSize");
                        sizeField.setAccessible(true);
                        final int size = sizeField.getInt(helper);
                        return Arrays.asList(Arrays.copyOf(listeners, size));
                    }
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        return Collections.emptyList();
    }
    
    public static <K, V> List<MapChangeListener<? super K, ? super V>> getMapChangeListeners(ObservableMap<K, V> observable) {
        final Object helper = getExpressionHelper(observable);
        if (helper == null) {
            return Collections.emptyList();
        }
        final Class helperClass = helper.getClass();

        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_SINGLE_MAP_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("listener");
                    field.setAccessible(true);
                    final MapChangeListener<? super K, ? super V> listener = (MapChangeListener)field.get(helper);
                    return Arrays.<MapChangeListener<? super K, ? super V>>asList(listener);
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(MAP_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("mapChangeListeners");
                    field.setAccessible(true);
                    final MapChangeListener<? super K, ? super V>[] listeners = (MapChangeListener[])field.get(helper);
                    if (listeners != null) {
                        final Field sizeField = clazz.getDeclaredField("mapChangeSize");
                        sizeField.setAccessible(true);
                        final int size = sizeField.getInt(helper);
                        return Arrays.asList(Arrays.copyOf(listeners, size));
                    }
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        return Collections.emptyList();
    }
    
    public static <E> List<SetChangeListener<? super E>> getSetChangeListeners(ObservableSet<E> observable) {
        final Object helper = getExpressionHelper(observable);
        if (helper == null) {
            return Collections.emptyList();
        }
        final Class helperClass = helper.getClass();

        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_SINGLE_SET_CHANGE);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("listener");
                    field.setAccessible(true);
                    final SetChangeListener<? super E> listener = (SetChangeListener)field.get(helper);
                    return Arrays.<SetChangeListener<? super E>>asList(listener);
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        try {
            final Class clazz = Class.forName(SET_EXPRESSION_HELPER_GENERIC);
            if (clazz.isAssignableFrom(helperClass)) {
                try {
                    final Field field = clazz.getDeclaredField("setChangeListeners");
                    field.setAccessible(true);
                    final SetChangeListener<? super E>[] listeners = (SetChangeListener[])field.get(helper);
                    if (listeners != null) {
                        final Field sizeField = clazz.getDeclaredField("setChangeSize");
                        sizeField.setAccessible(true);
                        final int size = sizeField.getInt(helper);
                        return Arrays.asList(Arrays.copyOf(listeners, size));
                    }
                } catch (Exception ex) { }
            }
        } catch (ClassNotFoundException ex) { }
        
        return Collections.emptyList();
    }
    
    private static Object getExpressionHelper(Object bean) {
        Class clazz = bean.getClass();
        while (clazz != Object.class) {
            try {
                final Field field = clazz.getDeclaredField("helper");
                field.setAccessible(true);
                return field.get(bean);
            } catch (Exception ex) { }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static List<InvalidationListener> getInvalidationListenerFromSingleInvalidationClass(Class clazz, Object helper) {
        try {
            final Field field = clazz.getDeclaredField("listener");
            field.setAccessible(true);
            final InvalidationListener listener = (InvalidationListener)field.get(helper);
            return Arrays.asList(listener);
        } catch (Exception ex) { }
        return Collections.emptyList();
    }
    
    private static List<InvalidationListener> getInvalidationListenerFromGenericClass(Class clazz, Object helper) {
        try {
            final Field field = clazz.getDeclaredField("invalidationListeners");
            field.setAccessible(true);
            final InvalidationListener[] listeners = (InvalidationListener[])field.get(helper);
            if (listeners != null) {
                final Field sizeField = clazz.getDeclaredField("invalidationSize");
                sizeField.setAccessible(true);
                final int size = sizeField.getInt(helper);
                return Arrays.asList(Arrays.copyOf(listeners, size));
            }
        } catch (Exception ex) { }
        return Collections.emptyList();
    }

    private static List<ChangeListener> getChangeListenerFromSingleChangeClass(Class clazz, Object helper) {
        try {
            final Field field = clazz.getDeclaredField("listener");
            field.setAccessible(true);
            final ChangeListener listener = (ChangeListener)field.get(helper);
            return Arrays.asList(listener);
        } catch (Exception ex) { }
        return Collections.emptyList();
    }

    private static List<ChangeListener> getChangeListenerFromGenericClass(Class clazz, Object helper) {
        try {
            final Field field = clazz.getDeclaredField("changeListeners");
            field.setAccessible(true);
            final ChangeListener[] listeners = (ChangeListener[])field.get(helper);
            if (listeners != null) {
                final Field sizeField = clazz.getDeclaredField("changeSize");
                sizeField.setAccessible(true);
                final int size = sizeField.getInt(helper);
                return Arrays.asList(Arrays.copyOf(listeners, size));
            }
        } catch (Exception ex) { }
        return Collections.emptyList();
    }
}
