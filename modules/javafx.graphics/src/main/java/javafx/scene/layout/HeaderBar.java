/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PreviewFeature;
import com.sun.javafx.geom.Vec2d;
import com.sun.javafx.scene.layout.HeaderButtonBehavior;
import com.sun.javafx.stage.HeaderButtonMetrics;
import com.sun.javafx.stage.StageHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.css.StyleableDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Subscription;

/**
 * A client-area header bar that is used as a replacement for the system-provided header bar in stages
 * with the {@link StageStyle#EXTENDED} style. This class enables the <em>click-and-drag to move</em> and
 * <em>double-click to maximize</em> behaviors that are usually afforded by system-provided header bars.
 * The entire {@code HeaderBar} background is draggable by default, but its content is not. Applications
 * can specify draggable content nodes of the {@code HeaderBar} with the {@link #setDragType(Node, HeaderDragType)}
 * method.
 * <p>
 * {@code HeaderBar} is a layout container that allows applications to place scene graph nodes in three areas:
 * {@link #leadingProperty() leading}, {@link #centerProperty() center}, and {@link #trailingProperty() trailing}.
 * All areas can be {@code null}. The default {@link #minHeightProperty() minHeight} of the {@code HeaderBar} is
 * set to match the height of the platform-specific default header buttons.
 *
 * <h2>Single header bar</h2>
 * Most applications should only add a single {@code HeaderBar} to the scene graph, placed at the top of the
 * scene and extending its entire width. This ensures that the reported values for
 * {@link #leftSystemInsetProperty() leftSystemInset} and {@link #rightSystemInsetProperty() rightSystemInset},
 * which describe the area reserved for the system-provided window buttons, correctly align with the location
 * of the {@code HeaderBar} and are taken into account when the contents of the {@code HeaderBar} are laid out.
 *
 * <h2>Multiple header bars</h2>
 * Applications that use multiple header bars might need to configure the additional padding inserted into the
 * layout to account for the system-reserved areas. For example, when two header bars are placed next to each
 * other in the horizontal direction, the default configuration incorrectly adds additional padding between the
 * two header bars. In this case, the {@link #leadingSystemPaddingProperty() leadingSystemPadding} and
 * {@link #trailingSystemPaddingProperty() trailingSystemPadding} properties can be used to remove the padding
 * that is not needed.
 *
 * <h2>Header button height</h2>
 * Applications can specify the preferred height for system-provided header buttons by setting the static
 * {@link #setPrefButtonHeight(Stage, double)} property on the {@code Stage} associated with the header bar.
 * This can be used to achieve a more cohesive visual appearance by having the system-provided header buttons
 * match the height of the client-area header bar.
 *
 * <h2>Custom header buttons</h2>
 * If more control over the header buttons is desired, applications can opt out of the system-provided header
 * buttons by setting {@link #setPrefButtonHeight(Stage, double)} to zero and place custom header buttons in
 * the JavaFX scene graph instead. Any JavaFX control can be used as a custom header button by setting its
 * semantic type with the {@link #setButtonType(Node, HeaderButtonType)} method.
 *
 * <h2>System menu</h2>
 * Some platforms support a system menu that can be summoned by right-clicking the draggable area.
 * The system menu will not be shown when:
 * <ol>
 *     <li>the {@code Stage} is in {@link Stage#fullScreenProperty() full-screen mode}, or
 *     <li>the {@code HeaderBar} has {@link Event#consume() consumed} the
 *         {@link ContextMenuEvent#CONTEXT_MENU_REQUESTED} event.
 * </ol>
 *
 * <h2>Layout constraints</h2>
 * The {@code leading} and {@code trailing} children will be resized to their preferred widths and extend the
 * height of the {@code HeaderBar}. The {@code center} child will be resized to fill the available space.
 * {@code HeaderBar} honors the minimum, preferred, and maximum sizes of its children. If a child's resizable
 * range prevents it from be resized to fit within its position, it will be vertically centered relative to the
 * available space; this alignment can be customized with a layout constraint.
 * <p>
 * An application may set constraints on individual children to customize their layout.
 * For each constraint, {@code HeaderBar} provides static getter and setter methods.
 * <table style="white-space: nowrap">
 *     <caption>Layout constraints of {@code HeaderBar}</caption>
 *     <thead>
 *         <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 *     </thead>
 *     <tbody>
 *         <tr><th>alignment</th><td>{@link Pos}</td>
 *             <td>The alignment of the child within its area of the {@code HeaderBar}.</td>
 *         </tr>
 *         <tr><th>margin</th>
 *             <td>{@link Insets}</td><td>Margin space around the outside of the child.</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <h2>Special layout of centered child</h2>
 * If a child is configured to be centered in the {@link #centerProperty() center} area (i.e. its {@code alignment}
 * constraint is either {@code null}, {@link Pos#CENTER}, {@link Pos#TOP_CENTER}, or {@link Pos#BOTTOM_CENTER}),
 * it will be centered with respect to the entire header bar, and not with respect to the {@code center} area only.
 * This means that, for a header bar that extends the entire width of the {@code Stage}, the child will appear to
 * be horizontally centered within the {@code Stage}.
 * <p>
 * If a child should instead be centered with respect to the {@code center} area only, a possible solution is to
 * place another layout container like {@link BorderPane} in the {@code center} area, and then center the child
 * within the other layout container.
 *
 * <h2>Example</h2>
 * Usually, {@code HeaderBar} is placed in a root container like {@code BorderPane} to align it
 * with the top of the scene:
 * <pre>{@code
 * public class MyApp extends Application {
 *     @Override
 *     public void start(Stage stage) {
 *         var button = new Button("My button");
 *         HeaderBar.setAlignment(button, Pos.CENTER_LEFT);
 *         HeaderBar.setMargin(button, new Insets(5));
 *
 *         var headerBar = new HeaderBar();
 *         headerBar.setCenter(button);
 *
 *         var root = new BorderPane();
 *         root.setTop(headerBar);
 *
 *         stage.setScene(new Scene(root));
 *         stage.initStyle(StageStyle.EXTENDED);
 *         stage.show();
 *     }
 * }
 * }</pre>
 *
 * @since 25
 * @deprecated This is a preview feature which may be changed or removed in a future release.
 */
