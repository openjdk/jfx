/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

package touchsuite;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ImageViewer extends Application {

    private static final int GAP = 150;
    Image[] images;
    ImageView left, center, right;
    Group view;
    Rectangle bg;
    int index;
    boolean dragleft = true;
    boolean clockwise = true;
    boolean origSize = false;

    double mouseX, mouseY;

    Rotate rotate = new Rotate();
    Scale scale = new Scale();

    @Override public void start(Stage stage) {
        stage.setTitle("Image Viewer");

        Group root = new Group();
        Scene scene = new Scene(root, 500, 500);

        bg = new Rectangle(500, 500, Color.BLACK);

        images = loadImages();
        left = new ImageView();
        center = new ImageView();
        right = new ImageView();

        if (images.length > 0) setup(center, images[0]);
        if (images.length > 1) setup(right, images[1]);

        view = new Group(bg, left, right, center);

        center.setTranslateX((bg.getWidth() - center.getImage().getWidth()) / 2);
        alignNeighbours();

        root.getChildren().addAll(view);
        stage.setScene(scene);
        stage.show();

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                resize();
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1) {
                resize();
            }
        });

        view.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                if (event.isInertia()) {
                    return;
                }

                if (!event.isDirect()) {

                    if (Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())) {
                        drag(-event.getDeltaX(), 0);
                        event.consume();
                    } else if (Math.abs(event.getDeltaY()) > 0.5) {
                        double factor =
                                event.getDeltaY() > 0 ? 0.8 : 1.25;

                        double x = event.getX();
                        double y = event.getY();
                        zoom(x, y, factor, false);
                        dragEnd(false);
                    }
                    return;
                }

                if (event.getTouchCount() > 1) {
                    return;
                }
                drag(event.getDeltaX(), event.getDeltaY());
                event.consume();
            }
        });

        view.setOnScrollFinished(new EventHandler<ScrollEvent>() {
            @Override public void handle(ScrollEvent event) {
                dragEnd(true);
                event.consume();
            }
        });

        view.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isSynthesized()) {
                    return;
                }
                mouseX = event.getSceneX();
                mouseY = event.getSceneY();
                event.consume();
            }
        });

        view.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isSynthesized() || event.isStillSincePress()) {
                    return;
                }
                drag(event.getSceneX() - mouseX, event.getSceneY() - mouseY);
                mouseX = event.getSceneX();
                mouseY = event.getSceneY();
                event.consume();
            }
        });

        view.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isSynthesized() || event.isStillSincePress()) {
                    return;
                }
                dragEnd(true);
                event.consume();
            }
        });

        view.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (event.isStillSincePress() && !event.isSynthesized()) {
                    double angle = center.getRotate();
                    double toangle = 0;
                    while (toangle < Math.abs(angle)) {
                        toangle += 90;
                    }

                    if (angle < 0) {
                        toangle = - toangle;
                    }
                    if (event.getButton() == MouseButton.SECONDARY) {
                        toangle -= 90;
                    }
                    if (event.getButton() == MouseButton.PRIMARY) {
                        toangle += 90;
                    }

                    if (toangle != angle) {
                        RotateTransition t = new RotateTransition(Duration.millis(200), center);
                        t.setInterpolator(Interpolator.EASE_OUT);
                        t.setToAngle(toangle);
                        t.play();
                    }
                }
                event.consume();
            }
        });


        center.translateXProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable o) {
                center.getTranslateX();
                alignNeighbours();
            }
        });

        center.setOnRotate(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                if (event.isInertia()) {
                    return;
                }
                center.setRotate(center.getRotate() + event.getAngle());
                clockwise = event.getAngle() > 0;
                event.consume();
            }
        });

        center.setOnRotationFinished(new EventHandler<RotateEvent>() {
            @Override public void handle(RotateEvent event) {
                double angle = center.getRotate();

                double toangle = 0;
                while (toangle < Math.abs(angle)) {
                    toangle += 90;
                }

                if (angle < 0) {
                    toangle = - toangle;
                    if (clockwise) {
                        toangle += 90;
                    }
                } else {
                    if (!clockwise) {
                        toangle -= 90;
                    }
                }

                if (toangle - angle > 75) {
                    toangle -= 90;
                }
                if (toangle - angle < -75) {
                    toangle += 90;
                }

                RotateTransition t = new RotateTransition(Duration.millis(200), center);
                t.setInterpolator(Interpolator.EASE_OUT);
                t.setToAngle(toangle);
                t.play();
                event.consume();
            }
        });

        view.setOnZoom(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                if (event.isInertia()) {
                    return;
                }
                zoom(event.getX(), event.getY(), event.getZoomFactor(), false);
                event.consume();
            }
        });

        view.setOnZoomFinished(new EventHandler<ZoomEvent>() {
            @Override public void handle(ZoomEvent event) {
                zoomEnd();
                event.consume();
            }
        });
    }

    private void zoom(double x, double y, double factor, boolean slow) {
        final double w = center.getBoundsInParent().getWidth();
        final double h = center.getBoundsInParent().getHeight();

        final double minx = center.getBoundsInParent().getMinX();
        final double miny = center.getBoundsInParent().getMinY();

        double dw = w * (factor - 1);
        double xr = 2 * (w / 2 - (x - minx)) / w;

        double dh = h * (factor - 1);
        double yr = 2 * (h / 2 - (y - miny)) / h;

        if (slow) {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), center);
            st.setToX(center.getScaleX() * factor);
            st.setToY(center.getScaleY() * factor);
            st.setInterpolator(Interpolator.LINEAR);

            TranslateTransition tt = new TranslateTransition(Duration.millis(100), center);
            tt.setToX(center.getTranslateX() + xr * dw / 2);
            tt.setToY(center.getTranslateY() + yr * dh / 2);
            tt.setInterpolator(Interpolator.EASE_IN);

            ParallelTransition pt = new ParallelTransition(st, tt);
            pt.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    alignNeighbours();
                }
            });
            pt.play();

        } else {
            center.setScaleX(center.getScaleX() * factor);
            center.setScaleY(center.getScaleY() * factor);
            move (xr * dw / 2, yr * dh / 2);
            alignNeighbours();
        }

    }

    private void zoomEnd() {
        Image img = center.getImage();

        final double originalScale = Math.min(
                (bg.getHeight() - 20) / img.getHeight(),
                (bg.getWidth() - 20) / img.getWidth());

        final double newScale = center.getScaleX();
        final double ratio = newScale / originalScale;

        if (ratio > 0.8 && ratio < 1.25) {
            origSize = true;
            ScaleTransition st = new ScaleTransition(Duration.millis(200), center);
            st.setToX(originalScale);
            st.setToY(originalScale);

            TranslateTransition tt = new TranslateTransition(Duration.millis(200), center);
            tt.setToX((bg.getWidth() - img.getWidth()) / 2);
            tt.setToY((bg.getHeight() - img.getHeight()) / 2);

            tt.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    origSize = false;
                }
            });

            tt.play();
            st.play();
        }
    }

    private void drag(double deltaX, double deltaY) {
        if (deltaX != 0) {
            dragleft = (deltaX < 0);
        }
        move (deltaX, deltaY);
        alignNeighbours();
    }

    private void dragEnd(boolean slow) {
        final double imgWidth = center.getImage().getWidth();
        final double imgHeight = center.getImage().getHeight();

        final double minx = center.getTranslateX()
                - imgWidth * (center.getScaleX() - 1) / 2;
        final double maxx = center.getTranslateX()
                + imgWidth * (1 + (center.getScaleX() - 1) / 2);
        final double w = center.getImage().getWidth() * center.getScaleX();

        final double miny = center.getTranslateY()
                - imgHeight * (center.getScaleY() - 1) / 2;
        final double maxy = center.getTranslateY()
                + imgHeight * (1 + (center.getScaleY() - 1) / 2);
        final double h = center.getImage().getHeight() * center.getScaleY();

        final double rgap = maxx + getGap();
        final double lgap = minx - getGap();

        final TranslateTransition t =
                new TranslateTransition(Duration.millis(200), center);

        if ((lgap < 0 || dragleft || (!dragleft && left.getImage() == null)) &&
                (rgap > bg.getWidth() || !dragleft || (dragleft && right.getImage() == null))) {
            double tox = center.getTranslateX();
            if (w > bg.getWidth()) {
                if (minx > 0) {
                    tox = center.getTranslateX() - minx;
                } else if (maxx < bg.getWidth()) {
                    tox = center.getTranslateX() + bg.getWidth() - maxx;
                }
            } else {
                tox = (bg.getWidth() - center.getImage().getWidth()) / 2;
            }

            double toy = center.getTranslateY();
            if (h > bg.getHeight()) {
                if (miny > 0) {
                    toy = center.getTranslateY() - miny;
                } else if (maxy < bg.getHeight()) {
                    toy = center.getTranslateY() + bg.getHeight() - maxy;
                }
            } else {
                toy = (bg.getHeight() - center.getImage().getHeight()) / 2;
            }

            t.setToX(tox);
            t.setToY(toy);
        } else if (dragleft) {
            double tox = center.getTranslateX()
                    - (right.getTranslateX()
                        - (bg.getWidth() - right.getImage().getWidth()) / 2);
            t.setToX(tox);
            t.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    index++;
                    setup(center, images[index]);
                    center.setTranslateX((bg.getWidth() - center.getImage().getWidth()) / 2);
                    setup(right, images.length > index + 1 ? images[index + 1] : null);
                    setup(left, images[index - 1]);
                    alignNeighbours();
                }
            });
        } else {
            double tox = center.getTranslateX()
                    + ((bg.getWidth() - left.getImage().getWidth()) / 2
                        - left.getTranslateX());
            t.setToX(tox);
            t.setOnFinished(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent event) {
                    index--;
                    setup(center, images[index]);
                    center.setTranslateX((bg.getWidth() - center.getImage().getWidth()) / 2);
                    setup(right, images[index + 1]);
                    setup(left, images.length > 0 && index > 0 ? images[index - 1] : null);
                    alignNeighbours();
                }
            });
        }

        if (! origSize &&
                (t.getToX() != center.getTranslateX() || t.getToY() != center.getTranslateY())) {
            if (slow) {
                t.setInterpolator(Interpolator.EASE_OUT);
                t.play();
            } else {
                /* here the whole transition is not used, this might be optimized */
                center.setTranslateX(t.getToX());
                center.setTranslateY(t.getToY());
            }
        }
    }

    private void resize() {

        final double imgWidth = center.getImage().getWidth();
        final double imgHeight = center.getImage().getHeight();

        double scalex = (bg.getWidth() - 20) / imgWidth;
        double scaley = (bg.getHeight() - 20) / imgHeight;
        final double ratio = center.getScaleX() / Math.min(scalex, scaley);

        final double neww = bg.getScene().getWidth();
        final double newh = bg.getScene().getHeight();

        bg.setWidth(neww);
        bg.setHeight(newh);

        scalex = (neww - 20) / imgWidth;
        scaley = (newh - 20) / imgHeight;
        final double newScale = Math.min(scalex, scaley);

        center.setScaleX(newScale * ratio);
        center.setScaleY(newScale * ratio);

        if (center.getBoundsInParent().getHeight() < newh) {
            center.setTranslateY((newh - imgHeight) / 2);
        }
        if (center.getBoundsInParent().getWidth() < neww) {
            center.setTranslateX((neww - imgWidth) / 2);
        }

        if (left.getImage() != null) {
            left.setScaleX(newScale);
            left.setScaleY(newScale);
            left.setTranslateY((newh - left.getImage().getHeight()) / 2);
        }
        if (right.getImage() != null) {
            right.setScaleX(newScale);
            right.setScaleY(newScale);
            right.setTranslateY((newh - right.getImage().getHeight()) / 2);
        }

        alignNeighbours();
    }

    private void move(double x, double y) {
        final double h = center.getBoundsInParent().getHeight();

        double tox = center.getTranslateX() + x;
        double toy = center.getTranslateY() + y;

        Bounds cb = center.getBoundsInParent();

        if (h <= bg.getHeight()) {
            toy = (bg.getHeight() - center.getImage().getHeight()) / 2;
        } else if (cb.getMinY() + y >= 10) {
            toy = center.getTranslateY() - cb.getMinY() + 10;
        } else if (cb.getMaxY() + y <= bg.getHeight() - 10) {
            toy = center.getTranslateY() + bg.getHeight() - cb.getMaxY() - 10;
        }

        double minx = (cb.getWidth() > bg.getWidth()
                ? cb.getMinX()
                : cb.getMinX() - (bg.getWidth() - cb.getWidth()) / 2);

        double maxx = (cb.getWidth() > bg.getWidth()
                ? cb.getMaxX()
                : cb.getMaxX() + (bg.getWidth() - cb.getWidth()) / 2);

        if (index == 0 && minx + x >= 10) {
            tox = center.getTranslateX() - minx + 10;
        } else if (index == images.length - 1 && maxx + x <= bg.getWidth() - 10) {
            tox = center.getTranslateX() + bg.getWidth() - maxx - 10;
        }

        center.setTranslateX(tox);
        center.setTranslateY(toy);
    }

    private void setup(ImageView view, Image i) {
        view.setImage(i);

        double scale = 1.0;
        if (i != null) {
            final double scaley = (bg.getHeight() - 20) / i.getHeight();
            final double scalex = (bg.getWidth() - 20) / i.getWidth();
            scale = Math.min(scalex, scaley);
        }

        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setRotate(0);
        if (i != null) {
            view.setTranslateY((bg.getHeight() - i.getHeight()) / 2);
        }
    }

    private double getGap() {
        final double padding = (bg.getBoundsInParent().getWidth()
                - center.getBoundsInParent().getWidth()) / 2;
        return (padding > 0 ? padding + GAP : GAP);
    }

    private void alignNeighbours() {
        final double gap = getGap();

        final double leftEdge = center.getBoundsInParent().getMinX();
        final double rightEdge = center.getBoundsInParent().getMaxX();

        left.setTranslateX(leftEdge - gap
                - left.getBoundsInParent().getMaxX() + left.getTranslateX());
        right.setTranslateX(rightEdge + gap + right.getTranslateX()
                - right.getBoundsInParent().getMinX());
    }

    Image[] loadImages() {
        Image[] images = new Image[10];
        for (int i = 0; i < 10; i++) {
            images[i] = new Image(ImageViewer.class.getResource(
                    "images/flower_" + (i+1) + ".jpg").toExternalForm(), false);
        }

        return images;
    }

    public static String info() {
        return
                "This is a simple image viewer application. You can "
                + "switch between the images by dragging them to the sides, "
                + "each image can be zoomed and rotated. This demo works "
                + "with both touch screen and mouse.";
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
