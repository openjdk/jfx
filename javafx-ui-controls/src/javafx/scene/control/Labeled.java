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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;


/**
 * A Labeled {@link Control} is one which has as part of its user interface
 * a textual content associated with it. For example, a {@link Button} displays
 * {@code text}, as does a {@link Label}, a {@link Tooltip}, and many
 * other controls.
 * <p>
 * Labeled is also a convenient base class from which to extend when building
 * new Controls which, as part of their UI, display read-only textual content.
 * </p>
 *  
 * <p>Example of how to place a graphic above the text:
 * <pre><code>
 *  Image image = new Image(getClass().getResourceAsStream("image.png"));
 *  ImageView imageView = new ImageView();
 *  imageView.setImage(image);
 *  Label label = new Label("text", imageView);
 *  label.setContentDisplay(ContentDisplay.TOP);
 * </code></pre>
 *
 * @see Button
 * @see Label
 * @see ToggleButton
 */
@DefaultProperty("text")
public abstract class Labeled extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a Label with no text and graphic
     */
    public Labeled() { }

    /**
     * Creates a Label with text
     * @param text The text for the label.
     */
    public Labeled(String text) {
        setText(text);
    }

    /**
     * Creates a Label with text and a graphic
     * @param text The text for the label.
     * @param graphic The graphic for the label.
     */
    public Labeled(String text, Node graphic) {
        setText(text);
        setGraphic(graphic);
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * The text to display in the label. The text may be null.
     */
    public final StringProperty textProperty() {
        if (text == null) {
            text = new SimpleStringProperty(this, "text", "");
        }
        return text;
    }
    private StringProperty text;
    public final void setText(String value) { textProperty().setValue(value); }
    public final String getText() { return text == null ? "" : text.getValue(); }

    /**
     * Specifies how the text and graphic within the Labeled should be
     * aligned when there is empty space within the Labeled.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new ObjectPropertyBase<Pos>(Pos.CENTER_LEFT) {
                // We have to ensure that the cssPropertyInvalidated flag is called
                // even when the old value == the new value.
                @Override public void set(Pos value) {
                    super.set(value);
                    impl_cssPropertyInvalidated(StyleableProperties.ALIGNMENT);
                }

                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.ALIGNMENT);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "alignment";
                }
            };
        }
        return alignment;
    }
    @Styleable(property="-fx-alignment", initial="center_left")
    private ObjectProperty<Pos> alignment;
    public final void setAlignment(Pos value) { alignmentProperty().set(value); }
    public final Pos getAlignment() { return alignment == null ? Pos.CENTER_LEFT : alignment.get(); }


    /**
     * Specifies the behavior for lines of text <em>when text is multiline</em>
     * Unlike {@link #contentDisplayProperty} which affects the graphic and text, this setting
     * only affects multiple lines of text relative to the text bounds.
     */
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        if (textAlignment == null) {
            textAlignment = new ObjectPropertyBase<TextAlignment>(TextAlignment.LEFT) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.TEXT_ALIGNMENT);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "textAlignment";
                }
            };
        }
        return textAlignment;
    }
    @Styleable(property="-fx-text-alignment", initial="left")
    private ObjectProperty<TextAlignment> textAlignment;
    public final void setTextAlignment(TextAlignment value) { textAlignmentProperty().setValue(value); }
    public final TextAlignment getTextAlignment() { return textAlignment == null ? TextAlignment.LEFT : textAlignment.getValue(); }

    /**
     * Specifies the behavior to use if the text of the {@code Labeled}
     * exceeds the available space for rendering the text.
     */
    public final ObjectProperty<OverrunStyle> textOverrunProperty() {
        if (textOverrun == null) {
            textOverrun = new ObjectPropertyBase<OverrunStyle>(OverrunStyle.ELLIPSIS) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.TEXT_OVERRUN);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "textOverrun";
                }
            };
        }
        return textOverrun;
    }
    @Styleable(property="-fx-text-overrun", initial="ellipsis")
    private ObjectProperty<OverrunStyle> textOverrun;
    public final void setTextOverrun(OverrunStyle value) { textOverrunProperty().setValue(value); }
    public final OverrunStyle getTextOverrun() { return textOverrun == null ? OverrunStyle.ELLIPSIS : textOverrun.getValue(); }

    /**
     * If a run of text exceeds the width of the Labeled, then this variable
     * indicates whether the text should wrap onto another line.
     */
    public final BooleanProperty wrapTextProperty() {
        if (wrapText == null) {
            wrapText = new BooleanPropertyBase() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.WRAP_TEXT);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "wrapText";
                }
            };
        }
        return wrapText;
    }
    @Styleable(property="-fx-wrap-text", initial="false")
    private BooleanProperty wrapText;
    public final void setWrapText(boolean value) { wrapTextProperty().setValue(value); }
    public final boolean isWrapText() { return wrapText == null ? false : wrapText.getValue(); }

    /**
     * If wrapText is true, then contentBias will be HORIZONTAL, otherwise it is null.
     * @return orientation of width/height dependency or null if there is none
     */
    @Override public Orientation getContentBias() {
        return isWrapText()? Orientation.HORIZONTAL : null;
    }

    /**
     * The default font to use for text in the Labeled. If the Label's text is
     * rich text then this font may or may not be used depending on the font
     * information embedded in the rich text, but in any case where a default
     * font is required, this font will be used.
     */
    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new ObjectPropertyBase<Font>(Font.getDefault()) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.FONT);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "font";
                }
            };
        }
        return font;
    }

    @Styleable(property="-fx-font", inherits=true)
    private ObjectProperty<Font> font;
    public final void setFont(Font value) { fontProperty().setValue(value); }
    public final Font getFont() { return font == null ? Font.getDefault() : font.getValue(); }

    /**
     * An optional icon for the Labeled. This can be positioned relative to the
     * text by using {@link #setContentDisplay}.  The node specified for this
     * variable cannot appear elsewhere in the scene graph, otherwise
     * the {@code IllegalArgumentException} is thrown.  See the class
     * description of {@link javafx.scene.Node Node} for more detail.
     */
    public final ObjectProperty<Node> graphicProperty() {
        if (graphic == null) {
            graphic = new ObjectPropertyBase<Node>() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.GRAPHIC);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "graphic";
                }
            };
        }
        return graphic;
    }
    @Styleable(property="-fx-graphic", converter="com.sun.javafx.css.converters.URLConverter")
    private ObjectProperty<Node> graphic;
    public final void setGraphic(Node value) {
        graphicProperty().setValue(value);
        cachedImageUrl = null;
    }
    public final Node getGraphic() { return graphic == null ? null : graphic.getValue(); }

    /**
     * Whether all text should be underlined.
     */
    public final BooleanProperty underlineProperty() {
        if (underline == null) {
            underline = new SimpleBooleanProperty(this, "underline");
        }
        return underline;
    }
    @Styleable(property="-fx-underline", initial="false")
    private BooleanProperty underline;
    public final void setUnderline(boolean value) { underlineProperty().setValue(value); }
    public final boolean isUnderline() { return underline == null ? false : underline.getValue(); }

    /**
     * Specifies the positioning of the graphic relative to the text.
     */
    public final ObjectProperty<ContentDisplay> contentDisplayProperty() {
        if (contentDisplay == null) {
            contentDisplay = new ObjectPropertyBase<ContentDisplay>(ContentDisplay.LEFT) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.CONTENT_DISPLAY);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "contentDisplay";
                }
            };
        }
        return contentDisplay;
    }
    @Styleable(property="-fx-content-display", initial="left")
    private ObjectProperty<ContentDisplay> contentDisplay;
    public final void setContentDisplay(ContentDisplay value) { contentDisplayProperty().setValue(value); }
    public final ContentDisplay getContentDisplay() { return contentDisplay == null ? ContentDisplay.LEFT : contentDisplay.getValue(); }

    /**
     * The padding around the Labeled's text and graphic content.
     * By default labelPadding is Insets.EMPTY and cannot be set to null.
     * Subclasses may add nodes outside this padding and inside the Labeled's padding.
     *
     * This property can only be set from CSS.
     */
    public final ReadOnlyObjectProperty<Insets> labelPaddingProperty() {
        return labelPaddingPropertyImpl().getReadOnlyProperty();
    }
    private ReadOnlyObjectWrapper<Insets> labelPaddingPropertyImpl() {
        if (labelPadding == null) {
            labelPadding = new ReadOnlyObjectWrapper<Insets>(Insets.EMPTY) {
                private Insets lastValidValue = Insets.EMPTY;

                @Override
                public void invalidated() {
                    final Insets newValue = get();
                    if (newValue == null) {
                        set(lastValidValue);
                        throw new NullPointerException("cannot set labelPadding to null");
                    }
                    lastValidValue = newValue;
                    requestLayout();
                    impl_cssPropertyInvalidated(StyleableProperties.LABEL_PADDING);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "labelPadding";
                }
            };
        }
        return labelPadding;
    }
    @Styleable(property="-fx-label-padding")
    private ReadOnlyObjectWrapper<Insets> labelPadding;
    private void setLabelPadding(Insets value) { labelPaddingPropertyImpl().set(value); }
    public final Insets getLabelPadding() { return labelPadding == null ? Insets.EMPTY : labelPadding.get(); }

    /**
     * The amount of space between the graphic and text
     */
    public final DoubleProperty graphicTextGapProperty() {
        if (graphicTextGap == null) {
            graphicTextGap = new DoublePropertyBase(4) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.GRAPHIC_TEXT_GAP);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "graphicTextGap";
                }
            };
        }
        return graphicTextGap;
    }
    @Styleable(property="-fx-graphic-text-gap", initial="4")
    private DoubleProperty graphicTextGap;
    public final void setGraphicTextGap(double value) { graphicTextGapProperty().setValue(value); }
    public final double getGraphicTextGap() { return graphicTextGap == null ? 4 : graphicTextGap.getValue(); }


    /**
     * The {@link Paint} used to fill the text.
     */
    @Styleable(property="-fx-text-fill", initial="black", inherits=true)
    private ObjectProperty<Paint> textFill; // TODO for now change this

    public final void setTextFill(Paint value) {
        textFillProperty().set(value);
    }

    public final Paint getTextFill() {
        return textFill == null ? Color.BLACK : textFill.get();
    }

    public final ObjectProperty<Paint> textFillProperty() {
        if (textFill == null) {
            textFill = new ObjectPropertyBase<Paint>(Color.BLACK) {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.TEXT_FILL);
                }

                @Override
                public Object getBean() {
                    return Labeled.this;
                }

                @Override
                public String getName() {
                    return "textFill";
                }
            };
        }
        return textFill;
    }


    /**
     * MnemonicParsing property to enable/disable text parsing.
     * If this is set to true, then the Label text will be
     * parsed to see if it contains the mnemonic parsing character '_'.
     * When a mnemonic is detected the key combination will
     * be determined based on the succeeding character, and the mnemonic
     * added.
     * 
     * <p>
     * The default value for Labeled is false, but it
     * is enabled by default on some Controls.
     * </p>
     */
    private BooleanProperty mnemonicParsing;
    public final void setMnemonicParsing(boolean value) {
        mnemonicParsingProperty().set(value);
    }
    public final boolean isMnemonicParsing() {
        return mnemonicParsing == null ? false : mnemonicParsing.get();
    }
    public final BooleanProperty mnemonicParsingProperty() {
        if (mnemonicParsing == null) {
            mnemonicParsing = new SimpleBooleanProperty(this, "mnemonicParsing");
        }
        return mnemonicParsing;
    }

    //    /**
    //     * This is the symbol that is searched for in the text and used as
    //     * a mnemonic. You can change what symbol is used. Using the symbol
    //     * more than once will cause the symbol to be escaped. Thus, if "_"
    //     * (the default) is used, then the string "H_ello World" will use
    //     * "e" as the mnemonic. If "H__ello World" is used, then no mnemonic
    //     * will be used and the text will be rendered as "H_ello World".
    //     * TODO: Have i18n review this part of the API to confirm proper
    //     * externalization will work as expected
    //     */

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

     /**
      * @treatasprivate implementation detail
      */
    private static class StyleableProperties {
        private static final StyleableProperty FONT = new StyleableProperty(Labeled.class, "font", StyleableProperty.FONT.getSubProperties());
        private static final StyleableProperty ALIGNMENT = new StyleableProperty(Labeled.class, "alignment");
        private static final StyleableProperty TEXT_ALIGNMENT = new StyleableProperty(Labeled.class, "textAlignment");
        private static final StyleableProperty TEXT_FILL = new StyleableProperty(Labeled.class, "textFill");
        private static final StyleableProperty TEXT_OVERRUN = new StyleableProperty(Labeled.class, "textOverrun");
        private static final StyleableProperty WRAP_TEXT = new StyleableProperty(Labeled.class, "wrapText");
        private static final StyleableProperty GRAPHIC = new StyleableProperty(Labeled.class, "graphic");
        private static final StyleableProperty UNDERLINE = new StyleableProperty(Labeled.class, "underline");
        private static final StyleableProperty CONTENT_DISPLAY = new StyleableProperty(Labeled.class, "contentDisplay");
        private static final StyleableProperty LABEL_PADDING = new StyleableProperty(Labeled.class, "labelPadding");
        private static final StyleableProperty GRAPHIC_TEXT_GAP = new StyleableProperty(Labeled.class, "graphicTextGap");

        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FONT,
                ALIGNMENT,
                TEXT_ALIGNMENT,
                TEXT_FILL,
                TEXT_OVERRUN,
                WRAP_TEXT,
                GRAPHIC,
                UNDERLINE,
                CONTENT_DISPLAY,
                LABEL_PADDING,
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
        return Labeled.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Labeled.StyleableProperties.STYLEABLES;
    }

    private String cachedImageUrl = null;
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-alignment".equals(property)) {
            setAlignment((Pos)value);
        } else if ("-fx-text-alignment".equals(property)) {
            setTextAlignment((TextAlignment)value);
        } else if ("-fx-text-fill".equals(property)) {
            setTextFill((Paint)value);
        } else if ("-fx-text-overrun".equals(property)) {
            setTextOverrun((OverrunStyle)value);
        } else if ("-fx-wrap-text".equals(property)) {
            setWrapText(((Boolean)value).booleanValue());
        } else if ("-fx-font".equals(property)) {
            setFont((Font)value);
        } else if ("-fx-graphic".equals(property)) {            
            String imageUrl = (String)value;
            if (imageUrl != null && !imageUrl.equals(cachedImageUrl)) {
                setGraphic(new ImageView(new Image(imageUrl)));
            }
            cachedImageUrl = imageUrl;
        } else if ("-fx-underline".equals(property)) {
            setUnderline(((Boolean)value).booleanValue());
        } else if ("-fx-content-display".equals(property)) {
            setContentDisplay((ContentDisplay)value);
        } else if ("-fx-label-padding".equals(property)) {
            setLabelPadding((Insets)value);
        } else if ("-fx-graphic-text-gap".equals(property)) {
            setGraphicTextGap(((Double)value).doubleValue());
//        } else {
//            return false;
        }
//        return true;
         return super.impl_cssSet(property, value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-alignment".equals(property))
            return alignment == null || !alignment.isBound();
        else if ("-fx-text-alignment".equals(property))
            return textAlignment == null || !textAlignment.isBound();
        else if ("-fx-text-fill".equals(property))
            return textFill == null || !textFill.isBound();
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
        else if ("-fx-label-padding".equals(property))
            return labelPadding == null || !labelPadding.isBound();
        else if ("-fx-graphic-text-gap".equals(property))
            return graphicTextGap == null || !graphicTextGap.isBound();
        else if ("-fx-underline".equals(property))
            return underline == null || !underline.isBound();
        else
            return super.impl_cssSettable(property);
    }
}
