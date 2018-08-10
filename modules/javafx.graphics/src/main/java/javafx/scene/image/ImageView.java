/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.css.StyleManager;
import javafx.css.converter.URLConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.ImageViewHelper;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGImageView;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.DefaultProperty;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleableStringProperty;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * <pre><code>
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
 * public class HelloImageView extends Application {
 *
 *     {@literal @Override} public void start(Stage stage) {
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
 * </code></pre>
 * <p>
 * The code above produces the following:
 * </p>
 * <p>
 * <img src="doc-files/imageview.png" alt="A visual rendering of the ImageView example">
 * </p>
 * @since JavaFX 2.0
 */
@DefaultProperty("image")
public class ImageView extends Node {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        ImageViewHelper.setImageViewAccessor(new ImageViewHelper.ImageViewAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((ImageView) node).doCreatePeer();
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((ImageView) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
            BaseBounds bounds, BaseTransform tx) {
                return ((ImageView) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((ImageView) node).doComputeContains(localX, localY);
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        ImageViewHelper.initHelper(this);
    }
    /**
     * Allocates a new ImageView object.
     */
    public ImageView() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
    }

    /**
     * Allocates a new ImageView object with image loaded from the specified
     * URL.
     * <p>
     * The {@code new ImageView(url)} has the same effect as
     * {@code new ImageView(new Image(url))}.
     * </p>
     *
     * @param url the string representing the URL from which to load the image
     * @throws NullPointerException if URL is null
     * @throws IllegalArgumentException if URL is invalid or unsupported
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
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.IMAGE_VIEW);
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        setImage(image);
    }

    /**
     * The {@link Image} to be painted by this {@code ImageView}.
     *
     * @defaultValue null
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
                        Toolkit.getImageAccessor().getImageProperty(oldImage).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (_image.isAnimation() || _image.getProgress() < 1);
                    oldImage = _image;

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }
                    if (dimensionChanged) {
                        invalidateWidthHeight();
                        NodeHelper.geomChanged(ImageView.this);
                    }
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_CONTENTS);
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

                    final String imageUrl = get();
                    if (imageUrl != null) {
                        setImage(StyleManager.getInstance().getCachedImage(imageUrl));
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
                public CssMetaData<ImageView,String> getCssMetaData() {
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
            invalidateWidthHeight();
            NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_CONTENTS);
            NodeHelper.geomChanged(ImageView.this);
        }
    };
    /**
     * The current x coordinate of the {@code ImageView} origin.
     *
     * @defaultValue 0
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
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(ImageView.this);
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
     * @defaultValue 0
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
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_GEOMETRY);
                    NodeHelper.geomChanged(ImageView.this);
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
     * necessary to fit. If set to a value &lt;= 0, then the intrinsic width of the
     * image will be used as the {@code fitWidth}.
     * <p>
     * See {@link #preserveRatioProperty() preserveRatio} for information on interaction between image
     * view's {@code fitWidth}, {@code fitHeight} and {@code preserveRatio}
     * attributes.
     *
     * @defaultValue 0
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
                    invalidateWidthHeight();
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_VIEWPORT);
                    NodeHelper.geomChanged(ImageView.this);
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
     * as necessary to fit. If set to a value &lt;= 0, then the intrinsic height of
     * the image will be used as the {@code fitHeight}.
     * <p>
     * See {@link #preserveRatioProperty() preserveRatio} for information on interaction between image
     * view's {@code fitWidth}, {@code fitHeight} and {@code preserveRatio}
     * attributes.
     * </p>
     *
     * @defaultValue 0
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
                    invalidateWidthHeight();
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_VIEWPORT);
                    NodeHelper.geomChanged(ImageView.this);
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
     * <p>
     * If set to {@code true}, it affects the dimensions of this
     * {@code ImageView} in the following way
     * <ul>
     * <li>If only {@code fitWidth} is set, height is scaled to preserve ratio
     * <li>If only {@code fitHeight} is set, width is scaled to preserve ratio
     * <li>If both are set, they both may be scaled to get the best fit in a
     * width by height rectangle while preserving the original aspect ratio
     * </ul>
     *
     * If unset or set to {@code false}, it affects the dimensions of this
     * {@code ImageView} in the following way
     * <ul>
     * <li>If only {@code fitWidth} is set, image's view width is scaled to
     * match and height is unchanged;
     * <li>If only {@code fitHeight} is set, image's view height is scaled to
     * match and height is unchanged;
     * <li>If both are set, the image view is scaled to match both.
     * </ul>
     *
     * Note that the dimensions of this node as reported by the node's bounds
     * will be equal to the size of the scaled image and is guaranteed to be
     * contained within {@code fitWidth x fitHeight} bonding box.
     *
     * @defaultValue false
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
                    invalidateWidthHeight();
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_VIEWPORT);
                    NodeHelper.geomChanged(ImageView.this);
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
     * @defaultValue platform-dependent
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
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_SMOOTH);
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
     * Platform-dependent default value of the {@link #smoothProperty() smooth} property.
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
     * @defaultValue null
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
                    invalidateWidthHeight();
                    NodeHelper.markDirty(ImageView.this, DirtyBits.NODE_VIEWPORT);
                    NodeHelper.geomChanged(ImageView.this);
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
    // NodeHelper.geomChanged(ImageView.this);
    // }
    //
    // imageHeight = bind image.height on replace {
    // NodeHelper.geomChanged(ImageView.this);
    // }

    private double destWidth, destHeight;

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGImageView();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        recomputeWidthHeight();

        bounds = bounds.deriveWithNewBounds((float)getX(), (float)getY(), 0.0f,
                (float)(getX() + destWidth), (float)(getY() + destHeight), 0.0f);
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    private boolean validWH;

    private void invalidateWidthHeight() {
        validWH = false;
    }

    private void recomputeWidthHeight() {
        if (validWH) {
            return;
        }
        Image localImage = getImage();
        Rectangle2D localViewport = getViewport();

        double w = 0;
        double h = 0;
        if (localViewport != null && localViewport.getWidth() > 0 && localViewport.getHeight() > 0) {
            w = localViewport.getWidth();
            h = localViewport.getHeight();
        } else if (localImage != null) {
            w = localImage.getWidth();
            h = localImage.getHeight();
        }

        double localFitWidth = getFitWidth();
        double localFitHeight = getFitHeight();

        if (isPreserveRatio() && w > 0 && h > 0 && (localFitWidth > 0 || localFitHeight > 0)) {
            if (localFitWidth <= 0 || (localFitHeight > 0 && localFitWidth * h > localFitHeight * w)) {
                w = w * localFitHeight / h;
                h = localFitHeight;
            } else {
                h = h * localFitWidth / w;
                w = localFitWidth;
            }
        } else {
            if (localFitWidth > 0f) {
                w = localFitWidth;
            }
            if (localFitHeight > 0f) {
                h = localFitHeight;
            }
        }

        // Store these values for use later in doComputeContains() to support
        // Node.contains().
        destWidth = w;
        destHeight = h;

        validWH = true;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        if (getImage() == null) {
            return false;
        }

        recomputeWidthHeight();
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

        // desWidth Note and destHeight are computed by NodeHelper.computeGeomBounds()
        // via a call from Node.contains() before calling
        // doComputeContains().
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
        return Toolkit.getToolkit().imageContains(
                Toolkit.getImageAccessor().getPlatformImage(localImage), (float)dx, (float)dy);
    }

    /***************************************************************************
     * * Stylesheet Handling * *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "image-view";

    /*
     * Super-lazy instantiation pattern from Bill Pugh.
     */
     private static class StyleableProperties {
        // TODO
        // "preserve-ratio","smooth","viewport","fit-width","fit-height"
         private static final CssMetaData<ImageView, String> IMAGE =
            new CssMetaData<ImageView,String>("-fx-image",
                URLConverter.getInstance()) {

            @Override
            public boolean isSettable(ImageView n) {
                // Note that we care about the image, not imageUrl
                return n.image == null || !n.image.isBound();
            }

            @Override
            public StyleableProperty<String> getStyleableProperty(ImageView n) {
                return (StyleableProperty<String>)n.imageUrlProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Node.getClassCssMetaData());
            styleables.add(IMAGE);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    void updateViewport() {
        recomputeWidthHeight();
        if (getImage() == null || Toolkit.getImageAccessor().getPlatformImage(getImage()) == null) {
            return;
        }

        Rectangle2D localViewport = getViewport();
        final NGImageView peer = NodeHelper.getPeer(this);
        if (localViewport != null) {
            peer.setViewport((float)localViewport.getMinX(), (float)localViewport.getMinY(),
                    (float)localViewport.getWidth(), (float)localViewport.getHeight(),
                    (float)destWidth, (float)destHeight);
        } else {
            peer.setViewport(0, 0, 0, 0, (float)destWidth, (float)destHeight);
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        final NGImageView peer = NodeHelper.getPeer(this);
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            peer.setX((float)getX());
            peer.setY((float)getY());
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_SMOOTH)) {
            peer.setSmooth(isSmooth());
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            peer.setImage(getImage() != null
                    ? Toolkit.getImageAccessor().getPlatformImage(getImage()) : null);
        }
        // The NG part expects this to be called when image changes
        if (NodeHelper.isDirty(this, DirtyBits.NODE_VIEWPORT) || NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            updateViewport();
        }
    }
}
