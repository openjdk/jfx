/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.iio.ImageFormatDescription.Signature;
import com.sun.javafx.iio.bmp.BMPImageLoaderFactory;
import com.sun.javafx.iio.common.ImageTools;
import com.sun.javafx.iio.gif.GIFImageLoaderFactory;
import com.sun.javafx.iio.ios.IosImageLoaderFactory;
import com.sun.javafx.iio.jpeg.JPEGImageLoaderFactory;
import com.sun.javafx.iio.png.PNGImageLoaderFactory;
import com.sun.javafx.util.DataURI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * A convenience class for simple image loading. Factories for creating loaders
 * for image formats must be registered with this class.
 */
public class ImageStorage {

    /**
     * An enumeration of supported image types.
     */
    public static enum ImageType {

        /**
         * An image with a single channel of 8-bit valued gray levels.
         */
        GRAY,
        /**
         * An image with with two 8-bit valued channels, one of gray levels,
         * the other of non-premultiplied opacity, ordered as GAGAGA...
         */
        GRAY_ALPHA,
        /**
         * An image with with two 8-bit valued channels, one of gray levels,
         * the other of premultiplied opacity, ordered as GAGAGA...
         */
        GRAY_ALPHA_PRE,
        /**
         * An image with with one 8-bit channel of indexes into a 24-bit
         * lookup table which maps the indexes to 8-bit RGB components.
         */
        PALETTE,
        /**
         * An image with with one 8-bit channel of indexes into a 32-bit
         * lookup table which maps the indexes to 8-bit RGBA components
         * wherein the opacity is not-premultiplied.
         */
        PALETTE_ALPHA,
        /**
         * An image with with one 8-bit channel of indexes into a 32-bit
         * lookup table which maps the indexes to 8-bit RGBA components
         * wherein the opacity is premultiplied.
         */
        PALETTE_ALPHA_PRE,
        /**
         * An image with with one 8-bit channel of indexes into a 24-bit
         * lookup table which maps the indexes to 8-bit RGB components, and
         * a single transparent index to indicate the location of transparent
         * pixels.
         */
        PALETTE_TRANS,
        /**
         * An image with with three 8-bit valued channels of red, green, and
         * blue, respectively, ordered as RGBRGBRGB...
         */
        RGB,
        /**
         * An image with with four 8-bit valued channels of red, green, blue,
         * and non-premultiplied opacity, respectively, ordered as
         * RGBARGBARGBA...
         */
        RGBA,
        /**
         * An image with with four 8-bit valued channels of red, green, blue,
         * and premultiplied opacity, respectively, ordered as
         * RGBARGBARGBA...
         */
        RGBA_PRE
    };
    /**
     * A mapping of lower case file extensions to loader factories.
     */
//    private static HashMap<String, ImageLoaderFactory> loaderFactoriesByExtension;
    /**
     * A mapping of format signature byte sequences to loader factories.
     */
    private static final HashMap<Signature, ImageLoaderFactory> loaderFactoriesBySignature;
    private static final ImageLoaderFactory[] loaderFactories;
    private static final boolean isIOS = PlatformUtil.isIOS();

    private static int maxSignatureLength;

    static {
        if (isIOS) {
            //On iOS we have single factory/ native loader
            //for all image formats
            loaderFactories = new ImageLoaderFactory[]{
                IosImageLoaderFactory.getInstance()
            };
        } else {
            loaderFactories = new ImageLoaderFactory[]{
                GIFImageLoaderFactory.getInstance(),
                JPEGImageLoaderFactory.getInstance(),
                PNGImageLoaderFactory.getInstance(),
                BMPImageLoaderFactory.getInstance()
                // Note: append ImageLoadFactory for any new format here.
            };
        }

//        loaderFactoriesByExtension = new HashMap(numExtensions);
        loaderFactoriesBySignature = new HashMap<Signature, ImageLoaderFactory>(loaderFactories.length);

        for (int i = 0; i < loaderFactories.length; i++) {
            addImageLoaderFactory(loaderFactories[i]);
        }
    }

    public static ImageFormatDescription[] getSupportedDescriptions() {
        ImageFormatDescription[] formats = new ImageFormatDescription[loaderFactories.length];
        for (int i = 0; i < loaderFactories.length; i++) {
            formats[i] = loaderFactories[i].getFormatDescription();
        }
        return (formats);
    }

