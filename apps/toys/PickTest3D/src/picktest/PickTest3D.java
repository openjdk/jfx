/*
 * Copyright (c) 2010, 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package picktest;

import java.util.List;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.VPos;
import javafx.scene.Camera;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * 3D picking test application.
 * It shows a complex object built of 3D shapes. A small second window is shown,
 * monitoring on-line the current pick result. As you move the mouse over
 * the shapes, you can see the monitor window updated and also the picked
 * texture coordinates highlighted. By mouse dragging the red/blue shapes
 * you can rotate the shape groups as if they were hanging on the gray wires.
 * By dragging the green handles or the gray wires you can rotate the whole
 * thing. By rotating the mouse wheel over a red or blue sphere or cylinder
 * you can change its "divisions" parameter. By right click you can switch
 * each shape's cullFace, by right click with ctrl modifier pressed you can
 * switch each shape's depthTest. By dragging the mouse with modifiers you move
 * the camera (ctrl - translate x/y, alt - rotate x/y,
 * shift - translate z as you move up/down).
 */
public class PickTest3D extends Application {

    double anchorX, anchorY, anchorAngle;
    boolean cameraInMove = false;
    
    private static final WritableImage diffuseMap = new WritableImage(16,16);
        
    private static final WritableImage pressedMap = new WritableImage(1,1);

    private static final double SPINNER_OFFSET = 300;
    private static final double HANDLE_OFFSET = 250;
    private static final float COVER_SIZE = 100;

    private static Monitor monitor;

