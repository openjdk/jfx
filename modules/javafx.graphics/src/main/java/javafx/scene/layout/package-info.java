/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>
 * Provides classes to support user interface layout.
 * Each layout pane class supports a different layout strategy for its children
 * and applications may nest these layout panes to achieve the needed layout structure
 * in the user interface.  Once a node is added to one of the layout panes,
 * the pane will automatically manage the layout for the node, so the application
 * should not position or resize the node directly; see &quot;Node Resizability&quot;
 * for more details.
 * </p>
 *
 * <h2>Scene Graph Layout Mechanism</h2>
 * <p>
 * The scene graph layout mechanism is driven automatically by the system once
 * the application creates and displays a {@link javafx.scene.Scene Scene}.
 * The scene graph detects dynamic node changes which affect layout (such as a
 * change in size or content) and calls {@code requestLayout()}, which marks that
 * branch as needing layout so that on the next pulse, a top-down layout pass is
 * executed on that branch by invoking {@code layout()} on that branch's root.
 * During that layout pass, the {@code layoutChildren()} callback method will
 * be called on each parent to layout its children.  This mechanism is designed
 * to maximize layout efficiency by ensuring multiple layout requests are coalesced
 * and processed in a single pass rather than executing re-layout on on each minute
 * change. Therefore, applications should not invoke layout directly on nodes.
 * </p>
 *
 *
 * <h2>Node Resizability</h2>
 * <p>
 * The scene graph supports both resizable and non-resizable node classes.  The
 * {@code isResizable()} method on {@link javafx.scene.Node Node} returns whether a
 * given node is resizable or not.  {@literal A resizable node class is one which supports a range
 * of acceptable sizes (minimum <= preferred <= maximum), allowing its parent to resize
 * it within that range during layout, given the parent's own layout policy and the
 * layout needs of sibling nodes.}  Node supports the following methods for layout code
 * to determine a node's resizable range:
 * <pre><code>
 *     public Orientation getContentBias()
 *     public double minWidth(double height)
 *     public double minHeight(double width)
 *     public double prefWidth(double height)
 *     public double prefHeight(double width)
 *     public double maxWidth(double height)
 *     public double maxHeight(double width)
 * </code></pre>
 * <p>
 * Non-resizable node classes, on the other hand, do <em>not</em> have a consistent
 * resizing API and so are <em>not</em> resized by their parents during layout.
 * Applications must establish the size of non-resizable nodes by setting
 * appropriate properties on each instance. These classes return their current layout bounds for
 * min, pref, and max, and the {@code resize()} method becomes a no-op.</p>
 * <p>
 * <br>Resizable classes: {@link javafx.scene.layout.Region Region}, {@link javafx.scene.control.Control Control}, {@link javafx.scene.web.WebView WebView}
 * <br>Non-Resizable classes: {@link javafx.scene.Group Group}, {@link javafx.scene.shape.Shape Shape}, {@link javafx.scene.text.Text Text}
 * </p>
 * <p>
 * For example, a Button control (resizable) computes its min, pref, and max sizes
 * which its parent will use to resize it during layout, so the application only needs
 * to configure its content and properties:
 *
 * <pre><code>    Button button = new Button("Apply");
 * </code></pre>
 * However, a Circle (non-resizable) cannot be resized by its parent, so the application
 * needs to set appropriate geometric properties which determine its size:
 *
 * <pre><code>    Circle circle = new Circle();
 *     circle.setRadius(50);
 * </code></pre>
 *
 * <h2>Resizable Range</h2>
 *
 * Each resizable node class computes an appropriate min, pref, and max size based
 * on its own content and property settings (it's 'intrinsic' size range).
 * Some resizable classes have an unbounded max size (all layout panes) while
 * others have a max size that is clamped by default to their preferred size (buttons)
 * (See individual class documentation for the default range of each class).
 * While these defaults are geared towards common usage, applications often need
 * to explicitly alter or set a node's resizable range to achieve certain layouts.
 * The resizable classes provide properties for overriding the min, pref and max
 * sizes for this purpose.
 * <p>For example, to override the preferred size of a ListView:</p>
 * <pre><code>    listview.setPrefSize(200,300);
 * </code></pre>
 * <p>Or, to change the max width of a button so it will resize wider to fill a space:
 * <pre><code>    button.setMaxWidth(Double.MAX_VALUE);
 * </code></pre>
 * <p>For the inverse case, where the application needs to clamp the node's min
 * or max size to its preferred:
 * <pre><code>    listview.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
 * </code></pre>
 * And finally, if the application needs to restore the intrinsically computed values:
 * <pre><code>    listview.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
 * </code></pre>
 *
 * <h2>CSS Styling and Node Sizing</h2>
 *
 * Applications cannot reliably query the bounds of a resizable node until it has been
 * added to a scene because the size of that node may be dependent on CSS.  This is
 * because CSS is used to style many aspects of a node which affect it's preferred size
 * (font, padding, borders, etc) and so the node cannot be laid out (resized) until
 * CSS has been applied and the parent can access valid size range metrics.
 * This is always true for Controls (and any panes that contain them), because they
 * rely on CSS for their default style, even if no user-level style sheets have been set.
 * Stylesheets are set at the Scene level, which means that styles cannot even
 * be determined until a node's enclosing scene has been initialized. Once a Scene
 * is initialized, CSS is applied to nodes on each pulse (when needed) just before
 * the layout pass.
 *
 *
 *
 * <h2>Visual Bounds vs. Layout Bounds</h2>
 *
 * A graphically rich user interface often has the need to make a distinction between
 * a node's visual bounds and the bounds used for layout.  For example, the tight visual
 * bounds of a Text node's character glyphs would not work for layout, as the text
 * would not be aligned and leading/trailing whitespace would be discounted.  Also,
 * sometimes applications wish to apply affects and transforms to nodes without
 * disturbing the surrounding layout (bouncing, jiggling, drop shadows, glows, etc).
 * To support this distinction in the scene graph, {@link javafx.scene.Node Node}
 * provides the {@code layoutBounds} property to define the 'logical' bounds
 * of the node for layout and {@code boundsInParent} to define the visual bounds
 * once all effects, clipping, and transforms have been applied.
 *
 * <p>These two bounds properties will often differ for a given node and
 * {@code layoutBounds} is computed differently depending on the node class:
 *
 * <table border="1">
 *  <caption>Bounds Computation Table</caption>
 *  <thead>
 *      <tr>
 *          <th scope="col">Node Type</th>
 *          <th scope="col">Layout Bounds</th>
 *      </tr>
 *  </thead>
 *  <tbody>
 *      <tr>
 *          <th scope="row">{@link javafx.scene.shape.Shape Shape},{@link javafx.scene.image.ImageView ImageView}</th>
 *          <td>Includes geometric bounds (geometry plus stroke).
 *              Does NOT include effect, clip, or any transforms.
 *          </td>
 *      </tr>
 *      <tr>
 *          <th scope="row">{@link javafx.scene.text.Text Text}</th>
 *          <td>logical bounds based on the font height and content width, including white space.
 *              can be configured to be tight bounds around chars glyphs by setting {@code boundsType}.
 *              Does NOT include effect, clip, or any transforms.
 *          </td>
 *      </tr>
 *      <tr>
 *          <th scope="row">{@link javafx.scene.layout.Region Region}, {@link javafx.scene.control.Control Control}, {@link javafx.scene.web.WebView WebView}</th>
 *          <td>always {@code [0,0 width x height]} regardless of visual bounds,
 *              which might be larger or smaller than layout bounds.
 *          </td>
 *      </tr>
 *      <tr>
 *          <th scope="row">{@link javafx.scene.Group Group}</th>
 *          <td>Union of all visible children's visual bounds ({@code boundsInParent})
 *              Does NOT include effect, clip, or transforms set directly on group,
 *              however DOES include effect, clip, transforms set on individual children since
 *              those are included in the child's {@code boundsInParent}.
 *          </td>
 *      </tr>
 *  </tbody>
 * </table>
 * <p>
 * So for example, if a {@link javafx.scene.effect.DropShadow DropShadow} is added to a shape,
 * that shadow will <em>not</em>  be factored into layout by default.  Or, if a
 * {@link javafx.animation.ScaleTransition ScaleTransition} is used to
 * pulse the size of a button, that pulse animation will not disturb layout around
 * that button.  If an application wishes to have the effect, clip, or transform
 * factored into the layout of a node, it should wrap that node in a Group.
 * </p>
 */
package javafx.scene.layout;
