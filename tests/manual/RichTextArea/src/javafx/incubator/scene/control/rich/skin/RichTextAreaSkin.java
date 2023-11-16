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

package javafx.incubator.scene.control.rich.skin;

import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.incubator.scene.control.rich.ConfigurationParameters;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.DataFormat;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import com.sun.javafx.scene.control.rich.Params;
import com.sun.javafx.scene.control.rich.RichTextAreaBehavior;
import com.sun.javafx.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.javafx.scene.control.rich.RichUtils;
import com.sun.javafx.scene.control.rich.VFlow;
import com.sun.javafx.scene.control.rich.util.ListenerHelper;

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
    private final ConfigurationParameters config;
    private final ListenerHelper listenerHelper;
    private final RichTextAreaBehavior behavior;
    private final Pane mainPane;
    private final VFlow vflow;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;
    private final StyledTextModel.ChangeListener modelChangeListener;

    static {
        RichTextAreaSkinHelper.setAccessor(new RichTextAreaSkinHelper.Accessor() {
            @Override
            public VFlow getVFlow(Skin<?> skin) {
                if (skin instanceof RichTextAreaSkin s) {
                    return s.getVFlow();
                }
                return null;
            }
        });
    }

    /**
     * Constructs the skin.
     * @param control the owner
     * @param cnf the configuration parameters
     */
    public RichTextAreaSkin(RichTextArea control, ConfigurationParameters cnf) {
        super(control);
        
        this.config = cnf;
        this.listenerHelper = new ListenerHelper();

        modelChangeListener = new StyledTextModel.ChangeListener() {
            @Override
            public void eventTextUpdated(TextPos start, TextPos end, int top, int ins, int btm) {
                handleTextUpdated(start, end, top, ins, btm);
                // TODO do we need to reflow if useContentXX is on?
            }

            @Override
            public void eventStyleUpdated(TextPos start, TextPos end) {
                handleStyleUpdated(start, end);
                // TODO do we need to reflow if useContentXX is on?
            }
        };
        
        vscroll = createVScrollBar();
        vscroll.setOrientation(Orientation.VERTICAL);
        vscroll.setMin(0.0);
        vscroll.setMax(1.0);
        vscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        vscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());
        vscroll.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
                return !control.isUseContentHeight();
            },
            control.useContentHeightProperty()
        ));

        hscroll = createHScrollBar();
        hscroll.setOrientation(Orientation.HORIZONTAL);
        hscroll.setMin(0.0);
        hscroll.setMax(1.0);
        hscroll.setUnitIncrement(Params.SCROLL_BARS_UNIT_INCREMENT);
        hscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());
        hscroll.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
                return !control.isWrapText() && !control.isUseContentWidth();
            },
            control.wrapTextProperty(),
            control.useContentWidthProperty()
        ));

        vflow = new VFlow(this, config, vscroll, hscroll);

        // TODO corner? only when both scroll bars are visible

        mainPane = new Pane(vflow, vscroll, hscroll) {
            @Override
            protected void layoutChildren() {
                double x0 = snappedLeftInset();
                double y0 = snappedTopInset();

                double vscrollWidth;
                if (vscroll.isVisible()) {
                    vscrollWidth = vscroll.prefWidth(-1);
                } else {
                    vscrollWidth = 0.0;
                }

                double hscrollHeight;
                if (hscroll.isVisible()) {
                    hscrollHeight = hscroll.prefHeight(-1);
                } else {
                    hscrollHeight = 0.0;
                }
                
                double w;
                if (control.isUseContentWidth()) {
                    w = vflow.getFlowWidth();
                } else {
                    w = snapSizeX(getWidth() - x0 - snappedRightInset() - snapSizeX(vscrollWidth) - snapSizeX(Params.LAYOUT_FOCUS_BORDER));
                }

                double h;
                if (control.isUseContentHeight()) {
                    h = vflow.getFlowHeight();
                } else {
                    h = snapSizeY(getHeight() - y0 - snappedBottomInset() - snapSizeY(hscrollHeight) - snapSizeY(Params.LAYOUT_FOCUS_BORDER));
                }

                //D.f("w=%.1f h=%.1f pref=%.1f", w, h, prefHeight(-1)); // FIX

                layoutInArea(vscroll, w, y0 + 1.0, vscrollWidth, h, -1, null, true, true, HPos.RIGHT, VPos.TOP);
                layoutInArea(hscroll, x0 + 1, h, w, hscrollHeight, -1, null, true, true, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(vflow, x0, y0, w, h, -1, null, true, true, HPos.LEFT, VPos.TOP);
            }
        };
        getChildren().add(mainPane);
        mainPane.setStyle("-fx-font-family: 'Iosevka Fixed SS16'; -fx-font-size: 9;");

        behavior = new RichTextAreaBehavior(control);

        listenerHelper.addInvalidationListener(vflow::handleSelectionChange, control.selectionSegmentProperty());
        listenerHelper.addInvalidationListener(vflow::updateRateRestartBlink, true, control.caretBlinkPeriodProperty());
//        listenerHelper.addInvalidationListener(vflow::updateTabSize, control.tabSizeProperty());
        listenerHelper.addInvalidationListener(vflow::updateCaretAndSelection, control.highlightCurrentParagraphProperty());
        listenerHelper.addInvalidationListener(vflow::handleContentPadding, true, control.contentPaddingProperty());
        listenerHelper.addInvalidationListener(vflow::handleDefaultParagraphAttributes, true, control.defaultParagraphAttributesProperty());
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
        listenerHelper.addInvalidationListener(this::handleFontChange, true, getSkinnable().fontProperty());
        listenerHelper.addChangeListener(control.modelProperty(), true, this::handleModelChange);
    }

    @Override
    public void install() {
        behavior.install();
    }

    @Override
    public void dispose() {
        if (getSkinnable() != null) {
            listenerHelper.disconnect();
            behavior.dispose();
            vflow.dispose();
            super.dispose();
        }
    }

    public void handleModelChange(Object src, StyledTextModel old, StyledTextModel m) {
        if (old != null) {
            old.removeChangeListener(modelChangeListener);
        }

        if (m != null) {
            m.addChangeListener(modelChangeListener);
        }
    }

    protected void handleTextUpdated(TextPos start, TextPos end, int addedTop, int linesAdded, int addedBottom) {
        vflow.handleTextUpdated(start, end, addedTop, linesAdded, addedBottom);
    }
    
    protected void handleStyleUpdated(TextPos start, TextPos end) {
        vflow.handleStyleUpdated(start, end);
    }

    private final ScrollBar createVScrollBar() {
        Supplier<ScrollBar> gen = config.scrollBarGeneratorVertical;
        return gen == null ? new ScrollBar() : gen.get();
    }

    private final ScrollBar createHScrollBar() {
        Supplier<ScrollBar> gen = config.scrollBarGeneratorHorizontal;
        return gen == null ? new ScrollBar() : gen.get();
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

    private void handleFontChange() {
        // TODO use the default paragraph style
        Font f = getSkinnable().getFont();
        if (f != null) {
            String family = f.getFamily();
            double size = f.getSize();
            String name = f.getName().toLowerCase();
            // FIX once JDK-8092191 is in
            String style = RichUtils.guessFontStyle(name);
            String weight = RichUtils.guessFontWeight(name);
            String s =
                "-fx-font-family:'" + family +
                "'; -fx-font-size:" + size +
                "; -fx-font-style:" + style +
                "; -fx-font-weight:" + weight +
                ";";
            mainPane.setStyle(s);
            getSkinnable().requestLayout();
            vflow.requestControlLayout(true);
        }
    }

    // TODO is this needed?
    // the purpose of this design is unclear: why have this code here when it should be done by the container?
    // in fact, why duplicate children list in the skin in the first place?
//    @Override
//    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
//        if (getSkinnable().isUseContentHeight()) {
//            double hscrollHeight = 0.0;
//            if (hscroll.isVisible()) {
//                hscrollHeight = hscroll.prefHeight(-1);
//            }
//            //return vflow.prefHeight(-1) + hscrollHeight;
//            return vflow.getFlowHeight() + hscrollHeight;
//        }
//        return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
//    }
//
//    @Override
//    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
//        if (getSkinnable().isUseContentWidth()) {
//            double vscrollWidth = 0.0;
//            if (vscroll.isVisible()) {
//                vscrollWidth = vscroll.prefWidth(-1);
//            }
//            //return vflow.prefWidth(-1) + vscrollWidth;
//            return vflow.getFlowWidth() + vscrollWidth;
//        }
//        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
//    }

    /**
     * Copies the text in the specified format when selection exists and when the export in this format
     * is supported by the model, and the skin must be installed; otherwise, this method is a no-op.
     * @param format data format
     */
    public void copy(DataFormat format) {
        behavior.copy(format);
    }

    /**
     * Pastes the clipboard content at the caret, or, if selection exists, replacing the selected text.
     * The format must be supported by the model, and the skin must be installed,
     * otherwise this method has no effect.
     * @param format data format
     */
    public void paste(DataFormat format) {
        behavior.paste(format);
    }
}
