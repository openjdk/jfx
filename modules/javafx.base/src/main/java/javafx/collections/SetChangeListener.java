/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Interface that receives notifications of changes to an ObservableSet.
 * @param <E> the element type
 * @since JavaFX 2.1
 */
@FunctionalInterface
public interface SetChangeListener<E> {

    /**
     * An elementary change done to an ObservableSet.
     * Change contains information about an add or remove operation.
     * Note that adding element that is already in the set does not
     * modify the set and hence no change will be generated.
     *
     * @param <E> element type
     * @since JavaFX 2.1
     */
    public static abstract class Change<E> {

        private ObservableSet<E> set;

        /**
         * Constructs a change associated with a set.
         * @param set the source of the change
         */
        public Change(ObservableSet<E> set) {
            this.set = set;
        }

        /**
         * An observable set that is associated with the change.
         * @return the source set
         */
        public ObservableSet<E> getSet() {
            return set;
        }

        /**
         * If this change is a result of add operation.
         * @return true if a new element was added to the set
         */
        public abstract boolean wasAdded();

        /**
         * If this change is a result of removal operation.
         * @return true if an old element was removed from the set
         */
        public abstract boolean wasRemoved();

        /**
         * Get the new element. Return null if this is a removal.
         * @return the element that was just added
         */
        public abstract E getElementAdded();

        /**
         * Get the old element. Return null if this is an addition.
         * @return the element that was just removed
         */
        public abstract E getElementRemoved();

    }

    /**
     * Called after a change has been made to an ObservableSet.
     * This method is called on every elementary change (add/remove) once.
     * This means, complex changes like removeAll(Collection) or clear()
     * may result in more than one call of onChanged method.
     *
     * @param change the change that was made
     */
    void onChanged(Change<? extends E> change);
}
