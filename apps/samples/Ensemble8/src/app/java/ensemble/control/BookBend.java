/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates.
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
package ensemble.control;


import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.paint.*;
import javafx.scene.shape.*;

public class BookBend {
    private Color bendEndColor = Color.LIGHTBLUE.interpolate(Color.BLACK, 0.6);
    private Color bendStartColor = Color.LIGHTBLUE.darker();
    private Color pathColor = Color.LIGHTBLUE;
    private int newWidth, newHeight;
    private double oldTargetX, oldTargetY;
    private double targetX = 250;
    private double targetY = 250;
    private Path clip;
    private Path p;
    private Path shadow;
    private float[][] buffer = new float[1000][2];
    private FloatMap map = new FloatMap();
    private Node node;
    private boolean updateNeeded = false;
    private boolean isAnimationTimerActive = false;
    private AnimationTimer animationTimer = new AnimationTimer() {

        @Override
        public void handle(long l) {
            if(updateNeeded){
                update();
            }
        }
    };

    private void startAnimationTimer(){
        if(!isAnimationTimerActive){
            this.animationTimer.start();
            isAnimationTimerActive = true;
        }
    }

    private void stopAnimationTimer(){
        this.animationTimer.stop();
        isAnimationTimerActive = false;
    }

    private void setUpdateNeeded(boolean value){
        if(isAnimationTimerActive == false && value == true && node.getScene() != null){
            startAnimationTimer();
        }
        this.updateNeeded = value;
    }

    public BookBend(final Node node, Path path) {
        this(node, path, null, null);
    }

    public BookBend(final Node node, Path path, Path shadow) {
        this(node, path, shadow, null);
    }

    /**
     * Creates DisplacementMap effect for the bend
     * @param node target node
     * @param path path that is used to draw the opposite side of a bend
     * Its fill is updated with linear gradient.
     * @param shadow path that is used to draw the shadow of a bend
     * Its fill is updated with linear gradient.
     * @param clip path that is used to clip the content of the page either
     * for mouse operations or for visuals
     */
    public BookBend(final Node node, Path path, Path shadow, Path clip) {
        this.node = node;
        this.p = path;
        this.shadow = shadow;
        this.clip = clip;

        node.setEffect(new DisplacementMap(map));
        node.layoutBoundsProperty().addListener((Observable arg0) -> {
            newWidth = (int) Math.round(node.getLayoutBounds().getWidth());
            newHeight = (int) Math.round(node.getLayoutBounds().getHeight());
            if (newWidth != map.getWidth() || newHeight != map.getHeight()) {
                setUpdateNeeded(true);
            }
        });
        node.sceneProperty().addListener((ObservableValue<? extends Scene> ov, Scene oldValue, Scene newValue) -> {
            if(newValue == null){
                stopAnimationTimer();
            }
        });
        newWidth = (int) Math.round(node.getLayoutBounds().getWidth());
        newHeight = (int) Math.round(node.getLayoutBounds().getHeight());
    }

