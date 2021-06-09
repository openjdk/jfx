/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoublePropertyBase;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.DataURI;
import javafx.animation.Interpolator;
import javafx.animation.KeyValue;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * The {@code Image} class represents graphical images and is used for loading
 * images from a specified URL.
 *
 * <p>
 * Supported image formats are:
 * <ul>
 * <li><a href="http://msdn.microsoft.com/en-us/library/dd183376(v=vs.85).aspx">BMP</a></li>
 * <li><a href="http://www.w3.org/Graphics/GIF/spec-gif89a.txt">GIF</a></li>
 * <li><a href="http://www.ijg.org">JPEG</a></li>
 * <li><a href="http://www.libpng.org/pub/png/spec/">PNG</a></li>
 * </ul>
 *
 * <p>
 * Images can be resized as they are loaded (for example to reduce the amount of
 * memory consumed by the image). The application can specify the quality of
 * filtering used when scaling, and whether or not to preserve the original
 * image's aspect ratio.
 *
 * <p>If a URL string is passed to a constructor, it be any of the following:
 * <ol>
 *     <li>the name of a resource that can be resolved by the context
 *         {@link ClassLoader} for this thread
 *     <li>a file path that can be resolved by {@link java.io.File}
 *     <li>a URL that can be resolved by {@link java.net.URL} and for
 *         which a protocol handler exists
 * </ol>
 *
 * <p>The RFC 2397 "data" scheme for URLs is supported in addition to
 * the protocol handlers that are registered for the application.
 * If a URL uses the "data" scheme, the data must be base64-encoded
 * and the MIME type must either be empty or a subtype of the
 * {@code image} type.
 *
 * <p>Use {@link ImageView} for displaying images loaded with this
 * class. The same {@code Image} instance can be displayed by multiple
 * {@code ImageView}s.
 *
 *<p>Example code for loading images:

<PRE>
import javafx.scene.image.Image;

// load an image in background, displaying a placeholder while it's loading
// (assuming there's an ImageView node somewhere displaying this image)
// The image is located in default package of the classpath
Image image1 = new Image("/flower.png", true);

// load an image and resize it to 100x150 without preserving its original
// aspect ratio
// The image is located in my.res package of the classpath
Image image2 = new Image("my/res/flower.png", 100, 150, false, false);

// load an image and resize it to width of 100 while preserving its
// original aspect ratio, using faster filtering method
// The image is downloaded from the supplied URL through http protocol
Image image3 = new Image("http://sample.com/res/flower.png", 100, 0, false, false);

// load an image and resize it only in one dimension, to the height of 100 and
// the original width, without preserving original aspect ratio
// The image is located in the current working directory
Image image4 = new Image("file:flower.png", 0, 100, false, false);

</PRE>
 * @since JavaFX 2.0
 */
public class Image {

    static {
        Toolkit.setImageAccessor(new Toolkit.ImageAccessor() {

            @Override
            public boolean isAnimation(Image image) {
                return image.isAnimation();
            }

            @Override
            public ReadOnlyObjectProperty<PlatformImage>
                    getImageProperty(Image image)
            {
                return image.acc_platformImageProperty();
            }

            @Override
            public int[] getPreColors(PixelFormat<ByteBuffer> pf) {
                return ((PixelFormat.IndexedPixelFormat) pf).getPreColors();
            }

            @Override
            public int[] getNonPreColors(PixelFormat<ByteBuffer> pf) {
                return ((PixelFormat.IndexedPixelFormat) pf).getNonPreColors();
            }

            @Override
            public  Object getPlatformImage(Image image) {
                return image.getPlatformImage();
            }

            @Override
            public Image fromPlatformImage(Object image) {
                return Image.fromPlatformImage(image);
            }
        });
    }

    // Matches strings that start with a valid URI scheme
    private static final Pattern URL_QUICKMATCH = Pattern.compile("^\\p{Alpha}[\\p{Alnum}+.-]*:.*$");
    /**
     * The string representing the URL to use in fetching the pixel data.
     *
     * @defaultValue empty string
     */
    private final String url;

    /**
     * Returns the url used to fetch the pixel data contained in the Image instance,
     * if specified in the constructor. If no url is provided in the constructor (for
     * instance, if the Image is constructed from an
     * {@link #Image(InputStream) InputStream}), this method will return null.
     *
     * @return a String containing the URL used to fetch the pixel data for this
     *      Image instance.
     * @since 9
     */
    public final String getUrl() {
        return url;
    }

