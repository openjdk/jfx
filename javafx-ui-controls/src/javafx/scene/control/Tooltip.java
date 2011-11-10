/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import javafx.util.Duration;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

/**
 * Tooltips are common UI elements which are typically used for showing
 * additional information about a Control when the Control is hovered over by
 * the mouse. Any Control can show a tooltip, though by default most don't.
 * Tooltips in JavaFX are implemented by using Popups. In JavaFX, arbitrary
 * content can be embedded in a tooltip and just limited to a String.
 * <p>
 * The Tooltip has two different states: activated and visible. The Skin
 * observes these two states and reacts accordingly, e.g starts timers, shows
 * or hides the tooltip which may be implemented as a popup.
 * The example below shows how to create a tooltip for a Button control,
 *
 * <pre>
 * import javafx.scene.control.Tooltip;
 * import javafx.scene.control.Button;
 *
 * Button button = new Button("Hover Over Me");
 * button.setTooltip(new Tooltip("Tooltip for Button"));
 * </pre>
 *
 * You can also use tooltip with any node, though not quite as conveniently
 * as with controls.
 *
 * <pre>
 * Rectangle rect = new Rectangle(0, 0, 100, 100);
 * Tooltip t = new Tooltip("A Square");
 * Tooltip.install(rect, t);
 * </pre>
 *
 * This tooltip with then participate in the typical tooltip semantics. Note
 * that the Tooltip does not have to be uninstalled, it will be garbage
 * collected when no more nodes reference it, or when all nodes that reference
 * it are also garbage collected. It is possible to manually uninstall the
 * tooltip, however.
 *
 * A single tooltip can be installed on multiple target nodes or multiple
 * controls.
 */
public class Tooltip extends PopupControl {
//    private static TooltipBehavior BEHAVIOR = new TooltipBehavior(
//        new Duration(1000), new Duration(5000), new Duration(600), true);
    private static String TOOLTIP_PROP_KEY = "javafx.scene.control.Tooltip";
    private static TooltipBehavior BEHAVIOR = new TooltipBehavior(
        new Duration(1000), new Duration(5000), new Duration(200), false);

    /**
     * Associates the given {@link Tooltip} with the given {@link Node}. The tooltip
     * can then behave similar to when it is set on any {@link Control}. A single
     * tooltip can be associated with multiple nodes.
     * @see Tooltip
     */
    public static void install(Node node, Tooltip t) {
        BEHAVIOR.install(node, t);
    }

    /**
     * Removes the association of the given {@link Tooltip} on the specified
     * {@link Node}. Hence hovering on the node will no longer result in showing of the
     * tooltip.
     * @see Tooltip
     */
    public static void uninstall(Node node, Tooltip t) {
        BEHAVIOR.uninstall(node);
    }

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a tooltip with an empty string for its text.
     */
    public Tooltip() {
        initialize();
    }

    /**
     * Creates a tooltip with the specified text.
     *
     * @param text A text string for the tooltip.
     */
    public Tooltip(String text) {
        setText(text);
        initialize();
    }

