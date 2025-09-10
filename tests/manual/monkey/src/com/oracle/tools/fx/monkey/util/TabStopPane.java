/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.fx.monkey.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Background;
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

/**
 * Visual editor for a TabStopPolicy.
 */
public class TabStopPane extends Pane {

    private final TabStopPolicy policy;
    private int seq;
    private List<Tick> ticks;
    private Tick clickedStop;
    private boolean dragged;
    private boolean modified;
    private static final double HALFWIDTH = 4;
    private static final double TOO_CLOSE = 2;

    public TabStopPane(TabStopPolicy p) {
        this.policy = p;
        setPrefHeight(15);
        setBackground(Background.fill(Color.WHITE));

        p.tabStops().subscribe(this::update);
        p.defaultIntervalProperty().subscribe(this::update);
        widthProperty().subscribe(this::update);

        addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
    }

    private void update() {
        ticks = null;
        requestLayout();
    }

    // ticks: Paths (with TabStop in properties)
    // init: create from policy
    // layout: create/update paths, setAll
    // edit: add/remove ticks, setAll
    // release: update policy
    private List<Tick> createTicks() {
        double width = getWidth();
        double x = 0.0;
        ArrayList<Tick> ts = new ArrayList<>(16);

        // tab stops
        for (TabStop t : policy.tabStops()) {
            x = (float)t.getPosition();
            if ((x - HALFWIDTH) > width) {
                break;
            }
            ts.add(createTabStop(t));
        }

        // default stops
        double defaultStops = policy.getDefaultInterval();
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
        if (ticks == null) {
            ticks = createTicks();
            getChildren().setAll(ticks);
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

    private void handleMousePressed(MouseEvent ev) {
        double x = ev.getX();
        dragged = false;
        modified = false;
        clickedStop = findTabStop(ev);
        ev.consume();
    }

    private void handleMouseReleased(MouseEvent ev) {
        // was dragged? update tab stops
        // was tabstop? remove
        if (clickedStop == null) {
            double x = ev.getX();
            Tick t = createTabStop(new TabStop(x));
            getChildren().add(t);
            ticks.add(t);
            modified = true;
        } else {
            boolean remove = (dragged && deduplicate()) | (!dragged);
            if (remove) {
                getChildren().remove(clickedStop);
                ticks.remove(clickedStop);
                modified = true;
            }
        }
        clickedStop = null;
        dragged = false;
        if (modified) {
            List<TabStop> ts = toTabStops();
            policy.tabStops().setAll(ts);
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
