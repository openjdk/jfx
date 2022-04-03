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

package javafx.scene.text;

import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.TransformedShape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.shape.ShapeHelper;
import com.sun.javafx.scene.shape.TextHelper;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGShape;
import com.sun.javafx.sg.prism.NGText;
import com.sun.javafx.scene.text.FontHelper;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;

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
 * @since JavaFX 2.0
 */
@DefaultProperty("text")
public class Text extends Shape {
    static {
        TextHelper.setTextAccessor(new TextHelper.TextAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((Text) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((Text) node).doUpdatePeer();
            }

            @Override
            public Bounds doComputeLayoutBounds(Node node) {
                return ((Text) node).doComputeLayoutBounds();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((Text) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((Text) node).doComputeContains(localX, localY);
            }

            @Override
            public void doGeomChanged(Node node) {
                ((Text) node).doGeomChanged();
            }

            @Override
            public com.sun.javafx.geom.Shape doConfigShape(Shape shape) {
                return ((Text) shape).doConfigShape();
            }
        });
    }

    private TextLayout layout;
    private static final PathElement[] EMPTY_PATH_ELEMENT_ARRAY = new PathElement[0];

    {
        // To initialize the class helper at the begining each constructor of this class
        TextHelper.initHelper(this);
    }

    /**
     * Creates an empty instance of Text.
     */
    public Text() {
        setAccessibleRole(AccessibleRole.TEXT);
        InvalidationListener listener = observable -> checkSpan();
        parentProperty().addListener(listener);
        managedProperty().addListener(listener);
        effectiveNodeOrientationProperty().addListener(observable -> checkOrientation());
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

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGText();
    }

    private boolean isSpan;
    private boolean isSpan() {
        return isSpan;
    }

    private void checkSpan() {
        isSpan = isManaged() && getParent() instanceof TextFlow;
        if (isSpan() && !pickOnBoundsProperty().isBound()) {
            /* Documented behavior. See class description for TextFlow */
            setPickOnBounds(false);
        }
    }

    private void checkOrientation() {
        if (!isSpan()) {
            NodeOrientation orientation = getEffectiveNodeOrientation();
            boolean rtl =  orientation == NodeOrientation.RIGHT_TO_LEFT;
            int dir = rtl ? TextLayout.DIRECTION_RTL : TextLayout.DIRECTION_LTR;
            TextLayout layout = getTextLayout();
            if (layout.setDirection(dir)) {
                needsTextLayout();
            }
        }
    }

    @Override
    public boolean usesMirroring() {
        return false;
    }

    private void needsFullTextLayout() {
        if (isSpan()) {
            /* Create new text span every time the font or text changes
             * so the text layout can see that the content has changed.
             */
            textSpan = null;

            /* Relies on NodeHelper.geomChanged(this) to request text flow to relayout */
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
        NodeHelper.geomChanged(this);
        NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
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
            layout.setTabSize(getTabSize());
        }
        return layout;
    }

    private GlyphList[] textRuns = null;
    private BaseBounds spanBounds = new RectBounds(); /* relative to the textlayout */
    private boolean spanBoundsInvalid = true;

    void layoutSpan(GlyphList[] runs) {
        TextSpan span = getTextSpan();
        int count = 0;
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
        spanBoundsInvalid = true;

        /* Sometimes a property change in the text node will causes layout in
         * text flow. In this case all the dirty bits are already clear and no
         * extra work is necessary. Other times the layout is caused by changes
         * in the text flow object (wrapping width and text alignment for example).
         * In the second case the dirty bits must be set here using
         * NodeHelper.geomChanged(this) and NodeHelper.markDirty(). Note that NodeHelper.geomChanged(this)
         * causes another (undesired) layout request in the parent.
         * In general this is not a problem because shapes are not resizable and
         * region objects do not propagate layout changes to the parent.
         * This is a special case where a shape is resized by the parent during
         * layoutChildren(). See TextFlow#requestLayout() for information how
         * text flow deals with this situation.
         */
        NodeHelper.geomChanged(this);
        NodeHelper.markDirty(this, DirtyBits.NODE_CONTENTS);
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
        if (isStrikethrough()) type |= TextLayout.TYPE_STRIKETHROUGH;
        if (isUnderline()) type |= TextLayout.TYPE_UNDERLINE;

        TextSpan filter = null;
        if (isSpan()) {
            /* Spans are always relative to the top */
            type |= TextLayout.TYPE_TOP;
            filter = getTextSpan();
        } else {
            /* Relative to baseline (first line)
             * This shape can be translate in the y axis according
             * to text origin, see ShapeHelper.configShape().
             */
            type |= TextLayout.TYPE_BASELINE;
        }
        return layout.getShape(type, filter);
    }

