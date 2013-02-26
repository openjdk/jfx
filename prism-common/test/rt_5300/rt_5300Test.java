/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package rt_5300;

import com.sun.javafx.geom.Arc2D;
import com.sun.prism.BasicStroke;
import com.sun.prism.Image;
import com.sun.prism.PixelFormat;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.Assert;

public class rt_5300Test {

    public rt_5300Test() {
    }

    @Test()
    public void RT5346() {
        int num_bands = 4;
        byte[] bytes = new byte[32 * 32 * num_bands];
        for( int k = 0; k < bytes.length; k++)
        {
            bytes[k] = (byte)0xff;
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.rewind();
        Image Img = Image.fromByteBgraPreData(buf, 32, 32);
        Image image = null;
        try
        {
            image = Img.iconify( (ByteBuffer) Img.getPixelBuffer(), 32, 32);
        } catch (Exception e) {
            //throw exception System.out.println("Exception Caught: " + e.toString() );
        }

        Assert.assertTrue(assertImageIcon(image));
    }

    @Test(timeout=5000)
    public void testArcs() {
        test(10f, null);
        test(10f, new float[] {2f, 2f});
    }

    public static void test(float lw, float dashes[]) {
        test(lw, dashes, BasicStroke.CAP_BUTT);
        test(lw, dashes, BasicStroke.CAP_ROUND);
        test(lw, dashes, BasicStroke.CAP_SQUARE);
    }

    public static void test(float lw, float dashes[], int cap) {
        test(lw, dashes, cap, BasicStroke.JOIN_BEVEL);
        test(lw, dashes, cap, BasicStroke.JOIN_MITER);
        test(lw, dashes, cap, BasicStroke.JOIN_ROUND);
    }

    public static void test(float lw, float dashes[], int cap, int join) {
        BasicStroke bs;
        if (dashes == null) {
            bs = new BasicStroke(lw, cap, join, 10f);
        } else {
            bs = new BasicStroke(lw, cap, join, 10f, dashes, 0f);
        }
        Arc2D a = new Arc2D();
        a.setFrame(0, 0, 100, 100);
        test(bs, a, Arc2D.OPEN);
        test(bs, a, Arc2D.CHORD);
        test(bs, a, Arc2D.PIE);
    }

    public static void test(BasicStroke bs, Arc2D a, int arctype) {
        a.setArcType(arctype);
        for (int s = 0; s <= 360; s += 30) {
            a.start = s;
            for (int e = 0; e <= 360; e += 30) {
                a.extent = e;
                bs.createStrokedShape(a);
            }
        }
    }

    private boolean assertImageIcon(Image ico) {
       if (ico == null) return false;
       if (ico.getPixelFormat() != PixelFormat.INT_ARGB_PRE) {
           return false;
       }

       if (ico.getHeight() != 32 && ico.getWidth() != 32) {
           return false;
       } else {
           return true;
       }

    }


}
