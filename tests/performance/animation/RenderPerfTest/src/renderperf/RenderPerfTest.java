/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package renderperf;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.control.Button;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * {@link RenderPerfTest} is an application to measure graphic rendering performance
 * of JavaFX graphic rendering by calculatiing the Frames per second (FPS) value.
 * The application calculates the FPS by creating a JavaFX environment and rendering
 * objects such as Circle, Image, Text etc based on the test executed. Rendered objects
 * are animated by changing their coordinates every frame which creates animation.
 * Each test case is run for a duration of {@link #TEST_TIME} and number of frames rendered
 * are tracked to calculate FPS.
 *
 * <p>
 * Steps to run the application:
 * <ol>
 *  <li>cd RenderPerfTest/src</li>
 *  <li>Command to compile the program: javac {@literal @}{@literal <}path_to{@literal >}/compile.args renderperf/{@link RenderPerfTest}.java</li>
 *  <li>Command to execute the program: java {@literal @}{@literal <}path_to{@literal >}/run.args renderperf/{@link RenderPerfTest} -t {@literal <}test_name{@literal <} -n {@literal <}number_of_objects{@literal <} -h</li>
 *  Where:
 *  <ul>
 *      <li>test_name: Name of the test to be executed. If not specified, all tests are executed.</li>
 *      <li>number_of_objects: Number of objects to be rendered in the test. If not specified, default value is 1000.</li>
 *      <li>-h: help: prints application usage.</li>
 *  </ul>
 * NOTE: Set JVM command line parameter -Djavafx.animation.fullspeed=true to run animation at full speed
 * </ol>
 * <p>
 * Example - Command to execute the Circle test with 10000 circle objects: <br>
 * java -Djavafx.animation.fullspeed=true {@literal @}{@literal <}path_to{@literal >}/run.args renderperf/{@link RenderPerfTest} -t Circle -n 10000.
 *
 */

public class RenderPerfTest {
    private static final double WIDTH = 800;
    private static final double HEIGHT = 800;
    private static final double R = 25;
    private static final long SECOND_IN_NANOS = 1_000_000_000L;
    private static final long WARMUP_TIME_SECONDS = 5;
    private static final long WARMUP_TIME = WARMUP_TIME_SECONDS * SECOND_IN_NANOS;
    private static final long DEFAULT_TEST_TIME_SECONDS = 10;
    private static final Color[] marker = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.MAGENTA};

    private static Stage stage;
    private static Scene scene;
    private static Group group;
    private static Random random = new Random(100);
    private static int objectCount = 0;
    private static long testDuration = DEFAULT_TEST_TIME_SECONDS;
    private static ArrayList<String> testList = null;

    interface Renderable {
        void addComponents(Group node);
        void updateCoordinates();
        void updateComponentCoordinates();
        void releaseResource();
    }

    private static int roundUpTo4(int n) {
        if (n % 4 != 0) {
            n = (n + 3) & ~(0x3);
            System.out.println("Test requires object count that is a multiple of 4 - rounding up to " + n);
        }
        return n;
    }

    /**
     * This method is used to create object of {@link ParticleRenderable}
     * which will be used to call the {@link PerfMeter#exec} method of
     * {@link PerfMeter} to render the components or update coordinates.
     * This method shall be called for each teast case.
     *
     * @param renderer object of a particle renderer which inherits from {@link ParticleRenderer}
     *
     * @return object of {@link ParticleRenderable}
     */
    ParticleRenderable createPR(ParticleRenderer renderer) {
        return new ParticleRenderable(renderer, WIDTH, HEIGHT);
    }

    /**
     * Primary function of {@link ParticleRenderable} class is to invoke
     * the methods of {@link Particles} class which in turn invokes
     * {@link ParticleRenderable} methods to render components on JavaFX application.
     * Object of {@link ParticleRenderable} is created for every {@link ParticleRenderable}
     * child class i.e for each test case which makes it easy to reuse the
     * coordinates generated for rendering the components.
     * This class helps in separating the individual test case component rendering code
     * independant of the test execution.
     */
    static class ParticleRenderable implements Renderable {
        private ParticleRenderer renderer;

        private double[] bx;
        private double[] by;
        private double[] vx;
        private double[] vy;
        private double r;
        private int n;

        private double width;
        private double height;

        ParticleRenderable(ParticleRenderer renderer, double width, double height) {
            this.renderer = renderer;
            this.n = renderer.getObjectCount();
            this.r = renderer.getParticleRadius();

            bx = new double[n];
            by = new double[n];
            vx = new double[n];
            vy = new double[n];
            this.width = width;
            this.height = height;

            for (int i = 0; i < n; i++) {
                bx[i] = random.nextDouble(r, (width - r));
                by[i] = random.nextDouble(r, (height - r));
                vx[i] = random.nextDouble(-2, 2);
                vy[i] = random.nextDouble(-2, 2);
            }
        }

        @Override
        public void addComponents(Group node) {
            renderer.addComponents(node, n, bx, by, vx, vy);
        }

        @Override
        public void updateCoordinates() {
            for (int i = 0; i < n; i++) {
                bx[i] += vx[i];
                if (bx[i] + r > width || bx[i] - r < 0) vx[i] = -vx[i];
                by[i] += vy[i];
                if (by[i] + r > height || by[i] - r < 0) vy[i] = -vy[i];
            }
        }

        @Override
        public void updateComponentCoordinates() {
            renderer.updateComponentCoordinates(n, bx, by, vx, vy);
        }

        @Override
        public void releaseResource() {
            renderer.releaseResource();
        }
    }

    /**
     * Interface which shall be implemented by all the particle renderer sub-classes
     * used in different test cases.
     * Methods for adding components, changing location of components and
     * releasing resources used for rendering the components are defined.
     */
    interface ParticleRenderer {
        void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy);
        void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy);
        void releaseResource();
        int getObjectCount();
        double getParticleRadius();
    }

    static abstract class FlatParticleRenderer implements ParticleRenderer {
        Color[] colors;
        int n;
        double r;

        FlatParticleRenderer(int n, double r) {
            colors = new Color[n];
            this.n = n;
            this.r = r;

            for (int i = 0; i < n; i++) {
                colors[i] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            }
        }

        public void releaseResource() {
            colors = null;
        }

        public int getObjectCount() {
            return n;
        }

        public double getParticleRadius() {
            return r;
        }
    }

    static class ArcRenderer extends FlatParticleRenderer {
        Arc[] arc;

        ArcRenderer(int n, double r) {
            super(n, r);
            arc = new Arc[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                arc[id] = new Arc();

                arc[id].setCenterX(x[id]);
                arc[id].setCenterY(y[id]);
                arc[id].setRadiusX(r);
                arc[id].setRadiusY(r);
                arc[id].setStartAngle(random.nextDouble(100));
                arc[id].setLength(random.nextDouble(360));
                arc[id].setType(ArcType.ROUND);
                arc[id].setFill(colors[id % colors.length]);
                node.getChildren().add(arc[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                arc[id].setCenterX(x[id]);
                arc[id].setCenterY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            arc = null;
        }
    }

    static class OpenArcRenderer extends ArcRenderer {
        OpenArcRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                arc[id].setType(ArcType.OPEN);
                arc[id].setFill(null);
                arc[id].setStroke(colors[id % colors.length]);
            }
        }
    }

    static class CubicCurveRenderer extends FlatParticleRenderer {
        CubicCurve[] cubicCurve;

        CubicCurveRenderer(int n, double r) {
            super(n, r);
            cubicCurve = new CubicCurve[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                cubicCurve[id] = new CubicCurve();

                cubicCurve[id].setStartX(0);
                cubicCurve[id].setStartY(50);
                cubicCurve[id].setControlX1(25);
                cubicCurve[id].setControlY1(0);
                cubicCurve[id].setControlX2(75);
                cubicCurve[id].setControlY2(100);
                cubicCurve[id].setEndX(100);
                cubicCurve[id].setEndY(50);
                cubicCurve[id].setFill(colors[id % colors.length]);
                node.getChildren().add(cubicCurve[id]);
            }

        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                cubicCurve[id].setTranslateX(x[id] - r);
                cubicCurve[id].setTranslateY(y[id] - (2 * r));
            }
        }

        public void releaseResource() {
            super.releaseResource();
            cubicCurve = null;
        }
    }

    static class QuadCurveRenderer extends FlatParticleRenderer {
        QuadCurve[] quadCurve;

        QuadCurveRenderer(int n, double r) {
            super(n, r);
            quadCurve = new QuadCurve[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                quadCurve[id] = new QuadCurve();

                quadCurve[id].setStartX(0);
                quadCurve[id].setStartY(50);
                quadCurve[id].setControlX(25);
                quadCurve[id].setControlY(0);
                quadCurve[id].setEndX(100);
                quadCurve[id].setEndY(50);
                quadCurve[id].setFill(colors[id % colors.length]);
                node.getChildren().add(quadCurve[id]);
            }

        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                quadCurve[id].setTranslateX(x[id] - r);
                quadCurve[id].setTranslateY(y[id] - (2 * r));
            }
        }

        public void releaseResource() {
            super.releaseResource();
            quadCurve = null;
        }
    }

    static class CircleRenderer extends FlatParticleRenderer {
        Circle[] circle;

        CircleRenderer(int n, double r) {
            super(n, r);
            circle = new Circle[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                circle[id] = new Circle();

                circle[id].setCenterX(x[id]);
                circle[id].setCenterY(y[id]);
                circle[id].setRadius(r);
                circle[id].setFill(colors[id % colors.length]);
                node.getChildren().add(circle[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                circle[id].setCenterX(x[id]);
                circle[id].setCenterY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            circle = null;
        }
    }

    static class CircleRendererRH extends CircleRenderer {

        CircleRendererRH(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                circle[id].setSmooth(false);
            }
        }
    }

    static class CircleRendererBlendMultiply extends CircleRenderer {

        CircleRendererBlendMultiply(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id += 10) {
                circle[id].setBlendMode(BlendMode.MULTIPLY);
            }
        }
    }

    static class CircleRendererBlendAdd extends CircleRenderer {

        CircleRendererBlendAdd(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id += 10) {
                circle[id].setBlendMode(BlendMode.ADD);
            }
        }
    }

    static class CircleRendererBlendDarken extends CircleRenderer {

        CircleRendererBlendDarken(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id += 10) {
                circle[id].setBlendMode(BlendMode.DARKEN);
            }
        }
    }

    static class StrokedCircleRenderer extends CircleRenderer {

        StrokedCircleRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                circle[id].setFill(null);
                circle[id].setStroke(colors[id % colors.length]);
            }
        }
    }

    static class LinGradCircleRenderer extends CircleRenderer {
        Stop[] stops;
        LinearGradient linGradient;

        LinGradCircleRenderer(int n, double r) {
            super(n, r);
            stops = new Stop[] { new Stop(0, Color.BLACK), new Stop(1, Color.RED)};
            linGradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                circle[id].setFill(linGradient);
            }
        }
    }

    static class RadGradCircleRenderer extends CircleRenderer {
        Stop[] stops;
        RadialGradient radGradient;

        RadGradCircleRenderer(int n, double r) {
            super(n, r);
            stops = new Stop[] { new Stop(0.0, Color.WHITE), new Stop(0.1, Color.RED), new Stop(1.0, Color.DARKRED)};
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                radGradient = new RadialGradient(0, 0, x[id], y[id], 60, false, CycleMethod.NO_CYCLE, stops);
                circle[id].setFill(radGradient);
            }
        }
    }

    static class EllipseRenderer extends FlatParticleRenderer {
        Ellipse[] ellipse;

        EllipseRenderer(int n, double r) {
            super(n, r);
            ellipse = new Ellipse[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                ellipse[id] = new Ellipse();

                ellipse[id].setCenterX(x[id]);
                ellipse[id].setCenterY(y[id]);
                ellipse[id].setRadiusX(2 * r);
                ellipse[id].setRadiusY(r);
                ellipse[id].setFill(colors[id % colors.length]);
                node.getChildren().add(ellipse[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                ellipse[id].setCenterX(x[id]);
                ellipse[id].setCenterY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            ellipse = null;
        }
    }

    static class LineRenderer extends FlatParticleRenderer {
        Line[] line;

        LineRenderer(int n, double r) {
            super(n, r);
            line = new Line[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                line[id] = new Line();

                line[id].setStartX(0);
                line[id].setStartY(0);
                line[id].setEndX(50);
                line[id].setEndY(50);
                line[id].setStroke(colors[id % colors.length]);
                node.getChildren().add(line[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                line[id].setTranslateX(x[id] - r);
                line[id].setTranslateY(y[id] - r);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            line = null;
        }
    }

    static class PathRenderer extends FlatParticleRenderer {
        Path[] path;

        PathRenderer(int n, double r) {
            super(n, r);
            path = new Path[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                MoveTo moveTo = new MoveTo();
                moveTo.setX(0);
                moveTo.setY(0);

                CubicCurveTo cubicCurveTo = new CubicCurveTo();
                cubicCurveTo.setX(40);
                cubicCurveTo.setY(45);
                cubicCurveTo.setControlX1(0);
                cubicCurveTo.setControlY1(0);
                cubicCurveTo.setControlX2(30);
                cubicCurveTo.setControlY2(80);

                QuadCurveTo quadCurveTo = new QuadCurveTo();
                quadCurveTo.setX(60);
                quadCurveTo.setY(45);
                quadCurveTo.setControlX(50);
                quadCurveTo.setControlY(0);

                ArcTo arcTo = new ArcTo();
                arcTo.setX(80);
                arcTo.setY(45);
                arcTo.setRadiusX(20);
                arcTo.setRadiusY(40);
                arcTo.setLargeArcFlag(true);
                arcTo.setSweepFlag(true);

                path[id] = new Path();
                path[id].setStroke(colors[id % colors.length]);
                path[id].getElements().addAll(moveTo, cubicCurveTo, quadCurveTo, arcTo);

                node.getChildren().add(path[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                path[id].setTranslateX(x[id] - r);
                path[id].setTranslateY(y[id] - r);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            path = null;
        }
    }

    static class RectangleRenderer extends FlatParticleRenderer {
        Rectangle[] rectangle;

        RectangleRenderer(int n, double r) {
            super(n, r);
            rectangle = new Rectangle[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                rectangle[id] = new Rectangle();

                rectangle[id].setX(x[id] - r);
                rectangle[id].setY(y[id] - r);
                rectangle[id].setWidth(2 * r);
                rectangle[id].setHeight(2 * r);
                rectangle[id].setFill(colors[id % colors.length]);
                node.getChildren().add(rectangle[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                rectangle[id].setX(x[id] - r);
                rectangle[id].setY(y[id] - r);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            rectangle = null;
        }
    }

    static class RectangleRendererRH extends RectangleRenderer {

        RectangleRendererRH(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                rectangle[id].setSmooth(false);
                rectangle[id].setRotate(45);
            }
        }
    }

    static class StrokedRectangleRenderer extends RectangleRenderer {

        StrokedRectangleRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                rectangle[id].setFill(null);
                rectangle[id].setStroke(colors[id % colors.length]);
            }
        }
    }

    static class PolygonRenderer extends FlatParticleRenderer {
        Polygon[] polygon;

        PolygonRenderer(int n, double r) {
            super(n, r);
            polygon = new Polygon[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                polygon[id] = new Polygon();

                polygon[id].getPoints().addAll(new Double[]{
                    0.0, 20.0,
                    20.0, 0.0,
                    40.0, 20.0,
                    40.0, 40.0,
                    20.0, 60.0,
                    0.0, 40.0
                });
                polygon[id].setFill(colors[id % colors.length]);
                node.getChildren().add(polygon[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                polygon[id].setTranslateX(x[id] - r);
                polygon[id].setTranslateY(y[id] - r);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            polygon = null;
        }
    }

    static class StrokedPolygonRenderer extends PolygonRenderer {

        StrokedPolygonRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                polygon[id].setFill(null);
                polygon[id].setStroke(colors[id % colors.length]);
            }
        }
    }

    static class Box3DRenderer extends FlatParticleRenderer {
        Box[] box;

        Box3DRenderer(int n, double r) {
            super(n, r);
            box = new Box[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                box[id] = new Box(2 * r, 2 * r, 2 * r);

                box[id].setTranslateX(x[id]);
                box[id].setTranslateY(y[id]);

                PhongMaterial material = new PhongMaterial();
                material.setDiffuseColor(colors[id % colors.length]);
                box[id].setMaterial(material);

                box[id].setRotationAxis(new Point3D(1, 1, 1));
                box[id].setRotate(45);
                node.getChildren().add(box[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                box[id].setTranslateX(x[id]);
                box[id].setTranslateY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            box = null;
        }
    }

    static class CylinderRenderer extends FlatParticleRenderer {
        Cylinder[] cylinder;

        CylinderRenderer(int n, double r) {
            super(n, r);
            cylinder = new Cylinder[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                cylinder[id] = new Cylinder(r, 2 * r);

                PhongMaterial material = new PhongMaterial();
                material.setDiffuseColor(colors[id % colors.length]);
                cylinder[id].setMaterial(material);

                cylinder[id].setRotationAxis(new Point3D(1, 1, 1));
                cylinder[id].setRotate(45);
                node.getChildren().add(cylinder[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                cylinder[id].setTranslateX(x[id]);
                cylinder[id].setTranslateY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            cylinder = null;
        }
    }

    static class SphereRenderer extends FlatParticleRenderer {
        Sphere[] sphere;

        SphereRenderer(int n, double r) {
            super(n, r);
            sphere = new Sphere[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                sphere[id] = new Sphere(r);

                PhongMaterial material = new PhongMaterial();
                material.setDiffuseColor(colors[id % colors.length]);
                sphere[id].setMaterial(material);

                node.getChildren().add(sphere[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                sphere[id].setTranslateX(x[id]);
                sphere[id].setTranslateY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            sphere = null;
        }
    }

    static class MeshRenderer extends FlatParticleRenderer {
        MeshView[] meshView;

        final static float minX = -10;
        final static float minY = -10;
        final static float maxX = 10;
        final static float maxY = 10;
        final int pointSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 6;
        final int scale = 3;

        MeshRenderer(int n, double r) {
            super(n, r);
            meshView = new MeshView[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int subDivX = 2;
            int subDivY = 2;
            int numDivX = subDivX + 1;
            int numVerts = (subDivY + 1) * numDivX;
            float points[] = new float[numVerts * pointSize];
            float texCoords[] = new float[numVerts * texCoordSize];
            int faceCount = subDivX * subDivY * 2;
            int faces[] = new int[ faceCount * faceSize];

            // Create points and texture coordinates
            for (int i = 0; i <= subDivY; i++) {
                float dy = (float) i / subDivY;
                double fy = (1 - dy) * minY + dy * maxY;
                for (int j = 0; j <= subDivX; j++) {
                    float dx = (float) j / subDivX;
                    double fx = (1 - dx) * minX + dx * maxX;
                    int index = i * numDivX * pointSize + (j * pointSize);
                    points[index] = (float) fx * scale;
                    points[index + 1] = (float) fy * scale;
                    points[index + 2] = (float) getSinDivX(fx, fy) * scale;
                    index = i * numDivX * texCoordSize + (j * texCoordSize);
                    texCoords[index] = dx;
                    texCoords[index + 1] = dy;
                }
            }

            // Create faces
            for (int i = 0; i < subDivY; i++) {
                for (int j = 0; j < subDivX; j++) {
                    int p00 = i * numDivX + j;
                    int p01 = p00 + 1;
                    int p10 = p00 + numDivX;
                    int p11 = p10 + 1;
                    int tc00 = i * numDivX + j;
                    int tc01 = tc00 + 1;
                    int tc10 = tc00 + numDivX;
                    int tc11 = tc10 + 1;

                    int index = (i * subDivX * faceSize + (j * faceSize)) * 2;
                    faces[index + 0] = p00;
                    faces[index + 1] = tc00;
                    faces[index + 2] = p10;
                    faces[index + 3] = tc10;
                    faces[index + 4] = p11;
                    faces[index + 5] = tc11;
                    index += faceSize;
                    faces[index + 0] = p11;
                    faces[index + 1] = tc11;
                    faces[index + 2] = p01;
                    faces[index + 3] = tc01;
                    faces[index + 4] = p00;
                    faces[index + 5] = tc00;
                }
            }

            TriangleMesh triangleMesh = new TriangleMesh();
            triangleMesh.getPoints().setAll(points);
            triangleMesh.getTexCoords().setAll(texCoords);
            triangleMesh.getFaces().setAll(faces);

            for (int id = 0; id < n; id++) {
                PhongMaterial material = new PhongMaterial();
                material.setDiffuseColor(colors[id % colors.length]);
                material.setSpecularColor(colors[id % colors.length]);
                String url = RenderPerfTest.class.getResource("duke.png").toString();
                material.setDiffuseMap(new Image(url));

                meshView[id] = new MeshView(triangleMesh);
                meshView[id].setMaterial(material);
                meshView[id].setDrawMode(DrawMode.FILL);
                meshView[id].setCullFace(CullFace.BACK);

                node.getChildren().add(meshView[id]);
            }
        }

        private double getSinDivX(double x, double y) {
            float funcValue = -30.0f;
            double r = Math.sqrt(x*x + y*y);
            return funcValue * (r == 0 ? 1 : Math.sin(r) / r);
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                meshView[id].setTranslateX(x[id]);
                meshView[id].setTranslateY(y[id]);
            }
        }

        public void releaseResource() {
            super.releaseResource();
            meshView = null;
        }
    }

    static class WhiteTextRenderer implements ParticleRenderer {
        int n;
        double r;
        Text[] text;

        WhiteTextRenderer(int n, double r) {
            this.n = n;
            this.r = r;
            text = new Text[n];
        }

        void setPaint(Text t, int id) {
            t.setFill(Color.WHITE);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                text[id] = new Text(x[id], y[id], "The quick brown fox jumps over the lazy dog");
                setPaint(text[id], id);
                node.getChildren().add(text[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                text[id].setX(x[id]);
                text[id].setY(y[id]);
            }
        }

        public void releaseResource() {
            text = null;
        }

        public int getObjectCount() {
            return n;
        }

        public double getParticleRadius() {
            return r;
        }
    }

    static class ColorTextRenderer extends WhiteTextRenderer {
        Color[] colors;

        ColorTextRenderer(int n, double r) {
            super(n, r);
            colors = new Color[n];

            for (int i = 0; i < n; i++) {
                colors[i] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            }
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                text[id].setFill(colors[id % colors.length]);
            }
        }
    }

    static class LargeTextRenderer extends WhiteTextRenderer {
        Font font;

        LargeTextRenderer(int n, double r) {
            super(n, r);
            font = new Font(48);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                text[id].setFont(font);
            }
        }
    }

    static class LargeColorTextRenderer extends ColorTextRenderer {
        Font font;

        LargeColorTextRenderer(int n, double r) {
            super(n, r);
            font = new Font(48);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                text[id].setFont(font);
            }
        }
    }

    static class ImageRenderer implements ParticleRenderer {
        ImageView[] dukeImg;
        Image image;
        int n;
        double r;

        ImageRenderer(int n, double r) {
            this.n = n;
            this.r = r;
            try {
                String url = RenderPerfTest.class.getResource("duke.png").toString();
                image = new Image(url);
                dukeImg = new ImageView[n];
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                dukeImg[id] = new ImageView();
                dukeImg[id].setImage(image);
                dukeImg[id].setX(x[id] - r);
                dukeImg[id].setY(y[id] - 2 * r);

                node.getChildren().add(dukeImg[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                dukeImg[id].setX(x[id] - r);
                dukeImg[id].setY(y[id] - 2 * r);
            }
        }

        public void releaseResource() {
            image = null;
            dukeImg = null;
        }

        public int getObjectCount() {
            return n;
        }

        public double getParticleRadius() {
            return r;
        }
    }

    static class ImageRendererRH extends ImageRenderer {

        ImageRendererRH(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            super.addComponents(node, n, x, y, vx, vy);
            for (int id = 0; id < n; id++) {
                dukeImg[id].setSmooth(false);
            }
        }
    }

    static class MultiShapeRendererInterleaved extends FlatParticleRenderer {
        Circle[] circle;
        Rectangle[] rectangle;
        Arc[] arc;
        Ellipse[] ellipse;

        MultiShapeRendererInterleaved(int n, double r) {
            super((n = roundUpTo4(n)), r);
            circle = new Circle[n / 4];
            rectangle = new Rectangle[n / 4];
            arc = new Arc[n / 4];
            ellipse = new Ellipse[n / 4];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;
             for (int id = 0; id < n / 4; id++) {
                circle[id] = new Circle();

                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                circle[id].setRadius(r);
                circle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(circle[id]);
                index++;

                rectangle[id] = new Rectangle();

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                rectangle[id].setWidth(2 * r);
                rectangle[id].setHeight(2 * r);
                rectangle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(rectangle[id]);
                index++;

                arc[id] = new Arc();

                arc[id].setCenterX(x[index]);
                arc[id].setCenterY(y[index]);
                arc[id].setRadiusX(r);
                arc[id].setRadiusY(r);
                arc[id].setStartAngle(random.nextDouble(100));
                arc[id].setLength(random.nextDouble(360));
                arc[id].setType(ArcType.ROUND);
                arc[id].setFill(colors[index % colors.length]);
                node.getChildren().add(arc[id]);
                index++;

                ellipse[id] = new Ellipse();

                ellipse[id].setCenterX(x[index]);
                ellipse[id].setCenterY(y[index]);
                ellipse[id].setRadiusX(2 * r);
                ellipse[id].setRadiusY(r);
                ellipse[id].setFill(colors[index % colors.length]);
                node.getChildren().add(ellipse[id]);
                index++;
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                index++;

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                index++;

                arc[id].setCenterX(x[index]);
                arc[id].setCenterY(y[index]);
                index++;

                ellipse[id].setCenterX(x[index]);
                ellipse[id].setCenterY(y[index]);
                index++;
            }
        }

        public void releaseResource() {
            circle = null;
            rectangle = null;
            arc = null;
            ellipse = null;
        }
    }

    static class MultiShapeRenderer extends MultiShapeRendererInterleaved {

        MultiShapeRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;
             for (int id = 0; id < n / 4; id++) {
                circle[id] = new Circle();

                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                circle[id].setRadius(r);
                circle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(circle[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {

                rectangle[id] = new Rectangle();

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                rectangle[id].setWidth(2 * r);
                rectangle[id].setHeight(2 * r);
                rectangle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(rectangle[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {

                arc[id] = new Arc();

                arc[id].setCenterX(x[index]);
                arc[id].setCenterY(y[index]);
                arc[id].setRadiusX(r);
                arc[id].setRadiusY(r);
                arc[id].setStartAngle(random.nextDouble(100));
                arc[id].setLength(random.nextDouble(360));
                arc[id].setType(ArcType.ROUND);
                arc[id].setFill(colors[index % colors.length]);
                node.getChildren().add(arc[id]);
                index++;
            }
            for (int id = 0; id < n / 4; id++) {

                ellipse[id] = new Ellipse();

                ellipse[id].setCenterX(x[index]);
                ellipse[id].setCenterY(y[index]);
                ellipse[id].setRadiusX(2 * r);
                ellipse[id].setRadiusY(r);
                ellipse[id].setFill(colors[index % colors.length]);
                node.getChildren().add(ellipse[id]);
                index++;
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                index++;
            }
            for (int id = 0; id < n / 4; id++) {
                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                arc[id].setCenterX(x[index]);
                arc[id].setCenterY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                ellipse[id].setCenterX(x[index]);
                ellipse[id].setCenterY(y[index]);
                index++;
            }
        }
    }

    static class MultiShape2D3DRendererInterleaved extends FlatParticleRenderer {
        Circle[] circle;
        Sphere[] sphere;
        Rectangle[] rectangle;
        Box[] box;

        MultiShape2D3DRendererInterleaved(int n, double r) {
            super((n = roundUpTo4(n)), r);
            circle = new Circle[n / 4];
            sphere = new Sphere[n / 4];
            rectangle = new Rectangle[n / 4];
            box = new Box[n / 4];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;
             for (int id = 0; id < n / 4; id++) {
                circle[id] = new Circle();

                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                circle[id].setRadius(r);
                circle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(circle[id]);
                index++;

                sphere[id] = new Sphere(r);

                PhongMaterial materialSphere = new PhongMaterial();
                materialSphere.setDiffuseColor(colors[index % colors.length]);
                sphere[id].setMaterial(materialSphere);
                node.getChildren().add(sphere[id]);
                index++;

                rectangle[id] = new Rectangle();

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                rectangle[id].setWidth(2 * r);
                rectangle[id].setHeight(2 * r);
                rectangle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(rectangle[id]);
                index++;

                box[id] = new Box(2 * r, 2 * r, 2 * r);

                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);

                PhongMaterial materialBox = new PhongMaterial();
                materialBox.setDiffuseColor(colors[index % colors.length]);
                box[id].setMaterial(materialBox);

                box[id].setRotationAxis(new Point3D(1, 1, 1));
                box[id].setRotate(45);
                node.getChildren().add(box[id]);
                index++;
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                index++;

                sphere[id].setTranslateX(x[index]);
                sphere[id].setTranslateY(y[index]);
                index++;

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                index++;

                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);
                index++;
            }
        }

        public void releaseResource() {
            circle = null;
            sphere = null;
            rectangle = null;
            box = null;
        }
    }

    static class MultiShape2D3DRenderer extends MultiShape2D3DRendererInterleaved {
        MultiShape2D3DRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;
            for (int id = 0; id < n / 4; id++) {
                circle[id] = new Circle();

                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                circle[id].setRadius(r);
                circle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(circle[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                sphere[id] = new Sphere(r);

                PhongMaterial materialSphere = new PhongMaterial();
                materialSphere.setDiffuseColor(colors[index % colors.length]);
                sphere[id].setMaterial(materialSphere);
                node.getChildren().add(sphere[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {

                rectangle[id] = new Rectangle();

                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                rectangle[id].setWidth(2 * r);
                rectangle[id].setHeight(2 * r);
                rectangle[id].setFill(colors[index % colors.length]);
                node.getChildren().add(rectangle[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {

                box[id] = new Box(2 * r, 2 * r, 2 * r);

                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);

                PhongMaterial materialBox = new PhongMaterial();
                materialBox.setDiffuseColor(colors[index % colors.length]);
                box[id].setMaterial(materialBox);

                box[id].setRotationAxis(new Point3D(1, 1, 1));
                box[id].setRotate(45);
                node.getChildren().add(box[id]);
                index++;
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                circle[id].setCenterX(x[index]);
                circle[id].setCenterY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                sphere[id].setTranslateX(x[index]);
                sphere[id].setTranslateY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                rectangle[id].setX(x[index] - r);
                rectangle[id].setY(y[index] - r);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);
                index++;
            }
        }
    }

    static class MultiShape3DRendererInterleaved extends FlatParticleRenderer {
        Sphere[] sphere;
        Box[] box;
        Cylinder[] cylinder;
        MeshView[] meshView;

        final static float minX = -10;
        final static float minY = -10;
        final static float maxX = 10;
        final static float maxY = 10;
        final int pointSize = 3;
        final int texCoordSize = 2;
        final int faceSize = 6;
        final int scale = 3;


        MultiShape3DRendererInterleaved(int n, double r) {
            super((n = roundUpTo4(n)), r);
            sphere = new Sphere[n / 4];
            box = new Box[n / 4];
            cylinder = new Cylinder[n / 4];
            meshView = new MeshView[n / 4];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;

            int subDivX = 2;
            int subDivY = 2;
            int numDivX = subDivX + 1;
            int numVerts = (subDivY + 1) * numDivX;
            float points[] = new float[numVerts * pointSize];
            float texCoords[] = new float[numVerts * texCoordSize];
            int faceCount = subDivX * subDivY * 2;
            int faces[] = new int[ faceCount * faceSize];

            // Create points and texture coordinates
            for (int i = 0; i <= subDivY; i++) {
                float dy = (float) i / subDivY;
                double fy = (1 - dy) * minY + dy * maxY;
                for (int j = 0; j <= subDivX; j++) {
                    float dx = (float) j / subDivX;
                    double fx = (1 - dx) * minX + dx * maxX;
                    int idx = i * numDivX * pointSize + (j * pointSize);
                    points[idx] = (float) fx * scale;
                    points[idx + 1] = (float) fy * scale;
                    points[idx + 2] = (float) getSinDivX(fx, fy) * scale;
                    idx = i * numDivX * texCoordSize + (j * texCoordSize);
                    texCoords[idx] = dx;
                    texCoords[idx + 1] = dy;
                }
            }

            // Create faces
            for (int i = 0; i < subDivY; i++) {
                for (int j = 0; j < subDivX; j++) {
                    int p00 = i * numDivX + j;
                    int p01 = p00 + 1;
                    int p10 = p00 + numDivX;
                    int p11 = p10 + 1;
                    int tc00 = i * numDivX + j;
                    int tc01 = tc00 + 1;
                    int tc10 = tc00 + numDivX;
                    int tc11 = tc10 + 1;

                    int idx = (i * subDivX * faceSize + (j * faceSize)) * 2;
                    faces[idx + 0] = p00;
                    faces[idx + 1] = tc00;
                    faces[idx + 2] = p10;
                    faces[idx + 3] = tc10;
                    faces[idx + 4] = p11;
                    faces[idx + 5] = tc11;
                    idx += faceSize;
                    faces[idx + 0] = p11;
                    faces[idx + 1] = tc11;
                    faces[idx + 2] = p01;
                    faces[idx + 3] = tc01;
                    faces[idx + 4] = p00;
                    faces[idx + 5] = tc00;
                }
            }

            TriangleMesh triangleMesh = new TriangleMesh();
            triangleMesh.getPoints().setAll(points);
            triangleMesh.getTexCoords().setAll(texCoords);
            triangleMesh.getFaces().setAll(faces);

            for (int id = 0; id < n / 4; id++) {
                sphere[id] = new Sphere(r);

                PhongMaterial materialSphere = new PhongMaterial();
                materialSphere.setDiffuseColor(colors[index % colors.length]);
                sphere[id].setMaterial(materialSphere);
                node.getChildren().add(sphere[id]);
                index++;

                box[id] = new Box(2 * r, 2 * r, 2 * r);

                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);

                PhongMaterial materialBox = new PhongMaterial();
                materialBox.setDiffuseColor(colors[index % colors.length]);
                box[id].setMaterial(materialBox);

                box[id].setRotationAxis(new Point3D(1, 1, 1));
                box[id].setRotate(45);
                node.getChildren().add(box[id]);
                index++;


                cylinder[id] = new Cylinder(r, 2 * r);

                PhongMaterial materialCylinder = new PhongMaterial();
                materialCylinder.setDiffuseColor(colors[id % colors.length]);
                cylinder[id].setMaterial(materialCylinder);

                cylinder[id].setRotationAxis(new Point3D(1, 1, 1));
                cylinder[id].setRotate(45);
                node.getChildren().add(cylinder[id]);
                index++;

                PhongMaterial materialMesh = new PhongMaterial();
                materialMesh.setDiffuseColor(colors[id % colors.length]);
                materialMesh.setSpecularColor(colors[id % colors.length]);
                String url = RenderPerfTest.class.getResource("duke.png").toString();
                materialMesh.setDiffuseMap(new Image(url));

                meshView[id] = new MeshView(triangleMesh);
                meshView[id].setMaterial(materialMesh);
                meshView[id].setDrawMode(DrawMode.FILL);
                meshView[id].setCullFace(CullFace.BACK);
                node.getChildren().add(meshView[id]);
                index++;
            }
        }

        private double getSinDivX(double x, double y) {
            float funcValue = -30.0f;
            double r = Math.sqrt(x*x + y*y);
            return funcValue * (r == 0 ? 1 : Math.sin(r) / r);
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                sphere[id].setTranslateX(x[index]);
                sphere[id].setTranslateY(y[index]);
                index++;

                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);
                index++;

                cylinder[id].setTranslateX(x[index]);
                cylinder[id].setTranslateY(y[index]);
                index++;

                meshView[id].setTranslateX(x[index]);
                meshView[id].setTranslateY(y[index]);
                index++;
            }
        }

        public void releaseResource() {
            sphere = null;
            box = null;
            cylinder = null;
            meshView = null;
        }
    }

    static class MultiShape3DRenderer extends MultiShape3DRendererInterleaved {
        MultiShape3DRenderer(int n, double r) {
            super(n, r);
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index =0;
            for (int id = 0; id < n / 4; id++) {
                sphere[id] = new Sphere(r);

                PhongMaterial materialSphere = new PhongMaterial();
                materialSphere.setDiffuseColor(colors[index % colors.length]);
                sphere[id].setMaterial(materialSphere);
                node.getChildren().add(sphere[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                box[id] = new Box(2 * r, 2 * r, 2 * r);
                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);

                PhongMaterial materialBox = new PhongMaterial();
                materialBox.setDiffuseColor(colors[index % colors.length]);
                box[id].setMaterial(materialBox);

                box[id].setRotationAxis(new Point3D(1, 1, 1));
                box[id].setRotate(45);
                node.getChildren().add(box[id]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                cylinder[id] = new Cylinder(r, 2 * r);

                PhongMaterial materialCylinder = new PhongMaterial();
                materialCylinder.setDiffuseColor(colors[id % colors.length]);
                cylinder[id].setMaterial(materialCylinder);

                cylinder[id].setRotationAxis(new Point3D(1, 1, 1));
                cylinder[id].setRotate(45);
                node.getChildren().add(cylinder[id]);
                index++;
            }

            int subDivX = 2;
            int subDivY = 2;
            int numDivX = subDivX + 1;
            int numVerts = (subDivY + 1) * numDivX;
            float points[] = new float[numVerts * pointSize];
            float texCoords[] = new float[numVerts * texCoordSize];
            int faceCount = subDivX * subDivY * 2;
            int faces[] = new int[ faceCount * faceSize];

            // Create points and texture coordinates
            for (int i = 0; i <= subDivY; i++) {
                float dy = (float) i / subDivY;
                double fy = (1 - dy) * minY + dy * maxY;
                for (int j = 0; j <= subDivX; j++) {
                    float dx = (float) j / subDivX;
                    double fx = (1 - dx) * minX + dx * maxX;
                    int idx = i * numDivX * pointSize + (j * pointSize);
                    points[idx] = (float) fx * scale;
                    points[idx + 1] = (float) fy * scale;
                    points[idx + 2] = (float) getSinDivX(fx, fy) * scale;
                    idx = i * numDivX * texCoordSize + (j * texCoordSize);
                    texCoords[idx] = dx;
                    texCoords[idx + 1] = dy;
                }
            }

            // Create faces
            for (int i = 0; i < subDivY; i++) {
                for (int j = 0; j < subDivX; j++) {
                    int p00 = i * numDivX + j;
                    int p01 = p00 + 1;
                    int p10 = p00 + numDivX;
                    int p11 = p10 + 1;
                    int tc00 = i * numDivX + j;
                    int tc01 = tc00 + 1;
                    int tc10 = tc00 + numDivX;
                    int tc11 = tc10 + 1;

                    int idx = (i * subDivX * faceSize + (j * faceSize)) * 2;
                    faces[idx + 0] = p00;
                    faces[idx + 1] = tc00;
                    faces[idx + 2] = p10;
                    faces[idx + 3] = tc10;
                    faces[idx + 4] = p11;
                    faces[idx + 5] = tc11;
                    idx += faceSize;
                    faces[idx + 0] = p11;
                    faces[idx + 1] = tc11;
                    faces[idx + 2] = p01;
                    faces[idx + 3] = tc01;
                    faces[idx + 4] = p00;
                    faces[idx + 5] = tc00;
                }
            }

            TriangleMesh triangleMesh = new TriangleMesh();
            triangleMesh.getPoints().setAll(points);
            triangleMesh.getTexCoords().setAll(texCoords);
            triangleMesh.getFaces().setAll(faces);

            for (int id = 0; id < n / 4; id++) {
                PhongMaterial materialMesh = new PhongMaterial();
                materialMesh.setDiffuseColor(colors[id % colors.length]);
                materialMesh.setSpecularColor(colors[id % colors.length]);
                String url = RenderPerfTest.class.getResource("duke.png").toString();
                materialMesh.setDiffuseMap(new Image(url));

                meshView[id] = new MeshView(triangleMesh);
                meshView[id].setMaterial(materialMesh);
                meshView[id].setDrawMode(DrawMode.FILL);
                meshView[id].setCullFace(CullFace.BACK);
                node.getChildren().add(meshView[id]);
                index++;
            }
        }

        private double getSinDivX(double x, double y) {
            float funcValue = -30.0f;
            double r = Math.sqrt(x*x + y*y);
            return funcValue * (r == 0 ? 1 : Math.sin(r) / r);
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            int index = 0;
            for (int id = 0; id < n / 4; id++) {
                sphere[id].setTranslateX(x[index]);
                sphere[id].setTranslateY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                box[id].setTranslateX(x[index]);
                box[id].setTranslateY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                cylinder[id].setTranslateX(x[index]);
                cylinder[id].setTranslateY(y[index]);
                index++;
            }

            for (int id = 0; id < n / 4; id++) {
                meshView[id].setTranslateX(x[index]);
                meshView[id].setTranslateY(y[index]);
                index++;
            }
        }
    }

    static class ButtonRenderer implements ParticleRenderer {
        int n;
        double r;
        Button[] button;

        ButtonRenderer(int n, double r) {
            this.n = n;
            this.r = r;
            button = new Button[n];
        }

        @Override
        public void addComponents(Group node, int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                button[id] = new Button();
                button[id].setText(String.valueOf(id));
                button[id].setLayoutX(x[id]);
                button[id].setLayoutY(y[id]);
                node.getChildren().add(button[id]);
            }
        }

        public void updateComponentCoordinates(int n, double[] x, double[] y, double[] vx, double[] vy) {
            for (int id = 0; id < n; id++) {
                button[id].setLayoutX(x[id]);
                button[id].setLayoutY(y[id]);
            }
        }

        public void releaseResource() {
            button = null;
        }

        public int getObjectCount() {
            return n;
        }

        public double getParticleRadius() {
            return r;
        }
    }

    /**
     * {@link PerfMeter} is the class which runs each test.
     * This uses the JavaFX applcation environment created and invokes {@link ParticleRenderable}
     * methods to render component and animate. The values details required to calculate
     * FPS is also tracked in this class.
     */
    static class PerfMeter {
        private String name;

        private int frames = 0;
        private long testTimeSeconds = DEFAULT_TEST_TIME_SECONDS;
        private long testTimeNanos = testTimeSeconds * SECOND_IN_NANOS;
        AnimationTimer frameRateMeter;

        long startTime = 0;
        long lastTickTime = 0;
        boolean warmUp = true;
        boolean completed = false;
        boolean stopped = false;

        PerfMeter(String name) {
            this(name, DEFAULT_TEST_TIME_SECONDS);
        }

        PerfMeter(String name, long testTimeSeconds) {
            this.name = name;
            this.testTimeSeconds = testTimeSeconds;
            this.testTimeNanos = this.testTimeSeconds * SECOND_IN_NANOS;
        }

        /**
         * The method which invokes {@link ParticleRenderable} methods for rendering and
         * moving the components on the JavaFX application. Same JavaFX application
         * environment is used for all the test cases.
         * This method warms up the test environment and then runs animation to calculate FPS.
         * The overridden {@link AnimationTimer#handle} method, gets invoked for each frame
         * which keeps track of the number of frames rendered, duration of the test case
         * to calculate FPS value.
         *
         * @params  renderable
         *          object of {@link Renderable}
         */
        void exec(final Renderable renderable) throws Exception {
            final CountDownLatch startupLatch = new CountDownLatch(1);
            final CountDownLatch stopLatch = new CountDownLatch(1);
            final CountDownLatch stageHiddenLatch = new CountDownLatch(1);

            Platform.runLater(() -> {
                group = new Group();
                renderable.addComponents(group);
                scene = new Scene(group, WIDTH, HEIGHT);
                scene.setFill(Color.BLACK);

                stage.setScene(scene);
                stage.setAlwaysOnTop(true);
                stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
                stage.setOnHidden(event -> Platform.runLater(stageHiddenLatch::countDown));
                stage.setOnCloseRequest(event -> {
                    Platform.runLater(stopLatch::countDown);
                    stopped = true;
                });

                stage.show();

                frameRateMeter = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (startTime == 0) {
                            startTime = now;
                        }

                        lastTickTime = now;

                        if (warmUp && (now >= startTime + WARMUP_TIME)) {
                            startTime = now;
                            frames = 0;
                            warmUp = false;
                        }

                        if (!stopped) {
                            moveComponents(renderable);
                        }

                        if (!warmUp) {
                            frames++;
                            if (testTimeSeconds > 0 && now >= startTime + testTimeNanos) {
                                completed = true;
                                stopLatch.countDown();
                            }
                        }
                    }
                };
            });

            if (!startupLatch.await(20, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout waiting for stage to load.");
            }
            Platform.runLater(() -> frameRateMeter.start());

            if (testTimeSeconds > 0) {
                // timed run, which means we can also timeout if something goes wrong
                if (!stopLatch.await(testTimeSeconds + 10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timeout waiting for test execution completion.\n" + name + ": Test workload could be too high. Try running the test with lesser number of objects.");
                }
            } else {
                // infinite run, await until the stage is closed
                stopLatch.await();
            }
            Platform.runLater(() -> frameRateMeter.stop());

            reportFPS();
            Platform.runLater(() -> {
                renderable.releaseResource();
                stage.hide();
            });

            if (!stageHiddenLatch.await(20, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout waiting for stage to get hidden.");
            }
            startTime = 0;
            warmUp = true;
        }

        void moveComponents(final Renderable renderable){
            Platform.runLater(() -> {
                renderable.updateCoordinates();
                renderable.updateComponentCoordinates();
            });
        }

        void reportFPS() {
            if (warmUp) {
                System.out.println(String.format("Test %s stopped before warm-up was completed. Results not valid.", name));
            } else {
                long totalTestTime = testTimeNanos;
                if (totalTestTime == 0 || !completed) {
                    // infinite run, we have to get the time from stop-start delta
                    totalTestTime = (lastTickTime - startTime);
                }
                double totalTestTimeSeconds = (double)totalTestTime / SECOND_IN_NANOS;
                double frameRate = frames / totalTestTimeSeconds;
                System.out.println(String.format("%s (Objects Frames Time FPS): %d, %d, %.2f, %.2f", name, objectCount, frames, totalTestTimeSeconds, frameRate));
            }
        }
    }



    public void testArc() throws Exception {
        (new PerfMeter("Arc", testDuration)).exec(createPR(new ArcRenderer(objectCount, R)));
    }

    public void testOpenArc() throws Exception {
        (new PerfMeter("OpenArc", testDuration)).exec(createPR(new OpenArcRenderer(objectCount, R)));
    }

    public void testCubicCurve() throws Exception {
        (new PerfMeter("CubicCurve", testDuration)).exec(createPR(new CubicCurveRenderer(objectCount, R)));
    }

    public void testQuadCurve() throws Exception {
        (new PerfMeter("QuadCurve", testDuration)).exec(createPR(new QuadCurveRenderer(objectCount, R)));
    }

    public void testCircle() throws Exception {
        (new PerfMeter("Circle", testDuration)).exec(createPR(new CircleRenderer(objectCount, R)));
    }

    public void testCircleRH() throws Exception {
        (new PerfMeter("CircleRH", testDuration)).exec(createPR(new CircleRendererRH(objectCount, R)));
    }

    public void testCircleBlendMultiply() throws Exception {
        (new PerfMeter("CircleBlendMultiply", testDuration)).exec(createPR(new CircleRendererBlendMultiply(objectCount, R)));
    }

    public void testCircleBlendAdd() throws Exception {
        (new PerfMeter("CircleBlendAdd", testDuration)).exec(createPR(new CircleRendererBlendAdd(objectCount, R)));
    }

    public void testCircleBlendDarken() throws Exception {
        (new PerfMeter("CircleBlendDarken", testDuration)).exec(createPR(new CircleRendererBlendDarken(objectCount, R)));
    }

    public void testStrokedCircle() throws Exception {
        (new PerfMeter("StrokedCircle", testDuration)).exec(createPR(new StrokedCircleRenderer(objectCount, R)));
    }

    public void testLinGradCircle() throws Exception {
        (new PerfMeter("LinGradCircle", testDuration)).exec(createPR(new LinGradCircleRenderer(objectCount, R)));
    }

    public void testRadGradCircle() throws Exception {
        (new PerfMeter("RadGradCircle", testDuration)).exec(createPR(new RadGradCircleRenderer(objectCount, R)));
    }

    public void testEllipse() throws Exception {
        (new PerfMeter("Ellipse", testDuration)).exec(createPR(new EllipseRenderer(objectCount, R)));
    }

    public void testLine() throws Exception {
        (new PerfMeter("Line", testDuration)).exec(createPR(new LineRenderer(objectCount, R)));
    }

    public void testPath() throws Exception {
        (new PerfMeter("Path", testDuration)).exec(createPR(new PathRenderer(objectCount, R)));
    }

    public void testRectangle() throws Exception {
        (new PerfMeter("Rectangle", testDuration)).exec(createPR(new RectangleRenderer(objectCount, R)));
    }

    public void testRotatedRectangleRH() throws Exception {
        (new PerfMeter("RotatedRectangleRH", testDuration)).exec(createPR(new RectangleRendererRH(objectCount, R)));
    }

    public void testStrokedRectangle() throws Exception {
        (new PerfMeter("StrokedRectangle", testDuration)).exec(createPR(new StrokedRectangleRenderer(objectCount, R)));
    }

    public void testPolygon() throws Exception {
        (new PerfMeter("Polygon", testDuration)).exec(createPR(new PolygonRenderer(objectCount, R)));
    }

    public void testStrokedPolygon() throws Exception {
        (new PerfMeter("StrokedPolygon", testDuration)).exec(createPR(new StrokedPolygonRenderer(objectCount, R)));
    }

    public void testWhiteText() throws Exception {
        (new PerfMeter("WhiteText", testDuration)).exec(createPR(new WhiteTextRenderer(objectCount, R)));
    }

    public void testColorText() throws Exception {
        (new PerfMeter("ColorText", testDuration)).exec(createPR(new ColorTextRenderer(objectCount, R)));
    }

    public void testLargeText() throws Exception {
        (new PerfMeter("LargeText", testDuration)).exec(createPR(new LargeTextRenderer(objectCount, R)));
    }

    public void testLargeColorText() throws Exception {
        (new PerfMeter("LargeColorText", testDuration)).exec(createPR(new LargeColorTextRenderer(objectCount, R)));
    }

    public void testImage() throws Exception {
        (new PerfMeter("Image", testDuration)).exec(createPR(new ImageRenderer(objectCount, R)));
    }

    public void testImageRH() throws Exception {
        (new PerfMeter("ImageRH", testDuration)).exec(createPR(new ImageRendererRH(objectCount, R)));
    }

    public void test3DBox() throws Exception {
        (new PerfMeter("3DBox", testDuration)).exec(createPR(new Box3DRenderer(objectCount, R)));
    }

    public void test3DCylinder() throws Exception {
        (new PerfMeter("3DCylinder", testDuration)).exec(createPR(new CylinderRenderer(objectCount, R)));
    }

    public void test3DSphere() throws Exception {
        (new PerfMeter("3DSphere", testDuration)).exec(createPR(new SphereRenderer(objectCount, R)));
    }

    public void test3DMesh() throws Exception {
        (new PerfMeter("3DMesh", testDuration)).exec(createPR(new MeshRenderer(objectCount, R)));
    }

    public void testMultiShape2DInterleaved() throws Exception {
        (new PerfMeter("MultiShape2DInterleaved", testDuration)).exec(createPR(new MultiShapeRendererInterleaved(objectCount, R)));
    }

    public void testMultiShape2D() throws Exception {
        (new PerfMeter("MultiShape2D", testDuration)).exec(createPR(new MultiShapeRenderer(objectCount, R)));
    }

    public void testMultiShape3DInterleaved() throws Exception {
        (new PerfMeter("MultiShape3DInterleaved", testDuration)).exec(createPR(new MultiShape3DRendererInterleaved(objectCount, R)));
    }

    public void testMultiShape3D() throws Exception {
        (new PerfMeter("MultiShape3D", testDuration)).exec(createPR(new MultiShape3DRenderer(objectCount, R)));
    }

    public void testMultiShape2D3DInterleaved() throws Exception {
        (new PerfMeter("MultiShape2D3DInterleaved", testDuration)).exec(createPR(new MultiShape2D3DRendererInterleaved(objectCount, R)));
    }

    public void testMultiShape2D3D() throws Exception {
        (new PerfMeter("MultiShape2D3D", testDuration)).exec(createPR(new MultiShape2D3DRenderer(objectCount, R)));
    }

    public void testButton() throws Exception {
        (new PerfMeter("Button", testDuration)).exec(createPR(new ButtonRenderer(objectCount, R)));
    }

    /**
     * Initialize the JavaFX application environment.
     * Once the stage is initialized, all tests use
     * same environment for execution.
     */
    public void intializeFxEnvironment() {
        Platform.startup(() -> {
            stage = new Stage();
            Platform.setImplicitExit(false);
        });
    }

    public void exitFxEnvironment() {
        Platform.exit();
    }

    public boolean parseCmdOptions(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch(arg) {
            case "-t":
                while ((i+1) < args.length && args[i + 1].charAt(0) != '-') {
                    testList.add(args[++i]);
                }
                if (testList.size() == 0) return false;
                break;
            case "-n":
                try {
                    objectCount = Integer.parseInt(args[++i]);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("\nnumber_of_objects not provided.");
                    return false;
                }
                break;
            case "-d":
                try {
                    testDuration = Integer.parseInt(args[++i]);
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("\ntest_duration_in_seconds not provided.");
                    return false;
                }
                break;
            case "-h":
            case "--help":
            default:
                return false;
            }
        }
        return true;
    }

    public static void printTests() {
        Method[] methods = RenderPerfTest.class.getDeclaredMethods();
        System.out.println("\nSupported tests:");
        for (Method m : methods) {
            if (m.getName().startsWith("test")) {
                System.out.println(m.getName().replaceFirst("test", ""));
            }
        }
    }

    public static void printUsage() {
        System.out.println("Usage: java @<path_to>/run.args RenderPerfTest [-t <test_name>...] [-n <number_of_objects>] [-d <test_duration_in_seconds>] [-h]");
        System.out.println("       Where test_name: Name of the test (or tests) to be executed.");
        System.out.println("             number_of_objects: Number of objects to be rendered in the test(s)");
        System.out.println("             test_duration_in_seconds: How many seconds should each test take (default 10, set 0 for infinite run)");
        System.out.println("                                       NOTE: Tests have extra 5 seconds warmup time where performance is NOT measured.");
        System.out.println("             -h: help: print application usage");
        System.out.println("NOTE: Set JVM command line parameter -Djavafx.animation.fullspeed=true to run animation at full speed");

        printTests();
    }

    public static void main(String[] args) throws Exception {
        RenderPerfTest test = new RenderPerfTest();

        test.intializeFxEnvironment();
        testList = new ArrayList<String>();

        if (!test.parseCmdOptions(args)) {
            printUsage();
            test.exitFxEnvironment();
            return;
        }

        if (test.objectCount == 0) {
            test.objectCount = 1000;
        }

        if (test.testDuration == 0) {
            System.out.println("NOTE: Test length set to 0. Test will run indefinitely until Stage is closed.");
        }

        try {
            if (testList.size() != 0) {
                for(String testName: testList) {
                    Method m = RenderPerfTest.class.getDeclaredMethod("test" + testName);
                    m.invoke(test);
                }
            } else {
                Method[] methods = RenderPerfTest.class.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.getName().startsWith("test")) {
                        m.invoke(test);
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            System.out.println("\nIncorrect Test Name!");
            printTests();
        } catch (InvocationTargetException e) {
            System.out.println(e.getCause().getMessage());
        } catch (Exception e) {
            System.out.println("\nUnexpected error occurred");
            e.printStackTrace();
        }
        test.exitFxEnvironment();
    }
}
