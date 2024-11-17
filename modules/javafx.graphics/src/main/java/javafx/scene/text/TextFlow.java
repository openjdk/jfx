/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.PathElement;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Point2D;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.IntegerProperty;
import javafx.css.Styleable;
import javafx.css.StyleableIntegerProperty;
import javafx.css.StyleableProperty;

/**
 * A specialized layout for rich text.
 * It can be used to lay out several {@link Text} nodes in a single text flow.
 * {@code TextFlow} uses the text and the font of each {@code Text} node inside of it,
 * plus its own width and text alignment to determine the location for each child.
 * A single {@code Text} node can span over several lines due to wrapping, and
 * the visual location of the {@code Text} node can differ from the logical location
 * due to bidi reordering.
 *
 * <p>
 * Any {@code Node} other than {@code Text} will be treated as an embedded object in the
 * text layout. It will be inserted in the content using its preferred width,
 * height, and baseline offset.
 *
 * <p>
 * When a {@code Text} node is inside a {@code TextFlow}, some of its properties are ignored.
 * For example, the {@code x} and {@code y} properties of the {@code Text} node are ignored since
 * the location of the node is determined by the parent. Likewise, the wrapping
 * width in the {@code Text} node is ignored since the width used for wrapping
 * is the {@code TextFlow}'s width. The value of the {@code pickOnBounds} property
 * of a {@code Text} node is set to {@code false} when it is laid out by the
 * {@code TextFlow}. This happens because the content of a single {@code Text} node can be
 * split and placed in different locations in the {@code TextFlow} (usually due to
 * line breaking and bidi reordering).
 *
 * <p>
 * The wrapping width of the layout is determined by the region's current width.
 * It can be specified by the application by setting the {@code TextFlow}'s preferred
 * width. If no wrapping is desired, the application can either set the preferred
 * with to {@code Double.MAX_VALUE} or
 * {@link javafx.scene.layout.Region#USE_COMPUTED_SIZE Region.USE_COMPUTED_SIZE}.
 *
 * <p>
 * Paragraphs are separated by {@code '\n'} present in any {@code Text} child.
 *
 * <p>
 * Example of a TextFlow:
 * <pre>{@code
 *     Text text1 = new Text("Big italic red text");
 *     text1.setFill(Color.RED);
 *     text1.setFont(Font.font("Helvetica", FontPosture.ITALIC, 40));
 *     Text text2 = new Text(" little bold blue text");
 *     text2.setFill(Color.BLUE);
 *     text2.setFont(Font.font("Helvetica", FontWeight.BOLD, 10));
 *     TextFlow textFlow = new TextFlow(text1, text2);
 * }</pre>
 *
 * <p>
 * {@code TextFlow} lays out each managed child regardless of the child's visible property value;
 * unmanaged children are ignored for all layout calculations.</p>
 *
 * <p>
 * {@code TextFlow} may be styled with backgrounds and borders using CSS. See its
 * {@link javafx.scene.layout.Region Region} superclass for details.</p>
 *
 * <h2>Resizable Range</h2>
 *
 * <p>
 * A {@code TextFlow}'s parent will resize the {@code TextFlow} within the {@code TextFlow}'s range
 * during layout. By default, the {@code TextFlow} computes this range based on its content
 * as outlined in the tables below.
 * </p>
 *
 * <table border="1">
 * <caption>TextFlow Resize Table</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets</td>
 * <td>top/bottom insets plus the height of the text content</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus the width of the text content</td>
 * <td>top/bottom insets plus the height of the text content</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>{@code Double.MAX_VALUE}</td><td>{@code Double.MAX_VALUE}</td></tr>
 * </table>
 * <p>
 * A {@code TextFlow}'s unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * {@code TextFlow} provides properties for setting the size range directly. These
 * properties default to the sentinel value {@code Region.USE_COMPUTED_SIZE}, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>textflow.setMaxWidth(500);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to {@code Region.USE_COMPUTED_SIZE}.
 * <p>
 * {@code TextFlow} does not clip its content by default, so it is possible that children's
 * bounds may extend outside of its own bounds if a child's preferred size is larger than
 * the space the {@code TextFlow} has to allocate for it.</p>
 *
 * @since JavaFX 8.0
 */
public class TextFlow extends Pane {

    private TextLayout layout;
    private boolean needsContent;
    private boolean inLayout;

    /**
     * Creates an empty TextFlow layout.
     */
    public TextFlow() {
        super();
        effectiveNodeOrientationProperty().addListener(observable -> checkOrientation());
        setAccessibleRole(AccessibleRole.TEXT);
    }

