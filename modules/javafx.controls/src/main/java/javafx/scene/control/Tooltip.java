/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.IDProperty;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.stage.PopupWindowHelper;

import javafx.css.SimpleStyleableBooleanProperty;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleOrigin;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableStringProperty;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.converter.StringConverter;
import javafx.css.converter.DurationConverter;
import javafx.scene.control.skin.TooltipSkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.beans.value.WritableValue;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleRole;
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

/**
 * Tooltips are common UI elements which are typically used for showing
 * additional information about a Node in the scenegraph when the Node is
 * hovered over by the mouse. Any Node can show a tooltip. In most cases a
 * Tooltip is created and its {@link #textProperty() text} property is modified
 * to show plain text to the user. However, a Tooltip is able to show within it
 * an arbitrary scenegraph of nodes - this is done by creating the scenegraph
 * and setting it inside the Tooltip {@link #graphicProperty() graphic}
 * property.
 *
 * <p>You use the following approach to set a Tooltip on any node:
 *
 * <pre> Rectangle rect = new Rectangle(0, 0, 100, 100);
 * Tooltip t = new Tooltip("A Square");
 * Tooltip.install(rect, t);</pre>
 *
 * This tooltip will then participate with the typical tooltip semantics (i.e.
 * appearing on hover, etc). Note that the Tooltip does not have to be
 * uninstalled: it will be garbage collected when it is not referenced by any
 * Node. It is possible to manually uninstall the tooltip, however.
 *
 * <p>A single tooltip can be installed on multiple target nodes or multiple
 * controls.
 *
 * <p>Because most Tooltips are shown on UI controls, there is special API
 * for all controls to make installing a Tooltip less verbose. The example below
 * shows how to create a tooltip for a Button control:
 *
 * <pre> Button button = new Button("Hover Over Me");
 * button.setTooltip(new Tooltip("Tooltip for Button"));</pre>
 *
 * <img src="doc-files/Tooltip.png" alt="Image of the Tooltip control">
 *
 * @since JavaFX 2.0
 */
@IDProperty("id")
public class Tooltip extends PopupControl {
    private static String TOOLTIP_PROP_KEY = "javafx.scene.control.Tooltip";

    // RT-31134 : the tooltip style includes a shadow around the tooltip with a
    // width of 9 and height of 5. This causes mouse events to not reach the control
    // underneath resulting in losing hover state on the control while the tooltip is showing.
    // Displaying the tooltip at an offset indicated below resolves this issue.
    // RT-37107: The y-offset was upped to 7 to ensure no overlaps when the tooltip
    // is shown near the right edge of the screen.
    private static int TOOLTIP_XOFFSET = 10;
    private static int TOOLTIP_YOFFSET = 7;

    private static TooltipBehavior BEHAVIOR = new TooltipBehavior(false);

    /**
     * Associates the given {@link Tooltip} with the given {@link Node}. The tooltip
     * can then behave similar to when it is set on any {@link Control}. A single
     * tooltip can be associated with multiple nodes.
     * @param node the node
     * @param t the tooltip
     * @see Tooltip
     */
    public static void install(Node node, Tooltip t) {
        BEHAVIOR.install(node, t);
    }

    /**
     * Removes the association of the given {@link Tooltip} on the specified
     * {@link Node}. Hence hovering on the node will no longer result in showing of the
     * tooltip.
     * @param node the node
     * @param t the tooltip
     * @see Tooltip
     */
    public static void uninstall(Node node, Tooltip t) {
        BEHAVIOR.uninstall(node);
    }

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a tooltip with an empty string for its text.
     */
    public Tooltip() {
        this(null);
    }

