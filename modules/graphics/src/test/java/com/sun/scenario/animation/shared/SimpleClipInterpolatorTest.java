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

public class SimpleClipInterpolatorTest {
	
	@Test
	public void testSetKeyFrame() {
    	final KeyFrame kf1 = new KeyFrame(Duration.ZERO);
    	final KeyFrame kf2 = new KeyFrame(Duration.millis(1000));
    	final KeyFrame kf3 = new KeyFrame(Duration.millis(2000));
    	final SimpleClipInterpolator sci = new SimpleClipInterpolator(kf1, kf2,  6000);
    	
    	assertNotSame(sci, sci.setKeyFrames(new KeyFrame[]{kf1, kf2, kf3}, new long[] {0, 6000, 12000}));
    	assertSame   (sci, sci.setKeyFrames(new KeyFrame[]{kf1, kf2     }, new long[] {0, 6000,      }));
    	assertSame   (sci, sci.setKeyFrames(new KeyFrame[]{kf1,      kf3}, new long[] {0,       12000}));
    	assertNotSame(sci, sci.setKeyFrames(new KeyFrame[]{kf1          }, new long[] {0,            }));
    	assertNotSame(sci, sci.setKeyFrames(new KeyFrame[]{     kf2, kf3}, new long[] {   6000, 12000}));
    	assertSame   (sci, sci.setKeyFrames(new KeyFrame[]{     kf2     }, new long[] {   6000       }));
    	assertSame   (sci, sci.setKeyFrames(new KeyFrame[]{          kf3}, new long[] {         12000}));
    	assertNotSame(sci, sci.setKeyFrames(new KeyFrame[]{             }, new long[] {              }));
	}
    