    /**
     * Creates a TextFlow layout with the given children.
     *
     * @param children children.
     */
    public TextFlow(Node... children) {
        this();
        getChildren().addAll(children);
    }

    private void checkOrientation() {
        NodeOrientation orientation = getEffectiveNodeOrientation();
        boolean rtl =  orientation == NodeOrientation.RIGHT_TO_LEFT;
        int dir = rtl ? TextLayout.DIRECTION_RTL : TextLayout.DIRECTION_LTR;
        TextLayout layout = getTextLayout();
        if (layout.setDirection(dir)) {
            requestLayout();
        }
    }

    /**
     * Maps local point to {@link HitInfo} in the content.
     *
     * @param point the specified point to be tested
     * @return a {@code HitInfo} representing the character index found
     * @since 9
     */
    public final HitInfo hitTest(javafx.geometry.Point2D point) {
        if (point != null) {
            TextLayout layout = getTextLayout();
            double x = point.getX();
            double y = point.getY();
            TextLayout.Hit h = layout.getHitInfo((float)x, (float)y);
            return new HitInfo(h.getCharIndex(), h.getInsertionIndex(), h.isLeading());
        } else {
            return null;
        }
    }

    /**
     * Returns shape of caret in local coordinates.
     *
     * @param charIndex the character index for the caret
     * @param leading whether the caret is biased on the leading edge of the character
     * @return an array of {@code PathElement} which can be used to create a {@code Shape}
     * @since 9
     */
    public PathElement[] caretShape(int charIndex, boolean leading) {
        return getTextLayout().getCaretShape(charIndex, leading, 0, 0);
    }

    /**
     * Returns shape for the range of the text in local coordinates.
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
     * @since 21
     */
    public final PathElement[] underlineShape(int start, int end) {
        return getRange(start, end, TextLayout.TYPE_UNDERLINE);
    }

    @Override
    public boolean usesMirroring() {
        return false;
    }

    @Override protected void setWidth(double value) {
        if (value != getWidth()) {
            TextLayout layout = getTextLayout();
            Insets insets = getInsets();
            double left = snapSpaceX(insets.getLeft());
            double right = snapSpaceX(insets.getRight());
            double width = Math.max(1, value - left - right);
            layout.setWrapWidth((float)width);
            super.setWidth(value);
        }
    }

    @Override protected double computePrefWidth(double height) {
        TextLayout layout = getTextLayout();
        layout.setWrapWidth(0);
        double width = layout.getBounds().getWidth();
        Insets insets = getInsets();
        double left = snapSpaceX(insets.getLeft());
        double right = snapSpaceX(insets.getRight());
        double wrappingWidth = Math.max(1, getWidth() - left - right);
        layout.setWrapWidth((float)wrappingWidth);
        return left + width + right;
    }

    @Override protected double computePrefHeight(double width) {
        TextLayout layout = getTextLayout();
        Insets insets = getInsets();
        double left = snapSpaceX(insets.getLeft());
        double right = snapSpaceX(insets.getRight());
        if (width == USE_COMPUTED_SIZE) {
            layout.setWrapWidth(0);
        } else {
            double wrappingWidth = Math.max(1, width - left - right);
            layout.setWrapWidth((float)wrappingWidth);
        }
        double height = layout.getBounds().getHeight();
        double wrappingWidth = Math.max(1, getWidth() - left - right);
        layout.setWrapWidth((float)wrappingWidth);
        double top = snapSpaceY(insets.getTop());
        double bottom = snapSpaceY(insets.getBottom());
        return top + height + bottom;
    }

    @Override protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override public void requestLayout() {
        /* The geometry of text nodes can be changed during layout children.
         * For that reason it has to call NodeHelper.geomChanged(this) causing
         * requestLayout() to happen during layoutChildren().
         * The inLayout flag prevents this call to cause any extra work.
         */
        if (inLayout) return;

        /*
        * There is no need to reset the text layout's content every time
        * requestLayout() is called. For example, the content needs
        * to be set when:
        *  children add or removed
        *  children managed state changes
        *  children geomChanged (width/height of embedded node)
        *  children content changes (text/font of text node)
        * The content does not need to set when:
        *  the width/height changes in the region
        *  the insets changes in the region
        *
        * Unfortunately, it is not possible to know what change invoked request
        * layout. The solution is to always reset the content in the text
        * layout and rely on it to preserve itself if the new content equals to
        * the old one. The cost to generate the new content is not avoid.
        */
        needsContent = true;
        super.requestLayout();
    }

