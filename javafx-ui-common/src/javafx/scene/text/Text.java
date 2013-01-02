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
import java.util.Collections;
import java.util.List;

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

import com.sun.javafx.css.StyleableBooleanProperty;
import com.sun.javafx.css.StyleableDoubleProperty;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.TransformedShape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.accessible.AccessibleText;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.PGSpan;
import com.sun.javafx.sg.PGShape.Mode;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.accessible.providers.AccessibleProvider;
import com.sun.javafx.accessible.AccessibleNode;

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
 */
@DefaultProperty("text")
public class Text extends Shape {

    private TextLayout layout;
    private static final PathElement[] EMPTY_PATH_ELEMENT_ARRAY = new PathElement[0];

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected final PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGSpan();
    }

    private PGSpan getPGSpan() {
        return (PGSpan) impl_getPGNode();
    }

    /**
     * Creates an empty instance of Text.
     */
    public Text() {
        InvalidationListener listener = new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                checkSpan();
                checkOrientation();
            }
        };
        parentProperty().addListener(listener);
        managedProperty().addListener(listener);
        setPickOnBounds(true);
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

    private boolean isSpan;
    private boolean isSpan() {
        return isSpan;
    }

    private void checkSpan() {
        isSpan = isManaged() && getParent() instanceof TextFlow;
    }

    private void checkOrientation() {
        if (!isSpan()) {
            /* Using impl_transformsChanged to detect for orientation change.
             * This can be improved if EffectiveNodeOrientation becomes a
             * property. See http://javafx-jira.kenai.com/browse/RT-26140
             */
            NodeOrientation orientation = getEffectiveNodeOrientation();
            boolean rtl =  orientation == NodeOrientation.RIGHT_TO_LEFT;
            int dir = rtl ? TextLayout.DIRECTION_RTL : TextLayout.DIRECTION_LTR;
            TextLayout layout = getTextLayout();
            if (layout.setDirection(dir)) {
                needsTextLayout();
            }
        }
    }

    @Deprecated
    public void impl_transformsChanged() {
        super.impl_transformsChanged();
        checkOrientation();
    }

    @Override
    public boolean isAutomaticallyMirrored() {
        return false;
    }

    private void needsFullTextLayout() {
        if (isSpan()) {
            /* Create new text span every time the font or text changes
             * so the text layout can see that the content has changed.
             */
            textSpan = null;

            /* Relies on impl_geomChanged() to request text flow to relayout */
        } else {
            TextLayout layout = getTextLayout();
            String string = getTextInternal();
            Object font = getFontInternal();
            layout.setContent(string, font);
        }
        needsTextLayout();
    }

    private void needsTextLayout() {
        textRuns = null;
        impl_geomChanged();
        impl_markDirty(DirtyBits.NODE_CONTENTS);
    }

    private TextSpan textSpan;
    TextSpan getTextSpan() {
        if (textSpan == null) {
            textSpan = new TextSpan() {
                @Override public String getText() {
                    return getTextInternal();
                }
                @Override public Object getFont() {
                    return getFontInternal();
                }
                @Override public RectBounds getBounds() {
                    return null;
                }
            };
        }
        return textSpan;
    }

    private TextLayout getTextLayout() {
        if (isSpan()) {
            layout = null;
            TextFlow parent = (TextFlow)getParent();
            return parent.getTextLayout();
        }
        if (layout == null) {
            TextLayoutFactory factory = Toolkit.getToolkit().getTextLayoutFactory();
            layout = factory.createLayout();
            String string = getTextInternal();
            Object font = getFontInternal();
            TextAlignment alignment = getTextAlignment();
            if (alignment == null) alignment = DEFAULT_TEXT_ALIGNMENT;
            layout.setContent(string, font);
            layout.setAlignment(alignment.ordinal());
            layout.setLineSpacing((float)getLineSpacing());
            layout.setWrapWidth((float)getWrappingWidth());
            if (getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT) {
                layout.setDirection(TextLayout.DIRECTION_RTL);
            } else {
                layout.setDirection(TextLayout.DIRECTION_LTR);
            }
        }
        return layout;
    }

    private GlyphList[] textRuns = null;
    private BaseBounds spanBounds = new RectBounds(); /* relative to the textlayout */
    private boolean spanBoundsInvalid = true;

    void layoutSpan(GlyphList[] runs) {
        /* Sometimes a property change in the text node will causes layout in 
         * text flow. In this case all the dirty bits are already clear and no 
         * extra work is necessary. Other times the layout is caused by changes  
         * in the text flow object (wrapping width and text alignment for example).
         * In the second case the dirty bits must be set here using 
         * needsTextLayout(). Note that needsTextLayout() uses impl_geomChanged() 
         * which causes another (undesired) layout request in the parent.
         * In general this is not a problem because shapes are not resizable and 
         * region do not propagate layout changes to the parent.
         * This is a special case where a shape is resized by the parent during
         * layoutChildren().  See TextFlow#requestLayout() for information how 
         * text flow deals with this situation.
         */
        needsTextLayout();

        spanBoundsInvalid = true;
        int count = 0;
        TextSpan span = getTextSpan();
        for (int i = 0; i < runs.length; i++) {
            GlyphList run = runs[i];
            if (run.getTextSpan() == span) {
                count++;
            }
        }
        textRuns = new GlyphList[count];
        count = 0;
        for (int i = 0; i < runs.length; i++) {
            GlyphList run = runs[i];
            if (run.getTextSpan() == span) {
                textRuns[count++] = run;
            }
        }
    }

    BaseBounds getSpanBounds() {
        if (spanBoundsInvalid) {
            GlyphList[] runs = getRuns();
            if (runs.length != 0) {
                float left = Float.POSITIVE_INFINITY;
                float top = Float.POSITIVE_INFINITY;
                float right = 0;
                float bottom = 0;
                for (int i = 0; i < runs.length; i++) {
                    GlyphList run = runs[i];
                    com.sun.javafx.geom.Point2D location = run.getLocation();
                    float width = run.getWidth();
                    float height = run.getLineBounds().getHeight();
                    left = Math.min(location.x, left);
                    top = Math.min(location.y, top);
                    right = Math.max(location.x + width, right);
                    bottom = Math.max(location.y + height, bottom);
                }
                spanBounds = spanBounds.deriveWithNewBounds(left, top, 0,
                                                            right, bottom, 0);
            } else {
                spanBounds = spanBounds.makeEmpty();
            }
            spanBoundsInvalid = false;
        }
        return spanBounds;
    }

    private GlyphList[] getRuns() {
        if (textRuns != null) return textRuns;
        if (isSpan()) {
            /* List of run is initialized when the TextFlow layout the children */
            getParent().layout();
        } else {
            TextLayout layout = getTextLayout();
            textRuns = layout.getRuns();
        }
        return textRuns;
    }

    private com.sun.javafx.geom.Shape getShape() {
        TextLayout layout = getTextLayout();
        /* TextLayout has the text shape cached */
        int type = TextLayout.TYPE_TEXT;
        TextSpan filter = null;
        if (isSpan()) {
            /* Spans are always relative to the top */
            type |= TextLayout.TYPE_TOP;
            filter = getTextSpan();
        } else {
            /* Relative to baseline (first line)
             * This shape can be translate in the y axis according
             * to text origin, see impl_configShape().
             */
            type |= TextLayout.TYPE_BASELINE;
        }
        return layout.getShape(type, filter);
    }

    private BaseBounds getVisualBounds() {
        return getShape().getBounds();
    }

    private BaseBounds getLogicalBounds() {
        TextLayout layout = getTextLayout();
        /* TextLayout has the bounds cached */
        return layout.getBounds();
    }

    /**
     * Defines text string that is to be displayed.
     *
     * @defaultValue empty string
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
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "text"; }
                @Override  public void invalidated() {
                    needsFullTextLayout();
                    setImpl_selectionStart(-1);
                    setImpl_selectionEnd(-1);
                    setImpl_caretPosition(-1);
                    setImpl_caretBias(true);

                    // MH: Functionality copied from store() method,
                    // which was removed.
                    // Wonder what should happen if text is bound
                    //  and becomes null?
                    final String value = get();
                    if ((value == null) && !isBound()) {
                        set("");
                    }
                }
            };
        }
        return text;
    }

    /**
     * Defines the X coordinate of text origin.
     *
     * @defaultValue 0
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
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "x"; }
                @Override public void invalidated() {
                    impl_geomChanged();
                }
            };
        }
        return x;
    }

    /**
     * Defines the Y coordinate of text origin.
     *
     * @defaultValue 0
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
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "y"; }
                @Override public void invalidated() {
                    impl_geomChanged();
                }
            };
        }
        return y;
    }

    /**
     * Defines the font of text.
     *
     * @defaultValue Font{}
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
    private Object getFontInternal() {
        Font font = getFont();
        if (font == null) font = Font.getDefault();
        return font.impl_getNativeFont();
    }

    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "font"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.FONT;
                }
                @Override public void invalidated() {
                    needsFullTextLayout();
                    impl_markDirty(DirtyBits.TEXT_FONT);
                }
            };
        }
        return font;
    }

    public final void setTextOrigin(VPos value) {
        textOriginProperty().set(value);
    }

    public final VPos getTextOrigin() {
        if (attributes == null || attributes.textOrigin == null) {
            return DEFAULT_TEXT_ORIGIN;
        }
        return attributes.getTextOrigin();
    }

    /**
     * Defines the origin of text coordinate system in local coordinates.
     * Note: in case multiple rows are rendered {@code VPos.BASELINE} and
     * {@code VPos.TOP} define the origin of the top row while
     * {@code VPos.BOTTOM} defines the origin of the bottom row.
     *
     * @defaultValue VPos.BASELINE
     */
    public final ObjectProperty<VPos> textOriginProperty() {
        return getTextAttribute().textOriginProperty();
    }

    public final void setBoundsType(TextBoundsType value) {
        boundsTypeProperty().set(value);
    }

    public final TextBoundsType getBoundsType() {
        if (attributes == null || attributes.boundsType == null) {
            return DEFAULT_BOUNDS_TYPE;
        }
        return attributes.getBoundsType();
    }

    /**
     * Determines how the bounds of the text node are calculated.
     * Logical bounds is a more appropriate default for text than
     * the visual bounds. See {@code TextBoundsType} for more information.
     *
     * @defaultValue TextBoundsType.LOGICAL
     * @since JavaFX 1.3
     */
    public final ObjectProperty<TextBoundsType> boundsTypeProperty() {
        return getTextAttribute().boundsTypeProperty();
    }

    /**
     * Defines a width constraint for the text in user space coordinates,
     * e.g. pixels, not glyph or character count.
     * If the value is {@code > 0} text will be line wrapped as needed
     * to satisfy this constraint.
     *
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
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "wrappingWidth"; }
                @Override public void invalidated() {
                    if (!isSpan()) {
                        TextLayout layout = getTextLayout();
                        if (layout.setWrapWidth((float)get())) {
                            needsTextLayout();
                        } else {
                            impl_geomChanged();
                        }
                    }
                }
            };
        }
        return wrappingWidth;
    }

    public final void setUnderline(boolean value) {
        underlineProperty().set(value);
    }

    public final boolean isUnderline() {
        if (attributes == null || attributes.underline == null) {
            return DEFAULT_UNDERLINE;
        }
        return attributes.isUnderline();
    }

    /**
     * Defines if each line of text should have a line below it.
     *
     * @defaultValue false
     */
    public final BooleanProperty underlineProperty() {
        return getTextAttribute().underlineProperty();
    }

    public final void setStrikethrough(boolean value) {
        strikethroughProperty().set(value);
    }

    public final boolean isStrikethrough() {
        if (attributes == null || attributes.strikethrough == null) {
            return DEFAULT_STRIKETHROUGH;
        }
        return attributes.isStrikethrough();
    }

    /**
     * Defines if each line of text should have a line through it.
     *
     * @defaultValue false
     */
    public final BooleanProperty strikethroughProperty() {
        return getTextAttribute().strikethroughProperty();
    }

    public final void setTextAlignment(TextAlignment value) {
        textAlignmentProperty().set(value);
    }

    public final TextAlignment getTextAlignment() {
        if (attributes == null || attributes.textAlignment == null) {
            return DEFAULT_TEXT_ALIGNMENT;
        }
        return attributes.getTextAlignment();
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
     * @defaultValue TextAlignment.LEFT
     */   
    public final ObjectProperty<TextAlignment> textAlignmentProperty() {
        return getTextAttribute().textAlignmentProperty();
    }

    public final void setLineSpacing(double spacing) {
        lineSpacingProperty().set(spacing);
    }

    public final double getLineSpacing() {
        if (attributes == null || attributes.lineSpacing == null) {
            return DEFAULT_LINE_SPACING;
        }
        return attributes.getLineSpacing();
    }

    /**
     * Defines the vertical space in pixel between lines.
     *
     * @defaultValue 0
     *
     * @since 8.0
     */
    public final DoubleProperty lineSpacingProperty() {
        return getTextAttribute().lineSpacingProperty();
    }

    @Override
    public final double getBaselineOffset() {
        return baselineOffsetProperty().get();
    }

    /**
     * The 'alphabetic' (or roman) baseline offset from the Text node's
     * layoutBounds.minY location.
     * The value typically corresponds to the max ascent of the font.
     *
     * @since JavaFX 1.3
     */
    public final ReadOnlyDoubleProperty baselineOffsetProperty() {
        return getTextAttribute().baselineOffsetProperty();
    }

    /**
     * Specifies a requested font smoothing type : gray or LCD.
     *
     * The width of the bounding box is defined by the widest row.
     *
     * Note: LCD mode doesn't apply in numerous cases, such as various
     * compositing modes, where effects are applied and very large glyphs.
     *
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
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "fontSmoothingType"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.FONT_SMOOTHING_TYPE;
                }
                @Override public void invalidated() {
                    impl_markDirty(DirtyBits.TEXT_ATTRS);
                    impl_geomChanged();
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
     * @since JavaFX 1.3
     */
    //@GenerateProperty private boolean pickOnBounds = true;

    // private API to enable cursor and selection for text editing control

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected final void impl_geomChanged() {
        super.impl_geomChanged();
        if (attributes != null) {
            if (attributes.impl_caretBinding != null) {
                attributes.impl_caretBinding.invalidate();
            }
            if (attributes.impl_selectionBinding != null) {
                attributes.impl_selectionBinding.invalidate();
            }
        }
        impl_markDirty(DirtyBits.NODE_GEOMETRY);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] getImpl_selectionShape() {
        return impl_selectionShapeProperty().get();
    }

    /**
     * Shape of selection in local coordinates. 
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final ReadOnlyObjectProperty<PathElement[]> impl_selectionShapeProperty() {
        return getTextAttribute().impl_selectionShapeProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_selectionStart(int value) {
        if (value == -1 && 
                (attributes == null || attributes.impl_selectionStart == null)) {
            return;
        }
        impl_selectionStartProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_selectionStart() {
        if (attributes == null || attributes.impl_selectionStart == null) {
            return DEFAULT_SELECTION_START;
        }
        return attributes.getImpl_selectionStart();
    }

    /**
     * Selection start index in the content. 
     * set to {@code -1} to unset selection.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_selectionStartProperty() {
        return getTextAttribute().impl_selectionStartProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_selectionEnd(int value) {
        if (value == -1 && 
                (attributes == null || attributes.impl_selectionEnd == null)) {
            return;
        }
        impl_selectionEndProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_selectionEnd() {
        if (attributes == null || attributes.impl_selectionEnd == null) {
            return DEFAULT_SELECTION_END;
        }
        return attributes.getImpl_selectionEnd();
    }

    /**
     * Selection end index in the content. 
     * set to {@code -1} to unset selection.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_selectionEndProperty() {
        return getTextAttribute().impl_selectionEndProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final ObjectProperty<Paint> impl_selectionFillProperty() {
        return getTextAttribute().impl_selectionFillProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] getImpl_caretShape() {
        return impl_caretShapeProperty().get();
    }

    /**
     * Shape of caret in local coordinates.
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
    */
    @Deprecated
    public final ReadOnlyObjectProperty<PathElement[]> impl_caretShapeProperty() {
        return getTextAttribute().impl_caretShapeProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_caretPosition(int value) {
        if (value == -1 && 
                (attributes == null || attributes.impl_caretPosition == null)) {
            return;
        }
        impl_caretPositionProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final int getImpl_caretPosition() {
        if (attributes == null || attributes.impl_caretPosition == null) {
            return DEFAULT_CARET_POSITION;
        }
        return attributes.getImpl_caretPosition();
    }

    /**
     * caret index in the content. 
     * set to {@code -1} to unset caret.
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_caretPositionProperty() {
        return getTextAttribute().impl_caretPositionProperty();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final void setImpl_caretBias(boolean value) {
        if (value && (attributes == null || attributes.impl_caretBias == null)) {
            return;
        }
        impl_caretBiasProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final boolean isImpl_caretBias() {
        if (attributes == null || attributes.impl_caretBias == null) {
            return DEFAULT_CARET_BIAS;
        } 
        return getTextAttribute().isImpl_caretBias();
    }

    /**
     * caret bias in the content. true means a bias towards forward character
     * (true=leading/false=trailing)
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final BooleanProperty impl_caretBiasProperty() {
        return getTextAttribute().impl_caretBiasProperty();
    }

    /**
     * Maps local point to index in the content.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final HitInfo impl_hitTestChar(Point2D point) {
        if (point == null) return null;
        TextLayout layout = getTextLayout();
        double x = point.getX() - getX();
        double y = point.getY() - getY() + getYRendering();
        return layout.getHitInfo((float)x, (float)y);
    }

    private PathElement[] getRange(int start, int end, int type) {
        int length = getTextInternal().length();
        if (0 <= start && start < end  && end <= length) {
            TextLayout layout = getTextLayout();
            float x = (float)getX();
            float y = (float)getY() - getYRendering();
            return layout.getRange(start, end, type, x, y);
        }
        return EMPTY_PATH_ELEMENT_ARRAY;
    }

    /**
     * Returns shape for the range of the text in local coordinates.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] impl_getRangeShape(int start, int end) {
        return getRange(start, end, TextLayout.TYPE_TEXT);
    }

    /**
     * Returns shape for the underline in local coordinates.
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final PathElement[] impl_getUnderlineShape(int start, int end) {
        return getRange(start, end, TextLayout.TYPE_UNDERLINE);
    }

    /**
     * Shows/Hides on-screen keyboard if available (mobile platform)
     *
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    public final void impl_displaySoftwareKeyboard(boolean display) {
    }

    private float getYAdjustment(BaseBounds bounds) {
        VPos origin = getTextOrigin();
        if (origin == null) origin = DEFAULT_TEXT_ORIGIN;
        switch (origin) {
        case TOP: return -bounds.getMinY();
        case BASELINE: return 0;
        case CENTER: return -bounds.getMinY() - bounds.getHeight() / 2;
        case BOTTOM: return -bounds.getMinY() - bounds.getHeight();
        default: return 0;
        }
    }

    private float getYRendering() {
        /* Always logical for rendering */
        BaseBounds bounds = getLogicalBounds();

        VPos origin = getTextOrigin();
        if (origin == null) origin = DEFAULT_TEXT_ORIGIN;
        if (getBoundsType() == TextBoundsType.VISUAL) {
            BaseBounds vBounds = getVisualBounds();
            float delta = vBounds.getMinY() - bounds.getMinY();
            switch (origin) {
            case TOP: return delta;
            case BASELINE: return -vBounds.getMinY() + delta;
            case CENTER: return vBounds.getHeight() / 2 + delta;
            case BOTTOM: return vBounds.getHeight() + delta;
            default: return 0;
            }
        } else {
            switch (origin) {
            case TOP: return 0;
            case BASELINE: return -bounds.getMinY();
            case CENTER: return bounds.getHeight() / 2;
            case BOTTOM: return bounds.getHeight();
            default: return 0;
            }
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected final Bounds impl_computeLayoutBounds() {
        if (isSpan()) {
            BaseBounds bounds = getSpanBounds();
            double width = bounds.getWidth();
            double height = bounds.getHeight();
            return new BoundingBox(0, 0, width, height);
        }

        if (getBoundsType() == TextBoundsType.VISUAL) {
            /* In Node the layout bounds is computed based in the geom
             * bounds and in Shape the geom bounds is computed based
             * on the shape (generated here in #configShape()) */
            return super.impl_computeLayoutBounds();
        }
        BaseBounds bounds = getLogicalBounds();
        double x = bounds.getMinX() + getX();
        double y = bounds.getMinY() + getY() + getYAdjustment(bounds);
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double wrappingWidth = getWrappingWidth();
        if (wrappingWidth != 0) width = wrappingWidth;
        return new BoundingBox(x, y, width, height);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public final BaseBounds impl_computeGeomBounds(BaseBounds bounds,
                                                   BaseTransform tx) {
        if (isSpan()) {
            if (impl_mode != Mode.FILL && getStrokeType() != StrokeType.INSIDE) {
                return super.impl_computeGeomBounds(bounds, tx);
            }

            TextLayout layout = getTextLayout();
            bounds = layout.getBounds(getTextSpan(), bounds);
            BaseBounds spanBounds = getSpanBounds();
            float minX = bounds.getMinX() - spanBounds.getMinX();
            float minY = bounds.getMinY() - spanBounds.getMinY();
            float maxX = minX + bounds.getWidth();
            float maxY = minY + bounds.getHeight();
            bounds = bounds.deriveWithNewBounds(minX, minY, 0, maxX, maxY, 0);
            return tx.transform(bounds, bounds);
        }

        if (getBoundsType() == TextBoundsType.VISUAL) {
            if (getTextInternal().length() == 0 || impl_mode == Mode.EMPTY) {
                return bounds.makeEmpty();
            }

            /* Let the super class compute the bounds using shape */
            return super.impl_computeGeomBounds(bounds, tx);
        }

        BaseBounds textBounds = getLogicalBounds();
        float x = textBounds.getMinX() + (float)getX();
        float yadj = getYAdjustment(textBounds);
        float y = textBounds.getMinY() + yadj + (float)getY();
        float width = textBounds.getWidth();
        float height = textBounds.getHeight();
        float wrappingWidth = (float)getWrappingWidth();
        if (wrappingWidth > width) width = wrappingWidth;
        textBounds = new RectBounds(x, y, x + width, y + height);

        /* handle stroked text */
        if (impl_mode != Mode.FILL && getStrokeType() != StrokeType.INSIDE) {
            bounds =
                super.impl_computeGeomBounds(bounds,
                                             BaseTransform.IDENTITY_TRANSFORM);
        } else {
            TextLayout layout = getTextLayout();
            bounds = layout.getBounds(null, bounds);
            x = bounds.getMinX() + (float)getX();
            width = bounds.getWidth();
            bounds = bounds.deriveWithNewBounds(x, y, 0, x + width, y + height, 0);
        }

        bounds = bounds.deriveWithUnion(textBounds);
        return tx.transform(bounds, bounds);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected final boolean impl_computeContains(double localX, double localY) {
        //TODO Presently only support bounds based picking.
        return true;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public final com.sun.javafx.geom.Shape impl_configShape() {
        if (impl_mode == Mode.EMPTY || getTextInternal().length() == 0) {
            return new Path2D();
        }
        com.sun.javafx.geom.Shape shape = getShape();
        float x, y;
        if (isSpan()) {
            BaseBounds bounds = getSpanBounds();
            x = -bounds.getMinX();
            y = -bounds.getMinY();
        } else {
            x = (float)getX();
            y = getYAdjustment(getVisualBounds()) + (float)getY();
        }
        return TransformedShape.translatedShape(shape, x, y);
    }

   /***************************************************************************
    *                                                                         *
    *                            Stylesheet Handling                          *
    *                                                                         *
    **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {

         private static final CssMetaData<Text,Font> FONT =
            new CssMetaData.FONT<Text>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(Text node) {
                return node.font == null || !node.font.isBound();
            }

            @Override
            public WritableValue<Font> getWritableValue(Text node) {
                return node.fontProperty();
            }
         };

         private static final CssMetaData<Text,Boolean> UNDERLINE =
            new CssMetaData<Text,Boolean>("-fx-underline",
                 BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null ||
                       node.attributes.underline == null ||
                      !node.attributes.underline.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Text node) {
                return node.underlineProperty();
            }
         };

         private static final CssMetaData<Text,Boolean> STRIKETHROUGH =
            new CssMetaData<Text,Boolean>("-fx-strikethrough",
                 BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null ||
                       node.attributes.strikethrough == null ||
                      !node.attributes.strikethrough.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(Text node) {
                return node.strikethroughProperty();
            }
         };

         private static final
             CssMetaData<Text,TextAlignment> TEXT_ALIGNMENT =
                 new CssMetaData<Text,TextAlignment>("-fx-text-alignment",
                 new EnumConverter<TextAlignment>(TextAlignment.class),
                 TextAlignment.LEFT) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null ||
                       node.attributes.textAlignment == null ||
                      !node.attributes.textAlignment.isBound();
            }

            @Override
            public WritableValue<TextAlignment> getWritableValue(Text node) {
                return node.textAlignmentProperty();
            }
         };

         private static final CssMetaData<Text,VPos> TEXT_ORIGIN =
                 new CssMetaData<Text,VPos>("-fx-text-origin",
                 new EnumConverter<VPos>(VPos.class),
                 VPos.BASELINE) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null || 
                       node.attributes.textOrigin == null || 
                      !node.attributes.textOrigin.isBound();
            }

            @Override
            public WritableValue<VPos> getWritableValue(Text node) {
                return node.textOriginProperty();
            }
         };

         private static final CssMetaData<Text,FontSmoothingType>
             FONT_SMOOTHING_TYPE =
             new CssMetaData<Text,FontSmoothingType>(
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

         private static final
             CssMetaData<Text,Number> LINE_SPACING =
                 new CssMetaData<Text,Number>("-fx-line-spacing",
                 SizeConverter.getInstance(), 0) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null ||
                       node.attributes.lineSpacing == null ||
                      !node.attributes.lineSpacing.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(Text node) {
                return node.lineSpacingProperty();
            }
         };

	 private final static List<CssMetaData> STYLEABLES;
         static {
            final List<CssMetaData> styleables =
                new ArrayList<CssMetaData>(Shape.getClassCssMetaData());
            Collections.addAll(styleables,
                FONT,
                UNDERLINE,
                STRIKETHROUGH,
                TEXT_ALIGNMENT,
                TEXT_ORIGIN,
                FONT_SMOOTHING_TYPE,
                LINE_SPACING
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }

    @SuppressWarnings("deprecation")
    private void updatePGText() {
        PGSpan peer = getPGSpan();
        if (impl_isDirty(DirtyBits.TEXT_ATTRS)) {
            peer.setUnderline(isUnderline());
            peer.setStrikethrough(isStrikethrough());
            FontSmoothingType smoothing = getFontSmoothingType();
            if (smoothing == null) smoothing = FontSmoothingType.GRAY;
            peer.setFontSmoothingType(smoothing.ordinal());
        }
        if (impl_isDirty(DirtyBits.TEXT_FONT)) {
            peer.setFont(getFontInternal());
        }
        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            peer.setGlyphs(getRuns());
        }
        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            if (isSpan()) {
                BaseBounds spanBounds = getSpanBounds();
                peer.setLayoutLocation(spanBounds.getMinX(), spanBounds.getMinY());
            } else {
                float x = (float)getX();
                float y = (float)getY();
                float yadj = getYRendering();
                peer.setLayoutLocation(-x, yadj - y);
            }
        }
        if (impl_isDirty(DirtyBits.TEXT_SELECTION)) {
            Object fillObj = null;
            int start = getImpl_selectionStart();
            int end = getImpl_selectionEnd();
            int length = getTextInternal().length();
            if (0 <= start && start < end  && end <= length) {
                Paint fill = impl_selectionFillProperty().get();
                fillObj = fill != null ? fill.impl_getPlatformPaint() : null;
            }
            peer.setSelection(start, end, fillObj);
        }
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended
     * for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public final void impl_updatePG() {
        super.impl_updatePG();
        updatePGText();
    }

    private AccessibleNode accText ;
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated public AccessibleProvider impl_getAccessible() {
        if( accText == null)
            accText = new AccessibleText(this);
        return (AccessibleProvider)accText ;
    }
    
    /***************************************************************************
     *                                                                         *
     *                       Seldom Used Properties                            *
     *                                                                         *
     **************************************************************************/

    private TextAttribute attributes;
    
    private TextAttribute getTextAttribute() {
        if (attributes == null) {
            attributes = new TextAttribute();
        }
        return attributes;
    }
    
    private static final VPos DEFAULT_TEXT_ORIGIN = VPos.BASELINE;
    private static final TextBoundsType DEFAULT_BOUNDS_TYPE = TextBoundsType.LOGICAL;
    private static final boolean DEFAULT_UNDERLINE = false;
    private static final boolean DEFAULT_STRIKETHROUGH = false;
    private static final TextAlignment DEFAULT_TEXT_ALIGNMENT = TextAlignment.LEFT;
    private static final double DEFAULT_LINE_SPACING = 0;
    private static final int DEFAULT_CARET_POSITION = -1;
    private static final int DEFAULT_SELECTION_START = -1;
    private static final int DEFAULT_SELECTION_END = -1;
    private static final Color DEFAULT_SELECTION_FILL= Color.WHITE;
    private static final boolean DEFAULT_CARET_BIAS = true;
    
    private final class TextAttribute {

        private ObjectProperty<VPos> textOrigin;

        public final VPos getTextOrigin() {
            return textOrigin == null ? DEFAULT_TEXT_ORIGIN : textOrigin.get();
        }

        public final ObjectProperty<VPos> textOriginProperty() {
            if (textOrigin == null) {
                textOrigin = new StyleableObjectProperty<VPos>(DEFAULT_TEXT_ORIGIN) {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "textOrigin"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.TEXT_ORIGIN;
                    }
                    @Override public void invalidated() {
                        impl_geomChanged();
                    }
                };
            }
            return textOrigin;
        }
        
        private ObjectProperty<TextBoundsType> boundsType;

        public final TextBoundsType getBoundsType() {
            return boundsType == null ? DEFAULT_BOUNDS_TYPE : boundsType.get();
        }

        public final ObjectProperty<TextBoundsType> boundsTypeProperty() {
            if (boundsType == null) {
                boundsType =
                   new ObjectPropertyBase<TextBoundsType>(DEFAULT_BOUNDS_TYPE) {
                       @Override public Object getBean() { return Text.this; }
                       @Override public String getName() { return "boundsType"; }
                       @Override public void invalidated() {
                           impl_geomChanged();
                       }
                };
            }
            return boundsType;
        }
        
        private BooleanProperty underline;

        public final boolean isUnderline() {
            return underline == null ? DEFAULT_UNDERLINE : underline.get();
        }

        public final BooleanProperty underlineProperty() {
            if (underline == null) {
                underline = new StyleableBooleanProperty() {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "underline"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.UNDERLINE;
                    }
                    @Override public void invalidated() {
                        impl_markDirty(DirtyBits.TEXT_ATTRS);
                    }
                };
            }
            return underline;
        }
        
        private BooleanProperty strikethrough;

        public final boolean isStrikethrough() {
            return strikethrough == null ? DEFAULT_STRIKETHROUGH : strikethrough.get();
        }

        public final BooleanProperty strikethroughProperty() {
            if (strikethrough == null) {
                strikethrough = new StyleableBooleanProperty() {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "strikethrough"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.STRIKETHROUGH;
                    }
                    @Override public void invalidated() {
                        impl_markDirty(DirtyBits.TEXT_ATTRS);
                    }
                };
            }
            return strikethrough;
        }
        
        private ObjectProperty<TextAlignment> textAlignment;

        public final TextAlignment getTextAlignment() {
            return textAlignment == null ? DEFAULT_TEXT_ALIGNMENT : textAlignment.get();
        }

        public final ObjectProperty<TextAlignment> textAlignmentProperty() {
            if (textAlignment == null) {
                textAlignment =
                    new StyleableObjectProperty<TextAlignment>(DEFAULT_TEXT_ALIGNMENT) {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "textAlignment"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.TEXT_ALIGNMENT;
                    }
                    @Override public void invalidated() {
                        if (!isSpan()) {
                            TextAlignment alignment = get();
                            if (alignment == null) {
                                alignment = DEFAULT_TEXT_ALIGNMENT;
                            }
                            TextLayout layout = getTextLayout();
                            if (layout.setAlignment(alignment.ordinal())) {
                                needsTextLayout();
                            }
                        }
                    }
                };
            }
            return textAlignment;
        }
        
        private DoubleProperty lineSpacing;

        public final double getLineSpacing() {
            return lineSpacing == null ? DEFAULT_LINE_SPACING : lineSpacing.get();
        }

        public final DoubleProperty lineSpacingProperty() {
            if (lineSpacing == null) {
                lineSpacing =
                    new StyleableDoubleProperty(DEFAULT_LINE_SPACING) {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "lineSpacing"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.LINE_SPACING;
                    }
                    @Override public void invalidated() {
                        if (!isSpan()) {
                            TextLayout layout = getTextLayout();
                            if (layout.setLineSpacing((float)get())) {
                                needsTextLayout();
                            }
                        }
                    }
                };
            }
            return lineSpacing;
        }

        private ReadOnlyDoubleWrapper baselineOffset;

        public final ReadOnlyDoubleProperty baselineOffsetProperty() {
            if (baselineOffset == null) {
                baselineOffset = new ReadOnlyDoubleWrapper(Text.this, "baselineOffset") {
                    {bind(new DoubleBinding() {
                        {bind(fontProperty());}
                        @Override protected double computeValue() {
                            /* This method should never be used for spans.
                             * If it is, it will still returns the ascent 
                             * for the first line in the layout */
                            BaseBounds bounds = getLogicalBounds();
                            return -bounds.getMinY();
                        }
                    });}
                };
            }
            return baselineOffset.getReadOnlyProperty();
        }

        @Deprecated
        private ObjectProperty<PathElement[]> impl_selectionShape;
        private ObjectBinding<PathElement[]> impl_selectionBinding;

        @Deprecated
        public final ReadOnlyObjectProperty<PathElement[]> impl_selectionShapeProperty() {
            if (impl_selectionShape == null) {
                impl_selectionBinding = new ObjectBinding<PathElement[]>() {
                    {bind(impl_selectionStartProperty(), impl_selectionEndProperty());}
                    @Override protected PathElement[] computeValue() {
                        int start = getImpl_selectionStart();
                        int end = getImpl_selectionEnd();
                        return getRange(start, end, TextLayout.TYPE_TEXT);
                    }
              };
              impl_selectionShape = new SimpleObjectProperty<PathElement[]>(Text.this, "impl_selectionShape");
              impl_selectionShape.bind(impl_selectionBinding);
            }
            return impl_selectionShape;
        }

        private ObjectProperty<Paint> selectionFill;

        @Deprecated
        public final ObjectProperty<Paint> impl_selectionFillProperty() {
            if (selectionFill == null) {
                selectionFill = 
                    new ObjectPropertyBase<Paint>(DEFAULT_SELECTION_FILL) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "impl_selectionFill"; }
                        @Override protected void invalidated() {
                            impl_markDirty(DirtyBits.TEXT_SELECTION);
                        }
                    };
            }
            return selectionFill;
        }

        @Deprecated
        private IntegerProperty impl_selectionStart;

        @Deprecated
        public final int getImpl_selectionStart() {
            return impl_selectionStart == null ? DEFAULT_SELECTION_START : impl_selectionStart.get();
        }

        @Deprecated
        public final IntegerProperty impl_selectionStartProperty() {
            if (impl_selectionStart == null) {
                impl_selectionStart = 
                    new IntegerPropertyBase(DEFAULT_SELECTION_START) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "impl_selectionStart"; }
                        @Override protected void invalidated() {
                            impl_markDirty(DirtyBits.TEXT_SELECTION);
                        }
                };
            }
            return impl_selectionStart;
        }

        @Deprecated
        private IntegerProperty impl_selectionEnd;

        @Deprecated
        public final int getImpl_selectionEnd() {
            return impl_selectionEnd == null ? DEFAULT_SELECTION_END : impl_selectionEnd.get();
        }

        @Deprecated
        public final IntegerProperty impl_selectionEndProperty() {
            if (impl_selectionEnd == null) {
                impl_selectionEnd = 
                    new IntegerPropertyBase(DEFAULT_SELECTION_END) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "impl_selectionEnd"; }
                        @Override protected void invalidated() {
                            impl_markDirty(DirtyBits.TEXT_SELECTION);
                        }
                    };
            }
            return impl_selectionEnd;
        }

        @Deprecated
        private ObjectProperty<PathElement[]> impl_caretShape;
        private ObjectBinding<PathElement[]> impl_caretBinding;

        @Deprecated
        public final ReadOnlyObjectProperty<PathElement[]> impl_caretShapeProperty() {
            if (impl_caretShape == null) {
                impl_caretBinding = new ObjectBinding<PathElement[]>() {
                    {bind(impl_caretPositionProperty(), impl_caretBiasProperty());}
                    @Override protected PathElement[] computeValue() {
                        int pos = getImpl_caretPosition();
                        int length = getTextInternal().length();
                        if (0 <= pos && pos <= length) {
                            boolean bias = isImpl_caretBias();
                            float x = (float)getX();
                            float y = (float)getY() - getYRendering();
                            TextLayout layout = getTextLayout();
                            return layout.getCaretShape(pos, bias, x, y);
                        }
                        return EMPTY_PATH_ELEMENT_ARRAY;
                    }
                };
                impl_caretShape = new SimpleObjectProperty<PathElement[]>(Text.this, "impl_caretShape");
                impl_caretShape.bind(impl_caretBinding);
            }
            return impl_caretShape;
        }
        
        @Deprecated
        private IntegerProperty impl_caretPosition;

        @Deprecated
        public final int getImpl_caretPosition() {
            return impl_caretPosition == null ? DEFAULT_CARET_POSITION : impl_caretPosition.get();
        }

        @Deprecated
        public final IntegerProperty impl_caretPositionProperty() {
            if (impl_caretPosition == null) {
                impl_caretPosition =
                        new SimpleIntegerProperty(Text.this, "impl_caretPosition", DEFAULT_CARET_POSITION);
            }
            return impl_caretPosition;
        }
        
        @Deprecated
        private BooleanProperty impl_caretBias;

        @Deprecated
        public final boolean isImpl_caretBias() {
            return impl_caretBias == null ? DEFAULT_CARET_BIAS : impl_caretBias.get();
        }

        @Deprecated
        public final BooleanProperty impl_caretBiasProperty() {
            if (impl_caretBias == null) {
                impl_caretBias =
                        new SimpleBooleanProperty(Text.this, "impl_caretBias", DEFAULT_CARET_BIAS);
            }
            return impl_caretBias;
        }
    }

}