@Deprecated(since = "25")
public class HeaderBar extends Region {

    private static final Dimension2D EMPTY = new Dimension2D(0, 0);
    private static final String DRAG_TYPE = "headerbar-drag-type";
    private static final String BUTTON_TYPE = "headerbar-button-type";
    private static final String ALIGNMENT = "headerbar-alignment";
    private static final String MARGIN = "headerbar-margin";

    /**
     * Specifies the {@code HeaderDragType} of the child, indicating whether it is a draggable
     * part of the {@code HeaderBar}.
     * <p>
     * Setting the value to {@code null} will remove the flag.
     *
     * @param child the child node
     * @param value the {@code HeaderDragType}, or {@code null} to remove the flag
     */
    public static void setDragType(Node child, HeaderDragType value) {
        Pane.setConstraint(child, DRAG_TYPE, value);
    }

    /**
     * Returns the {@code HeaderDragType} of the specified child.
     *
     * @param child the child node
     * @return the {@code HeaderDragType}, or {@code null} if not set
     */
    public static HeaderDragType getDragType(Node child) {
        return (HeaderDragType)Pane.getConstraint(child, DRAG_TYPE);
    }

    /**
     * Specifies the {@code HeaderButtonType} of the child, indicating its semantic use in the header bar.
     * <p>
     * This property can be set on any {@link Node}. Specifying a header button type also provides the behavior
     * associated with the button type. If the default behavior is not desired, applications can register an
     * event filter on the child node that consumes the {@link MouseEvent#MOUSE_RELEASED} event.
     *
     * @param child the child node
     * @param value the {@code HeaderButtonType}, or {@code null}
     */
    public static void setButtonType(Node child, HeaderButtonType value) {
        Pane.setConstraint(child, BUTTON_TYPE, value);

        if (child.getProperties().get(HeaderButtonBehavior.class) instanceof HeaderButtonBehavior behavior) {
            behavior.dispose();
        }

        if (value != null) {
            child.getProperties().put(HeaderButtonBehavior.class, new HeaderButtonBehavior(child, value));
        } else {
            child.getProperties().remove(HeaderButtonBehavior.class);
        }
    }

    /**
     * Returns the {@code HeaderButtonType} of the specified child.
     *
     * @param child the child node
     * @return the {@code HeaderButtonType}, or {@code null}
     */
    public static HeaderButtonType getButtonType(Node child) {
        return (HeaderButtonType)Pane.getConstraint(child, BUTTON_TYPE);
    }

