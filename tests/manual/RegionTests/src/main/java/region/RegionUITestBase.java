/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package region;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for different Region based UI tests. A new region is created
 * for each test, so the tests can configure anything they like.
 */
public class RegionUITestBase extends Application {
    // The region under test.
    private Region region;
    private Label content;
    private InvalidationListener regionListener = new InvalidationListener() {
        @Override public void invalidated(Observable observable) {
            content.resizeRelocate(
                    region.getInsets().getLeft(),
                    region.getInsets().getTop(),
                    region.getWidth() - (region.getInsets().getLeft() + region.getInsets().getRight()),
                    region.getHeight() - (region.getInsets().getTop() + region.getInsets().getBottom())
            );
        }
    };

    @Override public void start(Stage stage) throws Exception {
        final List<Method> tests = new ArrayList<Method>();

        Class clazz = getClass();
        Method[] methods = clazz.getMethods();
        for (int i=0; i<methods.length; i++) {
            Class<?>[] paramTypes = methods[i].getParameterTypes();
            if (paramTypes.length == 1 && Region.class.isAssignableFrom(paramTypes[0])) {
                tests.add(methods[i]);
            }
        }
        Collections.sort(tests, new Comparator<Method>() {
            @Override public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        final VBox parent = new VBox(5);
        parent.setAlignment(Pos.CENTER);
        parent.setFillWidth(false);
        parent.setPadding(new Insets(12));
        final Label label = new Label();
        label.setMinHeight(200);
        label.setWrapText(true);
        content = new Label("Region Content Area");
        content.setAlignment(Pos.CENTER);
        content.setManaged(false);

        final Line minXLine = new Line();
        minXLine.setManaged(false);
        final Line maxXLine = new Line();
        maxXLine.setManaged(false);
        final Line minYLine = new Line();
        minYLine.setManaged(false);
        final Line maxYLine = new Line();
        maxYLine.setManaged(false);

        final BorderPane background = new BorderPane();
        background.setStyle("-fx-background-image: url('region/checker.png');");
        final Rectangle boundsRect = new Rectangle();
        boundsRect.setWidth(300);
        boundsRect.setHeight(200);
        background.layoutXProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                boundsRect.setX(background.getLayoutX());
                content.setTranslateX(background.getLayoutX());
            }
        });
        background.layoutYProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                boundsRect.setY(background.getLayoutY());
                content.setTranslateY(background.getLayoutY());
            }
        });
        final InvalidationListener contentListener = new InvalidationListener() {
            @Override public void invalidated(Observable observable) {
                final double bx = background.getLayoutX();
                final double by = background.getLayoutY();
                final double bx2 = bx + background.getWidth();
                final double by2 = by + background.getHeight();

                final double x = bx + content.getLayoutX();
                final double y = by + content.getLayoutY();
                final double width = content.getWidth();
                final double height = content.getHeight();
                final double x2 = x + width;
                final double y2 = y + height;

                minXLine.setStartX(x);
                minXLine.setEndX(x);
                minXLine.setStartY(by - 20);
                minXLine.setEndY(by2 + 20);

                maxXLine.setStartX(x2);
                maxXLine.setEndX(x2);
                maxXLine.setStartY(by - 20);
                maxXLine.setEndY(by2 + 20);

                minYLine.setStartX(bx - 20);
                minYLine.setEndX(bx2 + 20);
                minYLine.setStartY(y);
                minYLine.setEndY(y);

                maxYLine.setStartX(bx - 20);
                maxYLine.setEndX(bx2 + 20);
                maxYLine.setStartY(y2);
                maxYLine.setEndY(y2);
            }
        };
        content.layoutXProperty().addListener(contentListener);
        content.layoutYProperty().addListener(contentListener);
        content.widthProperty().addListener(contentListener);
        content.heightProperty().addListener(contentListener);
        background.layoutXProperty().addListener(contentListener);
        background.layoutYProperty().addListener(contentListener);
        boundsRect.setFill(null);
        boundsRect.setStroke(Color.BLACK);
        boundsRect.setStrokeType(StrokeType.OUTSIDE);
        boundsRect.setManaged(false);
        parent.getChildren().addAll(label, background, boundsRect, content, minXLine, maxXLine, minYLine, maxYLine);

        // I'm going to use a Pagination control to implement my multiple
        // test test!
        Pagination pagination = new Pagination(tests.size());
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override public Node call(Integer param) {
                if (region != null) {
                    // remove listeners of the region insets and region bounds
                    region.widthProperty().removeListener(regionListener);
                    region.heightProperty().removeListener(regionListener);
                    region.layoutXProperty().removeListener(regionListener);
                    region.layoutYProperty().removeListener(regionListener);
                    region.insetsProperty().removeListener(regionListener);
                }
                region = new Pane();
                region.widthProperty().addListener(regionListener);
                region.heightProperty().addListener(regionListener);
                region.layoutXProperty().addListener(regionListener);
                region.layoutYProperty().addListener(regionListener);
                region.insetsProperty().addListener(regionListener);

                background.setCenter(region);
                region.setPrefSize(300, 200);
                Method test = tests.get(param);
                try {
                    test.invoke(RegionUITestBase.this, region);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                label.setText("Test: " + test.getName() + "\nStyle=" + (region.getStyle().replace(";", ";\n\t")));
                return parent;
            }
        });

        Scene scene = new Scene(pagination);
        stage.setScene(scene);
        stage.show();
    }

}
