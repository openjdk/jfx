/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.CaretInfo;
import javafx.scene.text.HitInfo;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextLineInfo;
import javafx.util.Duration;

/**
 * Visualizes text geometry available via LayoutInfo API.
 *
 * show caret and shape modifiers:
 * caret:
 * - SHIFT: legacy caret API
 * - no modifiers: new caret API
 *
 * selection (click and drag):
 * - SHIFT: new selection API
 * - SHORTCUT: new strike-through API
 * - OPTION: new underline API
 * - no modifiers: legacy selection API
 *
 * https://bugs.openjdk.org/browse/JDK-8341670 [Text,TextFlow] Public API for Text Layout Info (Enhancement - P4)
 * https://bugs.openjdk.org/browse/JDK-8341672: [Text/TextFlow] getRangeInfo (Enhancement - P4)
 * https://bugs.openjdk.org/browse/JDK-8341671: [Text/TextFlow] getCaretInfo (Enhancement - P4)
 * https://bugs.openjdk.org/browse/JDK-8341438 TextFlow: incorrect caretShape(), hitTest(), rangeShape() with non-empty padding/border
 * https://bugs.openjdk.org/browse/JDK-8317120 RFE: TextFlow.rangeShape() ignores lineSpacing
 * https://bugs.openjdk.org/browse/JDK-8317122 RFE: TextFlow.preferredHeight ignores lineSpacing
 */
public class LayoutInfoVisualizer {

    public final SimpleObjectProperty<TextShapeLogic> shapeLogic = new SimpleObjectProperty<>(TextShapeLogic.LAYOUT_INFO);
    public final SimpleBooleanProperty showCaretAndRange = new SimpleBooleanProperty();
    public final SimpleBooleanProperty showLines = new SimpleBooleanProperty();
    public final SimpleBooleanProperty showLayoutBounds = new SimpleBooleanProperty();
    public final SimpleBooleanProperty includeLineSpace = new SimpleBooleanProperty();

    private Pane parent;
    private final BooleanBinding isAnimating;
    private final SimpleObjectProperty<Node> owner = new SimpleObjectProperty<>();
    private Timeline animation;
    private Path boundsPath;
    private Path caretPath;
    private Path selectionPath;
    private Path strikeThroughPath;
    private Path underlinePath;
    private Group lines;
    private EventHandler<MouseEvent> mouseListener;
    private int startIndex;
    private boolean useSelectionShape;
    private boolean useStrikeThroughShape;
    private boolean useUnderlineShape;

    private static final double CARET_VIEW_ORDER = 1000;
    private static final double RANGE_VIEW_ORDER = 1010;
    private static final double TEXT_LINES_VIEW_ORDER = 1020;
    private static final double BOUNDS_VIEW_ORDER = 1030;

    public LayoutInfoVisualizer() {
        isAnimating = Bindings.createBooleanBinding(() -> {
                return
                    // FIX stop animation if not showing
                    (owner.get() != null) &&
                    (
                        showLines.get() ||
                        showLayoutBounds.get()
                    );
            },
            owner,
            showLines,
            showLayoutBounds
        );
        isAnimating.addListener((p) -> update());
        showCaretAndRange.addListener((p) -> updateCaretAndRange());
    }

    public void attach(Pane parent, Text t) {
        this.parent = parent;
        owner.set(t);
    }

    public void attach(Pane parent, TextFlow t) {
        this.parent = parent;
        owner.set(t);
    }

    public String caretOptionText() {
        String ctrl = FX.isMac() ? "⌘" : "ctrl";
        return String.format("caret and range (shift: strike-through, %s: underline)", ctrl);
    }

    Node owner() {
        return owner.get();
    }

    void update() {
        if (isAnimating.get()) {
            if (animation == null) {
                animation = new Timeline(
                    new KeyFrame(Duration.millis(100), (ev) -> refresh())
                );
                animation.setCycleCount(Timeline.INDEFINITE);
                animation.setDelay(Duration.millis(20));
                animation.play();
            }
        } else {
            if (animation != null) {
                animation.stop();
                animation = null;
                refresh();
            }
        }
    }

    void refresh() {
        updateLayoutBounds();
        updateTextLines();
    }