    /**
     * Sentinel value that can be used for {@link #setPrefButtonHeight(Stage, double)} to indicate that
     * the platform should choose the platform-specific default button height.
     */
    public static final double USE_DEFAULT_SIZE = -1;

    /**
     * Specifies the preferred height of the system-provided header buttons of the specified stage.
     * <p>
     * Any value except zero and {@link #USE_DEFAULT_SIZE} is only a hint for the platform window toolkit.
     * The platform might accommodate the preferred height in various ways, such as by stretching the header
     * buttons (fully or partially) to fill the preferred height, or centering the header buttons (fully or
     * partially) within the preferred height. Some platforms might only accommodate the preferred height
     * within platform-specific constraints, or ignore it entirely.
     * <p>
     * Setting the preferred height to zero hides the system-provided header buttons, allowing applications to
     * use custom header buttons instead (see {@link #setButtonType(Node, HeaderButtonType)}).
     * <p>
     * The default value {@code USE_DEFAULT_SIZE} indicates that the platform should choose the button height.
     *
     * @param stage the {@code Stage}
     * @param height the preferred height, or 0 to hide the system-provided header buttons
     */
    public static void setPrefButtonHeight(Stage stage, double height) {
        StageHelper.setPrefHeaderButtonHeight(stage, height);
    }

    /**
     * Returns the preferred height of the system-provided header buttons of the specified stage.
     *
     * @param stage the {@code Stage}
     * @return the preferred height of the system-provided header buttons
     */
    public static double getPrefButtonHeight(Stage stage) {
        return StageHelper.getPrefHeaderButtonHeight(stage);
    }

    /**
     * Sets the alignment for the child when contained in a {@code HeaderBar}.
     * If set, will override the header bar's default alignment for the child's position.
     * Setting the value to {@code null} will remove the constraint.
     *
     * @param child the child node
     * @param value the alignment position
     */
    public static void setAlignment(Node child, Pos value) {
        Pane.setConstraint(child, ALIGNMENT, value);
    }

    /**
     * Returns the child's alignment in the {@code HeaderBar}.
     *
     * @param child the child node
     * @return the alignment position for the child, or {@code null} if no alignment was set
     */
    public static Pos getAlignment(Node child) {
        return (Pos)Pane.getConstraint(child, ALIGNMENT);
    }

    /**
     * Sets the margin for the child when contained in a {@code HeaderBar}.
     * If set, the header bar will lay it out with the margin space around it.
     * Setting the value to {@code null} will remove the constraint.
     *
     * @param child the child node
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        Pane.setConstraint(child, MARGIN, value);
    }

    /**
     * Returns the child's margin.
     *
     * @param child the child node
     * @return the margin for the child, or {@code null} if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)Pane.getConstraint(child, MARGIN);
    }

    private Subscription subscription = Subscription.EMPTY;
    private HeaderButtonMetrics currentMetrics;
    private boolean currentFullScreen;

    /**
     * Creates a new {@code HeaderBar}.
     */
    public HeaderBar() {
        PreviewFeature.HEADER_BAR.checkEnabled();

        // Inflate the minHeight property. This is important so that we can track whether a stylesheet or
        // user code changes the property value before we set it to the height of the native title bar.
        minHeightProperty();

        ObservableValue<Stage> stage = sceneProperty()
            .flatMap(Scene::windowProperty)
            .map(w -> w instanceof Stage s ? s : null);

        stage.flatMap(Stage::fullScreenProperty)
            .orElse(false)
            .subscribe(this::onFullScreenChanged);

        stage.subscribe(this::onStageChanged);
    }

    /**
     * Creates a new {@code HeaderBar} with the specified children.
     *
     * @param leading the leading node, or {@code null}
     * @param center the center node, or {@code null}
     * @param trailing the trailing node, or {@code null}
     */
    public HeaderBar(Node leading, Node center, Node trailing) {
        this();
        setLeading(leading);
        setCenter(center);
        setTrailing(trailing);
    }

    private void onStageChanged(Stage stage) {
        subscription.unsubscribe();

        if (stage != null) {
            subscription = StageHelper.getHeaderButtonMetrics(stage).subscribe(this::onMetricsChanged);
        }
    }

    private void onMetricsChanged(HeaderButtonMetrics metrics) {
        currentMetrics = metrics;
        updateInsets();
    }

