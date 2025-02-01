/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SideDecorator;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.ContentChange;
import jfx.incubator.scene.control.richtext.model.ParagraphDirection;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

/**
 * Contains all the parts representing the visuals of the RichTextAreaSkin.
 *
 * Component hierarchy (all the way to the top):
 * <pre>
 *  RichTextArea (Region) .rich-text-area
 *    └─ VFlow (Pane) .flow
 *        ├─ left gutter (ClippedPane) .left-side
 *        ├─ right gutter (ClippedPane) .right-side
 *        ├─ vertical ScrollBar
 *        ├─ horizontal ScrollBar
 *        └─ view port (ClippedPane) .vport
 *            └─ content (StackPane) .content
 *                ├─ caret (Path) .caret
 *                ├─ cells[]
 *                ├─ selection highlight (Path) .selection-highlight
 *                └─ caret line highlight (Path) .caret-line
 * </pre>
 */
public class VFlow extends Pane implements StyleResolver, StyledTextModel.Listener {
    private final RichTextAreaSkin skin;
    private final RichTextArea control;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;
    private final ClippedPane leftGutter;
    private final ClippedPane rightGutter;
    private final Pane content;
    private final ClippedPane vport;
    private final Path caretPath;
    private final Path caretLineHighlight;
    private final Path selectionHighlight;
    private final SimpleBooleanProperty caretVisible = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty suppressBlink = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty offsetX = new SimpleDoubleProperty(0.0);
    private final ReadOnlyObjectWrapper<Origin> origin = new ReadOnlyObjectWrapper(Origin.ZERO);
    private final Timeline caretAnimation;
    private final FastCache<TextCell> cellCache;
    private CellArrangement arrangement;
    private boolean dirty = true;
    private FastCache<Node> leftCache;
    private FastCache<Node> rightCache;
    private boolean handleScrollEvents = true;
    private boolean vsbPressed;
    private double contentPaddingTop;
    private double contentPaddingBottom;
    private double contentPaddingLeft;
    private double contentPaddingRight;
    private double leftSide;
    private double rightSide;
    private boolean inReflow;
    private double unwrappedContentWidth;
    private double viewPortWidth;
    private double viewPortHeight;
    private static final Text measurer = makeMeasurer();
    private static final VFlowCellContext context = new VFlowCellContext();

    public VFlow(RichTextAreaSkin skin, ScrollBar vsb, ScrollBar hsb) {
        this.skin = skin;
        this.control = skin.getSkinnable();
        this.vscroll = vsb;
        this.hscroll = hsb;

        vscroll.setManaged(false);
        vscroll.setMin(0.0);
        vscroll.setMax(1.0);
        vscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        vscroll.setBlockIncrement(Params.SCROLL_BARS_BLOCK_INCREMENT);

        hscroll.setManaged(false);
        hscroll.setMin(0.0);
        hscroll.setMax(1.0);
        hscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        hscroll.setBlockIncrement(Params.SCROLL_BARS_BLOCK_INCREMENT);

        cellCache = new FastCache(Params.CELL_CACHE_SIZE);

        getStyleClass().add("vflow");
        setPadding(new Insets(Params.LAYOUT_FOCUS_BORDER));

        // TODO consider creating on demand
        leftGutter = new ClippedPane("left-side");
        leftGutter.setManaged(false);
        // TODO consider creating on demand
        rightGutter = new ClippedPane("right-side");
        rightGutter.setManaged(false);

        vport = new ClippedPane("vport");
        vport.setManaged(false);

        content = new Pane();
        content.getStyleClass().add("content");
        content.setManaged(false);

        caretPath = new Path();
        caretPath.getStyleClass().add("caret");
        caretPath.setManaged(false);

        caretLineHighlight = new Path();
        caretLineHighlight.getStyleClass().add("caret-line");
        caretLineHighlight.setManaged(false);

        selectionHighlight = new Path();
        selectionHighlight.getStyleClass().add("selection-highlight");
        selectionHighlight.setManaged(false);

        // layout
        getChildren().addAll(leftGutter, rightGutter, vscroll, hscroll, vport);
        vport.getChildren().addAll(content);
        content.getChildren().addAll(caretLineHighlight, selectionHighlight, caretPath);
        // caret on top, then the cells (visual order = 0), then the selection highlight, then the caret line
        caretPath.setViewOrder(-10);
        selectionHighlight.setViewOrder(10);
        caretLineHighlight.setViewOrder(20);

        caretAnimation = new Timeline();
        caretAnimation.setCycleCount(Animation.INDEFINITE);

        caretPath.visibleProperty().bind(new BooleanBinding() {
            {
                bind(
                    caretVisible,
                    control.displayCaretProperty(),
                    control.focusedProperty(),
                    control.disabledProperty(),
                    suppressBlink
                );
            }

            @Override
            protected boolean computeValue() {
                return
                    (isCaretVisible() || suppressBlink.get()) &&
                    control.isDisplayCaret() &&
                    control.isFocused() &&
                    (!control.isDisabled());
            }
        });

        offsetX.addListener((p) -> updateHorizontalScrollBar());
        origin.addListener((p) -> handleOriginChange());
        widthProperty().addListener((p) -> handleWidthChange());
        heightProperty().addListener((p) -> handleHeightChange());

        vscroll.addEventFilter(MouseEvent.ANY, this::handleVScrollMouseEvent);

        updateHorizontalScrollBar();
        handleOriginChange();
    }

    public void dispose() {
        caretPath.visibleProperty().unbind();
    }

    public Pane getContentPane() {
        return content;
    }

    private static Text makeMeasurer() {
        Text t = new Text("8");
        t.setManaged(false);
        return t;
    }

    public void handleModelChange() {
        setOrigin(new Origin(0, -contentPaddingTop));
        handleWrapText();
//        setUnwrappedContentWidth(0.0);
//        setOffsetX(0.0);
//        requestControlLayout(true);
//        control.select(TextPos.ZERO);
    }

