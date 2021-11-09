/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import javafx.beans.NamedArg;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.sun.javafx.UnmodifiableArrayList;
import com.sun.javafx.css.SubCssMetaData;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.URLConverter;
import com.sun.javafx.scene.layout.region.LayeredBackgroundPositionConverter;
import com.sun.javafx.scene.layout.region.LayeredBackgroundSizeConverter;
import com.sun.javafx.scene.layout.region.CornerRadiiConverter;
import com.sun.javafx.scene.layout.region.RepeatStruct;
import com.sun.javafx.scene.layout.region.RepeatStructConverter;
import com.sun.javafx.tk.Toolkit;

/**
 * The Background of a {@link Region}. A Background is an immutable object which
 * encapsulates the entire set of data required to render the background
 * of a Region. Because this class is immutable, you can freely reuse the same
 * Background on many different Regions. Please refer to
 * <a href="../doc-files/cssref.html">JavaFX CSS Reference Guide</a> for a
 * complete description of the CSS rules for styling the background of a Region.
 * <p>
 * Every Background is comprised of {@link #getFills() fills} and / or
 * {@link #getImages() images}. Neither list will ever be null, but either or
 * both may be empty. Each defined {@link BackgroundFill} is rendered in order,
 * followed by each defined {@link BackgroundImage}.
 * <p>
 * The Background's {@link #getOutsets() outsets} define any extension of the drawing area of a Region
 * which is necessary to account for all background drawing. These outsets are strictly
 * defined by the BackgroundFills that are specified on this Background, if any, because
 * all BackgroundImages are clipped to the drawing area, and do not define it. The
 * outsets values are strictly non-negative.
 *
 * @since JavaFX 8.0
 */
@SuppressWarnings("unchecked")
public final class Background {
    static final CssMetaData<Node,Paint[]> BACKGROUND_COLOR =
            new SubCssMetaData<>("-fx-background-color",
                    PaintConverter.SequenceConverter.getInstance(),
                    new Paint[] {Color.TRANSPARENT});

    static final CssMetaData<Node,CornerRadii[]> BACKGROUND_RADIUS =
            new SubCssMetaData<>("-fx-background-radius",
                    CornerRadiiConverter.getInstance(),
                    new CornerRadii[] {CornerRadii.EMPTY});

    static final CssMetaData<Node,Insets[]> BACKGROUND_INSETS =
            new SubCssMetaData<>("-fx-background-insets",
                    InsetsConverter.SequenceConverter.getInstance(),
                    new Insets[] {Insets.EMPTY});

    static final CssMetaData<Node,Image[]> BACKGROUND_IMAGE =
            new SubCssMetaData<>("-fx-background-image",
                    URLConverter.SequenceConverter.getInstance());

    static final CssMetaData<Node,RepeatStruct[]> BACKGROUND_REPEAT =
            new SubCssMetaData<>("-fx-background-repeat",
                    RepeatStructConverter.getInstance(),
                    new RepeatStruct[] {new RepeatStruct(BackgroundRepeat.REPEAT,
                                                         BackgroundRepeat.REPEAT) });

    static final CssMetaData<Node,BackgroundPosition[]> BACKGROUND_POSITION =
            new SubCssMetaData<>("-fx-background-position",
                    LayeredBackgroundPositionConverter.getInstance(),
                    new BackgroundPosition[] { BackgroundPosition.DEFAULT });

    static final CssMetaData<Node,BackgroundSize[]> BACKGROUND_SIZE =
            new SubCssMetaData<>("-fx-background-size",
                    LayeredBackgroundSizeConverter.getInstance(),
                    new BackgroundSize[] { BackgroundSize.DEFAULT } );

