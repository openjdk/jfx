/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.javafx.experiments.utils3d.geom;

/**
 * Base class for mutable bounds objects. There are two concrete specializations,
 * BoxBounds (3D) and RectBounds (2D). Various "derive" methods exist which are
 * used to mutate the bounds objects, such that they can be converted between the
 * different types as appropriate, or modified in place if possible. This allows
 * us to churn memory as little as possible without representing everything as
 * if it were in 3D space (there are some computational cost savings to being
 * able to ignore the Z values).
 */
public abstract class BaseBounds {

    /**
     * The different types of BaseBounds that are currently supported.
     * We might support other types of bounds in the future (such as
     * SPHERE) which are also 2D or 3D but are defined in some way
     * other than with a bounding box. Such bounds can sometimes more
     * accurately represent the pixels
     */
    public static enum BoundsType {
        RECTANGLE, // A 2D axis-aligned bounding rectangle
        BOX,  // A 3D axis-aligned bounding box
    }

    // Only allow subclasses in this package
    BaseBounds() {
    }

    /**
     * Duplicates this instance. This differs from deriveWithNewBounds(other)
     * where "other" would be this, in that derive methods may return the
     * same instance, whereas copy will always return a new instance.
     */
    public abstract BaseBounds copy();

    /**
     * Return true if this bounds is of a 2D BoundsType, else false.
     */
    public abstract boolean is2D();

    public abstract BoundsType getBoundsType();

    /**
     * Convenience function for getting the width of this bounds.
     * The dimension along the X-Axis.
     */
    public abstract float getWidth();

    /**
     * Convenience function for getting the height of this bounds.
     * The dimension along the Y-Axis.
     */
    public abstract float getHeight();

    /**
     * Convenience function for getting the depth of this bounds.
     * The dimension along the Z-Axis.
     */
    public abstract float getDepth();

    public abstract float getMinX();

    public abstract float getMinY();

    public abstract float getMinZ();

    public abstract float getMaxX();

    public abstract float getMaxY();

    public abstract float getMaxZ();

    public abstract void translate(float x, float y, float z);

    public abstract Vec2f getMin(Vec2f min);

    public abstract Vec2f getMax(Vec2f max);

    public abstract Vec3f getMin(Vec3f min);

    public abstract Vec3f getMax(Vec3f max);

    public abstract BaseBounds deriveWithUnion(BaseBounds other);

    // TODO: Add variants of deriveWithNewBounds such as pair of Vec* (RT-26886)
    public abstract BaseBounds deriveWithNewBounds(Rectangle other);

    public abstract BaseBounds deriveWithNewBounds(BaseBounds other);

    public abstract BaseBounds deriveWithNewBounds(float minX, float minY, float minZ,
                                                   float maxX, float maxY, float maxZ);

    public abstract BaseBounds deriveWithNewBoundsAndSort(float minX, float minY, float minZ,
                                                          float maxX, float maxY, float maxZ);

    public abstract BaseBounds deriveWithPadding(float h, float v, float d);

    public abstract void intersectWith(Rectangle other);

    public abstract void intersectWith(BaseBounds other);

    public abstract void intersectWith(float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ);

    /**
     * Sets the bounds based on the given points, and also ensures that
     * after having done so that this bounds instance is sorted (x1<=x2 and y1<=y2).
     */
    public abstract void setBoundsAndSort(Point2D p1, Point2D p2);

    public abstract void setBoundsAndSort(float minX, float minY, float minZ,
                                          float maxX, float maxY, float maxZ);

    // TODO: obsolete add and replace with deriveWithUnion(Vec2f v) and deriveWithUnion(Vec3f v)
    // (RT-26886)
    public abstract void add(Point2D p);

    public abstract void add(float x, float y, float z);

    public abstract boolean contains(Point2D p);

    public abstract boolean contains(float x, float y);

    public abstract boolean intersects(float x, float y, float width, float height);

    public abstract boolean isEmpty();

    public abstract void roundOut();

    /**
     * Sets the given RectBounds (or creates a new instance of bounds is null) to
     * have the minX, minY, maxX, and maxY of this BoxBounds, dropping the Z values.
     *
     * @param bounds The bounds to fill with values, or null. If null, a new RectBounds
     *               is returned. If not null, the given bounds will be populated and
     *               then returned
     * @return a non-null reference to a RectBounds containing the minX, minY, maxX, and
     * maxY of this BoxBounds.
     */
    public abstract RectBounds flattenInto(RectBounds bounds);

    public abstract BaseBounds makeEmpty();

    public abstract boolean disjoint(float x, float y, float width, float height);

    protected abstract void sortMinMax();

    public static BaseBounds getInstance(float minX, float minY, float minZ,
                                         float maxX, float maxY, float maxZ) {
        if (minZ == 0 && maxZ == 0) {
            return getInstance(minX, minY, maxX, maxY);
        } else {
            return new BoxBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static BaseBounds getInstance(float minX, float minY,
                                         float maxX, float maxY) {
        return new RectBounds(minX, minY, maxX, maxY);
    }
}