    private void updateCaretAndRange() {
        if (showCaretAndRange.get()) {
            // caret
            if (caretPath == null) {
                caretPath = new Path();
                caretPath.setStrokeWidth(1);
                caretPath.setStroke(Color.RED);
                caretPath.setManaged(false);
                caretPath.setViewOrder(CARET_VIEW_ORDER);
                parent.getChildren().add(caretPath);
            }

            // selection
            if (selectionPath == null) {
                selectionPath = new Path();
                selectionPath.setStrokeWidth(0);
                selectionPath.setFill(Color.rgb(0, 128, 255, 0.3));
                selectionPath.setManaged(false);
                selectionPath.setViewOrder(RANGE_VIEW_ORDER);
                parent.getChildren().add(selectionPath);
            }

            // strike through
            if (strikeThroughPath == null) {
                strikeThroughPath = new Path();
                strikeThroughPath.setStrokeWidth(0);
                strikeThroughPath.setFill(Color.rgb(0, 128, 0, 0.7));
                strikeThroughPath.setManaged(false);
                strikeThroughPath.setViewOrder(RANGE_VIEW_ORDER);
                parent.getChildren().add(strikeThroughPath);
            }

            // underline
            if (underlinePath == null) {
                underlinePath = new Path();
                underlinePath.setStrokeWidth(0);
                underlinePath.setFill(Color.rgb(0, 0, 0, 0.3));
                underlinePath.setManaged(false);
                underlinePath.setViewOrder(RANGE_VIEW_ORDER);
                parent.getChildren().add(underlinePath);
            }

            // mouse
            if (mouseListener == null) {
                mouseListener = this::handleMouseEvent;
                owner().addEventHandler(MouseEvent.ANY, mouseListener);
            }
        } else {
            // mouse
            if (mouseListener != null) {
                owner().removeEventHandler(MouseEvent.ANY, mouseListener);
                mouseListener = null;
            }

            // caret
            if (caretPath != null) {
                parent.getChildren().remove(caretPath);
                caretPath = null;
            }

            // selection
            if (selectionPath != null) {
                parent.getChildren().remove(selectionPath);
                selectionPath = null;
            }

            // strike through
            if (strikeThroughPath != null) {
                parent.getChildren().remove(strikeThroughPath);
                strikeThroughPath = null;
            }

            // underline
            if (underlinePath == null) {
                parent.getChildren().remove(underlinePath);
                underlinePath = null;
            }
        }
    }

    private void updateLayoutBounds() {
        if (showLayoutBounds.get()) {
            if (boundsPath == null) {
                boundsPath = new Path();
                boundsPath.setViewOrder(BOUNDS_VIEW_ORDER);
                boundsPath.setStrokeWidth(0);
                boundsPath.setFill(Color.rgb(255, 128, 0, 0.1));
                boundsPath.setManaged(false);
                parent.getChildren().add(boundsPath);
            }

            PathElement[] ps = createLayoutBoundsShape();
            boundsPath.getElements().setAll(ps);
        } else {
            if (boundsPath != null) {
                parent.getChildren().remove(boundsPath);
                boundsPath = null;
            }
        }
    }

    private void updateTextLines() {
        if (showLines.get()) {
            if (lines == null) {
                lines = new Group();
                lines.setAutoSizeChildren(false);
                lines.setViewOrder(TEXT_LINES_VIEW_ORDER);
                lines.setManaged(false);
                parent.getChildren().add(lines);
            }
            lines.getChildren().setAll(createTextLinesShapes());
        } else {
            if (lines != null) {
                parent.getChildren().remove(lines);
                lines = null;
            }
        }
    }

    private LayoutInfo layoutInfo() {
        Node n = owner();
        if (n instanceof Text t) {
            return t.getLayoutInfo();
        } else if (n instanceof TextFlow t) {
            return t.getLayoutInfo();
        }
        return null;
    }

    private int getTextLength() {
        Node n = owner();
        if (n instanceof Text t) {
            return t.getText().length();
        } else if (n instanceof TextFlow t) {
            return FX.getTextLength(t);
        }
        return 0;
    }

    private HitInfo hitInfo(MouseEvent ev) {
        Node n = owner();
        double x = ev.getScreenX();
        double y = ev.getScreenY();
        Point2D p = n.screenToLocal(x, y);
        if (n instanceof Text t) {
            return t.hitTest(p);
        } else if (n instanceof TextFlow t) {
            TextShapeLogic logic = textShapeLogic();
            switch(logic) {
            case NEW:
                return t.getHitInfo(p);
            case LAYOUT_INFO:
                return t.getHitInfo(p);
            case LEGACY:
                return t.hitTest(p);
            }
        }
        return null;
    }

    private static PathElement[] toPathElementsArray(List<Rectangle2D> rs) {
        ArrayList<PathElement> a = new ArrayList<>();
        for (Rectangle2D r : rs) {
            a.add(new MoveTo(r.getMinX(), r.getMinY()));
            a.add(new LineTo(r.getMaxX(), r.getMinY()));
            a.add(new LineTo(r.getMaxX(), r.getMaxY()));
            a.add(new LineTo(r.getMinX(), r.getMaxY()));
            a.add(new LineTo(r.getMinX(), r.getMinY()));
        }
        return a.toArray(PathElement[]::new);
    }

