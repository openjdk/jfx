/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

package javafx.beans.property;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

import com.sun.javafx.binding.OldValueCachingListenerManager;

/**
 * Base class for all readonly properties wrapping a {@code boolean}. This class provides a default
 * implementation to attach listener.
 *
 * @see ReadOnlyBooleanProperty
 * @since JavaFX 2.0
 */
public abstract class ReadOnlyBooleanPropertyBase extends ReadOnlyBooleanProperty {
    private static final OldValueCachingListenerManager<Boolean, ReadOnlyBooleanPropertyBase> LISTENER_MANAGER =
        new OldValueCachingListenerManager<>() {
            @Override
            protected Object getData(ReadOnlyBooleanPropertyBase instance) {
                return instance.listenerData;
            }

            @Override
            protected void setData(ReadOnlyBooleanPropertyBase instance, Object data) {
                instance.listenerData = data;
            }
        };

    Object listenerData;

    /**
     * Creates a default {@code ReadOnlyBooleanPropertyBase}.
     */
    public ReadOnlyBooleanPropertyBase() {
    }

    @Override
    public void addListener(InvalidationListener listener) {
        LISTENER_MANAGER.addListener(this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        LISTENER_MANAGER.removeListener(this, listener);
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        LISTENER_MANAGER.addListener(this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        LISTENER_MANAGER.removeListener(this, listener);
    }

    /**
     * Sends notifications to all attached
     * {@link javafx.beans.InvalidationListener InvalidationListeners} and
     * {@link javafx.beans.value.ChangeListener ChangeListeners}.
     *
     * This method needs to be called, if the value of this property changes.
     */
    protected void fireValueChangedEvent() {
        LISTENER_MANAGER.fireValueChanged(this, listenerData);
    }

}