    public void handleWrapText() {
        if (control.isWrapText()) {
            double w = viewPortWidth;
            setUnwrappedContentWidth(w);
        } else {
            setUnwrappedContentWidth(0.0);
        }
        setOffsetX(0.0);

        layoutChildren();
        updateHorizontalScrollBar();
        updateVerticalScrollBar();
        requestControlLayout(true);
    }

    public void handleDecoratorChange() {
        leftCache = updateSideCache(control.getLeftDecorator(), leftCache);
        rightCache = updateSideCache(control.getRightDecorator(), rightCache);
        requestControlLayout(false);
        updateHorizontalScrollBar();
    }

    private FastCache<Node> updateSideCache(SideDecorator decorator, FastCache<Node> cache) {
        if (decorator == null) {
            if (cache != null) {
                cache.clear();
            }
        } else {
            if (cache == null) {
                cache = new FastCache<>(Params.CELL_CACHE_SIZE);
            } else {
                cache.clear();
            }
        }
        return cache;
    }

    public void invalidateLayout() {
        cellCache.clear();
        requestLayout();
        updateHorizontalScrollBar(); // defer?
        updateVerticalScrollBar(); // defer?
    }

    public void handleContentPadding() {
        Insets m = control.getContentPadding();
        if (m == null) {
            m = Insets.EMPTY;
        }

        contentPaddingLeft = snapPositionX(m.getLeft());
        contentPaddingRight = snapPositionX(m.getRight());
        contentPaddingTop = snapPositionY(m.getTop());
        contentPaddingBottom = snapPositionY(m.getBottom());

        setOffsetX(0.0);

        if (getOrigin().index() == 0) {
            setOrigin(new Origin(0, -contentPaddingTop));
        }

        requestLayout();
        updateHorizontalScrollBar();
        updateVerticalScrollBar();
    }

    public double leftPadding() {
        return contentPaddingLeft;
    }

    /** Location of the top left corner. */
    public final ReadOnlyProperty<Origin> originProperty() {
        return origin.getReadOnlyProperty();
    }

    public final Origin getOrigin() {
        return origin.get();
    }

    private void setOrigin(Origin or) {
        if (or == null) {
            throw new NullPointerException();
        }
        // prevent scrolling
        if (control.isUseContentHeight() || ((or.index() == 0) && (or.offset() == 0))) {
            or = new Origin(0, -contentPaddingTop);
        }
        origin.set(or);
    }

    private void handleOriginChange() {
        if (!inReflow) {
            requestLayout();
        }
    }

    public int topCellIndex() {
        return getOrigin().index();
    }

    public final double getOffsetX() {
        return offsetX.get();
    }

    private final void setOffsetX(double x) {
        // prevent scrolling
        if (control.isUseContentWidth()) {
            x = 0.0;
        } else {
            x = Math.max(0.0, snapPositionX(x));
        }
        offsetX.set(x);
        content.setTranslateX(-x);
    }

    private void setUnwrappedContentWidth(double w) {
        double min = snapPositionX(Params.LAYOUT_MIN_WIDTH);
        if (w < min) {
            w = min;
        }
        unwrappedContentWidth = w;
    }

    /** returns the content width, including padding and horizontal guard, snapped */
    private double contentWidth() {
        return unwrappedContentWidth + contentPaddingLeft + contentPaddingRight + snapPositionX(Params.HORIZONTAL_GUARD);
    }

    public void setCaretVisible(boolean on) {
        caretVisible.set(on);
    }

    public boolean isCaretVisible() {
        return caretVisible.get();
    }

    /** reacts to width changes */
    void handleWidthChange() {
        if (!control.isWrapText()) {
            // scroll horizontally when expanding beyond right boundary
            double delta = unwrappedContentWidth + contentPaddingRight - getOffsetX() - viewPortWidth;
            if (delta < 0.0) {
                double off = getOffsetX() + delta;
                if (off > -contentPaddingLeft) {
                    setOffsetX(off);
                }
            }

            updateHorizontalScrollBar();
        }
        requestLayout();
    }

    /** reacts to height changes */
    void handleHeightChange() {
        requestLayout();
    }

    public void handleSelectionChange() {
        setSuppressBlink(true);
        updateCaretAndSelection();
        scrollCaretToVisible();
        setSuppressBlink(false);
    }

    public void updateCaretAndSelection() {
        if (arrangement == null) {
            removeCaretAndSelection();
            return;
        }

        TextPos caret = control.getCaretPosition();
        if (caret == null) {
            removeCaretAndSelection();
            return;
        }

        TextPos anchor = control.getAnchorPosition();
        if (anchor == null) {
            anchor = caret;
        }

        // current line highlight
        if (control.isHighlightCurrentParagraph()) {
            FxPathBuilder b = new FxPathBuilder();
            createCurrentLineHighlight(b, caret);
            caretLineHighlight.getElements().setAll(b.getPathElements());
        } else {
            caretLineHighlight.getElements().clear();
        }

        // selection
        FxPathBuilder b = new FxPathBuilder();
        createSelectionHighlight(b, anchor, caret);
        selectionHighlight.getElements().setAll(b.getPathElements());

        // caret
        b = new FxPathBuilder();
        createCaretPath(b, caret);
        caretPath.getElements().setAll(b.getPathElements());
    }

    protected void removeCaretAndSelection() {
        caretLineHighlight.getElements().clear();
        selectionHighlight.getElements().clear();
        caretPath.getElements().clear();
    }

    protected void createCaretPath(FxPathBuilder b, TextPos p) {
        CaretInfo c = getCaretInfo(p);
        if (c != null) {
            b.addAll(c.path());
        }
    }