    /**
     * Returns the number of bands for a raw image of the specified type.
     *
     * @param type the type of image
     * @return the number of bands of a raw image of this type
     */
    public static int getNumBands(ImageType type) {
        int numBands = -1;
        switch (type) {
            case GRAY:
            case PALETTE:
            case PALETTE_ALPHA:
            case PALETTE_ALPHA_PRE:
            case PALETTE_TRANS:
                numBands = 1;
                break;
            case GRAY_ALPHA:
            case GRAY_ALPHA_PRE:
                numBands = 2;
                break;
            case RGB:
                numBands = 3;
                break;
            case RGBA:
            case RGBA_PRE:
                numBands = 4;
                break;
            default:
                throw new IllegalArgumentException("Unknown ImageType " + type);
        }
        return numBands;
    }

    /**
     * Registers an image loader factory. The factory replaces any other factory
     * previously registered for the file extensions (converted to lower case)
     * and signature indicated by the format description.
     *
     * @param factory the factory to register.
     */
    public static void addImageLoaderFactory(ImageLoaderFactory factory) {
        ImageFormatDescription desc = factory.getFormatDescription();
//        String[] extensions = desc.getExtensions();
//        for (int j = 0; j < extensions.length; j++) {
//            loaderFactoriesByExtension.put(extensions[j].toLowerCase(), factory);
//        }

        for (final Signature signature: desc.getSignatures()) {
            loaderFactoriesBySignature.put(signature, factory);
        }

        // invalidate max signature length
        synchronized (ImageStorage.class) {
            maxSignatureLength = -1;
        }
    }

    /**
     * Load all images present in the specified stream. The image will be
     * rescaled according to this algorithm:
     *
     * <code><pre>
     * int finalWidth, finalHeight; // final dimensions
     * int width, height;     // specified maximum dimensions
     * // Use source dimensions as default values.
     * if (width <= 0) {
     *     width = sourceWidth;
     * }
     * if (height <= 0) {
     *     height = sourceHeight;
     * }
     * // If not downscaling reset the dimensions to those of the source.
     * if (!((width < sourceWidth && height <= sourceHeight) ||
     *       (width <= sourceWidth && height < sourceHeight))) {
     *      finalWidth = sourceWidth;
     *      finalHeight = sourceHeight;
     * } else if(preserveAspectRatio) {
     *      double r = (double) sourceWidth / (double) sourceHeight;
     *      finalHeight = (int) ((width / r < height ? width / r : height) + 0.5);
     *      finalWidth = (int) (r * finalHeight + 0.5);
     * } else {
     *      finalWidth = width;
     *      finalHeight = height;
     * }
     * </pre></code>
     *
     * @param input the image data stream.
     * @param listener a listener to receive notifications about image loading.
     * @param width the desired width of the image; if non-positive,
     * the original image width will be used.
     * @param height the desired height of the image; if non-positive, the
     * original image height will be used.
     * @param preserveAspectRatio whether to preserve the width-to-height ratio
     * of the image.
     * @param smooth whether to apply smoothing when downsampling.
     * @return the sequence of all images in the specified source or
     * <code>null</code> on error.
     */
    public static ImageFrame[] loadAll(InputStream input, ImageLoadListener listener,
            double width, double height, boolean preserveAspectRatio,
            float pixelScale, boolean smooth) throws ImageStorageException {
        ImageLoader loader = null;
        ImageFrame[] images = null;

        try {
            if (isIOS) {
                // no extension/signature recognition done here,
                // we always want the iOS native loader
                loader = IosImageLoaderFactory.getInstance().createImageLoader(input);
            } else {
                loader = getLoaderBySignature(input, listener);
            }
            if (loader != null) {
                images = loadAll(loader, width, height, preserveAspectRatio, pixelScale, smooth);
            } else {
                throw new ImageStorageException("No loader for image data");
            }
        } catch (ImageStorageException ise) {
            throw ise;
        } catch (IOException e) {
            throw new ImageStorageException(e.getMessage(), e);
        } finally {
            if (loader != null) {
                loader.dispose();
            }
        }
        return images;
    }

