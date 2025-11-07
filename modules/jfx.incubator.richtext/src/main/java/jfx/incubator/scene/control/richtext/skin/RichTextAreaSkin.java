/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.skin;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.DataFormat;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import com.sun.jfx.incubator.scene.control.input.InputMapHelper;
import com.sun.jfx.incubator.scene.control.richtext.Params;
import com.sun.jfx.incubator.scene.control.richtext.RichTextAreaBehavior;
import com.sun.jfx.incubator.scene.control.richtext.RichTextAreaHelper;
import com.sun.jfx.incubator.scene.control.richtext.RichTextAreaSkinHelper;
import com.sun.jfx.incubator.scene.control.richtext.TextCell;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import com.sun.jfx.incubator.scene.control.richtext.util.ListenerHelper;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.StyleHandlerRegistry;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * Provides visual representation for RichTextArea.
 * <p>
 * This skin consists of a top level Pane that manages the following children:
 * <ul>
 * <li>virtual flow Pane
 * <li>horizontal scroll bar
 * <li>vertical scroll bar
 * </ul>
 *
 * @since 24
 */
public class RichTextAreaSkin extends SkinBase<RichTextArea> {
    private final ListenerHelper listenerHelper;
    private final RichTextAreaBehavior behavior;
    private final VFlow vflow;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;
    private final EventHandler<InputMethodEvent> inputMethodTextChangedHandler = this::handleInputMethodEvent;
    private InputMethodRequests inputMethodRequests;
    private Ime ime;

    static {
        RichTextAreaSkinHelper.setAccessor(new RichTextAreaSkinHelper.Accessor() {
            @Override
            public VFlow getVFlow(Skin<?> skin) {
                if (skin instanceof RichTextAreaSkin s) {
                    return s.getVFlow();
                }
                return null;
            }

            @Override
            public ListenerHelper getListenerHelper(Skin<?> skin) {
                return ((RichTextAreaSkin)skin).listenerHelper;
            }
        });
    }

    /**
     * Constructs the skin.
     * @param control the owner
     */
    public RichTextAreaSkin(RichTextArea control) {
        super(control);

        this.listenerHelper = new ListenerHelper();

        vscroll = createVScrollBar();
        vscroll.setOrientation(Orientation.VERTICAL);
        vscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());