    private BaseBounds getVisualBounds() {
        if (ShapeHelper.getMode(this) == NGShape.Mode.FILL || getStrokeType() == StrokeType.INSIDE) {
            int type = TextLayout.TYPE_TEXT;
            if (isStrikethrough()) type |= TextLayout.TYPE_STRIKETHROUGH;
            if (isUnderline()) type |= TextLayout.TYPE_UNDERLINE;
            return getTextLayout().getVisualBounds(type);
        } else {
            return getShape().getBounds();
        }
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
                    setSelectionStart(-1);
                    setSelectionEnd(-1);
                    setCaretPosition(-1);
                    setCaretBias(true);

                    // MH: Functionality copied from store() method,
                    // which was removed.
                    // Wonder what should happen if text is bound
                    //  and becomes null?
                    final String value = get();
                    if ((value == null) && !isBound()) {
                        set("");
                    }
                    notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
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
                    NodeHelper.geomChanged(Text.this);
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
                    NodeHelper.geomChanged(Text.this);
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
        return FontHelper.getNativeFont(font);
    }

    public final ObjectProperty<Font> fontProperty() {
        if (font == null) {
            font = new StyleableObjectProperty<Font>(Font.getDefault()) {
                @Override public Object getBean() { return Text.this; }
                @Override public String getName() { return "font"; }
                @Override public CssMetaData<Text,Font> getCssMetaData() {
                    return StyleableProperties.FONT;
                }
                @Override public void invalidated() {
                    needsFullTextLayout();
                    NodeHelper.markDirty(Text.this, DirtyBits.TEXT_FONT);
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
     * @return the origin of text coordinate system in local coordinates
     * @defaultValue VPos.BASELINE
     */
    public final ObjectProperty<VPos> textOriginProperty() {
        return getTextAttribute().textOriginProperty();
    }

    /**
     * Determines how the bounds of the text node are calculated.
     * Logical bounds is a more appropriate default for text than
     * the visual bounds. See {@code TextBoundsType} for more information.
     *
     * @defaultValue TextBoundsType.LOGICAL
     */
    private ObjectProperty<TextBoundsType> boundsType;

    public final void setBoundsType(TextBoundsType value) {
        boundsTypeProperty().set(value);
    }

    public final TextBoundsType getBoundsType() {
        return boundsType == null ?
            DEFAULT_BOUNDS_TYPE : boundsTypeProperty().get();
    }

    public final ObjectProperty<TextBoundsType> boundsTypeProperty() {
        if (boundsType == null) {
            boundsType =
               new StyleableObjectProperty<TextBoundsType>(DEFAULT_BOUNDS_TYPE) {
                   @Override public Object getBean() { return Text.this; }
                   @Override public String getName() { return "boundsType"; }
                   @Override public CssMetaData<Text,TextBoundsType> getCssMetaData() {
                       return StyleableProperties.BOUNDS_TYPE;
                   }
                   @Override public void invalidated() {
                       TextLayout layout = getTextLayout();
                       int type = 0;
                       if (boundsType.get() == TextBoundsType.LOGICAL_VERTICAL_CENTER) {
                           type |= TextLayout.BOUNDS_CENTER;
                       }
                       if (layout.setBoundsType(type)) {
                           needsTextLayout();
                       } else {
                           NodeHelper.geomChanged(Text.this);
                       }
                   }
            };
        }
        return boundsType;
    }

    /**
     * Defines a width constraint for the text in user space coordinates.
     * The width is measured in pixels (and not glyph or character count).
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
                            NodeHelper.geomChanged(Text.this);
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
     * @return if each line of text should have a line below it
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
     * @return if each line of text should have a line through it
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
     * @return the horizontal text alignment in the bounding box
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
     * @return the vertical space in pixel between lines
     * @defaultValue 0
     *
     * @since JavaFX 8.0
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
     * @return the baseline offset from this text node
     */
    public final ReadOnlyDoubleProperty baselineOffsetProperty() {
        return getTextAttribute().baselineOffsetProperty();
    }

    /**
     * Specifies a requested font smoothing type: gray or LCD.
     * <p>
     * Note: LCD mode doesn't apply in numerous cases, such as various
     * compositing modes, where effects are applied and very large glyphs.
     *
     * @defaultValue {@code FontSmoothingType.GRAY}
     * @since JavaFX 2.1
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
                @Override public CssMetaData<Text,FontSmoothingType> getCssMetaData() {
                    return StyleableProperties.FONT_SMOOTHING_TYPE;
                }
                @Override public void invalidated() {
                    NodeHelper.markDirty(Text.this, DirtyBits.TEXT_ATTRS);
                    NodeHelper.geomChanged(Text.this);
                }
            };
        }
        return fontSmoothingType;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doGeomChanged() {
        if (attributes != null) {
            if (attributes.caretBinding != null) {
                attributes.caretBinding.invalidate();
            }
            if (attributes.selectionBinding != null) {
                attributes.selectionBinding.invalidate();
            }
        }
        NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
    }

    public final PathElement[] getSelectionShape() {
        return selectionShapeProperty().get();
    }

    /**
     * The shape of the selection in local coordinates.
     *
     * @return the {@code selectionShape} property
     *
     * @since 9
     */
    public final ReadOnlyObjectProperty<PathElement[]> selectionShapeProperty() {
        return getTextAttribute().selectionShapeProperty();
    }

    public final void setSelectionStart(int value) {
        if (value == -1 &&
                (attributes == null || attributes.selectionStart == null)) {
            return;
        }
        selectionStartProperty().set(value);
    }

    public final int getSelectionStart() {
        if (attributes == null || attributes.selectionStart == null) {
            return DEFAULT_SELECTION_START;
        }
        return attributes.getSelectionStart();
    }

    /**
     * The start index of the selection in the content.
     * If the value is -1, the selection is unset.
     *
     * @return the {@code selectionStart} property
     *
     * @defaultValue -1
     *
     * @since 9
     */
    public final IntegerProperty selectionStartProperty() {
        return getTextAttribute().selectionStartProperty();
    }

    public final void setSelectionEnd(int value) {
        if (value == -1 &&
                (attributes == null || attributes.selectionEnd == null)) {
            return;
        }
        selectionEndProperty().set(value);
    }

    public final int getSelectionEnd() {
        if (attributes == null || attributes.selectionEnd == null) {
            return DEFAULT_SELECTION_END;
        }
        return attributes.getSelectionEnd();
    }

    /**
     * The end index of the selection in the content.
     * If the value is -1, the selection is unset.
     *
     * @return the {@code selectionEnd} property
     *
     * @defaultValue -1
     *
     * @since 9
     */
    public final IntegerProperty selectionEndProperty() {
        return getTextAttribute().selectionEndProperty();
    }

    /**
     * The fill color of selected text.
     *
     * @return the fill color of selected text
     * @since 9
     */
    public final ObjectProperty<Paint> selectionFillProperty() {
        return getTextAttribute().selectionFillProperty();
    }

    public final void setSelectionFill(Paint paint) {
        selectionFillProperty().set(paint);
    }
    public final Paint getSelectionFill() {
        return selectionFillProperty().get();
    }

    public final PathElement[] getCaretShape() {
        return caretShapeProperty().get();
    }

    /**
     * The shape of caret, in local coordinates.
     *
     * @return the {@code caretShape} property
     *
     * @since 9
     */
    public final ReadOnlyObjectProperty<PathElement[]> caretShapeProperty() {
        return getTextAttribute().caretShapeProperty();
    }

    public final void setCaretPosition(int value) {
        if (value == -1 &&
                (attributes == null || attributes.caretPosition == null)) {
            return;
        }
        caretPositionProperty().set(value);
    }

    public final int getCaretPosition() {
        if (attributes == null || attributes.caretPosition == null) {
            return DEFAULT_CARET_POSITION;
        }
        return attributes.getCaretPosition();
    }

    /**
     * The caret index in the content.
     * If the value is -1, the caret is unset.
     *
     * @return the {@code caretPosition} property
     *
     * @defaultValue -1
     *
     * @since 9
     */
    public final IntegerProperty caretPositionProperty() {
        return getTextAttribute().caretPositionProperty();
    }

    public final void setCaretBias(boolean value) {
        if (value && (attributes == null || attributes.caretBias == null)) {
            return;
        }
        caretBiasProperty().set(value);
    }

    public final boolean isCaretBias() {
        if (attributes == null || attributes.caretBias == null) {
            return DEFAULT_CARET_BIAS;
        }
        return getTextAttribute().isCaretBias();
    }

    /**
     * The type of caret bias in the content. If {@code true}, the bias is towards the leading character edge,
     * otherwise, the bias is towards the trailing character edge.
     *
     * @return the {@code caretBias} property
     *
     * @defaultValue {@code true}
     *
     * @since 9
     */
    public final BooleanProperty caretBiasProperty() {
        return getTextAttribute().caretBiasProperty();
    }

    /**
     * Maps local point to index in the content.
     *
     * @param point the specified point to be tested
     * @return a {@code HitInfo} representing the character index found
     * @since 9
     */
    public final HitInfo hitTest(Point2D point) {
        if (point == null) return null;
        TextLayout layout = getTextLayout();
        double x = point.getX() - getX();
        double y = point.getY() - getY() + getYRendering();
        TextLayout.Hit layoutHit = layout.getHitInfo((float)x, (float)y);
        return new HitInfo(layoutHit.getCharIndex(), layoutHit.getInsertionIndex(),
                           layoutHit.isLeading(), getText());
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
     * Returns the shape for the caret at the given index and bias.
     *
     * @param charIndex the character index for the caret
     * @param caretBias whether the caret is biased on the leading edge of the character
     * @return an array of {@code PathElement} which can be used to create a {@code Shape}
     * @since 9
     */
    public final PathElement[] caretShape(int charIndex, boolean caretBias) {
        if (0 <= charIndex && charIndex <= getTextInternal().length()) {
            float x = (float)getX();
            float y = (float)getY() - getYRendering();
            return getTextLayout().getCaretShape(charIndex, caretBias, x, y);
        } else {
            return null;
        }
    }

    /**
     * Returns the shape for the range of the text in local coordinates.
     *
     * @param start the beginning character index for the range
     * @param end the end character index (non-inclusive) for the range
     * @return an array of {@code PathElement} which can be used to create a {@code Shape}
     * @since 9
     */
    public final PathElement[] rangeShape(int start, int end) {
        return getRange(start, end, TextLayout.TYPE_TEXT);
    }

    /**
     * Returns the shape for the underline in local coordinates.
     *
     * @param start the beginning character index for the range
     * @param end the end character index (non-inclusive) for the range
     * @return an array of {@code PathElement} which can be used to create a {@code Shape}
     * @since 9
     */
    public final PathElement[] underlineShape(int start, int end) {
        return getRange(start, end, TextLayout.TYPE_UNDERLINE);
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
        if (isSpan()) return 0;

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

    private Bounds doComputeLayoutBounds() {
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
            return TextHelper.superComputeLayoutBounds(this);
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

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds,
                                                   BaseTransform tx) {
        if (isSpan()) {
            if (ShapeHelper.getMode(this) != NGShape.Mode.FILL && getStrokeType() != StrokeType.INSIDE) {
                return TextHelper.superComputeGeomBounds(this, bounds, tx);
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
            if (getTextInternal().length() == 0 || ShapeHelper.getMode(this) == NGShape.Mode.EMPTY) {
                return bounds.makeEmpty();
            }
            if (ShapeHelper.getMode(this) == NGShape.Mode.FILL || getStrokeType() == StrokeType.INSIDE) {
                /* Optimize for FILL and INNER STROKE: save the cost of shaping each glyph */
                BaseBounds visualBounds = getVisualBounds();
                float x = visualBounds.getMinX() + (float) getX();
                float yadj = getYAdjustment(visualBounds);
                float y = visualBounds.getMinY() + yadj + (float) getY();
                bounds.deriveWithNewBounds(x, y, 0, x + visualBounds.getWidth(),
                        y + visualBounds.getHeight(), 0);
                return tx.transform(bounds, bounds);
            } else {
                /* Let the superclass compute the bounds using shape */
                return TextHelper.superComputeGeomBounds(this, bounds, tx);
            }
        }

        BaseBounds textBounds = getLogicalBounds();
        float x = textBounds.getMinX() + (float)getX();
        float yadj = getYAdjustment(textBounds);
        float y = textBounds.getMinY() + yadj + (float)getY();
        float width = textBounds.getWidth();
        float height = textBounds.getHeight();
        float wrappingWidth = (float)getWrappingWidth();
        if (wrappingWidth > width) {
            width = wrappingWidth;
        } else {
            /* The following adjustment is necessary for the text bounds to be
             * relative to the same location as the mirrored bounds returned
             * by layout.getBounds().
             */
            if (wrappingWidth > 0) {
                NodeOrientation orientation = getEffectiveNodeOrientation();
                if (orientation == NodeOrientation.RIGHT_TO_LEFT) {
                    x -= width - wrappingWidth;
                }
            }
        }
        textBounds = new RectBounds(x, y, x + width, y + height);

        /* handle stroked text */
        if (ShapeHelper.getMode(this) != NGShape.Mode.FILL && getStrokeType() != StrokeType.INSIDE) {
            bounds = TextHelper.superComputeGeomBounds(this, bounds,
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

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        /* Used for spans, regular text uses bounds based picking */
        double x = localX + getSpanBounds().getMinX();
        double y = localY + getSpanBounds().getMinY();
        GlyphList[] runs = getRuns();
        if (runs.length != 0) {
            for (int i = 0; i < runs.length; i++) {
                GlyphList run = runs[i];
                com.sun.javafx.geom.Point2D location = run.getLocation();
                float width = run.getWidth();
                RectBounds lineBounds = run.getLineBounds();
                float height = lineBounds.getHeight();
                if (location.x <= x && x < location.x + width &&
                    location.y <= y && y < location.y + height) {
                        return true;
                }
            }
        }
        return false;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private com.sun.javafx.geom.Shape doConfigShape() {
        if (ShapeHelper.getMode(this) == NGShape.Mode.EMPTY || getTextInternal().length() == 0) {
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

    /**
     * The size of a tab stop in spaces.
     * Values less than 1 are treated as 1.
     *
     * @return the {@code tabSize} property
     *
     * @defaultValue 8
     *
     * @since 14
     */
    public final IntegerProperty tabSizeProperty() {
        return getTextAttribute().tabSizeProperty();
    }

    public final int getTabSize() {
        if (attributes == null || attributes.tabSize == null) {
            return TextLayout.DEFAULT_TAB_SIZE;
        }
        return getTextAttribute().getTabSize();
    }

    public final void setTabSize(int spaces) {
        tabSizeProperty().set(spaces);
    }


   /* *************************************************************************
    *                                                                         *
    *                            Stylesheet Handling                          *
    *                                                                         *
    **************************************************************************/

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
    private static class StyleableProperties {

        private static final CssMetaData<Text,Font> FONT =
            new FontCssMetaData<Text>("-fx-font", Font.getDefault()) {

            @Override
            public boolean isSettable(Text node) {
                return node.font == null || !node.font.isBound();
            }

            @Override
            public StyleableProperty<Font> getStyleableProperty(Text node) {
                return (StyleableProperty<Font>)node.fontProperty();
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
            public StyleableProperty<Boolean> getStyleableProperty(Text node) {
                return (StyleableProperty<Boolean>)node.underlineProperty();
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
            public StyleableProperty<Boolean> getStyleableProperty(Text node) {
                return (StyleableProperty<Boolean>)node.strikethroughProperty();
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
            public StyleableProperty<TextAlignment> getStyleableProperty(Text node) {
                return (StyleableProperty<TextAlignment>)node.textAlignmentProperty();
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
            public StyleableProperty<VPos> getStyleableProperty(Text node) {
                return (StyleableProperty<VPos>)node.textOriginProperty();
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
            public StyleableProperty<FontSmoothingType>
                                 getStyleableProperty(Text node) {

                return (StyleableProperty<FontSmoothingType>)node.fontSmoothingTypeProperty();
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
            public StyleableProperty<Number> getStyleableProperty(Text node) {
                return (StyleableProperty<Number>)node.lineSpacingProperty();
            }
        };

        private static final CssMetaData<Text, TextBoundsType>
            BOUNDS_TYPE =
            new CssMetaData<Text,TextBoundsType>(
                "-fx-bounds-type",
                new EnumConverter<TextBoundsType>(TextBoundsType.class),
                DEFAULT_BOUNDS_TYPE) {

            @Override
            public boolean isSettable(Text node) {
                return node.boundsType == null || !node.boundsType.isBound();
            }

            @Override
            public StyleableProperty<TextBoundsType> getStyleableProperty(Text node) {
                return (StyleableProperty<TextBoundsType>)node.boundsTypeProperty();
            }
        };

        private static final CssMetaData<Text, Number> TAB_SIZE =
                new CssMetaData<Text,Number>("-fx-tab-size",
                SizeConverter.getInstance(), TextLayout.DEFAULT_TAB_SIZE) {

            @Override
            public boolean isSettable(Text node) {
                return node.attributes == null ||
                       node.attributes.tabSize == null ||
                      !node.attributes.tabSize.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(Text node) {
                return (StyleableProperty<Number>)node.tabSizeProperty();
            }
        };

    private final static List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Shape.getClassCssMetaData());
            styleables.add(FONT);
            styleables.add(UNDERLINE);
            styleables.add(STRIKETHROUGH);
            styleables.add(TEXT_ALIGNMENT);
            styleables.add(TEXT_ORIGIN);
            styleables.add(FONT_SMOOTHING_TYPE);
            styleables.add(LINE_SPACING);
            styleables.add(BOUNDS_TYPE);
            styleables.add(TAB_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8.0
     */


    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @SuppressWarnings("deprecation")
    private void updatePGText() {
        final NGText peer = NodeHelper.getPeer(this);
        if (NodeHelper.isDirty(this, DirtyBits.TEXT_ATTRS)) {
            peer.setUnderline(isUnderline());
            peer.setStrikethrough(isStrikethrough());
            FontSmoothingType smoothing = getFontSmoothingType();
            if (smoothing == null) smoothing = FontSmoothingType.GRAY;
            peer.setFontSmoothingType(smoothing.ordinal());
        }
        if (NodeHelper.isDirty(this, DirtyBits.TEXT_FONT)) {
            peer.setFont(getFontInternal());
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            peer.setGlyphs(getRuns());
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
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
        if (NodeHelper.isDirty(this, DirtyBits.TEXT_SELECTION)) {
            Object fillObj = null;
            int start = getSelectionStart();
            int end = getSelectionEnd();
            int length = getTextInternal().length();
            if (0 <= start && start < end  && end <= length) {
                Paint fill = selectionFillProperty().get();
                fillObj = fill != null ? Toolkit.getPaintAccessor().getPlatformPaint(fill) : null;
            }
            peer.setSelection(start, end, fillObj);
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        updatePGText();
    }

    /* *************************************************************************
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

        final VPos getTextOrigin() {
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
                        NodeHelper.geomChanged(Text.this);
                    }
                };
            }
            return textOrigin;
        }

        private BooleanProperty underline;

        final boolean isUnderline() {
            return underline == null ? DEFAULT_UNDERLINE : underline.get();
        }

        final BooleanProperty underlineProperty() {
            if (underline == null) {
                underline = new StyleableBooleanProperty() {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "underline"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.UNDERLINE;
                    }
                    @Override public void invalidated() {
                        NodeHelper.markDirty(Text.this, DirtyBits.TEXT_ATTRS);
                        if (getBoundsType() == TextBoundsType.VISUAL) {
                            NodeHelper.geomChanged(Text.this);
                        }
                    }
                };
            }
            return underline;
        }

        private BooleanProperty strikethrough;

        final boolean isStrikethrough() {
            return strikethrough == null ? DEFAULT_STRIKETHROUGH : strikethrough.get();
        }

        final BooleanProperty strikethroughProperty() {
            if (strikethrough == null) {
                strikethrough = new StyleableBooleanProperty() {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "strikethrough"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.STRIKETHROUGH;
                    }
                    @Override public void invalidated() {
                        NodeHelper.markDirty(Text.this, DirtyBits.TEXT_ATTRS);
                        if (getBoundsType() == TextBoundsType.VISUAL) {
                            NodeHelper.geomChanged(Text.this);
                        }
                    }
                };
            }
            return strikethrough;
        }

        private ObjectProperty<TextAlignment> textAlignment;

        final TextAlignment getTextAlignment() {
            return textAlignment == null ? DEFAULT_TEXT_ALIGNMENT : textAlignment.get();
        }

        final ObjectProperty<TextAlignment> textAlignmentProperty() {
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

        final double getLineSpacing() {
            return lineSpacing == null ? DEFAULT_LINE_SPACING : lineSpacing.get();
        }

        final DoubleProperty lineSpacingProperty() {
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

        final ReadOnlyDoubleProperty baselineOffsetProperty() {
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

        private ObjectProperty<PathElement[]> selectionShape;
        private ObjectBinding<PathElement[]> selectionBinding;

        final ReadOnlyObjectProperty<PathElement[]> selectionShapeProperty() {
            if (selectionShape == null) {
                selectionBinding = new ObjectBinding<PathElement[]>() {
                    {bind(selectionStartProperty(), selectionEndProperty());}
                    @Override protected PathElement[] computeValue() {
                        int start = getSelectionStart();
                        int end = getSelectionEnd();
                        return getRange(start, end, TextLayout.TYPE_TEXT);
                    }
              };
              selectionShape = new SimpleObjectProperty<PathElement[]>(Text.this, "selectionShape");
              selectionShape.bind(selectionBinding);
            }
            return selectionShape;
        }

        private ObjectProperty<Paint> selectionFill;

        final ObjectProperty<Paint> selectionFillProperty() {
            if (selectionFill == null) {
                selectionFill =
                    new ObjectPropertyBase<Paint>(DEFAULT_SELECTION_FILL) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "selectionFill"; }
                        @Override protected void invalidated() {
                            NodeHelper.markDirty(Text.this, DirtyBits.TEXT_SELECTION);
                        }
                    };
            }
            return selectionFill;
        }

        private IntegerProperty selectionStart;

        final int getSelectionStart() {
            return selectionStart == null ? DEFAULT_SELECTION_START : selectionStart.get();
        }

        final IntegerProperty selectionStartProperty() {
            if (selectionStart == null) {
                selectionStart =
                    new IntegerPropertyBase(DEFAULT_SELECTION_START) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "selectionStart"; }
                        @Override protected void invalidated() {
                            NodeHelper.markDirty(Text.this, DirtyBits.TEXT_SELECTION);
                            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_START);
                        }
                };
            }
            return selectionStart;
        }

        private IntegerProperty selectionEnd;

        final int getSelectionEnd() {
            return selectionEnd == null ? DEFAULT_SELECTION_END : selectionEnd.get();
        }

        final IntegerProperty selectionEndProperty() {
            if (selectionEnd == null) {
                selectionEnd =
                    new IntegerPropertyBase(DEFAULT_SELECTION_END) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "selectionEnd"; }
                        @Override protected void invalidated() {
                            NodeHelper.markDirty(Text.this, DirtyBits.TEXT_SELECTION);
                            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_END);
                        }
                    };
            }
            return selectionEnd;
        }

        private ObjectProperty<PathElement[]> caretShape;
        private ObjectBinding<PathElement[]> caretBinding;

        final ReadOnlyObjectProperty<PathElement[]> caretShapeProperty() {
            if (caretShape == null) {
                caretBinding = new ObjectBinding<PathElement[]>() {
                    {bind(caretPositionProperty(), caretBiasProperty());}
                    @Override protected PathElement[] computeValue() {
                        int pos = getCaretPosition();
                        int length = getTextInternal().length();
                        if (0 <= pos && pos <= length) {
                            boolean bias = isCaretBias();
                            float x = (float)getX();
                            float y = (float)getY() - getYRendering();
                            TextLayout layout = getTextLayout();
                            return layout.getCaretShape(pos, bias, x, y);
                        }
                        return EMPTY_PATH_ELEMENT_ARRAY;
                    }
                };
                caretShape = new SimpleObjectProperty<PathElement[]>(Text.this, "caretShape");
                caretShape.bind(caretBinding);
            }
            return caretShape;
        }

        private IntegerProperty caretPosition;

        final int getCaretPosition() {
            return caretPosition == null ? DEFAULT_CARET_POSITION : caretPosition.get();
        }

        final IntegerProperty caretPositionProperty() {
            if (caretPosition == null) {
                caretPosition =
                    new IntegerPropertyBase(DEFAULT_CARET_POSITION) {
                        @Override public Object getBean() { return Text.this; }
                        @Override public String getName() { return "caretPosition"; }
                        @Override protected void invalidated() {
                            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_END);
                        }
                    };
            }
            return caretPosition;
        }

        private BooleanProperty caretBias;

        final boolean isCaretBias() {
            return caretBias == null ? DEFAULT_CARET_BIAS : caretBias.get();
        }

        final BooleanProperty caretBiasProperty() {
            if (caretBias == null) {
                caretBias =
                        new SimpleBooleanProperty(Text.this, "caretBias", DEFAULT_CARET_BIAS);
            }
            return caretBias;
        }

        private IntegerProperty tabSize;

        final int getTabSize() {
            return tabSize == null ? TextLayout.DEFAULT_TAB_SIZE : tabSize.get();
        }

        final IntegerProperty tabSizeProperty() {
            if (tabSize == null) {
                tabSize = new StyleableIntegerProperty(TextLayout.DEFAULT_TAB_SIZE) {
                    @Override public Object getBean() { return Text.this; }
                    @Override public String getName() { return "tabSize"; }
                    @Override public CssMetaData getCssMetaData() {
                        return StyleableProperties.TAB_SIZE;
                    }
                    @Override protected void invalidated() {
                        if (!isSpan()) {
                            TextLayout layout = getTextLayout();
                            if (layout.setTabSize(get())) {
                                needsTextLayout();
                            }
                            NodeHelper.markDirty(Text.this, DirtyBits.TEXT_ATTRS);
                            if (getBoundsType() == TextBoundsType.VISUAL) {
                                NodeHelper.geomChanged(Text.this);
                            }
                        }
                    }
                };
            }
            return tabSize;
        }
    }

    /**
     * Returns a string representation of this {@code Text} object.
     * @return a string representation of this {@code Text} object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Text[");

        String id = getId();
        if (id != null) {
            sb.append("id=").append(id).append(", ");
        }

        sb.append("text=\"").append(getText()).append("\"");
        sb.append(", x=").append(getX());
        sb.append(", y=").append(getY());
        sb.append(", alignment=").append(getTextAlignment());
        sb.append(", origin=").append(getTextOrigin());
        sb.append(", boundsType=").append(getBoundsType());

        double spacing = getLineSpacing();
        if (spacing != DEFAULT_LINE_SPACING) {
            sb.append(", lineSpacing=").append(spacing);
        }

        double wrap = getWrappingWidth();
        if (wrap != 0) {
            sb.append(", wrappingWidth=").append(wrap);
        }

        int tab = getTabSize();
        if (tab != TextLayout.DEFAULT_TAB_SIZE) {
            sb.append(", tabSize=").append(tab);
        }

        sb.append(", font=").append(getFont());
        sb.append(", fontSmoothingType=").append(getFontSmoothingType());

        if (isStrikethrough()) {
            sb.append(", strikethrough");
        }
        if (isUnderline()) {
            sb.append(", underline");
        }

        sb.append(", fill=").append(getFill());

        Paint stroke = getStroke();
        if (stroke != null) {
            sb.append(", stroke=").append(stroke);
            sb.append(", strokeWidth=").append(getStrokeWidth());
        }

        return sb.append("]").toString();
    }

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case TEXT: {
                String accText = getAccessibleText();
                if (accText != null && !accText.isEmpty()) return accText;
                return getText();
            }
            case FONT: return getFont();
            case CARET_OFFSET: {
                int sel = getCaretPosition();
                if (sel >=  0) return sel;
                return getText().length();
            }
            case SELECTION_START: {
                int sel = getSelectionStart();
                if (sel >=  0) return sel;
                sel = getCaretPosition();
                if (sel >=  0) return sel;
                return getText().length();
            }
            case SELECTION_END:  {
                int sel = getSelectionEnd();
                if (sel >=  0) return sel;
                sel = getCaretPosition();
                if (sel >=  0) return sel;
                return getText().length();
            }
            case LINE_FOR_OFFSET: {
                int offset = (Integer)parameters[0];
                if (offset > getTextInternal().length()) return null;
                TextLine[] lines = getTextLayout().getLines();
                int lineIndex = 0;
                for (int i = 1; i < lines.length; i++) {
                    TextLine line = lines[i];
                    if (line.getStart() > offset) break;
                    lineIndex++;
                }
                return lineIndex;
            }
            case LINE_START: {
                int lineIndex = (Integer)parameters[0];
                TextLine[] lines = getTextLayout().getLines();
                if (0 <= lineIndex && lineIndex < lines.length) {
                    TextLine line = lines[lineIndex];
                    return line.getStart();
                }
                return null;
            }
            case LINE_END: {
                int lineIndex = (Integer)parameters[0];
                TextLine[] lines = getTextLayout().getLines();
                if (0 <= lineIndex && lineIndex < lines.length) {
                    TextLine line = lines[lineIndex];
                    return line.getStart() + line.getLength();
                }
                return null;
            }
            case OFFSET_AT_POINT: {
                Point2D point = (Point2D)parameters[0];
                point = screenToLocal(point);
                return hitTest(point).getCharIndex();
            }
            case BOUNDS_FOR_RANGE: {
                int start = (Integer)parameters[0];
                int end = (Integer)parameters[1];
                PathElement[] elements = rangeShape(start, end + 1);
                /* Each bounds is defined by a MoveTo (top-left) followed by
                 * 4 LineTo (to top-right, bottom-right, bottom-left, back to top-left).
                 */
                Bounds[] bounds = new Bounds[elements.length / 5];
                int index = 0;
                for (int i = 0; i < bounds.length; i++) {
                    MoveTo topLeft = (MoveTo)elements[index];
                    LineTo topRight = (LineTo)elements[index+1];
                    LineTo bottomRight = (LineTo)elements[index+2];
                    BoundingBox b = new BoundingBox(topLeft.getX(), topLeft.getY(),
                                                    topRight.getX() - topLeft.getX(),
                                                    bottomRight.getY() - topRight.getY());
                    bounds[i] = localToScreen(b);
                    index += 5;
                }
                return bounds;
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