    private void onFullScreenChanged(boolean fullScreen) {
        currentFullScreen = fullScreen;
        updateInsets();
    }

    private void updateInsets() {
        if (currentFullScreen || currentMetrics == null) {
            leftSystemInset.set(EMPTY);
            rightSystemInset.set(EMPTY);
            minSystemHeight.set(0);
        } else {
            leftSystemInset.set(currentMetrics.leftInset());
            rightSystemInset.set(currentMetrics.rightInset());
            minSystemHeight.set(currentMetrics.minHeight());
        }
    }

    /**
     * Describes the size of the left system-reserved inset, which is an area reserved for the iconify, maximize,
     * and close window buttons. If there are no window buttons on the left side of the window, the returned area
     * is an empty {@code Dimension2D}.
     * <p>
     * Note that the left system inset refers to the left side of the window, independent of layout orientation.
     */
    private final ReadOnlyObjectWrapper<Dimension2D> leftSystemInset =
        new ReadOnlyObjectWrapper<>(this, "leftSystemInset", EMPTY) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    public final ReadOnlyObjectProperty<Dimension2D> leftSystemInsetProperty() {
        return leftSystemInset.getReadOnlyProperty();
    }

    public final Dimension2D getLeftSystemInset() {
        return leftSystemInset.get();
    }

    /**
     * Describes the size of the right system-reserved inset, which is an area reserved for the iconify, maximize,
     * and close window buttons. If there are no window buttons on the right side of the window, the returned area
     * is an empty {@code Dimension2D}.
     * <p>
     * Note that the right system inset refers to the right side of the window, independent of layout orientation.
     */
    private final ReadOnlyObjectWrapper<Dimension2D> rightSystemInset =
        new ReadOnlyObjectWrapper<>(this, "rightSystemInset", EMPTY) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    public final ReadOnlyObjectProperty<Dimension2D> rightSystemInsetProperty() {
        return rightSystemInset.getReadOnlyProperty();
    }

    public final Dimension2D getRightSystemInset() {
        return rightSystemInset.get();
    }

    /**
     * The system-provided minimum recommended height for the {@code HeaderBar}, which usually corresponds
     * to the height of the default header buttons. Applications can use this value as a sensible lower limit
     * for the height of the {@code HeaderBar}.
     * <p>
     * By default, {@link #minHeightProperty() minHeight} is set to the value of {@code minSystemHeight},
     * unless {@code minHeight} is explicitly set by a stylesheet or application code.
     */
    private final ReadOnlyDoubleWrapper minSystemHeight =
        new ReadOnlyDoubleWrapper(this, "minSystemHeight") {
            @Override
            protected void invalidated() {
                double height = get();
                var minHeight = (StyleableDoubleProperty)minHeightProperty();

                // Only change minHeight if it was not set by a stylesheet or application code.
                if (minHeight.getStyleOrigin() == null) {
                    minHeight.applyStyle(null, height);
                }
            }
        };

    public final ReadOnlyDoubleProperty minSystemHeightProperty() {
        return minSystemHeight.getReadOnlyProperty();
    }

    public final double getMinSystemHeight() {
        return minSystemHeight.get();
    }

    /**
     * The leading area of the {@code HeaderBar}.
     * <p>
     * The leading area corresponds to the left area in a left-to-right layout, and to the right area
     * in a right-to-left layout.
     *
     * @defaultValue {@code null}
     */
    private final ObjectProperty<Node> leading = new NodeProperty("leading");

    public final ObjectProperty<Node> leadingProperty() {
        return leading;
    }

    public final Node getLeading() {
        return leading.get();
    }

    public final void setLeading(Node value) {
        leading.set(value);
    }

    /**
     * The center area of the {@code HeaderBar}.
     *
     * @defaultValue {@code null}
     */
    private final ObjectProperty<Node> center = new NodeProperty("center");

    public final ObjectProperty<Node> centerProperty() {
        return center;
    }

    public final Node getCenter() {
        return center.get();
    }

    public final void setCenter(Node value) {
        center.set(value);
    }

    /**
     * The trailing area of the {@code HeaderBar}.
     * <p>
     * The trailing area corresponds to the right area in a left-to-right layout, and to the left area
     * in a right-to-left layout.
     *
     * @defaultValue {@code null}
     */
    private final ObjectProperty<Node> trailing = new NodeProperty("trailing");

    public final ObjectProperty<Node> trailingProperty() {
        return trailing;
    }

