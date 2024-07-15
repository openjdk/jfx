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

package jfx.incubator.scene.control.rich.skin;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.DataFormat;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import com.sun.jfx.incubator.scene.control.rich.Params;
import com.sun.jfx.incubator.scene.control.rich.RichTextAreaBehavior;
import com.sun.jfx.incubator.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.jfx.incubator.scene.control.rich.TextCell;
import com.sun.jfx.incubator.scene.control.rich.VFlow;
import com.sun.jfx.incubator.scene.control.rich.util.ListenerHelper;
import com.sun.jfx.incubator.scene.control.rich.util.RichUtils;
import jfx.incubator.scene.control.rich.CellContext;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.StyleHandlerRegistry;
import jfx.incubator.scene.control.rich.StyleResolver;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.StyleAttribute;
import jfx.incubator.scene.control.rich.model.StyleAttrs;
import jfx.incubator.scene.control.rich.model.StyledTextModel;

/**
 * Provides visual representation for RichTextArea.
 * <p>
 * This skin consists of a top level Pane that manages the following children:
 * <ul>
 * <li>virtual flow Pane
 * <li>horizontal scroll bar
 * <li>vertical scroll bar
 * </ul>
 */
public class RichTextAreaSkin extends SkinBase<RichTextArea> {
    private final ListenerHelper listenerHelper;
    private final RichTextAreaBehavior behavior;
    private final Pane mainPane;
    private final VFlow vflow;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;

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
        vscroll.setMin(0.0);
        vscroll.setMax(1.0);
        vscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        vscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());

        hscroll = createHScrollBar();
        hscroll.setOrientation(Orientation.HORIZONTAL);
        hscroll.setMin(0.0);
        hscroll.setMax(1.0);
        hscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        hscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());

        vflow = new VFlow(this, vscroll, hscroll);

        mainPane = new Pane(vflow, vscroll, hscroll) {
            @Override
            protected double computePrefHeight(double width) {
                if (control.isUseContentHeight()) {
                    double h = vflow.prefHeight(width);
                    if (hscroll.isVisible()) {
                        h += hscroll.prefHeight(width);
                    }
                    Insets m = getInsets();
                    return h + m.getTop() + m.getBottom();
                }
                return super.computePrefHeight(width);
            }

            @Override
            protected double computePrefWidth(double height) {
                if (control.isUseContentWidth()) {
                    double w = vflow.prefWidth(height); // or use unwrapped width + left + right?
                    if (vscroll.isVisible()) {
                        w += vscroll.prefWidth(height);
                    }
                    w += (Params.LAYOUT_FOCUS_BORDER * 2);
                    Insets m = getInsets();
                    return w + m.getLeft() + m.getRight();
                }
                return super.computePrefWidth(height);
            }

            @Override
            protected void layoutChildren() {
                double x0 = snappedLeftInset() + snapSizeX(Params.LAYOUT_FOCUS_BORDER);
                double y0 = snappedTopInset() + snapSizeY(Params.LAYOUT_FOCUS_BORDER);
                double width = getWidth();
                double height = getHeight();

                double vsbWidth = vscroll.isVisible() ? vscroll.prefWidth(height) : 0.0;
                double hsbHeight = hscroll.isVisible() ? snapSizeY(hscroll.prefHeight(width)) : 0.0;

                double w;
                if (control.isUseContentWidth()) {
                    w = vflow.getFlowWidth();
                } else {
                    w = snapSizeX(width - x0 - snappedRightInset() - snapSizeX(vsbWidth) - snapSizeX(Params.LAYOUT_FOCUS_BORDER));
                }

                double h;
                if (control.isUseContentHeight()) {
                    h = vflow.prefHeight(width);
                } else {
                    h = height - y0 - snappedBottomInset() - snapSizeY(Params.LAYOUT_FOCUS_BORDER) - hsbHeight;
                }

                layoutInArea(vscroll, w, y0, vsbWidth, h + hsbHeight, -1, null, true, true, HPos.RIGHT, VPos.TOP);
                layoutInArea(hscroll, x0, y0 + h, w, hsbHeight, -1, null, true, true, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(vflow, x0, y0, w, h, -1, null, true, true, HPos.LEFT, VPos.TOP);
            }
        };
        mainPane.getStyleClass().add("main-pane");
        getChildren().add(mainPane);

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
        getSkinnable().getInputMap().setSkinInputMap(behavior.getSkinInputMap());
    }

    @Override
    public void dispose() {
        if (getSkinnable() != null) {
            listenerHelper.disconnect();
            vflow.dispose();
            super.dispose();
        }
    }

    private void handleModelChange(Object src, StyledTextModel old, StyledTextModel m) {
        if (old != null) {
            old.removeChangeListener(vflow);
        }

        if (m != null) {
            m.addChangeListener(vflow);
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
        behavior.copy(format);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The format must be supported by the model, and the skin must be installed,
     * otherwise this method has no effect.
     *
     * @param format data format
     */
    public void pasteText(DataFormat format) {
        behavior.paste(format);
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
    public void applyStyles(CellContext context, StyleAttrs attrs, boolean forParagraph) {
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
        }
        super.executeAccessibleAction(action, parameters);
    }

    @Override
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
        case BOUNDS_FOR_RANGE:
            // TODO mention in javadoc that returns screen coordinates!
            {
                TextPos p = getSkinnable().getCaretPosition();
                if (p == null) {
                    return null;
                }
                int start = (Integer)parameters[0];
                int end = (Integer)parameters[1];
                PathElement[] elements = getVFlow().getRangeShape(p.index(), start, end + 1);
                return RichUtils.pathToBoundsArray(getVFlow(), elements);
            }
            case FONT: 
            {
                StyleAttrs a = getSkinnable().getActiveStyleAttrs();
                if (a != null) {
                    String family = a.getFontFamily();
                    Double size = a.getFontSize();
                    if ((family != null) && (size != null)) {
                        return Font.font(family, size);
                    }
                }
                return null;
            }
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
        default:
            return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
