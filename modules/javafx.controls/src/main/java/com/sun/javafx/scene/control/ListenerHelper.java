/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.transform.Transform;
import javafx.stage.Window;

/**
 * This class provides convenience methods for adding various listeners, both strong and weak,
 * as well as a single {@link #disconnect()} method to remove all listeners.
 * <p>
 * There are two usage patterns:
 * <ul>
 * <li>Client code registers a number of listeners and removes them all at once via {@link #disconnect()} call.
 * <li>Client code registers a number of listeners and removes one via its {@link IDisconnectable} instance.
 * </ul>
 * <p>
 * Original code is re-licensed to Oracle by the author.
 * https://github.com/andy-goryachev/FxTextEditor/blob/master/src/goryachev/fx/FxDisconnector.java
 * Copyright © 2021-2022 Andy Goryachev <andy@goryachev.com>
 */
public class ListenerHelper implements IDisconnectable {
    private final ArrayList<IDisconnectable> items = new ArrayList<>(4);
    private static final Object KEY = new Object();

    public ListenerHelper() {
    }

    public static ListenerHelper get(Node n) {
        Object x = n.getProperties().get(KEY);
        if (x instanceof ListenerHelper h) {
            return h;
        }
        ListenerHelper d = new ListenerHelper();
        n.getProperties().put(KEY, d);
        return d;
    }

    public static void disconnect(Node n) {
        Object x = n.getProperties().remove(KEY);
        if (x instanceof ListenerHelper h) {
            h.disconnect();
        }
    }

    public IDisconnectable addDisconnectable(Runnable r) {
        IDisconnectable d = new IDisconnectable() {
            @Override
            public void disconnect() {
                items.remove(this);
                r.run();
            }
        };
        items.add(d);
        return d;
    }

    @Override
    public void disconnect() {
        for (int i = items.size() - 1; i >= 0; i--) {
            IDisconnectable d = items.remove(i);
            d.disconnect();
        }
    }

    // change listeners

    public IDisconnectable addChangeListener(Runnable callback, ObservableValue<?>... props) {
        return addChangeListener(callback, false, props);
    }

    public IDisconnectable addChangeListener(Runnable onChange, boolean fireImmediately, ObservableValue<?>... props) {
        if (onChange == null) {
            throw new NullPointerException("onChange must not be null.");
        }

        ChLi li = new ChLi() {
            @Override
            public void disconnect() {
                for (ObservableValue p : props) {
                    p.removeListener(this);
                }
                items.remove(this);
            }

            @Override
            public void changed(ObservableValue p, Object oldValue, Object newValue) {
                onChange.run();
            }
        };

        items.add(li);

        for (ObservableValue p : props) {
            p.addListener(li);
        }

        if (fireImmediately) {
            onChange.run();
        }

        return li;
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, ChangeListener<T> listener) {
        return addChangeListener(prop, false, listener);
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, boolean fireImmediately, ChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        IDisconnectable d = new IDisconnectable() {
            @Override
            public void disconnect() {
                prop.removeListener(listener);
                items.remove(this);
            }
        };

        items.add(d);
        prop.addListener(listener);

        if (fireImmediately) {
            T v = prop.getValue();
            listener.changed(prop, null, v);
        }

        return d;
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, Consumer<T> callback) {
        return addChangeListener(prop, false, callback);
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, boolean fireImmediately, Consumer<T> callback) {
        if (callback == null) {
            throw new NullPointerException("Callback must be specified.");
        }

        ChLi<T> d = new ChLi<T>() {
            @Override
            public void disconnect() {
                prop.removeListener(this);
                items.remove(this);
            }

            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                callback.accept(newValue);
            }
        };

        items.add(d);
        prop.addListener(d);

        if (fireImmediately) {
            T v = prop.getValue();
            callback.accept(v);
        }

