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
// Original code is re-licensed to Oracle by the author.
// https://github.com/andy-goryachev/FxTextEditor/blob/master/src/goryachev/fx/FxDisconnector.java
// Copyright © 2021-2022 Andy Goryachev <andy@goryachev.com>
package com.sun.javafx.scene.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
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
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.transform.Transform;
import javafx.stage.Window;

/**
 * This class provides convenience methods for adding various listeners, both
 * strong and weak, as well as a single {@link #disconnect()} method to remove
 * all listeners.
 * <p>
 * There are two usage patterns:
 * <ul>
 * <li>Client code registers a number of listeners and removes them all at once
 * via {@link #disconnect()} call.
 * <li>Client code registers a number of listeners and removes one via its
 * {@link IDisconnectable} instance.
 * </ul>
 *
 * This class is currently used for clean replacement of {@link Skin}s.
 * We should consider making this class a part of the public API in {@code javax.base},
 * since it proved itself useful in removing listeners and handlers in bulk at the application level.
 */
public class ListenerHelper implements IDisconnectable {
    private WeakReference<Object> ownerRef;
    private final ArrayList<IDisconnectable> items = new ArrayList<>(4);
    private static Function<SkinBase<?>,ListenerHelper> accessor;

    public ListenerHelper(Object owner) {
        ownerRef = new WeakReference<>(owner);
    }

    public ListenerHelper() {
    }

    public static void setAccessor(Function<SkinBase<?>,ListenerHelper> a) {
        accessor = a;
    }

    public static ListenerHelper get(SkinBase<?> skin) {
        return accessor.apply(skin);
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

    private boolean isAliveOrDisconnect() {
        if (ownerRef != null) {
            if (ownerRef.get() == null) {
                disconnect();
                return false;
            }
        }
        return true;
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
                if (isAliveOrDisconnect()) {
                    onChange.run();
                }
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

        ChLi<T> li = new ChLi<T>() {
            @Override
            public void disconnect() {
                prop.removeListener(this);
                items.remove(this);
            }

            @Override
            public void changed(ObservableValue<? extends T> src, T oldValue, T newValue) {
                if (isAliveOrDisconnect()) {
                    listener.changed(src, oldValue, newValue);
                }
            }
        };

        items.add(li);
        prop.addListener(li);

        if (fireImmediately) {
            T v = prop.getValue();
            listener.changed(prop, null, v);
        }

        return li;
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, Consumer<T> callback) {
        return addChangeListener(prop, false, callback);
    }

    public <T> IDisconnectable addChangeListener(ObservableValue<T> prop, boolean fireImmediately, Consumer<T> callback) {
        if (callback == null) {
            throw new NullPointerException("Callback must be specified.");
        }

        ChLi<T> li = new ChLi<T>() {
            @Override
            public void disconnect() {
                prop.removeListener(this);
                items.remove(this);
            }

            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                if (isAliveOrDisconnect()) {
                    callback.accept(newValue);
                }
            }
        };

        items.add(li);
        prop.addListener(li);

        if (fireImmediately) {
            T v = prop.getValue();
            callback.accept(v);
        }

        return li;
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
                if (isAliveOrDisconnect()) {
                    callback.run();
                }
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

        InLi li = new InLi() {
            @Override
            public void disconnect() {
                prop.removeListener(this);
                items.remove(this);
            }

            @Override
            public void invalidated(Observable observable) {
                if (isAliveOrDisconnect()) {
                    listener.invalidated(observable);
                }
            }
        };

        items.add(li);
        prop.addListener(li);

        if (fireImmediately) {
            listener.invalidated(prop);
        }

        return li;
    }

    // list change listeners

    public <T> IDisconnectable addListChangeListener(ObservableList<T> list, ListChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        LiChLi<T> li = new LiChLi<T>() {
            @Override
            public void disconnect() {
                list.removeListener(this);
                items.remove(this);
            }

            @Override
            public void onChanged(Change<? extends T> ch) {
                if (isAliveOrDisconnect()) {
                    listener.onChanged(ch);
                }
            }
        };

        items.add(li);
        list.addListener(li);

        return li;
    }

    // map change listener

    public <K,V> IDisconnectable addMapChangeListener(ObservableMap<K,V> list, MapChangeListener<K,V> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        MaChLi<K,V> li = new MaChLi<K,V>() {
            @Override
            public void disconnect() {
                list.removeListener(this);
                items.remove(this);
            }

            @Override
            public void onChanged(Change<? extends K, ? extends V> ch) {
                if (isAliveOrDisconnect()) {
                    listener.onChanged(ch);
                }
            }
        };

        items.add(li);
        list.addListener(li);

        return li;
    }

    // set change listeners

    public <T> IDisconnectable addSetChangeListener(ObservableSet<T> set, SetChangeListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener must be specified.");
        }

        SeChLi<T> li = new SeChLi<T>() {
            @Override
            public void disconnect() {
                set.removeListener(this);
                items.remove(this);
            }

            @Override
            public void onChanged(Change<? extends T> ch) {
                if (isAliveOrDisconnect()) {
                    listener.onChanged(ch);
                }
            }
        };

        items.add(li);
        set.addListener(li);

        return li;
    }

    // event handlers

    public <T extends Event> IDisconnectable addEventHandler(Object x, EventType<T> t, EventHandler<T> handler) {
        EvHa<T> h = new EvHa<>(handler) {
            @Override
            public void disconnect() {
                if (x instanceof Node n) {
                    n.removeEventHandler(t, this);
                } else if (x instanceof Window y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof Scene y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof MenuItem y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof TreeItem y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof TableColumnBase y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof Transform y) {
                    y.removeEventHandler(t, this);
                } else if (x instanceof Task y) {
                    y.removeEventHandler(t, this);
                }
            }
        };

        items.add(h);

        // we really need an interface here ... "HasEventHandlers"
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

        return h;
    }

    // event filters

    public <T extends Event> IDisconnectable addEventFilter(Object x, EventType<T> t, EventHandler<T> handler) {
        EvHa<T> h = new EvHa<>(handler) {
            @Override
            public void disconnect() {
                if (x instanceof Node n) {
                    n.removeEventFilter(t, this);
                } else if (x instanceof Window y) {
                    y.removeEventFilter(t, this);
                } else if (x instanceof Scene y) {
                    y.removeEventFilter(t, this);
                } else if (x instanceof Transform y) {
                    y.removeEventFilter(t, this);
                } else if (x instanceof Task y) {
                    y.removeEventFilter(t, this);
                }
            }
        };

        items.add(h);

        // we really need an interface here ... "HasEventFilters"
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

        return h;
    }

    //

    private static abstract class ChLi<T> implements IDisconnectable, ChangeListener<T> { }

    private static abstract class InLi implements IDisconnectable, InvalidationListener { }

    private static abstract class LiChLi<T> implements IDisconnectable, ListChangeListener<T> { }

    private static abstract class MaChLi<K,V> implements IDisconnectable, MapChangeListener<K,V> { }

    private static abstract class SeChLi<T> implements IDisconnectable, SetChangeListener<T> { }

    private abstract class EvHa<T extends Event> implements IDisconnectable, EventHandler<T> {
        private final EventHandler<T> handler;

        public EvHa(EventHandler<T> h) {
            this.handler = h;
        }

        @Override
        public void handle(T ev) {
            if (isAliveOrDisconnect()) {
                handler.handle(ev);
            }
        }
    }
}
