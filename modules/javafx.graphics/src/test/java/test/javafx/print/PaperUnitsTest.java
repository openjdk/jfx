/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import javafx.print.Paper;
import com.sun.javafx.print.PrintHelper;
import com.sun.javafx.print.Units;

public class PaperUnitsTest {

  @Test public void dummyTest() {
  }

  @Test public void createPaperPts() {
     double wid = 100.0;
     double hgt = 200.0;
     Paper p = PrintHelper.createPaper("TestPOINT", wid, hgt, Units.POINT);
     int ptsWid = (int)p.getWidth();
     int ptsHgt = (int)p.getHeight();
     int expectedPtsWid = (int)wid;
     int expectedPtsHgt = (int)hgt;
     assertTrue("Points width is not as expected", ptsWid == expectedPtsWid);
     assertTrue("Points height is not as expected", ptsHgt == expectedPtsHgt);
   }

  @Test public void createPaperInches() {
     double inWid = 100.0;
     double inHgt = 200.0;
     Paper p = PrintHelper.createPaper("TestINCH", inWid, inHgt, Units.INCH);
     int ptsWid = (int)p.getWidth();
     int ptsHgt = (int)p.getHeight();
     int expectedPtsWid = (int)((inWid * 72) + 0.5);
     int expectedPtsHgt = (int)((inHgt * 72) + 0.5);
     assertTrue("Inches width is not as expected", ptsWid == expectedPtsWid);
     assertTrue("Inches height is not as expected", ptsHgt == expectedPtsHgt);
   }

  @Test public void createPaperMM() {
     double mmWid = 100.0;
     double mmHgt = 200.0;
     Paper p = PrintHelper.createPaper("TestMM", mmWid, mmHgt, Units.MM);
     double ptsWid = p.getWidth();
     double ptsHgt = p.getHeight();
     double expectedPtsWid = (mmWid * 72) / 25.4;
     double expectedPtsHgt = (mmHgt * 72) / 25.4;
     assertEquals("MM width is not as expected", ptsWid, expectedPtsWid, 0.001);
     assertEquals("MM height is not as expected", ptsHgt, expectedPtsHgt, 0.001);
   }
}
