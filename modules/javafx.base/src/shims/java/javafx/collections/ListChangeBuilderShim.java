/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package javafx.collections;

import java.util.List;

public class ListChangeBuilderShim<E> {

    private ListChangeBuilder lcb;

    public ListChangeBuilderShim(ObservableListBase<E> list) {
        lcb = new ListChangeBuilder<>(list);
    }

    public ListChangeBuilder<E> getBuilder() {
        return lcb;
    }

    public void beginChange() {
        lcb.beginChange();
    }

    public void endChange() {
        lcb.endChange();
    }

    public void nextRemove(int idx, List<? extends E> removed) {
        lcb.nextRemove(idx, removed);
    }

    public void nextRemove(int idx, E removed) {
        lcb.nextRemove(idx, removed);
    }

    public void nextAdd(int from, int to) {
        lcb.nextAdd(from, to);
    }

    public void nextPermutation(int from, int to, int[] perm) {
        lcb.nextPermutation(from, to, perm);
    }

    public void nextReplace(int from, int to, List removed) {
        lcb.nextReplace(from, to, removed);
    }

    public final void nextUpdate(int pos) {
        lcb.nextUpdate(pos);
    }

    public final void nextSet(int idx, E old) {
        lcb.nextSet(idx, old);
    }
}
