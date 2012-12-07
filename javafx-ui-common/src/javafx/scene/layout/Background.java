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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import com.sun.javafx.UnmodifiableArrayList;
import com.sun.javafx.css.StyleablePropertyMetaData;
import com.sun.javafx.css.SubCSSProperty;
import com.sun.javafx.css.converters.InsetsConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.URLConverter;
import com.sun.javafx.scene.layout.region.LayeredBackgroundPositionConverter;
import com.sun.javafx.scene.layout.region.LayeredBackgroundSizeConverter;
import com.sun.javafx.scene.layout.region.RepeatStruct;
import com.sun.javafx.scene.layout.region.RepeatStructConverter;

/**
 * The Background of a {@link Region}. A Background is an immutable object which
 * encapsulates the entire set of data required to render the background
 * of a Region. Because this class is immutable, you can freely reuse the same
 * Background on many different Regions. Please refer to
 * {@link ../doc-files/cssref.html JavaFX CSS Reference} for a complete description
 * of the CSS rules for styling the background of a Region.
 * <p/>
 * Every Background is comprised of {@link #getFills() fills} and / or
 * {@link #getImages() images}. Neither list will ever be null, but either or
 * both may be empty. Each defined {@link BackgroundFill} is rendered in order,
 * followed by each defined {@link BackgroundImage}.
 * <p/>
 * The Background's {@link #getOutsets() outsets} define any extension of the drawing area of a Region
 * which is necessary to account for all background drawing. These outsets are strictly
 * defined by the BackgroundFills that are specified on this Background, if any, because
 * all BackgroundImages are clipped to the drawing area, and do not define it. The
 * outsets values are strictly non-negative.
 *
 * @since JavaFX 8
 */
public final class Background {
    static final StyleablePropertyMetaData<Node,Paint[]> BACKGROUND_COLOR =
            new SubCSSProperty<Paint[]>("-fx-background-color",
                    PaintConverter.SequenceConverter.getInstance(),
                    new Paint[] {Color.TRANSPARENT});

    static final StyleablePropertyMetaData<Node,Insets[]> BACKGROUND_RADIUS =
            new SubCSSProperty<Insets[]>("-fx-background-radius",
                    InsetsConverter.SequenceConverter.getInstance(),
                    new Insets[] {Insets.EMPTY});

    static final StyleablePropertyMetaData<Node,Insets[]> BACKGROUND_INSETS =
            new SubCSSProperty<Insets[]>("-fx-background-insets",
                    InsetsConverter.SequenceConverter.getInstance(),
                    new Insets[] {Insets.EMPTY});

    static final StyleablePropertyMetaData<Node,Image[]> BACKGROUND_IMAGE =
            new SubCSSProperty<Image[]>("-fx-background-image",
                    URLConverter.SequenceConverter.getInstance());

    static final StyleablePropertyMetaData<Node,RepeatStruct[]> BACKGROUND_REPEAT =
            new SubCSSProperty<RepeatStruct[]>("-fx-background-repeat",
                    RepeatStructConverter.getInstance(),
                    new RepeatStruct[] {new RepeatStruct(BackgroundRepeat.REPEAT,
                                                         BackgroundRepeat.REPEAT) });

    static final StyleablePropertyMetaData<Node,BackgroundPosition[]> BACKGROUND_POSITION =
            new SubCSSProperty<BackgroundPosition[]>("-fx-background-position",
                    LayeredBackgroundPositionConverter.getInstance(),
                    new BackgroundPosition[] { BackgroundPosition.DEFAULT });

    static final StyleablePropertyMetaData<Node,BackgroundSize[]> BACKGROUND_SIZE =
            new SubCSSProperty<BackgroundSize[]>("-fx-background-size",
                    LayeredBackgroundSizeConverter.getInstance(),
                    new BackgroundSize[] { BackgroundSize.DEFAULT } );

