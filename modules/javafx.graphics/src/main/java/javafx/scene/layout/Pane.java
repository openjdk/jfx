/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import com.sun.javafx.scene.layout.PaneHelper;
import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * Base class for layout panes which need to expose the children list as public
 * so that users of the subclass can freely add/remove children.
 * <p>
 * This class may be used directly in cases where absolute positioning of children
 * is required since it does not perform layout beyond resizing resizable children
 * to their preferred sizes. It is the application's responsibility to position the
 * children since the pane leaves the positions alone during layout.
 * For example:
 * <pre>{@code
 *     Pane canvas = new Pane();
 *     canvas.setStyle("-fx-background-color: black;");
 *     canvas.setPrefSize(200,200);
 *     Circle circle = new Circle(50,Color.BLUE);
 *     circle.relocate(20, 20);
 *     Rectangle rectangle = new Rectangle(100,100,Color.RED);
 *     rectangle.relocate(70,70);
 *     canvas.getChildren().addAll(circle,rectangle);
 * }</pre>
 * <p>
 * Note: if an application needs children to be kept aligned within a parent (centered,
 * positioned at top-left, etc), it should use a {@link javafx.scene.layout.StackPane StackPane}
 * instead.</p>
 *
 * <p>
 * Pane resizes each managed child regardless of the child's visible property value;
 * unmanaged children are ignored for all layout calculations.</p>
 *
 * <h3>Resizable Range</h3>
 *
 * <p>
 * A pane's parent will resize the pane within the pane's resizable range
 * during layout.   By default the pane computes this range based on its content
 * as outlined in the table below:
 * </p>
 *
 * <table border="1">
 * <caption>Pane Resize Table</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left plus right insets.</td>
 * <td>top plus bottom insets.</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>width required to encompass each child at its current x location and preferred width.</td>
 * <td>height required to encompass each child at its current y location and preferred height.</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A pane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * Pane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>pane.setPrefSize(500,400);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to Region.USE_COMPUTED_SIZE.
 * <p>
 * Pane does not clip its content by default, so it is possible that children's
 * bounds may extend outside its own bounds, either if children are positioned
 * at negative coordinates or the pane is resized smaller than its preferred size.</p>
 *
 * @since JavaFX 2.0
 */
@DefaultProperty("children")
public class Pane extends Region {
    static {
        PaneHelper.setPaneAccessor(new PaneHelper.PaneAccessor() {
        });
    }

    static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            node.getProperties().remove(key);
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }

    static Object getConstraint(Node node, Object key) {
        if (node.hasProperties()) {
            Object value = node.getProperties().get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        PaneHelper.initHelper(this);
    }
    /**
     * Creates a Pane layout.
     */
    public Pane() {
        super();
    }

    /**
     * Creates a Pane layout.
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public Pane(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     *
     * @return modifiable list of children.
     */
    @Override public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

}
