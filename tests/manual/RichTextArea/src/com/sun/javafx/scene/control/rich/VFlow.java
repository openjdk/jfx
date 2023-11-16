/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollBar;
import javafx.incubator.scene.control.rich.CaretInfo;
import javafx.incubator.scene.control.rich.ConfigurationParameters;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.SideDecorator;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.CssStyles;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledSegment;
import javafx.incubator.scene.control.rich.skin.RichTextAreaSkin;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Virtual text flow deals with TextCells, scroll bars, and conversion
 * between the model and the screen coordinates.
 */
public class VFlow extends Pane implements StyleResolver {
    private final RichTextArea control;
    private final ConfigurationParameters config;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;
    private final ClippedPane leftGutter;
    private final ClippedPane rightGutter;
    private final StackPane content;
    private final ClippedPane flow;
    private final Path caretPath;
    private final Path caretLineHighlight;
    private final Path selectionHighlight;
    private final SimpleBooleanProperty caretVisible = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty suppressBlink = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty offsetX = new SimpleDoubleProperty(0.0);
    private final SimpleDoubleProperty contentWidth = new SimpleDoubleProperty(0.0);
    private final ReadOnlyObjectWrapper<Origin> origin = new ReadOnlyObjectWrapper(Origin.ZERO);
    private final Timeline caretAnimation;
    private final FastCache<TextCell> cellCache;
    private CellArrangement arrangement;
    private boolean dirty = true;
    private FastCache<Node> leftCache;
    private FastCache<Node> rightCache;
    private boolean handleScrollEvents = true;
    private boolean vsbPressed;
    private double topPadding;
    private double bottomPadding;
    private double leftPadding;
    private double rightPadding;
    private double leftSide;
    private double rightSide;
    private boolean inReflow;
    private static final Text measurer = makeMeasurer();

    public VFlow(RichTextAreaSkin skin, ConfigurationParameters c, ScrollBar vscroll, ScrollBar hscroll) {
        this.control = skin.getSkinnable();
        this.config = c;
        this.vscroll = vscroll;
        this.hscroll = hscroll;

        cellCache = new FastCache(Params.CELL_CACHE_SIZE);

        getStyleClass().add("vflow");

        // TODO consider creating on demand
        leftGutter = new ClippedPane("left-side");
        leftGutter.setManaged(false);
        // TODO consider creating on demand
        rightGutter = new ClippedPane("right-side");
        rightGutter.setManaged(false);

        content = new StackPane();
        content.getStyleClass().add("content");
        content.setManaged(false);

        flow = new ClippedPane("flow");
        flow.setManaged(true);

        caretPath = new Path();
        caretPath.getStyleClass().add("caret");
        caretPath.setManaged(false);
        caretPath.setStrokeWidth(1.0);

        caretLineHighlight = new Path();
        caretLineHighlight.getStyleClass().add("caret-line");
        caretLineHighlight.setManaged(false);

        selectionHighlight = new Path();
        selectionHighlight.getStyleClass().add("selection-highlight");
        selectionHighlight.setManaged(false);

        // layout
        content.getChildren().addAll(caretLineHighlight, selectionHighlight, flow, caretPath);
        getChildren().addAll(content, leftGutter, rightGutter);

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
        
        contentWidth.addListener((p) -> updateHorizontalScrollBar());
        offsetX.addListener((p) -> updateHorizontalScrollBar());
        origin.addListener((p) -> handleOriginChange());
        widthProperty().addListener((p) -> updateWidth()); // TODO check for use content width?
        
        vscroll.addEventFilter(MouseEvent.ANY, this::handleVScrollMouseEvent);
        
        updateHorizontalScrollBar();
        handleOriginChange();
    }

    public void dispose() {
        caretPath.visibleProperty().unbind();
    }

    public Pane getContentPane() {
        return flow;
    }

    private static Text makeMeasurer() {
        Text t = new Text("8");
        t.setManaged(false);
        return t;
    }

    public void handleModelChange() {
        setContentWidth(0.0);
        setOrigin(new Origin(0, -topPadding));
        setOffsetX(-leftPadding);
        requestControlLayout(true);
        control.setCaret(TextPos.ZERO);
    }