    /**
     * Load all images present in the specified input. For more details refer to
     * {@link #loadAll(InputStream, ImageLoadListener, double, double, boolean, float, boolean)}.
     */
    public static ImageFrame[] loadAll(String input, ImageLoadListener listener,
            double width, double height, boolean preserveAspectRatio,
            float devPixelScale, boolean smooth) throws ImageStorageException {

        if (input == null || input.isEmpty()) {
            throw new ImageStorageException("URL can't be null or empty");
        }

        ImageFrame[] images = null;
        InputStream theStream = null;
        ImageLoader loader = null;

        try {
            float imgPixelScale = 1.0f;
            try {
                if (devPixelScale >= 1.5f) {
                    // Use Mac Retina conventions for >= 1.5f
                    try {
                        String name2x = ImageTools.getScaledImageName(input);
                        theStream = ImageTools.createInputStream(name2x);
                        imgPixelScale = 2.0f;
                    } catch (IOException ignored) {
                    }
                }

                if (theStream == null) {
                    try {
                        theStream = ImageTools.createInputStream(input);
                    } catch (IOException ex) {
                        DataURI dataUri = DataURI.tryParse(input);
                        if (dataUri != null) {
                            String mimeType = dataUri.getMimeType();
                            if (mimeType != null && !"image".equalsIgnoreCase(dataUri.getMimeType())) {
                                throw new IllegalArgumentException("Unexpected MIME type: " + dataUri.getMimeType());
                            }

                            theStream = new ByteArrayInputStream(dataUri.getData());
                        } else {
                            throw ex;
                        }
                    }
                }

                if (isIOS) {
                    loader = IosImageLoaderFactory.getInstance().createImageLoader(theStream);
                } else {
                    loader = getLoaderBySignature(theStream, listener);
                }
            } catch (Exception e) {
                throw new ImageStorageException(e.getMessage(), e);
            }

            if (loader != null) {
                images = loadAll(loader, width, height, preserveAspectRatio, imgPixelScale, smooth);
            } else {
                throw new ImageStorageException("No loader for image data");
            }
        } finally {
            if (loader != null) {
                loader.dispose();
            }
            try {
                if (theStream != null) {
                    theStream.close();
                }
            } catch (IOException ignored) {
            }
        }

        return images;
    }

    private static synchronized int getMaxSignatureLength() {
        if (maxSignatureLength < 0) {
            maxSignatureLength = 0;
            for (final Signature signature:
                    loaderFactoriesBySignature.keySet()) {
                final int signatureLength = signature.getLength();
                if (maxSignatureLength < signatureLength) {
                    maxSignatureLength = signatureLength;
                }
            }
        }

        return maxSignatureLength;
    }

    private static ImageFrame[] loadAll(ImageLoader loader,
            double width, double height, boolean preserveAspectRatio,
            float pixelScale, boolean smooth) throws ImageStorageException {
        ImageFrame[] images = null;
        ArrayList<ImageFrame> list = new ArrayList<ImageFrame>();
        int imageIndex = 0;
        ImageFrame image = null;
        int imgw = (int) Math.round(width * pixelScale);
        int imgh = (int) Math.round(height * pixelScale);
        do {
            try {
                image = loader.load(imageIndex++, imgw, imgh, preserveAspectRatio, smooth);
            } catch (Exception e) {
                // allow partially loaded animated images
                if (imageIndex > 1) {
                    break;
                } else {
                    throw new ImageStorageException(e.getMessage(), e);
                }
            }
            if (image != null) {
                image.setPixelScale(pixelScale);
                list.add(image);
            } else {
                break;
            }
        } while (true);
        int numImages = list.size();
        if (numImages > 0) {
            images = new ImageFrame[numImages];
            list.toArray(images);
        }
        return images;
    }

//    private static ImageLoader getLoaderByExtension(String input, ImageLoadListener listener) {
//        ImageLoader loader = null;
//
//        int dotIndex = input.lastIndexOf(".");
//        if (dotIndex != -1) {
//            String extension = input.substring(dotIndex + 1).toLowerCase();
//            Set extensions = loaderFactoriesByExtension.keySet();
//            if (extensions.contains(extension)) {
//                ImageLoaderFactory factory = loaderFactoriesByExtension.get(extension);
//                InputStream stream = ImageTools.createInputStream(input);
//                if (stream != null) {
//                    loader = factory.createImageLoader(stream);
//                    if (listener != null) {
//                        loader.addListener(listener);
//                    }
//                }
//            }
//        }
//
//        return loader;
//    }

    private static ImageLoader getLoaderBySignature(InputStream stream, ImageLoadListener listener) throws IOException {
        byte[] header = new byte[getMaxSignatureLength()];
        ImageTools.readFully(stream, header);

        for (final Entry<Signature, ImageLoaderFactory> factoryRegistration:
                 loaderFactoriesBySignature.entrySet()) {
            if (factoryRegistration.getKey().matches(header)) {
                InputStream headerStream = new ByteArrayInputStream(header);
                InputStream seqStream = new SequenceInputStream(headerStream, stream);
                ImageLoader loader = factoryRegistration.getValue().createImageLoader(seqStream);
                if (listener != null) {
                    loader.addListener(listener);
                }

                return loader;
            }
        }

        // not found
        return null;
    }

    private ImageStorage() {
    }
}
