/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui;

import com.sun.glass.events.MouseEvent;
import com.sun.javafx.binding.ObjectConstant;
import com.sun.javafx.util.Utils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableIntegerProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.HPos;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Subscription;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contains the visuals and behaviors for the minimize/maximize/close buttons on an {@link StageStyle#EXTENDED}
 * window for platforms that use client-side decorations (Windows and Linux/GTK). This control supports
 * left-to-right and right-to-left orientations, as well as a customizable layout order of buttons.
 *
 * <h2>Substructure</h2>
 * <ul>
 *     <li>{@link Region} — {@code window-button-container}
 *     <ul>
 *         <li>{@link Region} — {@code window-button}, {@code minimize-button}
 *         <li>{@link Region} — {@code window-button}, {@code maximize-button}
 *         <li>{@link Region} — {@code window-button}, {@code close-button}
 *     </ul>
 * </ul>
 *
 * <table style="white-space: nowrap">
 *     <caption>CSS properties of {@code window-button-container}</caption>
 *     <thead>
 *         <tr><th>CSS property</th><th>Values</th><th>Default</th><th>Comment</th></tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <th>-fx-button-placement</th><td>[ left | right ]</td><td>right</td>
 *             <td style="white-space: break-line">
 *                 Specifies the placement of the window buttons on the left or the right side of the window.
 *             </td>
 *         </tr>
 *         <tr>
 *             <th>-fx-allow-rtl</th><td>&lt;boolean&gt;</td><td>true</td>
 *             <td style="white-space: break-line">
 *                 Specifies whether the minimize/maximize/close buttons support right-to-left orientations.
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <table style="white-space: nowrap">
 *     <caption>CSS properties of {@code minimize-button}, {@code maximize-button}, {@code close-button}</caption>
 *     <thead>
 *         <tr><th>CSS property</th><th>Values</th><th>Default</th><th>Comment</th></tr>
 *     </thead>
 *     <tbody><tr>
 *         <th>-fx-button-order</th><td>&lt;integer&gt;</td><td>0/1/2</td>
 *         <td style="white-space: break-line">
 *             Specifies the layout order of a button relative to the other buttons.
 *             Lower values are laid out before higher values.</td>
 *     </tr></tbody>
 * </table>
 *
 * <table style="white-space: nowrap">
 *     <caption>Conditional style classes of window buttons</caption>
 *     <thead>
 *         <tr style="min-width: 200"><th>Style class</th><th>Applies to</th><th>Comment</th></tr>
 *     </thead>
 *     <tbody>
 *         <tr><th>.dark</th>
 *             <td>all buttons</td>
 *             <td style="white-space: break-line">
 *                 This style class will be present if the brightness of {@link Scene#fillProperty()}
 *                 as determined by {@link Utils#calculateAverageBrightness(Paint)} is less than 0.5
 *             </td>
 *         </tr>
 *         <tr><th>.restore</th><th>{@code maximize-button}</th><td style="white-space: break-line">
 *             This style class will be present if {@link Stage#isMaximized()} is {@code true}</td>
 *         </tr>
 *     </tbody>
 * </table>
 */
public final class WindowControlsOverlay extends Region {

    private static final CssMetaData<WindowControlsOverlay, HorizontalDirection> BUTTON_PLACEMENT_METADATA =
        new CssMetaData<>("-fx-button-placement",
                StyleConverter.getEnumConverter(HorizontalDirection.class),
                HorizontalDirection.RIGHT) {
            @Override
            public boolean isSettable(WindowControlsOverlay overlay) {
                return true;
            }

            @Override
            public StyleableProperty<HorizontalDirection> getStyleableProperty(WindowControlsOverlay overlay) {
                return overlay.buttonPlacement;
            }
        };

    private static final CssMetaData<WindowControlsOverlay, Boolean> ALLOW_RTL_METADATA =
        new CssMetaData<>("-fx-allow-rtl", StyleConverter.getBooleanConverter(), true) {
            @Override
            public boolean isSettable(WindowControlsOverlay overlay) {
                return true;
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(WindowControlsOverlay overlay) {
                return overlay.allowRtl;
            }
        };

    private static final List<CssMetaData<?, ?>> METADATA =
        Stream.concat(getClassCssMetaData().stream(),
        Stream.of(BUTTON_PLACEMENT_METADATA, ALLOW_RTL_METADATA)).toList();

    private static final PseudoClass HOVER_PSEUDOCLASS = PseudoClass.getPseudoClass("hover");
    private static final PseudoClass PRESSED_PSEUDOCLASS = PseudoClass.getPseudoClass("pressed");
    private static final PseudoClass ACTIVE_PSEUDOCLASS = PseudoClass.getPseudoClass("active");
    private static final String DARK_STYLE_CLASS = "dark";
    private static final String RESTORE_STYLE_CLASS = "restore";
    private static final String UTILITY_STYLE_CLASS = "utility";

    /**
     * The metrics (placement and size) of the window buttons.
     */
    private final ObjectProperty<WindowOverlayMetrics> metrics = new SimpleObjectProperty<>(
        this, "metrics", new WindowOverlayMetrics(HorizontalDirection.RIGHT, new Dimension2D(0, 0), 0));

    /**
     * Specifies the placement of the window buttons on the left or the right side of the window.
     * <p>
     * This property corresponds to the {@code -fx-button-placement} CSS property.
     */
    private final StyleableObjectProperty<HorizontalDirection> buttonPlacement =
        new SimpleStyleableObjectProperty<>(
                BUTTON_PLACEMENT_METADATA, this, "buttonPlacement", HorizontalDirection.RIGHT) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    /**
     * Specifies whether the minimize/maximize/close buttons support right-to-left orientations.
     * <p>
     * If this property is {@code true} and the effective node orientation is right-to-left, the
     * window buttons are mirrored to the other side of the window.
     * <p>
     * This property corresponds to the {@code -fx-allow-rtl} CSS property.
     */
    private final StyleableBooleanProperty allowRtl =
        new SimpleStyleableBooleanProperty(ALLOW_RTL_METADATA, this, "allowRtl", true) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    /**
     * Contains the buttons in the order as they will appear on the window.
     * This list is automatically updated by the implementation of {@link ButtonRegion#buttonOrder}.
     */
    private final List<ButtonRegion> orderedButtons = new ArrayList<>(3);
    private final ButtonRegion minimizeButton = new ButtonRegion(ButtonType.MINIMIZE, "minimize-button", 0);
    private final ButtonRegion maximizeButton = new ButtonRegion(ButtonType.MAXIMIZE, "maximize-button", 1);
    private final ButtonRegion closeButton = new ButtonRegion(ButtonType.CLOSE, "close-button", 2);
    private final Subscription subscriptions;
    private final boolean utility;

    private Node buttonAtMouseDown;

    public WindowControlsOverlay(ObservableValue<String> stylesheet, boolean utility) {
        this.utility = utility;

        var stage = sceneProperty()
            .flatMap(Scene::windowProperty)
            .map(w -> w instanceof Stage ? (Stage)w : null);

        var focusedSubscription = stage
            .flatMap(Stage::focusedProperty)
            .orElse(true)
            .subscribe(this::onFocusedChanged);

        var resizableSubscription = stage
            .flatMap(Stage::resizableProperty)
            .orElse(true)
            .subscribe(this::onResizableChanged);

        var maximizedSubscription = stage
            .flatMap(Stage::maximizedProperty)
            .orElse(false)
            .subscribe(this::onMaximizedChanged);

        var updateStylesheetSubscription = sceneProperty()
            .flatMap(Scene::fillProperty)
            .map(this::isDarkBackground)
            .orElse(false)
            .subscribe(x -> updateStyleClass()); // use a value subscriber, not an invalidation subscriber

        subscriptions = Subscription.combine(
            focusedSubscription,
            resizableSubscription,
            maximizedSubscription,
            updateStylesheetSubscription,
            stylesheet.subscribe(this::updateStylesheet));

        getStyleClass().setAll("window-button-container");

        if (utility) {
            minimizeButton.managedProperty().bind(ObjectConstant.valueOf(false));
            maximizeButton.managedProperty().bind(ObjectConstant.valueOf(false));
            getChildren().add(closeButton);
            getStyleClass().add(UTILITY_STYLE_CLASS);
        } else {
            getChildren().addAll(minimizeButton, maximizeButton, closeButton);
        }
    }

    public void dispose() {
        subscriptions.unsubscribe();
    }

    public ReadOnlyObjectProperty<WindowOverlayMetrics> metricsProperty() {
        return metrics;
    }

    /**
     * Classifies and returns the button type at the specified coordinate, or returns
     * {@code null} if the specified coordinate does not intersect a button.
     *
     * @param x the X coordinate, in pixels relative to the window
     * @param y the Y coordinate, in pixels relative to the window
     * @return the {@code ButtonType} or {@code null}
     */
    public ButtonType buttonAt(double x, double y) {
        if (!utility) {
            for (var button : orderedButtons) {
                if (button.isVisible() && button.getBoundsInParent().contains(x, y)) {
                    return button.getButtonType();
                }
            }
        } else if (closeButton.isVisible() && closeButton.getBoundsInParent().contains(x, y)) {
            return ButtonType.CLOSE;
        }

        return null;
    }

    /**
     * Handles the specified mouse event.
     *
     * @param type the event type
     * @param button the button type
     * @param x the X coordinate, in pixels relative to the window
     * @param y the Y coordinate, in pixels relative to the window
     * @return {@code true} if the event was handled, {@code false} otherwise
     */
    public boolean handleMouseEvent(int type, int button, double x, double y) {
        ButtonType buttonType = buttonAt(x, y);
        Node node = buttonType != null ? switch (buttonType) {
            case MINIMIZE -> minimizeButton;
            case MAXIMIZE -> maximizeButton;
            case CLOSE -> closeButton;
        } : null;

        if (type == MouseEvent.NC_ENTER || type == MouseEvent.NC_MOVE || type == MouseEvent.NC_DRAG) {
            handleMouseOver(node);
        } else if (type == MouseEvent.NC_EXIT) {
            handleMouseExit();
        } else if (type == MouseEvent.NC_UP && button == MouseEvent.BUTTON_LEFT) {
            handleMouseUp(node, buttonType);
        } else if (node != null && type == MouseEvent.NC_DOWN && button == MouseEvent.BUTTON_LEFT) {
            handleMouseDown(node);
        }

        return node != null || buttonAtMouseDown != null;
    }

    private void handleMouseOver(Node button) {
        minimizeButton.pseudoClassStateChanged(HOVER_PSEUDOCLASS, button == minimizeButton);
        maximizeButton.pseudoClassStateChanged(HOVER_PSEUDOCLASS, button == maximizeButton);
        closeButton.pseudoClassStateChanged(HOVER_PSEUDOCLASS, button == closeButton);

        if (buttonAtMouseDown != null && buttonAtMouseDown != button) {
            buttonAtMouseDown.pseudoClassStateChanged(PRESSED_PSEUDOCLASS, false);
        }
    }

    private void handleMouseExit() {
        buttonAtMouseDown = null;

        for (var node : new Node[] {minimizeButton, maximizeButton, closeButton}) {
            node.pseudoClassStateChanged(HOVER_PSEUDOCLASS, false);
            node.pseudoClassStateChanged(PRESSED_PSEUDOCLASS, false);
        }
    }

    private void handleMouseDown(Node node) {
        buttonAtMouseDown = node;

        if (!node.isDisabled()) {
            node.pseudoClassStateChanged(PRESSED_PSEUDOCLASS, true);
        }
    }

    private void handleMouseUp(Node node, ButtonType buttonType) {
        boolean releasedOnButton = (buttonAtMouseDown == node);
        buttonAtMouseDown = null;
        Scene scene = getScene();

        if (node == null || node.isDisabled()
                || scene == null || !(scene.getWindow() instanceof Stage stage)) {
            return;
        }

        node.pseudoClassStateChanged(PRESSED_PSEUDOCLASS, false);

        if (releasedOnButton) {
            switch (buttonType) {
                case MINIMIZE -> stage.setIconified(true);
                case MAXIMIZE -> stage.setMaximized(!stage.isMaximized());
                case CLOSE -> stage.close();
            }
        }
    }

    private void onFocusedChanged(boolean focused) {
        minimizeButton.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS, focused);
        maximizeButton.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS, focused);
        closeButton.pseudoClassStateChanged(ACTIVE_PSEUDOCLASS, focused);
    }

    private void onResizableChanged(boolean resizable) {
        maximizeButton.setDisable(!resizable);
    }

    private void onMaximizedChanged(boolean maximized) {
        toggleStyleClass(maximizeButton, RESTORE_STYLE_CLASS, maximized);
    }

    private void updateStyleClass() {
        boolean darkScene = isDarkBackground(getScene() != null ? getScene().getFill() : null);
        toggleStyleClass(minimizeButton, DARK_STYLE_CLASS, darkScene);
        toggleStyleClass(maximizeButton, DARK_STYLE_CLASS, darkScene);
        toggleStyleClass(closeButton, DARK_STYLE_CLASS, darkScene);
    }

    private void updateStylesheet(String stylesheet) {
        getStylesheets().setAll(stylesheet);
    }

    private void toggleStyleClass(Node node, String styleClass, boolean enabled) {
        if (enabled && !node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        } else if (!enabled) {
            node.getStyleClass().remove(styleClass);
        }
    }

    private boolean isDarkBackground(Paint paint) {
        return paint != null && Utils.calculateAverageBrightness(paint) < 0.5;
    }

    @Override
    protected void layoutChildren() {
        boolean left;
        Node button1, button2, button3;

        if (allowRtl.get() && getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
            button1 = orderedButtons.get(2);
            button2 = orderedButtons.get(1);
            button3 = orderedButtons.get(0);
            left = buttonPlacement.get() != HorizontalDirection.LEFT;
        } else {
            button1 = orderedButtons.get(0);
            button2 = orderedButtons.get(1);
            button3 = orderedButtons.get(2);
            left = buttonPlacement.get() == HorizontalDirection.LEFT;
        }

        double width = getWidth();
        double button1Width = snapSizeX(boundedWidth(button1));
        double button2Width = snapSizeX(boundedWidth(button2));
        double button3Width = snapSizeX(boundedWidth(button3));
        double button1Height = snapSizeY(boundedHeight(button1));
        double button2Height = snapSizeY(boundedHeight(button2));
        double button3Height = snapSizeY(boundedHeight(button3));
        double button1X = snapPositionX(left ? 0 : width - button1Width - button2Width - button3Width);
        double button2X = snapPositionX(left ? button1Width : width - button3Width - button2Width);
        double button3X = snapPositionX(left ? button1Width + button2Width : width - button3Width);
        double totalWidth = snapSizeX(button1Width + button2Width + button3Width);
        double totalHeight = snapSizeY(Math.max(button1Height, Math.max(button2Height, button3Height)));
        Dimension2D currentSize = metrics.get().size();

        // Update the overlay metrics if they have changed.
        if (currentSize.getWidth() != totalWidth || currentSize.getHeight() != totalHeight) {
            var newMetrics = new WindowOverlayMetrics(
                left ? HorizontalDirection.LEFT : HorizontalDirection.RIGHT,
                new Dimension2D(totalWidth, totalHeight), totalHeight);

            metrics.set(newMetrics);
        }

        layoutInArea(button1, button1X, 0, button1Width, button1Height, BASELINE_OFFSET_SAME_AS_HEIGHT,
                     Insets.EMPTY, true, true, HPos.LEFT, VPos.TOP, false);

        layoutInArea(button2, button2X, 0, button2Width, button2Height, BASELINE_OFFSET_SAME_AS_HEIGHT,
                     Insets.EMPTY, true, true, HPos.LEFT, VPos.TOP, false);

        layoutInArea(button3, button3X, 0, button3Width, button3Height, BASELINE_OFFSET_SAME_AS_HEIGHT,
                     Insets.EMPTY, true, true, HPos.LEFT, VPos.TOP, false);
    }

    @Override
    public boolean usesMirroring() {
        return false;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return METADATA;
    }

    private static double boundedWidth(Node node) {
        return node.isManaged() ? boundedSize(node.minWidth(-1), node.prefWidth(-1), node.maxWidth(-1)) : 0;
    }

    private static double boundedHeight(Node node) {
        return node.isManaged() ? boundedSize(node.minHeight(-1), node.prefHeight(-1), node.maxHeight(-1)) : 0;
    }

    private static double boundedSize(double min, double pref, double max) {
        return Math.min(Math.max(pref, min), Math.max(min, max));
    }

    public enum ButtonType {
        MINIMIZE,
        MAXIMIZE,
        CLOSE
    }

    private class ButtonRegion extends Region {

        private static final CssMetaData<ButtonRegion, Number> BUTTON_ORDER_METADATA =
            new CssMetaData<>("-fx-button-order", StyleConverter.getSizeConverter()) {
                @Override
                public boolean isSettable(ButtonRegion node) {
                    return true;
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(ButtonRegion region) {
                    return region.buttonOrder;
                }
            };

        private static final List<CssMetaData<?, ?>> METADATA =
            Stream.concat(getClassCssMetaData().stream(), Stream.of(BUTTON_ORDER_METADATA)).toList();

        /**
         * Specifies the layout order of this button relative to the other buttons.
         * Buttons with a lower value are laid out before buttons with a higher value.
         * <p>
         * This property corresponds to the {@code -fx-button-order} CSS property.
         */
        private final StyleableIntegerProperty buttonOrder =
            new SimpleStyleableIntegerProperty(BUTTON_ORDER_METADATA, this, "buttonOrder") {
                @Override
                protected void invalidated() {
                    requestParentLayout();

                    WindowControlsOverlay.this.orderedButtons.sort(
                        Comparator.comparing(ButtonRegion::getButtonOrder));
                }
            };

        private final Region glyph = new Region();
        private final ButtonType type;

        ButtonRegion(ButtonType type, String styleClass, int order) {
            this.type = type;
            orderedButtons.add(this);
            buttonOrder.set(order);
            glyph.getStyleClass().setAll("glyph");
            getChildren().add(glyph);
            getStyleClass().setAll("window-button", styleClass);
        }

        public ButtonType getButtonType() {
            return type;
        }

        public int getButtonOrder() {
            return buttonOrder.get();
        }

        @Override
        protected void layoutChildren() {
            layoutInArea(glyph, 0, 0, getWidth(), getHeight(), 0, HPos.LEFT, VPos.TOP);
        }

        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
            return METADATA;
        }
    }
}