    protected double wrappedWidth() {
        double w = getWidth() - leftPadding - rightPadding - leftSide - rightSide;
        w = Math.max(w, 0.0);
        return snapSpaceX(w);
    }

    public void handleWrapText() {
        if (control.isWrapText()) {
            double w = wrappedWidth();
            setContentWidth(w);
        } else {
            setContentWidth(0.0);
        }
        setOffsetX(-leftPadding);

        updateHorizontalScrollBar();
        updateVerticalScrollBar();
        requestControlLayout(true);
    }

    public void handleDecoratorChange() {
        leftCache = updateSideCache(control.getLeftDecorator(), leftCache);
        rightCache = updateSideCache(control.getRightDecorator(), rightCache);
        requestControlLayout(false);
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

    public void handleDefaultParagraphAttributes() {
        cellCache.clear();
        requestLayout();
        updateHorizontalScrollBar();
        updateVerticalScrollBar();
    }

    public void handleContentPadding() {
        updateContentPadding();
        
        setOffsetX(-leftPadding);

        if (getOrigin().index() == 0) {
            if (getOrigin().offset() < -topPadding) {
                setOrigin(new Origin(0, -topPadding));
            }
        }

        requestLayout();
        updateHorizontalScrollBar();
        updateVerticalScrollBar();
    }

    public void updateContentPadding() {
        Insets m = control.getContentPadding();
        if (m == null) {
            leftPadding = 0.0;
            rightPadding = 0.0;
            topPadding = 0.0;
            bottomPadding = 0.0;
        } else {
            leftPadding = snapPositionX(m.getLeft());
            rightPadding = snapPositionX(m.getRight());
            topPadding = snapPositionY(m.getTop());
            bottomPadding = snapPositionY(m.getBottom());
        }
    }

    public double leftPadding() {
        return leftPadding;
    }

    /** Location of the top left corner. */
    public final ReadOnlyProperty<Origin> originProperty() {
        return origin.getReadOnlyProperty();
    }

    public final Origin getOrigin() {
        return origin.get();
    }

    private void setOrigin(Origin p) {
        if (p == null) {
            throw new NullPointerException();
        }
        // prevent scrolling
        if (control.isUseContentHeight()) {
            p = new Origin(0, -topPadding);
        }
        origin.set(p);
    }

    private void handleOriginChange() {
        if (!inReflow) {
            requestLayout();
        }
    }

    public int topCellIndex() {
        return getOrigin().index();
    }

    public double getOffsetX() {
        return offsetX.get();
    }

    public void setOffsetX(double x) {
        // prevent scrolling
        if (control.isUseContentWidth()) {
            x = -leftPadding;
        }
        offsetX.set(x);
    }

    /** horizontal scroll offset */
    public DoubleProperty offsetXProperty() {
        return offsetX;
    }
    
    /** width of the area allocated for content (text cells) */
    public double getContentWidth() {
        return contentWidth.get();
    }
    
    public void setContentWidth(double w) {
        if (w < Params.LAYOUT_MIN_WIDTH) {
            w = Params.LAYOUT_MIN_WIDTH;
        }
        contentWidth.set(w);
    }

    public void setCaretVisible(boolean on) {
        caretVisible.set(on);
    }

    public boolean isCaretVisible() {
        return caretVisible.get();
    }

    /** reacts to width changes */
    protected void updateWidth() {
        if (control.isWrapText()) {
            double w = wrappedWidth();
            setContentWidth(w);
        } else {
            double w = getOffsetX() + flow.getWidth();
            double uw = arrangement.getUnwrappedWidth();
            if (uw > w) {
                w = uw;
            }

            if (w > getContentWidth()) {
                setContentWidth(w);
            }
            updateHorizontalScrollBar();
        }
    }
    
    public void handleSelectionChange() {
        updateCaretAndSelection();
        scrollCaretToVisible();
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
        if(anchor == null) {
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
        selectionHighlight.setTranslateX(leftPadding);

        // caret
        b = new FxPathBuilder();
        createCaretPath(b, caret);
        caretPath.getElements().setAll(b.getPathElements());
        caretPath.setTranslateX(leftPadding);
    }

    protected void removeCaretAndSelection() {
        caretLineHighlight.getElements().clear();
        selectionHighlight.getElements().clear();
        caretPath.getElements().clear();
    }

    protected void createCaretPath(FxPathBuilder b, TextPos p) {
        CaretInfo c = getCaretInfo(p);
        if(c != null) {
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
        double left = -leftPadding;
        double right = getContentWidth() + leftPadding + rightPadding;
        // TODO
        boolean topLTR = true;
        boolean bottomLTR = true;

        // FIX
        double lineSpacing = 0.0; // this is a problem!
        new SelectionHelper(b, left, right).generate(top, bottom, topLTR, bottomLTR, leftPadding, lineSpacing);
    }

    protected void createCurrentLineHighlight(FxPathBuilder b, TextPos caret) {
        int ix = caret.index();
        TextCell cell = arrangement().getVisibleCell(ix);
        if(cell != null) {
            double w;
            if(control.isWrapText()) {
                w = getWidth();
            } else {
                w = getContentWidth();
            }
            cell.addBoxOutline(b, 0.0, snapPositionX(w), cell.getCellHeight());
        }
    }

    /** uses vflow.content cooridinates */ 
    public TextPos getTextPosLocal(double localX, double localY) {
        // convert to cell coordinates
        double x = localX + getOffsetX();
        return arrangement().getTextPos(x, localY);
    }

    /** in vflow.flow coordinates */
    // TODO vflow.flow? or content?
    protected CaretInfo getCaretInfo(TextPos p) {
        return arrangement().getCaretInfo(flow, getOffsetX() + leftPadding, p);
    }

    /** returns caret sizing info using vflow.content coordinates, or null */
    public CaretInfo getCaretInfo() {
        TextPos p = control.getCaretPosition();
        if (p == null) {
            return null; // TODO check
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

    /** returns the shape if both ends are at the same line */
    protected PathElement[] getRangeShape(int line, int startOffset, int endOffset) {
        TextCell cell = arrangement().getVisibleCell(line);
        if (cell == null) {
            return null;
        }

        if (endOffset < 0) {
            // FIX to the edge?? but beware of RTL
            endOffset = cell.getTextLength();
        }

        Insets m = contentPadding();
        double dx = (m == null) ? 0.0 : -m.getLeft();
        double dy = 0.0;

        PathElement[] pe;
        if (startOffset == endOffset) {
            // TODO handle split caret!
            pe = cell.getCaretShape(flow, startOffset, true, dx, dy);
        } else {
            pe = cell.getRangeShape(flow, startOffset, endOffset, dx, dy);
        }
        return pe;
    }

    public void setSuppressBlink(boolean on) {
        suppressBlink.set(on);
        
        if(!on) {
            updateRateRestartBlink();
        }
    }

    public void updateRateRestartBlink() {
        Duration t1 = control.getCaretBlinkPeriod();
        Duration t2 = t1.multiply(2.0);

        caretAnimation.stop();
        caretAnimation.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO, (ev) -> setCaretVisible(true)),
            new KeyFrame(t1, (ev) -> setCaretVisible(false)),
            new KeyFrame(t2)
        );
        caretAnimation.play();
    }
    
    public int getParagraphCount() {
        return control.getParagraphCount();
    }

    /**
     * Returns control's content padding.
     * @return the content padding, can be null
     */
    public Insets contentPadding() {
        return control.getContentPadding();
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
            double h = getHeight();
            val = toScrollBarValue((topCellIndex() - ar.topCount()) * av + ar.topHeight(), h, max);
            visible = h / max;
        }

        handleScrollEvents = false;

        vscroll.setMin(0.0);
        vscroll.setMax(1.0);
        vscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
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

            double max = vscroll.getMax();
            double val = vscroll.getValue();
            double visible = vscroll.getVisibleAmount();
            double pos = fromScrollBarValue(val, visible, max); // max is 1.0

            Origin p = arrangement().fromAbsolutePosition(pos);
            setOrigin(p);
        }
    }

    /** updates HSB in response to change in width, layout, or offsetX */ 
    protected void updateHorizontalScrollBar() {
        boolean wrap = control.isWrapText();
        if (wrap) {
            return;
        }

        double max = getContentWidth() + leftPadding + rightPadding;
        double w = flow.getWidth();
        double off = getOffsetX();
        double vis = w / max;
        double val = toScrollBarValue(off, w, max);

        handleScrollEvents = false;

        hscroll.setMin(0.0);
        hscroll.setMax(1.0);
        hscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        hscroll.setVisibleAmount(vis);
        hscroll.setValue(val);

        handleScrollEvents = true;
    }

    /** handles user moving the scroll bar */
    public void handleHorizontalScroll() {
        if (handleScrollEvents) {
            if (arrangement == null) {
                return;
            } else if (control.isWrapText()) {
                return;
            }
            
            double max = getContentWidth() + leftPadding + rightPadding;
            double visible = flow.getWidth();
            double val = hscroll.getValue();
            double off = fromScrollBarValue(val, visible, max) - leftPadding;

            setOffsetX(snapPositionX(off));
            // no need to recompute the flow
            placeCells();
            updateCaretAndSelection();
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
        TextCell cell = cellCache.get(modelIndex);
        if (cell == null) {
            RichParagraph rp = control.getModel().getParagraph(modelIndex);
            cell = createTextCell(modelIndex, rp);
            cellCache.add(cell.getIndex(), cell);
        }
        return cell;
    }

    private TextCell createTextCell(int index, RichParagraph par) {
        // merge paragraph attributes
        StyleAttrs defaultAttrs = control.getDefaultParagraphAttributes();
        StyleAttrs pa = par.getParagraphAttributes();
        if ((defaultAttrs != null) && (!defaultAttrs.isEmpty())) {
            if (pa == null) {
                pa = defaultAttrs;
            } else {
                pa = defaultAttrs.builder().merge(pa).build();
            }
        }

        TextCell cell;
        Supplier<Region> gen = par.getParagraphRegion();
        if (gen != null) {
            // it's a paragraph node
            Region content = gen.get();
            cell = new TextCell(index, content);
        } else {
            // it's a regular text cell
            cell = new TextCell(index);

            // first line indent
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
                // as it makes the caret collapse to a single point
                cell.add(new Text(""));
            } else {
                for (StyledSegment seg : segments) {
                    switch (seg.getType()) {
                    case INLINE_NODE:
                        Node n = seg.getInlineNodeGenerator().get();
                        cell.add(n);
                        break;
                    case TEXT:
                        Text t = createTextNode(seg);
                        cell.add(t);
                        break;
                    }
                }
            }
        }

        if (pa != null) {
            // - need to resolve paragraph attributes only
            // - Resolver needs to separate character/paragraph attributes
            // - StyleAttrs.createStyleString() might need a boolean
            applyStyles(cell.getContent(), pa, true);
        }

        // finally adding paragraph attributes that affect TextCell
        if (pa != null) {
            String bullet = pa.get(StyleAttrs.BULLET);
            if (bullet != null) {
                cell.setBullet(bullet);
            }
        }

        // apply attributes to the TextCell (outer container)
        if (pa != null) {
            String style = StyleUtil.generateTextCellStyle(pa);
            cell.setStyle(style);
        }
        return cell;
    }

    private Text createTextNode(StyledSegment seg) {
        String text = seg.getText();
        Text t = new Text(text);
        StyleAttrs a = seg.getStyleAttrs(this);
        if (a != null) {
            applyStyles(t, a, false);
        }
        return t;
    }

    private void applyStyles(Node n, StyleAttrs a, boolean forParagraph) {
        boolean unwrapped = !control.isWrapText();
        CssStyles css = a.getCssStyles();
        if (css == null) {
            String style = StyleUtil.getStyleString(a, forParagraph, unwrapped);
            n.setStyle(style);
        } else {
            n.setStyle(css.style());
            String[] names = css.names();
            if (names != null) {
                n.getStyleClass().addAll(names);
            }
        }

        // FIX perhaps it should always force orientation
        if (forParagraph) {
            if (!unwrapped) {
                if (a.getBoolean(StyleAttrs.RIGHT_TO_LEFT)) {
                    n.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                }
            }
        }
    }

    private double computeSideWidth(SideDecorator d) {
        if (d != null) {
            double w = d.getPrefWidth(getWidth());
            if (w <= 0.0) {
                int top = topCellIndex();
                Node n = d.getNode(top, true);
                n.setManaged(false);

                flow.getChildren().add(n);
                try {
                    n.applyCss();
                    if (n instanceof Parent p) {
                        p.layout();
                    }
                    w = n.prefWidth(-1);
                } finally {
                    flow.getChildren().remove(n);
                }
            }
            // introducing some granularity in order to avoid left boundary moving back and forth when scrolling
            double granularity = 15;
            w = (Math.round((w + 1.0) / granularity) + 1.0) * granularity;
            return snapSizeX(w);
        }
        return 0.0;
    }

    @Override
    protected void layoutChildren() {
        reflow();
    }

    protected void reflow() {
//        System.out.println("reflow"); // FIX
        inReflow = true;
        try {
            // remove old nodes, if any
            if (arrangement != null) {
                arrangement.removeNodesFrom(flow);
                arrangement = null;
            }
    
            arrangement = new CellArrangement(this);
            layoutCells();
    
            checkForExcessiveWhitespaceAtTheEnd();
            updateCaretAndSelection();
    
            // eliminate VSB during scrolling with a mouse
            // the VSB will finally get updated on mouse released event
            if (!vsbPressed) {
                updateVerticalScrollBar();
            }
        } finally {
            dirty = false;
            inReflow = false;
        }
    }

    /** returns a non-null layout, laying out cells if necessary */
    protected CellArrangement arrangement() {
        if (!inReflow && dirty || (arrangement == null)) {
            reflow();
        }
        return arrangement;
    }

    /** recomputes sliding window */
    protected void layoutCells() {
        if (control.getModel() == null) {
            leftGutter.setVisible(false);
            rightGutter.setVisible(false);
            return;
        }

        // sides
        SideDecorator leftDecorator = control.getLeftDecorator();
        SideDecorator rightDecorator = control.getRightDecorator();
        leftSide = computeSideWidth(leftDecorator);
        rightSide = computeSideWidth(rightDecorator);

        int paragraphCount = getParagraphCount();
        boolean useContentHeight = control.isUseContentHeight();
        boolean useContentWidth = control.isUseContentWidth();
        boolean wrap = control.isWrapText() && !useContentWidth;
        double forWidth;
        double maxWidth;
        if (wrap) {
            forWidth = wrappedWidth();
            maxWidth = forWidth;
        } else {
            forWidth = -1.0;
            maxWidth = Params.MAX_WIDTH_FOR_LAYOUT;
        }

        double width = getWidth();
        double height = useContentHeight ? 0.0 : getHeight();

        double ytop = snapPositionY(-getOrigin().offset());
        double y = ytop;
        double unwrappedWidth = 0;
        double margin = Params.SLIDING_WINDOW_EXTENT * height;
        int topMarginCount = 0;
        int bottomMarginCount = 0;
        int count = 0;
        boolean visible = true;
        // TODO if topCount < marginCount, increase bottomCount correspondingly
        // also, update Origin if layout hit the beginning/end of the document
        
        // populating visible part of the sliding window + bottom margin
        int i = topCellIndex();
        for ( ; i < paragraphCount; i++) {
            TextCell cell = getCell(i);
            // TODO skip computation if layout width is the same
            Region r = cell.getContent();
            flow.getChildren().add(cell);
            cell.setMaxWidth(maxWidth);
            cell.setMaxHeight(USE_COMPUTED_SIZE);

            cell.applyCss();
            cell.layout();

            arrangement.addCell(cell);

            double h = cell.prefHeight(forWidth) + getLineSpacing(r);
            h = snapSizeY(h); // is this right?  or snap(y + h) - snap(y) ?
            cell.setPosition(y, h/*, forWidth*/);

            if (!wrap) {
                if (visible) {
                    double w = cell.prefWidth(-1);
                    if (unwrappedWidth < w) {
                        unwrappedWidth = w;
                    }
                }
            }

            y = snapPositionY(y + h);
            count++;

            if(useContentHeight) {
                height = y;
            } else {
                // stop populating the bottom part of the sliding window
                // when exceeded both pixel and line count margins
                if (visible) {
                    if (y > height) {
                        topMarginCount = (int)Math.ceil(count * Params.SLIDING_WINDOW_EXTENT);
                        bottomMarginCount = count + topMarginCount;
                        arrangement.setVisibleCellCount(count);
                        visible = false;
                    }
                } else {
                    // remove invisible cell from layout after sizing
                    flow.getChildren().remove(cell);
    
                    if ((y > (height + margin)) && (count > bottomMarginCount)) {
                        break;
                    }
                }
            }
        }

        // in case there are less paragraphs than can fit in the view
        if (visible) {
            arrangement.setVisibleCellCount(count);
        }
        
        if (i == paragraphCount) {
            y += bottomPadding;
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
                    n = leftDecorator.getNode(ix, false);
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
                    n = rightDecorator.getNode(cell.getIndex(), false);
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

        arrangement.setBottomCount(count);
        arrangement.setBottomHeight(y);
        arrangement.setUnwrappedWidth(snapSizeX(unwrappedWidth));
        count = 0;
        y = ytop;

        // populate top margin, going backwards from topCellIndex
        // TODO populate more, if bottom ended prematurely
        for (i = topCellIndex() - 1; i >= 0; i--) {
            TextCell cell = getCell(i);
            // TODO maybe skip computation if layout width is the same
            Region r = cell.getContent();
            flow.getChildren().add(cell);
            cell.setMaxWidth(maxWidth);
            cell.setMaxHeight(USE_COMPUTED_SIZE);

            cell.applyCss();
            cell.layout();

            arrangement.addCell(cell);

            double h = cell.prefHeight(forWidth) + getLineSpacing(r);
            h = snapSizeY(h); // is this right?  or snap(y + h) - snap(y) ?
            y = snapPositionY(y - h);
            count++;

            cell.setPosition(y, h/*, forWidth*/);

            flow.getChildren().remove(cell);

            // stop populating the top part of the sliding window
            // when exceeded both pixel and line count margins
            if ((-y > margin) && (count > topMarginCount)) {
                break;
            }
        }

        arrangement.setTopHeight(-y);

        if (useContentWidth) {
            width = unwrappedWidth + leftSide + rightSide + leftPadding + rightPadding;
        }

        // lay out gutters
        if (leftDecorator == null) {
            leftGutter.setVisible(false); // TODO perhaps use bindings, and rely on .isVisible() here?
        } else {
            leftGutter.setVisible(true);
            layoutInArea(leftGutter, 0.0, 0.0, leftSide, height, 0.0, HPos.CENTER, VPos.CENTER);
        }

        if (rightDecorator == null) {
            rightGutter.setVisible(false);
        } else {
            rightGutter.setVisible(true);
            layoutInArea(rightGutter, width - rightSide, 0.0, rightSide, height, 0.0, HPos.CENTER, VPos.CENTER);
        }

        layoutInArea(content, leftSide, 0.0, width - leftSide - rightSide, height, 0.0, HPos.CENTER, VPos.CENTER);

        if (wrap) {
            double w = wrappedWidth();
            setContentWidth(w);
        } else {
            if ((unwrappedWidth == 0.0) || (getContentWidth() < unwrappedWidth)) {
                setContentWidth(Math.max(unwrappedWidth, width));
            }
        }

        // TODO perhaps move the code here
        if (useContentWidth) {
            updatePrefWidth();
        }

        if (useContentHeight) {
            updatePrefHeight();
        }

        placeCells();
    }

    protected void placeCells() {
        boolean wrap = control.isWrapText() && !control.isUseContentWidth();
        double w = wrap ? getContentWidth() : Params.MAX_WIDTH_FOR_LAYOUT;
        double x = snapPositionX(-getOffsetX());

        leftGutter.getChildren().clear();
        rightGutter.getChildren().clear();

        boolean addLeft = control.getLeftDecorator() != null;
        boolean addRight = control.getRightDecorator() != null;

        int sz = arrangement.getVisibleCellCount();
        for (int i = 0; i < sz; i++) {
            TextCell cell = arrangement.getCellAt(i);
            double h = cell.getCellHeight();
            double y = cell.getY();
            flow.layoutInArea(cell, x, y, w, h);

            // this step is needed to get the correct caret path afterwards
            cell.layout();

            // place side nodes
            if (addLeft) {
                Node n = arrangement.getLeftNodeAt(i);
                if (n != null) {
                    leftGutter.getChildren().add(n);
                    leftGutter.layoutInArea(n, 0.0, y, leftGutter.getWidth(), h);
                }
            }

            if (addRight) {
                Node n = arrangement.getRightNodeAt(i);
                if (n != null) {
                    rightGutter.getChildren().add(n);
                    rightGutter.layoutInArea(n, 0.0, y, rightGutter.getWidth(), h);
                }
            }
        }
    }

    private double getLineSpacing(Region r) {
        if(r instanceof TextFlow f) {
            return f.getLineSpacing();
        }
        return 0.0;
    }

    public double getViewHeight() {
        return flow.getHeight();
    }
    
    public void pageUp() {
        blockScroll(-getViewHeight());
    }
    
    public void pageDown() {
        blockScroll(getViewHeight());
    }
    
    public void scroll(double fractionOfHeight) {
        blockScroll(getViewHeight() * fractionOfHeight);
    }

    /** scroll by a number of pixels, delta must not exceed the view height in absolute terms */
    public void blockScroll(double delta) {
        blockScroll(delta, false);
    }

    /** scroll by a number of pixels, delta must not exceed the view height in absolute terms */
    public void blockScroll(double delta, boolean forceLayout) {
        Origin or = arrangement().computeOrigin(delta);
        if (or != null) {
            setOrigin(or);
            if (forceLayout) {
                layoutChildren();
            }
        }
    }

    public void hscroll(double delta) {
        double cw = flow.getWidth();
        double off = getOffsetX() + delta * cw;
        if (off < 0.0) {
            off = 0.0;
        } else if (off + cw > (getContentWidth() + leftPadding)) {
            off = Math.max(0.0, getContentWidth() + leftPadding - cw);
        }
        setOffsetX(off - leftPadding);
        // no need to recompute the flow
        placeCells();
        updateCaretAndSelection();
    }

    /** scrolls to visible area, using vflow.content coordinates */
    public void scrollToVisible(double x, double y) {
        if (y < 0.0) {
            // above viewport
            blockScroll(y);
        } else if (y >= getViewHeight()) {
            // below viewport
            blockScroll(y - getViewHeight());
        }
        
        scrollHorizontalToVisible(x);
    }

    public void scrollCaretToVisible() {
        CaretInfo c = getCaretInfo();
        if (c == null) {
            // caret is outside of the layout; let's set the origin first to the caret position
            // and then block scroll to avoid scrolling past the document end, if needed
            TextPos p = control.getCaretPosition();
            if (p != null) {
                int ix = p.index();
                Origin or = new Origin(ix, 0.0);
                setOrigin(or);
                checkForExcessiveWhitespaceAtTheEnd();
            }
        } else {
            // block scroll, if needed
            if (c.getMinY() < 0.0) {
                blockScroll(c.getMinY());
            } else if (c.getMaxY() > getViewHeight()) {
                blockScroll(c.getMaxY() - getViewHeight());
            }
            
            if (!control.isWrapText()) {
                // FIX primary caret
                double x = c.getMinX();
                if (x + leftPadding < 0.0) {
                    scrollHorizontalToVisible(x);
                } else {
                    scrollHorizontalToVisible(c.getMaxX());
                }
            }
        }
    }

    /** x - vflow.content coordinate */
    private void scrollHorizontalToVisible(double x) {
        if (!control.isWrapText()) {
            x += leftPadding;
            double cw = flow.getWidth();
            double off;
            if (x < 0.0) {
                off = Math.max(getOffsetX() + x - Params.HORIZONTAL_GUARD, 0.0);
            } else if (x > cw) {
                off = getOffsetX() + x - cw + Params.HORIZONTAL_GUARD;
            } else {
                return;
            }

            setOffsetX(off);
            placeCells();
            updateCaretAndSelection();
        }
    }

    protected void checkForExcessiveWhitespaceAtTheEnd() {
        double delta = arrangement().bottomHeight() - getViewHeight();
        if (delta < 0) {
            if (getOrigin().index() == 0) {
                if (getOrigin().offset() <= -topPadding) {
                    return;
                }
            }
            blockScroll(delta);
        }
    }

//    public void updateTabSize() {
//        //CaretInfo c = getCaretInfo();
//        requestLayout();
//        // TODO remember caret line position, do layout pass, block move to preserve the caret y position
//        // as it might shift (only if wrapping is enabled)
//        // also if wrap is off, might need a horizontal block scroll to keep caret in the same x position
//    }

    // TODO this could be more advanced to reduce the amount of re-computation and re-flow
    public void handleTextUpdated(TextPos start, TextPos end, int addedTop, int linesAdded, int addedBottom) {
        // change origin if start position is before the top line
        Origin origin = getOrigin();
        if (start.index() < origin.index()) {
            origin = new Origin(start.index(), 0.0);
            setOrigin(origin);
        }

        // TODO clear cache >= start, update layout
        cellCache.clear();
        // TODO rebuild from start.lineIndex()
        requestLayout();
    }

    // TODO this implementation might be more advanced to reduce the amount of re-computation and re-flow
    public void handleStyleUpdated(TextPos start, TextPos end) {
        // TODO clear cache >= start, update layout
        cellCache.clear();
        // TODO rebuild from start.lineIndex()
        requestLayout();
    }

    @Override
    public StyleAttrs resolveStyles(StyleAttrs attrs) {
        if (attrs == null) {
            return attrs;
        }
        CssStyles css = attrs.getCssStyles();
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
            return StyleAttrs.from(measurer);
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
                double w = getContentWidth();
                double h = r.prefHeight(w);
                layoutInArea(r, 0, -h, w, h, 0, HPos.CENTER, VPos.CENTER);
            }
            return n.snapshot(null, null);
        } finally {
            getChildren().remove(n);
        }
    }

