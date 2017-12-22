/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.scene.GroupHelper;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import java.util.Collection;



/**
 * A {@code Group} node contains an ObservableList of children that
 * are rendered in order whenever this node is rendered.
 * <p>
 * A {@code Group} will take on the collective bounds of its children and is
 * not directly resizable.
 * <p>
 * Any transform, effect, or state applied to a {@code Group} will be applied
 * to all children of that group.  Such transforms and effects will NOT be included
 * in this Group's layout bounds, however if transforms and effects are set
 * directly on children of this Group, those will be included in this Group's layout bounds.
 * <p>
 * By default, a {@code Group} will "auto-size" its managed resizable
 * children to their preferred sizes during the layout pass to ensure that Regions
 * and Controls are sized properly as their state changes.  If an application
 * needs to disable this auto-sizing behavior, then it should set
 * {@link #autoSizeChildrenProperty() autoSizeChildren} to {@code false} and understand that if the preferred size of
 * the children change, they will not automatically resize (so buyer beware!).
 *
 * <p>Group Example:</p>
 *
<PRE>
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import java.lang.Math;

Group g = new Group();
for (int i = 0; i &lt; 5; i++) {
    Rectangle r = new Rectangle();
    r.setY(i * 20);
    r.setWidth(100);
    r.setHeight(10);
    r.setFill(Color.RED);
    g.getChildren().add(r);
}
</PRE>
 * @since JavaFX 2.0
 */
@DefaultProperty("children")
public  class Group extends Parent {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        GroupHelper.setGroupAccessor(new GroupHelper.GroupAccessor() {
            @Override
            public Bounds doComputeLayoutBounds(Node node) {
                return ((Group) node).doComputeLayoutBounds();
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        GroupHelper.initHelper(this);
    }

    /**
     * Constructs a group.
     */
    public Group() { }

    /**
     * Constructs a group consisting of children.
     *
     * @param children children.
     */
    public Group(Node... children) {
        getChildren().addAll(children);
    }

    /**
     * Constructs a group consisting of the given children.
     *
     * @param children children of the group
     * @throws NullPointerException if the specified collection is null
     * @since JavaFX 8.0
     */
    public Group(Collection<Node> children) {
        getChildren().addAll(children);
    }

    /**
     * Controls whether or not this {@code Group} will automatically resize any
     * managed resizable children to their preferred sizes
     * during the layout pass. If set to {@code false}, then the application is
     * responsible for setting the size of this Group's resizable children, otherwise
     * such nodes may end up with a zero width/height and will not be visible.
     * This variable has no effect on content nodes which are not resizable (Shape, Text, etc).
     *
     * @defaultValue true
     */
    private BooleanProperty autoSizeChildren;


    public final void setAutoSizeChildren(boolean value){
        autoSizeChildrenProperty().set(value);
    }

    public final boolean isAutoSizeChildren() {
        return autoSizeChildren == null ? true : autoSizeChildren.get();
    }

    public final BooleanProperty autoSizeChildrenProperty() {
        if (autoSizeChildren == null) {
            autoSizeChildren = new BooleanPropertyBase(true) {

                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return Group.this;
                }

                @Override
                public String getName() {
                    return "autoSizeChildren";
                }
            };
        }
        return autoSizeChildren;
    }

    /**
     * Gets the list of children of this {@code Group}.
     * @return the list of children of this {@code Group}.
     */
    @Override public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private Bounds doComputeLayoutBounds() {
        layout(); // Needs to done prematurely, as we otherwise don't know the bounds of the children
        return null; // helper only requires this node to call layout().
    }

    /**
     * Group defines the preferred width as simply being the width of its layout bounds, which
     * in turn is simply the union of the layout bounds of all of its children. That is,
     * the preferred width is the one that it is at, because a Group cannot be resized.
     *
     * Note: as the layout bounds in autosize Group depend on the Group to be already laid-out,
     * this call will do the layout of the Group if necessary.
     *
     * @param height This parameter is ignored by Group
     * @return The layout bounds width
     */
    @Override
    public double prefWidth(double height) {
        if (isAutoSizeChildren()) {
            layout();
        }
        final double result = getLayoutBounds().getWidth();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Group defines the preferred height as simply being the height of its layout bounds, which
     * in turn is simply the union of the layout bounds of all of its children. That is,
     * the preferred height is the one that it is at, because a Group cannot be resized.
     *
     * Note: as the layout bounds in autosize Group depend on the Group to be already laid-out,
     * this call will do the layout of the Group if necessary.
     *
     * @param width This parameter is ignored by Group
     * @return The layout bounds height
     */
    @Override
    public double prefHeight(double width) {
        if (isAutoSizeChildren()) {
            layout();
        }
        final double result = getLayoutBounds().getHeight();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }


    @Override
    public double minHeight(double width) {
        return prefHeight(width);
    }

    @Override
    public double minWidth(double height) {
        return prefWidth(height);
    }

    /**
     * Group implements layoutChildren such that each child is resized to its preferred
     * size, if the child is resizable. Non-resizable children are simply left alone.
     * If {@link #autoSizeChildrenProperty() autoSizeChildren} is false, then Group does nothing in this method.
     */
    @Override protected void layoutChildren() {
        if (isAutoSizeChildren()) {
            super.layoutChildren();
        }
    }
}
