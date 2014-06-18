/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SharedBufferTest {

    private static final int SEGMENT_SIZE = 0x1000;
    private static final Random random = new Random();


    private SharedBuffer sb = new SharedBuffer();


    @BeforeClass
    public static void beforeClass() throws ClassNotFoundException {
        Class.forName(WebPage.class.getName());
    }


    @Test
    public void testConstructor1() {
        sb.dispose();
        sb = new SharedBuffer();
    }

    @Test
    public void testSizePredefinedIncrements() {
        int[] increments = new int[] {
            1,
            5,
            10,
            100,
            1000,
            SEGMENT_SIZE,
            SEGMENT_SIZE * 2,
            SEGMENT_SIZE * 10,
        };
        int expected = 0;
        assertEquals(expected, sb.size());
        for (int increment : increments) {
            sb.append(new byte[increment], 0, increment);
            expected += increment;
            assertEquals(expected, sb.size());
        }
    }

    @Test
    public void testSizeRandomIncrements() {
        int expected = 0;
        assertEquals(expected, sb.size());
        for (int i = 0; i < 100; i++) {
            int increment = random.nextInt(SEGMENT_SIZE * 10);
            sb.append(new byte[increment], 0, increment);
            expected += increment;
            assertEquals(expected, sb.size());
        }
    }

    @Test
    public void testSizeZeroNativePointer() {
        sb.dispose();
        try {
            sb.dispose();
            fail("IllegalStateException but not thrown");
        } catch (IllegalStateException expected) {}
        sb = null;
    }

    @Test
    public void testGetSomeDataFirstSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(0, 10), getSomeData(0, 10));
    }

    @Test
    public void testGetSomeDataFirstSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(7, 10), getSomeData(7, 10));
    }

    @Test
    public void testGetSomeDataFirstSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE - 10, 10),
                getSomeData(SEGMENT_SIZE - 10, 10));
    }

    @Test
    public void testGetSomeDataFirstSegmentLastTenBytesWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE - 5, 5),
                getSomeData(SEGMENT_SIZE - 5, 10));
    }

    @Test
    public void testGetSomeDataInteriorSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(SEGMENT_SIZE, 10), getSomeData(SEGMENT_SIZE, 10));
    }

    @Test
    public void testGetSomeDataInteriorSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE + 9, 10),
                getSomeData(SEGMENT_SIZE + 9, 10));
    }

    @Test
    public void testGetSomeDataInteriorSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2 - 10, 10),
                getSomeData(SEGMENT_SIZE * 2 - 10, 10));
    }

    @Test
    public void testGetSomeDataInteriorSegmentLastTenBytesWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2 - 5, 5),
                getSomeData(SEGMENT_SIZE * 2 - 5, 10));
    }

    @Test
    public void testGetSomeDataLastSegmentFirstTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2, 10),
                getSomeData(SEGMENT_SIZE * 2, 10));
    }

    @Test
    public void testGetSomeDataLastSegmentInteriorTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2 + 9, 10),
                getSomeData(SEGMENT_SIZE * 2 + 9, 10));
    }

    @Test
    public void testGetSomeDataLastSegmentLastTenBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2.5 - 10, 10),
                getSomeData(SEGMENT_SIZE * 2.5 - 10, 10));
    }

    @Test
    public void testGetSomeDataLastSegmentLastTenBytesWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2.5 - 5, 5),
                getSomeData(SEGMENT_SIZE * 2.5 - 5, 10));
    }

    @Test
    public void testGetSomeDataTenBytesAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE * 2.5, 10));
    }

    @Test
    public void testGetSomeDataTenBytesFromEmptyBuffer() {
        assertArrayEquals(new byte[0], getSomeData(0, 10));
    }

    @Test
    public void testGetSomeDataFirstSegment() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(0, SEGMENT_SIZE), getSomeData(0, SEGMENT_SIZE));
    }

    @Test
    public void testGetSomeDataFirstSegmentWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 0.5, SEGMENT_SIZE - SEGMENT_SIZE * 0.5),
                getSomeData(SEGMENT_SIZE * 0.5, SEGMENT_SIZE));
    }

    @Test
    public void testGetSomeDataInteriorSegment() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE, SEGMENT_SIZE),
                getSomeData(SEGMENT_SIZE, SEGMENT_SIZE));
    }

    @Test
    public void testGetSomeDataInteriorSegmentWithTruncation() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 1.5, SEGMENT_SIZE - SEGMENT_SIZE * 0.5),
                getSomeData(SEGMENT_SIZE * 1.5, SEGMENT_SIZE));
    }

    @Test
    public void testGetSomeDataLastSegment() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE * 0.5),
                getSomeData(SEGMENT_SIZE * 2, SEGMENT_SIZE));
    }

    @Test
    public void testGetSomeDataFirstSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(0, 0));
    }

    @Test
    public void testGetSomeDataFirstSegmentInteriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE * 0.5, 0));
    }

    @Test
    public void testGetSomeDataInteriorSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE, 0));
    }

    @Test
    public void testGetSomeDataInteriorSegmentInterriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE * 1.5, 0));
    }

    @Test
    public void testGetSomeDataLastSegmentFirstZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE * 2, 0));
    }

    @Test
    public void testGetSomeDataLastSegmentInteriorZeroBytes() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(g(0,0), getSomeData(SEGMENT_SIZE * 2 + 7, 0));
    }

    @Test
    public void testGetSomeDataZeroBytesAfterLastByte() {
        append(SEGMENT_SIZE * 2.5);
        assertArrayEquals(new byte[0], getSomeData(SEGMENT_SIZE * 2.5, 0));
    }

    @Test
    public void testGetSomeDataZeroBytesFromEmptyBuffer() {
        assertArrayEquals(new byte[0], getSomeData(0, 0));
    }

    @Test
    public void testGetSomeDataZeroNativePointer() {
        sb.dispose();
        try {
            sb.getSomeData(0, new byte[1], 0, 1);
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}
        sb = null;
    }

    @Test
    public void testGetSomeDataNegativePosition() {
        try {
            sb.getSomeData(-1, new byte[1], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testGetSomeDataPositionGreaterThanSize() {
        try {
            sb.getSomeData(1, new byte[1], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
        try {
            sb.getSomeData(100, new byte[1], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        append(100);
        try {
            sb.getSomeData(101, new byte[1], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
        try {
            sb.getSomeData(200, new byte[1], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testGetSomeDataNullBuffer() {
        try {
            sb.getSomeData(0, null, 0, 0);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testGetSomeDataNegativeOffset() {
        try {
            sb.getSomeData(0, new byte[0], -1, 0);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testGetSomeDataNegativeLength() {
        try {
            sb.getSomeData(0, new byte[0], 0, -1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testGetSomeDataIllegalBufferOrOffsetOrLength() {
        try {
            sb.getSomeData(0, new byte[0], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.getSomeData(0, new byte[0], 1, 0);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.getSomeData(0, new byte[10], 0, 11);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.getSomeData(0, new byte[10], 1, 10);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testAppendTenBytes() {
        append(g(0, 10));
        assertSharedBufferContains(g(0, 10));
    }

    @Test
    public void testAppendSegment() {
        append(g(0, SEGMENT_SIZE));
        assertSharedBufferContains(g(0, SEGMENT_SIZE));
    }

    @Test
    public void testAppendThreeSegments() {
        append(g(0, SEGMENT_SIZE * 3));
        assertSharedBufferContains(
                g(0, SEGMENT_SIZE),
                g(SEGMENT_SIZE, SEGMENT_SIZE),
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE));
    }

    @Test
    public void testAppendThreeSegmentsPlusSegmentPlusTenBytes() {
        append(g(0, SEGMENT_SIZE * 3));
        append(g(0, SEGMENT_SIZE));
        append(g(0, 10));
        assertSharedBufferContains(
                g(0, SEGMENT_SIZE),
                g(SEGMENT_SIZE, SEGMENT_SIZE),
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE),
                g(0, SEGMENT_SIZE),
                g(0, 10));
    }

    @Test
    public void testAppendTenBytesPlusSegmentPlusThreeSegments() {
        append(g(0, 10));
        append(g(0, SEGMENT_SIZE));
        append(g(0, SEGMENT_SIZE * 3));
        assertSharedBufferContains(
                g(0, 10),
                g(0, SEGMENT_SIZE),
                g(0, SEGMENT_SIZE),
                g(SEGMENT_SIZE, SEGMENT_SIZE),
                g(SEGMENT_SIZE * 2, SEGMENT_SIZE));
    }

    @Test
    public void testAppendZeroBytes() {
        append(new byte[0]);
        assertSharedBufferContains();
    }

    @Test
    public void testAppendZeroBytesPlusTenBytes() {
        append(new byte[0]);
        append(g(0, 10));
        assertSharedBufferContains(g(0, 10));
    }

    @Test
    public void testAppendTenBytesPlusZeroBytes() {
        append(g(0, 10));
        append(new byte[0]);
        assertSharedBufferContains(g(0, 10));
    }

    @Test
    public void testAppendZeroNativePointer() {
        sb.dispose();
        try {
            sb.append(new byte[1], 0, 1);
            fail("IllegalStateException expected but not thrown");
        } catch (IllegalStateException expected) {}
        sb = null;
    }

    @Test
    public void testAppendNullBuffer() {
        try {
            sb.append(null, 0, 1);
            fail("NullPointerException expected but not thrown");
        } catch (NullPointerException expected) {}
    }

    @Test
    public void testAppendNegativeOffset() {
        try {
            sb.append(new byte[0], -1, 0);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testAppendNegativeLength() {
        try {
            sb.append(new byte[0], 0, -1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testAppendIllegalBufferOrOffsetOrLength() {
        try {
            sb.append(new byte[0], 0, 1);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.append(new byte[0], 1, 0);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.append(new byte[10], 0, 11);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}

        try {
            sb.append(new byte[10], 1, 10);
            fail("IndexOutOfBoundsException expected but not thrown");
        } catch (IndexOutOfBoundsException expected) {}
    }

    @Test
    public void testDispose() {
        sb.dispose();
        sb = null;
    }

    @Test
    public void testDisposeZeroNativePointer() {
        sb.dispose();
        try {
            sb.dispose();
            fail("IllegalStateException but not thrown");
        } catch (IllegalStateException expected) {}
        sb = null;
    }


    @After
    public void after() {
        if (sb != null) {
            sb.dispose();
        }
    }

    private void append(double length) {
        byte[] data = g(0, (int) length);
        sb.append(data, 0, data.length);
    }

    private static byte[] g(double start, double count) {
        int intCount = (int) count;
        byte[] result = new byte[intCount];
        for (int i = 0; i < intCount; i++) {
            result[i] = (byte) ((i + (int) start) & 0xff);
        }
        return result;
    }

    private byte[] getSomeData(double position, int length) {
        int offset = random.nextBoolean() ? random.nextInt(100) : 0;
        int extraLength = random.nextBoolean() ? random.nextInt(200) : 0;
        byte[] buffer = g(0, offset + length + extraLength);
        int len = sb.getSomeData((long) position, buffer, offset, length);
        assertTrue("Unexpected len: " + len, len >= 0);
        for (int i = 0; i < offset; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
        for (int i = offset + len; i < buffer.length; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
        byte[] result = new byte[len];
        System.arraycopy(buffer, offset, result, 0, len);
        return result;
    }

    private void append(byte[] data) {
        int offset = random.nextBoolean() ? random.nextInt(100) : 0;
        int extraLength = random.nextBoolean() ? random.nextInt(200) : 0;
        byte[] buffer = g(0, offset + data.length + extraLength);
        System.arraycopy(data, 0, buffer, offset, data.length);
        sb.append(buffer, offset, data.length);
        for (int i = 0; i < offset; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
        for (int i = offset + data.length; i < buffer.length; i++) {
            assertEquals((byte) (i & 0xff), buffer[i]);
        }
    }

    private void assertSharedBufferContains(byte[]... expectedChunks) {
        ArrayList<byte[]> expectedChunkList =
                new ArrayList<byte[]>(Arrays.asList(expectedChunks));
        expectedChunkList.add(new byte[0]);
        long position = 0;
        for (byte[] expectedChunk : expectedChunkList) {
            byte[] buffer = new byte[SEGMENT_SIZE + 1];
            int len = sb.getSomeData(position, buffer, 0, buffer.length);
            byte[] actualChunk = new byte[len];
            System.arraycopy(buffer, 0, actualChunk, 0, len);
            assertArrayEquals(expectedChunk, actualChunk);
            position += len;
        }
    }
}