    @Override public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override protected void layoutChildren() {
        inLayout = true;
        Insets insets = getInsets();
        double top = snapSpaceY(insets.getTop());
        double left = snapSpaceX(insets.getLeft());

        GlyphList[] runs = getTextLayout().getRuns();
        for (int j = 0; j < runs.length; j++) {
            GlyphList run = runs[j];
            TextSpan span = run.getTextSpan();
            if (span instanceof EmbeddedSpan) {
                Node child = ((EmbeddedSpan)span).getNode();
                Point2D location = run.getLocation();
                double baselineOffset = -run.getLineBounds().getMinY();

                layoutInArea(child, left + location.x, top + location.y,
                             run.getWidth(), run.getHeight(),
                             baselineOffset, null, true, true,
                             HPos.CENTER, VPos.BASELINE);
            }
        }

        List<Node> managed = getManagedChildren();
        for (Node node: managed) {
            if (node instanceof Text) {
                Text text = (Text)node;
                text.layoutSpan(runs);
                BaseBounds spanBounds = text.getSpanBounds();
                text.relocate(left + spanBounds.getMinX(),
                              top + spanBounds.getMinY());
            }
        }
        inLayout = false;
    }

    private PathElement[] getRange(int start, int end, int type) {
        TextLayout layout = getTextLayout();
        return layout.getRange(start, end, type, 0, 0);
    }

    private static class EmbeddedSpan implements TextSpan {
        RectBounds bounds;
        Node node;
        public EmbeddedSpan(Node node, double baseline, double width, double height) {
            this.node = node;
            bounds = new RectBounds(0, (float)-baseline,
                                    (float)width, (float)(height - baseline));
        }

        @Override public String getText() {
            return "\uFFFC";
        }

        @Override public Object getFont() {
            return null;
        }

        @Override public RectBounds getBounds() {
            return bounds;
        }

        public Node getNode() {
            return node;
        }
    }