    protected void createSelectionHighlight(FxPathBuilder b, TextPos start, TextPos end) {
        // probably unnecessary
        if ((start == null) || (end == null)) {
            return;
        }

        int eq = start.compareTo(end);
        if (eq == 0) {
            return;
        } else if (eq > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }

        int topCellIndex = topCellIndex();
        if (end.index() < topCellIndex) {
            // selection is above visible area
            return;
        } else if (start.index() >= (topCellIndex + arrangement().getVisibleCellCount())) {
            // selection is below visible area
            return;
        }

        // get selection shapes for top and bottom segments,
        // translated to this VFlow coordinates.
        PathElement[] top;
        PathElement[] bottom;
        if (start.index() == end.index()) {
            top = getRangeShape(start.index(), start.offset(), end.offset());
            bottom = null;
        } else {
            top = getRangeShape(start.index(), start.offset(), -1);
            if (top == null) {
                top = getRangeTop();
            }

            bottom = getRangeShape(end.index(), 0, end.offset());
            if (bottom == null) {
                bottom = getRangeBottom();
            }
        }

        // generate shapes
        double left = -contentPaddingLeft;
        double right;
        if (control.isWrapText()) {
            right = getWidth();
        } else {
            right = Math.max(getWidth(), contentWidth());
        }
        boolean topLTR = true;
        boolean bottomLTR = true;

        double lineSpacing = 0.0; // FIX JDK-8317120
        new SelectionHelper(b, left, right).generate(top, bottom, topLTR, bottomLTR, contentPaddingLeft, lineSpacing);
    }

    protected void createCurrentLineHighlight(FxPathBuilder b, TextPos caret) {
        int ix = caret.index();
        TextCell cell = arrangement().getVisibleCell(ix);
        if (cell != null) {
            double w;
            if (control.isWrapText()) {
                w = getWidth();
            } else {
                w = Math.max(getWidth(), contentWidth());
            }
            cell.addBoxOutline(b, 0.0, snapPositionX(w), cell.getCellHeight());
        }
    }

    /** uses vflow.content cooridinates */
    public TextPos getTextPosLocal(double localX, double localY) {
        return arrangement().getTextPos(localX - contentPaddingLeft, localY);
    }

    /** in vflow.content coordinates */
    protected CaretInfo getCaretInfo(TextPos p) {
        return arrangement().getCaretInfo(content, p);
    }

    /** returns caret sizing info using vflow.content coordinates, or null */
    public CaretInfo getCaretInfo() {
        TextPos p = control.getCaretPosition();
        if (p == null) {
            return null;
        }
        return getCaretInfo(p);
    }

    protected PathElement[] getRangeTop() {
        double w = getWidth();
        return new PathElement[] {
            new MoveTo(0, -1),
            new LineTo(w, -1),
            new LineTo(w, 0),
            new LineTo(0, 0),
            new LineTo(0, -1)
        };
    }

    protected PathElement[] getRangeBottom() {
        double w = getWidth();
        double h = getHeight();
        double h1 = h + 1.0;

        return new PathElement[] {
            new MoveTo(0, h),
            new LineTo(w, h),
            new LineTo(w, h1),
            new LineTo(0, h1),
            new LineTo(0, h)
        };
    }

    /** returns the shape if both ends are at the same line, in VFlow coordinates */
    public PathElement[] getRangeShape(int line, int startOffset, int endOffset) {
        TextCell cell = arrangement().getVisibleCell(line);
        if (cell == null) {
            return null;
        }

        if (endOffset < 0) {
            // FIX to the edge?? but beware of RTL
            endOffset = cell.getTextLength();
        }

        PathElement[] pe;
        if (startOffset == endOffset) {
            // TODO handle split caret!
            pe = cell.getCaretShape(content, startOffset, true);
        } else {
            pe = cell.getRangeShape(content, startOffset, endOffset);
        }
        return pe;
    }

    public final void setSuppressBlink(boolean on) {
        suppressBlink.set(on);
        if (!on) {
            updateRateRestartBlink();
        }
    }

