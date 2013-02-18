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

package com.sun.javafx.beans.event;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;

/**
 * A helper base class for notify listeners. It holds reference to its
 * {@code WeakListener} wrapper.
 * <p>
 * Without this class, an observer which uses weak listeners would need to hold
 * two references. The first to the original {@code ChangeListener} to prevent
 * it from being garbage collected. The second to the {@code WeakListener}
 * wrapper so it can unregister once registered listener. The second reference
 * is eliminated by using this class.
 * <p>
 * Example:
<PRE>
public class Observer {
    private AbstractNotifyListener listener = new AbstractNotifyListener() {
        public void handle(Bean bean, PropertyReference property) {
            // do something
        }
    };

    public void start() {
        subject.addChangeListener(property, listener.getWeakListener());
    }

    public void stop() {
        subject.removeChangeListener(property, listener.getWeakListener());
    }
}
</PRE>
 */
public abstract class AbstractNotifyListener implements InvalidationListener {
    private final WeakInvalidationListener weakListener = new WeakInvalidationListener(this);

    public InvalidationListener getWeakListener() {
        return weakListener;
    }
}
