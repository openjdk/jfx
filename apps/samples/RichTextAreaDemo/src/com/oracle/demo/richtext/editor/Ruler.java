/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
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

    private final RichTextArea editor;
    private final SimpleObjectProperty<TabStopPolicy> policy;
    private int seq;
    private List<Tick> ticks;
    private Tick clickedStop;
    private boolean dragged;
    private boolean modified;
    private Pane tickPane;
    private static final double HEIGHT = 12;
    private static final double HALFWIDTH = 4;
    private static final double TOO_CLOSE = 2;

    public Ruler(RichTextArea editor) {
        this.editor = editor;
        setPrefHeight(HEIGHT);
        setBackground(Background.fill(Color.gray(0.7)));
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
                    sub = p.tabStops().subscribe(Ruler.this::update);
                    sub.and(p.defaultStopsProperty().subscribe(Ruler.this::update));
                }
            }
        };

        widthProperty().subscribe(this::update);
        editor.contentPaddingProperty().subscribe(this::update);
        editor.modelProperty().subscribe(this::update);

        addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);

        editor.selectionProperty().subscribe(this::handleSelection);
    }

    public final void setTabStopPolicy(TabStopPolicy p) {
        policy.set(p);
    }

    public final ObjectProperty<TabStopPolicy> tabStopPolicyProperty() {
        return policy;
    }

    private void update() {
        ticks = null;
        requestLayout();
    }

    private List<Tick> createTicks(TabStopPolicy p) {
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
        double defaultStops = p.getDefaultStops();
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
        Insets m = editor.getContentPadding();
        if (m != null) {
            m = new Insets(0, m.getRight(), 0, m.getLeft());
        }
        layoutInArea(tickPane, 0, 0, getWidth(), getHeight(), 0, m, true, true, HPos.CENTER, VPos.CENTER);

        // TODO model!  watch for default tabs which should be a part of the model
        if (ticks == null) {
            TabStopPolicy p = policy.get();
            ticks = createTicks(p);
            tickPane.getChildren().setAll(ticks);
        }
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

    // returns true if the current tick should be removed
    private boolean deduplicate() {
        double pos = clickedStop.position;
        for (Tick t : ticks) {
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

    private void handleSelection(SelectionSegment sel) {
        // TODO update tab stop policy
    }

    private void handleMousePressed(MouseEvent ev) {
        double x = ev.getX();
        dragged = false;
        modified = false;
        clickedStop = findTabStop(ev);
        ev.consume();
    }

    private void handleMouseReleased(MouseEvent ev) {
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
            ticks.add(t);
            modified = true;
        } else {
            boolean remove = (dragged && deduplicate()) | (!dragged);
            if (remove) {
                tickPane.getChildren().remove(clickedStop);
                ticks.remove(clickedStop);
                modified = true;
            }
        }
        clickedStop = null;
        dragged = false;
        if (modified) {
            List<TabStop> ts = toTabStops();
            p.tabStops().setAll(ts);
        }
        ev.consume();
    }

    private void handleMouseDragged(MouseEvent ev) {
        // update the tabstop being dragged
        if (clickedStop != null) {
            double x = ev.getX();
            clickedStop.position = x;
            clickedStop.tabStop = new TabStop(x);
            clickedStop.getElements().setAll(createTabStopPathElements(x));
            dragged = true;
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
        for (Tick t : ticks) {
            if (t.isTabStop()) {
                rv.add(t.tabStop);
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