    TextLayout getTextLayout() {
        if (layout == null) {
            TextLayoutFactory factory = Toolkit.getToolkit().getTextLayoutFactory();
            layout = factory.createLayout();
            layout.setTabSize(getTabSize());
            needsContent = true;
        }
        if (needsContent) {
            List<Node> children = getManagedChildren();
            TextSpan[] spans = new TextSpan[children.size()];
            for (int i = 0; i < spans.length; i++) {
                Node node = children.get(i);
                if (node instanceof Text) {
                    spans[i] = ((Text)node).getTextSpan();
                } else {
                    /* Creating a text span every time forces text layout
                     * to run a full text analysis in the new content.
                     */
                    double baseline = node.getBaselineOffset();
                    if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                        baseline = node.getLayoutBounds().getHeight();
                    }
                    double width = computeChildPrefAreaWidth(node, null);
                    double height = computeChildPrefAreaHeight(node, null);
                    spans[i] = new EmbeddedSpan(node, baseline, width, height);
                }
            }
            layout.setContent(spans);
            needsContent = false;
        }
        return layout;
    }

    /**
     * Defines horizontal text alignment.
     *
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
            textAlignment =
                new StyleableObjectProperty<TextAlignment>(TextAlignment.LEFT) {
                @Override public Object getBean() { return TextFlow.this; }
                @Override public String getName() { return "textAlignment"; }
                @Override public CssMetaData<TextFlow, TextAlignment> getCssMetaData() {
                    return StyleableProperties.TEXT_ALIGNMENT;
                }
                @Override public void invalidated() {
                    TextAlignment align = get();
                    if (align == null) align = TextAlignment.LEFT;
                    TextLayout layout = getTextLayout();
                    layout.setAlignment(align.ordinal());
                    requestLayout();
                }
            };
        }
        return textAlignment;
    }

    /**
     * Defines the vertical space in pixel between lines.
     *
     * @defaultValue 0
     *
     * @since JavaFX 8.0
     */
    private DoubleProperty lineSpacing;

    public final void setLineSpacing(double spacing) {
        lineSpacingProperty().set(spacing);
    }

    public final double getLineSpacing() {
        return lineSpacing == null ? 0 : lineSpacing.get();
    }

    public final DoubleProperty lineSpacingProperty() {
        if (lineSpacing == null) {
            lineSpacing =
                new StyleableDoubleProperty(0) {
                @Override public Object getBean() { return TextFlow.this; }
                @Override public String getName() { return "lineSpacing"; }
                @Override public CssMetaData<TextFlow, Number> getCssMetaData() {
                    return StyleableProperties.LINE_SPACING;
                }
                @Override public void invalidated() {
                    TextLayout layout = getTextLayout();
                    if (layout.setLineSpacing((float)get())) {
                        requestLayout();
                    }
                }
            };
        }
        return lineSpacing;
    }

    /**
     * The size of a tab stop in spaces.
     * Values less than 1 are treated as 1. This value overrides the
     * {@code tabSize} of contained {@link Text} nodes.
     *
     * @defaultValue 8
     *
     * @since 14
     */
    private IntegerProperty tabSize;

    public final IntegerProperty tabSizeProperty() {
        if (tabSize == null) {
            tabSize = new StyleableIntegerProperty(TextLayout.DEFAULT_TAB_SIZE) {
                @Override public Object getBean() { return TextFlow.this; }
                @Override public String getName() { return "tabSize"; }
                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.TAB_SIZE;
                }
                @Override protected void invalidated() {
                    TextLayout layout = getTextLayout();
                    if (layout.setTabSize(get())) {
                        requestLayout();
                    }
                }
            };
        }
        return tabSize;
    }

    public final int getTabSize() {
        return tabSize == null ? TextLayout.DEFAULT_TAB_SIZE : tabSize.get();
    }

    public final void setTabSize(int spaces) {
        tabSizeProperty().set(spaces);
    }

    @Override public final double getBaselineOffset() {
        Insets insets = getInsets();
        double top = snapSpaceY(insets.getTop());
        return top - getTextLayout().getBounds().getMinY();
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

        private static final
            CssMetaData<TextFlow, TextAlignment> TEXT_ALIGNMENT =
                new CssMetaData<>("-fx-text-alignment",
                new EnumConverter<>(TextAlignment.class),
                TextAlignment.LEFT) {

            @Override public boolean isSettable(TextFlow node) {
                return node.textAlignment == null || !node.textAlignment.isBound();
            }

            @Override public StyleableProperty<TextAlignment> getStyleableProperty(TextFlow node) {
                return (StyleableProperty<TextAlignment>)node.textAlignmentProperty();
            }
        };

        private static final
            CssMetaData<TextFlow,Number> LINE_SPACING =
                new CssMetaData<>("-fx-line-spacing",
                SizeConverter.getInstance(), 0) {

            @Override public boolean isSettable(TextFlow node) {
                return node.lineSpacing == null || !node.lineSpacing.isBound();
            }

            @Override public StyleableProperty<Number> getStyleableProperty(TextFlow node) {
                return (StyleableProperty<Number>)node.lineSpacingProperty();
            }
        };

        private static final CssMetaData<TextFlow, Number> TAB_SIZE =
                new CssMetaData<>("-fx-tab-size",
                SizeConverter.getInstance(), TextLayout.DEFAULT_TAB_SIZE) {

            @Override
            public boolean isSettable(TextFlow node) {
                return node.tabSize == null || !node.tabSize.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TextFlow node) {
                return (StyleableProperty<Number>)node.tabSizeProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Pane.getClassCssMetaData());
            styleables.add(TEXT_ALIGNMENT);
            styleables.add(LINE_SPACING);
            styleables.add(TAB_SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /* The methods in this section are copied from Region due to package visibility restriction */
    static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }

    double computeChildPrefAreaWidth(Node child, Insets margin) {
        return computeChildPrefAreaWidth(child, margin, -1);
    }

    double computeChildPrefAreaWidth(Node child, Insets margin, double height) {
        double top = margin != null? snapSpaceY(margin.getTop()) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom()) : 0;
        double left = margin != null? snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null? snapSpaceX(margin.getRight()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            alt = snapSizeY(boundedSize(
                    child.minHeight(-1), height != -1? height - top - bottom :
                           child.prefHeight(-1), child.maxHeight(-1)));
        }
        return left + snapSizeX(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin) {
        return computeChildPrefAreaHeight(child, margin, -1);
    }

    double computeChildPrefAreaHeight(Node child, Insets margin, double width) {
        double top = margin != null? snapSpaceY(margin.getTop()) : 0;
        double bottom = margin != null? snapSpaceY(margin.getBottom()) : 0;
        double left = margin != null? snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null? snapSpaceX(margin.getRight()) : 0;
        double alt = -1;
        if (child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            alt = snapSizeX(boundedSize(
                    child.minWidth(-1), width != -1? width - left - right :
                           child.prefWidth(-1), child.maxWidth(-1)));
        }
        return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
    }
    /* end of copied code */

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case TEXT: {
                String accText = getAccessibleText();
                if (accText != null && !accText.isEmpty()) return accText;

                StringBuilder title = new StringBuilder();
                for (Node node: getChildren()) {
                    Object text = node.queryAccessibleAttribute(AccessibleAttribute.TEXT, parameters);
                    if (text != null) {
                        title.append(text.toString());
                    }
                }
                return title.toString();
            }
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