    private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES =
            (List<CssMetaData<? extends Styleable, ?>>) (List) Collections.unmodifiableList(
                    // Unchecked!
                    Arrays.asList(BACKGROUND_COLOR,
                            BACKGROUND_INSETS,
                            BACKGROUND_RADIUS,
                            BACKGROUND_IMAGE,
                            BACKGROUND_REPEAT,
                            BACKGROUND_POSITION,
                            BACKGROUND_SIZE));

    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return STYLEABLES;
    }

    /**
     * An empty Background, useful to use instead of null.
     */
    public static final Background EMPTY = new Background((BackgroundFill[])null, null);

    /**
     * The list of BackgroundFills which together define the filled portion
     * of this Background. This List is unmodifiable and immutable. It
     * will never be null. The elements of this list will also never be null.
     * @return the list of BackgroundFills
     */
    public final List<BackgroundFill> getFills() { return fills; }
    final List<BackgroundFill> fills;

    /**
     * The list of BackgroundImages which together define the image portion
     * of this Background. This List is unmodifiable and immutable. It
     * will never be null. The elements of this list will also never be null.
     * @return the list of BackgroundImages
     */
    public final List<BackgroundImage> getImages() { return images; }
    final List<BackgroundImage> images;

    /**
     * The outsets of this Background. This represents the largest
     * bounding rectangle within which all drawing for the Background
     * will take place. The outsets will never be negative, and represent
     * the distance from the edge of the Region outward. Any BackgroundImages
     * which would extend beyond the outsets will be clipped. Only the
     * BackgroundFills contribute to the outsets.
     * @return the outsets
     */
    public final Insets getOutsets() { return outsets; }
    final Insets outsets;

    /**
     * Gets whether the background is empty. It is empty if there are no fills or images.
     * @return true if the Background is empty, false otherwise.
     */
    public final boolean isEmpty() {
        return fills.isEmpty() && images.isEmpty();
    }

    /**
     * Specifies whether the Background has at least one opaque fill.
     */
    private final boolean hasOpaqueFill;

    /**
     * Package-private immutable fields referring to the opaque insets
     * of this Background.
     */
    private final double opaqueFillTop, opaqueFillRight, opaqueFillBottom, opaqueFillLeft;
    final boolean hasPercentageBasedOpaqueFills;

    /**
     * True if there are any fills that are in some way based on the size of the region.
     * For example, if a CornerRadii on the fill is percentage based in either or both
     * dimensions.
     */
    final boolean hasPercentageBasedFills;

    /**
     * The cached hash code computation for the Background. One very big
     * reason for making Background immutable was to make it possible to
     * cache and reuse the same Background instance for multiple
     * Regions (for example, every un-hovered Button should have the same
     * Background instance). To enable efficient caching, we cache the hash.
     */
    private final int hash;

    /**
     * Create a new Background by supplying an array of BackgroundFills.
     * This array may be null, or may contain null values. Any null values
     * will be ignored and will not contribute to the {@link #getFills() fills}
     * or {@link #getOutsets() outsets}.
     *
     * @param fills     The fills. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of fills. A null array becomes an empty List.
     */
    public Background(final @NamedArg("fills") BackgroundFill... fills) {
        this(fills, null);
    }

    /**
     * Create a new Background by supplying an array of BackgroundImages.
     * This array may be null, or may contain null values. Any null values will
     * be ignored and will not contribute to the {@link #getImages() images}.
     *
     * @param images    The images. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of images. A null array becomes an empty List.
     */
    public Background(final @NamedArg("images") BackgroundImage... images) {
        this(null, images);
    }

    /**
     * Create a new Background supply two Lists, one for background fills and
     * one for background images. Either list may be null, and may contain nulls.
     * Any null values in these lists will be ignored and will not
     * contribute to the {@link #getFills() fills}, {@link #getImages() images}, or
     * {@link #getOutsets() outsets}.
     *
     * @param fills     The fills. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of fills. A null List becomes an empty List.
     * @param images    The images. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of images. A null List becomes an empty List.
     */
    public Background(final @NamedArg("fills") List<BackgroundFill> fills, final @NamedArg("images") List<BackgroundImage> images) {
        // NOTE: This constructor had to be supplied in order to cause a Builder
        // to be auto-generated, because otherwise the types of the fills and images
        // properties didn't match the types of the array based constructor parameters.
        // So a Builder will use this constructor, while the CSS engine uses the
        // array based constructor (for speed).
        this(fills == null ? null : fills.toArray(new BackgroundFill[fills.size()]),
             images == null ? null : images.toArray(new BackgroundImage[images.size()]));
    }

    /**
     * Create a new Background by supplying two arrays, one for background fills,
     * and one for background images. Either array may be null, and may contain null
     * values. Any null values in these arrays will be ignored and will not
     * contribute to the {@link #getFills() fills}, {@link #getImages() images}, or
     * {@link #getOutsets() outsets}.
     *
     * @param fills     The fills. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of fills. A null array becomes an empty List.
     * @param images    The images. This may be null, and may contain nulls. Any
     *                  contained nulls are filtered out and not included in the
     *                  final List of images. A null array becomes an empty List.
     */
    public Background(final @NamedArg("fills") BackgroundFill[] fills, final @NamedArg("images") BackgroundImage[] images) {
        // The cumulative insets
        double outerTop = 0, outerRight = 0, outerBottom = 0, outerLeft = 0;
        boolean hasPercentOpaqueInsets = false;
        boolean hasPercentFillRadii = false;
        boolean opaqueFill = false;

        // If the fills is empty or null then we know we can just use the shared
        // immutable empty list from Collections.
        if (fills == null || fills.length == 0) {
            this.fills = Collections.emptyList();
        } else {
            // We need to iterate over all of the supplied elements in the fills array.
            // Each null element is ignored. Each non-null element is inspected to
            // see if it contributes to the outsets.
            final BackgroundFill[] noNulls = new BackgroundFill[fills.length];
            int size = 0;
            for (int i=0; i<fills.length; i++) {
                final BackgroundFill fill = fills[i];
                if (fill != null) {
                    noNulls[size++] = fill;
                    final Insets fillInsets = fill.getInsets();
                    final double fillTop = fillInsets.getTop();
                    final double fillRight = fillInsets.getRight();
                    final double fillBottom = fillInsets.getBottom();
                    final double fillLeft = fillInsets.getLeft();
                    outerTop = outerTop <= fillTop ? outerTop : fillTop; // min
                    outerRight = outerRight <= fillRight ? outerRight : fillRight; // min
                    outerBottom = outerBottom <= fillBottom ? outerBottom : fillBottom; // min
                    outerLeft = outerLeft <= fillLeft ? outerLeft : fillLeft; // min

                    // The common case is to NOT have percent based radii
                    final boolean b = fill.getRadii().hasPercentBasedRadii;
                    hasPercentFillRadii |= b;
                    if (fill.fill.isOpaque()) {
                        opaqueFill = true;
                        if (b) {
                            hasPercentOpaqueInsets = true;
                        }
                    }
                }
            }
            this.fills = new UnmodifiableArrayList<>(noNulls, size);
        }
        hasPercentageBasedFills = hasPercentFillRadii;

        // This ensures that we either have outsets of 0, if all the insets were positive,
        // or a value greater than zero if they were negative.
        outsets = new Insets(
                Math.max(0, -outerTop),
                Math.max(0, -outerRight),
                Math.max(0, -outerBottom),
                Math.max(0, -outerLeft));

        // An null or empty images array results in an empty list
        if (images == null || images.length == 0) {
            this.images = Collections.emptyList();
        } else {
            // Filter out any  null values and create an immutable array list
            final BackgroundImage[] noNulls = new BackgroundImage[images.length];
            int size = 0;
            for (int i=0; i<images.length; i++) {
                final BackgroundImage image = images[i];
                if (image != null) noNulls[size++] = image;
            }
            this.images = new UnmodifiableArrayList<>(noNulls, size);
        }

        hasOpaqueFill = opaqueFill;
        if (hasPercentOpaqueInsets) {
            opaqueFillTop = Double.NaN;
            opaqueFillRight = Double.NaN;
            opaqueFillBottom = Double.NaN;
            opaqueFillLeft = Double.NaN;
        } else {
            double[] trbl = new double[4];
            computeOpaqueInsets(1, 1, true, trbl);
            opaqueFillTop = trbl[0];
            opaqueFillRight = trbl[1];
            opaqueFillBottom = trbl[2];
            opaqueFillLeft = trbl[3];
        }
        hasPercentageBasedOpaqueFills = hasPercentOpaqueInsets;

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result = this.fills.hashCode();
        result = 31 * result + this.images.hashCode();
        hash = result;
    }

    /**
     * A convenience factory method for creating a {@code Background} with a single {@code Paint}.
     *
     * @implSpec
     * This call is equivalent to {@link BackgroundFill#BackgroundFill(Paint, CornerRadii, Insets)
     * new Background(new BackgroundFill(fill, null, null));}.
     * @param fill the fill of the background. If {@code null}, {@code Color.TRANSPARENT} will be used.
     * @return a new background of the given fill
     * @since 18
     */
    public static Background fill(Paint fill) {
        return new Background(new BackgroundFill(fill, null, null));
    }

    /**
     * Gets whether the fill of this Background is based on percentages (that is, relative to the
     * size of the region being styled). Specifically, this returns true if any of the CornerRadii
     * on any of the fills on this Background has a radius that is based on percentages.
     *
     * @return True if any CornerRadii of any BackgroundFill on this background would return true, false otherwise.
     * @since JavaFX 8.0
     */
    public boolean isFillPercentageBased() {
        return hasPercentageBasedFills;
    }

    /**
     * Computes the opaque insets for a region with the specified width and height. This call
     * must be made whenever the width or height of the region change, because the opaque insets
     * are based on background fills, and the corner radii of a background fill can be percentage
     * based. Thus, we need to potentially recompute the opaque insets whenever the width or
     * height of the region change. On the other hand, if there are no percentage based corner
     * radii, then we can simply return the pre-computed and cached answers.
     *
     * @param width     The width of the region
     * @param height    The height of the region
     * @param trbl      A four-element array of doubles in order: top, right, bottom, left.
     */
    void computeOpaqueInsets(double width, double height, double[] trbl) {
        computeOpaqueInsets(width, height, false, trbl);
    }

    /**
     * Computes the opaque insets. The first time this is called from the constructor
     * we want to take the long route through and compute everything, whether there are
     * percentage based insets or not (the constructor ensures not to call it in the case
     * that it has percentage based insets!). All other times, this is called by the other
     * computeOpaqueInsets method with "firstTime" set to false, such that if we have
     * percentage based insets, then we will bail early.
     *
     * This method takes into account both fills and images. Because images can be
     * lazy loaded, we cannot pre-compute a bunch of things in the constructor for images
     * the way we can with fills. Instead, each time the method is called, we have to
     * inspect the images. However, we do have fast paths for cases where fills are used
     * and not images.
     *
     * @param width        The width of the region
     * @param height       The height of the region
     * @param firstTime    Whether this is being called from the constructor
     * @param trbl         A four-element array of doubles in order: top, right, bottom, left.
     */
    private void computeOpaqueInsets(double width, double height, boolean firstTime, double[] trbl) {
        double opaqueRegionTop = Double.NaN,
               opaqueRegionRight = Double.NaN,
               opaqueRegionBottom = Double.NaN,
               opaqueRegionLeft = Double.NaN;

        // If during object construction we determined that there is an opaque fill, then we need
        // to visit the fills and figure out which ones contribute to the opaque insets
        if (hasOpaqueFill) {
            // If during construction time we determined that none of the fills had a percentage based
            // opaque inset, then we can just use the pre-computed values. This is worth doing since
            // at this time all CSS based radii for BackgroundFills are literal values!
            if (!firstTime && !hasPercentageBasedOpaqueFills) {
                opaqueRegionTop = opaqueFillTop;
                opaqueRegionRight = opaqueFillRight;
                opaqueRegionBottom = opaqueFillBottom;
                opaqueRegionLeft = opaqueFillLeft;
            } else {
                // NOTE: We know at this point that there is an opaque fill, and that at least one
                // of them uses a percentage for at least one corner radius. Iterate over each
                // BackgroundFill. If the fill is opaque, then we will compute the largest rectangle
                // which will fit within its opaque area, taking the corner radii into account.
                // Initialize them to the "I Don't Know" answer.

                for (int i=0, max=fills.size(); i<max; i++) {
                    final BackgroundFill fill = fills.get(i);
                    final Insets fillInsets = fill.getInsets();
                    final double fillTop = fillInsets.getTop();
                    final double fillRight = fillInsets.getRight();
                    final double fillBottom = fillInsets.getBottom();
                    final double fillLeft = fillInsets.getLeft();

                    if (fill.fill.isOpaque()) {
                        // Some possible configurations:
                        //     (a) rect1 is completely contained by rect2
                        //     (b) rect2 is completely contained by rect1
                        //     (c) rect1 is the same height as rect 2 and they overlap on the left or right
                        //     (d) rect1 is the same width as rect 2 and they overlap on the top or bottom
                        //     (e) they are disjoint or overlap in an unsupported manner.
                        final CornerRadii radii = fill.getRadii();
                        final double topLeftHorizontalRadius = radii.isTopLeftHorizontalRadiusAsPercentage() ?
                                width * radii.getTopLeftHorizontalRadius() : radii.getTopLeftHorizontalRadius();
                        final double topLeftVerticalRadius = radii.isTopLeftVerticalRadiusAsPercentage() ?
                                height * radii.getTopLeftVerticalRadius() : radii.getTopLeftVerticalRadius();
                        final double topRightVerticalRadius = radii.isTopRightVerticalRadiusAsPercentage() ?
                                height * radii.getTopRightVerticalRadius() : radii.getTopRightVerticalRadius();
                        final double topRightHorizontalRadius = radii.isTopRightHorizontalRadiusAsPercentage() ?
                                width * radii.getTopRightHorizontalRadius() : radii.getTopRightHorizontalRadius();
                        final double bottomRightHorizontalRadius = radii.isBottomRightHorizontalRadiusAsPercentage() ?
                                width * radii.getBottomRightHorizontalRadius() : radii.getBottomRightHorizontalRadius();
                        final double bottomRightVerticalRadius = radii.isBottomRightVerticalRadiusAsPercentage() ?
                                height * radii.getBottomRightVerticalRadius() : radii.getBottomRightVerticalRadius();
                        final double bottomLeftVerticalRadius = radii.isBottomLeftVerticalRadiusAsPercentage() ?
                                height * radii.getBottomLeftVerticalRadius() : radii.getBottomLeftVerticalRadius();
                        final double bottomLeftHorizontalRadius = radii.isBottomLeftHorizontalRadiusAsPercentage() ?
                                width * radii.getBottomLeftHorizontalRadius() : radii.getBottomLeftHorizontalRadius();

                        final double t = fillTop + (Math.max(topLeftVerticalRadius, topRightVerticalRadius) / 2);
                        final double r = fillRight + (Math.max(topRightHorizontalRadius, bottomRightHorizontalRadius) / 2);
                        final double b = fillBottom + (Math.max(bottomLeftVerticalRadius, bottomRightVerticalRadius) / 2);
                        final double l = fillLeft + (Math.max(topLeftHorizontalRadius, bottomLeftHorizontalRadius) / 2);
                        if (Double.isNaN(opaqueRegionTop)) {
                            // This only happens for the first opaque fill we encounter
                            opaqueRegionTop = t;
                            opaqueRegionRight = r;
                            opaqueRegionBottom = b;
                            opaqueRegionLeft = l;
                        } else {
                            final boolean largerTop = t >= opaqueRegionTop;
                            final boolean largerRight = r >= opaqueRegionRight;
                            final boolean largerBottom = b >= opaqueRegionBottom;
                            final boolean largerLeft = l >= opaqueRegionLeft;
                            if (largerTop && largerRight && largerBottom && largerLeft) {
                                // The new fill is completely contained within the existing rect, so no change
                                continue;
                            } else if (!largerTop && !largerRight && !largerBottom && !largerLeft) {
                                // The new fill completely contains the existing rect, so use these
                                // new values for our opaque region
                                opaqueRegionTop = fillTop;
                                opaqueRegionRight = fillRight;
                                opaqueRegionBottom = fillBottom;
                                opaqueRegionLeft = fillLeft;
                            } else if (l == opaqueRegionLeft && r == opaqueRegionRight) {
                                // The left and right insets are the same between the two rects, so just pick
                                // the smallest top and bottom
                                opaqueRegionTop = Math.min(t, opaqueRegionTop);
                                opaqueRegionBottom = Math.min(b, opaqueRegionBottom);
                            } else if (t == opaqueRegionTop && b == opaqueRegionBottom) {
                                // The top and bottom are the same between the two rects so just pick
                                // the smallest left and right
                                opaqueRegionLeft = Math.min(l, opaqueRegionLeft);
                                opaqueRegionRight = Math.min(r, opaqueRegionRight);
                            } else {
                                // They are disjoint or overlap in some other manner. So we will just
                                // ignore this region.
                                continue;
                            }
                        }
                    }
                }
            }
        }

        // Check the background images. Since the image of a BackgroundImage might load asynchronously
        // and since we must inspect the image to check for opacity, we just have to visit all the
        // images each time this method is called rather than pre-computing results. With some work
        // we could end up caching the result eventually.
        final Toolkit.ImageAccessor acc = Toolkit.getImageAccessor();
        for (BackgroundImage bi : images) {
            if (bi.opaque == null) {
                // If the image is not yet loaded, just skip it
                // Note: Unit test wants this to be com.sun.javafx.tk.PlatformImage, not com.sun.prism.Image
                final com.sun.javafx.tk.PlatformImage platformImage = acc.getImageProperty(bi.image).get();
                if (platformImage == null) continue;

                // The image has been loaded, so update the opaque flag
                if (platformImage instanceof com.sun.prism.Image) {
                    bi.opaque = ((com.sun.prism.Image)platformImage).isOpaque();
                } else {
                    continue;
                }
            }

            // At this point we know that we're processing an image which has already been resolved
            // and we know whether it is opaque or not. Of course, we only care about processing
            // opaque images.
            if (bi.opaque) {
                if (bi.size.cover ||
                        (bi.size.height == BackgroundSize.AUTO && bi.size.width == BackgroundSize.AUTO &&
                        bi.size.widthAsPercentage && bi.size.heightAsPercentage)) {
                    // If the size mode is "cover" or AUTO, AUTO, and percentage based, then we're done -- we can simply
                    // accumulate insets of "0"
                    opaqueRegionTop = Double.isNaN(opaqueRegionTop) ? 0 : Math.min(0, opaqueRegionTop);
                    opaqueRegionRight = Double.isNaN(opaqueRegionRight) ? 0 : Math.min(0, opaqueRegionRight);
                    opaqueRegionBottom = Double.isNaN(opaqueRegionBottom) ? 0 : Math.min(0, opaqueRegionBottom);
                    opaqueRegionLeft = Double.isNaN(opaqueRegionLeft) ? 0 : Math.min(0, opaqueRegionLeft);
                    break;
                } else {
                    // Here we are taking into account all potential tiling cases including "contain". Basically,
                    // as long as the repeat is *not* SPACE, we know that we'll be touching every pixel, and we
                    // don't really care how big the tiles end up being. The only case where we care about the
                    // actual tile size is in the NO_REPEAT modes.

                    // If the repeatX or repeatY includes "SPACE" Then we bail, because we can't be happy about
                    // spaces strewn about within the region.
                    if (bi.repeatX == BackgroundRepeat.SPACE || bi.repeatY == BackgroundRepeat.SPACE) {
                        bi.opaque = false; // We'll treat it as false in the future
                        continue;
                    }

                    // If the repeatX and repeatY are "REPEAT" and/or "ROUND" (any combination thereof) then
                    // we know all pixels within the region width / height are being touched, so we can just
                    // set the opaqueRegion variables and we're done.
                    final boolean filledX = bi.repeatX == BackgroundRepeat.REPEAT || bi.repeatX == BackgroundRepeat.ROUND;
                    final boolean filledY = bi.repeatY == BackgroundRepeat.REPEAT || bi.repeatY == BackgroundRepeat.ROUND;
                    if (filledX && filledY) {
                        opaqueRegionTop = Double.isNaN(opaqueRegionTop) ? 0 : Math.min(0, opaqueRegionTop);
                        opaqueRegionRight = Double.isNaN(opaqueRegionRight) ? 0 : Math.min(0, opaqueRegionRight);
                        opaqueRegionBottom = Double.isNaN(opaqueRegionBottom) ? 0 : Math.min(0, opaqueRegionBottom);
                        opaqueRegionLeft = Double.isNaN(opaqueRegionLeft) ? 0 : Math.min(0, opaqueRegionLeft);
                        break;
                    }

                    // We know that one or the other dimension is not filled, so we have to compute the right
                    // width / height. This is basically a big copy/paste from NGRegion! Blah!
                    final double w = bi.size.widthAsPercentage ? bi.size.width * width : bi.size.width;
                    final double h = bi.size.heightAsPercentage ? bi.size.height * height : bi.size.height;
                    final double imgUnscaledWidth = bi.image.getWidth();
                    final double imgUnscaledHeight = bi.image.getHeight();

                    // Now figure out the width and height of each tile to be drawn. The actual image
                    // dimensions may be one thing, but we need to figure out what the size of the image
                    // in the destination is going to be.
                    final double tileWidth, tileHeight;
                    if (bi.size.contain) {
                        // In the case of "contain", we compute the destination size based on the largest
                        // possible scale such that the aspect ratio is maintained, yet one side of the
                        // region is completely filled.
                        final double scaleX = width / imgUnscaledWidth;
                        final double scaleY = height / imgUnscaledHeight;
                        final double scale = Math.min(scaleX, scaleY);
                        tileWidth = Math.ceil(scale * imgUnscaledWidth);
                        tileHeight = Math.ceil(scale * imgUnscaledHeight);
                    } else if (bi.size.width >= 0 && bi.size.height >= 0) {
                        // The width and height have been expressly defined. Note that AUTO is -1,
                        // and all other negative values are disallowed, so by checking >= 0, we
                        // are essentially saying "if neither is AUTO"
                        tileWidth = w;
                        tileHeight = h;
                    } else if (w >= 0) {
                        // In this case, the width is specified, but the height is AUTO
                        tileWidth = w;
                        final double scale = tileWidth / imgUnscaledWidth;
                        tileHeight = imgUnscaledHeight * scale;
                    } else if (h >= 0) {
                        // Here the height is specified and the width is AUTO
                        tileHeight = h;
                        final double scale = tileHeight / imgUnscaledHeight;
                        tileWidth = imgUnscaledWidth * scale;
                    } else {
                        // Both are auto.
                        tileWidth = imgUnscaledWidth;
                        tileHeight = imgUnscaledHeight;
                    }

                    opaqueRegionTop = Double.isNaN(opaqueRegionTop) ? 0 : Math.min(0, opaqueRegionTop);
                    opaqueRegionRight = Double.isNaN(opaqueRegionRight) ? (width - tileWidth) : Math.min(width - tileWidth, opaqueRegionRight);
                    opaqueRegionBottom = Double.isNaN(opaqueRegionBottom) ? (height - tileHeight) : Math.min(height - tileHeight, opaqueRegionBottom);
                    opaqueRegionLeft = Double.isNaN(opaqueRegionLeft) ? 0 : Math.min(0, opaqueRegionLeft);
                }
            }
        }

        trbl[0] = opaqueRegionTop;
        trbl[1] = opaqueRegionRight;
        trbl[2] = opaqueRegionBottom;
        trbl[3] = opaqueRegionLeft;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Background that = (Background) o;
        // Because the hash is cached, this can be a very fast check
        if (hash != that.hash) return false;
        if (!fills.equals(that.fills)) return false;
        if (!images.equals(that.images)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode() {
        return hash;
    }
}