    private static Color color(int index) {
        switch (index % 3) {
        case 0:
            return Color.rgb(255, 0, 0, 0.3);
        case 1:
            return Color.rgb(0, 255, 0, 0.3);
        default:
            return Color.rgb(0, 0, 255, 0.3);
        }
    }

    private List<Node> createTextLinesShapes() {
        LayoutInfo la = layoutInfo();
        List<TextLineInfo> lines = la.getTextLines(includeLineSpace.get());

        double dx = 0.0;
        double dy = 0.0;
        if (owner() instanceof Text t) {
            Bounds b = t.getBoundsInLocal();
            dx = b.getMinX();
            dy = b.getMinY();
        }

        ArrayList<Node> rv = new ArrayList<>();
        int i = 0;
        for (TextLineInfo line : lines) {
            Rectangle2D b = line.bounds();
            Color c = color(i++);
            Rectangle r = new Rectangle(b.getMinX() - dx, b.getMinY() - dy, b.getWidth(), b.getHeight());
            r.setFill(c);
            r.setStrokeWidth(0);
            r.setManaged(false);
            rv.add(r);
        }
        return rv;
    }

    private PathElement[] correctForTextOrigin(Text t, PathElement[] es) {
        Bounds b = t.getBoundsInLocal();
        double dx = b.getMinX();
        double dy = b.getMinY();
        if (dx == 0.0 && dy == 0.0) {
            return es;
        }

        dx = -dx;
        dy = -dy;

        PathElement[] rv = new PathElement[es.length];
        for (int i = 0; i < es.length; i++) {
            PathElement em = es[i];
            PathElement shifted;
            if (em instanceof MoveTo v) {
                shifted = new MoveTo(v.getX() + dx, v.getY() + dy);
            } else if (em instanceof LineTo v) {
                shifted = new LineTo(v.getX() + dx, v.getY() + dy);
            } else {
                shifted = em;
            }
            rv[i] = shifted;
        }
        return rv;
    }

    private static PathElement[] createCaretShape(CaretInfo ci) {
        ArrayList<PathElement> a = new ArrayList<>();
        for (int i = 0; i < ci.getSegmentCount(); i++) {
            Rectangle2D r = ci.getSegmentAt(i);
            a.add(new MoveTo(r.getMinX(), r.getMinY()));
            a.add(new LineTo(r.getMaxX(), r.getMaxY()));
        }
        return a.toArray(PathElement[]::new);
    }

    private PathElement[] createCaretShape(int charIndex, boolean leading) {
        TextShapeLogic logic = textShapeLogic();
        Node n = owner();
        if (n instanceof Text t) {
            switch (logic) {
            case NEW:
                break;
            case LAYOUT_INFO:
                CaretInfo ci = layoutInfo().caretInfoAt(charIndex, leading);
                return correctForTextOrigin(t, createCaretShape(ci));
            case LEGACY:
                return correctForTextOrigin(t, t.caretShape(charIndex, leading));
            default:
                break;
            }
        } else if (n instanceof TextFlow t) {
            switch (logic) {
            case NEW:
                return t.getCaretShape(charIndex, leading);
            case LAYOUT_INFO:
                CaretInfo ci = layoutInfo().caretInfoAt(charIndex, leading);
                return createCaretShape(ci);
            case LEGACY:
                return t.caretShape(charIndex, leading);
            default:
                break;
            }
        }
        return new PathElement[0];
    }

    private PathElement[] createLayoutBoundsShape() {
        LayoutInfo la = layoutInfo();
        List<Rectangle2D> rs = List.of(la.getLogicalBounds(includeLineSpace.get()));
        PathElement[] es = toPathElementsArray(rs);
        if (owner() instanceof Text t) {
            es = correctForTextOrigin(t, es);
        }
        return es;
    }

