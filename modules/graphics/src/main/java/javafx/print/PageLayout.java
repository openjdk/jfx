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

package javafx.print;

import static javafx.print.PageOrientation.*;

/**
 *
 * A PageLayout encapsulates the information needed to
 * lay out content. The reported width and height can be
 * considered equivalent to the clip enforced by a Window.
 * Applications that obtain a PageLayout instance will
 * need to inspect the width and height to perform layout and pagination.
 * Other information such as orientation and the Paper being used
 * and margins outside of this area are not needed for page rendering.
 * <p>
 * Printers usually have hardware margins where they cannot print.
 * A PageLayout instance obtained from a PrinterJob in the context
 * of a specific printer will be correctly set up to print over
 * the whole of that area. If an application adjusts the printable
 * area outside of this bounds, rendering to those areas will be
 * clipped by the device.
 * <p>
 * Within those hardware margins, the application may define any
 * printable area it needs. The resulting printable area will
 * define the effective dimensions
 * of the page available to the application at printing time.
 * <p>
 * Applying a PageLayout configured based on one printer,
 * to a job on a different printer may not work correctly,
 * as the second printer may not support the same margins, and may not
 * even support the same Paper. In such a case, the PageLayout must
 * be validated against the new printer.
 * <p>
 * A PageLayout is immutable.
 *
 * @since JavaFX 8.0
 */

public final class PageLayout {

    private PageOrientation orient;
    private Paper paper;
    private double lMargin, rMargin, tMargin, bMargin;

    /**
     * Create a PageLayout using the specified Paper size and orientation.
     * Default margins are 0.75 inch which is 56 points.
     * If the paper dimension is smaller than this,
     * the margins will be reduced.
     * @param paper the paper to use
     * @param orient orientation of the layout
     * @throws IllegalArgumentException if paper or orient is null.
     */
    PageLayout(Paper paper, PageOrientation orient) {
        this(paper, orient, 56, 56, 56, 56);
    }

    /**
     * Note that the margins are to be specified as applying after
     * the rotation due to the orientation. Thus the left margin
     * always defines the x origin of the printable area,
     * and the top margin always defines its y origin.
     * @param paper the paper to use
     * @param orient orientation of the layout
     * @param leftMargin the left margin in points.
     * @param rightMargin the left margin in points.
     * @param topMargin the top margin in points.
     * @param bottomMargin the bottom margin in points.
     * @throws IllegalArgumentException if the margins exceed the
     * corresponding paper dimension, or are negative, or if
     * paper or orient is null.
     */
    PageLayout(Paper paper, PageOrientation orient,
               double leftMargin, double rightMargin,
               double topMargin, double bottomMargin) {

        if (paper == null || orient == null ||
            leftMargin < 0 || rightMargin < 0 ||
            topMargin < 0 || bottomMargin < 0) {
            throw new IllegalArgumentException("Illegal parameters");
        }
        if (orient == PORTRAIT || orient == REVERSE_PORTRAIT) {
            if (leftMargin+rightMargin > paper.getWidth() ||
                topMargin+bottomMargin > paper.getHeight()) {
                throw new IllegalArgumentException("Bad margins");
            }
        } else if (leftMargin+rightMargin > paper.getHeight() ||
                   topMargin+bottomMargin > paper.getWidth()) {
            throw new IllegalArgumentException("Bad margins");
        }
        this.paper = paper;
        this.orient = orient;
        this.lMargin = leftMargin;
        this.rMargin = rightMargin;
        this.tMargin = topMargin;
        this.bMargin = bottomMargin;
    }

    public PageOrientation getPageOrientation() {
        return orient;
    }

    /**
     * The paper used.
     * @return the Paper used for this <code>PageLayout</code>.
     */
    public Paper getPaper() {
        return paper;
    }

    /**
     * Returns the width dimension of the printable area of the page,
     * in 1/72 of an inch points, taking into account the orientation.
     * <p>
     * The printable area is width or height reduced by the
     * requested margins on each side. If the requested margins
     * are smaller than the the hardware margins, rendering may
     * be clipped by the device.
     * <p>
     * Since the returned value accounts for orientation, this means if
     * if the orientation is LANDSCAPE or REVERSE_LANDSCAPE, then
     * the left and right margins are subtracted from the height of
     * the underlying paper, since it is rotated 90 degrees.
     * @return printable width in points.
     */
    public double getPrintableWidth() {
        double pw = 0;
        if (orient == PORTRAIT || orient == REVERSE_PORTRAIT) {
            pw =  paper.getWidth();
        } else {
            pw = paper.getHeight();
        }
        pw -= (lMargin+rMargin);
        if (pw < 0) {
            pw = 0;
        }
        return pw;
    }

    /**
     * Returns the height dimension of the printable area of the page,
     * in 1/72 of an inch, taking into account the orientation.
     * <p>
     * The printable area is width or height reduced by the
     * requested margins on each side. If the requested margins
     * are smaller than the the hardware margins, rendering may
     * be clipped by the device.
     * <p>
     * Since the returned value accounts for orientation, this means if
     * if the orientation is LANDSCAPE or REVERSE_LANDSCAPE, then
     * the top and bottom margins are subtracted from the height of
     * the underlying paper, since it is rotated 90 degrees.
     * @return printable height in points.
     */
    public double getPrintableHeight() {
        double ph = 0;
        if (orient == PORTRAIT || orient == REVERSE_PORTRAIT) {
            ph = paper.getHeight();
        } else {
            ph = paper.getWidth();
        }
        ph -= (tMargin+bMargin);
        if (ph < 0) {
            ph = 0;
        }
        return ph;
    }

    /**
     * Returns the left margin of the page layout in points.
     * This value is in the orientation of the PageLayout.
     * @return left margin in points.
     */
    public double getLeftMargin() {
        return lMargin;
    }

    /**
     * Returns the right margin of the page layout in points.
     * This value is in the orientation of the PageLayout.
     * @return right margin in points.
     */
    public double getRightMargin() {
        return rMargin;
    }

    /**
     * Returns the top margin of the page layout in points.
     * This value is in the orientation of the PageLayout.
     * @return top margin in points.
     */
    public double getTopMargin() {
        return tMargin;
    }

    /**
     * Returns the bottom margin of the page layout in points.
     * This value is in the orientation of the PageLayout.
     * @return bottom margin in points.
     */
    public double getBottomMargin() {
        return bMargin;
    }

    @Override public boolean equals(Object o) {
        if (o instanceof PageLayout) {
            PageLayout other = (PageLayout)o;
            return
                paper.equals(other.paper) &&
                orient.equals(other.orient) &&
                tMargin == other.tMargin &&
                bMargin == other.bMargin &&
                rMargin == other.rMargin &&
                lMargin == other.lMargin;
        } else {
            return false;
        }
    }

    @Override public int hashCode() {
        return paper.hashCode() + orient.hashCode()+
            (int)(tMargin+bMargin+lMargin+rMargin);
    }

    @Override public String toString() {
        return
            "Paper="+paper+
            " Orient="+orient+
            " leftMargin="+lMargin+
            " rightMargin="+rMargin+
            " topMargin="+tMargin+
            " bottomMargin="+bMargin;
    }
}
