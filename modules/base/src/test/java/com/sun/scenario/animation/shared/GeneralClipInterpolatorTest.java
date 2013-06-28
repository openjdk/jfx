/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.animation.shared;

import com.sun.javafx.animation.TickCalculation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;

import org.junit.Test;

public class GeneralClipInterpolatorTest {
	
	@Test
	public void testSetKeyFrame() {
    	final KeyFrame kf1 = new KeyFrame(Duration.ZERO);
    	final KeyFrame kf2 = new KeyFrame(Duration.millis(1000));
    	final KeyFrame kf3 = new KeyFrame(Duration.millis(2000));
    	final GeneralClipInterpolator gci = new GeneralClipInterpolator(new KeyFrame[] {kf1, kf2, kf3}, new long[] {0, 6000, 6000});
    	
    	assertSame   (gci, gci.setKeyFrames(new KeyFrame[]{kf1, kf2, kf3}, new long[] {0, 6000, 6000}));
    	assertNotSame(gci, gci.setKeyFrames(new KeyFrame[]{kf1, kf2     }, new long[] {0, 6000      }));
    	assertNotSame(gci, gci.setKeyFrames(new KeyFrame[]{kf1,      kf3}, new long[] {0,       6000}));
    	assertSame   (gci, gci.setKeyFrames(new KeyFrame[]{kf1          }, new long[] {0            }));
    	assertSame   (gci, gci.setKeyFrames(new KeyFrame[]{     kf2, kf3}, new long[] {   6000, 6000}));
    	assertNotSame(gci, gci.setKeyFrames(new KeyFrame[]{     kf2     }, new long[] {   6000      }));
    	assertNotSame(gci, gci.setKeyFrames(new KeyFrame[]{          kf3}, new long[] {         6000}));
    	assertSame   (gci, gci.setKeyFrames(new KeyFrame[]{             }, new long[] {             }));
	}
	
	@Test
	public void test_OneKeyFrameOnly() {
		// main purpose of this test is to ensure we do not throw exceptions
		final KeyFrame kf1 = new KeyFrame(Duration.ZERO);
		final KeyFrame kf2 = new KeyFrame(Duration.ZERO);
		
    	// one key frame
    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {kf1}, new long[] {0});
    	gci1.validate(true);
    	gci1.validate(true);
    	gci1.setKeyFrames(new KeyFrame[]{kf2}, new long[] {6000});
    	gci1.validate(true);
    	gci1.setKeyFrames(new KeyFrame[]{}, new long[] {});
    	gci1.validate(true);
    	
