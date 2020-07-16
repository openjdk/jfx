/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.print;

import javafx.application.Platform;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import static javafx.print.Printer.MarginType.HARDWARE_MINIMUM;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import static javax.print.attribute.standard.MediaPrintableArea.INCH;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

public class MarginsTest {

    @Test public void test() {

        Printer printer = Printer.getDefaultPrinter();
        assumeNotNull(printer);

        PageLayout layout =
             printer.createPageLayout(Paper.NA_LETTER,
                                      PageOrientation.PORTRAIT,
                                      HARDWARE_MINIMUM);

        int lm = (int)Math.round(layout.getLeftMargin());
        int rm = (int)Math.round(layout.getRightMargin());
        int bm = (int)Math.round(layout.getBottomMargin());
        int tm = (int)Math.round(layout.getTopMargin());

        System.out.println("FX : lm=" + lm + " rm=" + rm +
                           " tm=" + tm + " bm=" + bm);
        /* 0.75ins * 72 = 54 pts  */
        if (lm != 54 || rm != 54 || tm != 54 || bm != 54) {
            return; // Got something other than default.
        }

        /* Could this be what we got from 2D ? Unlikely but check. */
        PrintService service = PrintServiceLookup.lookupDefaultPrintService();
        /* If this is null, too much chance of false positive to continue */
        if (service == null) {
            return;
        }
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(MediaSizeName.NA_LETTER);
        MediaPrintableArea[] mpa = (MediaPrintableArea[])service.
            getSupportedAttributeValues(MediaPrintableArea.class, null, pras);
        if (mpa.length == 0) { // never null.
            return;
        }
        int mlm = (int)(Math.round(mpa[0].getX(INCH)*72));
        int mtm = (int)(Math.round(mpa[0].getX(INCH)*72));
        System.out.println("2D : lm=" + mlm + " tm= " + mtm);
        if (mlm == 54 && mtm == 54) {
            return;
        }
        fail("Margins differ.");
   }
}