    public final Node getTrailing() {
        return trailing.get();
    }

    public final void setTrailing(Node value) {
        trailing.set(value);
    }

    /**
     * Specifies whether additional padding should be added to the leading side of the {@code HeaderBar}.
     * The size of the additional padding corresponds to the size of the system-reserved area that contains
     * the default header buttons (iconify, maximize, and close). If the system-reserved area contains no
     * header buttons, no additional padding is added to the leading side of the {@code HeaderBar}.
     * <p>
     * Applications that use a single {@code HeaderBar} extending the entire width of the window should
     * set this property to {@code true} to prevent the header buttons from overlapping the content of the
     * {@code HeaderBar}.
     *
     * @defaultValue {@code true}
     * @see #trailingSystemPaddingProperty() trailingSystemPadding
     */
    private final BooleanProperty leadingSystemPadding = new BooleanPropertyBase(true) {
        @Override
        public Object getBean() {
            return HeaderBar.this;
        }

        @Override
        public String getName() {
            return "leadingSystemPadding";
        }

        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    public final BooleanProperty leadingSystemPaddingProperty() {
        return leadingSystemPadding;
    }

    public final boolean isLeadingSystemPadding() {
        return leadingSystemPadding.get();
    }

    public final void setLeadingSystemPadding(boolean value) {
        leadingSystemPadding.set(value);
    }

    /**
     * Specifies whether additional padding should be added to the trailing side of the {@code HeaderBar}.
     * The size of the additional padding corresponds to the size of the system-reserved area that contains
     * the default header buttons (iconify, maximize, and close). If the system-reserved area contains no
     * header buttons, no additional padding is added to the trailing side of the {@code HeaderBar}.
     * <p>
     * Applications that use a single {@code HeaderBar} extending the entire width of the window should
     * set this property to {@code true} to prevent the header buttons from overlapping the content of the
     * {@code HeaderBar}.
     *
     * @defaultValue {@code true}
     * @see #leadingSystemPaddingProperty() leadingSystemPadding
     */
    private final BooleanProperty trailingSystemPadding = new BooleanPropertyBase(true) {
        @Override
        public Object getBean() {
            return HeaderBar.this;
        }

        @Override
        public String getName() {
            return "trailingSystemPadding";
        }

        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    public final BooleanProperty trailingSystemPaddingProperty() {
        return trailingSystemPadding;
    }

    public final boolean isTrailingSystemPadding() {
        return trailingSystemPadding.get();
    }

    public final void setTrailingSystemPadding(boolean value) {
        trailingSystemPadding.set(value);
    }

    private boolean isLeftSystemPadding(NodeOrientation nodeOrientation) {
        return nodeOrientation == NodeOrientation.LEFT_TO_RIGHT && isLeadingSystemPadding()
            || nodeOrientation == NodeOrientation.RIGHT_TO_LEFT && isTrailingSystemPadding();
    }

    private boolean isRightSystemPadding(NodeOrientation nodeOrientation) {
        return nodeOrientation == NodeOrientation.LEFT_TO_RIGHT && isTrailingSystemPadding()
            || nodeOrientation == NodeOrientation.RIGHT_TO_LEFT && isLeadingSystemPadding();
    }

    @Override
    protected double computeMinWidth(double height) {
        Node leading = getLeading();
        Node center = getCenter();
        Node trailing = getTrailing();
        Insets insets = getInsets();
        double leftPrefWidth;
        double rightPrefWidth;
        double centerMinWidth;
        double systemPaddingWidth = 0;

        if (height != -1
                && (childHasContentBias(leading, Orientation.VERTICAL) ||
                    childHasContentBias(trailing, Orientation.VERTICAL) ||
                    childHasContentBias(center, Orientation.VERTICAL))) {
            double areaHeight = Math.max(0, height);
            leftPrefWidth = getAreaWidth(leading, areaHeight, false);
            rightPrefWidth = getAreaWidth(trailing, areaHeight, false);
            centerMinWidth = getAreaWidth(center, areaHeight, true);
        } else {
            leftPrefWidth = getAreaWidth(leading, -1, false);
            rightPrefWidth = getAreaWidth(trailing, -1, false);
            centerMinWidth = getAreaWidth(center, -1, true);
        }

        NodeOrientation nodeOrientation = getEffectiveNodeOrientation();

        if (isLeftSystemPadding(nodeOrientation)) {
            systemPaddingWidth += getLeftSystemInset().getWidth();
        }

        if (isRightSystemPadding(nodeOrientation)) {
            systemPaddingWidth += getRightSystemInset().getWidth();
        }

        return insets.getLeft()
             + leftPrefWidth
             + centerMinWidth
             + rightPrefWidth
             + insets.getRight()
             + systemPaddingWidth;
    }

    @Override
    protected double computeMinHeight(double width) {
        Node leading = getLeading();
        Node center = getCenter();
        Node trailing = getTrailing();
        Insets insets = getInsets();
        double leadingMinHeight = getAreaHeight(leading, -1, true);
        double trailingMinHeight = getAreaHeight(trailing, -1, true);
        double centerMinHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leadingPrefWidth = getAreaWidth(leading, -1, false);
            double trailingPrefWidth = getAreaWidth(trailing, -1, false);
            centerMinHeight = getAreaHeight(center, Math.max(0, width - leadingPrefWidth - trailingPrefWidth), true);
        } else {
            centerMinHeight = getAreaHeight(center, -1, true);
        }

        return insets.getTop()
             + insets.getBottom()
             + Math.max(centerMinHeight, Math.max(trailingMinHeight, leadingMinHeight));
    }

    @Override
    protected double computePrefHeight(double width) {
        Node leading = getLeading();
        Node center = getCenter();
        Node trailing = getTrailing();
        Insets insets = getInsets();
        double leadingPrefHeight = getAreaHeight(leading, -1, false);
        double trailingPrefHeight = getAreaHeight(trailing, -1, false);
        double centerPrefHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leadingPrefWidth = getAreaWidth(leading, -1, false);
            double trailingPrefWidth = getAreaWidth(trailing, -1, false);
            centerPrefHeight = getAreaHeight(center, Math.max(0, width - leadingPrefWidth - trailingPrefWidth), false);
        } else {
            centerPrefHeight = getAreaHeight(center, -1, false);
        }

        return insets.getTop()
             + insets.getBottom()
             + Math.max(centerPrefHeight, Math.max(trailingPrefHeight, leadingPrefHeight));
    }