    /**
     * Creates a tooltip with the specified text.
     *
     * @param text A text string for the tooltip.
     */
    public Tooltip(String text) {
        super();
        if (text != null) setText(text);
        bridge = new CSSBridge();
        PopupWindowHelper.getContent(this).setAll(bridge);
        getStyleClass().setAll("tooltip");
    }

    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The text to display in the tooltip. If the text is set to null, an empty
     * string will be displayed, despite the value being null.
     * @return the text property
     */
    public final StringProperty textProperty() { return text; }
    public final void setText(String value) {
        textProperty().setValue(value);
    }
    public final String getText() { return text.getValue() == null ? "" : text.getValue(); }
    private final StringProperty text = new SimpleStringProperty(this, "text", "") {
        @Override protected void invalidated() {
            super.invalidated();
            final String value = get();
            if (isShowing() && value != null && !value.equals(getText())) {
                //Dynamic tooltip content is location-dependant.
                //Chromium trick.
                setAnchorX(BEHAVIOR.lastMouseX);
                setAnchorY(BEHAVIOR.lastMouseY);
            }
        }
    };

    /**
     * Specifies the behavior for lines of text <em>when text is multiline</em>.
     * Unlike {@link #contentDisplayProperty() contentDisplay} which affects the
     * graphic and text, this setting only affects multiple lines of text
     * relative to the text bounds.
     * @return the text alignment property
     */
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        return textAlignment;
    }
    public final void setTextAlignment(TextAlignment value) {
        textAlignmentProperty().setValue(value);
    }
    public final TextAlignment getTextAlignment() {
        return textAlignmentProperty().getValue();
    }
    private final ObjectProperty<TextAlignment> textAlignment =
            new SimpleStyleableObjectProperty<>(TEXT_ALIGNMENT, this, "textAlignment", TextAlignment.LEFT);;

    /**
     * Specifies the behavior to use if the text of the {@code Tooltip}
     * exceeds the available space for rendering the text.
     * @return the text overrun property
     */
    public final ObjectProperty<OverrunStyle> textOverrunProperty() {
        return textOverrun;
    }
    public final void setTextOverrun(OverrunStyle value) {
        textOverrunProperty().setValue(value);
    }
    public final OverrunStyle getTextOverrun() {
        return textOverrunProperty().getValue();
    }
    private final ObjectProperty<OverrunStyle> textOverrun =
            new SimpleStyleableObjectProperty<OverrunStyle>(TEXT_OVERRUN, this, "textOverrun", OverrunStyle.ELLIPSIS);

    /**
     * If a run of text exceeds the width of the Tooltip, then this variable
     * indicates whether the text should wrap onto another line.
     * @return the wrap text property
     */
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }
    public final void setWrapText(boolean value) {
        wrapTextProperty().setValue(value);
    }
    public final boolean isWrapText() {
        return wrapTextProperty().getValue();
    }
    private final BooleanProperty wrapText =
            new SimpleStyleableBooleanProperty(WRAP_TEXT, this, "wrapText", false);


    /**
     * The default font to use for text in the Tooltip. If the Tooltip's text is
     * rich text then this font may or may not be used depending on the font
     * information embedded in the rich text, but in any case where a default
     * font is required, this font will be used.
     * @return the font property
     */
    public final ObjectProperty<Font> fontProperty() {
        return font;
    }
    public final void setFont(Font value) {
        fontProperty().setValue(value);
    }
    public final Font getFont() {
        return fontProperty().getValue();
    }
    private final ObjectProperty<Font> font = new StyleableObjectProperty<Font>(Font.getDefault()) {
        private boolean fontSetByCss = false;

        @Override public void applyStyle(StyleOrigin newOrigin, Font value) {
            // RT-20727 - if CSS is setting the font, then make sure invalidate doesn't call NodeHelper.reapplyCSS
            try {
                // super.applyStyle calls set which might throw if value is bound.
                // Have to make sure fontSetByCss is reset.
                fontSetByCss = true;
                super.applyStyle(newOrigin, value);
            } catch(Exception e) {
                throw e;
            } finally {
                fontSetByCss = false;
            }
        }

        @Override public void set(Font value) {
            final Font oldValue = get();
            StyleOrigin origin = ((StyleableObjectProperty<Font>)font).getStyleOrigin();
            if (origin == null || (value != null ? !value.equals(oldValue) : oldValue != null)) {
                super.set(value);
            }
        }

        @Override protected void invalidated() {
            // RT-20727 - if font is changed by calling setFont, then
            // css might need to be reapplied since font size affects
            // calculated values for styles with relative values
            if(fontSetByCss == false) {
                NodeHelper.reapplyCSS(Tooltip.this.bridge);
            }
        }

        @Override public CssMetaData<Tooltip.CSSBridge,Font> getCssMetaData() {
            return FONT;
        }

        @Override public Object getBean() {
            return Tooltip.this;
        }

        @Override public String getName() {
            return "font";
        }
    };


    /**
     * The delay between the mouse entering the hovered node and when the associated tooltip will be shown to the user.
     * The default delay is 1000ms.
     *
     * @return show delay property
     * @since 9
     * @defaultValue 1000ms
     */
    public final ObjectProperty<Duration> showDelayProperty() {
        return showDelayProperty;
    }
    public final void setShowDelay(Duration showDelay) {
        showDelayProperty.set(showDelay);
    }
    public final Duration getShowDelay() {
        return showDelayProperty.get();
    }
    private final ObjectProperty<Duration> showDelayProperty
            = new SimpleStyleableObjectProperty<>(SHOW_DELAY, this, "showDelay", new Duration(1000));


    /**
     * The duration that the tooltip should remain showing for until it is no longer visible to the user.
     * If the mouse leaves the control before the showDuration finishes, then the tooltip will remain showing
     * for the duration specified in the {@link #hideDelayProperty()}, even if the remaining time of the showDuration
     * is less than the hideDelay duration. The default value is 5000ms.
     *
     * @return the show duration property
     * @since 9
     * @defaultValue 5000ms
     */
    public final ObjectProperty<Duration> showDurationProperty() {
        return showDurationProperty;
    }
    public final void setShowDuration(Duration showDuration) {
        showDurationProperty.set(showDuration);
    }
    public final Duration getShowDuration() {
        return showDurationProperty.get();
    }
    private final ObjectProperty<Duration> showDurationProperty
            = new SimpleStyleableObjectProperty<>(SHOW_DURATION, this, "showDuration", new Duration(5000));


    /**
     * The duration in which to continue showing the tooltip after the mouse has left the node. Once this time has
     * elapsed the tooltip will hide. The default value is 200ms.
     *
     * @return the hide delay property
     * @since 9
     * @defaultValue 200ms
     */
    public final ObjectProperty<Duration> hideDelayProperty() {
        return hideDelayProperty;
    }
    public final void setHideDelay(Duration hideDelay) {
        hideDelayProperty.set(hideDelay);
    }
    public final Duration getHideDelay() {
        return hideDelayProperty.get();
    }
    private final ObjectProperty<Duration> hideDelayProperty
            = new SimpleStyleableObjectProperty<>(HIDE_DELAY, this, "hideDelay", new Duration(200));


    /**
     * An optional icon for the Tooltip. This can be positioned relative to the
     * text by using the {@link #contentDisplayProperty() content display}
     * property.
     * The node specified for this variable cannot appear elsewhere in the
     * scene graph, otherwise the {@code IllegalArgumentException} is thrown.
     * See the class description of {@link javafx.scene.Node Node} for more detail.
     * @return the graphic property
     */
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }
    public final void setGraphic(Node value) {
        graphicProperty().setValue(value);
    }
    public final Node getGraphic() {
        return graphicProperty().getValue();
    }
    private final ObjectProperty<Node> graphic = new StyleableObjectProperty<Node>() {
        // The graphic is styleable by css, but it is the
        // imageUrlProperty that handles the style value.
        @Override public CssMetaData getCssMetaData() {
            return GRAPHIC;
        }

        @Override public Object getBean() {
            return Tooltip.this;
        }

        @Override public String getName() {
            return "graphic";
        }

    };

    private StyleableStringProperty imageUrlProperty() {
        if (imageUrl == null) {
            imageUrl = new StyleableStringProperty() {
                // If imageUrlProperty is invalidated, this is the origin of the style that
                // triggered the invalidation. This is used in the invaildated() method where the
                // value of super.getStyleOrigin() is not valid until after the call to set(v) returns,
                // by which time invalidated will have been called.
                // This value is initialized to USER in case someone calls set on the imageUrlProperty, which
                // is possible:
                //     CssMetaData metaData = ((StyleableProperty)labeled.graphicProperty()).getCssMetaData();
                //     StyleableProperty prop = metaData.getStyleableProperty(labeled);
                //     prop.set(someUrl);
                //
                // TODO: Note that prop != labeled, which violates the contract between StyleableProperty and CssMetaData.
                StyleOrigin origin = StyleOrigin.USER;

                @Override public void applyStyle(StyleOrigin origin, String v) {

                    this.origin = origin;

                    // Don't want applyStyle to throw an exception which would leave this.origin set to the wrong value
                    if (graphic == null || graphic.isBound() == false) super.applyStyle(origin, v);

                    // Origin is only valid for this invocation of applyStyle, so reset it to USER in case someone calls set.
                    this.origin = StyleOrigin.USER;
                }

                @Override protected void invalidated() {

                    // need to call super.get() here since get() is overridden to return the graphicProperty's value
                    final String url = super.get();

                    if (url == null) {
                        ((StyleableProperty<Node>)(WritableValue<Node>)graphicProperty()).applyStyle(origin, null);
                    } else {
                        // RT-34466 - if graphic's url is the same as this property's value, then don't overwrite.
                        final Node graphicNode = Tooltip.this.getGraphic();
                        if (graphicNode instanceof ImageView) {
                            final ImageView imageView = (ImageView)graphicNode;
                            final Image image = imageView.getImage();
                            if (image != null) {
                                final String imageViewUrl = image.getUrl();
                                if (url.equals(imageViewUrl)) return;
                            }

                        }

                        final Image img = StyleManager.getInstance().getCachedImage(url);

                        if (img != null) {
                            // Note that it is tempting to try to re-use existing ImageView simply by setting
                            // the image on the current ImageView, if there is one. This would effectively change
                            // the image, but not the ImageView which means that no graphicProperty listeners would
                            // be notified. This is probably not what we want.

                            // Have to call applyStyle on graphicProperty so that the graphicProperty's
                            // origin matches the imageUrlProperty's origin.
                            ((StyleableProperty<Node>)(WritableValue<Node>)graphicProperty()).applyStyle(origin, new ImageView(img));
                        }
                    }
                }

                @Override public String get() {
                    // The value of the imageUrlProperty is that of the graphicProperty.
                    // Return the value in a way that doesn't expand the graphicProperty.
                    final Node graphic = getGraphic();
                    if (graphic instanceof ImageView) {
                        final Image image = ((ImageView)graphic).getImage();
                        if (image != null) {
                            return image.getUrl();
                        }
                    }
                    return null;
                }

                @Override public StyleOrigin getStyleOrigin() {
                    // The origin of the imageUrlProperty is that of the graphicProperty.
                    // Return the origin in a way that doesn't expand the graphicProperty.
                    return graphic != null ? ((StyleableProperty<Node>)(WritableValue<Node>)graphic).getStyleOrigin() : null;
                }

                @Override public Object getBean() {
                    return Tooltip.this;
                }

                @Override public String getName() {
                    return "imageUrl";
                }

                @Override public CssMetaData<Tooltip.CSSBridge,String> getCssMetaData() {
                    return GRAPHIC;
                }
            };
        }
        return imageUrl;
    }

    private StyleableStringProperty imageUrl = null;

    /**
     * Specifies the positioning of the graphic relative to the text.
     * @return the content display property
     */
    public final ObjectProperty<ContentDisplay> contentDisplayProperty() {
        return contentDisplay;
    }
    public final void setContentDisplay(ContentDisplay value) {
        contentDisplayProperty().setValue(value);
    }
    public final ContentDisplay getContentDisplay() {
        return contentDisplayProperty().getValue();
    }
    private final ObjectProperty<ContentDisplay> contentDisplay =
            new SimpleStyleableObjectProperty<>(CONTENT_DISPLAY, this, "contentDisplay", ContentDisplay.LEFT);

    /**
     * The amount of space between the graphic and text
     * @return the graphic text gap property
     */
    public final DoubleProperty graphicTextGapProperty() {
        return graphicTextGap;
    }
    public final void setGraphicTextGap(double value) {
        graphicTextGapProperty().setValue(value);
    }
    public final double getGraphicTextGap() {
        return graphicTextGapProperty().getValue();
    }
    private final DoubleProperty graphicTextGap =
            new SimpleStyleableDoubleProperty(GRAPHIC_TEXT_GAP, this, "graphicTextGap", 4d);

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



    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TooltipSkin(this);
    }



    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


    private static final CssMetaData<Tooltip.CSSBridge,Font> FONT =
            new FontCssMetaData<Tooltip.CSSBridge>("-fx-font", Font.getDefault()) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.fontProperty().isBound();
                }

                @Override
                public StyleableProperty<Font> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Font>)(WritableValue<Font>)cssBridge.tooltip.fontProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,TextAlignment> TEXT_ALIGNMENT =
            new CssMetaData<Tooltip.CSSBridge,TextAlignment>("-fx-text-alignment",
                    new EnumConverter<TextAlignment>(TextAlignment.class),
                    TextAlignment.LEFT) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.textAlignmentProperty().isBound();
                }

                @Override
                public StyleableProperty<TextAlignment> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<TextAlignment>)(WritableValue<TextAlignment>)cssBridge.tooltip.textAlignmentProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,OverrunStyle> TEXT_OVERRUN =
            new CssMetaData<Tooltip.CSSBridge,OverrunStyle>("-fx-text-overrun",
                    new EnumConverter<OverrunStyle>(OverrunStyle.class),
                    OverrunStyle.ELLIPSIS) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.textOverrunProperty().isBound();
                }

                @Override
                public StyleableProperty<OverrunStyle> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<OverrunStyle>)(WritableValue<OverrunStyle>)cssBridge.tooltip.textOverrunProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,Boolean> WRAP_TEXT =
            new CssMetaData<Tooltip.CSSBridge,Boolean>("-fx-wrap-text",
                    BooleanConverter.getInstance(), Boolean.FALSE) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.wrapTextProperty().isBound();
                }

                @Override
                public StyleableProperty<Boolean> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Boolean>)(WritableValue<Boolean>)cssBridge.tooltip.wrapTextProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,String> GRAPHIC =
            new CssMetaData<Tooltip.CSSBridge,String>("-fx-graphic",
                    StringConverter.getInstance()) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.graphicProperty().isBound();
                }

                @Override
                public StyleableProperty<String> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<String>)cssBridge.tooltip.imageUrlProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,ContentDisplay> CONTENT_DISPLAY =
            new CssMetaData<Tooltip.CSSBridge,ContentDisplay>("-fx-content-display",
                    new EnumConverter<ContentDisplay>(ContentDisplay.class),
                    ContentDisplay.LEFT) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.contentDisplayProperty().isBound();
                }

                @Override
                public StyleableProperty<ContentDisplay> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<ContentDisplay>)(WritableValue<ContentDisplay>)cssBridge.tooltip.contentDisplayProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,Number> GRAPHIC_TEXT_GAP =
            new CssMetaData<Tooltip.CSSBridge,Number>("-fx-graphic-text-gap",
                    SizeConverter.getInstance(), 4.0) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.graphicTextGapProperty().isBound();
                }

                @Override
                public StyleableProperty<Number> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Number>)(WritableValue<Number>)cssBridge.tooltip.graphicTextGapProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,Duration> SHOW_DELAY =
            new CssMetaData<Tooltip.CSSBridge,Duration>("-fx-show-delay",
                    DurationConverter.getInstance(), new Duration(1000)) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.showDelayProperty().isBound();
                }

                @Override
                public StyleableProperty<Duration> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Duration>)(WritableValue<Duration>)cssBridge.tooltip.showDelayProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,Duration> SHOW_DURATION =
            new CssMetaData<Tooltip.CSSBridge,Duration>("-fx-show-duration",
                    DurationConverter.getInstance(), new Duration(5000)) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.showDurationProperty().isBound();
                }

                @Override
                public StyleableProperty<Duration> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Duration>)(WritableValue<Duration>)cssBridge.tooltip.showDurationProperty();
                }
            };

    private static final CssMetaData<Tooltip.CSSBridge,Duration> HIDE_DELAY =
            new CssMetaData<Tooltip.CSSBridge,Duration>("-fx-hide-delay",
                    DurationConverter.getInstance(), new Duration(200)) {

                @Override
                public boolean isSettable(Tooltip.CSSBridge cssBridge) {
                    return !cssBridge.tooltip.hideDelayProperty().isBound();
                }

                @Override
                public StyleableProperty<Duration> getStyleableProperty(Tooltip.CSSBridge cssBridge) {
                    return (StyleableProperty<Duration>)(WritableValue<Duration>)cssBridge.tooltip.hideDelayProperty();
                }
            };


    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
    static {
        final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(PopupControl.getClassCssMetaData());
        styleables.add(FONT);
        styleables.add(TEXT_ALIGNMENT);
        styleables.add(TEXT_OVERRUN);
        styleables.add(WRAP_TEXT);
        styleables.add(GRAPHIC);
        styleables.add(CONTENT_DISPLAY);
        styleables.add(GRAPHIC_TEXT_GAP);
        styleables.add(SHOW_DELAY);
        styleables.add(SHOW_DURATION);
        styleables.add(HIDE_DELAY);
        STYLEABLES = Collections.unmodifiableList(styleables);
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override public Styleable getStyleableParent() {
        if (BEHAVIOR.hoveredNode == null) {
            return super.getStyleableParent();
        }
        return BEHAVIOR.hoveredNode;
    }



    /* *************************************************************************
     *                                                                         *
     * Support classes                                                         *
     *                                                                         *
     **************************************************************************/

    private final class CSSBridge extends PopupControl.CSSBridge {
        private Tooltip tooltip = Tooltip.this;

        CSSBridge() {
            super();
            setAccessibleRole(AccessibleRole.TOOLTIP);
        }
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
        private boolean cssForced = false;

        TooltipBehavior(final boolean hideOnExit) {
            this.hideOnExit = hideOnExit;

            activationTimer.setOnFinished(event -> {
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
                    double x = lastMouseX;
                    double y = lastMouseY;

                    // The tooltip always inherits the nodeOrientation of
                    // the Node that it is attached to (see RT-26147). It
                    // is possible to override this for the Tooltip content
                    // (but not the popup placement) by setting the
                    // nodeOrientation on tooltip.getScene().getRoot().
                    NodeOrientation nodeOrientation = hoveredNode.getEffectiveNodeOrientation();
                    activatedTooltip.getScene().setNodeOrientation(nodeOrientation);
                    if (nodeOrientation == NodeOrientation.RIGHT_TO_LEFT) {
                        x -= activatedTooltip.getWidth();
                    }

                    activatedTooltip.show(owner, x+TOOLTIP_XOFFSET, y+TOOLTIP_YOFFSET);

                    // RT-37107: Ensure the tooltip is displayed in a position
                    // where it will not be under the mouse, even when the tooltip
                    // is near the edge of the screen
                    if ((y+TOOLTIP_YOFFSET) > activatedTooltip.getAnchorY()) {
                        // the tooltip has been shifted vertically upwards,
                        // most likely to be underneath the mouse cursor, so we
                        // need to shift it further by hiding and reshowing
                        // in another location
                        activatedTooltip.hide();

                        y -= activatedTooltip.getHeight();
                        activatedTooltip.show(owner, x+TOOLTIP_XOFFSET, y);
                    }

                    visibleTooltip = activatedTooltip;
                    hoveredNode = null;
                    if (activatedTooltip.getShowDuration() != null) {
                        hideTimer.getKeyFrames().setAll(new KeyFrame(activatedTooltip.getShowDuration()));
                    }
                    hideTimer.playFromStart();
                }

                // Once the activation timer has expired, the tooltip is no
                // longer in the activated state, it is only in the visible
                // state, so we go ahead and set activated to false
                activatedTooltip.setActivated(false);
                activatedTooltip = null;
            });

            hideTimer.setOnFinished(event -> {
                // Hide the currently visible tooltip.
                assert visibleTooltip != null;
                visibleTooltip.hide();
                visibleTooltip = null;
                hoveredNode = null;
            });

            leftTimer.setOnFinished(event -> {
                if (!hideOnExit) {
                    // Hide the currently visible tooltip.
                    assert visibleTooltip != null;
                    visibleTooltip.hide();
                    visibleTooltip = null;
                    hoveredNode = null;
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
        private EventHandler<MouseEvent> MOVE_HANDLER = (MouseEvent event) -> {
            //Screen coordinates need to be actual for dynamic tooltip.
            //See Tooltip.setText

            lastMouseX = event.getScreenX();
            lastMouseY = event.getScreenY();

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
                        t.show(owner, event.getScreenX()+TOOLTIP_XOFFSET,
                                event.getScreenY()+TOOLTIP_YOFFSET);
                        leftTimer.stop();
                        if (t.getShowDuration() != null) {
                            hideTimer.getKeyFrames().setAll(new KeyFrame(t.getShowDuration()));
                        }
                        hideTimer.playFromStart();
                    } else {
                        // Force the CSS to be processed for the tooltip so that it uses the
                        // appropriate timings for showDelay, showDuration, and hideDelay.
                        if (!cssForced) {
                            double opacity = t.getOpacity();
                            t.setOpacity(0);
                            t.show(owner);
                            t.hide();
                            t.setOpacity(opacity);
                            cssForced = true;
                        }

                        // Start / restart the timer and make sure the tooltip
                        // is marked as activated.
                        t.setActivated(true);
                        activatedTooltip = t;
                        activationTimer.stop();
                        if (t.getShowDelay() != null) {
                            activationTimer.getKeyFrames().setAll(new KeyFrame(t.getShowDelay()));
                        }
                        activationTimer.playFromStart();
                    }
                }
            } else {
                // TODO should deregister, no point being here anymore!
            }
        };

        /**
         * Registers for mouse exit events. If the ACTIVATION_TIMER is running then
         * this will simply stop it. If the HIDE_TIMER is running then this will
         * stop the HIDE_TIMER, hide the tooltip, and start the LEFT_TIMER.
         */
        private EventHandler<MouseEvent> LEAVING_HANDLER = (MouseEvent event) -> {
            // detect bogus mouse exit events, if it didn't really move then ignore it
            if (activationTimer.getStatus() == Timeline.Status.RUNNING) {
                activationTimer.stop();
            } else if (hideTimer.getStatus() == Timeline.Status.RUNNING) {
                assert visibleTooltip != null;
                hideTimer.stop();
                if (hideOnExit) visibleTooltip.hide();
                Node source = (Node) event.getSource();
                Tooltip t = (Tooltip) source.getProperties().get(TOOLTIP_PROP_KEY);
                if (t != null) {
                    if (t.getHideDelay() != null) {
                        leftTimer.getKeyFrames().setAll(new KeyFrame(t.getHideDelay()));
                    }
                    leftTimer.playFromStart();
                }
            }

            hoveredNode = null;
            activatedTooltip = null;
            if (hideOnExit) visibleTooltip = null;
        };

        /**
         * Registers for mouse click, press, release, drag events. If any of these
         * occur, then the tooltip is hidden (if it is visible), it is deactivated,
         * and any and all timers are stopped.
         */
        private EventHandler<MouseEvent> KILL_HANDLER = (MouseEvent event) -> {
            activationTimer.stop();
            hideTimer.stop();
            leftTimer.stop();
            if (visibleTooltip != null) visibleTooltip.hide();
            hoveredNode = null;
            activatedTooltip = null;
            visibleTooltip = null;
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