    	// no key frames
    	final GeneralClipInterpolator gci2 = new GeneralClipInterpolator (new KeyFrame[] {}, new long[] {});
    	gci2.validate(true);
    	gci2.validate(true);
    	gci2.setKeyFrames(new KeyFrame[]{kf1}, new long[] {0});
    	gci2.validate(true);
    }

    @Test
    public void test_NoKeyValues() {
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO);
    	final KeyFrame start2 = new KeyFrame(Duration.ZERO);
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000));
    	final KeyFrame end2a = new KeyFrame(Duration.millis(2000));
    	final KeyFrame end2b = new KeyFrame(Duration.millis(3000));
    	
    	// four key frames
    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {start1, start2, end1, end2a}, new long[] {0, 0, 6000, 12000});
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(800));
    	gci1.interpolate(TickCalculation.fromMillis(1800));
    	
    	gci1.setKeyFrames(new KeyFrame[]{start1, start2, end1, end2b}, new long[] {0, 0, 6000, 18000});
    	gci1.interpolate(TickCalculation.fromMillis(400));
    	gci1.interpolate(TickCalculation.fromMillis(1400));
    	
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(600));
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	
    	// two key frames
    	final GeneralClipInterpolator gci2 = new GeneralClipInterpolator (new KeyFrame[] {end1, end2a}, new long[] {6000, 12000});
    	gci2.validate(true);
    	gci2.interpolate(TickCalculation.fromMillis(300));
    	gci2.interpolate(TickCalculation.fromMillis(1300));
    	
    	gci2.validate(true);
    	gci2.interpolate(TickCalculation.fromMillis(800));
    	gci2.interpolate(TickCalculation.fromMillis(1800));
    	
    	gci2.setKeyFrames(new KeyFrame[]{end1, end2b}, new long[] {6000, 18000});
    	gci2.interpolate(TickCalculation.fromMillis(400));
    	gci2.interpolate(TickCalculation.fromMillis(1400));
    	
    	gci2.validate(true);
    	gci2.interpolate(TickCalculation.fromMillis(600));
    	gci2.interpolate(TickCalculation.fromMillis(1600));
    }

    @Test
    public void test_ThreeKeyFrames_OneKeyValue() {
    	final IntegerProperty v = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO);
    	final KeyFrame start2 = new KeyFrame(Duration.ZERO, new KeyValue(v, 30));
    	final KeyFrame mid1 = new KeyFrame(Duration.millis(1000), new KeyValue(v, 40));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(2000), new KeyValue(v, 60));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(4000), new KeyValue(v, 70));
    	
    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {start1, start2, mid1, end1}, new long[] {0, 0, 6000, 12000});
    	v.set(0);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals(33, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	assertEquals(52, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// re-validate
    	v.set(20);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals(38, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1200));
    	assertEquals(44, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// set new key frames
    	gci1.setKeyFrames(new KeyFrame[]{start1, start2, mid1, end2}, new long[] {0, 0, 6000, 24000});
    	v.set(0);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals(36, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals(46, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// validate new key frames
    	v.set(0);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(700));
    	assertEquals(37, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals(43, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(4000));
    	assertEquals(70, v.get());
    }

    @Test
    public void test_TwoKeyFrames_OneKeyValue() {
    	final IntegerProperty v = new SimpleIntegerProperty();
    	final KeyFrame mid1 = new KeyFrame(Duration.millis(1000), new KeyValue(v, 40));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(2000), new KeyValue(v, 60));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(4000), new KeyValue(v, 70));

    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {mid1, end1}, new long[] {6000, 12000});
    	v.set(0);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 0, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals(12, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	assertEquals(52, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// re-validate
    	v.set(20);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(20, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals(36, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1200));
    	assertEquals(44, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// set new key frames
    	gci1.setKeyFrames(new KeyFrame[]{mid1, end2}, new long[] {6000, 24000});
    	v.set(0);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(20, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals(32, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals(46, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    	// validate new key frames
    	v.set(0);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 0, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(700));
    	assertEquals(28, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals(43, v.get());
    	gci1.interpolate(TickCalculation.fromMillis(4000));
    	assertEquals(70, v.get());
    }

    @Test
    public void test_ThreeKeyFrames_ThreeKeyValues() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
    	final IntegerProperty v2 = new SimpleIntegerProperty();
    	final IntegerProperty v3 = new SimpleIntegerProperty();
    	final IntegerProperty v4 = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO, new KeyValue(v3, 230), new KeyValue(v4, 330));
    	final KeyFrame start2 = new KeyFrame(Duration.ZERO, new KeyValue(v2, 130));
    	final KeyFrame mid1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 140), new KeyValue(v3, 240));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v2, 160));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(4000), new KeyValue(v1, 70), new KeyValue(v2, 170));
    	
    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {start1, start2, mid1, end1}, new long[] {0, 0, 6000, 12000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	v4.set(300);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(  0, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(230, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 12, v1.get());
    	assertEquals(133, v2.get());
    	assertEquals(233, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	assertEquals( 52, v1.get());
    	assertEquals(152, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	
    	// re-validate
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	v4.set(320);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(230, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals( 36, v1.get());
    	assertEquals(138, v2.get());
    	assertEquals(238, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1200));
    	assertEquals( 44, v1.get());
    	assertEquals(144, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    	
    	// change key frames
    	gci1.setKeyFrames(new KeyFrame[]{start1, start2, mid1, end2}, new long[] {0, 0, 6000, 24000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	v4.set(300);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(230, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 26, v1.get());
    	assertEquals(133, v2.get());
    	assertEquals(233, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals( 46, v1.get());
    	assertEquals(146, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(300, v4.get());
    	
    	// validate new key frames
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	v4.set(320);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(230, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals( 32, v1.get());
    	assertEquals(136, v2.get());
    	assertEquals(236, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals( 43, v1.get());
    	assertEquals(143, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    	gci1.interpolate(TickCalculation.fromMillis(4000));
    	assertEquals( 70, v1.get());
    	assertEquals(170, v2.get());
    	assertEquals(240, v3.get());
    	assertEquals(320, v4.get());
    }

    @Test
    public void test_TwoKeyFrames_ThreeKeyValues() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
    	final IntegerProperty v2 = new SimpleIntegerProperty();
    	final IntegerProperty v3 = new SimpleIntegerProperty();
    	final KeyFrame mid1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 140), new KeyValue(v3, 240));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v2, 160), new KeyValue(v3, 260));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(4000), new KeyValue(v1, 70), new KeyValue(v2, 170), new KeyValue(v3, 270));

    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {mid1, end1}, new long[] {6000, 12000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(  0, v1.get());
    	assertEquals(100, v2.get());
    	assertEquals(200, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 12, v1.get());
    	assertEquals(112, v2.get());
    	assertEquals(212, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	assertEquals( 52, v1.get());
    	assertEquals(152, v2.get());
    	assertEquals(252, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(260, v3.get());
    	
    	// re-validate
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals( 36, v1.get());
    	assertEquals(136, v2.get());
    	assertEquals(236, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1200));
    	assertEquals( 44, v1.get());
    	assertEquals(144, v2.get());
    	assertEquals(244, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(260, v3.get());
    	
    	// change key frames
    	gci1.setKeyFrames(new KeyFrame[]{mid1, end2}, new long[] {6000, 24000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 26, v1.get());
    	assertEquals(126, v2.get());
    	assertEquals(226, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals( 46, v1.get());
    	assertEquals(146, v2.get());
    	assertEquals(246, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(260, v3.get());
    	
    	// validate new key frames
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals( 32, v1.get());
    	assertEquals(132, v2.get());
    	assertEquals(232, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(1300));
    	assertEquals( 43, v1.get());
    	assertEquals(143, v2.get());
    	assertEquals(243, v3.get());
    	gci1.interpolate(TickCalculation.fromMillis(4000));
    	assertEquals( 70, v1.get());
    	assertEquals(170, v2.get());
    	assertEquals(270, v3.get());
    }


    @Test
    public void test_DuplicateKeyValue() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
    	final IntegerProperty v2 = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO, new KeyValue(v1, 30), new KeyValue(v2, 0));
    	final KeyFrame start2 = new KeyFrame(Duration.ZERO, new KeyValue(v1, 30), new KeyValue(v1, -30), new KeyValue(v2, 0));
    	final KeyFrame mid1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 100));
    	final KeyFrame mid2 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v1, -40), new KeyValue(v2, 100));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v2, 0));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v1, -60), new KeyValue(v2, 0));

        // single value in start, single value in mid, duplicate value in end
    	final GeneralClipInterpolator gci1 = new GeneralClipInterpolator (new KeyFrame[] {start1, mid1, end2}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci1.validate(true);
    	gci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v1.get());
        assertEquals(0, v2.get());
    	gci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals(33, v1.get());
        assertEquals(30, v2.get());
    	gci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v1.get());
        assertEquals(100, v2.get());
    	gci1.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()));
        assertEquals(40, v2.get());
    	gci1.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());

        // single value in start, duplicate value in mid, single value in end
    	final GeneralClipInterpolator gci2 = new GeneralClipInterpolator (new KeyFrame[] {start1, mid2, end1}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci2.validate(true);
    	gci2.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v1.get());
        assertEquals(0, v2.get());
    	gci2.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()));
        assertEquals(30, v2.get());
    	gci2.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci2.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (20 == v1.get()));
        assertEquals(40, v2.get());
    	gci2.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v1.get());
        assertEquals(0, v2.get());

        // duplicate value in start, single value in mid, single value in end
    	final GeneralClipInterpolator gci3 = new GeneralClipInterpolator (new KeyFrame[] {start2, mid1, end1}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci3.validate(true);
    	gci3.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	gci3.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (-9 == v1.get()));
        assertEquals(30, v2.get());
    	gci3.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v1.get());
        assertEquals(100, v2.get());
    	gci3.interpolate(TickCalculation.fromMillis(1600));
    	assertEquals(52, v1.get());
        assertEquals(40, v2.get());
    	gci3.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v1.get());
        assertEquals(0, v2.get());

        // duplicate value in start, duplicate value in mid, single value in end
    	final GeneralClipInterpolator gci4 = new GeneralClipInterpolator (new KeyFrame[] {start2, mid2, end1}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci4.validate(true);
    	gci4.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	gci4.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()) || (-9 == v1.get()) || (-33 == v1.get()));
        assertEquals(30, v2.get());
    	gci4.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci4.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (20 == v1.get()));
        assertEquals(40, v2.get());
    	gci4.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v1.get());
        assertEquals(0, v2.get());

        // duplicate value in start, single value in mid, duplicate value in end
    	final GeneralClipInterpolator gci5 = new GeneralClipInterpolator (new KeyFrame[] {start2, mid1, end2}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci5.validate(true);
    	gci5.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	gci5.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (-9 == v1.get()));
        assertEquals(30, v2.get());
    	gci5.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v1.get());
        assertEquals(100, v2.get());
    	gci5.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()));
        assertEquals(40, v2.get());
    	gci5.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());

        // single value in start, duplicate value in mid, duplicate value in end
    	final GeneralClipInterpolator gci6 = new GeneralClipInterpolator (new KeyFrame[] {start1, mid2, end2}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci6.validate(true);
    	gci6.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v1.get());
        assertEquals(0, v2.get());
    	gci6.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()));
        assertEquals(30, v2.get());
    	gci6.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci6.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()) || (20 == v1.get()) || (-52 == v1.get()));
        assertEquals(40, v2.get());
    	gci6.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());

        // duplicate value in start, duplicate value in mid, duplicate value in end
    	final GeneralClipInterpolator gci7 = new GeneralClipInterpolator (new KeyFrame[] {start2, mid2, end2}, new long[] {0, 6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci7.validate(true);
    	gci7.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	gci7.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()) || (-9 == v1.get()) || (-33 == v1.get()));
        assertEquals(30, v2.get());
    	gci7.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci7.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()) || (20 == v1.get()) || (-52 == v1.get()));
        assertEquals(40, v2.get());
    	gci7.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());

        // no value in start, single value in mid, duplicate value in end
    	final GeneralClipInterpolator gci8 = new GeneralClipInterpolator (new KeyFrame[] {mid1, end2}, new long[] {6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci8.validate(true);
    	gci8.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(0, v1.get());
        assertEquals(0, v2.get());
    	gci8.interpolate(TickCalculation.fromMillis(400));
    	assertEquals(16, v1.get());
        assertEquals(40, v2.get());
    	gci8.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v1.get());
        assertEquals(100, v2.get());
    	gci8.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()));
        assertEquals(40, v2.get());
    	gci8.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());

        // no value in start, duplicate value in mid, single value in end
    	final GeneralClipInterpolator gci9 = new GeneralClipInterpolator (new KeyFrame[] {mid2, end1}, new long[] {6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci9.validate(true);
    	gci9.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(0, v1.get());
        assertEquals(0, v2.get());
    	gci9.interpolate(TickCalculation.fromMillis(400));
    	assertTrue("v1.get(): " + v1.get(), (16 == v1.get()) || (-16 == v1.get()));
        assertEquals(40, v2.get());
    	gci9.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci9.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (20 == v1.get()));
        assertEquals(40, v2.get());
    	gci9.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v1.get());
        assertEquals(0, v2.get());

        // no value in start, duplicate value in mid, duplicate value in end
    	final GeneralClipInterpolator gci10 = new GeneralClipInterpolator (new KeyFrame[] {mid2, end2}, new long[] {6000, 12000});
    	v1.set(0);
    	v2.set(0);
    	gci10.validate(true);
    	gci10.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(0, v1.get());
        assertEquals(0, v2.get());
    	gci10.interpolate(TickCalculation.fromMillis(400));
    	assertTrue("v1.get(): " + v1.get(), (16 == v1.get()) || (-16 == v1.get()));
        assertEquals(40, v2.get());
    	gci10.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    	gci10.interpolate(TickCalculation.fromMillis(1600));
    	assertTrue("v1.get(): " + v1.get(), (52 == v1.get()) || (-20 == v1.get()) || (20 == v1.get()) || (-52 == v1.get()));
        assertEquals(40, v2.get());
    	gci10.interpolate(TickCalculation.fromMillis(2000));
    	assertTrue("v1.get(): " + v1.get(), (60 == v1.get()) || (-60 == v1.get()));
        assertEquals(0, v2.get());
    }
}
