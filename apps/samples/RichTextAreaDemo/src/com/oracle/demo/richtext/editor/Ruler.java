/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.util.Subscription;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;

/**
 * Ruler: a TabStopPolicy visual editor.
 */
public class Ruler extends BorderPane {

    private static final double HEIGHT = 12;
    private static final double HALFWIDTH = 4;
    private static final double TOO_CLOSE = 2;

    private final RichTextArea editor;
    private final SimpleObjectProperty<TabStopPolicy> policy;
    private int seq;
    private List<Tick> ticks;
    private Tick clickedStop;
    private boolean dragging;
    private boolean modified;
    private boolean popup;
    private Pane tickPane;
    private SelectionSegment selection;
    private SimpleObjectProperty<Runnable> onChange;

    public Ruler(RichTextArea editor) {
        this.editor = editor;
        setPrefHeight(HEIGHT);
        setBackground(Background.fill(Color.gray(0.8)));
        getStyleClass().add("ruler");

        tickPane = new Pane();
        tickPane.setBackground(Background.fill(Color.WHITE));
        tickPane.getStyleClass().add("reference");
        setCenter(tickPane);

        policy = new SimpleObjectProperty<>(this, "tabStopPolicy") {
            private Subscription sub;

            @Override
            protected void invalidated() {
                if (sub != null) {
                    sub.unsubscribe();
                    sub = null;
                }
                TabStopPolicy p = get();
                if (p != null) {
                    sub = p.tabStops().subscribe(Ruler.this::clearTicks);
                    sub.and(p.defaultIntervalProperty().subscribe(Ruler.this::requestLayout));
                }
            }
        };

        widthProperty().subscribe(this::requestLayout);
        editor.contentPaddingProperty().subscribe(this::requestLayout);
        editor.documentAreaProperty().subscribe(this::requestLayout);

        editor.modelProperty().subscribe(this::clearTicks);
        editor.selectionProperty().subscribe(this::clearTicks);

        tickPane.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        tickPane.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        tickPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
    }

    public final void setTabStopPolicy(TabStopPolicy p) {
        policy.set(p);
    }

    public final TabStopPolicy getTabStopPolicy() {
        return policy.get();
    }

    public final ObjectProperty<TabStopPolicy> tabStopPolicyProperty() {
        return policy;
    }

    public final ObjectProperty<Runnable> onChangeProperty() {
        if (onChange == null) {
            onChange = new SimpleObjectProperty<>();
        }
        return onChange;
    }

    public final void setOnChange(Runnable action) {
        onChangeProperty().set(action);
    }

    public final Runnable getOnChange() {
        return onChange == null ? null : onChange.get();
    }

    private void clearTicks() {
        ticks = null;
        requestLayout();
    }

    private List<Tick> ticks() {
        if (ticks == null) {
            ticks = createTicks();
        }
        return ticks;
    }

    private List<Tick> createTicks() {
        TabStopPolicy p = policy.get();
        if (p == null) {
            return List.of();
        }
        double width = getWidth();
        double x = 0.0;
        ArrayList<Tick> ts = new ArrayList<>(16);

        // tab stops
        for (TabStop t : p.tabStops()) {
            x = (float)t.getPosition();
            if ((x - HALFWIDTH) > width) {
                break;
            }
            ts.add(createTabStop(t));
        }

        // default stops
        double defaultStops = p.getDefaultInterval();
        if (defaultStops > 0.0) {
            for (;;) {
                x = nextPosition(x, defaultStops);
                if ((x - HALFWIDTH) > width) {
                    break;
                }
                ts.add(createTick(x));
            }
        }
        return ts;
    }

    // similar to FixedTabAdvancePolicy.nextPosition()
    private static double nextPosition(double position, double tabAdvance) {
        double n = (position / tabAdvance);
        return ((int)(n + Math.ulp(n)) + 1) * tabAdvance;
    }

    @Override
    protected void layoutChildren() {
        // account for left/right decorators and content padding
        double left = 0.0;
        double right = 0.0;
        Bounds b = editor.getDocumentArea();
        if (b != null) {
            left = b.getMinX();
            right = editor.getWidth() - b.getMaxX();
        }
        Insets m = editor.getContentPadding();
        if (m != null) {
            left += m.getLeft();
            right += m.getRight();
        }
        m = new Insets(0, right, 0, left);
        layoutInArea(tickPane, 0, 0, getWidth(), getHeight(), 0, m, true, true, HPos.CENTER, VPos.CENTER);

        // TODO listen to default tab stop policy in the model
        tickPane.getChildren().setAll(ticks());
    }