        return d;
    }

    // invalidation listeners

    public IDisconnectable addInvalidationListener(Runnable callback, ObservableValue<?>... props) {
        return addInvalidationListener(callback, false, props);
    }

    public IDisconnectable addInvalidationListener(Runnable callback, boolean fireImmediately, ObservableValue<?>... props) {
        if (callback == null) {
            throw new NullPointerException("Callback must be specified.");
        }

        InLi li = new InLi() {
            @Override
            public void disconnect() {
                for (ObservableValue p : props) {
                    p.removeListener(this);
                }
                items.remove(this);
            }

            @Override
            public void invalidated(Observable p) {
                callback.run();
            }
        };

        items.add(li);

        for (ObservableValue p : props) {
            p.addListener(li);
        }

        if (fireImmediately) {
            callback.run();
        }

        return li;
    }

    public <T> IDisconnectable addInvalidationListener(ObservableValue<T> prop, InvalidationListener listener) {
        return addInvalidationListener(prop, false, listener);
    }

    public <T> IDisconnectable addInvalidationListener(ObservableValue<T> prop, boolean fireImmediately, InvalidationListener listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        IDisconnectable d = new IDisconnectable() {
            @Override
            public void disconnect() {
                prop.removeListener(listener);
                items.remove(this);
            }
        };

        items.add(d);
        prop.addListener(listener);

        if (fireImmediately) {
            listener.invalidated(prop);
        }

        return d;
    }

    // list change listeners

    public <T> IDisconnectable addListChangeListener(ObservableList<T> list, ListChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        IDisconnectable d = new IDisconnectable() {
            @Override
            public void disconnect() {
                list.removeListener(listener);
                items.remove(this);
            }
        };

        items.add(d);
        list.addListener(listener);

        return d;
    }

    // event handlers

    public <T extends Event> IDisconnectable addEventHandler(Object x, EventType<T> t, EventHandler<T> h) {

        // we really need an interface here ... "HasEventHandlers"
        IDisconnectable d = addDisconnectable(() -> {
            if (x instanceof Node n) {
                n.removeEventHandler(t, h);
            } else if (x instanceof Window y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof Scene y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof MenuItem y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof TreeItem y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof TableColumnBase y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof Transform y) {
                y.removeEventHandler(t, h);
            } else if (x instanceof Task y) {
                y.removeEventHandler(t, h);
            }
        });

        if (x instanceof Node y) {
            y.addEventHandler(t, h);
        } else if (x instanceof Window y) {
            y.addEventHandler(t, h);
        } else if (x instanceof Scene y) {
            y.addEventHandler(t, h);
        } else if (x instanceof MenuItem y) {
            y.addEventHandler(t, h);
        } else if (x instanceof TreeItem y) {
            y.addEventHandler(t, h);
        } else if (x instanceof TableColumnBase y) {
            y.addEventHandler(t, h);
        } else if (x instanceof Transform y) {
            y.addEventHandler(t, h);
        } else if (x instanceof Task y) {
            y.addEventHandler(t, h);
        } else {
            throw new IllegalArgumentException("Cannot add event handler to " + x);
        }

        return d;
    }

    // event filters

    public <T extends Event> IDisconnectable addEventFilter(Object x, EventType<T> t, EventHandler<T> h) {
        // we really need an interface here ... "HasEventFilters"
        IDisconnectable d = addDisconnectable(() -> {
            if (x instanceof Node n) {
                n.removeEventFilter(t, h);
            } else if (x instanceof Window y) {
                y.removeEventFilter(t, h);
            } else if (x instanceof Scene y) {
                y.removeEventFilter(t, h);
            } else if (x instanceof Transform y) {
                y.removeEventFilter(t, h);
            } else if (x instanceof Task y) {
                y.removeEventFilter(t, h);
            }
        });

        if (x instanceof Node y) {
            y.addEventFilter(t, h);
        } else if (x instanceof Window y) {
            y.addEventFilter(t, h);
        } else if (x instanceof Scene y) {
            y.addEventFilter(t, h);
        } else if (x instanceof Transform y) {
            y.addEventFilter(t, h);
        } else if (x instanceof Task y) {
            y.addEventFilter(t, h);
        } else {
            throw new IllegalArgumentException("Cannot add event filter to " + x);
        }

        return d;
    }

    //

    protected static abstract class ChLi<T> implements IDisconnectable, ChangeListener<T> { }

    protected static abstract class InLi implements IDisconnectable, InvalidationListener { }

    protected static abstract class LiChLi<T> implements IDisconnectable, ListChangeListener<T> { }
}