    void handleMouseEvent(MouseEvent ev) {
        // caret
        HitInfo h = hitInfo(ev);
        int charIndex = h.getCharIndex();
        boolean leading = h.isLeading();
        caretPath.getElements().setAll(createCaretShape(charIndex, leading));

        // range
        var t = ev.getEventType();
        if (t == MouseEvent.MOUSE_PRESSED) {
            startIndex = h.getInsertionIndex();
            // drag: selection, drag+shift: stroke-through, drag+shortcut:underline
            useSelectionShape = !ev.isShiftDown() && !ev.isShortcutDown();
            useStrikeThroughShape = ev.isShiftDown() && !ev.isShortcutDown();
            useUnderlineShape = !ev.isShiftDown() && ev.isShortcutDown();
            ev.consume();
        } else if (t == MouseEvent.MOUSE_DRAGGED) {
            int start = startIndex;
            int end = h.getInsertionIndex();
            if (end < start) {
                int tmp = end;
                end = start;
                start = tmp;
            }

            if (useSelectionShape) {
                PathElement[] es = createSelectionShape(start, end);
                selectionPath.getElements().setAll(es);
            }

            if (useStrikeThroughShape) {
                PathElement[] es = createStrikeThroughShape(start, end);
                strikeThroughPath.getElements().setAll(es);
            }

            if (useUnderlineShape) {
                PathElement[] es = createUnderlineShape(start, end);
                underlinePath.getElements().setAll(es);
            }

            ev.consume();
        } else if (t == MouseEvent.MOUSE_RELEASED) {
            selectionPath.getElements().clear();
            strikeThroughPath.getElements().clear();
            underlinePath.getElements().clear();
            ev.consume();
        }
    }

    private PathElement[] createSelectionShape(int start, int end) {
        TextShapeLogic logic = textShapeLogic();
        Node n = owner();
        if (n instanceof Text t) {
            switch (logic) {
            case NEW:
                return correctForTextOrigin(t, t.getRangeShape(start, end, includeLineSpace.get()));
            case LAYOUT_INFO:
                return correctForTextOrigin(t, selectionShape(t.getLayoutInfo(), start, end));
            case LEGACY:
                return correctForTextOrigin(t, t.rangeShape(start, end));
            default:
                break;
            }
        } else if (n instanceof TextFlow t) {
            switch (logic) {
            case NEW:
                return t.getRangeShape(start, end, includeLineSpace.get());
            case LAYOUT_INFO:
                return selectionShape(t.getLayoutInfo(), start, end);
            case LEGACY:
                return t.rangeShape(start, end);
            default:
                break;
            }
        }
        return new PathElement[0];
    }

    private PathElement[] createStrikeThroughShape(int start, int end) {
        TextShapeLogic logic = textShapeLogic();
        Node n = owner();
        if (n instanceof Text t) {
            switch (logic) {
            case NEW:
                return correctForTextOrigin(t, t.getStrikeThroughShape(start, end));
            case LAYOUT_INFO:
                return correctForTextOrigin(t, strikeThroughShape(t.getLayoutInfo(), start, end));
            case LEGACY:
                // not available
                break;
            default:
                break;
            }
        } else if (n instanceof TextFlow t) {
            switch (logic) {
            case NEW:
                return t.getStrikeThroughShape(start, end);
            case LAYOUT_INFO:
                return strikeThroughShape(t.getLayoutInfo(), start, end);
            case LEGACY:
                // not available
                break;
            default:
                break;
            }
        }
        return new PathElement[0];
    }

    private PathElement[] createUnderlineShape(int start, int end) {
        TextShapeLogic logic = textShapeLogic();
        Node n = owner();
        if (n instanceof Text t) {
            switch (logic) {
            case NEW:
                // using legacy API
                return correctForTextOrigin(t, t.underlineShape(start, end));
            case LAYOUT_INFO:
                return correctForTextOrigin(t, underlineShape(t.getLayoutInfo(), start, end));
            case LEGACY:
                return correctForTextOrigin(t, t.underlineShape(start, end));
            default:
                break;
            }
        } else if (n instanceof TextFlow t) {
            switch (logic) {
            case NEW:
                return t.getUnderlineShape(start, end);
            case LAYOUT_INFO:
                return underlineShape(t.getLayoutInfo(), start, end);
            case LEGACY:
                return t.underlineShape(start, end);
            default:
                break;
            }
        }
        return new PathElement[0];
    }

    private PathElement[] selectionShape(LayoutInfo la, int start, int end) {
        List<Rectangle2D> rs = la.getSelectionGeometry(start, end, includeLineSpace.get());
        return toPathElementsArray(rs);
    }

    private PathElement[] strikeThroughShape(LayoutInfo la, int start, int end) {
        List<Rectangle2D> rs = la.getStrikeThroughGeometry(start, end);
        return toPathElementsArray(rs);
    }

    private PathElement[] underlineShape(LayoutInfo la, int start, int end) {
        List<Rectangle2D> rs = la.getUnderlineGeometry(start, end);
        return toPathElementsArray(rs);
    }

    private TextShapeLogic textShapeLogic() {
        TextShapeLogic v = shapeLogic.get();
        return v == null ? TextShapeLogic.LEGACY : v;
    }
}
