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

package javafx.scene.shape;

import com.sun.javafx.util.WeakReferenceQueue;
import com.sun.javafx.scene.shape.PathElementHelper;
import com.sun.javafx.sg.prism.NGPath;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.scene.Node;

import java.util.Iterator;


/**
 * The {@code PathElement} class represents an abstract element
 * of the {@link Path} that can represent any geometric objects
 * like straight lines, arcs, quadratic curves, cubic curves, etc.
 * @since JavaFX 2.0
 */
public abstract class PathElement {
    /*
     * Store the singleton instance of the PathElementHelper subclass corresponding
     * to the subclass of this instance of PathElement
     */
    private PathElementHelper pathElementHelper = null;

    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        PathElementHelper.setPathElementAccessor(new PathElementHelper.PathElementAccessor() {
            @Override
            public PathElementHelper getHelper(PathElement pathElement) {
                return pathElement.pathElementHelper;
            }

            @Override
            public void setHelper(PathElement pathElement, PathElementHelper pathElementHelper) {
                pathElement.pathElementHelper = pathElementHelper;
            }
        });
    }

    /**
     * Defines the sequence of {@code Path} objects this path element
     * is attached to.
     */
    WeakReferenceQueue nodes = new WeakReferenceQueue();

    /**
     * Constructor for subclasses to call.
     */
    public PathElement() {
    }

    void addNode(final Node n) {
        nodes.add(n);
    }

    void removeNode(final Node n) {
        nodes.remove(n);
    }

    void u() {
        final Iterator iterator = nodes.iterator();
        while (iterator.hasNext()) {
            ((Path) iterator.next()).markPathDirty();
        }
    }

    abstract void addTo(NGPath pgPath);

    /**
     * A flag that indicates whether the path coordinates are absolute or
     * relative. A value of true indicates that the coordinates are absolute
     * values. A value of false indicates that the values in this PathElement
     * are added to the coordinates of the previous PathElement to compute the
     * actual coordinates.
     *
     * @defaultValue true
     */
    private BooleanProperty absolute;


    public final void setAbsolute(boolean value) {
        absoluteProperty().set(value);
    }

    public final boolean isAbsolute() {
        return absolute == null || absolute.get();
    }

    public final BooleanProperty absoluteProperty() {
        if (absolute == null) {
            absolute = new BooleanPropertyBase(true) {
                @Override protected void invalidated() {
                    u();
                }

                @Override
                public Object getBean() {
                    return PathElement.this;
                }

                @Override
                public String getName() {
                    return "absolute";
                }
            };
        }
        return absolute;
    }
}