    private static final List<StyleablePropertyMetaData> STYLEABLES =
            (List<StyleablePropertyMetaData>) (List) Collections.unmodifiableList(
                    Arrays.asList(BACKGROUND_COLOR,
                            BACKGROUND_INSETS,
                            BACKGROUND_RADIUS,
                            BACKGROUND_IMAGE,
                            BACKGROUND_REPEAT,
                            BACKGROUND_POSITION,
                            BACKGROUND_SIZE));

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated public static List<StyleablePropertyMetaData> getClassStyleablePropertyMetaData() {
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
     */
    final List<BackgroundFill> fills;
    public final List<BackgroundFill> getFills() { return fills; }

    /**
     * The list of BackgroundImages which together define the image portion
     * of this Background. This List is unmodifiable and immutable. It
     * will never be null. The elements of this list will also never be null.
     */
    final List<BackgroundImage> images;
    public final List<BackgroundImage> getImages() { return images; }

    /**
     * The outsets of this Background. This represents the largest
     * bounding rectangle within which all drawing for the Background
     * will take place. The outsets will never be negative, and represent
     * the distance from the edge of the Region outward. Any BackgroundImages
     * which would extend beyond the outsets will be clipped. Only the
     * BackgroundFills contribute to the outsets.
     */
    final Insets outsets;
    public final Insets getOutsets() { return outsets; }

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
    final boolean hasOpaqueFill;

    /**
     * Package-private immutable fields referring to the opaque insets
     * of this Background.
     */
    private final double opaqueTop, opaqueRight, opaqueBottom, opaqueLeft;
    final boolean hasPercentageBasedOpaqueInsets;

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
    public Background(final BackgroundFill... fills) {
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
    public Background(final BackgroundImage... images) {
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
    public Background(final List<BackgroundFill> fills, final List<BackgroundImage> images) {
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
    public Background(final BackgroundFill[] fills, final BackgroundImage[] images) {
        // The cumulative insets
        double outerTop = 0, outerRight = 0, outerBottom = 0, outerLeft = 0;
        boolean hasPercentOpaqueInsets = false;
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

                    if (fill.fill.isOpaque()) {
                        opaqueFill = true;
                        if (fill.getRadii().hasPercentBasedRadii) {
                            hasPercentOpaqueInsets = true;
                        }
                    }
                }
            }
            this.fills = new UnmodifiableArrayList<BackgroundFill>(noNulls, size);
        }

        hasOpaqueFill = opaqueFill;
        if (hasPercentOpaqueInsets) {
            opaqueTop = Double.NaN;
            opaqueRight = Double.NaN;
            opaqueBottom = Double.NaN;
            opaqueLeft = Double.NaN;
        } else {
            double[] trbl = new double[4];
            computeOpaqueInsets(1, 1, true, trbl);
            opaqueTop = trbl[0];
            opaqueRight = trbl[1];
            opaqueBottom = trbl[2];
            opaqueLeft = trbl[3];
        }
        hasPercentageBasedOpaqueInsets = hasPercentOpaqueInsets;

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
            this.images = new UnmodifiableArrayList<BackgroundImage>(noNulls, size);
        }

        // Pre-compute the hash code. NOTE: all variables are prefixed with "this" so that we
        // do not accidentally compute the hash based on the constructor arguments rather than
        // based on the fields themselves!
        int result = this.fills.hashCode();
        result = 31 * result + this.images.hashCode();
        hash = result;
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
     * @param width        The width of the region
     * @param height       The height of the region
     * @param firstTime    Whether this is being called from the constructor
     * @param trbl         A four-element array of doubles in order: top, right, bottom, left.
     */
    private void computeOpaqueInsets(double width, double height, boolean firstTime, double[] trbl) {
        // If during object construction we determined that there are no opaque
        // fills, then we will simple return the "I don't know" answer for
        // the opaque insets.
        if (!hasOpaqueFill) {
            trbl[0] = Double.NaN;
            trbl[1] = Double.NaN;
            trbl[2] = Double.NaN;
            trbl[3] = Double.NaN;
            return;
        }

        // If during construction time we determined that none of the fills had a percentage based
        // opaque inset, then we can return the pre-computed values. This is worth doing since
        // at this time all CSS based radii for BackgroundFills are literal values!
        if (!firstTime && !hasPercentageBasedOpaqueInsets) {
            trbl[0] = opaqueTop;
            trbl[1] = opaqueRight;
            trbl[2] = opaqueBottom;
            trbl[3] = opaqueLeft;
            return;
        }

        // NOTE: We know at this point that there is an opaque fill, and that at least one
        // of them uses a percentage for at least one corner radius. Iterate over each
        // BackgroundFill. If the fill is opaque, then we will compute the largest rectangle
        // which will fit within its opaque area, taking the corner radii into account.
        // Initialize them to the "I Don't Know" answer.
        double opaqueRegionLeft = Double.NaN,
                opaqueRegionTop = Double.NaN,
                opaqueRegionRight = Double.NaN,
                opaqueRegionBottom = Double.NaN;

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

        trbl[0] = opaqueRegionTop;
        trbl[1] = opaqueRegionRight;
        trbl[2] = opaqueRegionBottom;
        trbl[3] = opaqueRegionLeft;
    }

    /**
     * @inheritDoc
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
     * @inheritDoc
     */
    @Override public int hashCode() {
        return hash;
    }
}