    @Override
    public boolean usesMirroring() {
        return false;
    }

    @Override
    protected void layoutChildren() {
        Node center = getCenter();
        Node left, right;
        Insets insets = getInsets();
        NodeOrientation nodeOrientation = getEffectiveNodeOrientation();
        boolean rtl = nodeOrientation == NodeOrientation.RIGHT_TO_LEFT;
        double width = Math.max(getWidth(), minWidth(-1));
        double height = Math.max(getHeight(), minHeight(-1));
        double leftWidth = 0;
        double rightWidth = 0;
        double insideY = insets.getTop();
        double insideHeight = height - insideY - insets.getBottom();
        double insideX, insideWidth;
        double leftSystemPaddingWidth = isLeftSystemPadding(nodeOrientation) ? getLeftSystemInset().getWidth() : 0;
        double rightSystemPaddingWidth = isRightSystemPadding(nodeOrientation) ? getRightSystemInset().getWidth() : 0;

        if (rtl) {
            left = getTrailing();
            right = getLeading();
            insideX = insets.getRight() + leftSystemPaddingWidth;
            insideWidth = width - insideX - insets.getLeft() - rightSystemPaddingWidth;
        } else {
            left = getLeading();
            right = getTrailing();
            insideX = insets.getLeft() + leftSystemPaddingWidth;
            insideWidth = width - insideX - insets.getRight() - rightSystemPaddingWidth;
        }

        if (left != null && left.isManaged()) {
            Insets leftMargin = adjustMarginForRTL(getNodeMargin(left), rtl);
            double adjustedWidth = adjustWidthByMargin(insideWidth, leftMargin);
            double childWidth = resizeChild(left, adjustedWidth, false, insideHeight, leftMargin);
            leftWidth = snapSpaceX(leftMargin.getLeft()) + childWidth + snapSpaceX(leftMargin.getRight());
            Pos alignment = getAlignment(left);

            positionInArea(
                left, insideX, insideY,
                leftWidth, insideHeight, 0,
                leftMargin,
                alignment != null ? alignment.getHpos() : HPos.CENTER,
                alignment != null ? alignment.getVpos() : VPos.CENTER,
                isSnapToPixel());
        }

        if (right != null && right.isManaged()) {
            Insets rightMargin = adjustMarginForRTL(getNodeMargin(right), rtl);
            double adjustedWidth = adjustWidthByMargin(insideWidth - leftWidth, rightMargin);
            double childWidth = resizeChild(right, adjustedWidth, false, insideHeight, rightMargin);
            rightWidth = snapSpaceX(rightMargin.getLeft()) + childWidth + snapSpaceX(rightMargin.getRight());
            Pos alignment = getAlignment(right);

            positionInArea(
                right, insideX + insideWidth - rightWidth, insideY,
                rightWidth, insideHeight, 0,
                rightMargin,
                alignment != null ? alignment.getHpos() : HPos.CENTER,
                alignment != null ? alignment.getVpos() : VPos.CENTER,
                isSnapToPixel());
        }

        if (center != null && center.isManaged()) {
            Insets centerMargin = adjustMarginForRTL(getNodeMargin(center), rtl);
            Pos alignment = getAlignment(center);

            if (alignment == null || alignment.getHpos() == HPos.CENTER) {
                double adjustedWidth = adjustWidthByMargin(insideWidth - leftWidth - rightWidth, centerMargin);
                double childWidth = resizeChild(center, adjustedWidth, true, insideHeight, centerMargin);
                double idealX = width / 2 - childWidth / 2;
                double minX = insideX + leftWidth + centerMargin.getLeft();
                double maxX = insideX + insideWidth - rightWidth - centerMargin.getRight();
                double adjustedX;

                if (idealX < minX) {
                    adjustedX = minX;
                } else if (idealX + childWidth > maxX) {
                    adjustedX = maxX - childWidth;
                } else {
                    adjustedX = idealX;
                }

                positionInArea(
                    center,
                    adjustedX, insideY,
                    childWidth, insideHeight, 0,
                    new Insets(centerMargin.getTop(), 0, centerMargin.getBottom(), 0),
                    HPos.LEFT, alignment != null ? alignment.getVpos() : VPos.CENTER,
                    isSnapToPixel());
            } else {
                layoutInArea(
                    center,
                    insideX + leftWidth, insideY,
                    insideWidth - leftWidth - rightWidth, insideHeight, 0,
                    centerMargin,
                    alignment.getHpos(), alignment.getVpos());
            }
        }
    }

