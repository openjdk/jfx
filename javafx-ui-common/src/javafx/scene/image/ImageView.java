/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.WritableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.css.StyleableObjectProperty;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.StyleableStringProperty;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGImageView;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.Toolkit;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.beans.DefaultProperty;
import javafx.beans.property.*;

/**
 * The {@code ImageView} is a {@code Node} used for painting images loaded with
 * {@link Image} class.
 *
 * <p>
 * This class allows resizing the displayed image (with or without preserving
 * the original aspect ratio) and specifying a viewport into the source image
 * for restricting the pixels displayed by this {@code ImageView}.
 * </p>
 *
 *
 * <p>
 * Example code for displaying images
 * </p>
 *
 * <pre>
 * <code>
 * import javafx.application.Application;
 * import javafx.geometry.Rectangle2D;
 * import javafx.scene.Group;
 * import javafx.scene.Scene; 
 * import javafx.scene.image.Image;
 * import javafx.scene.image.ImageView;
 * import javafx.scene.layout.HBox;
 * import javafx.scene.paint.Color;
 * import javafx.stage.Stage; 
 *
 * public class HelloMenu extends Application {
 * 
 *     &#64;Override public void start(Stage stage) {
 *         // load the image
 *         Image image = new Image("flower.png");
 * 
 *         // simple displays ImageView the image as is
 *         ImageView iv1 = new ImageView();
 *         iv1.setImage(image);
 * 
 *         // resizes the image to have width of 100 while preserving the ratio and using
 *         // higher quality filtering method; this ImageView is also cached to
 *         // improve performance
 *         ImageView iv2 = new ImageView();
 *         iv2.setImage(image);
 *         iv2.setFitWidth(100);
 *         iv2.setPreserveRatio(true);
 *         iv2.setSmooth(true);
 *         iv2.setCache(true);
 *
 *         // defines a viewport into the source image (achieving a "zoom" effect) and
 *         // displays it rotated
 *         ImageView iv3 = new ImageView();
 *         iv3.setImage(image);
 *         Rectangle2D viewportRect = new Rectangle2D(40, 35, 110, 110);
 *         iv3.setViewport(viewportRect);
 *         iv3.setRotate(90);
 *
 *         Group root = new Group();
 *         Scene scene = new Scene(root);
 *         scene.setFill(Color.BLACK);
 *         HBox box = new HBox();
 *         box.getChildren().add(iv1);
 *         box.getChildren().add(iv2);
 *         box.getChildren().add(iv3);
 *         root.getChildren().add(box);
 * 
 *         stage.setTitle("ImageView");
 *         stage.setWidth(415);
 *         stage.setHeight(200);
 *         stage.setScene(scene); 
 *         stage.sizeToScene(); 
 *         stage.show(); 
 *     }
 *
 *     public static void main(String[] args) {
 *         Application.launch(args);
 *     }
 * }
 * </code>
 * </pre>
 * <p>
 * The code above produces the following:
 * </p>
 * <p>
 * <img src="doc-files/imageview.png"/>
 * </p>
 */
@DefaultProperty("image")
public class ImageView extends Node {

    /**
     * Allocates a new ImageView object.
     */
    public ImageView() {}

    /**
     * Allocates a new ImageView object with image loaded from the specified
     * URL.
     * <p>
     * The {@code new ImageView(url)} has the same effect as
     * {@code new ImageView(new Image(url))}.
     * </p>
     *
     * @param url the string representing the URL from which to load the image
     * @since JavaFX 2.1
     */
    public ImageView(String url) {
        this(new Image(url));
    }

    /**
     * Allocates a new ImageView object using the given image.
     * 
     * @param image Image that this ImageView uses
     */
    public ImageView(Image image) {
        setImage(image);
    }

    /**
     * The {@link Image} to be painted by this {@code ImageView}.
     *
     * @defaultvalue null
     */
    private ObjectProperty<Image> image;

    public final void setImage(Image value) {
        imageProperty().set(value);
    }
    public final Image getImage() {
        return image == null ? null : image.get();
    }

