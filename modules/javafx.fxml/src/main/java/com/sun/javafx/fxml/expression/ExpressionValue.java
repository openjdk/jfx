/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.expression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import com.sun.javafx.fxml.BeanAdapter;

/**
 * Class representing an observable expression value.
 */
public class ExpressionValue extends ObservableValueBase<Object> {
    // Monitors a namespace for changes along a key path
    private class KeyPathMonitor {
        private String key;
        private KeyPathMonitor next;

        private Object namespace = null;

        private ListChangeListener<Object> listChangeListener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Object> change) {
                while (change.next()) {
                    int index = Integer.parseInt(key);

                    if (index >= change.getFrom() && index < change.getTo()) {
                        fireValueChangedEvent();
                        remonitor();
                    }
                }
            }
        };

        private MapChangeListener<String, Object> mapChangeListener = new MapChangeListener<>() {
            @Override
            public void onChanged(Change<? extends String, ? extends Object> change) {
                if (key.equals(change.getKey())) {
                    fireValueChangedEvent();
                    remonitor();
                }
            }
        };

        private ChangeListener<Object> propertyChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                fireValueChangedEvent();
                remonitor();
            }
        };

        public KeyPathMonitor(Iterator<String> keyPathIterator) {
            this.key = keyPathIterator.next();

            if (keyPathIterator.hasNext()) {
                next = new KeyPathMonitor(keyPathIterator);
            } else {
                next = null;
            }
        }

        @SuppressWarnings("unchecked")
        public void monitor(Object namespace) {
            if (namespace instanceof ObservableList<?>) {
                ((ObservableList<Object>)namespace).addListener(listChangeListener);
            } else if (namespace instanceof ObservableMap<?, ?>) {
                ((ObservableMap<String, Object>)namespace).addListener(mapChangeListener);
            } else {
                BeanAdapter namespaceAdapter = new BeanAdapter(namespace);
                ObservableValue<Object> propertyModel = namespaceAdapter.getPropertyModel(key);

                if (propertyModel != null) {
                    propertyModel.addListener(propertyChangeListener);
                }

                namespace = namespaceAdapter;
            }

            this.namespace = namespace;

            if (next != null) {
                Object value = Expression.get(namespace, key);
                if (value != null) {
                    next.monitor(value);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void unmonitor() {
            if (namespace instanceof ObservableList<?>) {
                ((ObservableList<Object>)namespace).removeListener(listChangeListener);
            } else if (namespace instanceof ObservableMap<?, ?>) {
                ((ObservableMap<String, Object>)namespace).removeListener(mapChangeListener);
            } else if (namespace != null) {
                BeanAdapter namespaceAdapter = (BeanAdapter)namespace;
                ObservableValue<Object> propertyModel = namespaceAdapter.getPropertyModel(key);

                if (propertyModel != null) {
                    propertyModel.removeListener(propertyChangeListener);
                }
            }

            namespace = null;

            if (next != null) {
                next.unmonitor();
            }
        }

        public void remonitor() {
            if (next != null) {
                next.unmonitor();
                Object value = Expression.get(namespace, key);
                if (value != null) {
                    next.monitor(value);
                }
            }
        }
    }

    private Object namespace;
    private Expression expression;
    private Class<?> type;

    private ArrayList<KeyPathMonitor> argumentMonitors;

    private int listenerCount = 0;

    public ExpressionValue(Object namespace, Expression expression, Class<?> type) {
        if (namespace == null) {
            throw new NullPointerException();
        }

        if (expression == null) {
            throw new NullPointerException();
        }

        if (type == null) {
            throw new NullPointerException();
        }

        this.namespace = namespace;
        this.expression = expression;
        this.type = type;

        List<KeyPath> arguments = expression.getArguments();
        argumentMonitors = new ArrayList<>(arguments.size());

        for (KeyPath argument : arguments) {
            argumentMonitors.add(new KeyPathMonitor(argument.iterator()));
        }
    }

    @Override
    public Object getValue() {
        return BeanAdapter.coerce(expression.evaluate(namespace), type);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        if (listenerCount == 0) {
            monitorArguments();
        }

        super.addListener(listener);
        listenerCount++;
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        super.removeListener(listener);
        listenerCount--;

        if (listenerCount == 0) {
            unmonitorArguments();
        }
    }

    @Override
    public void addListener(ChangeListener<? super Object> listener) {
        if (listenerCount == 0) {
            monitorArguments();
        }

        super.addListener(listener);
        listenerCount++;
    }

    @Override
    public void removeListener(ChangeListener<? super Object> listener) {
        super.removeListener(listener);
        listenerCount--;

        if (listenerCount == 0) {
            unmonitorArguments();
        }
    }

    private void monitorArguments() {
        for (KeyPathMonitor argumentMonitor : argumentMonitors) {
            argumentMonitor.monitor(namespace);
        }
    }

    private void unmonitorArguments() {
        for (KeyPathMonitor argumentMonitor : argumentMonitors) {
            argumentMonitor.unmonitor();
        }
    }
}