    @Test
    public void test_NoKeyValues() {
    	final KeyFrame start = new KeyFrame(Duration.ZERO);
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000));
    	
    	// two key frames
    	final SimpleClipInterpolator sci1 = new SimpleClipInterpolator (start, end1, 6000);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(800));
    	
    	sci1.setKeyFrames(new KeyFrame[]{start, end2}, new long[] {0, 12000});
    	sci1.interpolate(TickCalculation.fromMillis(400));
    	
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(600));
    	
    	// one key frame
    	final SimpleClipInterpolator sci2 = new SimpleClipInterpolator (end1, 6000);
    	sci2.validate(true);
    	sci2.interpolate(TickCalculation.fromMillis(300));
    	
    	sci2.validate(true);
    	sci2.interpolate(TickCalculation.fromMillis(800));
    	
    	sci2.setKeyFrames(new KeyFrame[]{end2}, new long[] {12000});
    	sci2.interpolate(TickCalculation.fromMillis(400));
    	
    	sci2.validate(true);
    	sci2.interpolate(TickCalculation.fromMillis(600));
    }

    @Test
    public void test_TwoKeyFrames_OneKeyValue() {
    	final IntegerProperty v = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO, new KeyValue(v, 30));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000), new KeyValue(v, 40));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000), new KeyValue(v, 60));
    	
    	final SimpleClipInterpolator sci1 = new SimpleClipInterpolator (start1, end1, 6000);
    	v.set(0);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals(33, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// re-validate
    	v.set(20);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals(38, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// set new key frames
    	sci1.setKeyFrames(new KeyFrame[]{start1, end2}, new long[] {0, 12000});
    	v.set(0);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(400));
    	assertEquals(34, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// validate new key frames
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals(39, v.get());
    	sci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    	
    }

    @Test
    public void test_OneKeyFrame_OneKeyValue() {
    	final IntegerProperty v = new SimpleIntegerProperty();
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000), new KeyValue(v, 40));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000), new KeyValue(v, 60));

    	final SimpleClipInterpolator sci3 = new SimpleClipInterpolator (end1, 6000);
    	v.set(0);
    	sci3.validate(true);
    	sci3.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(0, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(300));
    	assertEquals(12, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// re-validate
    	v.set(20);
    	sci3.validate(true);
    	sci3.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(20, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(800));
    	assertEquals(36, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// set new key frames
    	sci3.setKeyFrames(new KeyFrame[]{end2}, new long[] {12000});
    	v.set(0);
    	sci3.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(20, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(400));
    	assertEquals(28, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals(40, v.get());
    	
    	// validate new key frames
    	v.set(20);
    	sci3.validate(true);
    	sci3.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(20, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(600));
    	assertEquals(32, v.get());
    	sci3.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals(60, v.get());
    }

    @Test
    public void test_TwoKeyFrames_ThreeKeyValues() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
    	final IntegerProperty v2 = new SimpleIntegerProperty();
    	final IntegerProperty v3 = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO, new KeyValue(v2, 130), new KeyValue(v3, 230));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 140));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v2, 160));
    	
    	final SimpleClipInterpolator sci1 = new SimpleClipInterpolator (start1, end1, 6000);
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(  0, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(200, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 12, v1.get());
    	assertEquals(133, v2.get());
    	assertEquals(200, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(200, v3.get());
    	
    	// re-validate
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals( 36, v1.get());
    	assertEquals(138, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(220, v3.get());
    	
    	// change key frames
    	sci1.setKeyFrames(new KeyFrame[]{start1, end2}, new long[] {0, 12000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(200, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 26, v1.get());
    	assertEquals(133, v2.get());
    	assertEquals(200, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(200, v3.get());
    	
    	// validate new key frames
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(130, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals( 32, v1.get());
    	assertEquals(139, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(220, v3.get());
    	
    }

    @Test
    public void test_OneKeyFrames_ThreeKeyValues() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
    	final IntegerProperty v2 = new SimpleIntegerProperty();
    	final IntegerProperty v3 = new SimpleIntegerProperty();
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 140), new KeyValue(v3, 240));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(2000), new KeyValue(v1, 60), new KeyValue(v2, 160), new KeyValue(v3, 260));

    	final SimpleClipInterpolator sci1 = new SimpleClipInterpolator (end1, 6000);
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(  0, v1.get());
    	assertEquals(100, v2.get());
    	assertEquals(200, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 12, v1.get());
    	assertEquals(112, v2.get());
    	assertEquals(212, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	
    	// re-validate
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(800));
    	assertEquals( 36, v1.get());
    	assertEquals(136, v2.get());
    	assertEquals(236, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	
    	// change key frames
    	sci1.setKeyFrames(new KeyFrame[]{end2}, new long[] {12000});
    	v1.set(  0);
    	v2.set(100);
    	v3.set(200);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertEquals( 26, v1.get());
    	assertEquals(126, v2.get());
    	assertEquals(226, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertEquals( 40, v1.get());
    	assertEquals(140, v2.get());
    	assertEquals(240, v3.get());
    	
    	// validate new key frames
    	v1.set( 20);
    	v2.set(120);
    	v3.set(220);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals( 20, v1.get());
    	assertEquals(120, v2.get());
    	assertEquals(220, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(600));
    	assertEquals( 32, v1.get());
    	assertEquals(132, v2.get());
    	assertEquals(232, v3.get());
    	sci1.interpolate(TickCalculation.fromMillis(2000));
    	assertEquals( 60, v1.get());
    	assertEquals(160, v2.get());
    	assertEquals(260, v3.get());
    }

    @Test
    public void test_DuplicateKeyValue() {
    	final IntegerProperty v1 = new SimpleIntegerProperty();
        final IntegerProperty v2 = new SimpleIntegerProperty();
    	final KeyFrame start1 = new KeyFrame(Duration.ZERO, new KeyValue(v1, 30), new KeyValue(v2, 0));
    	final KeyFrame start2 = new KeyFrame(Duration.ZERO, new KeyValue(v1, 30), new KeyValue(v1, -30), new KeyValue(v2, 0));
    	final KeyFrame end1 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v2, 100));
    	final KeyFrame end2 = new KeyFrame(Duration.millis(1000), new KeyValue(v1, 40), new KeyValue(v1, -40), new KeyValue(v2, 100));

        // single value in start, duplicate value in end
    	final SimpleClipInterpolator sci1 = new SimpleClipInterpolator (start1, end2, 6000);
    	v1.set(0);
    	v2.set(0);
    	sci1.validate(true);
    	sci1.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(30, v1.get());
        assertEquals(0, v2.get());
    	sci1.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()));
        assertEquals(30, v2.get());
    	sci1.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());

        // duplicate value in start, single value in end
    	final SimpleClipInterpolator sci2 = new SimpleClipInterpolator (start2, end1, 6000);
    	v1.set(0);
    	v2.set(0);
    	sci2.validate(true);
    	sci2.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	sci2.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (-9 == v1.get()));
        assertEquals(30, v2.get());
    	sci2.interpolate(TickCalculation.fromMillis(1000));
        assertEquals(40, v1.get());
        assertEquals(100, v2.get());

        // duplicate value in start, duplicate value in end
    	final SimpleClipInterpolator sci3 = new SimpleClipInterpolator (start2, end2, 6000);
    	v1.set(0);
    	v2.set(0);
    	sci3.validate(true);
    	sci3.interpolate(TickCalculation.fromMillis(0));
    	assertTrue("v1.get(): " + v1.get(), (30 == v1.get()) || (-30 == v1.get()));
        assertEquals(0, v2.get());
    	sci3.interpolate(TickCalculation.fromMillis(300));
    	assertTrue("v1.get(): " + v1.get(), (33 == v1.get()) || (9 == v1.get()) || (-9 == v1.get()) || (-33 == v1.get()));
        assertEquals(30, v2.get());
    	sci3.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());

        // no value in start, duplicate value in end
    	final SimpleClipInterpolator sci4 = new SimpleClipInterpolator (end2, 6000);
    	v1.set(0);
    	v2.set(0);
    	sci4.validate(true);
    	sci4.interpolate(TickCalculation.fromMillis(0));
    	assertEquals(0, v1.get());
        assertEquals(0, v2.get());
    	sci4.interpolate(TickCalculation.fromMillis(400));
    	assertTrue("v1.get(): " + v1.get(), (16 == v1.get()) || (-16 == v1.get()));
        assertEquals(40, v2.get());
    	sci4.interpolate(TickCalculation.fromMillis(1000));
    	assertTrue("v1.get(): " + v1.get(), (40 == v1.get()) || (-40 == v1.get()));
        assertEquals(100, v2.get());
    }

}
