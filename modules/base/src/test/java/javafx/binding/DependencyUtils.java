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

package javafx.binding;

import java.util.ListIterator;

import javafx.beans.binding.Binding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static org.junit.Assert.*;

public class DependencyUtils {

    public static void checkDependencies(ObservableList<?> seq, Object... deps) {
        // we want to check the source dependencies, therefore we have to
        // remove all intermediate bindings
        final ObservableList<Object> copy = FXCollections.observableArrayList(seq);
        final ListIterator<Object> it = copy.listIterator();
        while (it.hasNext()) {
            final Object obj = it.next();
            if (obj instanceof Binding) {
                it.remove();
                final Binding binding = (Binding)obj;
                for (final Object newDep : binding.getDependencies()) {
                    it.add(newDep);
                }
            }
        }
        for (final Object obj : deps) {
            assertTrue(copy.contains(obj));
        }
    }

}
