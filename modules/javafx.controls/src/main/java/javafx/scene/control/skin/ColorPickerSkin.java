/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import static javafx.scene.paint.Color.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.converter.StringConverter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.Properties;
import com.sun.javafx.scene.control.behavior.ColorPickerBehavior;
import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import com.sun.javafx.scene.control.skin.Utils;

/**
 * Default skin implementation for the {@link ColorPicker} control.
 *
 * @see ColorPicker
 * @since 9
 */
public class ColorPickerSkin extends ComboBoxPopupControl<Color> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private Label displayNode;
    private StackPane pickerColorBox;
    private Rectangle colorRect;
    private ColorPalette popupContent;

    private final ColorPickerBehavior behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ColorPickerSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ColorPickerSkin(final ColorPicker control) {
        super(control);

        // install default input map for the control
        this.behavior = new ColorPickerBehavior(control);

        updateComboBoxMode();

        ListenerHelper.get(this).addChangeListener(control.valueProperty(), (ev) -> updateColor());

        // create displayNode
        displayNode = new Label();
        displayNode.getStyleClass().add("color-picker-label");
        displayNode.setManaged(false);

        // label graphic
        pickerColorBox = new PickerColorBox();
        pickerColorBox.getStyleClass().add("picker-color");
        colorRect = new Rectangle(12, 12);
        colorRect.getStyleClass().add("picker-color-rect");

        updateColor();

        pickerColorBox.getChildren().add(colorRect);
        displayNode.setGraphic(pickerColorBox);

        if (control.isShowing()) {
            show();
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- color label visible
    BooleanProperty colorLabelVisible = new StyleableBooleanProperty(true) {
        @Override public void invalidated() {
            if (displayNode != null) {
                if (colorLabelVisible.get()) {
                    displayNode.setText(colorDisplayName(((ColorPicker)getSkinnable()).getValue()));
                } else {
                    displayNode.setText("");
                }
            }
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorLabelVisible";
        }
        @Override public CssMetaData<ColorPicker,Boolean> getCssMetaData() {
            return StyleableProperties.COLOR_LABEL_VISIBLE;
        }
    };

    // --- image url
    private final StringProperty imageUrlProperty() { return imageUrl; }
    private final StyleableStringProperty imageUrl = new StyleableStringProperty() {
        @Override public void applyStyle(StyleOrigin origin, String v) {
            super.applyStyle(origin, v);
            if (v == null) {
                // remove old image view
                if (pickerColorBox.getChildren().size() == 2) pickerColorBox.getChildren().remove(1);
            } else {
                if (pickerColorBox.getChildren().size() == 2) {
                    ImageView imageView = (ImageView)pickerColorBox.getChildren().get(1);
                    imageView.setImage(StyleManager.getInstance().getCachedImage(v));
                } else {
                    pickerColorBox.getChildren().add(new ImageView(StyleManager.getInstance().getCachedImage(v)));
                }
            }
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "imageUrl";
        }
        @Override public CssMetaData<ColorPicker,String> getCssMetaData() {
            return StyleableProperties.GRAPHIC;
        }
    };

    // --- color rect width
    private final StyleableDoubleProperty colorRectWidth =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_WIDTH;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectWidth";
        }
    };

    // --- color rect height
    private final StyleableDoubleProperty colorRectHeight =  new StyleableDoubleProperty(12) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_HEIGHT;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectHeight";
        }
    };

    // --- color rect X
    private final StyleableDoubleProperty colorRectX =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_X;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectX";
        }
    };

    // --- color rect Y
    private final StyleableDoubleProperty colorRectY =  new StyleableDoubleProperty(0) {
        @Override protected void invalidated() {
            if(pickerColorBox!=null) pickerColorBox.requestLayout();
        }
        @Override public CssMetaData<ColorPicker,Number> getCssMetaData() {
            return StyleableProperties.COLOR_RECT_Y;
        }
        @Override public Object getBean() {
            return ColorPickerSkin.this;
        }
        @Override public String getName() {
            return "colorRectY";
        }
    };



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (!colorLabelVisible.get()) {
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
        String displayNodeText = displayNode.getText();
        double width = 0;
        for (String name : COLOR_NAME_MAP.values()) {
            displayNode.setText(name);
            width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
        }
        displayNode.setText(Utils.formatHexString(Color.BLACK)); // #000000
        width = Math.max(width, super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
        displayNode.setText(displayNodeText);
        return width;
    }

    /** {@inheritDoc} */
    @Override protected Node getPopupContent() {
        if (popupContent == null) {
//            popupContent = new ColorPalette(colorPicker.getValue(), colorPicker);
            popupContent = new ColorPalette((ColorPicker)getSkinnable());
            popupContent.setPopupControl(getPopup());
        }
        return popupContent;
    }

    /** {@inheritDoc} */
    @Override public void show() {
        super.show();
        final ColorPicker colorPicker = (ColorPicker)getSkinnable();
        popupContent.updateSelection(colorPicker.getValue());
    }

    /** {@inheritDoc} */
    @Override public Node getDisplayNode() {
        return displayNode;
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        updateComboBoxMode();
        super.layoutChildren(x, y, w, h);
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void focusLost() {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override ComboBoxBaseBehavior getBehavior() {
        return behavior;
    }

    private void updateComboBoxMode() {
        List<String> styleClass = getSkinnable().getStyleClass();
        if (styleClass.contains(ColorPicker.STYLE_CLASS_BUTTON)) {
            setMode(ComboBoxMode.BUTTON);
        } else if (styleClass.contains(ColorPicker.STYLE_CLASS_SPLIT_BUTTON)) {
            setMode(ComboBoxMode.SPLITBUTTON);
        }
    }

    // Translatable display names for the most common colors
    private static final Map<Color, String> COLOR_NAME_MAP = Map.ofEntries(
        Map.entry(TRANSPARENT, Properties.getColorPickerString("colorName.transparent")),
        Map.entry(BLACK,       Properties.getColorPickerString("colorName.black")),
        Map.entry(BLUE,        Properties.getColorPickerString("colorName.blue")),
        Map.entry(CYAN,        Properties.getColorPickerString("colorName.cyan")),
        Map.entry(DARKBLUE,    Properties.getColorPickerString("colorName.darkblue")),
        Map.entry(DARKCYAN,    Properties.getColorPickerString("colorName.darkcyan")),
        Map.entry(DARKGRAY,    Properties.getColorPickerString("colorName.darkgray")),
        Map.entry(DARKGREEN,   Properties.getColorPickerString("colorName.darkgreen")),
        Map.entry(DARKMAGENTA, Properties.getColorPickerString("colorName.darkmagenta")),
        Map.entry(DARKRED,     Properties.getColorPickerString("colorName.darkred")),
        Map.entry(GRAY,        Properties.getColorPickerString("colorName.gray")),
        Map.entry(GREEN,       Properties.getColorPickerString("colorName.green")),
        Map.entry(LIGHTBLUE,   Properties.getColorPickerString("colorName.lightblue")),
        Map.entry(LIGHTCYAN,   Properties.getColorPickerString("colorName.lightcyan")),
        Map.entry(LIGHTGRAY,   Properties.getColorPickerString("colorName.lightgray")),
        Map.entry(LIGHTGREEN,  Properties.getColorPickerString("colorName.lightgreen")),
        Map.entry(LIGHTYELLOW, Properties.getColorPickerString("colorName.lightyellow")),
        Map.entry(LIME,        Properties.getColorPickerString("colorName.lime")),
        Map.entry(MAGENTA,     Properties.getColorPickerString("colorName.magenta")),
        Map.entry(MAROON,      Properties.getColorPickerString("colorName.maroon")),
        Map.entry(MEDIUMBLUE,  Properties.getColorPickerString("colorName.mediumblue")),
        Map.entry(NAVY,        Properties.getColorPickerString("colorName.navy")),
        Map.entry(OLIVE,       Properties.getColorPickerString("colorName.olive")),
        Map.entry(ORANGE,      Properties.getColorPickerString("colorName.orange")),
        Map.entry(PINK,        Properties.getColorPickerString("colorName.pink")),
        Map.entry(PURPLE,      Properties.getColorPickerString("colorName.purple")),
        Map.entry(RED,         Properties.getColorPickerString("colorName.red")),
        Map.entry(TEAL,        Properties.getColorPickerString("colorName.teal")),
        Map.entry(WHITE,       Properties.getColorPickerString("colorName.white")),
        Map.entry(YELLOW,      Properties.getColorPickerString("colorName.yellow")));

    // CSS names.
    // Note that synonyms (such as "grey") have been removed here,
    // since a color can be presented with only one name in this
    // skin. If a reverse map is created for parsing names in the
    // future, then the synonyms should be included there. For a
    // full list of CSS names, see Color.java.
    private static final Map<Color, String> CSS_NAME_MAP = Map.ofEntries(
        Map.entry(ALICEBLUE,            "aliceblue"),
        Map.entry(ANTIQUEWHITE,         "antiquewhite"),
        Map.entry(AQUAMARINE,           "aquamarine"),
        Map.entry(AZURE,                "azure"),
        Map.entry(BEIGE,                "beige"),
        Map.entry(BISQUE,               "bisque"),
        Map.entry(BLACK,                "black"),
        Map.entry(BLANCHEDALMOND,       "blanchedalmond"),
        Map.entry(BLUE,                 "blue"),
        Map.entry(BLUEVIOLET,           "blueviolet"),
        Map.entry(BROWN,                "brown"),
        Map.entry(BURLYWOOD,            "burlywood"),
        Map.entry(CADETBLUE,            "cadetblue"),
        Map.entry(CHARTREUSE,           "chartreuse"),
        Map.entry(CHOCOLATE,            "chocolate"),
        Map.entry(CORAL,                "coral"),
        Map.entry(CORNFLOWERBLUE,       "cornflowerblue"),
        Map.entry(CORNSILK,             "cornsilk"),
        Map.entry(CRIMSON,              "crimson"),
        Map.entry(CYAN,                 "cyan"),
        Map.entry(DARKBLUE,             "darkblue"),
        Map.entry(DARKCYAN,             "darkcyan"),
        Map.entry(DARKGOLDENROD,        "darkgoldenrod"),
        Map.entry(DARKGRAY,             "darkgray"),
        Map.entry(DARKGREEN,            "darkgreen"),
        Map.entry(DARKKHAKI,            "darkkhaki"),
        Map.entry(DARKMAGENTA,          "darkmagenta"),
        Map.entry(DARKOLIVEGREEN,       "darkolivegreen"),
        Map.entry(DARKORANGE,           "darkorange"),
        Map.entry(DARKORCHID,           "darkorchid"),
        Map.entry(DARKRED,              "darkred"),
        Map.entry(DARKSALMON,           "darksalmon"),
        Map.entry(DARKSEAGREEN,         "darkseagreen"),
        Map.entry(DARKSLATEBLUE,        "darkslateblue"),
        Map.entry(DARKSLATEGRAY,        "darkslategray"),
        Map.entry(DARKTURQUOISE,        "darkturquoise"),
        Map.entry(DARKVIOLET,           "darkviolet"),
        Map.entry(DEEPPINK,             "deeppink"),
        Map.entry(DEEPSKYBLUE,          "deepskyblue"),
        Map.entry(DIMGRAY,              "dimgray"),
        Map.entry(DODGERBLUE,           "dodgerblue"),
        Map.entry(FIREBRICK,            "firebrick"),
        Map.entry(FLORALWHITE,          "floralwhite"),
        Map.entry(FORESTGREEN,          "forestgreen"),
        Map.entry(GAINSBORO,            "gainsboro"),
        Map.entry(GHOSTWHITE,           "ghostwhite"),
        Map.entry(GOLD,                 "gold"),
        Map.entry(GOLDENROD,            "goldenrod"),
        Map.entry(GRAY,                 "gray"),
        Map.entry(GREEN,                "green"),
        Map.entry(GREENYELLOW,          "greenyellow"),
        Map.entry(HONEYDEW,             "honeydew"),
        Map.entry(HOTPINK,              "hotpink"),
        Map.entry(INDIANRED,            "indianred"),
        Map.entry(INDIGO,               "indigo"),
        Map.entry(IVORY,                "ivory"),
        Map.entry(KHAKI,                "khaki"),
        Map.entry(LAVENDER,             "lavender"),
        Map.entry(LAVENDERBLUSH,        "lavenderblush"),
        Map.entry(LAWNGREEN,            "lawngreen"),
        Map.entry(LEMONCHIFFON,         "lemonchiffon"),
        Map.entry(LIGHTBLUE,            "lightblue"),
        Map.entry(LIGHTCORAL,           "lightcoral"),
        Map.entry(LIGHTCYAN,            "lightcyan"),
        Map.entry(LIGHTGOLDENRODYELLOW, "lightgoldenrodyellow"),
        Map.entry(LIGHTGRAY,            "lightgray"),
        Map.entry(LIGHTGREEN,           "lightgreen"),
        Map.entry(LIGHTPINK,            "lightpink"),
        Map.entry(LIGHTSALMON,          "lightsalmon"),
        Map.entry(LIGHTSEAGREEN,        "lightseagreen"),
        Map.entry(LIGHTSKYBLUE,         "lightskyblue"),
        Map.entry(LIGHTSLATEGRAY,       "lightslategray"),
        Map.entry(LIGHTSTEELBLUE,       "lightsteelblue"),
        Map.entry(LIGHTYELLOW,          "lightyellow"),
        Map.entry(LIME,                 "lime"),
        Map.entry(LIMEGREEN,            "limegreen"),
        Map.entry(LINEN,                "linen"),
        Map.entry(MAGENTA,              "magenta"),
        Map.entry(MAROON,               "maroon"),
        Map.entry(MEDIUMAQUAMARINE,     "mediumaquamarine"),
        Map.entry(MEDIUMBLUE,           "mediumblue"),
        Map.entry(MEDIUMORCHID,         "mediumorchid"),
        Map.entry(MEDIUMPURPLE,         "mediumpurple"),
        Map.entry(MEDIUMSEAGREEN,       "mediumseagreen"),
        Map.entry(MEDIUMSLATEBLUE,      "mediumslateblue"),
        Map.entry(MEDIUMSPRINGGREEN,    "mediumspringgreen"),
        Map.entry(MEDIUMTURQUOISE,      "mediumturquoise"),
        Map.entry(MEDIUMVIOLETRED,      "mediumvioletred"),
        Map.entry(MIDNIGHTBLUE,         "midnightblue"),
        Map.entry(MINTCREAM,            "mintcream"),
        Map.entry(MISTYROSE,            "mistyrose"),
        Map.entry(MOCCASIN,             "moccasin"),
        Map.entry(NAVAJOWHITE,          "navajowhite"),
        Map.entry(NAVY,                 "navy"),
        Map.entry(OLDLACE,              "oldlace"),
        Map.entry(OLIVE,                "olive"),
        Map.entry(OLIVEDRAB,            "olivedrab"),
        Map.entry(ORANGE,               "orange"),
        Map.entry(ORANGERED,            "orangered"),
        Map.entry(ORCHID,               "orchid"),
        Map.entry(PALEGOLDENROD,        "palegoldenrod"),
        Map.entry(PALEGREEN,            "palegreen"),
        Map.entry(PALETURQUOISE,        "paleturquoise"),
        Map.entry(PALEVIOLETRED,        "palevioletred"),
        Map.entry(PAPAYAWHIP,           "papayawhip"),
        Map.entry(PEACHPUFF,            "peachpuff"),
        Map.entry(PERU,                 "peru"),
        Map.entry(PINK,                 "pink"),
        Map.entry(PLUM,                 "plum"),
        Map.entry(POWDERBLUE,           "powderblue"),
        Map.entry(PURPLE,               "purple"),
        Map.entry(RED,                  "red"),
        Map.entry(ROSYBROWN,            "rosybrown"),
        Map.entry(ROYALBLUE,            "royalblue"),
        Map.entry(SADDLEBROWN,          "saddlebrown"),
        Map.entry(SALMON,               "salmon"),
        Map.entry(SANDYBROWN,           "sandybrown"),
        Map.entry(SEAGREEN,             "seagreen"),
        Map.entry(SEASHELL,             "seashell"),
        Map.entry(SIENNA,               "sienna"),
        Map.entry(SILVER,               "silver"),
        Map.entry(SKYBLUE,              "skyblue"),
        Map.entry(SLATEBLUE,            "slateblue"),
        Map.entry(SLATEGRAY,            "slategray"),
        Map.entry(SNOW,                 "snow"),
        Map.entry(SPRINGGREEN,          "springgreen"),
        Map.entry(STEELBLUE,            "steelblue"),
        Map.entry(TAN,                  "tan"),
        Map.entry(TEAL,                 "teal"),
        Map.entry(THISTLE,              "thistle"),
        Map.entry(TOMATO,               "tomato"),
        Map.entry(TRANSPARENT,          "transparent"),
        Map.entry(TURQUOISE,            "turquoise"),
        Map.entry(VIOLET,               "violet"),
        Map.entry(WHEAT,                "wheat"),
        Map.entry(WHITE,                "white"),
        Map.entry(WHITESMOKE,           "whitesmoke"),
        Map.entry(YELLOW,               "yellow"),
        Map.entry(YELLOWGREEN,          "yellowgreen"));

    static String colorDisplayName(Color c) {
        if (c != null) {
            String displayName = COLOR_NAME_MAP.get(c);
            if (displayName == null) {
                displayName = Utils.formatHexString(c);
            }
            return displayName;
        } else {
            return null;
        }
    }

    static String tooltipString(Color c) {
        if (c != null) {
            String tooltipStr = "";
            String displayName = COLOR_NAME_MAP.get(c);
            if (displayName != null) {
                tooltipStr += displayName + " ";
            }

            tooltipStr += Utils.formatHexString(c);

            String cssName = CSS_NAME_MAP.get(c);
            if (cssName != null) {
                tooltipStr += " (css: " + cssName + ")";
            }
            return tooltipStr;
        } else {
            return null;
        }
    }

    private void updateColor() {
        final ColorPicker colorPicker = (ColorPicker)getSkinnable();
        colorRect.setFill(colorPicker.getValue());
        if (colorLabelVisible.get()) {
            displayNode.setText(colorDisplayName(colorPicker.getValue()));
        } else {
            displayNode.setText("");
        }
    }



    /* *************************************************************************
    *                                                                         *
    *                         picker-color-cell                               *
    *                                                                         *
    **************************************************************************/

    private class PickerColorBox extends StackPane {
        @Override protected void layoutChildren() {
            final double top = snappedTopInset();
            final double left = snappedLeftInset();
            final double width = getWidth();
            final double height = getHeight();
            final double right = snappedRightInset();
            final double bottom = snappedBottomInset();
            colorRect.setX(snapPositionX(colorRectX.get()));
            colorRect.setY(snapPositionY(colorRectY.get()));
            colorRect.setWidth(snapSizeX(colorRectWidth.get()));
            colorRect.setHeight(snapSizeY(colorRectHeight.get()));
            if (getChildren().size() == 2) {
                final ImageView icon = (ImageView) getChildren().get(1);
                Pos childAlignment = StackPane.getAlignment(icon);
                layoutInArea(icon, left, top,
                             width - left - right, height - top - bottom,
                             0, getMargin(icon),
                             childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                             childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
                colorRect.setLayoutX(icon.getLayoutX());
                colorRect.setLayoutY(icon.getLayoutY());
            } else {
                Pos childAlignment = StackPane.getAlignment(colorRect);
                layoutInArea(colorRect, left, top,
                             width - left - right, height - top - bottom,
                             0, getMargin(colorRect),
                             childAlignment != null? childAlignment.getHpos() : getAlignment().getHpos(),
                             childAlignment != null? childAlignment.getVpos() : getAlignment().getVpos());
            }
        }
    }

    /* *************************************************************************
    *                                                                         *
    *                         Stylesheet Handling                             *
    *                                                                         *
    **************************************************************************/

     private static class StyleableProperties {
        private static final CssMetaData<ColorPicker,Boolean> COLOR_LABEL_VISIBLE =
                new CssMetaData<>("-fx-color-label-visible",
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override public boolean isSettable(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return skin.colorLabelVisible == null || !skin.colorLabelVisible.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(ColorPicker n) {
                final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                return (StyleableProperty<Boolean>)skin.colorLabelVisible;
            }
        };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_WIDTH =
                new CssMetaData<>("-fx-color-rect-width", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectWidth.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectWidth;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_HEIGHT =
                new CssMetaData<>("-fx-color-rect-height", SizeConverter.getInstance(), 12d) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectHeight.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectHeight;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_X =
                new CssMetaData<>("-fx-color-rect-x", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectX.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectX;
                    }
                };
        private static final CssMetaData<ColorPicker,Number> COLOR_RECT_Y =
                new CssMetaData<>("-fx-color-rect-y", SizeConverter.getInstance(), 0) {
                    @Override public boolean isSettable(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return !skin.colorRectY.isBound();
                    }
                    @Override public StyleableProperty<Number> getStyleableProperty(ColorPicker n) {
                        final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                        return skin.colorRectY;
                    }
                };
        private static final CssMetaData<ColorPicker,String> GRAPHIC =
            new CssMetaData<>("-fx-graphic", StringConverter.getInstance()) {
                @Override public boolean isSettable(ColorPicker n) {
                    final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                    return !skin.imageUrl.isBound();
                }
                @Override public StyleableProperty<String> getStyleableProperty(ColorPicker n) {
                    final ColorPickerSkin skin = (ColorPickerSkin) n.getSkin();
                    return skin.imageUrl;
                }
            };
        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(ComboBoxBaseSkin.getClassCssMetaData());
            styleables.add(COLOR_LABEL_VISIBLE);
            styleables.add(COLOR_RECT_WIDTH);
            styleables.add(COLOR_RECT_HEIGHT);
            styleables.add(COLOR_RECT_X);
            styleables.add(COLOR_RECT_Y);
            styleables.add(GRAPHIC);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /** {@inheritDoc} */
    @Override protected javafx.util.StringConverter<Color> getConverter() {
        return null;
    }

    /**
     * ColorPicker does not use a main text field, so this method has been
     * overridden to return null.
     */
    @Override protected TextField getEditor() {
        return null;
    }
}
