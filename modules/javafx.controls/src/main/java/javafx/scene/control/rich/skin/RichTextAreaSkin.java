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

package javafx.scene.control.rich.skin;

import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.rich.ConfigurationParameters;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.rich.D;
import com.sun.javafx.scene.control.rich.Params;
import com.sun.javafx.scene.control.rich.RichTextAreaBehavior;
import com.sun.javafx.scene.control.rich.RichTextAreaSkinHelper;
import com.sun.javafx.scene.control.rich.VFlow;

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

    public RichTextAreaSkin(RichTextArea control, ConfigurationParameters cnf) {
        super(control);
        
        this.config = cnf;
        this.listenerHelper = new ListenerHelper();
        
        vscroll = createVScrollBar();
        vscroll.setOrientation(Orientation.VERTICAL);
        //vscroll.setManaged(false);
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
        //hscroll.setManaged(false);
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
        //vflow.setManaged(false);

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
                
                boolean forceNormal = !true; // FIX

                double width;
                if (!forceNormal && control.isUseContentWidth()) {
//                    width = vflow.getFlowWidth() + vscrollWidth;
//                    width = vflow.prefWidth(-1) + vscrollWidth;
                    width = prefWidth(-1);
                } else {
                    width = getWidth() - x0 - snappedRightInset();
                }

                double height;
                if (!forceNormal && control.isUseContentHeight()) {
//                    height = vflow.getFlowHeight() + hscrollHeight;
//                    height = vflow.prefHeight(-1) + hscrollHeight;
                    height = prefHeight(-1);
                } else {
                    height = getHeight() - y0 - snappedBottomInset();
                }

                double w = snapSizeX(width - vscrollWidth - 1.0);
                double h = snapSizeY(height - hscrollHeight - 1.0);

                D.f("w=%.1f h=%.1f pref=%.1f", w, h, prefHeight(-1)); // FIX

                layoutInArea(vscroll, w, y0 + 1.0, vscrollWidth, h, -1, null, true, true, HPos.RIGHT, VPos.TOP);
                layoutInArea(hscroll, x0 + 1, h, w, hscrollHeight, -1, null, true, true, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(vflow, x0, y0, w, h, -1, null, true, true, HPos.LEFT, VPos.TOP);
            }
        };
        getChildren().add(mainPane);

        // TODO can use ConfigurationParameters generator to create custom behavior
        behavior = createBehavior();

        listenerHelper.addInvalidationListener(vflow::handleSelectionChange, control.selectionSegmentProperty());
        listenerHelper.addInvalidationListener(vflow::updateRateRestartBlink, true, control.caretBlinkPeriodProperty());
        listenerHelper.addInvalidationListener(vflow::updateTabSize, control.tabSizeProperty());
        listenerHelper.addInvalidationListener(vflow::updateCaretAndSelection, control.highlightCurrentLineProperty());
        listenerHelper.addInvalidationListener(vflow::handleContentPadding, true, control.contentPaddingProperty());
        listenerHelper.addInvalidationListener(vflow::handleLineSpacing, control.lineSpacingProperty());
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
    }

    /**
     * Called from the constructor.  Override to provide custom behavior (this requires public BehaviorBase).
     * TODO variant: generator in Config, or add methods to manipulate behavior to control
     */
    protected RichTextAreaBehavior createBehavior() {
        return new RichTextAreaBehavior(getSkinnable());
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

    @Override
    public void install() {
        behavior.install(this, listenerHelper);
    }

    @Override
    public void dispose() {
        if (getSkinnable() != null) {
            listenerHelper.disconnect();
            behavior.dispose(this);
            vflow.dispose();
    
            super.dispose();
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
}
