/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;

public class RegionShim extends Region {

    public static double boundedSize(double min, double pref, double max) {
        return Region.boundedSize(min, pref, max);
    }

    public static void addImageListener(Region r, Image image) {
        r.addImageListener(image);
    }

    public static double computeChildMaxAreaHeight(Region r,
            Node child, double maxBaselineComplement, Insets margin, double width) {
        return r.computeChildMaxAreaHeight(child, maxBaselineComplement, margin, width);
    }

    public static double computeChildMaxAreaWidth(Region r,
            Node child, double baselineComplement,
            Insets margin, double height, boolean fillHeight) {
        return r.computeChildMaxAreaWidth(child,
                baselineComplement, margin, height, fillHeight);
    }

    public static double computeChildMinAreaHeight(Region r,
            Node child, double minBaselineComplement,
            Insets margin, double width) {
        return r.computeChildMinAreaHeight(child,
                minBaselineComplement,
                margin, width );
    }

    public static double computeChildMinAreaWidth(Region r,
            Node child, Insets margin) {
        return r.computeChildMinAreaWidth(child, margin);
    }

    public static double computeChildMinAreaWidth(Region r,
            Node child, double baselineComplement,
            Insets margin, double height, boolean fillHeight) {
        return r.computeChildMinAreaWidth(child,
                baselineComplement,
                margin, height, fillHeight);
    }

    public static double computeChildPrefAreaHeight(Region r,
            Node child, Insets margin) {
        return r.computeChildPrefAreaHeight(child, margin);
    }

    public static double computeChildPrefAreaHeight(Region r,
            Node child, double prefBaselineComplement,
            Insets margin, double width) {
        return r.computeChildPrefAreaHeight(child,
                prefBaselineComplement, margin, width);
    }

    public static double computeChildPrefAreaWidth(Region r,
            Node child, Insets margin) {
        return r.computeChildPrefAreaWidth(child, margin);
    }

    public static double computeChildPrefAreaWidth(Region r,
            Node child, double baselineComplement, Insets margin,
            double height, boolean fillHeight) {
        return r.computeChildPrefAreaWidth(child, baselineComplement,
                margin, height, fillHeight);
    }

    public static void layoutInArea(Region r,
            Node child, double areaX, double areaY,
            double areaWidth, double areaHeight,
            double areaBaselineOffset,
            HPos halignment, VPos valignment) {
        r.layoutInArea(child, areaX, areaY,
                areaWidth, areaHeight, areaBaselineOffset,
                halignment, valignment);
    }

    public static void layoutInArea(Region r,
            Node child, double areaX, double areaY,
            double areaWidth, double areaHeight,
            double areaBaselineOffset,
            Insets margin,
            HPos halignment, VPos valignment) {
        r.layoutInArea(child, areaX, areaY,
                areaWidth, areaHeight,
                areaBaselineOffset,
                margin,
                halignment, valignment);
    }

    public static void layoutInArea(Region r,
            Node child, double areaX, double areaY,
            double areaWidth, double areaHeight,
            double areaBaselineOffset,
            Insets margin, boolean fillWidth, boolean fillHeight,
            HPos halignment, VPos valignment) {
        r.layoutInArea(child, areaX, areaY,
                areaWidth, areaHeight,
                areaBaselineOffset,
                margin, fillWidth, fillHeight,
                halignment, valignment);

    }

    public static void positionInArea(Region r,
            Node child, double areaX, double areaY,
            double areaWidth, double areaHeight,
            double areaBaselineOffset, HPos halignment,
            VPos valignment) {
        r.positionInArea(child, areaX, areaY,
                areaWidth, areaHeight,
                areaBaselineOffset, halignment,
                valignment);
    }

    public static void removeImageListener(Region r, Image image) {
        r.addImageListener(image);
    }

    public static double snapPortionX(Region r, double value) {
        return r.snapPortionX(value);
    }

    public static double snapPortionY(Region r, double value) {
        return r.snapPortionY(value);
    }

    public static void setWidth(Region r, double width) {
        r.setWidth(width);
    }

    public static void setHeight(Region r, double height) {
        r.setHeight(height);
    }

    //----------------------------------------------------------

        @Override public void addImageListener(Image image) {
            super.addImageListener(image);
        }

        @Override public void removeImageListener(Image image) {
            super.removeImageListener(image);
        }

}
