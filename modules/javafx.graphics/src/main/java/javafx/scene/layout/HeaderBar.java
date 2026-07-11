/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Vec2d;
import com.sun.javafx.scene.layout.HeaderButtonBehavior;
import com.sun.javafx.stage.StageHelper;
import javafx.application.ColorScheme;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
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
 * can specify draggable content nodes of the {@code HeaderBar} with the {@link #dragTypeProperty(Node) dragType}
 * attached property.
 * <p>
 * {@code HeaderBar} is a layout container that allows applications to place scene graph nodes in three areas:
 * {@link #leftProperty() left}, {@link #centerProperty() center}, and {@link #rightProperty() right}.
 * All areas can be {@code null}. The default {@link #minHeightProperty() minHeight} of the {@code HeaderBar} is
 * set to match {@link #systemMinHeightProperty(Stage) systemMinHeight} (unless explicitly set by a stylesheet or
 * application code), which usually corresponds to the height of the system-provided header buttons.
 *
 * <h2>Single header bar</h2>
 * Most applications should only add a single {@code HeaderBar} to the scene graph, placed at the top of the
 * scene and extending its entire width. This ensures that the reported values for
 * {@link #leftSystemInsetProperty(Stage) leftSystemInset} and {@link #rightSystemInsetProperty(Stage) rightSystemInset},
 * which describe the areas reserved for the system-provided header buttons, correctly align with the location
 * of the {@code HeaderBar} and are taken into account when the contents of the {@code HeaderBar} are laid out.
 *
 * <h2>Multiple header bars</h2>
 * Applications that use multiple header bars might need to configure the additional padding inserted into the
 * layout to account for the system-reserved areas. For example, when two header bars are placed next to each
 * other in the horizontal direction, the default configuration adds unnecessary additional padding between the
 * two header bars. In this case, the {@link #leftSystemPaddingProperty() leftSystemPadding} and
 * {@link #rightSystemPaddingProperty() rightSystemPadding} properties can be used to remove the padding
 * that is not needed.
 *
 * <h2>Header button height</h2>
 * Applications can specify the preferred height for the system-provided header buttons by setting the
 * {@link #systemButtonHeightProperty(Stage) systemButtonHeight} attached property for the {@code Stage} associated
 * with the header bar. This can be used to achieve a more cohesive visual appearance by having the system-provided
 * header buttons match the height of the client-area header bar.
 *
 * <h2>Color scheme</h2>
 * The color scheme of the system-provided default header buttons is automatically adjusted to match the
 * {@linkplain Scene.Preferences#colorSchemeProperty() scene color scheme}.
 * Applications can specify a different color scheme for the system-provided header buttons with the
 * {@link HeaderBar#systemColorSchemeProperty(Stage) systemColorScheme} attached property.
 *
 * <h2>Custom header buttons</h2>
 * If more control over the header buttons is desired, applications can opt out of the system-provided header
 * buttons by setting the {@link #systemButtonHeightProperty(Stage) systemButtonHeight} attached property for
 * the {@code Stage} associated with the header bar to zero and place custom header buttons in the JavaFX
 * scene graph instead. Any JavaFX control can be used as a custom header button by specifying its semantic
 * type with the {@link #buttonTypeProperty(Node) buttonType} attached property.
 *
 * <h2>System menu</h2>
 * Some platforms support a system menu that can be summoned by right-clicking the draggable area.
 * The system menu will not be shown when:
 * <ol>
 *     <li>the {@code Stage} is in {@linkplain Stage#fullScreenProperty() full-screen mode}, or
 *     <li>the {@code HeaderBar} has {@linkplain Event#consume() consumed} the
 *         {@link ContextMenuEvent#CONTEXT_MENU_REQUESTED} event.
 * </ol>
 *
 * <h2>Layout constraints</h2>
 * The {@code left} and {@code right} children will be resized to their preferred widths and extend the
 * height of the {@code HeaderBar}. The {@code center} child will be resized to fill the available space.
 * {@code HeaderBar} honors the minimum, preferred, and maximum sizes of its children. If a child's resizable
 * range prevents it from being resized to fit within its position, it will be vertically centered relative to
 * the available space; this alignment can be customized with a layout constraint.
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
 * place another layout container (such as {@link BorderPane}) in the {@code center} area, and then center the child
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
 * @since 27
 */
public class HeaderBar extends Region {

    private static final String ALIGNMENT = "headerbar-alignment";
    private static final String MARGIN = "headerbar-margin";

    /**
     * Sets the value of the {@link #dragTypeProperty(Node) dragType}
     * attached property for the specified child.
     *
     * @param child the child node
     * @param value the {@code HeaderDragType}, or {@code null} to remove the flag
     */
    public static void setDragType(Node child, HeaderDragType value) {
        if (getDragType(child) != value) {
            dragTypeProperty(child).set(value);
        }
    }

    /**
     * Gets the value of the {@link #dragTypeProperty(Node) dragType}
     * attached property for the specified child.
     *
     * @param child the child node
     * @return the {@code HeaderDragType}, or {@code null} if not set
     */
    @SuppressWarnings("unchecked")
    public static HeaderDragType getDragType(Node child) {
        if (!child.hasProperties()) {
            return null;
        }

        return child.getProperties().get(HeaderDragType.class) instanceof ObjectProperty<?> property
            ? ((ObjectProperty<HeaderDragType>)property).get()
            : null;
    }

    /**
     * Specifies the {@link HeaderDragType} of the child, indicating whether it is a draggable part
     * of the {@code HeaderBar}. A value of {@code null} indicates that the drag type is not set.
     *
     * @param child the child node
     * @return the {@code dragType} property
     * @defaultValue {@code null}
     */
    @SuppressWarnings("unchecked")
    public static ObjectProperty<HeaderDragType> dragTypeProperty(Node child) {
        if (child.getProperties().get(HeaderDragType.class) instanceof ObjectProperty<?> property) {
            return (ObjectProperty<HeaderDragType>)property;
        }

        var property = new SimpleObjectProperty<HeaderDragType>(child, "dragType");
        child.getProperties().put(HeaderDragType.class, property);
        return property;
    }

    /**
     * Sets the value of the {@link #buttonTypeProperty(Node) buttonType}
     * attached property for the specified child.
     *
     * @param child the child node
     * @param value the {@code HeaderButtonType}, or {@code null}
     */
    public static void setButtonType(Node child, HeaderButtonType value) {
        if (getButtonType(child) != value) {
            buttonTypeProperty(child).set(value);
        }
    }

    /**
     * Gets the value of the {@link #buttonTypeProperty(Node) buttonType}
     * attached property for the specified child.
     *
     * @param child the child node
     * @return the {@code HeaderButtonType}, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static HeaderButtonType getButtonType(Node child) {
        if (!child.hasProperties()) {
            return null;
        }

        return child.getProperties().get(HeaderButtonType.class) instanceof ObjectProperty<?> property
            ? ((ObjectProperty<HeaderButtonType>)property).get()
            : null;
    }

    /**
     * Specifies the {@link HeaderButtonType} of the child, indicating its semantic use in the header bar.
     * <p>
     * This property can be set on any {@link Node}. Specifying a {@code HeaderButtonType} also provides the
     * behavior associated with the button type. If the default behavior is not desired, applications can
     * register an event filter on the child node that consumes the {@link MouseEvent#MOUSE_RELEASED} event.
     *
     * @param child the child node
     * @return the {@code buttonType} property
     * @defaultValue {@code null}
     */
    @SuppressWarnings("unchecked")
    public static ObjectProperty<HeaderButtonType> buttonTypeProperty(Node child) {
        if (child.getProperties().get(HeaderButtonType.class) instanceof ObjectProperty<?> property) {
            return (ObjectProperty<HeaderButtonType>)property;
        }

        var property = new SimpleObjectProperty<HeaderButtonType>(child, "buttonType") {
            HeaderButtonBehavior behavior;

            @Override
            protected void invalidated() {
                HeaderButtonType type = get();

                if (behavior != null) {
                    behavior.dispose();
                }

                if (type != null) {
                    behavior = new HeaderButtonBehavior((Node)getBean(), type);
                }
            }
        };

        child.getProperties().put(HeaderButtonType.class, property);
        return property;
    }

    /**
     * Sets the value of the {@link #systemColorSchemeProperty(Stage) systemColorScheme}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @param value the color scheme, or {@code null} to indicate no preference
     */
    public static void setSystemColorScheme(Stage stage, ColorScheme value) {
        StageHelper.getExtendedProperties(stage).systemColorSchemeProperty().set(value);
    }

    /**
     * Gets the value of the {@link #systemColorSchemeProperty(Stage) systemColorScheme}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @return the color scheme
     */
    public static ColorScheme getSystemColorScheme(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemColorSchemeProperty().get();
    }

    /**
     * Specifies the color scheme of the system-provided header buttons for the specified {@code Stage}.
     * <p>
     * This is a <em>null-coalescing</em> property: if set to {@code null} (using the setter method,
     * {@link Property#setValue(Object)}, or with a binding), the property evaluates to the value of
     * {@link Scene.Preferences#colorSchemeProperty()}. Likewise, specifying a non-null value will
     * override the scene-provided value. Overriding the scene-provided color scheme is usually only
     * necessary in the rare case when an application needs different color schemes for header buttons
     * and the window content (bright title bar in dark mode, or a dark title bar in light mode).
     * <p>
     * The specified color scheme is only a hint for the platform window toolkit and may be ignored.
     *
     * @param stage the {@code Stage}
     * @return the {@code systemColorScheme} attached property
     * @defaultValue {@link Scene.Preferences#getColorScheme()}
     * @see Scene.Preferences#colorSchemeProperty()
     */
    public static ObjectProperty<ColorScheme> systemColorSchemeProperty(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemColorSchemeProperty();
    }

    /**
     * Sentinel value that can be used for the {@link #systemButtonHeightProperty(Stage) systemButtonHeight}
     * attached property to indicate that the platform should choose the platform-specific default button height.
     */
    public static final double USE_DEFAULT_SIZE = -1;

    /**
     * Sets the value of the {@link #systemButtonHeightProperty(Stage) systemButtonHeight}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @param height the preferred height, or 0 to hide the system-provided header buttons
     */
    public static void setSystemButtonHeight(Stage stage, double height) {
        StageHelper.getExtendedProperties(stage).systemButtonHeightProperty().set(height);
    }

    /**
     * Gets the value of the {@link #systemButtonHeightProperty(Stage) systemButtonHeight}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @return the preferred height of the system-provided header buttons
     */
    public static double getSystemButtonHeight(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemButtonHeightProperty().get();
    }

    /**
     * Specifies the preferred height of the system-provided header buttons for the specified {@code Stage}.
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
     * @return the {@code systemButtonHeight} attached property
     * @defaultValue {@code USE_DEFAULT_SIZE}
     */
    public static DoubleProperty systemButtonHeightProperty(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemButtonHeightProperty();
    }

    /**
     * Gets the value of the {@link #systemMinHeightProperty(Stage) systemMinHeight}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @return the system-recommended minimum height for the {@code HeaderBar}
     */
    public static double getSystemMinHeight(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemMinHeightProperty().get();
    }

    /**
     * Specifies the system-recommended minimum height for the {@code HeaderBar} for the specified {@code Stage},
     * which usually corresponds to the height of the default header buttons. Applications can use this value
     * as a sensible lower limit for the height of the {@code HeaderBar}.
     * <p>
     * By default, {@link #minHeightProperty() minHeight} is set to the value of {@code systemMinHeight},
     * unless {@code minHeight} is explicitly set by a stylesheet or application code.
     *
     * @param stage the {@code Stage}
     * @return the {@code systemMinHeight} attached property
     */
    public static ReadOnlyDoubleProperty systemMinHeightProperty(Stage stage) {
        return StageHelper.getExtendedProperties(stage).systemMinHeightProperty();
    }

    /**
     * Describes the size of the left system-reserved inset for the specified {@code Stage}, which is an area
     * reserved for the iconify, maximize, and close window buttons. If there are no window buttons on the left
     * side of the window, the returned area is an empty {@code Dimension2D}.
     *
     * @param stage the {@code Stage}
     * @return the {@code leftSystemInset} attached property
     */
    public static ReadOnlyObjectProperty<Dimension2D> leftSystemInsetProperty(Stage stage) {
        return StageHelper.getExtendedProperties(stage).leftSystemInsetProperty();
    }

    /**
     * Gets the value of the {@link #leftSystemInsetProperty(Stage) leftSystemInset}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @return the size of the left system-reserved inset
     */
    public static Dimension2D getLeftSystemInset(Stage stage) {
        return StageHelper.getExtendedProperties(stage).leftSystemInsetProperty().get();
    }

    /**
     * Describes the size of the right system-reserved inset for the specified {@code Stage}, which is an area
     * reserved for the iconify, maximize, and close window buttons. If there are no window buttons on the right
     * side of the window, the returned area is an empty {@code Dimension2D}.
     *
     * @param stage the {@code Stage}
     * @return the {@code rightSystemInset} attached property
     */
    public static ReadOnlyObjectProperty<Dimension2D> rightSystemInsetProperty(Stage stage) {
        return StageHelper.getExtendedProperties(stage).rightSystemInsetProperty();
    }

    /**
     * Gets the value of the {@link #rightSystemInsetProperty(Stage) rightSystemInset}
     * attached property for the specified {@code Stage}.
     *
     * @param stage the {@code Stage}
     * @return the size of the right system-reserved inset
     */
    public static Dimension2D getRightSystemInset(Stage stage) {
        return StageHelper.getExtendedProperties(stage).rightSystemInsetProperty().get();
    }

    /**
     * Sets the alignment for the child when contained in a {@code HeaderBar}.
     * If set, it will override the header bar's default alignment for the child's position.
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

    private Subscription subscriptions = Subscription.EMPTY;

    /**
     * Creates a new {@code HeaderBar}.
     */
    public HeaderBar() {
        // Inflate the minHeight property. This is important so that we can track whether a stylesheet or
        // user code changes the property value before we set it to the height of the native title bar.
        minHeightProperty();

        sceneProperty()
            .flatMap(Scene::windowProperty)
            .map(w -> w instanceof Stage stage ? stage : null)
            .subscribe(this::onStageChanged);
    }

    /**
     * Creates a new {@code HeaderBar} with the specified children.
     *
     * @param left the left node, or {@code null}
     * @param center the center node, or {@code null}
     * @param right the right node, or {@code null}
     */
    public HeaderBar(Node left, Node center, Node right) {
        this();
        setLeft(left);
        setCenter(center);
        setRight(right);
    }

    /**
     * The left area of the {@code HeaderBar}.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Node> left;

    public final ObjectProperty<Node> leftProperty() {
        if (left == null) {
            left = new NodeProperty("left");
        }

        return left;
    }

    public final Node getLeft() {
        return left != null ? left.get() : null;
    }

    public final void setLeft(Node value) {
        if (left != null || value != null) {
            leftProperty().set(value);
        }
    }

    /**
     * The center area of the {@code HeaderBar}.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Node> center;

    public final ObjectProperty<Node> centerProperty() {
        if (center == null) {
            center = new NodeProperty("center");
        }

        return center;
    }

    public final Node getCenter() {
        return center != null ? center.get() : null;
    }

    public final void setCenter(Node value) {
        if (center != null || value != null) {
            centerProperty().set(value);
        }
    }

    /**
     * The right area of the {@code HeaderBar}.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Node> right;

    public final ObjectProperty<Node> rightProperty() {
        if (right == null) {
            right = new NodeProperty("right");
        }

        return right;
    }

    public final Node getRight() {
        return right != null ? right.get() : null;
    }

    public final void setRight(Node value) {
        if (right != null || value != null) {
            rightProperty().set(value);
        }
    }

    /**
     * Specifies whether additional padding should be added to the left side of the {@code HeaderBar}.
     * The size of the additional padding corresponds to the size of the system-reserved area that contains
     * the default header buttons (iconify, maximize, and close). If the system-reserved area contains no
     * header buttons, no additional padding is added to the left side of the {@code HeaderBar}.
     * <p>
     * Applications that use a single {@code HeaderBar} extending the entire width of the window should
     * set this property to {@code true} to prevent the header buttons from overlapping the content of the
     * {@code HeaderBar}.
     *
     * @defaultValue {@code true}
     * @see #rightSystemPaddingProperty() rightSystemPadding
     */
    private BooleanProperty leftSystemPadding;

    public final BooleanProperty leftSystemPaddingProperty() {
        if (leftSystemPadding == null) {
            leftSystemPadding = new BooleanPropertyBase(true) {
                @Override
                public Object getBean() {
                    return HeaderBar.this;
                }

                @Override
                public String getName() {
                    return "leftSystemPadding";
                }

                @Override
                protected void invalidated() {
                    requestLayout();
                }
            };
        }

        return leftSystemPadding;
    }

    public final boolean isLeftSystemPadding() {
        return leftSystemPadding == null || leftSystemPadding.get();
    }

    public final void setLeftSystemPadding(boolean value) {
        if (leftSystemPadding != null || !value) {
            leftSystemPaddingProperty().set(value);
        }
    }

    /**
     * Specifies whether additional padding should be added to the right side of the {@code HeaderBar}.
     * The size of the additional padding corresponds to the size of the system-reserved area that contains
     * the default header buttons (iconify, maximize, and close). If the system-reserved area contains no
     * header buttons, no additional padding is added to the right side of the {@code HeaderBar}.
     * <p>
     * Applications that use a single {@code HeaderBar} extending the entire width of the window should
     * set this property to {@code true} to prevent the header buttons from overlapping the content of the
     * {@code HeaderBar}.
     *
     * @defaultValue {@code true}
     * @see #leftSystemPaddingProperty() leftSystemPadding
     */
    private BooleanProperty rightSystemPadding;

    public final BooleanProperty rightSystemPaddingProperty() {
        if (rightSystemPadding == null) {
            rightSystemPadding = new BooleanPropertyBase(true) {
                @Override
                public Object getBean() {
                    return HeaderBar.this;
                }

                @Override
                public String getName() {
                    return "rightSystemPadding";
                }

                @Override
                protected void invalidated() {
                    requestLayout();
                }
            };
        }

        return rightSystemPadding;
    }

    public final boolean isRightSystemPadding() {
        return rightSystemPadding == null || rightSystemPadding.get();
    }

    public final void setRightSystemPadding(boolean value) {
        if (rightSystemPadding != null || !value) {
            rightSystemPaddingProperty().set(value);
        }
    }

    @Override
    protected double computeMinWidth(double height) {
        Node left = getLeft();
        Node center = getCenter();
        Node right = getRight();
        Insets insets = getInsets();
        double leftPrefWidth;
        double rightPrefWidth;
        double centerMinWidth;
        double leftSystemPaddingWidth = 0;
        double rightSystemPaddingWidth = 0;

        if (height != -1
                && (childHasContentBias(left, Orientation.VERTICAL) ||
                    childHasContentBias(right, Orientation.VERTICAL) ||
                    childHasContentBias(center, Orientation.VERTICAL))) {
            double areaHeight = Math.max(0, height);
            leftPrefWidth = getAreaWidth(left, areaHeight, false);
            rightPrefWidth = getAreaWidth(right, areaHeight, false);
            centerMinWidth = getAreaWidth(center, areaHeight, true);
        } else {
            leftPrefWidth = getAreaWidth(left, -1, false);
            rightPrefWidth = getAreaWidth(right, -1, false);
            centerMinWidth = getAreaWidth(center, -1, true);
        }

        Scene scene = getScene();
        Stage stage = scene != null
            ? scene.getWindow() instanceof Stage s ? s : null
            : null;

        if (stage != null) {
            var properties = StageHelper.getExtendedProperties(stage);

            if (scene.getEffectiveNodeOrientation() != getEffectiveNodeOrientation()) {
                leftSystemPaddingWidth = isLeftSystemPadding() ? properties.rightSystemInsetProperty().get().getWidth() : 0;
                rightSystemPaddingWidth = isRightSystemPadding() ? properties.leftSystemInsetProperty().get().getWidth() : 0;
            } else {
                leftSystemPaddingWidth = isLeftSystemPadding() ? properties.leftSystemInsetProperty().get().getWidth() : 0;
                rightSystemPaddingWidth = isRightSystemPadding() ? properties.rightSystemInsetProperty().get().getWidth() : 0;
            }
        }

        return insets.getLeft()
             + leftPrefWidth
             + centerMinWidth
             + rightPrefWidth
             + insets.getRight()
             + leftSystemPaddingWidth
             + rightSystemPaddingWidth;
    }

    @Override
    protected double computeMinHeight(double width) {
        Node left = getLeft();
        Node center = getCenter();
        Node right = getRight();
        Insets insets = getInsets();
        double leftMinHeight = getAreaHeight(left, -1, true);
        double rightMinHeight = getAreaHeight(right, -1, true);
        double centerMinHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(left, -1, false);
            double rightPrefWidth = getAreaWidth(right, -1, false);
            centerMinHeight = getAreaHeight(center, Math.max(0, width - leftPrefWidth - rightPrefWidth), true);
        } else {
            centerMinHeight = getAreaHeight(center, -1, true);
        }

        return insets.getTop()
             + insets.getBottom()
             + Math.max(centerMinHeight, Math.max(rightMinHeight, leftMinHeight));
    }

    @Override
    protected double computePrefHeight(double width) {
        Node left = getLeft();
        Node center = getCenter();
        Node right = getRight();
        Insets insets = getInsets();
        double leftPrefHeight = getAreaHeight(left, -1, false);
        double rightPrefHeight = getAreaHeight(right, -1, false);
        double centerPrefHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(left, -1, false);
            double rightPrefWidth = getAreaWidth(right, -1, false);
            centerPrefHeight = getAreaHeight(center, Math.max(0, width - leftPrefWidth - rightPrefWidth), false);
        } else {
            centerPrefHeight = getAreaHeight(center, -1, false);
        }

        return insets.getTop()
             + insets.getBottom()
             + Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));
    }

    @Override
    protected void layoutChildren() {
        Node left = getLeft();
        Node center = getCenter();
        Node right = getRight();
        Insets insets = getInsets();
        double width = Math.max(getWidth(), minWidth(-1));
        double height = Math.max(getHeight(), minHeight(-1));
        double leftWidth = 0;
        double rightWidth = 0;
        double insideY = insets.getTop();
        double insideHeight = height - insideY - insets.getBottom();
        double rightSystemPaddingWidth = 0;
        double leftSystemPaddingWidth = 0;

        Scene scene = getScene();
        Stage stage = scene != null
            ? scene.getWindow() instanceof Stage s ? s : null
            : null;

        if (stage != null) {
            var properties = StageHelper.getExtendedProperties(stage);

            if (scene.getEffectiveNodeOrientation() != getEffectiveNodeOrientation()) {
                leftSystemPaddingWidth = isLeftSystemPadding() ? properties.rightSystemInsetProperty().get().getWidth() : 0;
                rightSystemPaddingWidth = isRightSystemPadding() ? properties.leftSystemInsetProperty().get().getWidth() : 0;
            } else {
                leftSystemPaddingWidth = isLeftSystemPadding() ? properties.leftSystemInsetProperty().get().getWidth() : 0;
                rightSystemPaddingWidth = isRightSystemPadding() ? properties.rightSystemInsetProperty().get().getWidth() : 0;
            }
        }

        double insideX = insets.getLeft() + leftSystemPaddingWidth;
        double insideWidth = width - insideX - insets.getRight() - rightSystemPaddingWidth;

        if (left != null && left.isManaged()) {
            Insets leftMargin = getNodeMargin(left);
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
            Insets rightMargin = getNodeMargin(right);
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
            Insets centerMargin = getNodeMargin(center);
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

    private void onStageChanged(Stage stage) {
        subscriptions.unsubscribe();

        if (stage != null) {
            var properties = StageHelper.getExtendedProperties(stage);

            subscriptions = Subscription.combine(
                properties.systemMinHeightProperty().subscribe(height -> {
                    var minHeight = (StyleableDoubleProperty)minHeightProperty();

                    // Only change minHeight if it was not set by a stylesheet or application code.
                    if (minHeight.getStyleOrigin() == null) {
                        minHeight.applyStyle(null, height);
                    }
                }),
                properties.subscribeLayoutInvalidated(this::requestLayout)
            );
        }
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
