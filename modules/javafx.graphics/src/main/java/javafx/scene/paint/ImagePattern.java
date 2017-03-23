/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.paint;

import javafx.beans.NamedArg;
import javafx.scene.image.Image;
import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.tk.Toolkit;

/**
 * <p>The {@code ImagePattern} class fills a shape with an image pattern. The
 * user may specify the anchor rectangle, which defines the position,
 * width, and height of the image relative to the upper left corner of the
 * shape. If the shape extends out of the anchor rectangle, the image is tiled.
 * </p>
 *
 * <p>If the {@code proportional} variable is set to true (the default)
 * then the anchor rectangle should be specified relative to the unit
 * square (0.0-&gt;1.0) and will be stretched across the shape.
 * If the {@code proportional} variable is set to false, then the anchor
 * rectangle should be specified in the local coordinate system of the shape
 * and the image will be stretched to fit the anchor rectangle. The anchor
 * rectangle will not be stretched across the shape.</p>
 *
 * <p>The example below demonstrates the use of the {@code proportional}
 * variable.  The shapes on the top row use proportional coordinates
 * (the default) to specify the anchor rectangle.  The shapes on the
 * bottom row use absolute coordinates.  The flower image is stretched
 * to fill the entire triangle shape, while the dot pattern image is tiled
 * within the circle shape.</p>
 *
<pre><code>
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

public class HelloImagePattern extends Application {

    private static final String flowerURL = "file:flower.png";
    private static final String dotsURL = "file:dots.png";

    {@literal @Override public void start(Stage stage)} {
        stage.setTitle("Image Pattern");
        Group root = new Group();
        Scene scene = new Scene(root, 600, 450);

        Image dots = new Image(dotsURL);
        Image flower = new Image(flowerURL);

        Polygon p = new Polygon();

        p.setLayoutX(10);
        p.setLayoutY(10);
        p.getPoints().add(50.0);
        p.getPoints().add(0.0);
        p.getPoints().add(100.0);
        p.getPoints().add(100.0);
        p.getPoints().add(0.0);
        p.getPoints().add(100.0);

        p.setFill(new ImagePattern(flower, 0, 0, 1, 1, true));

        root.getChildren().add(p);

        Polygon p2 = new Polygon();

        p2.setLayoutX(10);
        p2.setLayoutY(120);
        p2.getPoints().add(50.0);
        p2.getPoints().add(0.0);
        p2.getPoints().add(100.0);
        p2.getPoints().add(100.0);
        p2.getPoints().add(0.0);
        p2.getPoints().add(100.0);

        p2.setFill(new ImagePattern(flower, 0, 0, 100, 100, false));

        root.getChildren().add(p2);

        Circle circ = new Circle(50);
        circ.setTranslateX(120);
        circ.setTranslateY(10);
        circ.setCenterX(50);
        circ.setCenterY(50);
        circ.setFill(new ImagePattern(dots, 0.2, 0.2, 0.4, 0.4, true));

        root.getChildren().add(circ);

        Circle circ2 = new Circle(50);
        circ2.setTranslateX(120);
        circ2.setTranslateY(10);
        circ2.setCenterX(50);
        circ2.setCenterY(50);
        circ2.setFill(new ImagePattern(dots, 20, 20, 40, 40, false));

        root.getChildren().add(circ2);
        stage.setScene(scene);
        stage.show();
    }
</code></pre>
 * <p>The code above produces the following:</p>
 * <p><img src="doc-files/ImagePattern.png" alt="A visual rendering of the
 * HelloImagePattern example"></p>
 *
 * @since JavaFX 2.2
 */
public final class ImagePattern extends Paint {

    private Image image;

    /**
     * Gets the image to be used as a paint.
     *
     * @return Image to be used as a paint.
     */
    public final Image getImage() {
        return image;
    }


    private double x;

    /**
     * Gets the x origin of the anchor rectangle.
     *
     * @defaultValue 0.0
     * @return The x origin of the anchor rectangle.
     */
    public final double getX() {
        return x;
    }