    private void setArgb(PixelWriter pWriter,
            int startX, int endX, int startY, int endY, int value) {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                pWriter.setArgb(x, y, value);
            }
        }
    }
    
    private void make2By16CheckerPattern(PixelWriter pWriter, int start, int end, int startValue) {
        int endValue = startValue == 0Xffffffff ? 0Xff000000 : 0Xffffffff;
        setArgb(pWriter, 0, 2, start, end, startValue);
        setArgb(pWriter, 2, 4, start, end, endValue);
        setArgb(pWriter, 4, 6, start, end, startValue);
        setArgb(pWriter, 6, 8, start, end, endValue);
        setArgb(pWriter, 8, 10, start, end, startValue);
        setArgb(pWriter, 10, 12, start, end, endValue);
        setArgb(pWriter, 12, 14, start, end, startValue);
        setArgb(pWriter, 14, 16, start, end, endValue);
    }

    private void make16By16CheckerPattern(WritableImage map) {
        PixelWriter pWriter = map.getPixelWriter();
        make2By16CheckerPattern(pWriter, 0, 2, 0Xff000000);
        make2By16CheckerPattern(pWriter, 2, 4, 0Xffffffff);
        make2By16CheckerPattern(pWriter, 4, 6, 0Xff000000);
        make2By16CheckerPattern(pWriter, 6, 8, 0Xffffffff);
        make2By16CheckerPattern(pWriter, 8, 10, 0Xff000000);
        make2By16CheckerPattern(pWriter, 10, 12, 0Xffffffff);
        make2By16CheckerPattern(pWriter, 12, 14, 0Xff000000);
        make2By16CheckerPattern(pWriter, 14, 16, 0Xffffffff);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        make16By16CheckerPattern(diffuseMap);

        PixelWriter pWriter = pressedMap.getPixelWriter();
        pWriter.setArgb(0, 0, 0Xff0f0f0f);
        
        primaryStage.setTitle("3D picking test");

        final PerspectiveCamera camera = new PerspectiveCamera();

        final Node toy = createToy();

        final Group parent = new Group(toy);
        parent.setTranslateZ(600);
        parent.setTranslateX(-150);
        parent.setTranslateY(-200);
        parent.setScaleX(0.8);
        parent.setScaleY(0.8);
        parent.setScaleZ(0.8);

        final PointLight pointLight = new PointLight(Color.ANTIQUEWHITE);
        pointLight.setTranslateX(100);
        pointLight.setTranslateY(100);
        pointLight.setTranslateZ(-300);

        final Group root = new Group(parent, pointLight, new Group(camera));
        root.setDepthTest(DepthTest.ENABLE);

        final Scene scene = new Scene(root, 800, 800, true);
        scene.setCamera(camera);

        activateScene(scene, camera);

        primaryStage.setScene(scene);
        primaryStage.show();

        monitor = new Monitor();
        monitor.show();
        primaryStage.requestFocus();
        monitor.setY(primaryStage.getY());
        monitor.setX(primaryStage.getX() - monitor.getWidth());
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override public void handle(WindowEvent event) {
                monitor.close();
            }
        });
    }

    private static Node createToy() {

        Node h1 = createHandle("Top handle");
        h1.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        Node h2 = createCoveredHandle("Bottom handle");
        h2.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        h2.setTranslateY(2 * HANDLE_OFFSET + 2 * SPINNER_OFFSET + COVER_SIZE);

        Node h3 = createHandle("Left handle");
        h3.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET);
        Node h4 = createHandle("Right handle");
        h4.setTranslateX(2 * HANDLE_OFFSET + 2 * SPINNER_OFFSET);
        h4.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET);

        Node spheres = createSpheres();
        spheres.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        spheres.setTranslateY(HANDLE_OFFSET);

        Node boxes = createBoxes();
        boxes.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET);
        boxes.setTranslateX(HANDLE_OFFSET);

        Node cylinders = createCylinders();
        cylinders.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET * 2);
        cylinders.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET);

        Node star = createStar();
        star.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        star.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET * 2);


        Cylinder verticalWire = new Cylinder(15, 2 * HANDLE_OFFSET + 2 * SPINNER_OFFSET + COVER_SIZE);
        verticalWire.setMaterial(createGrayMaterial());
        activateShape(verticalWire, "Vertical Wire");

        verticalWire.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        verticalWire.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET + COVER_SIZE / 2);

        Cylinder horizontalWire = new Cylinder(15, 2 * HANDLE_OFFSET + 2 * SPINNER_OFFSET);
        horizontalWire.setMaterial(createGrayMaterial());
        activateShape(horizontalWire, "Horizontal Wire");
        horizontalWire.setRotate(90);
        horizontalWire.setTranslateX(HANDLE_OFFSET + SPINNER_OFFSET);
        horizontalWire.setTranslateY(HANDLE_OFFSET + SPINNER_OFFSET);

        RotableGroup xParent = new RotableGroup(Rotate.X_AXIS, spheres, boxes,
                cylinders, star, verticalWire, horizontalWire, h1, h2, h3, h4);
        xParent.setHandles(h1, h2, verticalWire);

        RotableGroup yParent = new RotableGroup(Rotate.Y_AXIS, xParent);
        yParent.setHandles(h3, h4, horizontalWire);


        return yParent;
    }

    private static Node createCoveredHandle(String name) {

        Node handle = createHandle(name);

        float[] points = new float[] {
            -COVER_SIZE, -COVER_SIZE, -COVER_SIZE,
             COVER_SIZE, -COVER_SIZE, -COVER_SIZE,
             COVER_SIZE, -COVER_SIZE,  COVER_SIZE,
            -COVER_SIZE, -COVER_SIZE,  COVER_SIZE,
            -1.5f*COVER_SIZE, 0, 0,
             1.5f*COVER_SIZE, 0, 0
        };

        float[] texCoords = new float[] {
            0, 0,
            1, 0,
            1, 1,
            0, 1
        };

        int[] faces = new int[] {
            0, 0, 1, 1, 2, 2,
            0, 0, 2, 2, 3, 3,
            3, 0, 4, 1, 0, 2,
            1, 0, 5, 2, 2, 3
        };

        final TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(points);
        triangleMesh.getTexCoords().setAll(texCoords);
        triangleMesh.getFaces().setAll(faces);
        final MeshView cover = new MeshView(triangleMesh);
        cover.setMaterial(createGreenMaterial());
        activateShape(cover, "Handle cover");

        cover.setCullFace(CullFace.NONE);

        return new Group(handle, cover);
    }

    private static Node createHandle(String name) {
        final Sphere handle = new Sphere(50);
        handle.setMaterial(createGreenMaterial());
        activateShape(handle, name);
        return handle;
    }

    private static Node createSpheres() {
        final Sphere red = new Sphere(150);
        red.setMaterial(createRedMaterial());

        final Sphere blue1 = new Sphere(50);
        blue1.setMaterial(createBlueMaterial());
        blue1.setTranslateZ(-150);
        blue1.setTranslateY(-50);

        final Sphere blue2 = new Sphere(50);
        blue2.setMaterial(createBlueMaterial());
        blue2.setTranslateZ(150);
        blue2.setTranslateY(50);

        final RotableGroup parent = new RotableGroup(Rotate.Y_AXIS, red, blue1, blue2);
        activateSphere(red, "Red sphere", parent);
        activateSphere(blue1, "Higher blue sphere", parent);
        activateSphere(blue2, "Lower blue sphere", parent);
        parent.setHandles(red, blue1, blue2);

        return parent;
    }

    private static Node createBoxes() {

        final Box red = new Box(300, 200, 300);
        red.setMaterial(createRedMaterial());
        activateShape(red, "Red box");

        final Box blue1 = new Box(100, 300, 100);
        blue1.setMaterial(createBlueMaterial());
        activateShape(blue1, "Thick blue box");

        final Box blue2 = new Box(100, 50, 400);
        blue2.setMaterial(createBlueMaterial());
        activateShape(blue2, "Thin blue box");

        final RotableGroup parent = new RotableGroup(Rotate.X_AXIS, blue1, blue2, red);
        parent.setHandles(red, blue1, blue2);

        return parent;
    }

    private static Node createCylinders() {

        final Cylinder red = new Cylinder(150, 300);
        red.setMaterial(createRedMaterial());
        red.setRotate(90);


        final Cylinder bigBlue = new Cylinder(100, 400);
        bigBlue.setMaterial(createBlueMaterial());

        final Cylinder blue1 = new Cylinder(50, 100);
        blue1.setMaterial(createBlueMaterial());
        blue1.setRotate(45);
        blue1.setTranslateZ(-150);

        final Cylinder blue2 = new Cylinder(50, 100);
        blue2.setMaterial(createBlueMaterial());
        blue2.setTranslateZ(150);

        final RotableGroup parent = new RotableGroup(
                Rotate.X_AXIS, red, bigBlue, blue1, blue2);
        activateCylinder(red, "Red cylinder", parent);
        activateCylinder(bigBlue, "Big blue cylinder", parent);
        activateCylinder(blue1, "Small rotated blue cylinder", parent);
        activateCylinder(blue2, "Small blue cylinder", parent);
        parent.setHandles(red, bigBlue, blue1, blue2);

        return parent;
    }

    private static Node createStar() {
        final Sphere sphere1 = new Sphere(150);
        sphere1.setMaterial(createRedMaterial());
        sphere1.setScaleX(0.5);
        sphere1.setRotate(45);

        final Sphere sphere2 = new Sphere(150);
        sphere2.setMaterial(createRedMaterial());
        sphere2.setScaleX(0.2);
        sphere2.setRotate(-45);

        final Box box1 = new Box(50, 50, 50);
        box1.setMaterial(createBlueMaterial());
        box1.getTransforms().addAll(
                new Rotate(90, new Point3D(1, 1, 0)),
                new Translate(100, 100, 0));

        final Box box2 = new Box(50, 50, 50);
        box2.setMaterial(createBlueMaterial());
        box2.getTransforms().addAll(
                new Rotate(90, new Point3D(1, 1, 0)),
                new Translate(-100, -100, 0));

        final Cylinder cylinder = new Cylinder(20, 40);
        cylinder.setMaterial(createBlueMaterial());
        cylinder.getTransforms().addAll(
                new Rotate(90),
                new Scale(1, 10)
                );

        final RotableGroup parent = new RotableGroup(
                Rotate.Y_AXIS, sphere1, sphere2, box1, box2, cylinder);

        activateSphere(sphere1, "Thick red disc", parent);
        activateSphere(sphere2, "Thin red disc", parent);
        activateShape(box1, "Higher blue box on red disc");
        activateShape(box2, "Lower blue box on red disc");
        activateCylinder(cylinder, "Stretched blue cyliner in red discs", parent);
        parent.setHandles(sphere1, sphere2, box1, box2, cylinder);

        return parent;
    }

    private static void activateSphere(final Sphere sphere, final String name, 
            final RotableGroup handleOf) {

        activateShape(sphere, name);
        sphere.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                int sign = (int) Math.signum(event.getDeltaY());
                int divs = sphere.getDivisions();
                int ndivs = divs + 4 * sign;
                if (ndivs < 4) {
                    ndivs = 4;
                } else if (ndivs > 20) {
                    ndivs = sign < 0 ? 20 : 64;
                }
                if (ndivs == divs) {
                    return;
                }
  
                Sphere s = new Sphere(sphere.getRadius(), ndivs);
                s.setMaterial(sphere.getMaterial());
                s.setTranslateX(sphere.getTranslateX());
                s.setTranslateY(sphere.getTranslateY());
                s.setTranslateZ(sphere.getTranslateZ());
                s.setRotationAxis(sphere.getRotationAxis());
                s.setRotate(sphere.getRotate());
                s.setScaleX(sphere.getScaleX());
                s.setScaleY(sphere.getScaleY());
                s.setScaleZ(sphere.getScaleZ());
                s.getTransforms().addAll(sphere.getTransforms());
                s.setCullFace(sphere.getCullFace());

                List children = ((Group) sphere.getParent()).getChildren();
                int i = children.indexOf(sphere);
                children.remove(i);
                children.add(i, s);

                handleOf.replaceHandle(sphere, s);

                activateSphere(s, name, handleOf);
            }
        });
    }

    private static void activateCylinder(final Cylinder cylinder,
            final String name, final RotableGroup handleOf) {

        activateShape(cylinder, name);
        cylinder.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                int sign = (int) Math.signum(event.getDeltaY());
                int divs = cylinder.getDivisions();
                int ndivs = divs + 4 * sign;
                if (ndivs < 4) {
                    ndivs = 4;
                } else if (ndivs > 20) {
                    ndivs = sign < 0 ? 20 : 64;
                }
                if (ndivs == divs) {
                    return;
                }

                Cylinder c = new Cylinder(cylinder.getRadius(), cylinder.getHeight(), ndivs);
                c.setMaterial(cylinder.getMaterial());
                c.setTranslateX(cylinder.getTranslateX());
                c.setTranslateY(cylinder.getTranslateY());
                c.setTranslateZ(cylinder.getTranslateZ());
                c.setRotationAxis(cylinder.getRotationAxis());
                c.setRotate(cylinder.getRotate());
                c.setScaleX(cylinder.getScaleX());
                c.setScaleY(cylinder.getScaleY());
                c.setScaleZ(cylinder.getScaleZ());
                c.getTransforms().addAll(cylinder.getTransforms());
                c.setCullFace(cylinder.getCullFace());

                List children = ((Group) cylinder.getParent()).getChildren();
                int i = children.indexOf(cylinder);
                children.remove(i);
                children.add(i, c);

                handleOf.replaceHandle(cylinder, c);

                activateCylinder(c, name, handleOf);
            }
        });
    }

    private static void activateShape(final Shape3D shape, final String name) {

        shape.setId(name);

        shape.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                ((PhongMaterial) shape.getMaterial()).setSelfIlluminationMap(pressedMap);
                monitor.setState(event.getPickResult());
            }
        });

        EventHandler<MouseEvent> moveHandler = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                PickResult res = event.getPickResult();
                if (res != null) {
                    if (((PhongMaterial)shape.getMaterial()).getSelfIlluminationMap() != pressedMap) {
                        Point2D tex = res.getIntersectedTexCoord();
                        if (tex != null) {
                            ((PhongMaterial)shape.getMaterial()).setSelfIlluminationMap(
                                    createHoverImage(tex.getX(), tex.getY()));
                        }
                    }
                }

                monitor.setState(res);
                event.consume();
            }
        };

        shape.setOnMouseMoved(moveHandler);
        shape.setOnMouseDragOver(moveHandler);

        shape.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                PickResult res = event.getPickResult();
                if (res == null) {
                    System.err.println("Mouse entered has not pickResult");
                }
                monitor.setState(res);
            }
        });

        shape.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                PickResult res = event.getPickResult();
                if (res == null) {
                    System.err.println("Mouse exited has not pickResult");
                }
                if (((PhongMaterial)shape.getMaterial()).getSelfIlluminationMap() != pressedMap) {
                    ((PhongMaterial)shape.getMaterial()).setSelfIlluminationMap(null);
                }
                monitor.setState(res);
                event.consume();
            }
        });

        shape.setOnMouseDragExited(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                PickResult res = event.getPickResult();
                if (res == null) {
                    System.err.println("Mouse exited has not pickResult");
                }
                if (((PhongMaterial)shape.getMaterial()).getSelfIlluminationMap() != pressedMap) {
                    ((PhongMaterial)shape.getMaterial()).setSelfIlluminationMap(null);
                }
                monitor.setState(res);
                event.consume();
            }
        });

        shape.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                PickResult res = event.getPickResult();

                if (res != null && res.getIntersectedTexCoord() != null && shape.isHover()) {
                    ((PhongMaterial)shape.getMaterial()).setSelfIlluminationMap(
                            createHoverImage(
                                res.getIntersectedTexCoord().getX(),
                                res.getIntersectedTexCoord().getY()));
                } else {
                    ((PhongMaterial)shape.getMaterial()).setSelfIlluminationMap(null);
                }
            }
        });

        shape.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.SECONDARY) {
                    if (event.isControlDown()) {
                        DepthTest curr = shape.getDepthTest();
                        switch(curr) {
                            case INHERIT: shape.setDepthTest(DepthTest.DISABLE); break;
                            case DISABLE: shape.setDepthTest(DepthTest.ENABLE); break;
                            case ENABLE: shape.setDepthTest(DepthTest.INHERIT); break;
                        }
                    } else {
                        CullFace curr = shape.getCullFace();
                        switch(curr) {
                            case NONE: shape.setCullFace(CullFace.BACK); break;
                            case BACK: shape.setCullFace(CullFace.FRONT); break;
                            case FRONT: shape.setCullFace(CullFace.NONE); break;
                        }
                    }
                }
                PickResult res = event.getPickResult();
                monitor.setState(res);
            }
        });

        shape.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                shape.startFullDrag();
                event.consume();
            }
        });
    }

    private void activateScene(final Scene scene, final Camera camera) {
        scene.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                monitor.setState(event.getPickResult());
            }
        });

        scene.setOnMouseDragOver(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                monitor.setState(event.getPickResult());
            }
        });

        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.getParent().setRotationAxis(Rotate.X_AXIS);

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isControlDown() || event.isAltDown() || event.isShiftDown()) {
                    cameraInMove = true;
                    anchorX = event.getSceneX();
                    anchorY = event.getSceneY();
                    event.consume();
                }
            }
        });

        scene.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                scene.startFullDrag();
                event.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (cameraInMove) {
                    double sx = event.getSceneX();
                    double sy = event.getSceneY();
                    if (cameraInMove) {
                        if (event.isControlDown()) {
                            camera.setTranslateX(camera.getTranslateX() + sx - anchorX);
                            camera.setTranslateY(camera.getTranslateY() + sy - anchorY);
                        } else if (event.isAltDown()) {
                            camera.setRotate(camera.getRotate() + sx - anchorX);
                            camera.getParent().setRotate(camera.getParent().getRotate() + sy - anchorY);
                        } else if (event.isShiftDown()) {
                            camera.setTranslateZ(camera.getTranslateZ() + (anchorY - sy) * 3);
                        }
                    }
                    anchorX = sx;
                    anchorY = sy;
                    event.consume();
                }

                monitor.setState(event.getPickResult());
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (cameraInMove) {
                    cameraInMove = false;
                    event.consume();
                }
            }
        });

    }

    private static Image createHoverImage(double x, double y) {
        Rectangle rect = new Rectangle(100, 100);
        rect.setFill(
                new RadialGradient(0, 0,
                    100 * x, 100 * y,
                    100, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.DARKGRAY.darker().darker()),
                    new Stop(0.3, Color.BLACK)));
        return rect.snapshot(null, null);
    }

    private static Material createMaterial(Color dif, Color spec) {
        PhongMaterial mat = new PhongMaterial();

        mat.setDiffuseColor(new Color(dif.getRed(), dif.getGreen(), dif.getBlue(), 1.0));
        mat.setSpecularColor(spec);
        mat.setDiffuseMap(diffuseMap);
        return mat;
    }

    private static Material createRedMaterial() {
        return createMaterial(Color.ORANGERED, Color.ORANGE);
    }

    private static Material createBlueMaterial() {
        return createMaterial(Color.ROYALBLUE, Color.LIGHTBLUE);
    }

    private static Material createGreenMaterial() {
        return createMaterial(Color.GREEN, Color.LIGHTGREEN);
    }

    private static Material createGrayMaterial() {
        return createMaterial(Color.GRAY, Color.WHITE);
    }

    private static class RotableGroup extends Group {
        double anchorX, anchorY, anchorAngle;
        Point3D axis;
        Point3D anchorAxis;
        Node[] handles;
        EventHandler<MouseEvent> pressHandler, dragHandler;

        public RotableGroup(Point3D axis, Node... children) {
            super(children);

            this.axis = axis;
            setRotationAxis(axis);

            pressHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    anchorX = event.getSceneX();
                    anchorY = event.getSceneY();
                    anchorAngle = getRotate();
                    anchorAxis = localToScene(RotableGroup.this.axis).subtract(localToScene(new Point3D(0, 0, 0)));
                    event.consume();
                }
            };

            dragHandler = new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    Point3D dragged = new Point3D(event.getSceneX() - anchorX,
                                                  event.getSceneY() - anchorY,
                                                  0);

                    double angle = anchorAxis.angle(dragged);

                    Point3D right = new Point3D(-anchorAxis.getY(), anchorAxis.getX(), 0);
                    double sign = right.angle(dragged) < 90 ? 1 : -1;

                    setRotate(anchorAngle + sign * dragged.magnitude() * Math.sin(Math.toRadians(angle)));

                    event.consume();
                }
            };
        }

        public void setHandles(Node... handles) {
            for (Node handle : handles) {
                handle.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
                handle.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
            }
        }

        public void replaceHandle(Node oldHandle, Node newHandle) {
            oldHandle.removeEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            oldHandle.removeEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
            newHandle.addEventHandler(MouseEvent.MOUSE_PRESSED, pressHandler);
            newHandle.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        }
    }

    private static class Monitor extends Stage {
        Text caption, data;

        public Monitor() {

            HBox root = new HBox(10);

            caption = new Text("Node:\n\nPoint:\nDistance:\nFace:\ntexture Coord:");
            caption.setTextOrigin(VPos.TOP);
            caption.setTextAlignment(TextAlignment.RIGHT);


            data = new Text("-- None --\n\n\n\n");
            data.setTextOrigin(VPos.TOP);
            data.setTextAlignment(TextAlignment.LEFT);

            root.getChildren().addAll(caption, data);
            Scene s = new Scene(root, 270, 120);
            setX(0);
            setY(0);
            setScene(s);
        }

        private static String point3DToString(Point3D pt) {
            if (pt == null) {
                return "null";
            }
            return String.format("%.1f; %.1f; %.1f", pt.getX(), pt.getY(), pt.getZ());
        }

        private static String point2DToString(Point2D pt) {
            if (pt == null) {
                return "null";
            }
            return String.format("%.2f; %.2f", pt.getX(), pt.getY());
        }

        private static String getCullFace(Node n) {
            if (n instanceof Shape3D) {
                return "(CullFace." + ((Shape3D) n).getCullFace() + ")";
            }
            return "";
        }

        public void setState(PickResult result) {
            if (result.getIntersectedNode() == null) {
                data.setText("Scene\n\n" +
                        point3DToString(result.getIntersectedPoint()) + "\n" +
                        String.format("%.1f", result.getIntersectedDistance())  + "\n" +
                        result.getIntersectedFace() + "\n" +
                        point2DToString(result.getIntersectedTexCoord()));
            } else {
                data.setText(result.getIntersectedNode().getId() + "\n" +
                        getCullFace(result.getIntersectedNode()) + "\n" +
                        point3DToString(result.getIntersectedPoint()) + "\n" +
                        String.format("%.1f", result.getIntersectedDistance())  + "\n" +
                        result.getIntersectedFace() + "\n" +
                        point2DToString(result.getIntersectedTexCoord()));
            }
        }
    }
}
