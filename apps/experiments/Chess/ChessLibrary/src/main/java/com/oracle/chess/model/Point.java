/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.chess.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point class.
 *
 */
public final class Point {

    static final char[] letters = { 'a', 'b', 'c', 'd', 'e', 'f', 'g' ,'h' };

    private final int x;
    private final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Point decrementX(int delta) {
        return fromXY(x - delta, y);
    }

    public Point incrementX(int delta) {
        return fromXY(x + delta, y);
    }

    public Point decrementY(int delta) {
        return fromXY(x, y - delta);
    }

    public Point incrementY(int delta) {
        return fromXY(x, y + delta);
    }
    
    @Override
    public int hashCode() {
        return y * 8 + x;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Point other = (Point) obj;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    private static final Map<Integer, Point> cache = new ConcurrentHashMap<>();

    public static Point fromXY(int x, int y) {
        final int index = y * Board.N_SQUARES + x;
        Point point = cache.get(index);
        if (point == null) {
            point = new Point(x, y);
            cache.put(index, point);
        }
        return point;
    }

    public static Point fromNotation(String s) {
        return fromXY((int) s.charAt(0) - 'a', (int) s.charAt(1) - '1');
    }

    public String toNotation() {
        return letters[x] + Integer.toString(y + 1);
    }
}
