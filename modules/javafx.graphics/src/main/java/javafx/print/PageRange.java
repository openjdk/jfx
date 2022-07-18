/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import javafx.beans.NamedArg;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;

/**
 * A {@code PageRange} is used to select or constrain the job print
 * stream pages to print.
 * Page numbering starts from 1 to correspond to user expectations.
 * The <i>start</i> page must be greater than zero and less than or
 * equal to the <i>end</i>page.
 * If start and end are equal, the range refers to a single page.
 * Values that exceed the number of job pages are harmlessly ignored
 * during printing.
 * @since JavaFX 8.0
 */
public final class PageRange {

    private ReadOnlyIntegerWrapper startPage, endPage;

    /**
     * Create a new PageRange with the specified start and end page numbers.
     * @param startPage the first page in the range.
     * @param endPage the last page in the range.
     * @throws IllegalArgumentException if the page range is not valid
     */
    public PageRange(@NamedArg("startPage") int startPage, @NamedArg("endPage") int endPage) {
        if (startPage <= 0 || startPage > endPage) {
            throw new IllegalArgumentException("Invalid range : " +
                                               startPage + " -> " + endPage);
        }
        startPageImplProperty().set(startPage);
        endPageImplProperty().set(endPage);
    }

    /**
     * <code>IntegerProperty</code> representing the starting
     * page number of the range. See {@link #setStartPage setStartPage()}
     * for more information.
     */
    private ReadOnlyIntegerWrapper startPageImplProperty() {
        if (startPage == null) {
            startPage =
                new ReadOnlyIntegerWrapper(PageRange.this, "startPage", 1) {

                @Override
                public void set(int value) {
                    if ((value <= 0) ||
                        (endPage != null && value < endPage.get())) {
                        return;
                    }
                    super.set(value);
                }
            };
        }
        return startPage;
    }

    /**
     * <code>IntegerProperty</code> representing the starting
     * page number of the range.
     * @return the starting page number of the range
     */
    public final ReadOnlyIntegerProperty startPageProperty() {
        return startPageImplProperty().getReadOnlyProperty();
    }

    public final int getStartPage() {
        return startPageProperty().get();
    }

    private ReadOnlyIntegerWrapper endPageImplProperty() {
        if (endPage == null) {
            endPage =
                new ReadOnlyIntegerWrapper(PageRange.this, "endPage", 9999) {

                @Override
                public void set(int value) {
                    if ((value <= 0) ||
                        (startPage != null && value < startPage.get())) {
                        return;
                    }
                    super.set(value);
                }

            };
        }
        return endPage;
    }

    /**
     * <code>IntegerProperty</code> representing the ending
     * page number of the range.
     * @return the ending page number of the range
     */
    public final ReadOnlyIntegerProperty endPageProperty() {
        return endPageImplProperty().getReadOnlyProperty();
    }

    public final int getEndPage() {
        return endPageProperty().get();
    }

    @Override
    public String toString() {
       return "Pages " + getStartPage() + " to " + getEndPage();
    }
}