        hscroll = createHScrollBar();
        hscroll.setOrientation(Orientation.HORIZONTAL);
        hscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());

        vflow = new VFlow(this, vscroll, hscroll);
        getChildren().add(vflow);

        behavior = new RichTextAreaBehavior(control);

        listenerHelper.addChangeListener(vflow::handleSelectionChange, control.selectionProperty());
        listenerHelper.addInvalidationListener(vflow::updateRateRestartBlink, true, control.caretBlinkPeriodProperty());
        listenerHelper.addInvalidationListener(vflow::updateCaretAndSelection, control.highlightCurrentParagraphProperty());
        listenerHelper.addInvalidationListener(vflow::handleContentPadding, true, control.contentPaddingProperty());
        listenerHelper.addInvalidationListener(vflow::handleDecoratorChange,
            control.leftDecoratorProperty(),
            control.rightDecoratorProperty()
        );
        listenerHelper.addInvalidationListener(vflow::handleUseContentHeight, true, control.useContentHeightProperty());
        listenerHelper.addInvalidationListener(vflow::handleUseContentWidth, true, control.useContentWidthProperty());
        listenerHelper.addInvalidationListener(vflow::handleVerticalScroll, vscroll.valueProperty());
        listenerHelper.addInvalidationListener(vflow::handleHorizontalScroll, hscroll.valueProperty());
        listenerHelper.addInvalidationListener(vflow::handleWrapText, control.wrapTextProperty());
        listenerHelper.addInvalidationListener(vflow::handleModelChange, control.modelProperty());
        listenerHelper.addChangeListener(control.modelProperty(), true, this::handleModelChange);
    }

    @Override
    public void install() {
        RichTextArea rta = getSkinnable();
        // TODO fix once SkinInputMap is public
        InputMapHelper.setSkinInputMap(rta.getInputMap(), behavior.getSkinInputMap());
        //rta.getInputMap().setSkinInputMap(behavior.getSkinInputMap());

        // IMPORTANT: both setOnInputMethodTextChanged() and setInputMethodRequests() are required for IME to work
        if (rta.getOnInputMethodTextChanged() == null) {
            rta.setOnInputMethodTextChanged(inputMethodTextChangedHandler);
        }

        if (rta.getInputMethodRequests() == null) {
            inputMethodRequests = new InputMethodRequests() {
                // returns the lower left corner of the character bounds
                // content is relative to the start of selection
                @Override
                public Point2D getTextLocation(int offset) {
                    SelectionSegment sel = rta.getSelection();
                    if (sel != null) {
                        TextPos p = sel.getMin();
                        p = RichUtils.advancePosition(p, offset);
                        return vflow.getImeLocationOnScreen(p);
                    }
                    return new Point2D(0, 0);
                }

                @Override
                public String getSelectedText() {
                    SelectionSegment sel = rta.getSelection();
                    if (sel != null) {
                        if (!sel.isCollapsed()) {
                            int limit = Params.IME_MAX_TEXT_LENGTH;
                            StringBuilder sb = new StringBuilder(limit);
                            RichTextAreaHelper.getText(rta, sel.getMin(), sel.getMax(), sb, limit);
                            return sb.toString();
                        }
                    }
                    return "";
                }

                @Override
                public int getLocationOffset(int x, int y) {
                    // TODO this is weird: the caller CInputMethod.characterIndexForPoint() talks about screen coordinates,
                    // but the implementation in TextArea treats it as local coordinates (and simply returns 0 in TextField)
                    // which makes no sense!
                    // BTW, could never hit this breakpoint.
                    //
                    // CInputMethod.characterIndexForPoint():
                    // Gets the offset within the composed text for the specified absolute x and y coordinates on the screen.
                    // This information is used, for example to handle mouse clicks and the mouse cursor.
                    // The offset is relative to the composed text, so offset 0 indicates the beginning of the composed text.
                    TextPos pos = vflow.getTextPosLocal(x, y);
                    return pos.offset() - ime.start.offset();
                }

                @Override
                public void cancelLatestCommittedText() {
                    // no-op as in TextInputControlSkin
                }
            };
            rta.setInputMethodRequests(inputMethodRequests);
        }
    }

    @Override
    public void dispose() {
        RichTextArea rta = getSkinnable();
        if (rta != null) {
            if (rta.getInputMethodRequests() == inputMethodRequests) {
                rta.setInputMethodRequests(null);
            }
            if (rta.getOnInputMethodTextChanged() == inputMethodTextChangedHandler) {
                rta.setOnInputMethodTextChanged(null);
            }

            listenerHelper.disconnect();
            vflow.dispose();
            getChildren().remove(vflow);

            super.dispose();
        }
    }

    private void handleInputMethodEvent(InputMethodEvent ev) {
        RichTextArea rta = getSkinnable();
        if (RichUtils.canEdit(rta) && !rta.isDisabled()) {
            SelectionSegment sel = rta.getSelection();
            if (sel == null) {
                return; // should not happen
            }

            // remove previous input method text (if any) or selected text
            TextPos rEnd = sel.getMax();
            if (ime != null) {
                if (ime.shapes != null) {
                    vflow.removeImHighlight(ime.shapes);
                    ime.shapes = null;
                }
                // imeStart is valid
                rEnd = RichUtils.advancePosition(ime.start, ime.length);
            } else {
                ime = new Ime();
                ime.start = sel.getMin();
            }

            String text = RichUtils.getImeText(ev);
            ime.length = text.length();

            // replace selection or previous ime text with composed or committed text
            rta.replaceText(ime.start, rEnd, text, false);

            // add ime shapes
            TextPos end = ime.start;
            if (ev.getComposed().size() > 0) {
                ime.shapes = new ArrayList<>();
                TextPos pos = ime.start;
                for (InputMethodTextRun run : ev.getComposed()) {
                    end = RichUtils.advancePosition(pos, run.getText().length());
                    appendImeShapes(ime.shapes, run.getHighlight(), pos, end);
                    pos = end;
                }
                vflow.addImeHighlights(ime.shapes, ime.start);
            } else {
                ime.shapes = null;
                end = RichUtils.advancePosition(ime.start, text.length());
            }
            rta.select(end);

            if ((ev.getCommitted().length() > 0) || (ime.length == 0)) {
                if (ime.shapes != null) {
                    vflow.removeImHighlight(ime.shapes);
                }
                ime = null;
            }
            ev.consume();
        }
    }

    private void appendImeShapes(List<Shape> shapes, InputMethodHighlight highlight, TextPos start, TextPos end) {
        double minX = 0.0;
        double maxX = 0.0;
        double minY = 0.0;
        double maxY = 0.0;

        List<PathElement> elements = vflow.getUnderlineShape(start, end);
        int sz = elements.size();
        for (int i = 0; i < sz; i++) {
            PathElement pe = elements.get(i);
            if (pe instanceof MoveTo em) {
                minX = maxX = em.getX();
                minY = maxY = em.getY();
            } else if (pe instanceof LineTo em) {
                minX = (minX < em.getX() ? minX : em.getX());
                maxX = (maxX > em.getX() ? maxX : em.getX());
                minY = (minY < em.getY() ? minY : em.getY());
                maxY = (maxY > em.getY() ? maxY : em.getY());
            } else if (pe instanceof HLineTo em) {
                minX = (minX < em.getX() ? minX : em.getX());
                maxX = (maxX > em.getX() ? maxX : em.getX());
            } else if (pe instanceof VLineTo em) {
                minY = (minY < em.getY() ? minY : em.getY());
                maxY = (maxY > em.getY() ? maxY : em.getY());
            }
            // don't assume that shapes are ended with ClosePath
            if (
                pe instanceof ClosePath ||
                i == sz - 1 ||
                (i < sz - 1 && elements.get(i + 1) instanceof MoveTo)
            )
            {
                // create the shape
                Shape sh = null;
                switch(highlight) {
                case SELECTED_RAW:
                    // blue background
                    sh = new Path(vflow.getRangeShape(start, end));
                    sh.setFill(imeSelectColor());
                    sh.setOpacity(0.3);
                    break;
                case UNSELECTED_RAW:
                    // dash underline
                    sh = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    sh.setStroke(imeColor());
                    sh.setStrokeWidth(maxY - minY);
                    ObservableList<Double> dashArray = sh.getStrokeDashArray();
                    dashArray.add(2.0);
                    dashArray.add(2.0);
                    break;
                case SELECTED_CONVERTED:
                    // thick underline
                    sh = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    sh.setStroke(imeColor());
                    sh.setStrokeWidth((maxY - minY) * 3);
                    break;
                case UNSELECTED_CONVERTED:
                    // single underline
                    sh = new Line(minX + 2, maxY + 1, maxX - 2, maxY + 1);
                    sh.setStroke(imeColor());
                    sh.setStrokeWidth(maxY - minY);
                    break;
                }

                if (sh != null) {
                    sh.setManaged(false);
                    shapes.add(sh);
                }
            }
        }
    }

    private Color imeColor() {
        return RichUtils.isDarkScheme(getSkinnable()) ? Color.WHITE : Color.BLACK;
    }

    private Color imeSelectColor() {
        // TODO might depend on the color scheme
        return Color.BLUE;
    }

    private void handleModelChange(Object src, StyledTextModel old, StyledTextModel m) {
        if (old != null) {
            old.removeListener(vflow);
        }

        if (m != null) {
            m.addListener(vflow);
        }
    }

    /**
     * Creates the vertical scroll bar.
     * <p>
     * The subclasses may override this method to provide custom ScrollBar implementation.
     *
     * @return the vertical scroll bar
     */
    protected ScrollBar createVScrollBar() {
        return new ScrollBar();
    }

    /**
     * Creates the horizontal scroll bar.
     * <p>
     * The subclasses may override this method to provide custom ScrollBar implementation.
     *
     * @return the horizontal scroll bar
     */
    protected ScrollBar createHScrollBar() {
        return new ScrollBar();
    }

    private VFlow getVFlow() {
        return vflow;
    }

    /**
     * Returns the skin's {@link StyleResolver}.
     * @return style resolver instance
     */
    public StyleResolver getStyleResolver() {
        return vflow;
    }

    /**
     * Copies the text in the specified format when selection exists and when the export in this format
     * is supported by the model, and the skin must be installed; otherwise, this method is a no-op.
     *
     * @param format data format
     */
    public void copyText(DataFormat format) {
        behavior.copyWithFormat(format);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The format must be supported by the model, and the skin must be installed,
     * otherwise this method has no effect.
     *
     * @param format data format
     */
    public void pasteText(DataFormat format) {
        behavior.pasteWithFormat(format);
    }

    /**
     * Applies styles based on supplied attribute set to either the whole paragraph or the text segment.
     * This method can be overriden by other skin implementations to provide additional styling.
     * The overriding method must call super implementation.
     *
     * @param context the cell context
     * @param attrs the attributes
     * @param forParagraph determines whether the styles are applied to the paragraph (true), or text segment (false)
     */
    public void applyStyles(CellContext context, StyleAttributeMap attrs, boolean forParagraph) {
        if (attrs != null) {
            RichTextArea c = getSkinnable();
            StyleHandlerRegistry r = c.getStyleHandlerRegistry();
            for (StyleAttribute a : attrs.getAttributes()) {
                Object v = attrs.get(a);
                if (v != null) {
                    r.process(c, forParagraph, context, a, v);
                }
            }
        }
    }

    /**
     * Discards any cached layout information and calls
     * {@link javafx.scene.Parent#requestLayout() requestLayout()}.
     */
    // TODO alternative: simply override requestLayout() ?
    public void refreshLayout() {
        vflow.invalidateLayout();
        getSkinnable().requestLayout();
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (getSkinnable().isUseContentHeight()) {
            return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
        }
        return Params.PREF_HEIGHT;
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (getSkinnable().isUseContentWidth()) {
            return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
        }
        return Params.PREF_WIDTH;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Params.MIN_HEIGHT;
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Params.MIN_WIDTH;
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        switch(action) {
        case SHOW_TEXT_RANGE:
            {
                Integer start = (Integer)parameters[0];
                Integer end = (Integer)parameters[1];
                if (start != null && end != null) {
                    // TODO
//                    scrollCharacterToVisible(end);
//                    scrollCharacterToVisible(start);
//                    scrollCharacterToVisible(end);
                }
                break;
            }
        default:
            super.executeAccessibleAction(action, parameters);
        }
    }

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
        case BOUNDS_FOR_RANGE:
            {
                TextPos p = getSkinnable().getCaretPosition();
                if (p != null) {
                    int start = (Integer)parameters[0];
                    int end = (Integer)parameters[1];
                    PathElement[] elements = getVFlow().getRangeShape(p.index(), start, end + 1);
                    return RichUtils.pathToBoundsArray(getVFlow(), elements);
                }
                return null;
            }
        case FONT:
            {
                StyleAttributeMap a = getSkinnable().getActiveStyleAttributeMap();
                if (a != null) {
                    String family = a.getFontFamily();
                    if (family != null) {
                        Double size = a.getFontSize();
                        if (size != null) {
                            return Font.font(family, size);
                        }
                    }
                }
                return null;
            }
        case HORIZONTAL_SCROLLBAR:
            return hscroll;
        case LINE_FOR_OFFSET:
            {
                TextPos p = getSkinnable().getCaretPosition();
                if (p != null) {
                    TextCell cell = getVFlow().getCell(p.index());
                    if (cell != null) {
                        int offset = (Integer)parameters[0];
                        return cell.lineForOffset(offset);
                    }
                }
                return null;
            }
        case LINE_START:
            {
                TextPos p = getSkinnable().getCaretPosition();
                if (p != null) {
                    TextCell cell = getVFlow().getCell(p.index());
                    if (cell != null) {
                        int lineIndex = (Integer)parameters[0];
                        return cell.lineStart(lineIndex);
                    }
                }
                return null;
            }
        case LINE_END:
            {
                TextPos p = getSkinnable().getCaretPosition();
                if (p != null) {
                    TextCell cell = getVFlow().getCell(p.index());
                    if (cell != null) {
                        int lineIndex = (Integer)parameters[0];
                        return cell.lineEnd(lineIndex);
                    }
                }
                return null;
            }
        case OFFSET_AT_POINT:
            {
                Point2D screenPoint = (Point2D)parameters[0];
                TextPos p = getSkinnable().getTextPosition(screenPoint.getX(), screenPoint.getY());
                return p == null ? null : p.charIndex();
            }
        case VERTICAL_SCROLLBAR:
            return vscroll;
        default:
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }

    // while IME is active
    private static class Ime {
        public TextPos start;
        public int length;
        public List<Shape> shapes;
    }
}
