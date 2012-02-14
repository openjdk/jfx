/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;

import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.FontConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.shape.PathUtils;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGShape;
import com.sun.javafx.sg.PGText;
import com.sun.javafx.tk.TextHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.DefaultProperty;
import javafx.beans.property.*;
import javafx.beans.value.WritableValue;

/**
 * The {@code Text} class defines a node that displays a text.
 *
 * Paragraphs are separated by {@code '\n'} and the text is wrapped on
 * paragraph boundaries.
 *
<PRE>
import javafx.scene.text.*;

Text t = new Text(10, 50, "This is a test");
t.setFont(new Font(20));
</PRE>
 *
<PRE>
import javafx.scene.text.*;

Text t = new Text();
text.setFont(new Font(20));
text.setText("First row\nSecond row");
</PRE>
 *
<PRE>
import javafx.scene.text.*;

Text t = new Text();
text.setFont(new Font(20));
text.setWrappingWidth(200);
text.setTextAlignment(TextAlignment.JUSTIFY)
text.setText("The quick brown fox jumps over the lazy dog");
</PRE>
 * @profile common
 */
@DefaultProperty("text")
public final class Text extends Shape {

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGText();
    }

    PGText getPGText() {
        return (PGText) impl_getPGNode();
    }

    /**
     * Creates an empty instance of Text.
     */
    public Text() {
        setPickOnBounds(true);
        getDecorationShapes();
        setBaselineOffset(Toolkit.getToolkit().getFontLoader().getFontMetrics(getFontInternal()).getAscent());
    }

    /**
     * Creates an instance of Text containing the given string.
     * @param text text to be contained in the instance
     */
    public Text(String text) {
        this();
        setText(text);
    }

    /**
     * Creates an instance of Text on the given coordinates containing the
     * given string.
     * @param x the horizontal position of the text
     * @param y the vertical position of the text
     * @param text text to be contained in the instance
     */
    public Text(double x, double y, String text) {
        this(text);
        setX(x);
        setY(y);
    }

    /**
     * Defines text string that is to be displayed.
     *
     * @defaultValue empty string
     * @profile common
     */
    private StringProperty text;

    public final void setText(String value) {
        if (value == null) value = "";
        textProperty().set(value);
    }

    public final String getText() {
        return text == null ? "" : text.get();
    }

    private String getTextInternal() {
        // this might return null in case of bound property
        String localText = getText();
        return localText == null ? "" : localText;
    }

    public final StringProperty textProperty() {
        if (text == null) {
            text = new StringPropertyBase("") {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_CONTENTS);
                    // If text property is invalid then text selection is also
                    // invalid.  There are different approaches that can be
                    // take here.  But we decided on a simplistic approach, for
                    // now, by resetting selection (start = -1, end = -1)
                    setImpl_selectionStart(-1);
                    setImpl_selectionEnd(-1);
                    impl_geomChanged();
                    // MH: Functionality copied from store() method, which was removed
                    // Wonder what should happen if text is bound and becomes null?
                    final String value = get();
                    if ((value == null) && !isBound()) {
                        set("");
                    }
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "text";
                }
            };
        }
        return text;
    }

    /**
     * Defines the X coordinate of text origin.
     *
     * @defaultValue 0
     * @profile common
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    /**
     * Defines the Y coordinate of text origin.
     *
     * @defaultValue 0
     * @profile common
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    /**
     * Defines the font of text.
     *
     * @defaultValue Font{}
     * @profile common
     */
    private ObjectProperty<Font> font;

    public final void setFont(Font value) {
        fontProperty().set(value);
    }

    public final Font getFont() {
        return font == null ? Font.getDefault() : font.get();
    }

    /**
     * Internally used safe version of getFont which never returns null.
     *
     * @return the font
     */
    private Font getFontInternal() {
        final Font fontValue = getFont();
        return (fontValue != null) ? fontValue : Font.getDefault();
    }

    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_FONT);
                    impl_geomChanged();
                    setBaselineOffset(Toolkit.getToolkit().getFontLoader().getFontMetrics(getFontInternal()).getAscent());
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.FONT;
                }
                
                @Override
                public Object getBean() {
                    return Text.this;
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
     * Defines the origin of text coordinate system in local coordinates.
     * Note: in case multiple rows are rendered {@code VPos.BASELINE} and
     * {@code VPos.TOP} define the origin of the top row while
     * {@code VPos.BOTTOM} defines the origin of the bottom row.
     *
     * @defaultValue VPos.BASELINE
     * @profile common
     */
    private ObjectProperty<VPos> textOrigin;


    public final void setTextOrigin(VPos value) {
        textOriginProperty().set(value);
    }

    public final VPos getTextOrigin() {
        return textOrigin == null ? VPos.BASELINE : textOrigin.get();
    }

    public final ObjectProperty<VPos> textOriginProperty() {
        if (textOrigin == null) {
            textOrigin = new StyleableObjectProperty<VPos>(VPos.BASELINE) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.TEXT_ORIGIN;
                }
                
                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "textOrigin";
                }
            };
        }
        return textOrigin;
    }

    /**
     * Determines how the bounds of the text node are calculated.
     * Logical bounds is a more appropriate default for text than
     * the visual bounds. See {@code TextBoundsType} for more information.
     *
     * @defaultValue TextBoundsType.LOGICAL
     * @profile common
     * @since JavaFX 1.3
     */
    private ObjectProperty<TextBoundsType> boundsType;

    public final void setBoundsType(TextBoundsType value) {
        boundsTypeProperty().set(value);
    }

    public final TextBoundsType getBoundsType() {
        return boundsType == null ? TextBoundsType.LOGICAL : boundsType.get();
    }

    public final ObjectProperty<TextBoundsType> boundsTypeProperty() {
        if (boundsType == null) {
            boundsType = new ObjectPropertyBase<TextBoundsType>(TextBoundsType.LOGICAL) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "boundsType";
                }
            };
        }
        return boundsType;
    }

    /**
     * Defines a width constraint for the text in user space coordinates,
     * e.g. pixels, not glyph or character count.
     * If the value is {@code > 0} text will be line wrapped as needed
     * to satisfy this constraint.
     *
     * @profile common
     * @defaultValue 0
     */
    private DoubleProperty wrappingWidth;

    public final void setWrappingWidth(double value) {
        wrappingWidthProperty().set(value);
    }

    public final double getWrappingWidth() {
        return wrappingWidth == null ? 0 : wrappingWidth.get();
    }

    public final DoubleProperty wrappingWidthProperty() {
        if (wrappingWidth == null) {
            wrappingWidth = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "wrappingWidth";
                }
            };
        }
        return wrappingWidth;
    }

    /**
     * Defines if each line of text should have a line below it.
     *
     * @profile common
     * @defaultValue false
     */
    private BooleanProperty underline;

    public final void setUnderline(boolean value) {
        underlineProperty().set(value);
    }

    public final boolean isUnderline() {
        return underline == null ? false : underline.get();
    }

    public final BooleanProperty underlineProperty() {
        if (underline == null) {
            underline = new StyleableBooleanProperty() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.UNDERLINE;
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "underline";
                }
            };
        }
        return underline;
    }

    /**
     * Defines if each line of text should have a line through it.
     *
     * @profile common
     * @defaultValue false
     */
    private BooleanProperty strikethrough;

    public final void setStrikethrough(boolean value) {
        strikethroughProperty().set(value);
    }

    public final boolean isStrikethrough() {
        return strikethrough == null ? false : strikethrough.get();
    }

    public final BooleanProperty strikethroughProperty() {
        if (strikethrough == null) {
            strikethrough = new StyleableBooleanProperty() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }


                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.STRIKETHROUGH;
                }
                
                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "strikethrough";
                }
            };
        }
        return strikethrough;
    }

    /**
     * Defines horizontal text alignment in the bounding box.
     *
     * The width of the bounding box is defined by the widest row.
     *
     * Note: In the case of a single line of text, where the width of the
     * node is determined by the width of the text, the alignment setting
     * has no effect.
     *
     * @profile common
     * @defaultValue TextAlignment.LEFT
     */
    private ObjectProperty<TextAlignment> textAlignment;

    public final void setTextAlignment(TextAlignment value) {
        textAlignmentProperty().set(value);
    }

    public final TextAlignment getTextAlignment() {
        return textAlignment == null ? TextAlignment.LEFT : textAlignment.get();
    }

    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        if (textAlignment == null) {
            textAlignment = new StyleableObjectProperty<TextAlignment>(TextAlignment.LEFT) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.TEXT_ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return Text.this;
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
     * The 'alphabetic' (or roman) baseline offset from the Text node's layoutBounds.minY location.
     * The value typically corresponds to the max ascent of the font.
     *
     * @profile common
     * @since JavaFX 1.3
     */
    //TODO(aim): not sure this needs to be a field vs. just a getter function that lazily computes it
    private ReadOnlyDoubleWrapper baselineOffset;

    private void setBaselineOffset(double value) {
        baselineOffsetPropertyImpl().set(value);
    }

    @Override
    public final double getBaselineOffset() {
        return baselineOffset == null ? 0.0 : baselineOffset.get();
    }

    public final ReadOnlyDoubleProperty baselineOffsetProperty() {
        return baselineOffsetPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper baselineOffsetPropertyImpl() {
        if (baselineOffset == null) {
            baselineOffset = new ReadOnlyDoubleWrapper(this, "baselineOffset");
        }
        return baselineOffset;
    }

    /**
     * Specifies a requested font smoothing type : gray or LCD.
     *
     * The width of the bounding box is defined by the widest row.
     *
     * Note: LCD mode doesn't apply in numerous cases, such as various
     * compositing modes, where effects are applied and very large glyphs.
     *
     * @profile common
     * @defaultValue FontSmoothingType.GRAY
     */
    private ObjectProperty<FontSmoothingType> fontSmoothingType;

    public final void setFontSmoothingType(FontSmoothingType value) {
        fontSmoothingTypeProperty().set(value);
    }

    public final FontSmoothingType getFontSmoothingType() {
        return fontSmoothingType == null ? 
            FontSmoothingType.GRAY : fontSmoothingType.get();
    }

    public final ObjectProperty<FontSmoothingType>
        fontSmoothingTypeProperty() {

        if (fontSmoothingType == null) {
            fontSmoothingType =
                new StyleableObjectProperty<FontSmoothingType>
                                               (FontSmoothingType.GRAY) {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
                }

                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.FONT_SMOOTHING_TYPE;
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "fontSmoothingType";
                }
            };
        }
        return fontSmoothingType;
    }

    /**
     * Defines how the picking computation is done for this text node when
     * triggered by a {@code MouseEvent} or a {@code contains} function call.
     *
     * If {@code pickOnBounds} is true, then picking is computed by
     * intersecting with the bounds of this text node, else picking is computed
     * by intersecting with the individual characters (geometric shape) of this
     * text node.
     * Picking based on bounds is more efficient and allows the spaces within
     * and between characters to be picked.
     *
     * @defaultValue true
     * @profile common
     * @since JavaFX 1.3
     */
    //@GenerateProperty private boolean pickOnBounds = true;

    // private API to enable cursor and selection for text editing control

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected void impl_geomChanged() {
        getDecorationShapes();
        super.impl_geomChanged();
    }
    //public-read var impl_selectionShape:PathElement[] = null;
    /**
     * Shape of selection in local coordinates.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    private ObjectProperty<PathElement[]> impl_selectionShape;
    //public-read var impl_selectionShape:PathElement[] = null;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_selectionShape(PathElement[] value) {
        impl_selectionShapeProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] getImpl_selectionShape() {
        return impl_selectionShape == null ? null : impl_selectionShape.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final ObjectProperty<PathElement[]> impl_selectionShapeProperty() {
        if (impl_selectionShape == null) {
            impl_selectionShape = new SimpleObjectProperty<PathElement[]>(
                                          this,
                                          "impl_selectionShape");
        }
        return impl_selectionShape;
    }

    /**
     * Selection start index in the content.
     * set to {@code -1} to unset selection.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    private IntegerProperty impl_selectionStart;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_selectionStart(int value) {
        impl_selectionStartProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_selectionStart() {
        return impl_selectionStart == null ? -1 : impl_selectionStart.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_selectionStartProperty() {
        if (impl_selectionStart == null) {
            impl_selectionStart = new IntegerPropertyBase(-1) {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_SELECTION);
                    getDecorationShapes();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "impl_selectionStart";
                }
            };
        }
        return impl_selectionStart;
    }

    /**
     * Selection end index in the content.
     * set to {@code -1} to unset selection.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    private IntegerProperty impl_selectionEnd;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_selectionEnd(int value) {
        impl_selectionEndProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_selectionEnd() {
        return impl_selectionEnd == null ? -1 : impl_selectionEnd.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_selectionEndProperty() {
        if (impl_selectionEnd == null) {
            impl_selectionEnd = new IntegerPropertyBase(-1) {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_SELECTION);
                    getDecorationShapes();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "impl_selectionEnd";
                }
            };
        }
        return impl_selectionEnd;
    }

    /**
     * stroke paint to be used for selected content.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_strokeOrFillChanged() {
        impl_markDirty(DirtyBits.TEXT_SELECTION);
    }

    /**
     * Shape of caret in local coordinates.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    /*public-read*/
    private ObjectProperty<PathElement[]> impl_caretShape;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_caretShape(PathElement[] value) {
        impl_caretShapeProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] getImpl_caretShape() {
        return impl_caretShape == null ? null : impl_caretShape.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final ObjectProperty<PathElement[]> impl_caretShapeProperty() {
        if (impl_caretShape == null) {
            impl_caretShape = new SimpleObjectProperty<PathElement[]>(
                                      this,
                                      "impl_caretShape");
        }
        return impl_caretShape;
    }

    /**
     * caret index in the content.
     * set to {@code -1} to unset caret.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    private IntegerProperty impl_caretPosition;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_caretPosition(int value) {
        impl_caretPositionProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_caretPosition() {
        return impl_caretPosition == null ? -1 : impl_caretPosition.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_caretPositionProperty() {
        if (impl_caretPosition == null) {
            impl_caretPosition = new IntegerPropertyBase(-1) {

                @Override
                protected void invalidated() {
                    getDecorationShapes();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "impl_caretPosition";
                }
            };
        }
        return impl_caretPosition;
    }

    /**
     * caret bias in the content. true means a bias towards forward charcter
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    private BooleanProperty impl_caretBias;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_caretBias(boolean value) {
        impl_caretBiasProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final boolean isImpl_caretBias() {
        return impl_caretBias == null ? true : impl_caretBias.get();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final BooleanProperty impl_caretBiasProperty() {
        if (impl_caretBias == null) {
            impl_caretBias = new BooleanPropertyBase(true) {

                @Override
                protected void invalidated() {
                    getDecorationShapes();
                }

                @Override
                public Object getBean() {
                    return Text.this;
                }

                @Override
                public String getName() {
                    return "impl_caretBias";
                }
            };
        }
        return impl_caretBias;
    }

    /**
     * Maps local point to index in the content.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public HitInfo impl_hitTestChar(Point2D point) {
        return Toolkit.getToolkit().convertHitInfoToFX(getTextHelper().getHitInfo((float)point.getX(), (float)point.getY()));
    }

    /**
     * Returns shape for the range of the text in local coordinates.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public PathElement[] impl_getRangeShape(int start, int end) {
        Object nativeShape =  getTextHelper().getRangeShape(start, end);
        return Toolkit.getToolkit().convertShapeToFXPath(nativeShape);
    }

    /**
     * Returns shape for the underline in local coordinates.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public PathElement[] impl_getUnderlineShape(int start, int end) {
        Object nativeShape =  getTextHelper().getUnderlineShape(start, end);
        return Toolkit.getToolkit().convertShapeToFXPath(nativeShape);
    }

    /**
     * updates decoration Shapes: selectionShape, caretShape
     */
    private void getDecorationShapes() {
        /*
         * TODO: if we do not read boundsInLocal our boundsInLocalListener
         * "on replace" does not work. It is updated once for null value and
         * never again. Richard?
         */
         // TODO!! There appears to be a bug here, where if I attempt to read
         // boundsInLocal at this time, then it will cause an invalid Bounds
         // to be created. Seems like an initialization order bug, maybe in the
         // compiler?
        //var t = boundsInLocal;

        if (getImpl_caretPosition() >= 0) {
            //convert insertion postiion into character index
            int charIndex = getImpl_caretPosition() - ((isImpl_caretBias()) ? 0 : 1);
            Scene.impl_setAllowPGAccess(true);
            Object nativeShape = getTextHelper().getCaretShape(charIndex, isImpl_caretBias());
            Scene.impl_setAllowPGAccess(false);
            setImpl_caretShape(Toolkit.getToolkit().convertShapeToFXPath(nativeShape));
        } else {
            setImpl_caretShape(null);
        }

        if (getImpl_selectionStart() >= 0 && getImpl_selectionEnd() >= 0) {
            Scene.impl_setAllowPGAccess(true);
            Object nativeShape = getTextHelper().getSelectionShape();
            Scene.impl_setAllowPGAccess(false);
            setImpl_selectionShape(Toolkit.getToolkit().convertShapeToFXPath(nativeShape));
        } else {
            setImpl_selectionShape(null);
        }
    }

    /**
     * Shows/Hides on-screen keyboard if available (mobile platform)
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public void impl_displaySoftwareKeyboard(boolean display) {
    }

    private TextHelper textHelper;
    private TextHelper getTextHelper() {
        if (textHelper == null) {
            textHelper = Toolkit.getToolkit().createTextHelper(this);
        }
        return textHelper;
    }

    /**
     * The cached layout bounds.
     * This is never null, but is frequently set to be
     * invalid whenever the bounds for the node have changed.
     */
    private RectBounds impl_layoutBounds = new RectBounds();
    private boolean impl_layoutBoundsInvalid = true;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_notifyLayoutBoundsChanged() {
        // REMIND: invalidate layout bounds
        impl_layoutBoundsInvalid = true;
        super.impl_notifyLayoutBoundsChanged();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public BaseBounds impl_computeLayoutBoundsInt(RectBounds bounds) {
        if (getBoundsType() == TextBoundsType.VISUAL) {
            // fast path for case where there simply isn't any text
            if (getTextInternal().equals("")) {
                return bounds.makeEmpty();
            }
        }
        // Even if the text is empty, for logical bounds we need to
        // return bounds which includes the height of the font.
        return getTextHelper().computeLayoutBounds(bounds);
    }

    /**
     * Returns layout bounds for a text node. Depending on bounds
     * reporting mode for this node, this may be logical or visual bounds.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected Bounds impl_computeLayoutBounds()
    {
        if (impl_layoutBoundsInvalid) {
            impl_computeLayoutBoundsInt(impl_layoutBounds);
            impl_layoutBoundsInvalid = false;
        }
        return new BoundingBox(
            impl_layoutBounds.getMinX(),
            impl_layoutBounds.getMinY(),
            impl_layoutBounds.getWidth(),
            impl_layoutBounds.getHeight());
    }


    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // fast path for case where neither fill nor stroke is set or where
        // there simply isn't any text. Applies only to VISUAL bounds.
        if ((impl_mode == PGShape.Mode.EMPTY || getTextInternal().equals("") &&
             getBoundsType() == TextBoundsType.VISUAL))
        {
            return bounds.makeEmpty();
        }

        // TODO: Scenario has this odd "isDegradedTransform" function for
        // handling an apparent bug on mac which happened when transform was
        // not identity but really close to it, which caused the bounds to
        // be computed as some enormous value. I'm not sure if this is a
        // problem yet in this implementation, but if so, then we need to do
        // the check right here.
        //if (isDegradedTransform(tx)) {
        //    tx = BaseTransform.IDENTITY_TRANSFORM;
        //}

        return getTextHelper().computeBounds(bounds, tx);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        // Need to call the TextHelper to do glyph (geometry) based picking.

        // Perform the expensive glyph (geometry) based picking
        // See the computeContains function in SGText.java for detail.
        return getTextHelper().contains((float)localX, (float)localY);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
	public com.sun.javafx.geom.Shape impl_configShape() {
        Object nativeShape = getTextHelper().getShape();
        final PathElement[] textPath =
                Toolkit.getToolkit().convertShapeToFXPath(nativeShape);

        return PathUtils.configShape(Arrays.asList(textPath), false);
    }

    private ObjectProperty<Paint> selectionFill;

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public ObjectProperty<Paint> impl_selectionFillProperty() {
        if (selectionFill == null) {
            selectionFill = new SimpleObjectProperty<Paint>(this, "selectionFill", Color.WHITE);
        }
        return selectionFill;
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {
         
         private static final StyleableProperty<Text,Font> FONT = 
            new StyleableProperty.FONT<Text>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(Text node) {
                return node.font == null || !node.font.isBound();
            }

            @Override
            public WritableValue<Font> getWritableValue(Text node) {
                return node.fontProperty();
            }
         };
         
         private static final StyleableProperty<Text,Boolean> UNDERLINE = 
            new StyleableProperty<Text,Boolean>("-fx-underline",
                 BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Text node) {
                return node.underline == null || !node.underline.isBound();
            }
                     
            @Override
            public WritableValue<Boolean> getWritableValue(Text node) {
                return node.underlineProperty();
            }

         };
         
         private static final StyleableProperty<Text,Boolean> STRIKETHROUGH = 
            new StyleableProperty<Text,Boolean>("-fx-strikethrough",
                 BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Text node) {
                return node.strikethrough == null || !node.strikethrough.isBound();
            }
                     
            @Override
            public WritableValue<Boolean> getWritableValue(Text node) {
                return node.strikethroughProperty();
            }

         };
         
         private static final StyleableProperty<Text,TextAlignment> TEXT_ALIGNMENT = 
                 new StyleableProperty<Text,TextAlignment>("-fx-text-alignment",
                 new EnumConverter<TextAlignment>(TextAlignment.class),
                 TextAlignment.LEFT) {

            @Override
            public boolean isSettable(Text node) {
                return node.textAlignment == null || !node.textAlignment.isBound();
            }

            @Override
            public WritableValue<TextAlignment> getWritableValue(Text node) {
                return node.textAlignmentProperty();
            }
         };
         
         private static final StyleableProperty<Text,VPos> TEXT_ORIGIN = 
                 new StyleableProperty<Text,VPos>("-fx-text-origin",
                 new EnumConverter<VPos>(VPos.class),
                 VPos.BASELINE) {

            @Override
            public boolean isSettable(Text node) {
                return node.textOrigin == null || !node.textOrigin.isBound();
            }

            @Override
            public WritableValue<VPos> getWritableValue(Text node) {
                return node.textOriginProperty();
            }
         };

         private static final StyleableProperty<Text,FontSmoothingType>
             FONT_SMOOTHING_TYPE = 
             new StyleableProperty<Text,FontSmoothingType>(
                 "-fx-font-smoothing-type",
                 new EnumConverter<FontSmoothingType>(FontSmoothingType.class),
                 FontSmoothingType.GRAY) {

            @Override
            public boolean isSettable(Text node) {
                return node.fontSmoothingType == null ||
                       !node.fontSmoothingType.isBound();
            }

            @Override
            public WritableValue<FontSmoothingType>
                                 getWritableValue(Text node) {

                return node.fontSmoothingTypeProperty();
            }
         };

         private static final List<StyleableProperty> STYLEABLES;
         static {
            final List<StyleableProperty> styleables =
                    new ArrayList<StyleableProperty>(Shape.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FONT,
                UNDERLINE,
                STRIKETHROUGH,
                TEXT_ALIGNMENT,
                TEXT_ORIGIN,
                FONT_SMOOTHING_TYPE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

         }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh. StyleableProperties is referenced
     * no earlier (and therefore loaded no earlier by the class loader) than
     * the moment that  impl_CSS_STYLEABLES() is called.
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Text.StyleableProperties.STYLEABLES;
    }

    private void updatePGText() {
        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            getPGText().setLocation((float)getX(), (float)getY());
        }
        if (impl_isDirty(DirtyBits.TEXT_ATTRS)) {
            PGText peer = getPGText();
            peer.setTextBoundsType(getBoundsType().ordinal());
            peer.setTextOrigin(getTextOrigin().ordinal());
            peer.setWrappingWidth((float)getWrappingWidth());
            peer.setUnderline(isUnderline());
            peer.setStrikethrough(isStrikethrough());
            peer.setTextAlignment(getTextAlignment().ordinal());
            peer.setFontSmoothingType(getFontSmoothingType().ordinal());
        }
        if (impl_isDirty(DirtyBits.TEXT_FONT)) {
            getPGText().setFont(getFontInternal().impl_getNativeFont());
        }
        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            getPGText().setText(getTextInternal());
        }
        if (impl_isDirty(DirtyBits.TEXT_SELECTION)) {
            if (getImpl_selectionStart() >= 0 && getImpl_selectionEnd() >= 0) {
                getPGText().setLogicalSelection(getImpl_selectionStart(),
                                                getImpl_selectionEnd());
                // getStroke and getFill can be null
                Paint strokePaint   = getStroke();
                Paint fillPaint     = selectionFill == null ? null : selectionFill.get();
                Object strokeObj = (strokePaint == null) ? null :
                                    strokePaint.impl_getPlatformPaint();
                Object fillObj = (fillPaint == null) ? null :
                                    fillPaint.impl_getPlatformPaint();

                getPGText().setSelectionPaint(strokeObj, fillObj);
            } else {
                // Deselect any PGText, in order to update selected text color
                getPGText().setLogicalSelection(0, 0);
            }
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        updatePGText();
    }
}
