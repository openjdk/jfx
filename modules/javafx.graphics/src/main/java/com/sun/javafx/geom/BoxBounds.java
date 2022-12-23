/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.geom;

import com.sun.javafx.geom.transform.Affine3D;

/**
 *
 */
public class BoxBounds extends BaseBounds {
    // minimum x value of boundining box
    private float minX;
    // maximum x value of boundining box
    private float maxX;
    // minimum y value of boundining box
    private float minY;
    // maximum y value of boundining box
    private float maxY;
    // minimum z value of boundining box
    private float minZ;
    // maximum z value of boundining box
    private float maxZ;

    /**
     * Create an axis aligned bounding box object, with an empty bounds
     * where maxX < minX, maxY < minY and maxZ < minZ.
     */
    public BoxBounds() {
        minX = minY = minZ = 0.0f;
        maxX = maxY = maxZ = -1.0f;
    }

    @Override
    public BaseBounds copy() {
        return new BoxBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Creates an axis aligned bounding box based on the minX, minY, minZ, maxX, maxY,
     * and maxZ values specified.
     */
    public BoxBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        setBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Creates an axis aligned bounding box as a copy of the specified
     * BoxBounds object.
     */
    public BoxBounds(BoxBounds other) {
        setBounds(other);
    }

    @Override
    public BoundsType getBoundsType() {
        return BoundsType.BOX;
    }

    @Override
    public boolean is2D() {
        return (Affine3D.almostZero(minZ) && Affine3D.almostZero(maxZ));
    }

    /**
     * Convenience function for getting the width of this bounds.
     * The dimension along the X-Axis.
     */
    @Override
    public float getWidth() {
        return maxX - minX;
    }

    /**
     * Convenience function for getting the height of this bounds.
     * The dimension along the Y-Axis.
     */
    @Override
    public float getHeight() {
        return maxY - minY;
    }

    /**
     * Convenience function for getting the depth of this bounds.
     * The dimension along the Z-Axis.
     */
    @Override
    public float getDepth() {
        return maxZ - minZ;
    }

    @Override
    public float getMinX() {
        return minX;
    }

    public void setMinX(float minX) {
        this.minX = minX;
    }

    @Override
    public float getMinY() {
        return minY;
    }

    public void setMinY(float minY) {
        this.minY = minY;
    }

    @Override
    public float getMinZ() {
        return minZ;
    }

    public void setMinZ(float minZ) {
        this.minZ = minZ;
    }

    @Override
    public float getMaxX() {
        return maxX;
    }

    public void setMaxX(float maxX) {
        this.maxX = maxX;
    }

    @Override
    public float getMaxY() {
        return maxY;
    }

    public void setMaxY(float maxY) {
        this.maxY = maxY;
    }

    @Override
    public float getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(float maxZ) {
        this.maxZ = maxZ;
    }

    @Override
    public Vec2f getMin(Vec2f min) {
        if (min == null) {
            min = new Vec2f();
        }
        min.x = minX;
        min.y = minY;
        return min;
    }

    @Override
    public Vec2f getMax(Vec2f max) {
        if (max == null) {
            max = new Vec2f();
        }
        max.x = maxX;
        max.y = maxY;
        return max;
    }

    @Override
    public Vec3f getMin(Vec3f min) {
        if (min == null) {
            min = new Vec3f();
        }
        min.x = minX;
        min.y = minY;
        min.z = minZ;
        return min;

    }

    @Override
    public Vec3f getMax(Vec3f max) {
        if (max == null) {
            max = new Vec3f();
        }
        max.x = maxX;
        max.y = maxY;
        max.z = maxZ;
        return max;

    }

    @Override
    public BaseBounds deriveWithUnion(BaseBounds other) {
        if ((other.getBoundsType() == BoundsType.RECTANGLE) ||
                (other.getBoundsType() == BoundsType.BOX)) {
            unionWith(other);
        } else {
            throw new UnsupportedOperationException("Unknown BoundsType");
        }
        return this;
    }

    @Override
    public BaseBounds deriveWithNewBounds(Rectangle other) {
        if (other.width < 0 || other.height < 0) return makeEmpty();
        setBounds(other.x, other.y, 0,
                other.x + other.width, other.y + other.height, 0);
        return this;
    }

    @Override
    public BaseBounds deriveWithNewBounds(BaseBounds other) {
        if (other.isEmpty()) return makeEmpty();
        if ((other.getBoundsType() == BoundsType.RECTANGLE) ||
                (other.getBoundsType() == BoundsType.BOX)) {
            minX = other.getMinX();
            minY = other.getMinY();
            minZ = other.getMinZ();
            maxX = other.getMaxX();
            maxY = other.getMaxY();
            maxZ = other.getMaxZ();
        } else {
            throw new UnsupportedOperationException("Unknown BoundsType");
        }
        return this;
    }

    @Override
    public BaseBounds deriveWithNewBounds(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        if ((maxX < minX) || (maxY < minY) || (maxZ < minZ)) return makeEmpty();
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    @Override
    public BaseBounds deriveWithNewBoundsAndSort(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        setBoundsAndSort(minX, minY, minZ, maxX, maxY, maxZ);
        return this;
    }

    @Override public RectBounds flattenInto(RectBounds bounds) {
        // Create the bounds if we need to
        if (bounds == null) bounds = new RectBounds();
        // Make it empty if we need to
        if (isEmpty()) return bounds.makeEmpty();
        // Populate it with values otherwise
        bounds.setBounds(minX, minY, maxX, maxY);
        return bounds;
    }

    /**
     * Set the bounds to match that of the BaseBounds object specified. The
     * specified bounds object must not be null.
     */
    public final void setBounds(BaseBounds other) {
        minX = other.getMinX();
        minY = other.getMinY();
        minZ = other.getMinZ();
        maxX = other.getMaxX();
        maxY = other.getMaxY();
        maxZ = other.getMaxZ();
    }

    /**
     * Set the bounds to the given values.
     */
    public final void setBounds(float minX, float minY,  float minZ,
            float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public void setBoundsAndSort(float minX, float minY,  float minZ,
            float maxX, float maxY, float maxZ) {
        setBounds(minX, minY, minZ, maxX, maxY, maxZ);
        sortMinMax();
    }

    @Override
    public void setBoundsAndSort(Point2D p1, Point2D p2) {
        setBoundsAndSort(p1.x, p1.y, 0.0f, p2.x, p2.y, 0.0f);
    }

    public void unionWith(BaseBounds other) {
        // Short circuit union if either bounds is empty.
        if (other.isEmpty()) return;
        if (this.isEmpty()) {
            setBounds(other);
            return;
        }

        minX = Math.min(minX, other.getMinX());
        minY = Math.min(minY, other.getMinY());
        minZ = Math.min(minZ, other.getMinZ());
        maxX = Math.max(maxX, other.getMaxX());
        maxY = Math.max(maxY, other.getMaxY());
        maxZ = Math.max(maxZ, other.getMaxZ());
    }


    public void unionWith(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        // Short circuit union if either bounds is empty.
        if ((maxX < minX) || (maxY < minY) || (maxZ < minZ)) return;
        if (this.isEmpty()) {
            setBounds(minX, minY, minZ, maxX, maxY, maxZ);
            return;
        }

        this.minX = Math.min(this.minX, minX);
        this.minY = Math.min(this.minY, minY);
        this.minZ = Math.min(this.minZ, minZ);
        this.maxX = Math.max(this.maxX, maxX);
        this.maxY = Math.max(this.maxY, maxY);
        this.maxZ = Math.max(this.maxZ, maxZ);
    }

    @Override
    public void add(float x, float y, float z) {
        unionWith(x, y, z, x, y, z);
    }

    @Override
    public void add(Point2D p) {
        add(p.x, p.y, 0.0f);
    }

    @Override
    public void intersectWith(Rectangle other) {
        float x = other.x;
        float y = other.y;
        intersectWith(x, y, 0,
                x + other.width, y + other.height, 0);
    }

    @Override
    public void intersectWith(BaseBounds other) {
        // Short circuit intersect if either bounds is empty.
        if (this.isEmpty()) return;
        if (other.isEmpty()) {
            makeEmpty();
            return;
        }

        minX = Math.max(minX, other.getMinX());
        minY = Math.max(minY, other.getMinY());
        minZ = Math.max(minZ, other.getMinZ());
        maxX = Math.min(maxX, other.getMaxX());
        maxY = Math.min(maxY, other.getMaxY());
        maxZ = Math.min(maxZ, other.getMaxZ());
    }

    @Override
    public void intersectWith(float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ) {
        // Short circuit intersect if either bounds is empty.
        if (this.isEmpty()) return;
        if ((maxX < minX) || (maxY < minY) || (maxZ < minZ)) {
            makeEmpty();
            return;
        }

        this.minX = Math.max(this.minX, minX);
        this.minY = Math.max(this.minY, minY);
        this.minZ = Math.max(this.minZ, minZ);
        this.maxX = Math.min(this.maxX, maxX);
        this.maxY = Math.min(this.maxY, maxY);
        this.maxZ = Math.min(this.maxZ, maxZ);
    }

    @Override
    public boolean contains(Point2D p) {
        if ((p == null) || isEmpty()) return false;
        return contains(p.x, p.y, 0.0f);
    }

    @Override
    public boolean contains(float x, float y) {
        if (isEmpty()) return false;
        return contains(x, y, 0.0f);
    }

    public boolean contains(float x, float y, float z) {
        if (isEmpty()) return false;
        return (x >= minX && x <= maxX && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ);
    }

    public boolean contains(float x, float y, float z,
            float width, float height, float depth) {
        if (isEmpty()) return false;
        return contains(x, y, z) && contains(x+width, y+height, z+depth);
    }

    @Override
    public boolean intersects(float x, float y, float width, float height) {
        return intersects(x, y, 0.0f, width, height, 0.0f);
    }

    public boolean intersects(float x, float y, float z,
            float width, float height, float depth) {
        if (isEmpty()) return false;
        return (x + width >= minX &&
                y + height >= minY &&
                z + depth >= minZ &&
                x <= maxX &&
                y <= maxY &&
                z <= maxZ);
    }

    public boolean intersects(BaseBounds other) {
        if ((other == null) || other.isEmpty() || isEmpty()) {
            return false;
        }
        return (other.getMaxX() >= minX &&
                other.getMaxY() >= minY &&
                other.getMaxZ() >= minZ &&
                other.getMinX() <= maxX &&
                other.getMinY() <= maxY &&
                other.getMinZ() <= maxZ);
    }

    @Override
    public boolean disjoint(float x, float y, float width, float height) {
        return disjoint(x, y, 0f, width, height, 0f);
    }

    public boolean disjoint(float x, float y, float z,
            float width, float height, float depth) {
        if (isEmpty()) return true;
        return (x + width < minX ||
                y + height < minY ||
                z + depth < minZ ||
                x > maxX ||
                y > maxY ||
                z > maxZ);
    }

    @Override
    public boolean isEmpty() {
        return maxX < minX || maxY < minY || maxZ < minZ;
    }

    /**
     * Adjusts the edges of this BoxBounds "outward" toward integral boundaries,
     * such that the rounded bounding box will always full enclose the original
     * bounding box.
     */
    @Override
    public void roundOut() {
        minX = (float) Math.floor(minX);
        minY = (float) Math.floor(minY);
        minZ = (float) Math.floor(minZ);
        maxX = (float) Math.ceil(maxX);
        maxY = (float) Math.ceil(maxY);
        maxZ = (float) Math.ceil(maxZ);
    }

    public void grow(float h, float v, float d) {
        minX -= h;
        maxX += h;
        minY -= v;
        maxY += v;
        minZ -= d;
        maxZ += d;
    }

    @Override
    public BaseBounds deriveWithPadding(float h, float v, float d) {
        grow(h, v, d);
        return this;
    }

    // for convenience, this function returns a reference to itself, so we can
    // change from using "bounds.makeEmpty(); return bounds;" to just
    // "return bounds.makeEmpty()"
    @Override
    public BoxBounds makeEmpty() {
        minX = minY = minZ = 0.0f;
        maxX = maxY = maxZ = -1.0f;
        return this;
    }

    @Override
    protected void sortMinMax() {
        if (minX > maxX) {
            float tmp = maxX;
            maxX = minX;
            minX = tmp;
        }
        if (minY > maxY) {
            float tmp = maxY;
            maxY = minY;
            minY = tmp;
        }
        if (minZ > maxZ) {
            float tmp = maxZ;
            maxZ = minZ;
            minZ = tmp;
        }
    }

    @Override
    public void translate(float x, float y, float z) {
        setMinX(getMinX() + x);
        setMinY(getMinY() + y);
        setMaxX(getMaxX() + x);
        setMaxY(getMaxY() + y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final BoxBounds other = (BoxBounds) obj;
        if (minX != other.getMinX()) return false;
        if (minY != other.getMinY()) return false;
        if (minZ != other.getMinZ()) return false;
        if (maxX != other.getMaxX()) return false;
        if (maxY != other.getMaxY()) return false;
        if (maxZ != other.getMaxZ()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Float.floatToIntBits(minX);
        hash = 79 * hash + Float.floatToIntBits(minY);
        hash = 79 * hash + Float.floatToIntBits(minZ);
        hash = 79 * hash + Float.floatToIntBits(maxX);
        hash = 79 * hash + Float.floatToIntBits(maxY);
        hash = 79 * hash + Float.floatToIntBits(maxZ);

        return hash;
    }

    @Override
    public String toString() {
        return "BoxBounds { minX:" + minX + ", minY:" + minY + ", minZ:" + minZ + ", maxX:" + maxX + ", maxY:" + maxY + ", maxZ:" + maxZ + "}";
    }

}