    private void initialize() {
        getStyleClass().setAll("tooltip");
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The text to display in the tooltip. If the text is set to null, an empty
     * string will be displayed, despite the value being null.
     */
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    public final StringProperty textProperty() { return text; }
    public final void setText(String value) {
        if (isShowing() && value != null && !value.equals(getText())) {
            //Dynamic tooltip content is location-dependant.
            //Chromium trick.
            setX(BEHAVIOR.lastMouseX);
            setY(BEHAVIOR.lastMouseY);
        }
        textProperty().setValue(value);
    }
    public final String getText() { return text == null ? "" : text.getValue(); }

    /**
     * Specifies the behavior for lines of text <em>when text is multiline</em>
     * Unlike {@link #contentDisplay} which affects the graphic and text, this setting
     * only affects multiple lines of text relative to the text bounds.
     */
    @Styleable(property="-fx-text-alignment", initial="left")
    private ObjectProperty<TextAlignment> textAlignment;
    public final void setTextAlignment(TextAlignment value) { textAlignmentProperty().setValue(value); }
    public final TextAlignment getTextAlignment() { return textAlignment == null ? TextAlignment.LEFT : textAlignment.getValue(); }
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        if (textAlignment == null) {
            textAlignment = new ObjectPropertyBase<TextAlignment>(TextAlignment.LEFT) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.TEXT_ALIGNMENT);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "textAlignment";
                }
            };
        }
        return textAlignment;
    }

    /**
     * Specifies the behavior to use if the text of the {@code Tooltip}
     * exceeds the available space for rendering the text.
     */
    @Styleable(property="-fx-text-overrun", initial="ellipsis")
    private ObjectProperty<OverrunStyle> textOverrun;
    public final void setTextOverrun(OverrunStyle value) { textOverrunProperty().setValue(value); }
    public final OverrunStyle getTextOverrun() { return textOverrun == null ? OverrunStyle.ELLIPSIS : textOverrun.getValue(); }
    public final ObjectProperty<OverrunStyle> textOverrunProperty() {
        if (textOverrun == null) {
            textOverrun = new ObjectPropertyBase<OverrunStyle>(OverrunStyle.ELLIPSIS) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.TEXT_OVERRUN);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "textOverrun";
                }
            };
        }
        return textOverrun;
    }

    /**
     * If a run of text exceeds the width of the Tooltip, then this variable
     * indicates whether the text should wrap onto another line.
     */
    @Styleable(property="-fx-wrap-text", initial="false")
    private BooleanProperty wrapText;
    public final void setWrapText(boolean value) { wrapTextProperty().setValue(value); }
    public final boolean isWrapText() { return wrapText == null ? false : wrapText.getValue(); }
    public final BooleanProperty wrapTextProperty() {
        if (wrapText == null) {
            wrapText = new BooleanPropertyBase() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.WRAP_TEXT);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "wrapText";
                }
            };
        }
        return wrapText;
    }

    /**
     * The default font to use for text in the Tooltip. If the Tooltip's text is
     * rich text then this font may or may not be used depending on the font
     * information embedded in the rich text, but in any case where a default
     * font is required, this font will be used.
     */
    @Styleable(property="-fx-font", inherits=true)
    private ObjectProperty<Font> font;
    public final void setFont(Font value) { fontProperty().setValue(value); }
    public final Font getFont() { return font == null ? Font.getDefault() : font.getValue(); }
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new ObjectPropertyBase<Font>(Font.getDefault()) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.FONT);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "font";
                }
            };
        }
        return font;
    }

    /**
     * An optional icon for the Tooltip. This can be positioned relative to the
     * text by using the {@link #contentDisplay} property.
     * The node specified for this variable cannot appear elsewhere in the
     * scene graph, otherwise the {@code IllegalArgumentException} is thrown.
     * See the class description of {@link javafx.scene.Node Node} for more detail.
     */
    @Styleable(property="-fx-graphic", converter="com.sun.javafx.css.converters.URLConverter")
    private ObjectProperty<Node> graphic;
    public final void setGraphic(Node value) {
        graphicProperty().setValue(value);
        cachedImageUrl = null;
    }
    public final Node getGraphic() { return graphic == null ? null : graphic.getValue(); }
    public final ObjectProperty<Node> graphicProperty() {
        if (graphic == null) {
            graphic = new ObjectPropertyBase<Node>() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.GRAPHIC);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "graphic";
                }
            };
        }
        return graphic;
    }

    /**
     * Specifies the positioning of the graphic relative to the text.
     */
    @Styleable(property="-fx-content-display", initial="left")
    private ObjectProperty<ContentDisplay> contentDisplay;
    public final void setContentDisplay(ContentDisplay value) { contentDisplayProperty().setValue(value); }
    public final ContentDisplay getContentDisplay() { return contentDisplay == null ? ContentDisplay.LEFT : contentDisplay.getValue(); }
    public final ObjectProperty<ContentDisplay> contentDisplayProperty() {
        if (contentDisplay == null) {
            contentDisplay = new ObjectPropertyBase<ContentDisplay>(ContentDisplay.LEFT) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.CONTENT_DISPLAY);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "contentDisplay";
                }
            };
        }
        return contentDisplay;
    }

    /**
     * The amount of space between the graphic and text
     */
    @Styleable(property="-fx-graphic-text-gap", initial="4")
    private DoubleProperty graphicTextGap;
    public final void setGraphicTextGap(double value) { graphicTextGapProperty().setValue(value); }
    public final double getGraphicTextGap() { return graphicTextGap == null ? 4 : graphicTextGap.getValue(); }
    public final DoubleProperty graphicTextGapProperty() {
        if (graphicTextGap == null) {
            graphicTextGap = new DoublePropertyBase(4) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.GRAPHIC_TEXT_GAP);
                }

                @Override
                public Object getBean() {
                    return Tooltip.this;
                }

                @Override
                public String getName() {
                    return "graphicTextGap";
                }
            };
        }
        return graphicTextGap;
    }

    /**
     * Typically, the tooltip is "activated" when the mouse moves over a Control.
     * There is usually some delay between when the Tooltip becomes "activated"
     * and when it is actually shown. The details (such as the amount of delay, etc)
     * is left to the Skin implementation.
     */
    private final ReadOnlyBooleanWrapper activated = new ReadOnlyBooleanWrapper(this, "activated");
    final void setActivated(boolean value) { activated.set(value); }
    public final boolean isActivated() { return activated.get(); }
    public final ReadOnlyBooleanProperty activatedProperty() { return activated.getReadOnlyProperty(); }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {
        private static final StyleableProperty FONT = new StyleableProperty(Tooltip.class, "font", StyleableProperty.FONT.getSubProperties());
        private static final StyleableProperty TEXT_ALIGNMENT = new StyleableProperty(Tooltip.class, "textAlignment");
        private static final StyleableProperty TEXT_OVERRUN = new StyleableProperty(Tooltip.class, "textOverrun");
        private static final StyleableProperty WRAP_TEXT = new StyleableProperty(Tooltip.class, "wrapText");
        private static final StyleableProperty GRAPHIC = new StyleableProperty(Tooltip.class, "graphic");
        private static final StyleableProperty CONTENT_DISPLAY = new StyleableProperty(Tooltip.class, "contentDisplay");
        private static final StyleableProperty GRAPHIC_TEXT_GAP = new StyleableProperty(Tooltip.class, "graphicTextGap");
        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(PopupControl.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FONT,
                TEXT_ALIGNMENT,
                TEXT_OVERRUN,
                WRAP_TEXT,
                GRAPHIC,
                CONTENT_DISPLAY,
                GRAPHIC_TEXT_GAP
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

            bitIndices = new int[StyleableProperty.getMaxIndex()];
            java.util.Arrays.fill(bitIndices, -1);
            for(int bitIndex=0; bitIndex<STYLEABLES.size(); bitIndex++) {
                bitIndices[STYLEABLES.get(bitIndex).getIndex()] = bitIndex;
            }
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected int[] impl_cssStyleablePropertyBitIndices() {
        return Tooltip.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Tooltip.StyleableProperties.STYLEABLES;
    }

    private String cachedImageUrl = null;
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-text-alignment".equals(property)) {
            setTextAlignment((TextAlignment)value);
        } else if ("-fx-text-overrun".equals(property)) {
            setTextOverrun((OverrunStyle)value);
        } else if ("-fx-wrap-text".equals(property)) {
            setWrapText((Boolean) value);
        } else if ("-fx-font".equals(property)) {
            setFont((Font)value);
        } else if ("-fx-graphic".equals(property)) {
            String imageUrl = (String)value;
            if (imageUrl != null && !imageUrl.equals(cachedImageUrl)) {
                setGraphic(new ImageView(new Image(imageUrl)));
            }
            cachedImageUrl = imageUrl;
        } else if ("-fx-content-display".equals(property)) {
            setContentDisplay((ContentDisplay)value);
        } else if ("-fx-graphic-text-gap".equals(property)) {
            setGraphicTextGap((Double) value);
        }
        return super.impl_cssSet(property, value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-text-alignment".equals(property))
            return textAlignment == null || !textAlignment.isBound();
        else if ("-fx-text-overrun".equals(property))
            return textOverrun == null || !textOverrun.isBound();
        else if ("-fx-wrap-text".equals(property))
            return wrapText == null || !wrapText.isBound();
        else if ("-fx-font".equals(property))
            return font == null || !font.isBound();
        else if ("-fx-graphic".equals(property))
            return graphic == null || !graphic.isBound();
        else if ("-fx-content-display".equals(property))
            return contentDisplay == null || !contentDisplay.isBound();
        else if ("-fx-graphic-text-gap".equals(property))
            return graphicTextGap == null || !graphicTextGap.isBound();
        else
            return super.impl_cssSettable(property);
    }

    private static class TooltipBehavior {

        /*
         * There are two key concepts with Tooltip: activated and visible. A Tooltip
         * is activated as soon as a mouse move occurs over the target node. When it
         * becomes activated, we start off the ACTIVATION_TIMER. If the
         * ACTIVATION_TIMER expires before another mouse event occurs, then we will
         * show the popup. This timer typically lasts about 1 second.
         *
         * Once visible, we reset the ACTIVATION_TIMER and start the HIDE_TIMER.
         * This second timer will allow the tooltip to remain visible for some time
         * period (such as 5 seconds). If the mouse hasn't moved, and the HIDE_TIMER
         * expires, then the tooltip is hidden and the tooltip is no longer
         * activated.
         *
         * If another mouse move occurs, the ACTIVATION_TIMER starts again, and the
         * same rules apply as above.
         *
         * If a mouse exit event occurs while the HIDE_TIMER is ticking, we reset
         * the HIDE_TIMER. Thus, the tooltip disappears after 5 seconds from the
         * last mouse move.
         *
         * If some other mouse event occurs while the HIDE_TIMER is running, other
         * than mouse move or mouse enter/exit (such as a click), then the tooltip
         * is hidden, the HIDE_TIMER stopped, and activated set to false.
         *
         * If a mouse exit occurs while the HIDE_TIMER is running, we stop the
         * HIDE_TIMER and start the LEFT_TIMER, and immediately hide the tooltip.
         * This timer is very short, maybe about a 1/2 second. If the mouse enters a
         * new node which also has a tooltip before LEFT_TIMER expires, then the
         * second tooltip is activated and shown immediately (the ACTIVATION_TIMER
         * having been bypassed), and the HIDE_TIMER is started. If the LEFT_TIMER
         * expires and there is no mouse movement over a control with a tooltip,
         * then we are back to the initial steady state where the next mouse move
         * over a node with a tooltip installed will start the ACTIVATION_TIMER.
         */

        private Timeline activationTimer = new Timeline();
        private Timeline hideTimer = new Timeline();
        private Timeline leftTimer = new Timeline();

        /**
         * The Node with a tooltip over which the mouse is hovering. There can
         * only be one of these at a time.
         */
        private Node hoveredNode;

        /**
         * The tooltip that is currently activated. There can only be one
         * of these at a time.
         */
        private Tooltip activatedTooltip;

        /**
         * The tooltip that is currently visible. There can only be one
         * of these at a time.
         */
        private Tooltip visibleTooltip;

        /**
         * The last position of the mouse, in screen coordinates.
         */
        private double lastMouseX;
        private double lastMouseY;

        private boolean hideOnExit;

        TooltipBehavior(Duration openDelay, Duration visibleDuration, Duration closeDelay, final boolean hideOnExit) {
            this.hideOnExit = hideOnExit;

            activationTimer.getKeyFrames().add(new KeyFrame(openDelay));
            activationTimer.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    // Show the currently activated tooltip and start the
                    // HIDE_TIMER.
                    assert activatedTooltip != null;
                    final Window owner = getWindow(hoveredNode);
                    final boolean treeVisible = isWindowHierarchyVisible(hoveredNode);

                    // If the ACTIVATED tooltip is part of a visible window
                    // hierarchy, we can go ahead and show the tooltip and
                    // start the HIDE_TIMER.
                    //
                    // If the owner is null or invisible, then it either means a
                    // bug in our code, the node was removed from a scene or
                    // window or made invisible, or the node is not part of a
                    // visible window hierarchy. In that case, we don't show the
                    // tooltip, and we don't start the HIDE_TIMER. We simply let
                    // ACTIVATED_TIMER expire, and wait until the next mouse
                    // the movement to start it again.
                    if (owner != null && owner.isShowing() && treeVisible) {
                        activatedTooltip.show(owner, lastMouseX, lastMouseY);
                        visibleTooltip = activatedTooltip;
                        hoveredNode = null;
                        hideTimer.playFromStart();
                    }

                    // Once the activation timer has expired, the tooltip is no
                    // longer in the activated state, it is only in the visible
                    // state, so we go ahead and set activated to false
                    activatedTooltip.setActivated(false);
                    activatedTooltip = null;
                }
            });

            hideTimer.getKeyFrames().add(new KeyFrame(visibleDuration));
            hideTimer.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    // Hide the currently visible tooltip.
                    assert visibleTooltip != null;
                    visibleTooltip.hide();
                    visibleTooltip = null;
                    hoveredNode = null;
                }
            });

            leftTimer.getKeyFrames().add(new KeyFrame(closeDelay));
            leftTimer.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    if (!hideOnExit) {
                        // Hide the currently visible tooltip.
                        assert visibleTooltip != null;
                        visibleTooltip.hide();
                        visibleTooltip = null;
                        hoveredNode = null;
                    }
                }
            });
        }

        /**
         * Registers for mouse move events only. When the mouse is moved, this
         * handler will detect it and decide whether to start the ACTIVATION_TIMER
         * (if the ACTIVATION_TIMER is not started), restart the ACTIVATION_TIMER
         * (if ACTIVATION_TIMER is running), or skip the ACTIVATION_TIMER and just
         * show the tooltip (if the LEFT_TIMER is running).
         */
        private EventHandler<MouseEvent> MOVE_HANDLER = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                //Screen coordinates need to be actual for dynamic tooltip.
                //See Tooltip.setText

                // detect bogus mouse moved events, if it didn't really move then ignore it
                double newMouseX = event.getScreenX();
                double newMouseY = event.getScreenY();
                if (newMouseX == lastMouseX && newMouseY == lastMouseY) {
                    return;
                }
                lastMouseX = newMouseX;
                lastMouseY = newMouseY;

                // If the HIDE_TIMER is running, then we don't want this event
                // handler to do anything, or change any state at all.
                if (hideTimer.getStatus() == Timeline.Status.RUNNING) {
                    return;
                }

                // Note that the "install" step will both register this handler
                // with the target node and also associate the tooltip with the
                // target node, by stashing it in the client properties of the node.
                hoveredNode = (Node) event.getSource();
                Tooltip t = (Tooltip) hoveredNode.getProperties().get(TOOLTIP_PROP_KEY);
                if (t != null) {
                    // In theory we should never get here with an invisible or
                    // non-existant window hierarchy, but might in some cases where
                    // people are feeding fake mouse events into the hierarchy. So
                    // we'll guard against that case.
                    final Window owner = getWindow(hoveredNode);
                    final boolean treeVisible = isWindowHierarchyVisible(hoveredNode);
                    if (owner != null && treeVisible) {
                        // Now we know that the currently HOVERED node has a tooltip
                        // and that it is part of a visible window Hierarchy.
                        // If LEFT_TIMER is running, then we make this tooltip
                        // visible immediately, stop the LEFT_TIMER, and start the
                        // HIDE_TIMER.
                        if (leftTimer.getStatus() == Timeline.Status.RUNNING) {
                            if (visibleTooltip != null) visibleTooltip.hide();
                            visibleTooltip = t;
                            hoveredNode = null;
                            t.show(owner, event.getScreenX(), event.getScreenY());
                            leftTimer.stop();
                            hideTimer.playFromStart();
                        } else {
                            // Start / restart the timer and make sure the tooltip
                            // is marked as activated.
                            t.setActivated(true);
                            activatedTooltip = t;
                            activationTimer.stop();
                            activationTimer.playFromStart();
                        }
                    }
                } else {
                    // TODO should deregister, no point being here anymore!
                }
            }
        };

        /**
         * Registers for mouse exit events. If the ACTIVATION_TIMER is running then
         * this will simply stop it. If the HIDE_TIMER is running then this will
         * stop the HIDE_TIMER, hide the tooltip, and start the LEFT_TIMER.
         */
        private EventHandler<MouseEvent> LEAVING_HANDLER = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (activationTimer.getStatus() == Timeline.Status.RUNNING) {
                    activationTimer.stop();
                } else if (hideTimer.getStatus() == Timeline.Status.RUNNING) {
                    assert visibleTooltip != null;
                    hideTimer.stop();
                    if (hideOnExit) visibleTooltip.hide();
                    leftTimer.playFromStart();
                }

                hoveredNode = null;
                activatedTooltip = null;
                if (hideOnExit) visibleTooltip = null;
            }
        };

        /**
         * Registers for mouse click, press, release, drag events. If any of these
         * occur, then the tooltip is hidden (if it is visible), it is deactivated,
         * and any and all timers are stopped.
         */
        private EventHandler<MouseEvent> KILL_HANDLER = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                activationTimer.stop();
                hideTimer.stop();
                leftTimer.stop();
                if (visibleTooltip != null) visibleTooltip.hide();
                hoveredNode = null;
                activatedTooltip = null;
                visibleTooltip = null;
            }
        };

        private void install(Node node, Tooltip t) {
            // Install the MOVE_HANDLER, LEAVING_HANDLER, and KILL_HANDLER on
            // the given node. Stash the tooltip in the node's client properties
            // map so that it is not gc'd. The handlers must all be installed
            // with a TODO weak reference so as not to cause a memory leak
            if (node == null) return;
            node.addEventHandler(MouseEvent.MOUSE_MOVED, MOVE_HANDLER);
            node.addEventHandler(MouseEvent.MOUSE_EXITED, LEAVING_HANDLER);
            node.addEventHandler(MouseEvent.MOUSE_PRESSED, KILL_HANDLER);
            node.getProperties().put(TOOLTIP_PROP_KEY, t);
        }

        private void uninstall(Node node) {
            if (node == null) return;
            node.removeEventHandler(MouseEvent.MOUSE_MOVED, MOVE_HANDLER);
            node.removeEventHandler(MouseEvent.MOUSE_EXITED, LEAVING_HANDLER);
            node.removeEventHandler(MouseEvent.MOUSE_PRESSED, KILL_HANDLER);
            Tooltip t = (Tooltip)node.getProperties().get(TOOLTIP_PROP_KEY);
            if (t != null) {
                node.getProperties().remove(TOOLTIP_PROP_KEY);
                if (t.equals(visibleTooltip) || t.equals(activatedTooltip)) {
                    KILL_HANDLER.handle(null);
                }
            }
        }

        /**
         * Gets the top level window associated with this node.
         * @param node the node
         * @return the top level window
         */
        private Window getWindow(final Node node) {
            final Scene scene = node == null ? null : node.getScene();
            return scene == null ? null : scene.getWindow();
        }

        /**
         * Gets whether the entire window hierarchy is visible for this node.
         * @param node the node to check
         * @return true if entire hierarchy is visible
         */
        private boolean isWindowHierarchyVisible(Node node) {
            boolean treeVisible = node != null;
            Parent parent = node == null ? null : node.getParent();
            while (parent != null && treeVisible) {
                treeVisible = parent.isVisible();
                parent = parent.getParent();
            }
            return treeVisible;
        }

    }
}