    public final void updateRateRestartBlink() {
        Duration t2 = control.getCaretBlinkPeriod();
        Duration t1 = t2.divide(2.0);

        caretAnimation.stop();
        caretAnimation.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO, (ev) -> setCaretVisible(true)),
            new KeyFrame(t1, (ev) -> setCaretVisible(false)),
            new KeyFrame(t2)
        );
        caretAnimation.play();
    }

    public final int getParagraphCount() {
        return control.getParagraphCount();
    }

    /**
     * Returns control's content padding, always non-null.
     * @return the content padding
     */
    public final Insets contentPadding() {
        Insets m = control.getContentPadding();
        return m == null ? Insets.EMPTY : m;
    }

    private void handleVScrollMouseEvent(MouseEvent ev) {
        EventType<? extends MouseEvent> t = ev.getEventType();
        if (t == MouseEvent.MOUSE_PRESSED) {
            vsbPressed = true;
        } else if (t == MouseEvent.MOUSE_RELEASED) {
            vsbPressed = false;
            updateVerticalScrollBar();
        }
    }

    /** updates VSB in response to change in height, layout, or offsetY */
    protected void updateVerticalScrollBar() {
        double visible;
        double val;
        if (getParagraphCount() == 0) {
            visible = 1.0;
            val = 0.0;
        } else {
            CellArrangement ar = arrangement();
            double av = ar.averageHeight();
            double max = ar.estimatedMax();
            double h = getViewPortHeight();
            val = toScrollBarValue((topCellIndex() - ar.topCount()) * av + ar.topHeight(), h, max);
            visible = h / max;
        }

        handleScrollEvents = false;

        vscroll.setVisibleAmount(visible);
        vscroll.setValue(val);

        handleScrollEvents = true;
    }

    /** handles user moving the vertical scroll bar */
    public void handleVerticalScroll() {
        if (handleScrollEvents) {
            if (getParagraphCount() == 0) {
                return;
            }

            // When scrolling virtualized views, we cannot rely on caching of cell heights as it's being done
            // in the VirtualFlow.  Instead, we must approximate using the information provided to us by the
            // sliding window.
            //
            // 1. rough positioning by using index = pos * (lineCount - 1)
            // 2. compute resulting position' based on (estimated) pixel counts
            //      pos' = topPixels / (topPixels + bottomPixels - viewportH)
            //    where
            //      topPixels = topPad + (topIndex)*av + topHeight
            //      bottomPixels = bottomPad + bottomHeight + (lineCount - origin.ix - bottomCount)*av
            // 3. then adjust by scrolling by pixels
            //      dy = (pos - pos') * totalPixels
            //    where
            //      totalPixels = topPixels + bottomPixels - viewportH
            //
            // there might still be some flicker due to the hsb appearing and disappearing

            double max = vscroll.getMax();
            double min = vscroll.getMin();
            double val = vscroll.getValue();
            double pos = (val - min) / max;

            int lineCount = getParagraphCount();
            int ix = Math.max(0, (int)Math.round(pos * (lineCount - 1)));
            Origin p = new Origin(ix, 0.0);
            setOrigin(p);
            layoutCells();

            CellArrangement a = arrangement();
            int topIx = a.topIndex();
            double topH = a.topHeight();
            double bottomH = a.bottomHeight();
            int cellCount = a.cellCount();
            double av = a.averageHeight();
            int originIx = getOrigin().index();
            double viewH = getViewPortHeight();

            double topPixels = contentPaddingTop + (topIx * av) + topH;
            double bottomPixels = bottomH + (lineCount - topIx - cellCount) * av + contentPaddingBottom;
            double totalScroll = Math.max(0.0, (topPixels + bottomPixels - viewH));
            double pos1 = topPixels / totalScroll;
            double dy = (pos - pos1) * totalScroll;

            scrollVerticalPixels(dy);
            layoutChildren();
        }
    }

    /** updates HSB in response to change in width, layout, or offsetX */
    protected void updateHorizontalScrollBar() {
        if (control.isWrapText()) {
            return;
        }

        double max = contentWidth();
        double w = vport.getWidth();
        double off = getOffsetX();
        double vis = w / max;
        double val = toScrollBarValue(off, w, max);

        handleScrollEvents = false;

        hscroll.setVisibleAmount(vis);
        hscroll.setValue(val);

        handleScrollEvents = true;
    }

    /** handles user moving the horizontal scroll bar */
    public void handleHorizontalScroll() {
        if (handleScrollEvents) {
            if ((arrangement == null) || control.isWrapText()) {
                return;
            }

            double max = contentWidth();
            double visible = vport.getWidth();
            double val = hscroll.getValue();
            double off = fromScrollBarValue(val, visible, max);

            setOffsetX(off);
        }
    }

    /**
     * javafx ScrollBar is weird in that the value has a range between [min,max] regardless of visible amount.
     * this method generates the value ScrollBar expects by renormalizing it to a [min,max-visible] range,
     * assuming min == 0.
     */
    private static double toScrollBarValue(double val, double visible, double max) {
        if (Math.abs(max - visible) < 1e-10) {
            return 0.0;
        } else {
            return val / (max - visible);
        }
    }

    /** inverse of {@link #toScrollBarValue}, returns the scroll bar value that takes into account visible amount */
    private static double fromScrollBarValue(double val, double visible, double max) {
        return val * (max - visible);
    }

    public TextCell getCell(int modelIndex) {
        return arrangement().getCell(modelIndex);
    }

    private TextCell createTextCell(int index, RichParagraph par) {
        if(par == null) {
            return null;
        }
        TextCell cell;
        StyleAttributeMap pa = par.getParagraphAttributes();
        Supplier<Region> gen = par.getParagraphRegion();
        if (gen != null) {
            // it's a paragraph node
            Region content = gen.get();
            cell = new TextCell(index, content);
        } else {
            // it's a regular text cell
            cell = new TextCell(index);

            // first line indent operates on TextCell and not its content
            if (pa != null) {
                Double firstLineIndent = pa.getFirstLineIndent();
                if (firstLineIndent != null) {
                    cell.add(new FirstLineIndentSpacer(firstLineIndent));
                }
            }

            // highlights
            List<Consumer<TextCell>> highlights = RichParagraphHelper.getHighlights(par);
            if (highlights != null) {
                for (Consumer<TextCell> h : highlights) {
                    h.accept(cell);
                }
            }

            // segments
            List<StyledSegment> segments = RichParagraphHelper.getSegments(par);
            if ((segments == null) || segments.isEmpty()) {
                // a bit of a hack: avoid TextCells with an empty TextFlow,
                // otherwise it makes the caret collapse to a single point
                cell.add(createTextNode("", StyleAttributeMap.EMPTY));
            } else {
                for (StyledSegment seg : segments) {
                    switch (seg.getType()) {
                    case INLINE_NODE:
                        Node n = seg.getInlineNodeGenerator().get();
                        cell.add(n);
                        break;
                    case TEXT:
                        String text = seg.getText();
                        StyleAttributeMap a = seg.getStyleAttributeMap(this);
                        Text t = createTextNode(text, a);
                        cell.add(t);
                        break;
                    }
                }
            }
        }

        if (pa == null) {
            pa = StyleAttributeMap.EMPTY;
        } else {
            // these two attributes operate on TextCell instead of its content
            String bullet = pa.getBullet();
            if (bullet != null) {
                cell.setBullet(bullet);
            }

            if (control.isWrapText()) {
                ParagraphDirection d = pa.getParagraphDirection();
                if (d != null) {
                    switch (d) {
                    case LEFT_TO_RIGHT:
                        cell.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                        break;
                    case RIGHT_TO_LEFT:
                        cell.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                        break;
                    }
                }
            }
        }

        context.reset(cell.getContent(), pa);
        skin.applyStyles(context, pa, true);
        context.apply();

        return cell;
    }

    private Text createTextNode(String text, StyleAttributeMap attrs) {
        Text t = new Text(text);
        context.reset(t, attrs);
        skin.applyStyles(context, attrs, false);
        context.apply();
        return t;
    }

    private double computeSideWidth(SideDecorator d) {
        if (d != null) {
            double w = d.getPrefWidth(getWidth());
            if (w <= 0.0) {
                int top = topCellIndex();
                Node n = d.getMeasurementNode(top);
                n.setManaged(false);

                content.getChildren().add(n);
                try {
                    n.applyCss();
                    if (n instanceof Parent p) {
                        p.layout();
                    }
                    w = n.prefWidth(-1);
                } finally {
                    content.getChildren().remove(n);
                }
            }
            // apply some granularity in order to avoid left boundary jittering back and forth when scrolling
            double granularity = 10;
            w = (Math.round((w + 1.0) / granularity) + 1.0) * granularity;
            return snapSizeX(w);
        }
        return 0.0;
    }

    /** returns a non-null layout, laying out cells if necessary */
    protected CellArrangement arrangement() {
        if (!inReflow && dirty || (arrangement == null)) {
            layoutChildren();
        }
        return arrangement;
    }

    private double getLineSpacing(Region r) {
        if (r instanceof TextFlow f) {
            return f.getLineSpacing();
        }
        return 0.0;
    }

    public double getViewPortHeight() {
        return viewPortHeight;
    }

    public void pageUp() {
        scrollVerticalPixels(-getViewPortHeight());
    }

    public void pageDown() {
        scrollVerticalPixels(getViewPortHeight());
    }

    public void scrollVerticalFraction(double fractionOfHeight) {
        scrollVerticalPixels(getViewPortHeight() * fractionOfHeight);
    }

    /** scroll by a number of pixels, delta must not exceed the view height in absolute terms */
    public void scrollVerticalPixels(double delta) {
        Origin or = arrangement().moveOrigin(delta);
        if (or != null) {
            setOrigin(or);
        }
    }

    public void scrollHorizontalFraction(double delta) {
        double w = content.getWidth() + contentPaddingLeft + contentPaddingRight;
        scrollHorizontalPixels(delta * w);
    }

    public void scrollHorizontalPixels(double delta) {
        double x = getOffsetX() + delta;
        if ((x + vport.getWidth()) > contentWidth()) {
            x = contentWidth() - vport.getWidth();
        }
        setOffsetX(x);
    }

    /** x in vflow.content coordinate */
    private void scrollHorizontalToVisible(double x) {
        if (!control.isWrapText()) {
            double off;
            if (x < getOffsetX()) {
                off = x - Params.HORIZONTAL_GUARD;
            } else if (x > (getOffsetX() + vport.getWidth())) {
                off = x + Params.HORIZONTAL_GUARD - vport.getWidth();
            } else {
                return;
            }

            setOffsetX(off);
        }
    }

    /** scrolls to visible area, using vflow.content coordinates */
    public void scrollToVisible(double x, double y) {
        if (y < snappedTopInset()) {
            // above viewport
            scrollVerticalPixels(y - snappedTopInset());
        } else if (y >= getViewPortHeight()) {
            // below viewport
            scrollVerticalPixels(y - getViewPortHeight());
        }

        scrollHorizontalToVisible(x);
    }

    public void scrollCaretToVisible() {
        TextPos caret = control.getCaretPosition();
        if (caret == null) {
            // no caret
            return;
        }

        boolean reflow = false;
        CaretInfo c = getCaretInfo();
        if (c == null) {
            // caret is outside of the layout; let's set the origin first to the caret position
            // and then block scroll to avoid scrolling past the document end, if needed
            int ix = caret.index();
            Origin or = new Origin(ix, 0.0);
            boolean moveDown = (ix > getOrigin().index());
            setOrigin(or);
            c = getCaretInfo();
            if (moveDown) {
                scrollVerticalPixels(c.getMaxY() - c.getMinY() - getViewPortHeight());
            }
            checkForExcessiveWhitespaceAtTheEnd();
            reflow = true;
        } else {
            // block scroll, if needed
            if (c.getMinY() < snappedTopInset()) {
                scrollVerticalPixels(c.getMinY() - snappedTopInset());
                reflow = true;
            } else if (c.getMaxY() > getViewPortHeight()) {
                scrollVerticalPixels(c.getMaxY() - getViewPortHeight());
                reflow = true;
            }

            if (!control.isWrapText()) {
                // FIX primary caret
                double x = c.getMinX();
                if (x + contentPaddingLeft < 0.0) {
                    scrollHorizontalToVisible(x);
                } else {
                    scrollHorizontalToVisible(c.getMaxX());
                }
            }
        }

        if (reflow) {
            layout();
        }
    }

    protected void checkForExcessiveWhitespaceAtTheEnd() {
        double delta = arrangement().bottomHeight() - getViewPortHeight();
        if (delta < 0) {
            if (getOrigin().index() == 0) {
                if (getOrigin().offset() <= -contentPaddingTop) {
                    return;
                }
            }
            scrollVerticalPixels(delta);
        }
    }

    @Override
    public void onContentChange(ContentChange ch) {
        if (ch.isEdit()) {
            Origin newOrigin = computeNewOrigin(ch);
            if (newOrigin != null) {
                setOrigin(newOrigin);
            }
        }
        // TODO this could be more advanced to reduce the amount of re-computation and re-flow
        // TODO clear cache >= start, update layout
        cellCache.clear();
        // TODO rebuild from start.lineIndex()
        requestLayout();
    }

    private Origin computeNewOrigin(ContentChange ch) {
        int startIndex = ch.getStart().index();
        int endIndex = ch.getEnd().index();
        // TODO store position of the last visible symbol, use that to compare with 'start' to avoid reflow
        Origin or = getOrigin();
        int lineDelta = endIndex - startIndex + ch.getLinesAdded();

        // jump to start if the old origin is within the changed range
        if ((startIndex <= or.index()) && (or.index() < (startIndex + lineDelta))) {
            return new Origin(startIndex, 0);
        }

        // adjust index only if the end precedes the origin
        if (lineDelta != 0) {
            if (
                (endIndex < or.index()) ||
                (
                    (endIndex == or.index()) &&
                    (ch.getEnd().offset() < or.offset())
                )
            )
            {
                return new Origin(or.index() + lineDelta, or.offset());
            }
        }

        return null;
    }

    @Override
    public StyleAttributeMap resolveStyles(StyleAttributeMap attrs) {
        if (attrs == null) {
            return attrs;
        }
        CssStyles css = attrs.get(CssStyles.CSS);
        if (css == null) {
            // no conversion is needed
            return attrs;
        }

        String directStyle = css.style();
        String[] names = css.names();

        getChildren().add(measurer);
        try {
            measurer.setStyle(directStyle);
            if (names == null) {
                measurer.getStyleClass().clear();
            } else {
                measurer.getStyleClass().setAll(names);
            }
            measurer.applyCss();
            return RichUtils.fromTextNode(measurer);
        } finally {
            getChildren().remove(measurer);
        }
    }

    @Override
    public WritableImage snapshot(Node n) {
        n.setManaged(false);
        getChildren().add(n);
        try {
            n.applyCss();
            if (n instanceof Region r) {
                double w = unwrappedContentWidth;
                double h = r.prefHeight(w);
                RichUtils.layoutInArea(r, 0, -h, w, h);
            }
            return n.snapshot(null, null);
        } finally {
            getChildren().remove(n);
        }
    }

    public TextPos moveHorizontally(boolean start, int caretIndex, int caretOffset) {
        TextCell cell = getCell(caretIndex);
        Integer off = cell.lineEdge(start, caretIndex, caretOffset);
        if (off == null) {
            return null;
        } else if(start || off == 0) {
            return TextPos.ofLeading(caretIndex, off);
        } else {
            return new TextPos(caretIndex, off, off - 1, false);
        }
    }

    /**
     * Computes the new TextPos for the target coordinates.  This method takes into account
     * the geometry of text as determined by the layout, thus taking into account
     * line spacing and paragraph padding and borders.
     *
     * @param caretIndex the current caret index
     * @param x the target x coordinate
     * @param y the target y coordinate
     * @param down direction of the movement relative to the caret
     * @return the new text position, or null if no movement should occur
     */
    public TextPos moveVertically(int caretIndex, double x, double y, boolean down) {
        TextCell cell = getCell(caretIndex);
        // account for line spacing
        if (down) {
            y += cell.getLineSpacing();
        }

        double cy = y - cell.getY();
        boolean inside = cell.isInsideText(x - contentPaddingLeft, cy, down);
        TextPos p = getTextPosLocal(x, y);
        if (p == null) {
            return null; // should not happen
        } else if (inside) {
            return p;
        }

        int ix = p.index();
        if (ix == caretIndex) {
            if (down) {
                ix++;
                if (ix >= skin.getSkinnable().getParagraphCount()) {
                    return skin.getSkinnable().getDocumentEnd();
                }
            } else {
                ix--;
                if (ix < 0) {
                    return TextPos.ZERO;
                }
            }
        }

        cell = getCell(ix);
        double py = cell.findHitCandidate(y - cell.getY(), down);
        p = getTextPosLocal(x, py + cell.getY());
        return p;
    }

    public void handleUseContentHeight() {
        boolean on = control.isUseContentHeight();
        if (on) {
            setUnwrappedContentWidth(0.0);
            setOrigin(new Origin(0, -contentPaddingTop));
            setOffsetX(0.0);
        }
        requestControlLayout(false);
    }

    public void handleUseContentWidth() {
        boolean on = control.isUseContentWidth();
        if (on) {
            setUnwrappedContentWidth(0.0);
            setOrigin(new Origin(0, -contentPaddingTop));
            setOffsetX(0.0);
        }
        requestControlLayout(false);
    }

    @Override
    public void requestLayout() {
        dirty = true;
        super.requestLayout();
    }

    /**
     * Requests full layout with optional clearing of the cached cells.
     * @param clearCache if true, clears the cell cache
     */
    public void requestControlLayout(boolean clearCache) {
        if (clearCache) {
            cellCache.clear();
        }
        requestParentLayout();
        requestLayout();
    }

    private void updatePrefWidth() {
        if (!control.prefWidthProperty().isBound()) {
            double w =
                arrangement().getUnwrappedWidth() +
                snapSizeX(leftSide) +
                snapSizeX(rightSide) +
                contentPaddingLeft +
                contentPaddingRight +
                snapSizeX(Params.HORIZONTAL_GUARD);

            if (w >= 0.0) {
                if (vscroll.isVisible()) {
                    w += vscroll.getWidth();
                }
            }

            Parent parent = getParent();
            if (parent instanceof Region r) {
                if (r.getPrefWidth() != w) {
                    r.setPrefWidth(w);
                    control.getParent().requestLayout();
                    requestControlLayout(false);
                }
            }
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        if (control.isUseContentHeight()) {
            double h = snapSizeY(Math.max(Params.LAYOUT_MIN_HEIGHT, arrangement().bottomHeight())) + snappedTopInset() + snappedBottomInset();
            if (hscroll.isVisible()) {
                h += hscroll.prefHeight(width);
            }
            return h;
        }
        return super.computePrefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        if (control.isUseContentWidth()) {
            double w = contentWidth() + leftSide + rightSide + snappedLeftInset() + snappedRightInset();
            if (vscroll.isVisible()) {
                w += vscroll.prefWidth(height);
            }
            return w;
        }
        return super.computePrefWidth(height);
    }

    @Override
    protected void layoutChildren() {
        inReflow = true;
        try {
            layoutCells();

            checkForExcessiveWhitespaceAtTheEnd();
            updateCaretAndSelection();

            // eliminate VSB jitter during scrolling with a mouse
            // the VSB will finally get updated on mouse released event
            if (!vsbPressed) {
                updateVerticalScrollBar();
            }
        } finally {
            dirty = false;
            inReflow = false;
        }
    }

    // must be called only from layoutCells
    // adds the cell region to vflow content
    // performs the cell layout
    // adds cell to arrangement
    private TextCell prepareCell(int modelIndex, double maxWidth) {
        TextCell cell = cellCache.get(modelIndex);
        if (cell == null) {
            RichParagraph rp = control.getModel().getParagraph(modelIndex);
            cell = createTextCell(modelIndex, rp);
            cellCache.add(cell.getIndex(), cell);
        }

        // TODO skip computation if layout width is the same
        Region r = cell.getContent();
        content.getChildren().add(cell);
        cell.setMaxWidth(maxWidth);
        cell.setMaxHeight(USE_COMPUTED_SIZE);
        cell.applyCss();
        cell.layout();
        arrangement.addCell(cell);
        return cell;
    }

    /**
     * Recomputes sliding window and lays out scrollbars, left/right sides, and viewport.
     * This process might be repeated if one of the scroll bars changes its visibility as a result.
     * (up to 4 times worst case)
     */
    protected void layoutCells() {
        if (arrangement != null) {
            arrangement.removeNodesFrom(content);
            arrangement = null;
        }
        arrangement = new CellArrangement(this, contentPaddingTop, contentPaddingBottom);

        double width = getWidth();
        if (width == 0.0) {
            return;
        }

        double padTop = snappedTopInset();
        double padBottom = snappedBottomInset();
        double padLeft = snappedLeftInset();
        double padRight = snappedRightInset();

        // sides
        SideDecorator leftDecorator = control.getLeftDecorator();
        SideDecorator rightDecorator = control.getRightDecorator();
        leftSide = computeSideWidth(leftDecorator);
        rightSide = computeSideWidth(rightDecorator);

        int paragraphCount = getParagraphCount();
        boolean useContentHeight = control.isUseContentHeight();
        boolean useContentWidth = control.isUseContentWidth();
        boolean wrap = control.isWrapText() && !useContentWidth;
        double height = getHeight();
        double vsbWidth = vscroll.isVisible() ? vscroll.prefWidth(-1) : 0.0;
        double hsbHeight = hscroll.isVisible() ? hscroll.prefHeight(-1) : 0.0;

        double forWidth; // to be used for cell sizing in prefHeight()
        double maxWidth; // max width to apply before the layout (or replace with cell's preferred width?)
        if (wrap) {
            forWidth = width - leftSide - rightSide - contentPaddingLeft - contentPaddingRight - vsbWidth - padLeft - padRight;
            maxWidth = forWidth;
        } else {
            forWidth = -1.0;
            maxWidth = Params.MAX_WIDTH_FOR_LAYOUT;
        }

        // total height of visible cells for the purpose of determining vsb visibility
        double arrangementHeight = 0.0;
        double unwrappedWidth = 0.0;
        double ytop = snapPositionY(-getOrigin().offset());
        double y = ytop;
        int topMarginCount = Params.SLIDING_WINDOW_EXTENT;
        int bottomMargin = 0;;
        int count = 0;
        boolean cellOnScreen = true;

        // populating visible part of the sliding window + bottom margin
        int i = topCellIndex();
        for ( ; i < paragraphCount; i++) {
            TextCell cell = prepareCell(i, maxWidth);

            double h = cell.prefHeight(forWidth) + getLineSpacing(cell.getContent());
            h = snapSizeY(h);
            cell.setPosition(y, h);

            if (!wrap) {
                if (cellOnScreen) {
                    double w = cell.prefWidth(-1);
                    cell.setCellWidth(w);
                    if (w > unwrappedWidth) {
                        unwrappedWidth = w;
                    }
                }
            }

            y = snapPositionY(y + h);
            arrangementHeight += h;
            count++;

            if (useContentHeight) {
                // avoid laying out millions of invisible cells
                if (y > Params.MAX_HEIGHT_SAFEGUARD) {
                    break;
                }
            } else {
                // stop populating the bottom part of the sliding window
                // when exceeded both pixel and line count margins
                if (cellOnScreen) {
                    if (y > height) {
                        // reached the cell below the last visible cell at the bottom
                        arrangement.setVisibleCellCount(count);
                        cellOnScreen = false;

                        bottomMargin = count + Params.SLIDING_WINDOW_EXTENT;
                        int less = bottomMargin - getParagraphCount();
                        if (less > 0) {
                            // more cells on top
                            topMarginCount += less;
                        }
                    }
                } else {
                    // remove invisible cell from layout after sizing
                    content.getChildren().remove(cell);

                    if (count > bottomMargin) {
                        break;
                    }
                }
            }
        }

        // in case there are less paragraphs than can fit in the view
        if (cellOnScreen) {
            arrangement.setVisibleCellCount(count);
        }

        if (i == paragraphCount) {
            y += contentPaddingBottom;
        }

        // populate side nodes
        if (leftDecorator != null) {
            if (leftCache == null) {
                leftCache = updateSideCache(leftDecorator, null);
            }

            for (i = 0; i < arrangement.getVisibleCellCount(); i++) {
                TextCell cell = arrangement.getCellAt(i);
                int ix = cell.getIndex();
                Node n = leftCache.get(ix);
                if (n == null) {
                    n = leftDecorator.getNode(ix);
                    if (n != null) {
                        n.setManaged(false);
                        leftCache.add(ix, n);
                    }
                }
                if (n != null) {
                    arrangement.addLeftNode(i, n);
                }
            }
        }

        if (rightDecorator != null) {
            if (rightCache == null) {
                rightCache = updateSideCache(rightDecorator, null);
            }

            for (i = 0; i < arrangement.getVisibleCellCount(); i++) {
                TextCell cell = arrangement.getCellAt(i);
                int ix = cell.getIndex();
                Node n = rightCache.get(ix);
                if (n == null) {
                    n = rightDecorator.getNode(cell.getIndex());
                    if (n != null) {
                        n.setManaged(false);
                        rightCache.add(ix, n);
                    }
                }
                if (n != null) {
                    arrangement.addRightNode(i, n);
                }
            }
        }

        unwrappedWidth = snapSizeX(unwrappedWidth);

        arrangement.setBottomCount(count);
        arrangement.setBottomHeight(y);
        arrangement.setUnwrappedWidth(unwrappedWidth);
        count = 0;
        y = ytop;

        // populate top margin, going backwards from topCellIndex
        for (i = topCellIndex() - 1; i >= 0; i--) {
            TextCell cell = prepareCell(i, maxWidth);

            double h = cell.prefHeight(forWidth) + getLineSpacing(cell.getContent());
            h = snapSizeY(h);
            y = snapPositionY(y - h);
            cell.setPosition(y, h);
            count++;

            cell.setPosition(y, h);

            content.getChildren().remove(cell);

            // stop populating the top part of the sliding window
            // when exceeded both pixel and line count margins
            if (count > topMarginCount) {
                break;
            }
        }

        arrangement.setTopHeight(-y);

        if (useContentWidth) {
            width = unwrappedWidth + leftSide + rightSide + contentPaddingLeft + contentPaddingRight + padLeft + padRight;
        }

        viewPortWidth = width - leftSide - rightSide - vsbWidth - padLeft - padRight;
        if (viewPortWidth < Params.MIN_VIEWPORT_WIDTH) {
            viewPortWidth = Params.MIN_VIEWPORT_WIDTH;
        }
        viewPortHeight = height - hsbHeight - padTop - padBottom;

        // layout

        // scroll bars
        boolean vsbVisible = useContentHeight ?
            false :
            (topCellIndex() > 0) ?
                true :
                (arrangementHeight + contentPaddingTop + contentPaddingBottom) > viewPortHeight;

        if (vsbVisible != vscroll.isVisible()) {
            vscroll.setVisible(vsbVisible);
            // do another layout pass with the scrollbar updated
            layoutCells();
            return;
        }
        if (vsbVisible) {
            width -= vsbWidth;
        }

        boolean hsbVisible = (wrap || useContentWidth) ?
            false :
            (unwrappedWidth + contentPaddingLeft + contentPaddingRight) > viewPortWidth;

        if (hscroll.isVisible() != hsbVisible) {
            hscroll.setVisible(hsbVisible);
            // do another layout pass with the scrollbar updated
            layoutCells();
            return;
        }

        double h;
        if (useContentHeight) {
            h = contentPaddingTop + contentPaddingBottom + arrangementHeight;
        } else {
            h = height - hsbHeight - padTop - padBottom;
        }

        if (vsbVisible) {
            RichUtils.layoutInArea(vscroll, width - padRight, padTop, vsbWidth, h);
        }
        if (hsbVisible) {
            RichUtils.layoutInArea(hscroll, padLeft, h, width - padLeft - padRight, hsbHeight);
        }

        // gutters
        if (leftDecorator == null) {
            leftGutter.setVisible(false);
        } else {
            leftGutter.setVisible(true);
            RichUtils.layoutInArea(leftGutter, padLeft, padTop, leftSide, h);
        }

        if (rightDecorator == null) {
            rightGutter.setVisible(false);
        } else {
            rightGutter.setVisible(true);
            RichUtils.layoutInArea(rightGutter, width - rightSide - padRight, padTop, rightSide, h);
        }

        RichUtils.layoutInArea(vport, leftSide + padLeft, padTop, viewPortWidth, h);
        // vport is a child of content
        RichUtils.layoutInArea(content, 0.0, 0.0, viewPortWidth, h);

        if (wrap) {
            double w = viewPortWidth;
            setUnwrappedContentWidth(w);
        } else {
            if (unwrappedContentWidth != unwrappedWidth) {
                setUnwrappedContentWidth(unwrappedWidth);
                updateHorizontalScrollBar();

                if (useContentWidth) {
                    requestControlLayout(false);
                }
            }
        }

        if (useContentWidth) {
            updatePrefWidth();
        }

        if (useContentHeight) {
            double ph = computePrefHeight(-1);
            double prev = getPrefHeight();

            // necessary for vertical stacking
            setPrefHeight(ph);
            requestParentLayout();

            // avoids infinite layout loop in MultipleStackedBoxWindow but ... why?
            if (ph != prev) {
                requestLayout();
                // FIX perhaps create a boolean for 'reflow is required' and layoutCells again at the end?
                // weird, need this to make sure the reflow happens when changing models
                Platform.runLater(() -> layoutChildren());
            }
        } else {
            if (getPrefHeight() != USE_COMPUTED_SIZE) {
                setPrefHeight(USE_COMPUTED_SIZE);
            }
        }

        // position cells

        leftGutter.getChildren().clear();
        rightGutter.getChildren().clear();

        boolean addLeft = control.getLeftDecorator() != null;
        boolean addRight = control.getRightDecorator() != null;
        double x = wrap ? 0.0 : contentPaddingLeft;

        int sz = arrangement.getVisibleCellCount();
        for (i = 0; i < sz; i++) {
            TextCell cell = arrangement.getCellAt(i);
            double ch = cell.getCellHeight();
            double cy = cell.getY();
            double cw = wrap ? viewPortWidth : cell.getCellWidth();
            RichUtils.layoutInArea(cell, x, cy, cw, ch);

            // needed to get the correct caret path afterwards
            cell.layout();

            // place side nodes
            if (addLeft) {
                Node n = arrangement.getLeftNodeAt(i);
                if (n != null) {
                    leftGutter.getChildren().add(n);
                    n.applyCss();
                    RichUtils.layoutInArea(n, 0.0, cy, leftGutter.getWidth(), ch);
                }
            }

            if (addRight) {
                Node n = arrangement.getRightNodeAt(i);
                if (n != null) {
                    rightGutter.getChildren().add(n);
                    n.applyCss();
                    RichUtils.layoutInArea(n, 0.0, cy, rightGutter.getWidth(), ch);
                }
            }
        }
    }
}