    private Tick findTabStop(MouseEvent ev) {
        PickResult pick = ev.getPickResult();
        Node n = pick.getIntersectedNode();
        if (n instanceof Tick t) {
            if (t.isTabStop()) {
                return t;
            }
        }
        return null;
    }

    private void handleSelection(SelectionSegment sel) {
        this.selection = sel;
        // TODO update tab stop policy
        if (sel == null) {
            // use model's default tab stops
        } else {
            // single selection vs multiple selection
        }
    }

    private void handleMousePressed(MouseEvent ev) {
        popup = ev.isPopupTrigger();
        if (!popup) {
            double x = ev.getX();
            dragging = false;
            modified = false;
            clickedStop = findTabStop(ev);
            ev.consume();
        }
    }

    private void handleMouseReleased(MouseEvent ev) {
        if (popup || ev.isPopupTrigger()) {
            return;
        }
        TabStopPolicy p = policy.get();
        if (p == null) {
            return;
        }

        // was dragged? update tab stops
        // was tabstop? remove
        if (clickedStop == null) {
            double x = ev.getX();
            Tick t = createTabStop(new TabStop(x));
            tickPane.getChildren().add(t);
            ticks().add(t);
            modified = true;
        } else {
            boolean remove = (dragging && deduplicate()) | (!dragging);
            if (remove) {
                tickPane.getChildren().remove(clickedStop);
                ticks().remove(clickedStop);
                modified = true;
            }
        }
        if (modified) {
            List<TabStop> ts = toTabStops();
            p.tabStops().setAll(ts);
            modified = false;
            Runnable r = getOnChange();
            if (r != null) {
                r.run();
            }
        }
        clickedStop = null;
        dragging = false;
        ev.consume();
    }

    // returns true if the current tick should be removed
    private boolean deduplicate() {
        double pos = clickedStop.position;
        for (Tick t : ticks()) {
            if (t != clickedStop) {
                if (t.isTabStop()) {
                    if (Math.abs(pos - t.position) < TOO_CLOSE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handleMouseDragged(MouseEvent ev) {
        // update the tabstop being dragged
        if (clickedStop != null) {
            double x = Math.max(0, ev.getX());
            clickedStop.position = x;
            clickedStop.tabStop = new TabStop(x);
            clickedStop.getElements().setAll(createTabStopPathElements(x));
            dragging = true;
            modified = true;
            ev.consume();
        }
    }

    private List<PathElement> createTabStopPathElements(double x) {
        double height = getHeight();
        double h2 = height / 2.0;
        ArrayList<PathElement> es = new ArrayList<>(5);
        es.add(new MoveTo(x, 0));
        es.add(new LineTo(x + HALFWIDTH, h2));
        es.add(new LineTo(x, height));
        es.add(new LineTo(x - HALFWIDTH, h2));
        es.add(new ClosePath());
        return es;
    }

    private Tick createTabStop(TabStop tab) {
        double x = tab.getPosition();
        Tick t = new Tick(x);
        t.tabStop = tab;
        t.setStroke(Color.BLACK);
        t.setStrokeWidth(0.5);
        t.setStrokeLineJoin(StrokeLineJoin.BEVEL);
        t.getElements().setAll(createTabStopPathElements(x));
        return t;
    }

    private Tick createTick(double x) {
        Tick t = new Tick(x);
        t.setStroke(Color.BLACK);
        t.setStrokeWidth(1.0);
        ArrayList<PathElement> es = new ArrayList<>(2);
        es.add(new MoveTo(x, 0));
        es.add(new LineTo(x, getHeight()));
        t.getElements().setAll(es);
        return t;
    }

    private List<TabStop> toTabStops() {
        ArrayList<TabStop> rv = new ArrayList<>();
        // ticks cannot be null at this point
        for (Tick t : ticks()) {
            if (t.isTabStop()) {
                TabStop ts = t.tabStop;
                if (ts.getPosition() > 0.1) {
                    rv.add(ts);
                }
            }
        }
        // sort
        rv.sort(new Comparator<TabStop>() {
            @Override
            public int compare(TabStop a, TabStop b) {
                return (int)Math.signum(a.getPosition() - b.getPosition());
            }
        });
        return rv;
    }

    /** Visual representation of a tab stop */
    private static class Tick extends Path {
        public double position;
        public TabStop tabStop;

        public Tick(double position) {
            this.position = position;
            setManaged(false);
            setPickOnBounds(true);
        }

        private boolean isTabStop() {
            return tabStop != null;
        }
    }
}
