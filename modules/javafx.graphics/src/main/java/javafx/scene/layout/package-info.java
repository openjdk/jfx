/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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
 *
 * <h2>Layout Orientation</h2>
 *
 * The layout orientation for a branch of the scene graph is controlled by the
 * {@link javafx.scene.Scene#nodeOrientationProperty() Scene.nodeOrientation} and
 * {@link javafx.scene.Node#nodeOrientationProperty() Node.nodeOrientation} properties.
 * A value set on a {@code Scene} applies to its root, and a value set on any {@code Node} applies to that
 * node and its descendants. It is typically left-to-right by default, but can be right-to-left depending
 * on the locale of the operating system.
 * <p>
 * For layout containers, the effective orientation determines how children are ordered and how horizontal
 * positions are interpreted. In a left-to-right orientation, the first child usually appears at the left edge
 * and subsequent children flow to the right. Similarly, in a right-to-left orientation, the first child
 * usually appears at the right edge and subsequent children flow to the left. When a container exposes
 * named regions, the {@code left} region refers to the leading edge of the container, and the {@code right}
 * region refers to the trailing edge of the container. In right-to-left mode, the {@code left} region will
 * therefore be laid out on the right side, and the {@code right} region will be laid out on the left side.
 * <p>
 * Authors typically do not need to account for layout orientation when laying out nodes. In right-to-left
 * mode, a mirroring transform is automatically applied to nodes, flipping the visual flow in the horizontal
 * direction. If this behavior is not desired, nodes can override {@link javafx.scene.Node#usesMirroring()}
 * and return {@code false}.
 *
 * <h2 id="pixel-snapping">Pixel Snapping</h2>
 *
 * In JavaFX, layout coordinates use {@code double} values, which means that a position or size can be fractional.
 * Mapping fractional coordinates onto a physical screen can introduce blurriness if the edge of a node falls
 * somewhere between physical pixels. JavaFX uses <em>snapping</em> to fix this problem; it ensures that the bounds
 * of scene graph nodes do not fall between physical pixels on the screen.
 * <p>
 * This section provides guidance for authors of custom {@link javafx.scene.layout.Region} or
 * {@link javafx.scene.control.SkinBase} implementations that override measurement and layout methods such as
 * {@link javafx.scene.Parent#layoutChildren()}, {@link javafx.scene.layout.Region#computeMinWidth(double)},
 * {@link javafx.scene.layout.Region#computeMinHeight(double)}, {@link javafx.scene.layout.Region#computePrefWidth(double)},
 * {@link javafx.scene.layout.Region#computePrefHeight(double)}, as well as the corresponding {@code SkinBase} methods.
 *
 * <h3>Logical vs. pixel coordinates</h3>
 *
 * Layout is expressed in logical coordinates, and the {@linkplain javafx.stage.Window#getRenderScaleX() horizontal}
 * and {@linkplain javafx.stage.Window#getRenderScaleY() vertical} render scales of a window convert logical units
 * to pixels in the window's rendering buffer:
 *
 * <pre>{@code
 *     physical pixels = logical units * render scale
 * }</pre>
 *
 * At a render scale of {@code 1.0}, one logical unit is one pixel. At a render scale of {@code 1.5}, one pixel is
 * {@code 1 / 1.5} ≈ {@code 0.6667} logical units. A correctly snapped value is therefore not necessarily an integer,
 * and simply rounding coordinates to integers is wrong. Conceptually, snapping applies a rounding operation in pixels
 * and converts the result back to logical units:
 *
 * <pre>{@code
 *     snappedValue = round(logicalValue * renderScale) / renderScale
 * }</pre>
 *
 * The render scales in the two axes can be different, and they can change when a window moves between screens.
 * A {@link javafx.scene.layout.Region Region} that is not attached to a window uses a render scale of {@code 1.0}.
 * Applications should use the snapping methods on {@code Region} instead of implementing the formula above or
 * caching its result. The built-in methods use the appropriate current render scale and account for floating-point
 * error near pixel boundaries.
 *
 * <h3>Choosing the snapping operation</h3>
 *
 * {@code Region} provides three pairs of axis-specific snapping methods. Their names describe the semantic role of
 * the value being snapped, which influences the snapping direction:
 *
 * <table border="1">
 *     <caption>Snapping operations</caption>
 *     <thead>
 *         <tr>
 *             <th scope="col">Meaning</th>
 *             <th scope="col">Method</th>
 *             <th scope="col">Rounding</th>
 *             <th scope="col">Example</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>Position of a child</td>
 *             <td>{@link javafx.scene.layout.Region#snapPositionX(double) snapPositionX} or
 *                 {@link javafx.scene.layout.Region#snapPositionY(double) snapPositionY}</td>
 *             <td>Nearest pixel</td>
 *             <td>The final {@code x} or {@code y} passed to
 *                 {@link javafx.scene.Node#relocate(double, double) relocate}</td>
 *         </tr>
 *         <tr>
 *             <td>Size of a child</td>
 *             <td>{@link javafx.scene.layout.Region#snapSizeX(double) snapSizeX} or
 *                 {@link javafx.scene.layout.Region#snapSizeY(double) snapSizeY}</td>
 *             <td>Up to the next pixel</td>
 *             <td>The final {@code width} or {@code height} passed to
 *                 {@link javafx.scene.Node#resize(double, double) resize}</td>
 *         </tr>
 *         <tr>
 *             <td>Empty space</td>
 *             <td>{@link javafx.scene.layout.Region#snapSpaceX(double) snapSpaceX} or
 *                 {@link javafx.scene.layout.Region#snapSpaceY(double) snapSpaceY}</td>
 *             <td>Nearest pixel</td>
 *             <td>An inset, a padding, or a gap between adjacent children</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * The reason for the different snapping methods is semantic:
 * <ul>
 *     <li>A position is rounded to the nearest pixel so that an edge stays close to its requested coordinate.
 *     <li>Sizes use ceiling so that snapping itself does not reduce the measured content allocation.
 *     <li>Empty space can usually become slightly smaller without losing content.
 * </ul>
 *
 * The {@linkplain javafx.scene.layout.Region#snappedTopInset() snapped inset} methods return the combined border
 * inset and padding; custom controls should normally use these methods instead of snapping
 * {@linkplain javafx.scene.layout.Region#getInsets() raw insets} manually.
 *
 * <h3>Why snapped content can still look blurry</h3>
 *
 * Enabling pixel snapping on a region does not guarantee that the region or its descendants will be rendered on
 * pixel boundaries. The {@link javafx.scene.layout.Region#snapToPixelProperty() snapToPixel} property controls
 * layout calculations <em>performed by that region only</em>; it is not inherited from the parent and does not
 * affect whether children snap their contents. A region owns the position and size it allocates to a child;
 * the child owns the layout of its own descendants.
 * <p>
 * For example, setting only the child's property does not repair a fractional position assigned by its parent:
 * {@snippet :
 * parent.setSnapToPixel(false);
 * child.setSnapToPixel(true);
 *
 * // At scale 1.0, the parent places the child between pixels
 * child.relocate(10.5, 20.5);
 * }
 *
 * In this case, even if the child snaps its descendants in its local coordinates, the entire child subtree remains
 * shifted by half a pixel. Conversely, a parent can allocate a child pixel-aligned outer bounds even when that child
 * has disabled snapping for its own layout.
 * <p>
 * Transforms are applied after the layout has been computed. For example, at render scale {@code 1.0},
 * the following translation undoes the alignment established during layout:
 * {@snippet :
 * child.relocate(snapPositionX(10), snapPositionY(20));
 * child.setTranslateX(0.5); // The rendered X coordinate is now between pixels
 * }
 * <p>
 * Therefore, if content appears blurry despite pixel snapping being enabled, check both how its ancestors position
 * and size it and whether transforms affect its final rendered position.
 *
 * <h3>Why snapping is difficult</h3>
 *
 * There is no <em>single</em> correct direction in which to round a value, and no <em>single</em> place in a layout
 * algorithm where snapping must occur. Since a value can represent a position, content size, empty space, or a
 * combined sum of such elements, each semantic role can require a different decision:
 * <ul>
 *     <li>Snapping too early can lose precision, while snapping too late can introduce errors.
 *     <li>Snapping several children independently can avoid clipping, but the region's measurement and layout
 *         methods must use consistent calculations so that the computed content size includes those allocations.
 * </ul>
 *
 * Additionally, adding or subtracting already-snapped values can introduce floating-point drift that may cause a
 * later snapping operation to incorrectly add or remove a pixel. Calling a snapping method somewhere in a layout
 * algorithm is not, by itself, evidence that the resulting value is correctly snapped. Implementing correct snapping
 * therefore requires a great deal of caution to get it right.
 *
 * <h3>Checklist for correct snapping</h3>
 *
 * <ol>
 *     <li><b>Use the axis-specific snapping method.</b><br>
 *         A horizontal value uses the X scale and a vertical value uses the Y scale.
 *         This matters when those scales differ:
 *         {@snippet :
 *         double left = snapSpaceX(margin.getLeft()); // Correct
 *         double top = snapSpaceY(margin.getTop());   // Correct
 *
 *         double top = snapSpaceX(margin.getTop());   // Incorrect axis
 *         double gap = snapSpace(rawGap);             // Deprecated and ambiguous
 *         }
 *     <li><b>Identify the owner of the snapping policy.</b><br>
 *         The region arranging a child owns the child's position and its allocated size; use that region's
 *         {@code isSnapToPixel} policy. For example, write:
 *         {@snippet :
 *         // Correct: this region owns the child's position
 *         double x = snapPositionX(computeChildX());
 *         double y = snapPositionY(computeChildY());
 *         child.relocate(x, y);
 *
 *         // Incorrect: the child's snapping policy does not control placement by its parent
 *         double x = child.snapPositionX(computeChildX());
 *         double y = child.snapPositionY(computeChildY());
 *         child.relocate(x, y);
 *         }
 *     <li><b>Classify each value before choosing a snapping method.</b><br>
 *         Positions and spaces use nearest-pixel rounding; content sizes use ceiling.
 *         This distinction prevents gaps from growing unnecessarily and content from being clipped:
 *         {@snippet :
 *         double x = snapPositionX(rawX);       // coordinate
 *         double gap = snapSpaceX(rawGap);      // empty space
 *         double width = snapSizeX(rawWidth);   // content size
 *
 *         // Incorrect: a 0.1-unit gap becomes a full pixel
 *         double gap = snapSizeX(0.1);
 *         }
 *     <li><b>Re-snap calculations whose exact result is known to be pixel-aligned.</b><br>
 *         If every operand of an addition or subtraction has already been snapped, the <em>idealized</em>
 *         mathematical result is also pixel-aligned. However, floating-point arithmetic can cause that result
 *         to be slightly off the pixel grid. In this situation, the fractional remainder is <em>known</em> to
 *         be arithmetic drift rather than an intentional offset, so re-snap the final result using nearest-pixel
 *         rounding before returning it or using it in another layout calculation:
 *         <ul>
 *             <li>Re-snap a coordinate with {@code snapPositionX/Y}.
 *             <li>Re-snap empty space (gaps, margins, padding) or an allocated content span with {@code snapSpaceX/Y}.
 *             <li>Do not use {@code snapSizeX/Y} to snap such a result, because ceiling can turn positive
 *                 floating-point drift into an extra pixel.
 *         </ul>
 *         For example, write:
 *         {@snippet :
 *         double firstWidth = snapSizeX(firstChildWidth);
 *         double gapWidth = snapSpaceX(getGap());
 *         double secondWidth = snapSizeX(secondChildWidth);
 *
 *         // Correct: re-snap the final allocated span with snapSpaceX after arithmetic
 *         double allocatedWidth = snapSpaceX(firstWidth + gapWidth + secondWidth);
 *
 *         // Correct: coordinate calculated from snapped values is re-snapped as a position
 *         double childX = snapPositionX(snappedLeftInset() + allocatedWidth);
 *
 *         // Incorrect: snapSizeX can turn floating-point noise into an extra pixel
 *         double allocatedWidth = snapSizeX(firstWidth + gapWidth + secondWidth);
 *
 *         // Incorrect: the sum of snapped values can add floating-point drift
 *         double allocatedWidth = firstWidth + gapWidth + secondWidth;
 *         }
 *     <li><b>Snap independent allocations independently.</b><br>
 *         If two independent pieces of content each need their own pixels, snapping only their sum can under-allocate
 *         layout space. At scale {@code 1.0}, two content widths of {@code 0.4} <em>each</em> require one pixel, while
 *         their combined raw width of {@code 0.8} would be rounded to only one pixel:
 *         {@snippet :
 *         // Correct: each content item receives an independent allocation
 *         double total = snapSpaceX(snapSizeX(firstWidth) + snapSizeX(secondWidth)); // 2.0
 *
 *         // Incorrect: the raw sum is treated as one allocation
 *         double total = snapSpaceX(firstWidth + secondWidth); // 1.0
 *         }
 *         Only the correct example gives each child its own snapped allocation before adding them; the outer
 *         {@code snapSpaceX} follows the preceding re-snapping rule. This independent allocation rule also
 *         applies to distinct margins and gaps.
 *         <p>
 *         Note that this only applies to <em>independent</em> allocations. If several children must fit within a
 *         fixed allocated space, the algorithm must consider all children together and coordinate rounding children
 *         up or down so that their sum does not exceed the allocated space.
 *     <li><b>Do not repeatedly snap the same semantic value.</b><br>
 *         Preserve precision while refining a value and call the appropriate snapping method only after the
 *         refinement. Repeated snapping is especially dangerous for content sizes because {@code snapSizeX/Y}
 *         uses ceiling. For example, if an adjustment is part of the same content measurement:
 *         {@snippet :
 *         // Correct: compute all terms first, then allocate the width once
 *         double width = snapSizeX(measuredWidth + measurementAdjustment);
 *
 *         // Incorrect: snapping early throws away information and the second snapSizeX can add another pixel
 *         double width = snapSizeX(snapSizeX(measuredWidth) + measurementAdjustment);
 *         }
 *         At scale {@code 1.0}, if both values are {@code 0.2}, the first form returns {@code 1.0}, while the second
 *         (incorrect) form returns {@code 2.0}. This rule does not conflict with the preceding rule: two independent
 *         pieces of content are two allocations, while two intermediate terms describing one piece of content are
 *         one allocation.
 *     <li><b>Use the same snapped dependent dimension for measurement and layout.</b><br>
 *         Content-biased nodes have an order dependency between width and height. For example, for a
 *         horizontally-biased child, height depends on width. Establish the snapped width first and pass
 *         that exact width to the height calculation:
 *         {@snippet :
 *         static double boundedSize(double value, double min, double max) {
 *             return Math.min(Math.max(value, min), Math.max(min, max));
 *         }
 *
 *         // This assumes the child should fill the available width
 *         double rawChildWidth = boundedSize(
 *             availableWidth,
 *             child.minWidth(-1),
 *             child.maxWidth(-1));
 *
 *         // Correct:
 *         // 1. Snap the width the child will actually receive
 *         double snappedChildWidth = snapSizeX(rawChildWidth);
 *
 *         // 2. Use the snapped width for every dependent height measurement
 *         double rawChildHeight = boundedSize(
 *             child.prefHeight(snappedChildWidth),
 *             child.minHeight(snappedChildWidth),
 *             child.maxHeight(snappedChildWidth));
 *
 *         // 3. Snap the height the child will receive
 *         double snappedChildHeight = snapSizeY(rawChildHeight);
 *
 *         // Incorrect:
 *         // 1. The dependent height is measured for rawChildWidth
 *         double rawChildHeight = boundedSize(
 *             child.prefHeight(rawChildWidth),
 *             child.minHeight(rawChildWidth),
 *             child.maxHeight(rawChildWidth));
 *
 *          // 2. rawChildHeight was measured for a width that the child might not
 *          //    actually receive, which makes the snapped value potentially wrong
 *          double snappedChildHeight = snapSizeY(rawChildHeight);
 *         }
 *     <li><b>Deliberately apply the snapping policy to values determined by the parent.</b><br>
 *         Since a region's position and allocated size are determined by its parent (and the {@code isSnapToPixel}
 *         policy of its parent), it must not reposition or resize itself. If the allocated width or height is not
 *         pixel-aligned, the region cannot both preserve the exact allocation and make its complete bounds
 *         pixel-aligned. The region must choose how to lay out its children according to its fitting and overflow
 *         policy. For example, a fill policy that accepts a slight underflow or overflow can round the available
 *         span to the nearest pixel before laying out a resizable child:
 *         {@snippet :
 *         // Snap own width and height to determine the content rectangle for children
 *         double availableWidth = snapSpaceX(getWidth());
 *         double availableHeight = snapSpaceY(getHeight());
 *
 *         // Assumes a completely unconstrained, resizable child
 *         child.resizeRelocate(0, 0, availableWidth, availableHeight);
 *         }
 *     <li><b>Property values should not be snapped when set, only when they are consumed.</b><br>
 *         The setter of a geometric property should not snap the value, and the getter should return the
 *         exact value set by the application:
 *         {@snippet :
 *         // Correct
 *         public void setGap(double value) {
 *             gap.set(value);
 *         }
 *
 *         protected void layoutChildren() {
 *             double snappedGap = snapSpaceX(getGap());
 *             // use snappedGap for this horizontal allocation
 *         }
 *
 *         // Incorrect: this loses the requested value and uses whichever scale is
 *         //            current when the setter happens to be called
 *         public void setGap(double value) {
 *             gap.set(snapSpaceX(value));
 *         }
 *         }
 * </ol>
 */
package javafx.scene.layout;