    private Insets adjustMarginForRTL(Insets margin, boolean rtl) {
        if (margin == null) {
            return null;
        }

        return rtl
            ? new Insets(margin.getTop(), margin.getLeft(), margin.getBottom(), margin.getRight())
            : margin;
    }

    private boolean childHasContentBias(Node child, Orientation orientation) {
        if (child != null && child.isManaged()) {
            return child.getContentBias() == orientation;
        }

        return false;
    }

    private double resizeChild(Node child, double adjustedWidth, boolean fillWidth, double insideHeight, Insets margin) {
        double adjustedHeight = adjustHeightByMargin(insideHeight, margin);
        double childWidth = fillWidth ? adjustedWidth : Math.min(snapSizeX(child.prefWidth(adjustedHeight)), adjustedWidth);
        Vec2d size = boundedNodeSizeWithBias(child, childWidth, adjustedHeight, true, true, TEMP_VEC2D);
        size.x = snapSizeX(size.x);
        size.y = snapSizeX(size.y);
        child.resize(size.x, size.y);
        return size.x;
    }

    private double getAreaWidth(Node child, double height, boolean minimum) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return minimum
                ? computeChildMinAreaWidth(child, -1, margin, height, false)
                : computeChildPrefAreaWidth(child, -1, margin, height, false);
        }

        return 0;
    }

    private double getAreaHeight(Node child, double width, boolean minimum) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return minimum
                ? computeChildMinAreaHeight(child, -1, margin, width, false)
                : computeChildPrefAreaHeight(child, -1, margin, width, false);
        }

        return 0;
    }

    private Insets getNodeMargin(Node child) {
        Insets margin = getMargin(child);
        return margin != null ? margin : Insets.EMPTY;
    }

    private final class NodeProperty extends ObjectPropertyBase<Node> {
        private final String name;
        private Node value;

        NodeProperty(String name) {
            this.name = name;
        }

        @Override
        public Object getBean() {
            return HeaderBar.this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        protected void invalidated() {
            if (value != null) {
                getChildren().remove(value);
            }

            value = get();

            if (value != null) {
                getChildren().add(value);
            }
        }
    }
}