    private final InputStream inputSource;

    final InputStream getInputSource() {
        return inputSource;
    }

    /**
     * The approximate percentage of image's loading that
     * has been completed. A positive value between 0 and 1 where 0 is 0% and 1
     * is 100%.
     *
     * @defaultValue 0
     */
    private ReadOnlyDoubleWrapper progress;


    /**
     * This is package private *only* for the sake of testing. We need a way to feed fake progress
     * values. It would be better if Image were refactored to be testable (for example, by allowing
     * the test code to provide its own implementation of background loading), but this is a simpler
     * and safer change for now.
     *
     * @param value should be 0-1.
     */
    final void setProgress(double value) {
        progressPropertyImpl().set(value);
    }

    public final double getProgress() {
        return progress == null ? 0.0 : progress.get();
    }

    public final ReadOnlyDoubleProperty progressProperty() {
        return progressPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyDoubleWrapper progressPropertyImpl() {
        if (progress == null) {
            progress = new ReadOnlyDoubleWrapper(this, "progress");
        }
        return progress;
    }
    // PENDING_DOC_REVIEW
    /**
     * The width of the bounding box within which the source image is
     * resized as necessary to fit. If set to a value {@code <= 0}, then the
     * intrinsic width of the image will be used.
     * <p/>
     * See {@link #isPreserveRatio() preserveRatio} for information on interaction between image's
     * {@code requestedWidth}, {@code requestedHeight} and {@code preserveRatio}
     * attributes.
     *
     * @defaultValue 0
     */
    private final double requestedWidth;

    /**
     * Gets the width of the bounding box within which the source image is
     * resized as necessary to fit. If set to a value {@code <= 0}, then the
     * intrinsic width of the image will be used.
     * <p>
     * See {@link #isPreserveRatio() preserveRatio} for information on interaction between image's
     * {@code requestedWidth}, {@code requestedHeight} and {@code preserveRatio}
     * attributes.
     *
     * @return The requested width
     */
    public final double getRequestedWidth() {
        return requestedWidth;
    }
    // PENDING_DOC_REVIEW
    /**
     * The height of the bounding box within which the source image is
     * resized as necessary to fit. If set to a value {@code <= 0}, then the
     * intrinsic height of the image will be used.
     * <p>
     * See {@link #isPreserveRatio() preserveRatio} for information on interaction between image's
     * {@code requestedWidth}, {@code requestedHeight} and {@code preserveRatio}
     * attributes.
     *
     * @defaultValue 0
     */
    private final double requestedHeight;

    /**
     * Gets the height of the bounding box within which the source image is
     * resized as necessary to fit. If set to a value {@code <= 0}, then the
     * intrinsic height of the image will be used.
     * <p>
     * See {@link #isPreserveRatio() preserveRatio} for information on interaction between image's
     * {@code requestedWidth}, {@code requestedHeight} and {@code preserveRatio}
     * attributes.
     *
     * @return The requested height
     */
    public final double getRequestedHeight() {
        return requestedHeight;
    }
    // PENDING_DOC_REVIEW
    /**
     * The image width or {@code 0} if the image loading fails. While the image
     * is being loaded it is set to {@code 0}.
     */
    private DoublePropertyImpl width;

    public final double getWidth() {
        return width == null ? 0.0 : width.get();
    }

    public final ReadOnlyDoubleProperty widthProperty() {
        return widthPropertyImpl();
    }

    private DoublePropertyImpl widthPropertyImpl() {
        if (width == null) {
            width = new DoublePropertyImpl("width");
        }

        return width;
    }

    private final class DoublePropertyImpl extends ReadOnlyDoublePropertyBase {
        private final String name;

        private double value;

        public DoublePropertyImpl(final String name) {
            this.name = name;
        }

        public void store(final double value) {
            this.value = value;
        }

        @Override
        public void fireValueChangedEvent() {
            super.fireValueChangedEvent();
        }

        @Override
        public double get() {
            return value;
        }

        @Override
        public Object getBean() {
            return Image.this;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    // PENDING_DOC_REVIEW
    /**
     * The image height or {@code 0} if the image loading fails. While the image
     * is being loaded it is set to {@code 0}.
     */
    private DoublePropertyImpl height;

    public final double getHeight() {
        return height == null ? 0.0 : height.get();
    }

    public final ReadOnlyDoubleProperty heightProperty() {
        return heightPropertyImpl();
    }

    private DoublePropertyImpl heightPropertyImpl() {
        if (height == null) {
            height = new DoublePropertyImpl("height");
        }

        return height;
    }

    /**
     * Indicates whether to preserve the aspect ratio of the original image
     * when scaling to fit the image within the bounding box provided by
     * {@code width} and {@code height}.
     * <p>
     * If set to {@code true}, it affects the dimensions of this {@code Image}
     * in the following way:
     * <ul>
     *  <li> If only {@code width} is set, height is scaled to preserve ratio
     *  <li> If only {@code height} is set, width is scaled to preserve ratio
     *  <li> If both are set, they both may be scaled to get the best fit in a
     *  width by height rectangle while preserving the original aspect ratio
     * </ul>
     * The reported {@code width} and {@code height} may be different from the
     * initially set values if they needed to be adjusted to preserve aspect
     * ratio.
     *
     * If unset or set to {@code false}, it affects the dimensions of this
     * {@code ImageView} in the following way:
     * <ul>
     *  <li> If only {@code width} is set, the image's width is scaled to
     *  match and height is unchanged;
     *  <li> If only {@code height} is set, the image's height is scaled to
     *  match and height is unchanged;
     *  <li> If both are set, the image is scaled to match both.
     * </ul>
     *
     * @defaultValue false
     */
    private final boolean preserveRatio;

    /**
     * Indicates whether to preserve the aspect ratio of the original image
     * when scaling to fit the image within the bounding box provided by
     * {@code width} and {@code height}.
     * <p>
     * If set to {@code true}, it affects the dimensions of this {@code Image}
     * in the following way:
     * <ul>
     *  <li> If only {@code width} is set, height is scaled to preserve ratio
     *  <li> If only {@code height} is set, width is scaled to preserve ratio
     *  <li> If both are set, they both may be scaled to get the best fit in a
     *  width by height rectangle while preserving the original aspect ratio
     * </ul>
     * The reported {@code width} and {@code height} may be different from the
     * initially set values if they needed to be adjusted to preserve aspect
     * ratio.
     *
     * If unset or set to {@code false}, it affects the dimensions of this
     * {@code ImageView} in the following way:
     * <ul>
     *  <li> If only {@code width} is set, the image's width is scaled to
     *  match and height is unchanged;
     *  <li> If only {@code height} is set, the image's height is scaled to
     *  match and height is unchanged;
     *  <li> If both are set, the image is scaled to match both.
     * </ul>
     *
     * @return true if the aspect ratio of the original image is to be
     *               preserved when scaling to fit the image within the bounding
     *               box provided by {@code width} and {@code height}.
     */
    public final boolean isPreserveRatio() {
        return preserveRatio;
    }

    /**
     * Indicates whether to use a better quality filtering algorithm or a faster
     * one when scaling this image to fit within the
     * bounding box provided by {@code width} and {@code height}.
     *
     * <p>
     * If not initialized or set to {@code true} a better quality filtering
     * will be used, otherwise a faster but lesser quality filtering will be
     * used.
     * </p>
     *
     * @defaultValue true
     */
    private final boolean smooth;

    /**
     * Indicates whether to use a better quality filtering algorithm or a faster
     * one when scaling this image to fit within the
     * bounding box provided by {@code width} and {@code height}.
     *
     * <p>
     * If not initialized or set to {@code true} a better quality filtering
     * will be used, otherwise a faster but lesser quality filtering will be
     * used.
     * </p>
     *
     * @return true if a better quality (but slower) filtering algorithm
     *              is used for scaling to fit within the
     *              bounding box provided by {@code width} and {@code height}.
     */
    public final boolean isSmooth() {
        return smooth;
    }

    /**
     * Indicates whether the image is being loaded in the background.
     *
     * @defaultValue false
     */
    private final boolean backgroundLoading;

    /**
     * Indicates whether the image is being loaded in the background.
     * @return true if the image is loaded in the background
     */
    public final boolean isBackgroundLoading() {
        return backgroundLoading;
    }

    /**
     * Indicates whether an error was detected while loading an image.
     *
     * @defaultValue false
     */
    private ReadOnlyBooleanWrapper error;


    private void setError(boolean value) {
        errorPropertyImpl().set(value);
    }

    public final boolean isError() {
        return error == null ? false : error.get();
    }

    public final ReadOnlyBooleanProperty errorProperty() {
        return errorPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper errorPropertyImpl() {
        if (error == null) {
            error = new ReadOnlyBooleanWrapper(this, "error");
        }
        return error;
    }

    /**
     * The exception which caused image loading to fail. Contains a non-null
     * value only if the {@code error} property is set to {@code true}.
     *
     * @since JavaFX 8.0
     */
    private ReadOnlyObjectWrapper<Exception> exception;

    private void setException(Exception value) {
        exceptionPropertyImpl().set(value);
    }

    public final Exception getException() {
        return exception == null ? null : exception.get();
    }

    public final ReadOnlyObjectProperty<Exception> exceptionProperty() {
        return exceptionPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyObjectWrapper<Exception> exceptionPropertyImpl() {
        if (exception == null) {
            exception = new ReadOnlyObjectWrapper<Exception>(this, "exception");
        }
        return exception;
    }

    /*
     * The underlying platform representation of this Image object.
     *
     * @defaultValue null
     */
    private ObjectPropertyImpl<PlatformImage> platformImage;

    final Object getPlatformImage() {
        return platformImage == null ? null : platformImage.get();
    }

    final ReadOnlyObjectProperty<PlatformImage> acc_platformImageProperty() {
        return platformImagePropertyImpl();
    }

    private ObjectPropertyImpl<PlatformImage> platformImagePropertyImpl() {
        if (platformImage == null) {
            platformImage = new ObjectPropertyImpl<PlatformImage>("platformImage");
        }

        return platformImage;
    }

    void pixelsDirty() {
        platformImagePropertyImpl().fireValueChangedEvent();
    }

    private final class ObjectPropertyImpl<T>
            extends ReadOnlyObjectPropertyBase<T> {
        private final String name;

        private T value;
        private boolean valid = true;

        public ObjectPropertyImpl(final String name) {
            this.name = name;
        }

        public void store(final T value) {
            this.value = value;
        }

        public void set(final T value) {
            if (this.value != value) {
                this.value = value;
                markInvalid();
            }
        }

        @Override
        public void fireValueChangedEvent() {
            super.fireValueChangedEvent();
        }

        private void markInvalid() {
            if (valid) {
                valid = false;
                fireValueChangedEvent();
            }
        }

        @Override
        public T get() {
            valid = true;
            return value;
        }

        @Override
        public Object getBean() {
            return Image.this;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Constructs an {@code Image} with content loaded from the specified URL.
     *
     * @param url a resource path, file path, or URL
     * @throws NullPointerException if {@code url} is null
     * @throws IllegalArgumentException if {@code url} is invalid or unsupported
     */
    public Image(@NamedArg("url") String url) {
        this(validateUrl(url), null, 0, 0, false, false, false);
        initialize(null);
    }

    /**
     * Constructs an {@code Image} with content loaded from the specified URL
     * using the specified parameters.
     *
     * @param url a resource path, file path, or URL
     * @param backgroundLoading indicates whether the image
     *      is being loaded in the background
     * @throws NullPointerException if {@code url} is null
     * @throws IllegalArgumentException if {@code url} is invalid or unsupported
     */
    public Image(@NamedArg("url") String url, @NamedArg("backgroundLoading") boolean backgroundLoading) {
        this(validateUrl(url), null, 0, 0, false, false, backgroundLoading);
        initialize(null);
    }

    /**
     * Constructs an {@code Image} with content loaded from the specified URL
     * using the specified parameters.
     *
     * @param url a resource path, file path, or URL
     * @param requestedWidth the image's bounding box width
     * @param requestedHeight the image's bounding box height
     * @param preserveRatio indicates whether to preserve the aspect ratio of
     *      the original image when scaling to fit the image within the
     *      specified bounding box
     * @param smooth indicates whether to use a better quality filtering
     *      algorithm or a faster one when scaling this image to fit within
     *      the specified bounding box
     * @throws NullPointerException if {@code url} is null
     * @throws IllegalArgumentException if {@code url} is invalid or unsupported
     */
    public Image(@NamedArg("url") String url, @NamedArg("requestedWidth") double requestedWidth, @NamedArg("requestedHeight") double requestedHeight,
                 @NamedArg("preserveRatio") boolean preserveRatio, @NamedArg("smooth") boolean smooth) {
        this(validateUrl(url), null, requestedWidth, requestedHeight,
             preserveRatio, smooth, false);
        initialize(null);
    }

    /**
     * Constructs an {@code Image} with content loaded from the specified URL
     * using the specified parameters.
     *
     * @param url a resource path, file path, or URL
     * @param requestedWidth the image's bounding box width
     * @param requestedHeight the image's bounding box height
     * @param preserveRatio indicates whether to preserve the aspect ratio of
     *      the original image when scaling to fit the image within the
     *      specified bounding box
     * @param smooth indicates whether to use a better quality filtering
     *      algorithm or a faster one when scaling this image to fit within
     *      the specified bounding box
     * @param backgroundLoading indicates whether the image
     *      is being loaded in the background
     * @throws NullPointerException if {@code url} is null
     * @throws IllegalArgumentException if {@code url} is invalid or unsupported
     */
    public Image(
            @NamedArg(value="url", defaultValue="\"\"") String url,
            @NamedArg("requestedWidth") double requestedWidth,
            @NamedArg("requestedHeight") double requestedHeight,
            @NamedArg("preserveRatio") boolean preserveRatio,
            @NamedArg(value="smooth", defaultValue="true") boolean smooth,
            @NamedArg("backgroundLoading") boolean backgroundLoading) {
        this(validateUrl(url), null, requestedWidth, requestedHeight,
             preserveRatio, smooth, backgroundLoading);
        initialize(null);
    }

    /**
     * Constructs an {@code Image} with content loaded from the specified
     * input stream.
     *
     * @param is the stream from which to load the image
     * @throws NullPointerException if input stream is null
     */
    public Image(@NamedArg("is") InputStream is) {
        this(null, validateInputStream(is), 0, 0, false, false, false);
        initialize(null);
    }

    /**
     * Constructs a new {@code Image} with the specified parameters.
     *
     * @param is the stream from which to load the image
     * @param requestedWidth the image's bounding box width
     * @param requestedHeight the image's bounding box height
     * @param preserveRatio indicates whether to preserve the aspect ratio of
     *      the original image when scaling to fit the image within the
     *      specified bounding box
     * @param smooth indicates whether to use a better quality filtering
     *      algorithm or a faster one when scaling this image to fit within
     *      the specified bounding box
     * @throws NullPointerException if input stream is null
     */
    public Image(@NamedArg("is") InputStream is, @NamedArg("requestedWidth") double requestedWidth, @NamedArg("requestedHeight") double requestedHeight,
                 @NamedArg("preserveRatio") boolean preserveRatio, @NamedArg("smooth") boolean smooth) {
        this(null, validateInputStream(is), requestedWidth, requestedHeight,
             preserveRatio, smooth, false);
        initialize(null);
    }

    /**
     * Package private internal constructor used only by {@link WritableImage}.
     * The dimensions must both be positive numbers <code>(&gt;&nbsp;0)</code>.
     *
     * @param width the width of the empty image
     * @param height the height of the empty image
     * @throws IllegalArgumentException if either dimension is negative or zero.
     */
    Image(int width, int height) {
        this(null, null, width, height, false, false, false);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive (w,h > 0)");
        }
        initialize(Toolkit.getToolkit().createPlatformImage(width, height));
    }

    /**
     * Package private internal constructor used only by {@code WritableImage}.
     *
     * @param pixelBuffer the {@code PixelBuffer} used to construct this image.
     */
    Image(PixelBuffer pixelBuffer) {
        this(null, null, pixelBuffer.getWidth(), pixelBuffer.getHeight(),
                false, false, false);
        initialize(pixelBuffer); // Creates an image using the java.nio.Buffer provided by PixelBuffer.
    }

    private Image(Object externalImage) {
        this(null, null, 0, 0, false, false, false);
        initialize(externalImage);
    }

    private Image(String url, InputStream is,
                  double requestedWidth, double requestedHeight,
                  boolean preserveRatio, boolean smooth,
                  boolean backgroundLoading) {
        this.url = url;
        this.inputSource = is;
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        this.preserveRatio = preserveRatio;
        this.smooth = smooth;
        this.backgroundLoading = backgroundLoading;
    }

    /**
     * Cancels the background loading of this image.
     *
     * <p>Has no effect if this image isn't loaded in background or if loading
     * has already completed.</p>
     */
    public void cancel() {
        if (backgroundTask != null) {
            backgroundTask.cancel();
        }
    }

    /*
     * used for testing
     */
    void dispose() {
        cancel();
        Platform.runLater(() -> {
            if (animation != null) {
                animation.stop();
            }
        });
    }

    private ImageTask backgroundTask;

    private void initialize(Object externalImage) {
        // we need to check the original values here, because setting placeholder
        // changes platformImage, so wrong branch of if would be used
        if (externalImage != null) {
            // Make an image from the provided platform-specific image
            // object (e.g. a BufferedImage in the case of the Swing profile)
            ImageLoader loader = loadPlatformImage(externalImage);
            finishImage(loader);
        } else if (isBackgroundLoading() && (inputSource == null)) {
            // Load image in the background.
            loadInBackground();
        } else {
            // Load image immediately.
            ImageLoader loader;
            if (inputSource != null) {
                loader = loadImage(inputSource, getRequestedWidth(), getRequestedHeight(),
                                   isPreserveRatio(), isSmooth());
            } else {
                loader = loadImage(getUrl(), getRequestedWidth(), getRequestedHeight(),
                                   isPreserveRatio(), isSmooth());
            }
            finishImage(loader);
        }
    }

    private void finishImage(ImageLoader loader) {
        final Exception loadingException = loader.getException();
        if (loadingException != null) {
            finishImage(loadingException);
            return;
        }

        if (loader.getFrameCount() > 1) {
            initializeAnimatedImage(loader);
        } else {
            PlatformImage pi = loader.getFrame(0);
            double w = loader.getWidth() / pi.getPixelScale();
            double h = loader.getHeight() / pi.getPixelScale();
            setPlatformImageWH(pi, w, h);
        }
        setProgress(1);
    }

    private void finishImage(Exception exception) {
       setException(exception);
       setError(true);
       setPlatformImageWH(null, 0, 0);
       setProgress(1);
    }

    // Support for animated images.
    private Animation animation;
    private volatile boolean isAnimated;
    // We keep the animation frames associated with the Image rather than with
    // the animation, so most of the data can be garbage collected while
    // the animation is still running.
    private PlatformImage[] animFrames;

    // Generates the animation Timeline for multiframe images.
    private void initializeAnimatedImage(ImageLoader loader) {
        final int frameCount = loader.getFrameCount();
        animFrames = new PlatformImage[frameCount];

        for (int i = 0; i < frameCount; ++i) {
            animFrames[i] = loader.getFrame(i);
        }

        PlatformImage zeroFrame = loader.getFrame(0);

        double w = loader.getWidth() / zeroFrame.getPixelScale();
        double h = loader.getHeight() / zeroFrame.getPixelScale();
        setPlatformImageWH(zeroFrame, w, h);

        isAnimated = true;
        Platform.runLater(() -> {
            animation = new Animation(this, loader);
            animation.start();
        });
    }

    private static final class Animation {
        final WeakReference<Image> imageRef;
        final Timeline timeline;
        final SimpleIntegerProperty frameIndex = new SimpleIntegerProperty() {
            @Override
            protected void invalidated() {
                updateImage(get());
            }
        };

        public Animation(final Image image, final ImageLoader loader) {
            imageRef = new WeakReference<Image>(image);
            timeline = new Timeline();
            int loopCount = loader.getLoopCount();
            timeline.setCycleCount(loopCount == 0 ? Timeline.INDEFINITE : loopCount);

            final int frameCount = loader.getFrameCount();
            int duration = 0;

            for (int i = 0; i < frameCount; ++i) {
                addKeyFrame(i, duration);
                duration = duration + loader.getFrameDelay(i);
            }

            // Note: we need one extra frame in the timeline to define how long
            // the last frame is shown, the wrap around is "instantaneous"
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(duration)));
        }

        public void start() {
            timeline.play();
        }

        public void stop() {
            timeline.stop();
        }

        private void updateImage(final int frameIndex) {
            final Image image = imageRef.get();
            if (image != null) {
                image.platformImagePropertyImpl().set(
                        image.animFrames[frameIndex]);
            } else {
                timeline.stop();
            }
        }

        private void addKeyFrame(final int index, final double duration) {
            timeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(duration),
                            new KeyValue(frameIndex, index, Interpolator.DISCRETE)
                    ));
        }
    }

    private void cycleTasks() {
        synchronized (pendingTasks) {
            runningTasks--;
            // do we have any pending tasks to run ?
            // we can assume we are under the throttle limit because
            // one task just completed.
            final ImageTask nextTask = pendingTasks.poll();
            if (nextTask != null) {
                runningTasks++;
                nextTask.start();
            }
        }
    }

    private void loadInBackground() {
        backgroundTask = new ImageTask();
        // This is an artificial throttle on background image loading tasks.
        // It has been shown that with large images, we can quickly use up the
        // heap loading images, even if they result in thumbnails.
        // The limit of MAX_RUNNING_TASKS is arbitrary, and was based on initial
        // testing with
        // about 60 2-6 megapixel images.
        synchronized (pendingTasks) {
            if (runningTasks >= MAX_RUNNING_TASKS) {
                pendingTasks.offer(backgroundTask);
            } else {
                runningTasks++;
                backgroundTask.start();
            }
        }
    }

    // Used by SwingUtils.toFXImage
    static Image fromPlatformImage(Object image) {
        return new Image(image);
    }

    private void setPlatformImageWH(final PlatformImage newPlatformImage,
                                    final double newWidth,
                                    final double newHeight) {
        if ((Toolkit.getImageAccessor().getPlatformImage(this) == newPlatformImage)
                && (getWidth() == newWidth)
                && (getHeight() == newHeight)) {
            return;
        }

        final Object oldPlatformImage = Toolkit.getImageAccessor().getPlatformImage(this);
        final double oldWidth = getWidth();
        final double oldHeight = getHeight();

        storePlatformImageWH(newPlatformImage, newWidth, newHeight);

        if (oldPlatformImage != newPlatformImage) {
            platformImagePropertyImpl().fireValueChangedEvent();
        }

        if (oldWidth != newWidth) {
            widthPropertyImpl().fireValueChangedEvent();
        }

        if (oldHeight != newHeight) {
            heightPropertyImpl().fireValueChangedEvent();
        }
    }

    private void storePlatformImageWH(final PlatformImage platformImage,
                                      final double width,
                                      final double height) {
        platformImagePropertyImpl().store(platformImage);
        widthPropertyImpl().store(width);
        heightPropertyImpl().store(height);
    }

    void setPlatformImage(PlatformImage newPlatformImage) {
        platformImage.set(newPlatformImage);
    }

    private static final int MAX_RUNNING_TASKS = 4;
    private static int runningTasks = 0;
    private static final Queue<ImageTask> pendingTasks =
            new LinkedList<ImageTask>();

    private final class ImageTask
            implements AsyncOperationListener<ImageLoader> {

        private final AsyncOperation peer;

        public ImageTask() {
            peer = constructPeer();
        }

        @Override
        public void onCancel() {
            finishImage(new CancellationException("Loading cancelled"));
            cycleTasks();
        }

        @Override
        public void onException(Exception exception) {
            finishImage(exception);
            cycleTasks();
        }

        @Override
        public void onCompletion(ImageLoader value) {
            finishImage(value);
            cycleTasks();
        }

        @Override
        public void onProgress(int cur, int max) {
            if (max > 0) {
                double curProgress = (double) cur / max;
                if ((curProgress < 1) && (curProgress >= (getProgress() + 0.1))) {
                    setProgress(curProgress);
                }
            }
        }

        public void start() {
            peer.start();
        }

        public void cancel() {
            peer.cancel();
        }

        private AsyncOperation constructPeer() {
            return loadImageAsync(this, url,
                                  requestedWidth, requestedHeight,
                                  preserveRatio, smooth);
        }
    }

    private static ImageLoader loadImage(
            String url, double width, double height,
            boolean preserveRatio, boolean smooth) {
        return Toolkit.getToolkit().loadImage(url, width, height,
                                              preserveRatio, smooth);

    }

    private static ImageLoader loadImage(
            InputStream stream, double width, double height,
            boolean preserveRatio, boolean smooth) {
        return Toolkit.getToolkit().loadImage(stream, width, height,
                                              preserveRatio, smooth);

    }

    private static AsyncOperation loadImageAsync(
            AsyncOperationListener<? extends ImageLoader> listener,
            String url, double width, double height,
            boolean preserveRatio, boolean smooth) {
        return Toolkit.getToolkit().loadImageAsync(listener, url,
                                                   width, height,
                                                   preserveRatio, smooth);
    }

    private static ImageLoader loadPlatformImage(Object platformImage) {
        return Toolkit.getToolkit().loadPlatformImage(platformImage);
    }

    private static String validateUrl(final String url) {
        if (url == null) {
            throw new NullPointerException("URL must not be null");
        }

        if (url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL must not be empty");
        }

        try {
            if (!URL_QUICKMATCH.matcher(url).matches()) {
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                URL resource;
                if (url.charAt(0) == '/') {
                    // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                    resource = contextClassLoader.getResource(url.substring(1));
                } else {
                    // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                    resource = contextClassLoader.getResource(url);
                }
                if (resource == null) {
                    throw new IllegalArgumentException("Invalid URL or resource not found");
                }
                return resource.toString();
            } else if (DataURI.matchScheme(url)) {
                return url;
            }

            if (new File(url).exists()) {
                return url;
            }

            // Use URL constructor for validation
            return new URL(url).toString();
        } catch (final IllegalArgumentException | MalformedURLException e) {
            throw new IllegalArgumentException(
                    constructDetailedExceptionMessage("Invalid URL", e), e);
        }
    }

    private static InputStream validateInputStream(
            final InputStream inputStream) {
        if (inputStream == null) {
            throw new NullPointerException("Input stream must not be null");
        }

        return inputStream;
    }

    private static String constructDetailedExceptionMessage(
            final String mainMessage,
            final Throwable cause) {
        if (cause == null) {
            return mainMessage;
        }

        final String causeMessage = cause.getMessage();
        return constructDetailedExceptionMessage(
                       (causeMessage != null)
                               ? mainMessage + ": " + causeMessage
                               : mainMessage,
                       cause.getCause());
    }

    /**
     * Indicates whether image is animated.
     */
    boolean isAnimation() {
        return isAnimated;
    }

    boolean pixelsReadable() {
        return (getProgress() >= 1.0 && !isAnimation() && !isError());
    }

    private PixelReader reader;
    /**
     * This method returns a {@code PixelReader} that provides access to
     * read the pixels of the image, if the image is readable.
     * If this method returns null then this image does not support reading
     * at this time.
     * This method will return null if the image is being loaded from a
     * source and is still incomplete {the progress is still &lt;1.0) or if
     * there was an error.
     * This method may also return null for some images in a format that
     * is not supported for reading and writing pixels to.
     *
     * @return the {@code PixelReader} for reading the pixel data of the image
     * @since JavaFX 2.2
     */
    public final PixelReader getPixelReader() {
        if (!pixelsReadable()) {
            return null;
        }
        if (reader == null) {
            reader = new PixelReader() {
                @Override
                public PixelFormat getPixelFormat() {
                    PlatformImage pimg = platformImage.get();
                    return pimg.getPlatformPixelFormat();
                }

                @Override
                public int getArgb(int x, int y) {
                    PlatformImage pimg = platformImage.get();
                    return pimg.getArgb(x, y);
                }

                @Override
                public Color getColor(int x, int y) {
                    int argb = getArgb(x, y);
                    int a = argb >>> 24;
                    int r = (argb >> 16) & 0xff;
                    int g = (argb >>  8) & 0xff;
                    int b = (argb      ) & 0xff;
                    return Color.rgb(r, g, b, a / 255.0);
                }

                @Override
                public <T extends Buffer>
                    void getPixels(int x, int y, int w, int h,
                                   WritablePixelFormat<T> pixelformat,
                                   T buffer, int scanlineStride)
                {
                    PlatformImage pimg = platformImage.get();
                    pimg.getPixels(x, y, w, h, pixelformat,
                                   buffer, scanlineStride);
                }

                @Override
                public void getPixels(int x, int y, int w, int h,
                                    WritablePixelFormat<ByteBuffer> pixelformat,
                                    byte buffer[], int offset, int scanlineStride)
                {
                    PlatformImage pimg = platformImage.get();
                    pimg.getPixels(x, y, w, h, pixelformat,
                                   buffer, offset, scanlineStride);
                }

                @Override
                public void getPixels(int x, int y, int w, int h,
                                    WritablePixelFormat<IntBuffer> pixelformat,
                                    int buffer[], int offset, int scanlineStride)
                {
                    PlatformImage pimg = platformImage.get();
                    pimg.getPixels(x, y, w, h, pixelformat,
                                   buffer, offset, scanlineStride);
                }
            };
        }
        return reader;
    }

    PlatformImage getWritablePlatformImage() {
        PlatformImage pimg = platformImage.get();
        if (!pimg.isWritable()) {
            pimg = pimg.promoteToWritableImage();
            // assert pimg.isWritable();
            platformImage.set(pimg);
        }
        return pimg;
    }
}
