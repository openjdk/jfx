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
package javafx.scene.control.rich;

import java.util.function.Supplier;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SkinBase;
import javafx.scene.control.rich.RichTextArea.Cmd;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import com.sun.javafx.scene.control.ListenerHelper;

/**
 * Provides visual representation for RichTextArea.
 * <p>
 * This skin manages a number of components:
 * <ul>
 * <li>virtual flow Region
 * <li>horizontal scroll bar
 * <li>vertical scroll bar
 * </ul>
 */
public class RichTextAreaSkin extends SkinBase<RichTextArea> implements StyleResolver {
    private final Config config;
    private final ListenerHelper listenerHelper;
    private final RichTextAreaBehavior behavior;
    private final VFlow vflow;
    private final ScrollBar vscroll;
    private final ScrollBar hscroll;
    private static final Text measurer = makeMeasurer();

    protected RichTextAreaSkin(RichTextArea control, Config cnf) {
        super(control);
        
        this.config = cnf;
        this.listenerHelper = new ListenerHelper();
        
        vscroll = createVScrollBar();
        vscroll.setOrientation(Orientation.VERTICAL);
        vscroll.setManaged(true);
        vscroll.setMin(0.0);
        vscroll.setMax(1.0);
        vscroll.setUnitIncrement(config.scrollBarsUnitIncrement);
        vscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());
        
        hscroll = createHScrollBar();
        hscroll.setOrientation(Orientation.HORIZONTAL);
        hscroll.setManaged(true);
        hscroll.setMin(0.0);
        hscroll.setMax(1.0);
        hscroll.setUnitIncrement(config.scrollBarsUnitIncrement);
        hscroll.addEventFilter(ScrollEvent.ANY, (ev) -> ev.consume());
        hscroll.visibleProperty().bind(control.wrapTextProperty().not());

        vflow = new VFlow(this, config, listenerHelper, vscroll, hscroll);

        // TODO corner? only when both scroll bars are visible

        getChildren().addAll(new Pane(vflow, vscroll, hscroll) {
            protected void layoutChildren() {
                double x0 = snappedLeftInset();
                double y0 = snappedTopInset();
                // is the snapping right?
                double width = getWidth() - x0 - snappedRightInset();
                double height = getHeight() - y0 - snappedBottomInset();

                double vscrollWidth = 0.0;
                if (vscroll.isVisible()) {
                    vscrollWidth = vscroll.prefWidth(-1);
                }

                double hscrollHeight = 0.0;
                if (hscroll.isVisible()) {
                    hscrollHeight = hscroll.prefHeight(-1);
                }

                double w = snapSizeX(width - vscrollWidth - 1.0);
                double h = snapSizeY(height - hscrollHeight - 1.0);

                layoutInArea(vscroll, w, y0 + 1.0, vscrollWidth, h, -1, null, true, true, HPos.RIGHT, VPos.TOP);
                layoutInArea(hscroll, x0 + 1, h, w, hscrollHeight, -1, null, true, true, HPos.LEFT, VPos.BOTTOM);
                layoutInArea(vflow, x0, y0, w, h, -1, null, true, true, HPos.LEFT, VPos.TOP);
            }
        });

        behavior = createBehavior();

        // TODO should this block be moved to VFlow?
        listenerHelper.addChangeListener(vflow::handleSelectionChange, control.selectionSegmentProperty());
        listenerHelper.addChangeListener(vflow::updateRateRestartBlink, true, control.caretBlinkPeriodProperty());
        listenerHelper.addInvalidationListener(vflow::updateTabSize, control.tabSizeProperty());
        listenerHelper.addInvalidationListener(vflow::updateCaretAndSelection, control.highlightCurrentLineProperty());
        listenerHelper.addInvalidationListener(vflow::handleContentPadding, true, control.contentPaddingProperty());
        listenerHelper.addInvalidationListener(vflow::handleLineSpacing, control.lineSpacingProperty());
        listenerHelper.addInvalidationListener(vflow::handleDecoratorChange,
            control.leftDecoratorProperty(),
            control.rightDecoratorProperty()
        );
        listenerHelper.addInvalidationListener(vflow::handleVerticalScroll, vscroll.valueProperty());
        listenerHelper.addInvalidationListener(vflow::handleHorizontalScroll, hscroll.valueProperty());
    }

    /** called from the constructor.  override to provide custom behavior */
    // TODO variant: generator in Config, or add methods to manipulate behavior to control
    protected RichTextAreaBehavior createBehavior() {
        return new RichTextAreaBehavior(this, config);
    }
    
    private final ScrollBar createVScrollBar() {
        Supplier<ScrollBar> gen = config.verticalScrollBarGenerator;
        return gen == null ? new ScrollBar() : gen.get();
    }

    private final ScrollBar createHScrollBar() {
        Supplier<ScrollBar> gen = config.horizontalScrollBarGenerator;
        return gen == null ? new ScrollBar() : gen.get();
    }

    public VFlow getVFlow() {
        return vflow;
    }

    @Override
    public void install() {
        behavior.install(listenerHelper);
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
    
    public void execute(Cmd a) {
        Runnable f = behavior.getFunction(a);
        if(f != null) {
            f.run();
        }
    }
    
    private static Text makeMeasurer() {
        Text t = new Text("8");
        t.setManaged(false);
        return t;
    }

    @Override
    public StyleAttrs convert(String directStyle, String[] css) {
        StyleAttrs a = new StyleAttrs();
        vflow.getChildren().add(measurer);
        try {
            measurer.setStyle(directStyle);
            if (css == null) {
                measurer.getStyleClass().clear();
            } else {
                measurer.getStyleClass().setAll(css);
            }
            measurer.applyCss();
        } finally {
            vflow.getChildren().remove(measurer);
        }
        return StyleAttrs.from(measurer);
    }

    @Override
    public WritableImage snapshot(Node n) {
        return vflow.snapshot(n);
    }
}