    public void handleUseContentHeight() {
        boolean on = control.isUseContentHeight();
        if (on) {
            setContentWidth(0.0);
            setOrigin(new Origin(0, -topPadding));
            setOffsetX(-leftPadding);
        }
        requestControlLayout(false);
    }

    public void handleUseContentWidth() {
        boolean on = control.isUseContentWidth();
        if (on) {
            setContentWidth(0.0);
            setOrigin(new Origin(0, -topPadding));
            setOffsetX(-leftPadding);
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

    // TODO move to caller?
    private void updatePrefWidth() {
        if (!control.prefWidthProperty().isBound()) {
            double w = getFlowWidth();
            if (w >= 0.0) {
                if (vscroll.isVisible()) {
                    w += vscroll.getWidth();
                }
            }

            //D.p("w=", w); // FIX

            if (mainPane().getPrefWidth() != w) {
                //setPrefWidth(w);
                mainPane().setPrefWidth(w);
                //control.setPrefWidth(w);

                //D.p("control.getParent().requestLayout();");
                control.getParent().requestLayout();
                requestControlLayout(false);
            }
        }
    }

    // TODO move to the caller?
    private void updatePrefHeight() {
        if (!control.prefHeightProperty().isBound()) {
            double h = getFlowHeight();
            if (h >= 0.0) {
                if (hscroll.isVisible()) {
                    h += hscroll.getHeight();
                }
            }

            //D.p("h=", h); // FIX

            if (mainPane().getPrefHeight() != h) {
                mainPane().setPrefHeight(h);

                //D.p("control.getParent().requestLayout();");
                control.getParent().requestLayout();
                requestControlLayout(false);
            }
        }
    }
    
    private Region mainPane() {
        return (Region)getParent();
    }

    public double getFlowHeight() {
        return
            snapSizeY(Math.max(Params.LAYOUT_MIN_HEIGHT, arrangement().bottomHeight())) +
            snapSizeY(Params.LAYOUT_FOCUS_BORDER) * 2;
    }

    public double getFlowWidth() {
        return
            arrangement().getUnwrappedWidth() +
            snapSizeX(leftSide) +
            snapSizeX(rightSide) +
            leftPadding +
            rightPadding +
            snapSizeX(Params.LAYOUT_CARET_ALLOWANCE) +
            snapSizeX(Params.LAYOUT_FOCUS_BORDER) * 2;
    }
}