    private Image oldImage;
    public final ObjectProperty<Image> imageProperty() {
        if (image == null) {
            image = new ObjectPropertyBase<Image>() {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();
                    boolean dimensionChanged = _image == null || oldImage == null ||
                                                (oldImage.getWidth() != _image.getWidth() ||
                                                oldImage.getHeight() != _image.getHeight());

                    if (needsListeners) {
                        oldImage.impl_platformImageProperty().removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (_image.isAnimation() || _image.getProgress() < 1);
                    oldImage = _image;

                    if (needsListeners) {
                        _image.impl_platformImageProperty().addListener(platformImageChangeListener.getWeakListener());
                    }
                    if (dimensionChanged) {
                        impl_geomChanged();
                    }
                    impl_markDirty(DirtyBits.NODE_CONTENTS);
                }
                
                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "image";
                }
            };
        }
        return image;
    }

    private StringProperty imageUrl = null;
    /**
     * The imageUrl property is set from CSS and then the image property is
     * set from the invalidated method. This ensures that the same image isn't
     * reloaded. 
     */
    private StringProperty imageUrlProperty() {
        if (imageUrl == null) {
            imageUrl = new StyleableStringProperty() {

                @Override
                protected void invalidated() {

                    String imageUrl = null;
                    if (get() != null) {
                        URL url = null;
                        try {
                            url = new URL(get());
                        } catch (MalformedURLException malf) {
                            // This may be a relative URL, so try resolving
                            // it using the application classloader
                            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                            url = cl.getResource(get());
                        }
                        if (url != null) {
                            setImage(new Image(url.toExternalForm())); 
                        }
                    } else {
                        setImage(null);
                    }                    
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "imageUrl";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.IMAGE;
                }
                
            };
        }
        return imageUrl;
    }    

    private final AbstractNotifyListener platformImageChangeListener =
            new AbstractNotifyListener() {
        @Override
        public void invalidated(Observable valueModel) {
            impl_markDirty(DirtyBits.NODE_CONTENTS);
            impl_geomChanged();
        }
    };
    /**
     * The current x coordinate of the {@code ImageView} origin.
     *
     * @defaultvalue 0
     */
    private DoubleProperty x;


    public final void setX(double value) {
        xProperty().set(value);
    }

    public final double getX() {
        return x == null ? 0.0 : x.get();
    }

    public final DoubleProperty xProperty() {
        if (x == null) {
            x = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "x";
                }
            };
        }
        return x;
    }

    /**
     * The current y coordinate of the {@code ImageView} origin.
     *
     * @defaultvalue 0
     */
    private DoubleProperty y;


    public final void setY(double value) {
        yProperty().set(value);
    }

    public final double getY() {
        return y == null ? 0.0 : y.get();
    }

    public final DoubleProperty yProperty() {
        if (y == null) {
            y = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "y";
                }
            };
        }
        return y;
    }

    /**
     * The width of the bounding box within which the source image is resized as
     * necessary to fit. If set to a value <= 0, then the intrinsic width of the
     * image will be used as the {@code fitWidth}.
     * <p/>
     * See {@link #preserveRatio} for information on interaction between image
     * view's {@code fitWidth}, {@code fitHeight} and {@code preserveRatio}
     * attributes.
     *
     * @defaultvalue 0
     */
    private DoubleProperty fitWidth;


    public final void setFitWidth(double value) {
        fitWidthProperty().set(value);
    }

    public final double getFitWidth() {
        return fitWidth == null ? 0.0 : fitWidth.get();
    }

    public final DoubleProperty fitWidthProperty() {
        if (fitWidth == null) {
            fitWidth = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_VIEWPORT);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "fitWidth";
                }
            };
        }
        return fitWidth;
    }

    /**
     * The height of the bounding box within which the source image is resized
     * as necessary to fit. If set to a value <= 0, then the intrinsic height of
     * the image will be used as the {@code fitHeight}.
     * <p>
     * See {@link #preserveRatio} for information on interaction between image
     * view's {@code fitWidth}, {@code fitHeight} and {@code preserveRatio}
     * attributes.
     * </p>
     *
     * @defaultvalue 0
     */
    private DoubleProperty fitHeight;


    public final void setFitHeight(double value) {
        fitHeightProperty().set(value);
    }

    public final double getFitHeight() {
        return fitHeight == null ? 0.0 : fitHeight.get();
    }

    public final DoubleProperty fitHeightProperty() {
        if (fitHeight == null) {
            fitHeight = new DoublePropertyBase() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_VIEWPORT);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "fitHeight";
                }
            };
        }
        return fitHeight;
    }

    /**
     * Indicates whether to preserve the aspect ratio of the source image when
     * scaling to fit the image within the fitting bounding box.
     * <p/>
     * If set to {@code true}, it affects the dimensions of this
     * {@code ImageView} in the following way *
     * <ul>
     * <li>If only {@code fitWidth} is set, height is scaled to preserve ratio
     * <li>If only {@code fitHeight} is set, width is scaled to preserve ratio
     * <li>If both are set, they both may be scaled to get the best fit in a
     * width by height rectangle while preserving the original aspect ratio
     * </ul>
     *
     * If unset or set to {@code false}, it affects the dimensions of this
     * {@code ImageView} in the following way *
     * <ul>
     * <li>If only {@code fitWidth} is set, image's view width is scaled to
     * match and height is unchanged;
     * <li>If only {@code fitHeight} is set, image's view height is scaled to
     * match and height is unchanged;
     * <li>If both are set, the image view is scaled to match both.
     * </ul>
     * </p>
     * Note that the dimensions of this node as reported by the node's bounds
     * will be equal to the size of the scaled image and is guaranteed to be
     * contained within {@code fitWidth x fitHeight} bonding box.
     *
     * @defaultvalue false
     */
    private BooleanProperty preserveRatio;


    public final void setPreserveRatio(boolean value) {
        preserveRatioProperty().set(value);
    }

    public final boolean isPreserveRatio() {
        return preserveRatio == null ? false : preserveRatio.get();
    }

    public final BooleanProperty preserveRatioProperty() {
        if (preserveRatio == null) {
            preserveRatio = new BooleanPropertyBase() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_VIEWPORT);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "preserveRatio";
                }
            };
        }
        return preserveRatio;
    }

    /**
     * Indicates whether to use a better quality filtering algorithm or a faster
     * one when transforming or scaling the source image to fit within the
     * bounding box provided by {@code fitWidth} and {@code fitHeight}.
     *
     * <p>
     * If set to {@code true} a better quality filtering will be used, if set to
     * {@code false} a faster but lesser quality filtering will be used.
     * </p>
     *
     * <p>
     * The default value depends on platform configuration.
     * </p>
     *
     * @defaultvalue platform-dependent
     */
    private BooleanProperty smooth;


    public final void setSmooth(boolean value) {
        smoothProperty().set(value);
    }

    public final boolean isSmooth() {
        return smooth == null ? SMOOTH_DEFAULT : smooth.get();
    }

    public final BooleanProperty smoothProperty() {
        if (smooth == null) {
            smooth = new BooleanPropertyBase(SMOOTH_DEFAULT) {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_SMOOTH);
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "smooth";
                }
            };
        }
        return smooth;
    }

    /**
     * Platform-dependent default value of the {@link #smoothProperty smooth} property.
     */
    public static final boolean SMOOTH_DEFAULT = Toolkit.getToolkit()
            .getDefaultImageSmooth();
    /**
     * The rectangular viewport into the image. The viewport is specified in the
     * coordinates of the image, prior to scaling or any other transformations.
     *
     * <p>
     * If {@code viewport} is {@code null}, the entire image is displayed. If
     * {@code viewport} is non-{@code null}, only the portion of the image which
     * falls within the viewport will be displayed. If the image does not fully
     * cover the viewport then any remaining area of the viewport will be empty.
     * </p>
     *
     * @defaultvalue null
     */
    private ObjectProperty<Rectangle2D> viewport;


    public final void setViewport(Rectangle2D value) {
        viewportProperty().set(value);
    }

    public final Rectangle2D getViewport() {
        return viewport == null ? null : viewport.get();
    }

    public final ObjectProperty<Rectangle2D> viewportProperty() {
        if (viewport == null) {
            viewport = new ObjectPropertyBase<Rectangle2D>() {

                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_VIEWPORT);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return ImageView.this;
                }

                @Override
                public String getName() {
                    return "viewport";
                }
            };
        }
        return viewport;
    }

    // Need to track changes to image width and image height and recompute
    // bounds when changed.
    // imageWidth = bind image.width on replace {
    // impl_geomChanged();
    // }
    //
    // imageHeight = bind image.height on replace {
    // impl_geomChanged();
    // }

    private double destWidth, destHeight;

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGImageView();
    }

    private PGImageView getPGImageView() {
        return (PGImageView) impl_getPGNode();
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {

        // need to figure out the width/height to use for computing bounds
        double w = 0;
        double h = 0;
        Image localImage = getImage();
        if (localImage != null) {
            w = localImage.getWidth();
            h = localImage.getHeight();
        }
        double localFitWidth = getFitWidth();
        double localFitHeight = getFitHeight();
        double newW = localFitWidth;
        double newH = localFitHeight;
        double vw = 0;
        double vh = 0;
        Rectangle2D localViewport = getViewport();
        if (localViewport != null) {
            vw = localViewport.getWidth();
            vh = localViewport.getHeight();
        }

        if (vw > 0 && vh > 0) {
            w = vw;
            h = vh;
        }

        if (localFitWidth <= 0.0 && localFitHeight <= 0.0) {
            newW = w;
            newH = h;
        } else if (isPreserveRatio()) {
            if (localFitWidth <= 0.0) {
                newW = (h > 0) ? w * (localFitHeight / h) : 0;
                newH = localFitHeight;
            } else if (localFitHeight <= 0.0) {
                newW = localFitWidth;
                newH = (w > 0) ? h * (localFitWidth / w) : 0;
            } else {
                if (w == 0.0)
                    w = localFitWidth;
                if (h == 0.0)
                    h = localFitHeight;
                double scale = Math.min(localFitWidth / w, localFitHeight / h);
                newW = w * scale;
                newH = h * scale;
            }
        } else if (getFitHeight() <= 0.0) {
            newH = h;
        } else if (getFitWidth() <= 0.0) {
            newW = w;
        }
        if (newH < 1) {
            newH = 1;
        }
        if (newW < 1) {
            newW = 1;
        }

        // Store these values for use later in impl_computeContains() to support
        // Node.contains().
        destWidth = newW;
        destHeight = newH;

        w = newW;
        h = newH;

//      TODO: the following block is never executed, remove or correct
//            newW = 1, newH = 1
//
//        // if the w or h are non-positive, then there is no size
//        // for the image view
//        if (w <= 0 || h <= 0) {
//            return bounds.invalidate();
//        }

        bounds = bounds.deriveWithNewBounds((float)getX(), (float)getY(), 0.0f,
                (float)(getX() + w), (float)(getY() + h), 0.0f);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {
        if (getImage() == null) {
            return false;
        }
        // Local Note bounds contain test is already done by the caller.
        // (Node.contains()).

        double dx = localX - getX();
        double dy = localY - getY();

        Image localImage = getImage();
        double srcWidth = localImage.getWidth();
        double srcHeight = localImage.getHeight();
        double viewWidth = srcWidth;
        double viewHeight = srcHeight;
        double vw = 0;
        double vh = 0;
        double vminx = 0;
        double vminy = 0;
        Rectangle2D localViewport = getViewport();
        if (localViewport != null) {
            vw = localViewport.getWidth();
            vh = localViewport.getHeight();
            vminx = localViewport.getMinX();
            vminy = localViewport.getMinY();
        }

        if (vw > 0 && vh > 0) {
            viewWidth = vw;
            viewHeight = vh;
        }

        // desWidth Note and destHeight are computed by impl_computeGeomBounds()
        // via a call from Node.contains() before calling
        // impl_computeContains().
        // Transform into image's coordinate system.
        dx = vminx + dx * viewWidth / destWidth;
        dy = vminy + dy * viewHeight / destHeight;
        // test whether it's inside the original image AND inside of viewport
        // (viewport may stick out from the image bounds)
        if (dx < 0.0 || dy < 0.0 || dx >= srcWidth || dy >= srcHeight ||
                dx < vminx || dy < vminy ||
                dx >= vminx + viewWidth || dy >= vminy + viewHeight) {
            return false;
        }
        // Do alpha test on the picked pixel.
        return Toolkit.getToolkit().imageContains(localImage.impl_getPlatformImage(), (float)dx, (float)dy);
    }

    /***************************************************************************
     * * Stylesheet Handling * *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatasprivate implementation detail
      */
     private static class StyleableProperties {
        // TODO
        // "preserve-ratio","smooth","viewport","fit-width","fit-height"
         private static final StyleableProperty<ImageView, String> IMAGE = 
            new StyleableProperty<ImageView,String>("-fx-image",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(ImageView n) {
                // Note that we care about the image, not imageUrl
                return n.image == null || !n.image.isBound();
            }

            @Override
            public WritableValue<String> getWritableValue(ImageView n) {
                return n.imageUrlProperty();
            }
        };
            
         private static final List<StyleableProperty> STYLEABLES;
         static {
            final List<StyleableProperty> styleables = 
		new ArrayList<StyleableProperty>(Node.impl_CSS_STYLEABLES());
            Collections.addAll(styleables, IMAGE);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh. StyleableProperties is referenced
     * no earlier (and therefore loaded no earlier by the class loader) than
     * the moment that  impl_CSS_STYLEABLES() is called.
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return ImageView.StyleableProperties.STYLEABLES;
    }


    void updateViewport() {

        if (getImage() == null || getImage().impl_getPlatformImage() == null) {
            return;
        }

        Rectangle2D localViewport = getViewport();
        if (localViewport != null) {
            getPGImageView().setViewport((float)getFitWidth(), (float)getFitHeight(),
                    (float)localViewport.getMinX(), (float)localViewport.getMinY(),
                    (float)localViewport.getWidth(), (float)localViewport.getHeight(), isPreserveRatio());
        } else {
            getPGImageView().setViewport((float)getFitWidth(), (float)getFitHeight(), 0, 0, 0, 0,
                    isPreserveRatio());
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_updatePG() {
        super.impl_updatePG();

        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            PGImageView peer = getPGImageView();
            peer.setX((float)getX());
            peer.setY((float)getY());
        }
        if (impl_isDirty(DirtyBits.NODE_SMOOTH)) {
            getPGImageView().setSmooth(isSmooth());
        }
        if (impl_isDirty(DirtyBits.NODE_VIEWPORT)) {
            updateViewport();
        }
        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            getPGImageView().setImage(getImage()!= null? getImage().impl_getPlatformImage():null);
            updateViewport();
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return alg.processLeafNode(this, ctx);
    }
}
