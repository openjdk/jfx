/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor
package javafx.scene.control.rich.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Unsynchronized list of WeakReferences.
 */
public class WeakList<T> {
    private ArrayList<WeakReference<T>> list;
    
    public WeakList() {
        this(8);
    }
    
    public WeakList(int capacity) {
        list = new ArrayList<>(capacity);
    }
    
    public int size() {
        return list.size();
    }
    
    public void add(T item) {
        list.add(new WeakReference<>(item));
    }
    
    public T get(int ix) {
        return list.get(ix).get();
    }
    
    public void remove(int ix) {
        list.remove(ix);
    }
    
    public void gc() {
        int sz = list.size();
        for (int i = sz - 1; i >= 0; --i) {
            WeakReference<T> ref = list.get(i);
            T item = ref.get();
            if(item == null) {
                list.remove(i);
            }
        }
    }
}