    private double y;

    /**
     * Gets the y origin of the anchor rectangle.
     *
     * @defaultValue 0.0
     * @return The y origin of the anchor rectangle.
     */
    public final double getY() {
        return y;
    }


    private double width = 1f;

    /**
     * Gets the width of the anchor rectangle.
     *
     * @defaultValue 1.0
     * @return The width of the anchor rectangle.
     */
    public final double getWidth() {
        return width;
    }


    private double height = 1f;

    /**
     * Gets the height of the anchor rectangle.
     *
     * @defaultValue 1.0
     * @return The height of the anchor rectangle.
     */
    public final double getHeight() {
        return height;
    }


    private boolean proportional = true;

    /**
     * Gets a boolean that indicates whether start and end locations are
     * proportional or absolute. If this flag is true, the two end points are
     * defined in a coordinate space where coordinates in the range
     * {@code [0..1]} are scaled to map onto the bounds of the shape that the
     * pattern fills. If this flag is false, then the coordinates are specified
     * in the local coordinate system of the node.
     *
     * @defaultValue true
     * @return boolean that is true if this paint is proportional.
     */
    public final boolean isProportional() {
        return proportional;
    }

    @Override public final boolean isOpaque() {
        // We only ever have Prism as our painting system these days, so we can just
        // cast this platformPaint to a prism paint type and check for isOpaque.
        // It would be better to change the type on platformPaint accordingly and
        // ditch the cast.
        return ((com.sun.prism.paint.ImagePattern)acc_getPlatformPaint()).isOpaque();
    }

    private Object platformPaint;

    /**
     * Creates a new instance of ImagePattern from the specified image. Default
     * values are used for all other parameters.
     *
     * @param image the image to be used as the paint.
     * @throws NullPointerException if the image is null.
     * @throws IllegalArgumentException if image is not done loading,
     * that is if progress is &lt; 1.
     */
    public ImagePattern(@NamedArg("image") Image image) {
        if (image == null) {
            throw new NullPointerException("Image must be non-null.");
        } else if (image.getProgress() < 1.0) {
            throw new IllegalArgumentException("Image not yet loaded");
        }
        this.image = image;
    }

    /**
     * Creates a new instance of ImagePattern.
     *
     * @param image the image to be used as the paint.
     * @param x the x origin of the anchor rectangle.
     * @param y the y origin of the anchor rectangle.
     * @param width the width of the anchor rectangle.
     * @param height the height of the anchor rectangle.
     * @param proportional whether the coordinates are proportional
     * to the shape which ImagePattern fills
     * @throws NullPointerException if the image is null.
     * @throws IllegalArgumentException if image is not done loading,
     * that is if progress is &lt; 1.
     */
    public ImagePattern(@NamedArg("image") Image image, @NamedArg("x") double x, @NamedArg("y") double y, @NamedArg("width") double width,
            @NamedArg("height") double height, @NamedArg("proportional") boolean proportional) {

        if (image == null) {
            throw new NullPointerException("Image must be non-null.");
        } else if (image.getProgress() < 1.0) {
            throw new IllegalArgumentException("Image not yet loaded");
        }
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.proportional = proportional;
    }

    @Override
    boolean acc_isMutable() {
        return Toolkit.getImageAccessor().isAnimation(image);
    }

    @Override
    void acc_addListener(AbstractNotifyListener platformChangeListener) {
        Toolkit.getImageAccessor().getImageProperty(image)
                .addListener(platformChangeListener);
    }

    @Override
    void acc_removeListener(AbstractNotifyListener platformChangeListener) {
        Toolkit.getImageAccessor().getImageProperty(image)
                .removeListener(platformChangeListener);
    }

    @Override Object acc_getPlatformPaint() {
        if (acc_isMutable() || platformPaint == null) {
            platformPaint = Toolkit.getToolkit().getPaint(this);
            assert platformPaint != null;
        }
        return platformPaint;
    }
}