    /**
     * Sets colors for path gradient. Values are used on next update().
     * @param pathColor Path Color
     * @param bendStartColor Starting path color
     * @param bendEndColor  Ending path color
     */
    public void setColors(Color pathColor, Color bendStartColor, Color bendEndColor) {
        this.pathColor = pathColor;
        this.bendEndColor = bendEndColor;
        this.bendStartColor = bendStartColor;

        Paint fill = p.getFill();
        if (fill instanceof LinearGradient) {
            LinearGradient lg = (LinearGradient) fill;
            if (lg.getStops().size() >= 3) {
                p.setFill(new LinearGradient(lg.getStartX(), lg.getStartY(), lg.getEndX(), lg.getEndY(), false, CycleMethod.NO_CYCLE,
                        new Stop(lg.getStops().get(0).getOffset(), pathColor),
                        new Stop(lg.getStops().get(1).getOffset(), bendStartColor),
                        new Stop(lg.getStops().get(2).getOffset(), bendEndColor)));
            }
        }
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public Color getPathColor() {
        return pathColor;
    }

    public Color getBendEndColor() {
        return bendEndColor;
    }

    public Color getBendStartColor() {
        return bendStartColor;
    }

    /**
     * Updates DisplacementMap and path to target coordinates.
     * @param targetX target X
     * @param targetY  target Y
     */
    public void update(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
        if (this.targetX != oldTargetX || this.targetY != oldTargetY) {
            setUpdateNeeded(true);
        }
    }

    /**
     * Updates DisplacementMap and path for current coordinates.
     */
    public void update() {
        setUpdateNeeded(false);

        if (newWidth == map.getWidth() && newHeight == map.getHeight()
                && targetX == oldTargetX && targetY == oldTargetY) {
            return;
        }
        oldTargetX = targetX;
        oldTargetY = targetY;
        if (newWidth != map.getWidth() || newHeight != map.getHeight()) {
            map.setWidth(newWidth);
            map.setHeight(newHeight);
        }

        final double W = node.getLayoutBounds().getWidth();
        final double H = node.getLayoutBounds().getHeight();

        // target point F for folded corner
        final double xF = Math.min(targetX, W - 1);
        final double yF = Math.min(targetY, H - 1);

        final Point2D F = new Point2D(xF, yF);

        // corner point O
        final double xO = W;
        final double yO = H;

        // distance between them
        final double FO = Math.hypot(xF - xO, yF - yO);
        final double AF = FO / 2;

        final double AC = Math.min(AF * 0.5, 200);

        // radius of the fold as seen along the l2 line
        final double R = AC / Math.PI * 1.5;
        final double BC = R;
        final double flat_R = AC;

        // Gradient for the line from target point to corner point
        final double K = (yO - yF) / (xO - xF);

        // angle of a line l1
        final double ANGLE = Math.atan(1 / K);

        // point A (on line l1 - the mirror line of target and corner points)
        final double xA = (xO + xF) / 2;
        final double yA = (yO + yF) / 2;

        // end points of line l1
        final double bottomX = xA - (H - yA) * K;
        final double bottomY = H;
        final double rightX = W;
        final double rightY = yA - (W - xA) / K;

        final Point2D RL1 = new Point2D(rightX, rightY);
        final Point2D BL1 = new Point2D(bottomX, bottomY);

        // point C (on line l2 - the line when distortion begins)
        final double kC = AC / AF;
        final double xC = xA - (xA - xF) * kC;
        final double yC = yA - (yA - yF) * kC;

        final Point2D C = new Point2D(xC, yC);

        final Point2D RL2 = new Point2D(W, yC - (W - xC) / K);
        final Point2D BL2 = new Point2D(xC - (H - yC) * K, H);

        // point B (on line l3 - the line where distortion ends)
        final double kB = BC / AC;
        final double xB = xC + (xA - xC) * kB;
        final double yB = yC + (yA - yC) * kB;

        // Bottom ellipse calculations
        final Point2D BP1 = calcIntersection(F, BL1, BL2, C);
        final Point2D BP3 = BL2;
        final Point2D BP2 = middle(BP1, BP3, 0.5);
        final Point2D BP4 = new Point2D(xB + BP2.getX() - xC, yB + BP2.getY() - yC);

        final double bE_x1 = hypot(BP2, BP3);
        final double bE_y2 = -hypot(BP2, BP4);
        final double bE_yc = -hypot(BP2, BL1);
        final double bE_y0 = bE_y2 * bE_y2 / (2 * bE_y2 - bE_yc);
        final double bE_b = bE_y0 - bE_y2;
        final double bE_a = Math.sqrt(-bE_x1 * bE_x1 / bE_y0 * bE_b * bE_b / bE_yc);

        // Right ellipse calculations
        final Point2D RP1 = calcIntersection(F, RL1, RL2, C);
        final Point2D RP3 = RL2;
        final Point2D RP2 = middle(RP1, RP3, 0.5);
        final Point2D RP4 = new Point2D(xB + RP2.getX() - xC, yB + RP2.getY() - yC);

        final double rE_x1 = hypot(RP2, RP3);
        final double rE_y2 = -hypot(RP2, RP4);
        final double rE_yc = -hypot(RP2, RL1);
        final double rE_y0 = rE_y2 * rE_y2 / (2 * rE_y2 - rE_yc);
        final double rE_b = rE_y0 - rE_y2;
        final double rE_a = Math.sqrt(-rE_x1 * rE_x1 / rE_y0 * rE_b * rE_b / rE_yc);

        p.setFill(new LinearGradient(xF, yF, xO, yO, false, CycleMethod.NO_CYCLE,
                new Stop(0, pathColor),
                new Stop((xC - xF) / (xO - xF), bendStartColor),
                new Stop((xB - xF) / (xO - xF), bendEndColor)));

        ArcTo arcTo1 = new ArcTo();
        arcTo1.setXAxisRotation(Math.toDegrees(-ANGLE));
        arcTo1.setRadiusX(bE_a);
        arcTo1.setRadiusY(bE_b);
        arcTo1.setX(BP1.getX());
        arcTo1.setY(BP1.getY());
        ArcTo arcTo2 = new ArcTo();
        arcTo2.setXAxisRotation(Math.toDegrees(-ANGLE));
        arcTo2.setRadiusX(rE_a);
        arcTo2.setRadiusY(rE_b);
        arcTo2.setX(RP4.getX());
        arcTo2.setY(RP4.getY());

        p.getElements().setAll(
                new MoveTo(BP4.getX(), BP4.getY()),
                arcTo1,
                new LineTo(xF, yF),
                new LineTo(RP1.getX(), RP1.getY()),
                arcTo2,
                new ClosePath());

        if (shadow != null) {
            double level0 = (xB - xF) / (xO - xF) - R / FO * 0.5;
            double level1 = (xB - xF) / (xO - xF) + (0.3 + (200 - AC) / 200) * R / FO;
            shadow.setFill(new LinearGradient(xF, yF, xO, yO, false, CycleMethod.NO_CYCLE,
                    new Stop(level0, Color.rgb(0, 0, 0, 0.7)),
                    new Stop(level0 * 0.3 + level1 * 0.7, Color.rgb(0, 0, 0, 0.25)),
                    new Stop(level1, Color.rgb(0, 0, 0, 0.0)),
                    new Stop(1, Color.rgb(0, 0, 0, 0))));

            ArcTo arcTo3 = new ArcTo();
            arcTo3.setXAxisRotation(Math.toDegrees(-ANGLE));
            arcTo3.setRadiusX(rE_a);
            arcTo3.setRadiusY(rE_b);
            arcTo3.setX(RP4.getX());
            arcTo3.setY(RP4.getY());
            arcTo3.setSweepFlag(true);
            ArcTo arcTo4 = new ArcTo();
            arcTo4.setXAxisRotation(Math.toDegrees(-ANGLE));
            arcTo4.setRadiusX(bE_a);
            arcTo4.setRadiusY(bE_b);
            arcTo4.setX(BP3.getX());
            arcTo4.setY(BP3.getY());
            arcTo4.setSweepFlag(true);

            shadow.getElements().setAll(
                    new MoveTo(RP3.getX(), RP3.getY()),
                    arcTo3,
                    new LineTo(BP4.getX(), BP4.getY()),
                    arcTo4,
                    new LineTo(xO, yO),
                    new ClosePath());
        }

        if (clip != null) {
            final Point2D RL3 = new Point2D(W, yB - (W - xB) / K);
            final Point2D BL3 = new Point2D(xB - (H - yB) * K, H);

            clip.getElements().setAll(
                    new MoveTo(0, 0),
                    RL3.getY() > 0 ? new LineTo(W, 0) : new LineTo(0, 0),
                    RL3.getY() >= 0 ? new LineTo(RL3.getX(), RL3.getY()) : new LineTo(xB - (0 - yB) * K, 0),
                    BL3.getX() >= 0 ? new LineTo(BL3.getX(), BL3.getY()) : new LineTo(0, yB - (0 - xB) / K),
                    BL3.getX() > 0 ? new LineTo(0, H) : new LineTo(0, 0),
                    new ClosePath());
        }

        final double K2 = -K;
        final double C2 = BP3.getX() - K2 * H;

        final double K3 = -K;
        final double C3 = xB - K3 * yB;

        final double STEP = Math.max(0.1, R / (buffer.length - 1));
        final double HYPOT = Math.hypot(1, K);
        final double yR = 1.5 * R;
        double x_1 = 0, y_1 = 0, cur_len = 0;
        for (double len = 0; len <= R; len += STEP) {
            final int index = (int) Math.round(len / STEP);
            final double angle = Math.asin(len / R);
            final double y = yR * Math.cos(angle);
            if (len > 0) {
                cur_len += Math.hypot(y - y_1, len - x_1);
            }
            buffer[index][0] = (float) angle;
            buffer[index][1] = (float) (cur_len * flat_R);
            x_1 = len;
            y_1 = y;
        }
        double total_len = cur_len;
        for (double len = 0; len <= R; len += STEP) {
            final int index = (int) Math.round(len / STEP);
            final double flat_len = buffer[index][1] / total_len;
            final double delta_len = flat_len - len;
            final double xs = delta_len / HYPOT;
            final double ys = K * delta_len / HYPOT;
            buffer[index][0] = (float) (xs / W);
            buffer[index][1] = (float) (ys / H);
        }

        for (int y = 0; y < map.getHeight(); y++) {
            final double lx2 = K2 * (y + 0.5) + C2;
            final double lx3 = K3 * (y + 0.5) + C3;
            for (int x = 0; x < map.getWidth(); x++) {
                if (x + 0.5 < lx2) {
                    map.setSamples(x, y, 0, 0);
                } else if (x + 0.5 >= lx3 - 1) {
                    map.setSamples(x, y, 1, 0);
                } else {
                    final double len = Math.abs((x + 0.5) - K2 * (y + 0.5) - C2) / HYPOT;
                    final int index = (int) Math.round(len / STEP);
                    map.setSamples(x, y, buffer[index][0], buffer[index][1]);
                }
            }
        }
    }

    private static Point2D calcIntersection(Point2D ap1, Point2D ap2, Point2D bp1, Point2D bp2) {
        final double a1 = ap1.getY() - ap2.getY();
        final double b1 = ap2.getX() - ap1.getX();
        final double c1 = ap1.getX() * ap2.getY() - ap2.getX() * ap1.getY();
        final double a2 = bp1.getY() - bp2.getY();
        final double b2 = bp2.getX() - bp1.getX();
        final double c2 = bp1.getX() * bp2.getY() - bp2.getX() * bp1.getY();
        final double d = a1 * b2 - a2 * b1;
        return new Point2D(
                (b1 * c2 - b2 * c1) / d,
                (c1 * a2 - c2 * a1) / d);
    }

    private static Point2D middle(Point2D a, Point2D a1, double value) {
        return new Point2D(
                a1.getX() * value + a.getX() * (1 - value),
                a1.getY() * value + a.getY() * (1 - value));
    }

    private static double hypot(Point2D p1, Point2D p2) {
        return Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    public void detach() {
        node.setEffect(null);
    }
}
