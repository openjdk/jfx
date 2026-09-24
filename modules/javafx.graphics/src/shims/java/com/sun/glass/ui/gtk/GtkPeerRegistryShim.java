/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.gtk;

/**
 * A {@code GtkPeerRegistry} of arbitrary objects, for tests: the registry class of {@code GtkWindow} and
 * {@code GtkView} is pure Java and needs neither GTK nor a display.
 */
public final class GtkPeerRegistryShim {

    private final GtkPeerRegistry<Object> registry = new GtkPeerRegistry<>();

    public Object get(long id) {
        return registry.get(id);
    }

    public boolean containsKey(long id) {
        return registry.containsKey(id);
    }

    public int size() {
        return registry.size();
    }

    public void put(long id, Object peer) {
        registry.put(id, peer);
    }

    public void putWeak(long id, Object peer) {
        registry.putWeak(id, peer);
    }

    public Object remove(long id) {
        return registry.remove(id);
    }
}
